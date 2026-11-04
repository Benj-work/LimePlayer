package com.lime.player.service;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.huawei.hmt.playercontroller.BasePlayerService;
import com.huawei.hmt.playercontroller.SkipType;
import com.lime.player.data.LibraryRepository;
import com.lime.player.data.MediaEntry;
import com.lime.player.ui.PlayerActivity;

/**
 * AIDL 服务端：接收语音助手的播放控制指令。
 *
 * <p>SDK 的 oneway 方法在 Binder 线程回调，这里统一切到主线程/工作线程再操作播放器与 UI。
 * 语音助手侧目前的 mediaId 是占位值（测试 App 传 media-A），因此解析失败时回退到片库第一部，
 * 保证「接口调通 + 能拉起播放」；后续接入真实媒体 id 时只需替换 {@link #resolve} 的匹配规则。
 */
public class VoicePlayerService extends BasePlayerService {

    private static final String TAG = "VoicePlayerService";

    private final Handler main = new Handler(Looper.getMainLooper());

    @Override
    protected void onPlay(String mediaId, final long positionMs) {
        final String query = mediaId == null ? "" : mediaId.trim();
        Log.d(TAG, "onPlay mediaId=" + query + " positionMs=" + positionMs);
        new Thread(new Runnable() {
            @Override
            public void run() {
                LibraryRepository repo = LibraryRepository.get();
                if (!repo.isScanned()) repo.scan(getApplicationContext());
                MediaEntry matched = resolve(repo, query);
                boolean fallback = false;
                if (matched == null && !repo.entries().isEmpty()) {
                    matched = repo.entries().get(0);
                    fallback = true;
                }
                final MediaEntry entry = matched;
                final boolean fb = fallback;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (entry == null) {
                            toast("语音点播失败：片库为空");
                            return;
                        }
                        startPlayback(entry, Math.max(0, positionMs), query, fb);
                    }
                });
            }
        }, "voice-play").start();
    }

    @Override
    protected void onSkip(int skipType) {
        Log.d(TAG, "onSkip type=" + skipType);
        PlaybackCommandBus.dispatchSkip(skipType);
        if (!PlaybackCommandBus.hasSink()) warnNoPlayer();

    }

    @Override
    protected void onFocusActor(String actorId) {
        Log.d(TAG, "onFocusActor actorId=" + actorId);
        PlaybackCommandBus.dispatchFocusActor(actorId);
        if (!PlaybackCommandBus.hasSink()) warnNoPlayer();

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume (voice)");
        PlaybackCommandBus.dispatchResume();
        if (!PlaybackCommandBus.hasSink()) warnNoPlayer();

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause (voice)");
        PlaybackCommandBus.dispatchPause();
        if (!PlaybackCommandBus.hasSink()) warnNoPlayer();

    }

    @Override
    protected void onSetSpeed(final float speed) {
        Log.d(TAG, "onSetSpeed speed=" + speed);
        if (speed <= 0f) {
            toastOnMain("倍速需大于 0");
            return;
        }
        PlaybackCommandBus.dispatchSpeed(speed);
        if (!PlaybackCommandBus.hasSink()) warnNoPlayer();

    }

    /** 支持：精确 id、文件路径、片名（含模糊）、文件名片段。 */
    private MediaEntry resolve(LibraryRepository repo, String query) {
        if (query.isEmpty()) return null;
        MediaEntry e = repo.findExact(query);
        if (e != null) return e;
        e = repo.findByPath(query);
        if (e != null) return e;
        for (MediaEntry item : repo.entries()) {
            if (item.title != null && item.title.equalsIgnoreCase(query)) return item;
        }
        for (MediaEntry item : repo.entries()) {
            if (item.title != null && (item.title.contains(query) || query.contains(item.title))) {
                return item;
            }
            for (MediaEntry ep : item.episodes) {
                if (ep.title != null && ep.title.contains(query)) return ep;
            }
        }
        for (MediaEntry item : repo.entries()) {
            if (item.videoPath != null && item.videoPath.contains(query)) return item;
            for (MediaEntry ep : item.episodes) {
                if (ep.videoPath != null && ep.videoPath.contains(query)) return ep;
            }
        }
        return null;
    }

    private void startPlayback(MediaEntry entry, long positionMs, String query, boolean fallback) {
        Intent intent = new Intent(this, PlayerActivity.class);
        if (entry.series && entry.episode > 0 && entry.id.startsWith("ep:")) {
            // 单集：定位到所属剧集的该集
            MediaEntry series = seriesOf(entry);
            intent.putExtra("id", series != null ? series.id : entry.id);
            intent.putExtra("episode", Math.max(0, entry.episode - 1));
        } else {
            intent.putExtra("id", entry.id);
        }
        intent.putExtra("startMs", positionMs);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startActivity(intent);
        } catch (Exception ex) {
            Log.e(TAG, "startActivity failed", ex);
            toast("拉起播放器失败：" + ex.getClass().getSimpleName());
            return;
        }
        String head = fallback
                ? "未匹配「" + query + "」，播放默认片源："
                : "语音点播：";
        toast(head + entry.title + format(positionMs));
    }

    private MediaEntry seriesOf(MediaEntry episode) {
        for (MediaEntry item : LibraryRepository.get().entries()) {
            for (MediaEntry ep : item.episodes) {
                if (ep.id.equals(episode.id)) return item;
            }
        }
        return null;
    }

    private String format(long ms) {
        if (ms <= 0) return "";
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return String.format(java.util.Locale.US, "（%s%02d:%02d）",
                h > 0 ? h + ":" : "", m, s);
    }

    /** 没有播放页时统一提示（服务端只回执，不做其它动作）。 */
    private void warnNoPlayer() {
        toastOnMain("当前没有正在播放的视频");
    }

    private void toastOnMain(final String msg) {
        main.post(new Runnable() {
            @Override
            public void run() {
                toast(msg);
            }
        });
    }

    private void toast(final String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /** 供日志/调试：当前跳过类型文案。 */
    static String skipName(int type) {
        switch (type) {
            case SkipType.SKIP_OPENING:
                return "片头";
            case SkipType.SKIP_ENDING:
                return "片尾";
            case SkipType.SKIP_AD:
                return "广告";
            case SkipType.SKIP_RECAP:
                return "前情回顾";
            default:
                return "未知(" + type + ")";
        }
    }
}
