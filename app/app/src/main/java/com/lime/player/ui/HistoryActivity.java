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

/** 观看历史页（对齐爱奇艺 PlayerRecordActivity 的结构：居中标题 + 右上编辑 + 四列卡片）。 */
public class HistoryActivity extends AppCompatActivity {

    private static final int SPAN = 4;

    private RecyclerView list;
    private View empty;
    private TextView edit;
    private TextView clearAll;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

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

        GridLayoutManager glm = new GridLayoutManager(this, SPAN);
        list.setLayoutManager(glm);
        adapter = new HistoryAdapter(new HistoryAdapter.Callback() {
            @Override
            public void onOpen(MediaEntry entry) {
                Intent i = new Intent(HistoryActivity.this, PlayerActivity.class);
                i.putExtra("id", entry.id);
                startActivity(i);
            }

            @Override
            public void onDelete(MediaEntry entry) {
                confirmDelete(entry);
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
                outRect.bottom = Ui.dp(HistoryActivity.this, 28);
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
        List<MediaEntry> entries = new ArrayList<>();
        List<Long> positions = new ArrayList<>();
        String history = Prefs.history(this);
        if (!history.trim().isEmpty()) {
            for (String line : history.split("\n")) {
                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;
                MediaEntry e = repo.findExact(parts[0]);
                if (e == null) continue;
                entries.add(e);
                positions.add(Prefs.position(this, e.id));
            }
        }
        adapter.submit(entries, positions);
        empty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        if (entries.isEmpty()) setEditMode(false);
        clearAll.setVisibility(adapter.isEditMode() && !entries.isEmpty()
                ? View.VISIBLE : View.GONE);
    }

    private void confirmDelete(final MediaEntry entry) {
        new AlertDialog.Builder(this)
                .setMessage("删除「" + entry.title + "」的观看记录？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> {
                    Prefs.removeHistory(this, entry.id);
                    Prefs.clearPosition(this, entry.id);
                    reload();
                })
                .show();
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setMessage("清空全部观看记录？")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", (d, w) -> {
                    Prefs.clearHistory(this);
                    Prefs.clearAllPositions(this);
                    setEditMode(false);
                    reload();
                })
                .show();
    }
}
