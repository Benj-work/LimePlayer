package com.lime.player.data;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 扫描 /sdcard/ExoPlayerLibrary/ 构建片库。 */
public final class LibraryRepository {

    public static final String LIBRARY_DIR = "/sdcard/ExoPlayerLibrary";
    private static final String[] VIDEO_EXT = {
            ".mp4", ".mkv", ".avi", ".mov", ".ts", ".webm", ".flv", ".m4v", ".wmv"
    };
    private static final Pattern EPISODE = Pattern.compile(
            "^(.*?)[. _-]*[Ss](\\d{1,2})[Ee](\\d{1,3})(?:[. _-]+(.*))?$");

    private static volatile LibraryRepository instance;

    private final List<MediaEntry> entries = new ArrayList<>();
    private volatile boolean scanned;

    public static LibraryRepository get() {
        if (instance == null) {
            synchronized (LibraryRepository.class) {
                if (instance == null) instance = new LibraryRepository();
            }
        }
        return instance;
    }

    public static File libraryDir() {
        return new File(LIBRARY_DIR);
    }

    public List<MediaEntry> entries() {
        return entries;
    }

    public boolean isScanned() {
        return scanned;
    }

    public MediaEntry findById(String id) {
        for (MediaEntry e : entries) {
            if (e.id.equals(id)) return e;
            for (MediaEntry ep : e.episodes) {
                if (ep.id.equals(id)) return e;
            }
        }
        return null;
    }

    /** 精确查找：单集 id 返回该集本身（findById 会返回所属剧集，适合详情页）。 */
    public MediaEntry findExact(String id) {
        if (id == null) return null;
        for (MediaEntry e : entries) {
            if (id.equals(e.id)) return e;
            for (MediaEntry ep : e.episodes) {
                if (id.equals(ep.id)) return ep;
            }
        }
        return null;
    }

    public MediaEntry findByPath(String path) {
        if (path == null) return null;
        for (MediaEntry e : entries) {
            if (path.equals(e.videoPath)) return e;
            for (MediaEntry ep : e.episodes) {
                if (path.equals(ep.videoPath)) return ep;
            }
        }
        return null;
    }

    public synchronized void scan(Context context) {
        entries.clear();
        File dir = libraryDir();
        File[] files = dir.listFiles();
        if (files == null) {
            scanned = true;
            return;
        }

        Map<String, MediaEntry> seriesMap = new LinkedHashMap<>();
        List<MediaEntry> movies = new ArrayList<>();

        for (File f : files) {
            if (f.isDirectory() || !isVideo(f.getName())) continue;

            String base = stripExt(f.getName());
            Matcher m = EPISODE.matcher(base);

            NfoParser.Meta meta = NfoParser.parse(new File(dir, base + ".nfo"));
            String poster = resolvePoster(dir, base, meta.poster);
            String backdrop = resolveBackdrop(dir, base);

            if (m.matches()) {
                String showName = clean(m.group(1));
                int season = parseInt(m.group(2), 1);
                int ep = parseInt(m.group(3), 1);
                String epTitle = clean(m.group(4));

                String seriesKey = showName + "#S" + season;
                MediaEntry series = seriesMap.get(seriesKey);
                if (series == null) {
                    series = new MediaEntry();
                    series.series = true;
                    series.seriesKey = seriesKey;
                    series.season = season;
                    series.title = !meta.showTitle.isEmpty() ? meta.showTitle : showName;
                    series.id = "series:" + seriesKey;
                    series.posterPath = poster;
                    series.backdropPath = backdrop;
                    series.year = meta.year;
                    series.rating = meta.rating;
                    series.plot = meta.plot;
                    series.director = meta.director;
                    series.genres.addAll(meta.genres);
                    series.actors.addAll(meta.actors);
                    seriesMap.put(seriesKey, series);
                }
                if (series.posterPath == null && poster != null) series.posterPath = poster;
                if (series.backdropPath == null && backdrop != null) series.backdropPath = backdrop;
                if (series.plot.isEmpty() && !meta.plot.isEmpty()) series.plot = meta.plot;
                if (series.rating == 0 && meta.rating > 0) series.rating = meta.rating;

                MediaEntry item = new MediaEntry();
                item.id = "ep:" + f.getAbsolutePath();
                item.title = series.title;
                item.videoPath = f.getAbsolutePath();
                item.posterPath = poster;
                item.backdropPath = backdrop;
                item.year = meta.year > 0 ? meta.year : series.year;
                item.rating = meta.rating > 0 ? meta.rating : series.rating;
                item.runtimeMin = meta.runtimeMin;
                item.plot = meta.plot;
                item.director = meta.director;
                item.genres.addAll(meta.genres);
                item.actors.addAll(meta.actors);
                item.series = true;
                item.seriesKey = seriesKey;
                item.season = season;
                item.episode = ep;
                if (epTitle.isEmpty()) {
                    epTitle = meta.title != null && !meta.title.isEmpty() ? meta.title : ("第" + ep + "集");
                }
                item.subtitleExtra = epTitle;
                probe(item);
                series.episodes.add(item);
            } else {
                MediaEntry movie = new MediaEntry();
                movie.id = "movie:" + f.getAbsolutePath();
                movie.videoPath = f.getAbsolutePath();
                movie.title = !meta.title.isEmpty() ? meta.title : base;
                movie.posterPath = poster;
                movie.backdropPath = backdrop;
                movie.year = meta.year;
                movie.rating = meta.rating;
                movie.runtimeMin = meta.runtimeMin;
                movie.plot = meta.plot;
                movie.director = meta.director;
                movie.genres.addAll(meta.genres);
                movie.actors.addAll(meta.actors);
                probe(movie);
                movies.add(movie);
            }
        }

        for (MediaEntry s : seriesMap.values()) {
            Collections.sort(s.episodes, new Comparator<MediaEntry>() {
                @Override
                public int compare(MediaEntry a, MediaEntry b) {
                    return Integer.compare(a.episode, b.episode);
                }
            });
            if (s.runtimeMin == 0 && !s.episodes.isEmpty()) s.runtimeMin = s.episodes.get(0).runtimeMin;
            if (!s.episodes.isEmpty()) {
                s.videoWidth = s.episodes.get(0).videoWidth;
                s.videoHeight = s.episodes.get(0).videoHeight;
                s.durationMs = s.episodes.get(0).durationMs;
            }
            entries.add(s);
        }
        entries.addAll(movies);

        Collections.sort(entries, new Comparator<MediaEntry>() {
            @Override
            public int compare(MediaEntry a, MediaEntry b) {
                int r = Float.compare(b.rating, a.rating);
                if (r != 0) return r;
                return a.title.compareTo(b.title);
            }
        });
        scanned = true;
    }

