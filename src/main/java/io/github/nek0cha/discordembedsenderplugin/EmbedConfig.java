package io.github.nek0cha.discordembedsenderplugin;

import java.util.List;

public class EmbedConfig {
    public long channel; // 送信先チャンネルID
    public String title;
    public String description;
    public String color; // "#RRGGBB" 形式または整数（オプション）
    public boolean timestamp = false;
    public List<Field> fields;
    public Image image;
    public Image thumbnail;
    public Footer footer;

    public static class Field {
        public String name;
        public String value;
        public boolean inline = false;
    }

    public static class Image {
        public String url;
    }

    public static class Footer {
        public String text;
        public String icon_url;
    }
}