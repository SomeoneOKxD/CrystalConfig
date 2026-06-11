package dev.someoneok.crystalconfig.render;

import dev.someoneok.crystalconfig.theme.Theme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RenderContext {
    private final DrawList drawList;
    private final RenderBackend backend;
    private final Theme theme;
    private final float deltaSeconds;
    private final boolean debugBounds;
    private final float scale;
    private final Map<String, TextMetrics> textMeasureCache = new HashMap<>(128);
    private final Map<String, TextMetrics> plainTextMeasureCache = new HashMap<>(128);
    private final boolean textShadow;

    public RenderContext(DrawList drawList, RenderBackend backend, Theme theme, float deltaSeconds, boolean debugBounds) {
        this(drawList, backend, theme, deltaSeconds, debugBounds, 1.0f, false);
    }

    public RenderContext(DrawList drawList, RenderBackend backend, Theme theme, float deltaSeconds, boolean debugBounds, float scale) {
        this(drawList, backend, theme, deltaSeconds, debugBounds, scale, false);
    }

    public RenderContext(DrawList drawList, RenderBackend backend, Theme theme, float deltaSeconds, boolean debugBounds, float scale, boolean textShadow) {
        this.drawList = drawList;
        this.backend = backend;
        this.theme = theme;
        this.deltaSeconds = deltaSeconds;
        this.debugBounds = debugBounds;
        this.scale = Math.max(0.25f, scale);
        this.textShadow = textShadow;
    }

    public Theme theme() { return theme; }
    public RenderBackend backend() { return backend; }
    public float deltaSeconds() { return deltaSeconds; }
    public boolean debugBounds() { return debugBounds; }
    public float scale() { return scale; }
    public boolean textShadow() { return textShadow; }

    public void rect(Rect rect, SdfRectStyle style, float z) {
        if (rect == null || rect.isEmpty() || style.opacity() <= 0.0f) return;
        if (style.fill().a() == 0 && style.border().a() == 0 && style.shadow().a() == 0) return;
        drawList.add(new QuadCommand(
                Material.SDF_RECT,
                scale(rect),
                style.fill(),
                style.border(),
                style.shadow(),
                style.radius() * scale,
                style.borderWidth() * scale,
                style.shadowRadius() * scale,
                style.shadowOffsetX() * scale,
                style.shadowOffsetY() * scale,
                style.opacity(),
                0,
                0,
                z
        ));
    }

    public void rect(Rect rect, ColorRGBA fill, float radius, float z) {
        rect(rect, SdfRectStyle.create().fill(fill).radius(radius), z);
    }

    public void specialQuad(Material material, Rect rect, ColorRGBA fill, float data0, float data1, float z) {
        if (rect == null || rect.isEmpty() || fill.a() == 0) return;
        drawList.add(new QuadCommand(material, scale(rect), fill, ColorRGBA.TRANSPARENT, ColorRGBA.TRANSPARENT, 0, 0, 0, 0, 0, 1, data0, data1, z));
    }

    public void text(String text, float x, float y, float fontSize, ColorRGBA color, float z) {
        text(text, x, y, fontSize, theme.fonts().regular(), color, 1.0f, false, z);
    }

    public void text(String text, float x, float y, float fontSize, ColorRGBA color, float opacity, boolean shadow, float z) {
        text(text, x, y, fontSize, theme.fonts().regular(), color, opacity, shadow, z);
    }

    public void text(String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float z) {
        text(text, x, y, fontSize, fontFace, color, 1.0f, false, z);
    }

    public void text(String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float opacity, boolean shadow, float z) {
        if (text == null || text.isEmpty() || fontSize <= 0.0f || color.a() == 0 || opacity <= 0.0f) return;
        if (!MinecraftTextFormatting.mightContainFormatting(text)) {
            drawTextCommand(text, x, y, fontSize, fontFace, color, opacity, shadow, z);
            return;
        }
        drawFormattedText(text, x, y, fontSize, fontFace, color, opacity, shadow, z);
    }

    public void plainText(String text, float x, float y, float fontSize, ColorRGBA color, float z) {
        plainText(text, x, y, fontSize, theme.fonts().regular(), color, 1.0f, false, z);
    }

    public void plainText(String text, float x, float y, float fontSize, ColorRGBA color, float opacity, boolean shadow, float z) {
        plainText(text, x, y, fontSize, theme.fonts().regular(), color, opacity, shadow, z);
    }

    public void plainText(String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float z) {
        plainText(text, x, y, fontSize, fontFace, color, 1.0f, false, z);
    }

    public void plainText(String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float opacity, boolean shadow, float z) {
        if (text == null || text.isEmpty() || fontSize <= 0.0f || color.a() == 0 || opacity <= 0.0f) return;
        drawTextCommand(text, x, y, fontSize, fontFace, color, opacity, shadow, z);
    }

    private void drawFormattedText(String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float opacity, boolean shadow, float z) {
        List<MinecraftTextFormatting.Segment> segments = MinecraftTextFormatting.parse(text, color, fontFace, theme.fonts().semibold());
        float cursorX = x;
        float lineHeight = lineHeight(fontSize, fontFace);
        float decorationHeight = Math.max(1.0f / scale, fontSize * 0.08f);
        for (MinecraftTextFormatting.Segment segment : segments) {
            if (segment.text().isEmpty()) continue;
            drawTextCommand(segment.text(), cursorX, y, fontSize, segment.fontFace(), segment.color(), opacity, shadow, z);
            float width = measurePlainText(segment.text(), fontSize, segment.fontFace()).width();
            ColorRGBA decoration = segment.color().multiplyAlpha(opacity);
            if (segment.underline()) {
                rect(new Rect(cursorX, y + lineHeight * 0.88f, width, decorationHeight), decoration, 0, z + 0.01f);
            }
            if (segment.strikethrough()) {
                rect(new Rect(cursorX, y + lineHeight * 0.52f, width, decorationHeight), decoration, 0, z + 0.01f);
            }
            cursorX += width;
        }
    }

    private void drawTextCommand(String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float opacity, boolean shadow, float z) {
        drawList.add(new TextCommand(
                text,
                x * scale,
                y * scale,
                fontSize * scale,
                fontFace,
                color,
                opacity,
                shadow || textShadow,
                z
        ));
    }

    public TextMetrics measureText(String text, float fontSize) {
        return measureText(text, fontSize, theme.fonts().regular());
    }

    public TextMetrics measurePlainText(String text, float fontSize) {
        return measurePlainText(text, fontSize, theme.fonts().regular());
    }

    public float lineHeight(float fontSize) {
        return measureText("Ag", fontSize).height();
    }

    public float lineHeight(float fontSize, String fontFace) {
        return measureText("Ag", fontSize, fontFace).height();
    }

    public TextMetrics measureText(String text, float fontSize, String fontFace) {
        String safeText = text == null ? "" : text;
        String key = "fmt\u0000" + safeText + '\u0000' + fontSize + '\u0000' + fontFace + '\u0000' + scale;
        TextMetrics cached = textMeasureCache.get(key);
        if (cached != null) return cached;
        TextMetrics metrics = MinecraftTextFormatting.measureText(backend, theme, safeText, fontSize * scale, fontFace);
        TextMetrics logical = new TextMetrics(metrics.width() / scale, metrics.height() / scale);
        textMeasureCache.put(key, logical);
        return logical;
    }

    public TextMetrics measurePlainText(String text, float fontSize, String fontFace) {
        String safeText = text == null ? "" : text;
        String key = "plain\u0000" + safeText + '\u0000' + fontSize + '\u0000' + fontFace + '\u0000' + scale;
        TextMetrics cached = plainTextMeasureCache.get(key);
        if (cached != null) return cached;
        TextMetrics metrics = backend.measureText(safeText, fontSize * scale, fontFace);
        TextMetrics logical = new TextMetrics(metrics.width() / scale, metrics.height() / scale);
        plainTextMeasureCache.put(key, logical);
        return logical;
    }

    public void pushClip(Rect rect) { drawList.pushClip(scale(rect)); }
    public void popClip() { drawList.popClip(); }

    private Rect scale(Rect rect) {
        if (scale == 1.0f) return rect;
        return new Rect(rect.x() * scale, rect.y() * scale, rect.w() * scale, rect.h() * scale);
    }
}
