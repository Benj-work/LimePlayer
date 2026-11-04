package com.lime.player.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lime.player.R;
import com.lime.player.data.MediaEntry;
import com.lime.player.util.ImageLoader;
import com.lime.player.util.Ui;

import java.util.ArrayList;
import java.util.List;

/** 观看历史：四列 16:9 卡片，封面底部带进度条。 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    public interface Callback {
        void onOpen(MediaEntry entry);

        void onDelete(MediaEntry entry);
    }

    private final List<MediaEntry> items = new ArrayList<>();
    private final List<Long> positions = new ArrayList<>();
    private final Callback callback;
    private final float radius;
    private boolean editMode;
    private int cardWidth;

    public HistoryAdapter(Callback callback, float radiusPx) {
        this.callback = callback;
        this.radius = radiusPx;
    }

    public void setCardWidth(int px) {
        this.cardWidth = px;
        notifyDataSetChanged();
    }

    public void setEditMode(boolean on) {
        this.editMode = on;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void submit(List<MediaEntry> entries, List<Long> pos) {
        items.clear();
        positions.clear();
        items.addAll(entries);
        positions.addAll(pos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_card, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final MediaEntry e = items.get(position);
        final long pos = positions.get(position);

        ViewGroup.LayoutParams lp = h.thumbBox.getLayoutParams();
        lp.height = cardWidth * 9 / 16;
        h.thumbBox.setLayoutParams(lp);

        if (cardWidth > 0) {
            String img = e.backdropPath != null && !e.backdropPath.isEmpty()
                    ? e.backdropPath : e.posterPath;
            ImageLoader.load(h.thumb, img, cardWidth, radius);
        }

        h.title.setText(e.title);
        h.sub.setText(subtitle(e, pos));

        float ratio = 0f;
        if (e.durationMs > 0) ratio = Math.min(1f, pos / (float) e.durationMs);
        int fillW = cardWidth > 0 ? (int) (cardWidth * ratio) : 0;
        ViewGroup.LayoutParams blp = h.barFill.getLayoutParams();
        blp.width = fillW;
        h.barFill.setLayoutParams(blp);

        h.delete.setVisibility(editMode ? View.VISIBLE : View.GONE);
        h.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.onDelete(e);
            }
        });
        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editMode) {
                    if (callback != null) callback.onDelete(e);
                } else if (callback != null) {
                    callback.onOpen(e);
                }
            }
        });
    }

    private String subtitle(MediaEntry e, long pos) {
        StringBuilder sb = new StringBuilder();
        if (e.series && e.episode > 0) {
            sb.append("第").append(e.episode).append("集");
        }
        String progress;
        if (e.durationMs > 0) {
            int pct = (int) Math.round(pos * 100.0 / e.durationMs);
            if (pct >= 99) {
                progress = "已看完";
            } else if (pct <= 0) {
                progress = "观看不足1%";
            } else {
                progress = "已看 " + pct + "%";
            }
        } else {
            progress = pos > 0 ? "已看 " + Ui.time(pos) : "观看不足1%";
        }
        if (sb.length() > 0) sb.append("  ·  ");
        sb.append(progress);
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final FrameLayout thumbBox;
        final ImageView thumb;
        final ImageView delete;
        final View barFill;
        final TextView title;
        final TextView sub;

        Holder(View v) {
            super(v);
            thumbBox = v.findViewById(R.id.thumb_box);
            thumb = v.findViewById(R.id.thumb);
            delete = v.findViewById(R.id.delete);
            barFill = v.findViewById(R.id.bar_fill);
            title = v.findViewById(R.id.title);
            sub = v.findViewById(R.id.sub);
        }
    }
}
