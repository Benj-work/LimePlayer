package com.lime.player.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lime.player.R;
import com.lime.player.data.LibraryRepository;
import com.lime.player.data.MediaEntry;
import com.lime.player.util.Prefs;
import com.lime.player.util.Ui;

import java.util.ArrayList;
import java.util.List;

/** 我的收藏页：沿用观看历史的页面框架（居中标题 + 右上编辑 + 四列卡片）。 */
public class FavoritesActivity extends AppCompatActivity {

    private static final int SPAN = 4;

    private RecyclerView list;
    private View empty;
    private TextView edit;
    private TextView clearAll;
    private FavoritesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ((TextView) findViewById(R.id.page_title)).setText("我的收藏");
        ((TextView) findViewById(R.id.empty_title)).setText("还没有收藏");
        ((TextView) findViewById(R.id.empty_hint)).setText("在片库长按海报即可收藏");

        list = findViewById(R.id.list);
        empty = findViewById(R.id.empty);
        edit = findViewById(R.id.edit);
        clearAll = findViewById(R.id.clear_all);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        list.setLayoutManager(new GridLayoutManager(this, SPAN));
        adapter = new FavoritesAdapter(new FavoritesAdapter.Callback() {
            @Override
            public void onOpen(MediaEntry entry) {
                Intent i = new Intent(FavoritesActivity.this, DetailActivity.class);
                i.putExtra("id", entry.id);
                startActivity(i);
            }

            @Override
            public void onRemove(MediaEntry entry) {
                confirmRemove(entry);
            }
        }, Ui.dp(this, 8));
        list.setAdapter(adapter);

        final int gap = Ui.dp(this, 16);
        list.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, View view,
                                       RecyclerView parent, RecyclerView.State state) {
                int pos = parent.getChildAdapterPosition(view);
                if (pos < 0) return;
                outRect.right = pos % SPAN == SPAN - 1 ? 0 : gap;
                outRect.bottom = Ui.dp(FavoritesActivity.this, 28);
            }
        });

        list.post(new Runnable() {
            @Override
            public void run() {
                int width = list.getWidth() - list.getPaddingStart() - list.getPaddingEnd();
                int card = (width - gap * (SPAN - 1)) / SPAN;
                if (card > 0) adapter.setCardWidth(card);
            }
        });

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEditMode(!adapter.isEditMode());
            }
        });
        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClearAll();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void setEditMode(boolean on) {
        adapter.setEditMode(on);
        edit.setText(on ? "完成" : "编辑");
        clearAll.setVisibility(on && adapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void reload() {
        LibraryRepository repo = LibraryRepository.get();
        // 按片库顺序展示，避免 HashSet 造成的随机排序
        java.util.Set<String> favs = Prefs.favorites(this);
        List<MediaEntry> entries = new ArrayList<>();
        for (MediaEntry e : repo.entries()) {
            if (favs.contains(e.id)) entries.add(e);
        }
        adapter.submit(entries);
        empty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        if (entries.isEmpty()) setEditMode(false);
        clearAll.setVisibility(adapter.isEditMode() && !entries.isEmpty()
                ? View.VISIBLE : View.GONE);
    }

    private void confirmRemove(final MediaEntry entry) {
        new AlertDialog.Builder(this)
                .setMessage("取消收藏「" + entry.title + "」？")
                .setNegativeButton("取消", null)
                .setPositiveButton("取消收藏", (d, w) -> {
                    Prefs.removeFavorite(this, entry.id);
                    reload();
                })
                .show();
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setMessage("清空全部收藏？")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", (d, w) -> {
                    Prefs.clearFavorites(this);
                    setEditMode(false);
                    reload();
                })
                .show();
    }
}
