package com.lime.player.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lime.player.R;
import com.lime.player.data.LibraryRepository;
import com.lime.player.data.MediaEntry;
import com.lime.player.util.ImageLoader;
import com.lime.player.util.Prefs;
import com.lime.player.util.Ui;

import java.util.Locale;

/** 影片详情 / 选集。 */
public class DetailActivity extends AppCompatActivity {

    private MediaEntry entry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String id = getIntent().getStringExtra("id");
        entry = LibraryRepository.get().findById(id);
        if (entry == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_detail);

        ImageView backdrop = findViewById(R.id.backdrop);
        ImageView back = findViewById(R.id.back);
        TextView title = findViewById(R.id.title);
        TextView rating = findViewById(R.id.rating);
        TextView ratingUnit = findViewById(R.id.rating_unit);
        TextView meta = findViewById(R.id.meta);
        TextView plot = findViewById(R.id.plot);
        TextView cast = findViewById(R.id.cast);
        TextView play = findViewById(R.id.btn_play);
        TextView resume = findViewById(R.id.btn_resume);
        View episodeSection = findViewById(R.id.episode_section);
        RecyclerView episodes = findViewById(R.id.episodes);

        // 有横版剧照就用剧照，没有则回落到竖版海报
        String bg = entry.backdropPath != null && !entry.backdropPath.isEmpty()
                ? entry.backdropPath : entry.posterPath;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        layoutBackdrop(backdrop, findViewById(R.id.scrim), bg, screenW);
        ImageLoader.load(backdrop, bg, screenW, 0);

        title.setText(entry.title);
        if (entry.rating > 0) {
            rating.setVisibility(View.VISIBLE);
            rating.setText(String.format(Locale.US, "%.1f", entry.rating));
            ratingUnit.setVisibility(View.VISIBLE);
        } else {
            rating.setVisibility(View.GONE);
            ratingUnit.setVisibility(View.GONE);
            findViewById(R.id.meta_divider).setVisibility(View.GONE);
        }
        meta.setText(entry.metaText());
        plot.setText(entry.plot == null || entry.plot.isEmpty() ? "暂无简介" : entry.plot);

        StringBuilder sb = new StringBuilder();
        if (!entry.director.isEmpty()) sb.append("导演：").append(entry.director);
        if (!entry.actors.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("主演：");
            int max = Math.min(entry.actors.size(), 6);
            for (int i = 0; i < max; i++) {
                if (i > 0) sb.append(" / ");
                sb.append(entry.actors.get(i));
            }
        }
        cast.setText(sb.toString());
        cast.setVisibility(sb.length() == 0 ? View.GONE : View.VISIBLE);

        final int resumeIndex = bestEpisodeIndex();
        if (resumeIndex >= 0) {
            resume.setVisibility(View.VISIBLE);
            long pos = Prefs.position(this, episodeAt(resumeIndex).id);
            resume.setText("继续观看 " + Ui.time(pos));
            resume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    play(resumeIndex);
                }
            });
        }

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(resumeIndex >= 0 ? resumeIndex : 0);
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (entry.series && !entry.episodes.isEmpty()) {
            episodeSection.setVisibility(View.VISIBLE);
            episodes.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            episodes.setAdapter(new EpisodeAdapter(entry.episodes,
                    resumeIndex >= 0 ? resumeIndex : 0, new EpisodeAdapter.OnPick() {
                @Override
                public void onPick(int index) {
                    play(index);
                }
            }));
        }
    }

    private MediaEntry episodeAt(int index) {
        if (entry.series && index < entry.episodes.size()) return entry.episodes.get(index);
        return entry;
    }

    /** 返回最近看过的分集下标；没有进度返回 -1。 */
    private int bestEpisodeIndex() {
        if (!entry.series) {
            return Prefs.position(this, entry.id) > 5000 ? 0 : -1;
        }
        int best = -1;
        long bestPos = 5000;
        for (int i = 0; i < entry.episodes.size(); i++) {
            long p = Prefs.position(this, entry.episodes.get(i).id);
            if (p > bestPos) {
                bestPos = p;
                best = i;
            }
        }
        return best;
    }

    /**
     * 按图片自身宽高比决定头图高度：至少盖住渐变遮罩（700dp），
     * 这样图片不会被上下裁掉，底边也不会落在遮罩半透明区里露出硬边。
     */
    private void layoutBackdrop(ImageView view, View scrim, String path, int screenW) {
        int scrimH = Ui.dp(this, 700);
        int h = scrimH;
        if (path != null && !path.isEmpty()) {
            try {
                android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(path, o);
                if (o.outWidth > 0 && o.outHeight > 0) {
                    int natural = (int) ((long) screenW * o.outHeight / o.outWidth);
                    int screenH = getResources().getDisplayMetrics().heightPixels;
                    // 下限盖住遮罩，上限不超过一屏（竖版海报回落时避免头图过高）
                    h = Math.min(Math.max(natural, scrimH), screenH);
                }
            } catch (Throwable ignored) {
            }
        }
        view.getLayoutParams().height = h;
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.requestLayout();
        if (scrim != null) {
            // 遮罩与头图等高，渐变末端正好落在图片底边，接缝不可见
            scrim.getLayoutParams().height = h;
            scrim.requestLayout();
        }
    }

    private void play(int index) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("id", entry.id);
        i.putExtra("episode", index);
        startActivity(i);
    }
}
