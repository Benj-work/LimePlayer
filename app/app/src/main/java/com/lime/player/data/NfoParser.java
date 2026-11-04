package com.lime.player.data;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** 读取 Kodi/Emby 风格的 .nfo 元数据。 */
public final class NfoParser {

    public static final class Meta {
        public boolean isEpisode;
        public String title = "";
        public String showTitle = "";
        public int year;
        public float rating;
        public int runtimeMin;
        public String plot = "";
        public String poster = "";
        public int season;
        public int episode;
        public final List<String> genres = new ArrayList<>();
        public final List<String> actors = new ArrayList<>();
        public String director = "";
    }

    private NfoParser() {
    }

    public static Meta parse(File nfo) {
        Meta meta = new Meta();
        if (nfo == null || !nfo.exists()) return meta;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            Document doc = b.parse(nfo);
            Element root = doc.getDocumentElement();
            if (root == null) return meta;
            String tag = root.getTagName();
            meta.isEpisode = "episodedetails".equalsIgnoreCase(tag);

            meta.title = text(root, "title");
            meta.year = intOf(text(root, "year"));
            meta.rating = floatOf(text(root, "rating"));
            meta.runtimeMin = intOf(text(root, "runtime"));
            meta.plot = text(root, "plot");
            if (meta.plot.isEmpty()) meta.plot = text(root, "outline");
            meta.director = text(root, "director");
            meta.season = intOf(text(root, "season"));
            meta.episode = intOf(text(root, "episode"));

            for (String g : all(root, "genre")) meta.genres.add(g);

            NodeList actors = root.getElementsByTagName("actor");
            for (int i = 0; i < actors.getLength(); i++) {
                Node n = actors.item(i);
                if (n instanceof Element) {
                    String name = text((Element) n, "name");
                    if (!name.isEmpty()) meta.actors.add(name);
                }
            }

            NodeList arts = root.getElementsByTagName("art");
            for (int i = 0; i < arts.getLength(); i++) {
                Node n = arts.item(i);
                if (n instanceof Element) {
                    String poster = text((Element) n, "poster");
                    if (!poster.isEmpty()) {
                        meta.poster = poster;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return meta;
    }

    private static String text(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getParentNode() == parent || (n.getParentNode() != null
                    && "art".equals(n.getParentNode().getNodeName()))) {
                String v = n.getTextContent();
                if (v != null && !v.trim().isEmpty()) return v.trim();
            }
        }
        return "";
    }

    private static List<String> all(Element parent, String tag) {
        List<String> out = new ArrayList<>();
        NodeList list = parent.getElementsByTagName(tag);
        for (int i = 0; i < list.getLength(); i++) {
            String v = list.item(i).getTextContent();
            if (v != null && !v.trim().isEmpty()) out.add(v.trim());
        }
        return out;
    }

    private static int intOf(String s) {
        try {
            return (int) Float.parseFloat(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static float floatOf(String s) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            return 0f;
        }
    }
}
