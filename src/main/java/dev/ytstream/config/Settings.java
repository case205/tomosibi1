package dev.ytstream.config;

import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** 検証済みの不変設定。ストリームキーは含まない(Secretとして別管理)。 */
public record Settings(
        String serverName,
        String rtmpUrl,
        int width, int height, int fps,
        int videoKbps, int audioKbps, boolean audioEnabled,
        String codec, String preset, String tune, int threads, boolean lowPriority,
        String ffmpegPath,
        boolean reconnectEnabled, int maxAttempts, int initialDelaySec, int maxDelaySec,
        String backgroundPath, String fontName, String fontFile,
        String themeName, String accentColor, int panelOpacity,
        boolean showChat, boolean showJoinLeave, boolean showPlayerList, boolean showUptime,
        int chatMaxLines, int chatMaxLength, int chatLifetimeSec, List<Pattern> chatBlocked,
        int toastLifetimeSec,
        boolean hideVanished, String bedrockPrefix,
        Map<String, Rect> layout, Map<String, String> labels,
        List<String> fatalProblems) {

    /** 画面に対する相対座標(0.0〜1.0)。解像度を変えてもレイアウトが崩れない。 */
    public record Rect(double x, double y, double w, double h) {
        public Rectangle toPixels(int width, int height) {
            return new Rectangle(
                    (int) Math.round(x * width), (int) Math.round(y * height),
                    (int) Math.round(w * width), (int) Math.round(h * height));
        }
    }

    public static final Map<String, Rect> DEFAULT_LAYOUT = new LinkedHashMap<>();
    public static final Map<String, String> DEFAULT_LABELS = new LinkedHashMap<>();

    static {
        DEFAULT_LAYOUT.put("header", new Rect(0.02, 0.03, 0.96, 0.13));
        DEFAULT_LAYOUT.put("toasts", new Rect(0.02, 0.19, 0.34, 0.26));
        DEFAULT_LAYOUT.put("chat", new Rect(0.02, 0.50, 0.48, 0.46));
        DEFAULT_LAYOUT.put("players", new Rect(0.76, 0.19, 0.22, 0.77));
        DEFAULT_LABELS.put("online", "オンライン");
        DEFAULT_LABELS.put("uptime", "稼働時間");
        DEFAULT_LABELS.put("players", "プレイヤー");
        DEFAULT_LABELS.put("chat", "チャット");
        DEFAULT_LABELS.put("joined", "{name} が参加しました");
        DEFAULT_LABELS.put("left", "{name} が退出しました");
        DEFAULT_LABELS.put("more", "他 {n} 人");
    }

    public Rect rect(String key) {
        Rect r = layout.get(key);
        return r != null ? r : DEFAULT_LAYOUT.get(key);
    }

    public String label(String key) {
        String v = labels.get(key);
        return v != null ? v : DEFAULT_LABELS.getOrDefault(key, key);
    }

    /** テストやフォールバック用の既定値。 */
    public static Settings defaults() {
        return new Settings(
                "My Minecraft Server", "rtmp://a.rtmp.youtube.com/live2",
                1280, 720, 30, 2500, 128, true,
                "libx264", "veryfast", "zerolatency", 2, true,
                "ffmpeg",
                true, 10, 5, 60,
                "background.png", "SansSerif", "",
                "default", "", 62,
                true, true, true, true,
                10, 100, 0, List.of(),
                6,
                true, ".",
                new LinkedHashMap<>(DEFAULT_LAYOUT), new LinkedHashMap<>(DEFAULT_LABELS),
                List.of());
    }
}
