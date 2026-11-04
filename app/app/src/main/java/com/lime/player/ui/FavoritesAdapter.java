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

/** 我的收藏：与观看历史同一套卡片，只显示片名与「类型 · 规格」，不显示进度。 */
public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.Holder> {

    public interface Callback {
        void onOpen(MediaEntry entry);

        void onRemove(MediaEntry entry);
    }

    private final List<MediaEntry> items = new ArrayList<>();
    private final Callback callback;
    private final float radius;
    private boolean editMode;
    private int cardWidth;

    public FavoritesAdapter(Callback callback, float radiusPx) {
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

    public void submit(List<MediaEntry> entries) {
        items.clear();
        items.addAll(entries);
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

        ViewGroup.LayoutParams lp = h.thumbBox.getLayoutParams();
        lp.height = cardWidth * 9 / 16;
        h.thumbBox.setLayoutParams(lp);

        if (cardWidth > 0) {
            String img = e.backdropPath != null && !e.backdropPath.isEmpty()
                    ? e.backdropPath : e.posterPath;
            ImageLoader.load(h.thumb, img, cardWidth, radius);
        }

        h.barTrack.setVisibility(View.GONE);
        h.barFill.setVisibility(View.GONE);
        h.title.setText(e.title);
        h.sub.setText(subtitle(e));

        h.delete.setVisibility(editMode ? View.VISIBLE : View.GONE);
        h.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) callback.onRemove(e);
            }
        });
        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editMode) {
                    if (callback != null) callback.onRemove(e);
                } else if (callback != null) {
                    callback.onOpen(e);
                }
            }
        });
    }

    private String subtitle(MediaEntry e) {
        StringBuilder sb = new StringBuilder();
        String genre = e.genreText();
        if (!genre.isEmpty()) sb.append(genre);
        String tag = e.tagText();
        if (!tag.isEmpty()) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append(tag);
        }
        if (sb.length() == 0) sb.append(e.subtitle());
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
        final View barTrack;
        final View barFill;
        final TextView title;
        final TextView sub;

        Holder(View v) {
            super(v);
            thumbBox = v.findViewById(R.id.thumb_box);
            thumb = v.findViewById(R.id.thumb);
            delete = v.findViewById(R.id.delete);
            barTrack = v.findViewById(R.id.bar_track);
            barFill = v.findViewById(R.id.bar_fill);
            title = v.findViewById(R.id.title);
            sub = v.findViewById(R.id.sub);
        }
    }
}
