package com.lime.player.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.huawei.hmt.playercontroller.SkipType;
import com.lime.player.R;
import com.lime.player.data.LibraryRepository;
import com.lime.player.data.MediaEntry;
import com.lime.player.service.PlaybackCommandBus;
import com.lime.player.util.Prefs;
import com.lime.player.util.Ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 全屏播放器：ExoPlayer + 类爱奇艺控制层。 */
public class PlayerActivity extends AppCompatActivity implements PlaybackCommandBus.Sink {

    private static final long AUTO_HIDE_MS = 4000;
    private static final float[] SPEEDS = {1f, 1.25f, 1.5f, 1.75f, 2f, 0.75f};
    private static final String[] RATIO_LABELS = {"适应", "铺满", "裁剪"};

    private PlayerView playerView;
    private ExoPlayer player;
    private FrameLayout root;
    private View topBar;
    private View bottomBar;
    private ImageView centerBtn;
    private ImageView btnPlay;
    private ImageView back;
    private TextView title;
    private TextView timeCurrent;
    private TextView timeTotal;
    private TextView btnSpeed;
    private TextView btnRatio;
    private TextView btnNext;
    private TextView btnEpisodes;
    private TextView hud;
    private SeekBar seek;
    private ProgressBar buffering;

    private final List<MediaEntry> playlist = new ArrayList<>();
    private int currentIndex;
    private boolean controlsVisible = true;
    private boolean scrubbing;
    private int speedIndex;
    private int ratioIndex;
    private long lastSaveAt;

    private final Handler ui = new Handler(Looper.getMainLooper());

