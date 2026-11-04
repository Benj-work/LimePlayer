package com.lime.player.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lime.player.R;
import com.lime.player.data.MediaEntry;

import java.util.ArrayList;
import java.util.List;

/** 选集 / 剧集横排。 */
public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.Holder> {

    public interface OnPick {
        void onPick(int index);
    }

    private final List<MediaEntry> data = new ArrayList<>();
    private final OnPick onPick;
    private int selected = -1;

    public EpisodeAdapter(List<MediaEntry> list, int selected, OnPick onPick) {
        if (list != null) data.addAll(list);
        this.selected = selected;
        this.onPick = onPick;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_episode, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder h, final int position) {
        final MediaEntry e = data.get(position);
        String label;
        if (e.episode > 0) {
            label = e.subtitleExtra != null && !e.subtitleExtra.isEmpty() && e.subtitleExtra.length() <= 6
                    ? e.subtitleExtra : ("第" + e.episode + "集");
        } else {
            label = String.valueOf(position + 1);
        }
        h.text.setText(label);
        h.text.setSelected(position == selected);
        h.text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selected = h.getAdapterPosition();
                notifyDataSetChanged();
                if (onPick != null) onPick.onPick(selected);
            }
        });
    }

    public void setSelected(int index) {
        selected = index;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView text;

        Holder(View v) {
            super(v);
            text = (TextView) v;
        }
    }
}
