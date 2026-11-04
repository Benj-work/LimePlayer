package com.lime.player.data;

import java.util.ArrayList;
import java.util.List;

/** 片库中的一个条目：电影，或一部剧（含多集）。 */
public class MediaEntry {

    public String id;
    public String title;
    public String videoPath;
    public String posterPath;
    /** 横版背景（详情页用），缺省时回落到海报。 */
    public String backdropPath;

    public int year;
    public float rating;
    public int runtimeMin;
    public int videoWidth;
    public int videoHeight;
    public long durationMs;
    public String plot = "";
    public final List<String> genres = new ArrayList<>();
    public final List<String> actors = new ArrayList<>();
    public String director = "";

    public boolean series;
    public String seriesKey;
    public int season;
    public int episode;
    /** 剧集的分集标题，如“广西溶洞”。 */
    public String subtitleExtra = "";

    public final List<MediaEntry> episodes = new ArrayList<>();

    public String subtitle() {
        StringBuilder sb = new StringBuilder();
        if (rating > 0) {
            sb.append(String.format(java.util.Locale.US, "%.1f", rating)).append("分");
        }
        if (year > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(year);
        } else if (!series && runtimeMin > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(runtimeMin).append("分钟");
        }
        return sb.toString();
    }

    public String quality() {
        if (videoHeight >= 2000 || videoWidth >= 3500) return "4K";
        if (videoHeight >= 1000 || videoWidth >= 1800) return "1080P";
        if (videoHeight >= 700) return "720P";
        return "";
    }

    public String tagText() {
        if (series) return "全" + episodes.size() + "集";
        String q = quality();
        if (!q.isEmpty()) return q;
        if (runtimeMin > 0) return runtimeMin + "分钟";
        return "";
    }

    public String genreText() {
        StringBuilder sb = new StringBuilder();
        for (String g : genres) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(g);
        }
        return sb.toString();
    }

    public String metaText() {
        StringBuilder sb = new StringBuilder();
        if (year > 0) sb.append(year);
        if (!genres.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(genreText());
        }
        if (runtimeMin > 0) {
            if (sb.length() > 0) sb.append(" · ");
            String q = quality();
            sb.append(q.isEmpty() ? "" : q + " · ").append(runtimeMin).append("分钟");
        }
        if (series) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("全").append(episodes.size()).append("集");
        }
        return sb.toString();
    }
}
