package dev.someoneok.crystalconfig.render;

import dev.someoneok.crystalconfig.theme.Theme;

import java.util.*;

/**
 * Parses legacy Minecraft text formatting codes for developer-provided display text.
 *
 * Supported color codes: section sign + 0-9/a-f.
 * Supported style codes: section sign + l/n/m/r.
 * Prefix a formatting code with a backslash to show it literally, e.g. "\\§cHello".
 */
public final class MinecraftTextFormatting {
    public static final char CODE_PREFIX = '\u00A7';
    private static final int CACHE_LIMIT = 1024;

    private static final ColorRGBA[] COLORS = new ColorRGBA[] {
            ColorRGBA.rgb(0x00, 0x00, 0x00), // 0 black
            ColorRGBA.rgb(0x00, 0x00, 0xAA), // 1 dark blue
            ColorRGBA.rgb(0x00, 0xAA, 0x00), // 2 dark green
            ColorRGBA.rgb(0x00, 0xAA, 0xAA), // 3 dark aqua
            ColorRGBA.rgb(0xAA, 0x00, 0x00), // 4 dark red
            ColorRGBA.rgb(0xAA, 0x00, 0xAA), // 5 dark purple
            ColorRGBA.rgb(0xFF, 0xAA, 0x00), // 6 gold
            ColorRGBA.rgb(0xAA, 0xAA, 0xAA), // 7 gray
            ColorRGBA.rgb(0x55, 0x55, 0x55), // 8 dark gray
            ColorRGBA.rgb(0x55, 0x55, 0xFF), // 9 blue
            ColorRGBA.rgb(0x55, 0xFF, 0x55), // a green
            ColorRGBA.rgb(0x55, 0xFF, 0xFF), // b aqua
            ColorRGBA.rgb(0xFF, 0x55, 0x55), // c red
            ColorRGBA.rgb(0xFF, 0x55, 0xFF), // d light purple
            ColorRGBA.rgb(0xFF, 0xFF, 0x55), // e yellow
            ColorRGBA.rgb(0xFF, 0xFF, 0xFF)  // f white
    };

    private static final Map<String, String> STRIP_CACHE = new LinkedHashMap<>(CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    private static final Map<String, String> SEARCH_CACHE = new LinkedHashMap<>(CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    private MinecraftTextFormatting() {}

    public static boolean mightContainFormatting(String text) {
        return text != null && text.indexOf(CODE_PREFIX) >= 0;
    }

    public static String stripFormatting(String text) {
        if (text == null || text.isEmpty()) return "";
        synchronized (STRIP_CACHE) {
            String cached = STRIP_CACHE.get(text);
            if (cached != null) return cached;
        }
        String stripped = stripFormattingUncached(text);
        synchronized (STRIP_CACHE) {
            STRIP_CACHE.put(text, stripped);
        }
        return stripped;
    }

    public static String normalizeForSearch(String text) {
        if (text == null || text.isEmpty()) return "";
        synchronized (SEARCH_CACHE) {
            String cached = SEARCH_CACHE.get(text);
            if (cached != null) return cached;
        }
        String normalized = stripFormatting(text).toLowerCase(Locale.ROOT);
        synchronized (SEARCH_CACHE) {
            SEARCH_CACHE.put(text, normalized);
        }
        return normalized;
    }

    public static TextMetrics measureText(RenderBackend backend, Theme theme, String text, float fontSize, String fontFace) {
        String safe = text == null ? "" : text;
        if (!mightContainFormatting(safe)) return backend.measureText(safe, fontSize, fontFace);

        float width = 0.0f;
        float height = backend.measureText("Ag", fontSize, fontFace).height();
        for (Segment segment : parse(safe, ColorRGBA.WHITE, fontFace, theme.fonts().semibold())) {
            if (segment.text().isEmpty()) continue;
            TextMetrics metrics = backend.measureText(segment.text(), fontSize, segment.fontFace());
            width += metrics.width();
            height = Math.max(height, metrics.height());
        }
        return new TextMetrics(width, height);
    }

    public static List<Segment> parse(String text, ColorRGBA baseColor, String baseFontFace, String boldFontFace) {
        String safe = text == null ? "" : text;
        List<Segment> segments = new ArrayList<>();
        StringBuilder run = new StringBuilder(Math.min(64, safe.length()));
        State state = new State(baseColor, baseFontFace, false, false, false);

        for (int i = 0; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            if (ch == '\\' && i + 2 < safe.length() && safe.charAt(i + 1) == CODE_PREFIX && isSupportedCode(safe.charAt(i + 2))) {
                run.append(CODE_PREFIX).append(safe.charAt(i + 2));
                i += 2;
                continue;
            }
            if (ch == CODE_PREFIX && i + 1 < safe.length() && isSupportedCode(safe.charAt(i + 1))) {
                flush(segments, run, state);
                state = applyCode(state, safe.charAt(++i), baseColor, baseFontFace, boldFontFace);
                continue;
            }
            run.append(ch);
        }
        flush(segments, run, state);
        return segments;
    }

    private static String stripFormattingUncached(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\\' && i + 2 < text.length() && text.charAt(i + 1) == CODE_PREFIX && isSupportedCode(text.charAt(i + 2))) {
                out.append(CODE_PREFIX).append(text.charAt(i + 2));
                i += 2;
                continue;
            }
            if (ch == CODE_PREFIX && i + 1 < text.length() && isSupportedCode(text.charAt(i + 1))) {
                i++;
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static void flush(List<Segment> segments, StringBuilder run, State state) {
        if (run.length() == 0) return;
        segments.add(new Segment(run.toString(), state.color(), state.fontFace(), state.bold(), state.underline(), state.strikethrough()));
        run.setLength(0);
    }

    private static State applyCode(State state, char code, ColorRGBA baseColor, String baseFontFace, String boldFontFace) {
        char normalized = Character.toLowerCase(code);
        int colorIndex = colorIndex(normalized);
        if (colorIndex >= 0) {
            ColorRGBA minecraftColor = COLORS[colorIndex].withAlpha(baseColor.a());
            return new State(minecraftColor, baseFontFace, false, false, false);
        }
        return switch (normalized) {
            case 'l' -> new State(state.color(), boldFontFace, true, state.underline(), state.strikethrough());
            case 'n' -> new State(state.color(), state.fontFace(), state.bold(), true, state.strikethrough());
            case 'm' -> new State(state.color(), state.fontFace(), state.bold(), state.underline(), true);
            case 'r' -> new State(baseColor, baseFontFace, false, false, false);
            default -> state;
        };
    }

    private static boolean isSupportedCode(char code) {
        char c = Character.toLowerCase(code);
        return colorIndex(c) >= 0 || c == 'l' || c == 'n' || c == 'm' || c == 'r';
    }

    private static int colorIndex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return 10 + (c - 'a');
        return -1;
    }

    public record Segment(String text, ColorRGBA color, String fontFace, boolean bold, boolean underline, boolean strikethrough) {}
    private record State(ColorRGBA color, String fontFace, boolean bold, boolean underline, boolean strikethrough) {}
}
