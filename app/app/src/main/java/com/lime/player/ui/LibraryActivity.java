package com.lime.player.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lime.player.R;
import com.lime.player.data.LibraryRepository;
import com.lime.player.data.MediaEntry;
import com.lime.player.util.ImageLoader;
import com.lime.player.util.Prefs;
import com.lime.player.util.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 首页：品牌栏 + 频道 + 海报墙。 */
public class LibraryActivity extends AppCompatActivity {

    private static final int REQ_STORAGE = 1001;
    private static final String[] TAB_LABELS = {"推荐", "电影", "剧集", "纪录片", "全部"};
    private static final String[] TAB_KEYS = {"recommend", "movie", "series", "doc", "all"};
    private static final int SPAN = 5;

    private RecyclerView grid;
    private MediaCardAdapter adapter;
    private LinearLayout tabContainer;
    private LinearLayout emptyView;
    private TextView emptyTitle;
    private TextView emptyDesc;
    private TextView emptyAction;

    private View searchBar;
    private EditText searchInput;
    private View searchClear;
    private boolean searchOpen;

    private int tabIndex = 0;
    private int sortMode = 0;
    private String query = "";
    private boolean loading;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        grid = findViewById(R.id.grid);
        tabContainer = findViewById(R.id.tab_container);
        emptyView = findViewById(R.id.empty_view);
        emptyTitle = findViewById(R.id.empty_title);
        emptyDesc = findViewById(R.id.empty_desc);
        emptyAction = findViewById(R.id.empty_action);

        setupGrid();
        setupNav();
        setupSearchBar();
        buildTabs();

        emptyAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasStorageAccess()) {
                    requestStorage();
                } else {
                    loadLibrary(true);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLibrary(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    // ---------------- 顶部频道 ----------------

    private void buildTabs() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < TAB_LABELS.length; i++) {
            final int index = i;
            View item = inflater.inflate(R.layout.item_tab, tabContainer, false);
            TextView text = item.findViewById(R.id.tab_text);
            text.setText(TAB_LABELS[i]);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectTab(index);
                }
            });
            tabContainer.addView(item);
        }
        selectTab(0);
    }

    private void selectTab(int index) {
        tabIndex = index;
        for (int i = 0; i < tabContainer.getChildCount(); i++) {
            View item = tabContainer.getChildAt(i);
            TextView text = item.findViewById(R.id.tab_text);
            View line = item.findViewById(R.id.tab_line);
            boolean on = i == index;
            text.setTextColor(ContextCompat.getColor(this,
                    on ? R.color.brand : R.color.text_secondary));
            text.setTextSize(on ? 31 : 29);
            text.getPaint().setFakeBoldText(on);
            line.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
        }
        applyFilter();
    }

    // ---------------- 海报墙 ----------------

    private void setupGrid() {
        final GridLayoutManager glm = new GridLayoutManager(this, SPAN);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter != null && adapter.hasWideRow()
                        && position == adapter.getItemCount() - 1) return SPAN;
                return 1;
            }
        });
        grid.setLayoutManager(glm);
        adapter = new MediaCardAdapter(new MediaCardAdapter.OnItemClick() {
            @Override
            public void onClick(MediaEntry entry) {
                openDetail(entry);
            }
        }, Ui.dp(this, 8));
        adapter.setOnItemLongClick(new MediaCardAdapter.OnItemLongClick() {
            @Override
            public void onLongClick(MediaEntry entry) {
                Prefs.toggleFavorite(LibraryActivity.this, entry.id);
                boolean fav = Prefs.isFavorite(LibraryActivity.this, entry.id);
                Toast.makeText(LibraryActivity.this,
                        (fav ? "已收藏 " : "已取消收藏 ") + entry.title,
                        Toast.LENGTH_SHORT).show();
            }
        });
        grid.setAdapter(adapter);

        final int gap = Ui.dp(this, 21);
        grid.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, View view,
                                       RecyclerView parent, RecyclerView.State state) {
                int pos = parent.getChildAdapterPosition(view);
                if (pos < 0) return;
                if (adapter != null && adapter.hasWideRow() && pos == adapter.getItemCount() - 1) {
                    outRect.bottom = Ui.dp(LibraryActivity.this, 18);
                    return;
                }
                int col = pos % SPAN;
                outRect.right = col == SPAN - 1 ? 0 : gap;
                outRect.bottom = Ui.dp(LibraryActivity.this, 26);
            }
        });

        grid.post(new Runnable() {
            @Override
            public void run() {
                int width = grid.getWidth() - grid.getPaddingStart() - grid.getPaddingEnd();
                int card = (width - gap * (SPAN - 1)) / SPAN;
                if (card > 0) adapter.setMetrics(card, width, gap);
            }
        });
    }

    private void openDetail(MediaEntry entry) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra("id", entry.id);
        startActivity(i);
    }

    // ---------------- 数据 ----------------

    private boolean hasStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                } catch (Exception ignored) {
                }
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
        }
    }

    private void loadLibrary(boolean force) {
        if (!hasStorageAccess()) {
            showPermissionState();
            return;
        }
        LibraryRepository repo = LibraryRepository.get();
        if (repo.isScanned() && !force) {
            applyFilter();
            return;
        }
        if (loading) return;
        loading = true;
        emptyView.setVisibility(View.GONE);
        io.execute(new Runnable() {
            @Override
            public void run() {
                LibraryRepository.get().scan(LibraryActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading = false;
                        applyFilter();
                    }
                });
            }
        });
    }

    private void showPermissionState() {
        emptyView.setVisibility(View.VISIBLE);
        emptyTitle.setText("需要访问片库");
        emptyDesc.setText("请允许「" + getString(R.string.app_name)
                + "」访问设备存储，才能读取 /sdcard/ExoPlayerLibrary/ 下的影片。");
        emptyAction.setText("授予访问权限");
        adapter.setData(new ArrayList<MediaEntry>(), new ArrayList<MediaEntry>(), false, false);
    }

    private void applyFilter() {
        if (adapter == null) return;
        LibraryRepository repo = LibraryRepository.get();
        List<MediaEntry> list = repo.byCategory(TAB_KEYS[tabIndex]);
        if (!query.isEmpty()) {
            List<MediaEntry> filtered = new ArrayList<>();
            String q = query.toLowerCase();
            for (MediaEntry e : list) {
                if (e.title.toLowerCase().contains(q)) filtered.add(e);
            }
            list = filtered;
        }
        list = sortList(list);

        // 推荐页：前 5 部进榜单（第一排竖版卡），其余进第二排横版推荐位
        if (tabIndex == 0 && list.size() > 5) {
            List<MediaEntry> cards = new ArrayList<>(list.subList(0, 5));
            List<MediaEntry> wides = new ArrayList<>(list.subList(5, list.size()));
            adapter.setData(cards, wides, true, false);
        } else {
            adapter.setData(list, new ArrayList<MediaEntry>(), false, true);
        }
        updateEmpty(list);
    }

    /** 排序方式：0 默认 1 评分 2 年份 3 名称 4 时长 */
    private List<MediaEntry> sortList(List<MediaEntry> src) {
        if (sortMode == 0) return src;
        List<MediaEntry> list = new ArrayList<>(src);
        Collections.sort(list, new Comparator<MediaEntry>() {
            @Override
            public int compare(MediaEntry a, MediaEntry b) {
                switch (sortMode) {
                    case 1:
                        return Float.compare(b.rating, a.rating);
                    case 2:
                        return Integer.compare(b.year, a.year);
                    case 3:
                        return a.title.compareTo(b.title);
                    case 4:
                        return Integer.compare(b.runtimeMin, a.runtimeMin);
                    default:
                        return 0;
                }
            }
        });
        return list;
    }

    private void showSortDialog() {
        final String[] labels = {"默认排序", "评分从高到低", "年份从新到旧", "名称", "时长从长到短"};
        new AlertDialog.Builder(this)
                .setTitle("筛选 / 排序")
                .setSingleChoiceItems(labels, sortMode, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        sortMode = which;
                        dialog.dismiss();
                        applyFilter();
                        grid.scrollToPosition(0);
                    }
                })
                .show();
    }

    private void updateEmpty(List<MediaEntry> list) {
        if (!hasStorageAccess()) {
            showPermissionState();
            return;
        }
        if (list.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            emptyTitle.setText(R.string.empty_title);
            emptyDesc.setText(query.isEmpty() ? getString(R.string.empty_desc)
                    : "没有找到「" + query + "」，换个关键词试试。");
            emptyAction.setText(R.string.rescan);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    // ---------------- 顶部导航 ----------------

    private void setupNav() {
        findViewById(R.id.filter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortDialog();
            }
        });
        findViewById(R.id.nav_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSearch();
            }
        });
        findViewById(R.id.nav_history).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LibraryActivity.this, HistoryActivity.class));
            }
        });
        findViewById(R.id.nav_fav).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LibraryActivity.this, FavoritesActivity.class));
            }
        });
        findViewById(R.id.nav_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });
    }

    // ---------------- 顶部滑出式搜索 ----------------

    private void setupSearchBar() {
        searchBar = findViewById(R.id.search_bar);
        searchInput = findViewById(R.id.search_input);
        searchClear = findViewById(R.id.search_clear);

        findViewById(R.id.search_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSearch();
            }
        });
        findViewById(R.id.search_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSearch();
            }
        });
        searchClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchInput.setText("");
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                searchClear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilter();
                grid.scrollToPosition(0);
            }
        });

        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideIme();
                    return true;
                }
                return false;
            }
        });
    }

    /** 搜索条自右侧滑入，覆盖整条顶栏。 */
    private void openSearch() {
        if (searchOpen) return;
        searchOpen = true;
        int w = getResources().getDisplayMetrics().widthPixels;
        searchBar.setVisibility(View.VISIBLE);
        searchBar.setTranslationX(w);
        searchBar.animate().translationX(0f).setDuration(260)
                .setInterpolator(new DecelerateInterpolator()).start();

        searchInput.setText(query);
        searchInput.setSelection(searchInput.getText().length());
        searchInput.requestFocus();
        searchInput.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (searchOpen) {
                    InputMethodManager imm =
                            (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 180);
    }

    /** 收回搜索条：清空条件、恢复完整列表。 */
    private void closeSearch() {
        if (!searchOpen) return;
        searchOpen = false;
        hideIme();
        searchInput.clearFocus();
        if (!searchInput.getText().toString().isEmpty()) searchInput.setText("");

        int w = getResources().getDisplayMetrics().widthPixels;
        searchBar.animate().translationX(w).setDuration(200)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        searchBar.setVisibility(View.INVISIBLE);
                    }
                }).start();
    }

    private void hideIme() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && searchInput != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (searchOpen) {
            closeSearch();
            return;
        }
        super.onBackPressed();
    }

    private void showSettingsDialog() {
        final boolean[] remember = {Prefs.rememberProgress(this)};
        String[] items = {
                "重新扫描片库",
                "记忆播放进度：" + (remember[0] ? "开" : "关"),
                "清除播放记录",
                "片库路径：" + LibraryRepository.LIBRARY_DIR
        };
        new AlertDialog.Builder(this)
                .setTitle("设置")
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            loadLibrary(true);
                            Toast.makeText(this, "正在重新扫描…", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            remember[0] = !remember[0];
                            Prefs.setRememberProgress(this, remember[0]);
                            Toast.makeText(this, remember[0] ? "已开启记忆播放进度" : "已关闭记忆播放进度",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            Prefs.clearAllPositions(this);
                            Toast.makeText(this, "播放记录已清除", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            Toast.makeText(this, "把视频放到 " + LibraryRepository.LIBRARY_DIR,
                                    Toast.LENGTH_LONG).show();
                            break;
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_STORAGE) {
            loadLibrary(true);
        }
    }
}
