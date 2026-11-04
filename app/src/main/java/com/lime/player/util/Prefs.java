package com.lime.player.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

/** 播放进度等轻量偏好设置。 */
public final class Prefs {

    private static final String NAME = "lime_player";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static long position(Context c, String id) {
        return sp(c).getLong("pos_" + id, 0L);
    }

    public static void setPosition(Context c, String id, long ms) {
        sp(c).edit().putLong("pos_" + id, ms).apply();
    }

    public static void clearPosition(Context c, String id) {
        sp(c).edit().remove("pos_" + id).apply();
    }

    public static boolean rememberProgress(Context c) {
        return sp(c).getBoolean("remember", true);
    }

    public static void setRememberProgress(Context c, boolean v) {
        sp(c).edit().putBoolean("remember", v).apply();
    }

    public static void clearAllPositions(Context c) {
        SharedPreferences p = sp(c);
        SharedPreferences.Editor e = p.edit();
        for (Map.Entry<String, ?> entry : p.getAll().entrySet()) {
            if (entry.getKey().startsWith("pos_")) e.remove(entry.getKey());
        }
        e.apply();
    }

    /** 最近播放（最多 6 条），值为 "id|path|title"。 */
    public static void markPlayed(Context c, String id, String path, String title) {
        String v = id + "|" + path + "|" + title.replace("|", "/");
        String history = sp(c).getString("history", "");
        StringBuilder sb = new StringBuilder(v);
        if (!history.isEmpty()) {
            for (String line : history.split("\n")) {
                if (line.startsWith(id + "|")) continue;
                if (sb.length() > 0) sb.append('\n');
                sb.append(line);
            }
        }
        String[] lines = sb.toString().split("\n");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 8); i++) {
            if (i > 0) out.append('\n');
            out.append(lines[i]);
        }
        sp(c).edit().putString("history", out.toString()).apply();
    }

    public static String history(Context c) {
        return sp(c).getString("history", "");
    }

    /** 删除单条观看记录。 */
    public static void removeHistory(Context c, String id) {
        String history = sp(c).getString("history", "");
        if (history.isEmpty()) return;
        StringBuilder out = new StringBuilder();
        for (String line : history.split("\n")) {
            if (line.startsWith(id + "|")) continue;
            if (out.length() > 0) out.append('\n');
            out.append(line);
        }
        sp(c).edit().putString("history", out.toString()).apply();
    }

    /** 清空观看记录（进度另行清理）。 */
    public static void clearHistory(Context c) {
        sp(c).edit().remove("history").apply();
    }

    public static void toggleFavorite(Context c, String id) {
        java.util.Set<String> set = favorites(c);
        if (!set.remove(id)) set.add(id);
        sp(c).edit().putStringSet("fav", set).apply();
    }

    public static boolean isFavorite(Context c, String id) {
        return favorites(c).contains(id);
    }

    /** 取消单个收藏。 */
    public static void removeFavorite(Context c, String id) {
        java.util.Set<String> set = favorites(c);
        if (set.remove(id)) sp(c).edit().putStringSet("fav", set).apply();
    }

    /** 清空收藏。 */
    public static void clearFavorites(Context c) {
        sp(c).edit().remove("fav").apply();
    }

    public static java.util.Set<String> favorites(Context c) {
        return new java.util.HashSet<>(sp(c).getStringSet("fav",
                new java.util.HashSet<String>()));
    }
}