    private final Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) hideControls();
        }
    };

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                updateProgress();
                ui.postDelayed(this, 200);
            } else {
                ui.postDelayed(this, 800);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);


        bindViews();
        if (!buildPlaylist()) {
            finish();
            return;
        }
        hideSystemBars();
        setupPlayer();
        setupControls();

        ui.post(ticker);
        showControls(true);

    }

    private void bindViews() {
        root = findViewById(R.id.root);
        playerView = findViewById(R.id.player_view);
        topBar = findViewById(R.id.top_bar);
        bottomBar = findViewById(R.id.bottom_bar);
        centerBtn = findViewById(R.id.center_btn);
        btnPlay = findViewById(R.id.btn_play);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        timeCurrent = findViewById(R.id.time_current);
        timeTotal = findViewById(R.id.time_total);
        btnSpeed = findViewById(R.id.btn_speed);
        btnRatio = findViewById(R.id.btn_ratio);
        btnNext = findViewById(R.id.btn_next);
        btnEpisodes = findViewById(R.id.btn_episodes);
        hud = findViewById(R.id.hud);
        seek = findViewById(R.id.seek);
        buffering = findViewById(R.id.buffering);
    }

    private boolean buildPlaylist() {
        String id = getIntent().getStringExtra("id");
        MediaEntry entry = LibraryRepository.get().findById(id);
        if (entry == null) return false;
        int start = getIntent().getIntExtra("episode", 0);
        if (entry.series && !entry.episodes.isEmpty()) {
            playlist.addAll(entry.episodes);
            currentIndex = Math.max(0, Math.min(start, playlist.size() - 1));
        } else {
            playlist.add(entry);
            currentIndex = 0;
        }
        return true;
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());
        c.hide(WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        player.setMediaItems(mediaItems(), currentIndex, startPositionMs());
        player.setPlayWhenReady(true);
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayIcon();
                if (isPlaying) {
                    scheduleHide();
                } else {
                    ui.removeCallbacks(hideRunnable);
                    showControls(false);
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                buffering.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
                if (state == Player.STATE_ENDED) {
                    showControls(false);
                }
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                currentIndex = player.getCurrentMediaItemIndex();
                updateTitle();
                updateNextButton();
            }
        });
        player.prepare();
        updateTitle();
        updateNextButton();
    }

    private List<MediaItem> mediaItems() {
        List<MediaItem> items = new ArrayList<>();
        for (MediaEntry e : playlist) {
            items.add(new MediaItem.Builder()
                    .setUri(Uri.fromFile(new File(e.videoPath)))
                    .setMediaId(e.id)
                    .build());
        }
        return items;
    }

    /** 语音助手点播会带 startMs，优先级高于断点续播。 */
    private long startPositionMs() {
        MediaEntry startEntry = playlist.get(currentIndex);
        long forced = getIntent().getLongExtra("startMs", -1);
        long startPos = forced >= 0 ? forced
                : (Prefs.rememberProgress(this) && !startEntry.series
                ? Prefs.position(this, startEntry.id) : 0);
        if (startEntry.series && forced < 0) startPos = 0;
        return Math.max(0, startPos);
    }

    /** 语音助手点播新片源：复用当前播放器实例，不重建页面。 */
    private void switchTo(Intent intent) {
        savePosition();
        setIntent(intent);
        playlist.clear();
        if (!buildPlaylist()) {
            finish();
            return;
        }
        hideSystemBars();
        player.stop();
        player.clearMediaItems();
        player.setMediaItems(mediaItems(), currentIndex, startPositionMs());
        player.prepare();
        player.setPlayWhenReady(true);
        updateTitle();
        updateNextButton();
        showControls(true);
    }

    private void setupControls() {
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        View.OnClickListener playPause = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.d("LimePlayer", "click play id=" + v.getId());
                togglePlay();
            }
        };
        btnPlay.setOnClickListener(playPause);
        centerBtn.setOnClickListener(playPause);

        findViewById(R.id.btn_rewind).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekBy(-10000);
            }
        });
        findViewById(R.id.btn_forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekBy(10000);
            }
        });

        btnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] labels = new String[SPEEDS.length];
                int checked = 0;
                for (int i = 0; i < SPEEDS.length; i++) {
                    labels[i] = formatSpeed(SPEEDS[i]) + (SPEEDS[i] == 1f ? "（正常）" : "");
                    if (SPEEDS[i] == SPEEDS[speedIndex]) checked = i;
                }
                new AlertDialog.Builder(PlayerActivity.this)
                        .setTitle("播放速度")
                        .setSingleChoiceItems(labels, checked, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                applySpeed(SPEEDS[which]);
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        btnRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ratioIndex = (ratioIndex + 1) % RATIO_LABELS.length;
                playerView.setResizeMode(ratioIndex == 0
                        ? AspectRatioFrameLayout.RESIZE_MODE_FIT
                        : ratioIndex == 1
                        ? AspectRatioFrameLayout.RESIZE_MODE_FILL
                        : AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                btnRatio.setText(RATIO_LABELS[ratioIndex]);
                showHud("画面 " + RATIO_LABELS[ratioIndex]);
                scheduleHide();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem();
                    player.play();
                }
                scheduleHide();
            }
        });

        btnEpisodes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEpisodesDialog();
            }
        });

        findViewById(R.id.btn_fullscreen).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSystemBars();
                showHud("已全屏");
            }
        });

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    timeCurrent.setText(Ui.time(progress * 1000L));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                scrubbing = true;
                ui.removeCallbacks(hideRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                scrubbing = false;
                player.seekTo(bar.getProgress() * 1000L);
                scheduleHide();
            }
        });

        final GestureDetector detector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        if (controlsVisible) hideControls();
                        else showControls(true);
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (e.getX() < root.getWidth() / 2f) seekBy(-10000);
                        else seekBy(10000);
                        return true;
                    }
                });

        View touchLayer = findViewById(R.id.touch_layer);
        touchLayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detector.onTouchEvent(event);
                return true;
            }
        });
    }

    private void showEpisodesDialog() {
        if (playlist.size() <= 1) return;
        final String[] labels = new String[playlist.size()];
        for (int i = 0; i < playlist.size(); i++) {
            MediaEntry e = playlist.get(i);
            labels[i] = (e.subtitleExtra != null && !e.subtitleExtra.isEmpty())
                    ? e.subtitleExtra : ("第" + Math.max(1, e.episode) + "集");
        }
        new AlertDialog.Builder(this)
                .setTitle("选集")
                .setSingleChoiceItems(labels, currentIndex, (d, which) -> {
                    d.dismiss();
                    if (which != currentIndex) {
                        player.seekTo(which, 0);
                        player.play();
                        showControls(true);
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void togglePlay() {
        if (player == null) return;
        if (player.getPlaybackState() == Player.STATE_ENDED) {
            player.seekTo(0);
            player.play();
        } else if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
        }
        updatePlayIcon();
        scheduleHide();
    }

    private void seekBy(long deltaMs) {
        if (player == null) return;
        long pos = player.getCurrentPosition() + deltaMs;
        long dur = player.getDuration();
        if (dur > 0) pos = Math.min(pos, dur - 1000);
        pos = Math.max(0, pos);
        player.seekTo(pos);
        showHud((deltaMs > 0 ? "+" : "-") + Math.abs(deltaMs) / 1000 + " 秒");
        showControls(false);
        scheduleHide();
    }

    private void updateProgress() {
        if (player == null) return;
        long dur = player.getDuration();
        long pos = player.getCurrentPosition();
        if (dur > 0) {
            int max = (int) (dur / 1000);
            if (!scrubbing) {
                seek.setMax(max);
                seek.setProgress((int) (pos / 1000));
                seek.setSecondaryProgress(
                        Math.max(seek.getProgress(), player.getBufferedPercentage() * max / 100));
                timeCurrent.setText(Ui.time(pos));
            }
            timeTotal.setText(Ui.time(dur));
        }
        long now = System.currentTimeMillis();
        if (now - lastSaveAt > 5000) {
            lastSaveAt = now;
            savePosition();
        }
    }

    private void savePosition() {
        if (player == null || currentIndex < 0 || currentIndex >= playlist.size()) return;
        if (!Prefs.rememberProgress(this)) return;
        MediaEntry e = playlist.get(currentIndex);
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (pos > 3000 && dur > 0 && pos < dur - 5000) {
            Prefs.setPosition(this, e.id, pos);
            Prefs.markPlayed(this, e.id, e.videoPath,
                    playlist.size() > 1 ? e.title + " 第" + Math.max(1, e.episode) + "集" : e.title);
        }
    }

    private void updateTitle() {
        if (playlist.isEmpty()) return;
        MediaEntry e = playlist.get(Math.max(0, Math.min(currentIndex, playlist.size() - 1)));
        if (playlist.size() > 1) {
            String extra = (e.subtitleExtra != null && !e.subtitleExtra.isEmpty())
                    ? " · " + e.subtitleExtra : (" · 第" + Math.max(1, e.episode) + "集");
            title.setText(e.title + extra);
        } else {
            title.setText(e.title);
        }
        btnEpisodes.setVisibility(playlist.size() > 1 ? View.VISIBLE : View.GONE);
    }

    private void updateNextButton() {
        btnNext.setVisibility(playlist.size() > 1 && currentIndex < playlist.size() - 1
                ? View.VISIBLE : View.GONE);
    }

    private void updatePlayIcon() {
        boolean playing = player != null && player.isPlaying();
        btnPlay.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        centerBtn.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        centerBtn.setVisibility(playing || !controlsVisible ? View.GONE : View.VISIBLE);
    }

    private void showControls(boolean autoHide) {
        controlsVisible = true;
        android.util.Log.d("LimePlayer", "showControls autoHide=" + autoHide);
        topBar.animate().alpha(1f).setDuration(180).start();
        bottomBar.animate().alpha(1f).setDuration(180).start();
        topBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
        updatePlayIcon();
        if (player != null && !player.isPlaying()) centerBtn.setVisibility(View.VISIBLE);
        scheduleHide();
        if (!autoHide) ui.removeCallbacks(hideRunnable);
    }

    private void hideControls() {
        controlsVisible = false;
        android.util.Log.d("LimePlayer", "hideControls");
        ui.removeCallbacks(hideRunnable);
        topBar.animate().alpha(0f).setDuration(180).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (!controlsVisible) topBar.setVisibility(View.INVISIBLE);
            }
        }).start();
        bottomBar.animate().alpha(0f).setDuration(180).withEndAction(new Runnable() {
            @Override
            public void run() {
                if (!controlsVisible) bottomBar.setVisibility(View.INVISIBLE);
            }
        }).start();
        centerBtn.setVisibility(View.GONE);
    }

    private void scheduleHide() {
        ui.removeCallbacks(hideRunnable);
        if (player != null && player.isPlaying()) {
            ui.postDelayed(hideRunnable, AUTO_HIDE_MS);
        }
    }

    private void showHud(String text) {
        hud.setText(text);
        hud.animate().cancel();
        hud.setAlpha(0f);
        hud.animate().alpha(1f).setDuration(120).withEndAction(new Runnable() {
            @Override
            public void run() {
                hud.animate().alpha(0f).setStartDelay(700).setDuration(250).start();
            }
        }).start();
    }

    private String formatSpeed(float speed) {
        if (speed == 1f) return "倍速";
        return String.format(Locale.US, "%sx", speed == Math.round(speed)
                ? String.valueOf((int) speed) : String.valueOf(speed));
    }

    /** 绑定保持到页面销毁：即使播放器在后台（被语音助手覆盖），指令依然能落到这个会话。 */
    @Override
    protected void onStart() {
        super.onStart();
        PlaybackCommandBus.register(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (player != null) {
            switchTo(intent);
        } else {
            setIntent(intent);
            recreate();
        }
    }

    /** 语音助手：跳过片头/片尾/广告/前情回顾。 */
    @Override
    public void onSkip(int skipType) {
        android.util.Log.d("VoiceBus", "PlayerActivity.onSkip=" + skipType
                + " player=" + (player != null)
                + " pos=" + (player != null ? player.getCurrentPosition() : -1));
        if (player == null) return;
        switch (skipType) {
            case SkipType.SKIP_OPENING:
                player.seekTo(0);
                toast("已跳过片头");
                break;
            case SkipType.SKIP_ENDING:
                if (currentIndex + 1 < playlist.size()) {
                    player.seekToNextMediaItem();
                    toast("已跳过片尾，播放下一集");
                } else if (player.getDuration() > 0) {
                    player.seekTo(player.getDuration());
                    toast("已跳过片尾");
                } else {
                    toast("已到片尾");
                }
                break;
            case SkipType.SKIP_AD:
                toast("当前片源没有广告");
                break;
            case SkipType.SKIP_RECAP:
                toast("当前片源没有前情回顾");
                break;
            default:
                toast("未知的跳过类型：" + skipType);
                break;
        }
    }

    /** 语音助手：只看某演员（当前片源无演员时间轴，先连通接口）。 */
    @Override
    public void onFocusActor(String actorId) {
        String name = actorId == null ? "" : actorId;
        boolean known = false;
        MediaEntry cur = currentEntry();
        if (cur != null) {
            for (String a : cur.actors) {
                if (a.equals(name) || a.contains(name)) {
                    known = true;
                    break;
                }
            }
        }
        toast(known
                ? "收到「只看 " + name + "」：缺少演员时间轴，暂不跳转"
                : "收到「只看 " + name + "」：片源未标注该演员");
    }

    /** 语音助手：暂停。 */
    @Override
    public void onVoicePause() {
        android.util.Log.d("VoiceBus", "PlayerActivity.onVoicePause player=" + (player != null)
                + " playing=" + (player != null && player.isPlaying()));
        if (player == null) return;
        if (!player.isPlaying()) {
            toast("当前已是暂停状态");
        } else {
            player.pause();
            toast("已暂停");
        }
        android.util.Log.d("VoiceBus", "after pause: isPlaying=" + player.isPlaying()
                + " pos=" + player.getCurrentPosition());
        showControls(true);
    }

    /** 语音助手：继续播放；已播完则从头开始。 */
    @Override
    public void onVoiceResume() {
        android.util.Log.d("VoiceBus", "PlayerActivity.onVoiceResume player=" + (player != null)
                + " state=" + (player != null ? player.getPlaybackState() : -1));
        if (player == null) return;
        if (player.getPlaybackState() == Player.STATE_ENDED) {
            player.seekTo(0);
        }
        if (player.isPlaying()) {
            toast("正在播放中");
        } else {
            player.play();
            toast("继续播放");
        }
        final ExoPlayer p = player;
        ui.postDelayed(new Runnable() {
            @Override
            public void run() {
                android.util.Log.d("VoiceBus", "after resume: isPlaying=" + p.isPlaying()
                        + " pos=" + p.getCurrentPosition() + " state=" + p.getPlaybackState());
            }
        }, 600);
        showControls(true);
    }

    /** 语音助手：设置倍速（0.25x ~ 3x，保留两位小数）。 */
    @Override
    public void onVoiceSetSpeed(float speed) {
        android.util.Log.d("VoiceBus", "PlayerActivity.onVoiceSetSpeed speed=" + speed
                + " player=" + (player != null));
        if (player == null) return;
        float applied = applySpeed(speed);
        android.util.Log.d("VoiceBus", "onVoiceSetSpeed applied=" + applied
                + " actual=" + player.getPlaybackParameters().speed
                + " playing=" + player.isPlaying());
        if (applied > 0f) toast("播放速度 " + speedLabel(applied));
    }

    /** 统一改速入口：按钮弹窗与语音指令共用，保证按钮文案与 HUD 同步。返回实际生效的倍速。 */
    private float applySpeed(float speed) {
        if (player == null) return 0f;
        float s = Math.max(0.25f, Math.min(3f, speed));
        s = Math.round(s * 100) / 100f;
        player.setPlaybackSpeed(s);
        for (int i = 0; i < SPEEDS.length; i++) {
            if (Math.abs(SPEEDS[i] - s) < 0.001f) {
                speedIndex = i;
                break;
            }
        }
        btnSpeed.setText(formatSpeed(s));
        showHud(speedLabel(s) + " 播放");
        return s;
    }

    /** 倍速文案：1 -> "1x"，1.25 -> "1.25x"（formatSpeed 在 1x 时返回按钮用的「倍速」）。 */
    private String speedLabel(float s) {
        String num = s == Math.round(s) ? String.valueOf((int) s) : String.valueOf(s);
        return num + "x";
    }

    private MediaEntry currentEntry() {
        if (playlist.isEmpty()) return null;
        int i = Math.max(0, Math.min(currentIndex, playlist.size() - 1));
        return playlist.get(i);
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePosition();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {

        PlaybackCommandBus.unregister(this);
        ui.removeCallbacksAndMessages(null);
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}