    public List<MediaEntry> byCategory(String category) {
        List<MediaEntry> out = new ArrayList<>();
        for (MediaEntry e : entries) {
            if ("movie".equals(category) && e.series) continue;
            if ("series".equals(category) && !e.series) continue;
            if ("doc".equals(category) && !isDocumentary(e)) continue;
            out.add(e);
        }
        return out;
    }

    private boolean isDocumentary(MediaEntry e) {
        for (String g : e.genres) {
            if (g.contains("纪录")) return true;
        }
        return false;
    }

    /** 读取真实分辨率与时长，用于画质角标和总时长。 */
    private static void probe(MediaEntry e) {
        android.media.MediaMetadataRetriever r = new android.media.MediaMetadataRetriever();
        try {
            r.setDataSource(e.videoPath);
            String w = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String d = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (w != null) e.videoWidth = Integer.parseInt(w);
            if (h != null) e.videoHeight = Integer.parseInt(h);
            if (d != null) {
                e.durationMs = Long.parseLong(d);
                if (e.runtimeMin <= 0) e.runtimeMin = (int) (e.durationMs / 60000);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                r.release();
            } catch (Throwable ignored) {
            }
        }
    }

    private static boolean isVideo(String name) {
        String lower = name.toLowerCase(Locale.US);
        for (String ext : VIDEO_EXT) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.replace('_', ' ').replace('.', ' ').trim();
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    /** 详情页横版背景：优先 <名>-fanart.jpg，其次 fanart.jpg / backdrop.jpg。 */
    private static String resolveBackdrop(File dir, String base) {
        String[] exts = {".jpg", ".jpeg", ".png", ".webp"};
        String[] suffixes = {"-fanart", ".fanart", "-backdrop", ".backdrop", "-thumb"};
        for (String s : suffixes) {
            for (String ext : exts) {
                File f = new File(dir, base + s + ext);
                if (f.exists()) return f.getAbsolutePath();
            }
        }
        for (String ext : exts) {
            File f = new File(dir, "fanart" + ext);
            if (f.exists()) return f.getAbsolutePath();
        }
        return null;
    }

    private static String resolvePoster(File dir, String base, String nfoPoster) {
        if (nfoPoster != null && !nfoPoster.isEmpty()) {
            File f = new File(nfoPoster);
            if (f.isAbsolute() && f.exists()) return f.getAbsolutePath();
            File rel = new File(dir, nfoPoster);
            if (rel.exists()) return rel.getAbsolutePath();
        }
        String[] exts = {".jpg", ".jpeg", ".png", ".webp"};
        for (String ext : exts) {
            File f = new File(dir, base + ext);
            if (f.exists()) return f.getAbsolutePath();
        }
        return null;
    }
}
