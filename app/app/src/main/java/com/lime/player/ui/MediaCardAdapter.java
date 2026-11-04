package com.lime.player.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lime.player.R;
import com.lime.player.data.MediaEntry;
import com.lime.player.util.ImageLoader;
import com.lime.player.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页卡片适配器，两种形态：
 * - 竖版海报卡（5 列，对应爱奇艺首页第一排榜单卡）
 * - 横向整行卡（一行 4 张 16:9 横图，对应爱奇艺首页第二排推荐模块）
 */
public class MediaCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CARD = 0;
    private static final int TYPE_WIDE_ROW = 1;

    public interface OnItemClick {
        void onClick(MediaEntry entry);
    }

    public interface OnItemLongClick {
        void onLongClick(MediaEntry entry);
    }

    private final List<MediaEntry> data = new ArrayList<>();
    private final List<MediaEntry> wides = new ArrayList<>();
    private final OnItemClick listener;
    private OnItemLongClick longClickListener;
    private boolean showRank;
    private boolean showSub;
    private int cardWidth;
    private int contentWidth;
    private int gapPx;
    private final float radius;

    public MediaCardAdapter(OnItemClick listener, float radiusPx) {
        this.listener = listener;
        this.radius = radiusPx;
    }

    public void setMetrics(int cardWidthPx, int contentWidthPx, int gapPx) {
        this.cardWidth = cardWidthPx;
        this.contentWidth = contentWidthPx;
        this.gapPx = gapPx;
    }

    public void setOnItemLongClick(OnItemLongClick l) {
        this.longClickListener = l;
    }

    public void setData(List<MediaEntry> cards, List<MediaEntry> wideCards,
                        boolean showRank, boolean showSub) {
        data.clear();
        wides.clear();
        if (cards != null) data.addAll(cards);
        if (wideCards != null) wides.addAll(wideCards);
        this.showRank = showRank;
        this.showSub = showSub;
        notifyDataSetChanged();
    }

    public boolean hasWideRow() {
        return !wides.isEmpty();
    }

    @Override
    public int getItemViewType(int position) {
        return (hasWideRow() && position == data.size()) ? TYPE_WIDE_ROW : TYPE_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_WIDE_ROW) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new WideRowHolder(row);
        }
        View v = inflater.inflate(R.layout.item_media_card, parent, false);
        return new CardHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof WideRowHolder) {
            bindWideRow((WideRowHolder) holder);
        } else {
            bindCard((CardHolder) holder, position);
        }
    }

    private void bindCard(CardHolder h, int index) {
        final MediaEntry e = data.get(index);
        h.title.setText(e.title);

        ViewGroup.LayoutParams lp = h.posterBox.getLayoutParams();
        lp.height = cardWidth * 458 / 344;
        h.posterBox.setLayoutParams(lp);

        if (cardWidth > 0) {
            ImageLoader.load(h.poster, e.posterPath, cardWidth, radius);
        }

        if (showRank) {
            h.rank.setVisibility(View.VISIBLE);
            h.rank.setText(String.valueOf(index + 1));
        } else {
            h.rank.setVisibility(View.GONE);
        }

        String tag = e.tagText();
        if (tag == null || tag.isEmpty()) {
            h.tag.setVisibility(View.GONE);
        } else {
            h.tag.setVisibility(View.VISIBLE);
            h.tag.setText(tag);
        }

        if (showSub) {
            h.subtitle.setVisibility(View.VISIBLE);
            h.subtitle.setText(e.subtitle());
        } else {
            h.subtitle.setVisibility(View.GONE);
        }

        bindClick(h.itemView, e);
    }

    private void bindWideRow(WideRowHolder h) {
        LinearLayout row = (LinearLayout) h.itemView;
        row.removeAllViews();
        int n = Math.min(4, wides.size());
        int w = contentWidth > 0 && gapPx > 0 ? (contentWidth - 3 * gapPx) / 4 : 0;
        for (int i = 0; i < n; i++) {
            final MediaEntry e = wides.get(i);
            View item = LayoutInflater.from(row.getContext())
                    .inflate(R.layout.item_wide_card, row, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    w > 0 ? w : ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.setMarginStart(gapPx);
            item.setLayoutParams(lp);
            row.addView(item);

            FrameLayout box = item.findViewById(R.id.wide_box);
            ViewGroup.LayoutParams blp = box.getLayoutParams();
            blp.height = (w > 0 ? w : 400) * 255 / 444;
            box.setLayoutParams(blp);

            ImageView poster = item.findViewById(R.id.wide_poster);
            if (w > 0) ImageLoader.load(poster, e.posterPath, w, radius);

            ((TextView) item.findViewById(R.id.wide_title)).setText(e.title);
            TextView sub = item.findViewById(R.id.wide_sub);
            String s = e.genreText();
            sub.setText(s.isEmpty() ? e.subtitle() : s);

            bindClick(item, e);
        }
    }

    private void bindClick(View v, final MediaEntry e) {
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null) listener.onClick(e);
            }
        });
        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (longClickListener != null) {
                    longClickListener.onLongClick(e);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size() + (hasWideRow() ? 1 : 0);
    }

    static class CardHolder extends RecyclerView.ViewHolder {
        final FrameLayout posterBox;
        final ImageView poster;
        final TextView rank;
        final TextView tag;
        final TextView title;
        final TextView subtitle;

        CardHolder(View v) {
            super(v);
            posterBox = v.findViewById(R.id.poster_box);
            poster = v.findViewById(R.id.poster);
            rank = v.findViewById(R.id.rank);
            tag = v.findViewById(R.id.tag);
            title = v.findViewById(R.id.title);
            subtitle = v.findViewById(R.id.subtitle);
        }
    }

    static class WideRowHolder extends RecyclerView.ViewHolder {
        WideRowHolder(View v) {
            super(v);
            Ui.dp(v.getContext(), 0);
        }
    }
}
