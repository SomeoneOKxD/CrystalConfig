package dev.someoneok.crystalconfig.render.text;

import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.TextMetrics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

import java.util.*;

public final class MsdfTextRenderer {
    private static final int METRICS_CACHE_LIMIT = 512;
    private static MsdfTextRenderer INSTANCE;

    private final MsdfFont regular;
    private final MsdfFont medium;
    private final MsdfFont semibold;
    private final MsdfFont fallbackSymbols;
    private final MsdfFont mediaBrands;
    private final Map<MetricsKey, TextMetrics> metricsCache = new LinkedHashMap<>(METRICS_CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<MetricsKey, TextMetrics> eldest) {
            return size() > METRICS_CACHE_LIMIT;
        }
    };

    private MsdfTextRenderer() {
        this.regular = MsdfFont.load("regular");
        this.medium = MsdfFont.load("medium");
        this.semibold = MsdfFont.load("semibold");
        this.fallbackSymbols = MsdfFont.load("fallback-symbols");
        this.mediaBrands = MsdfFont.tryLoad("media-brands").orElse(null);
    }

    public static MsdfTextRenderer get() {
        if (INSTANCE == null) INSTANCE = new MsdfTextRenderer();
        return INSTANCE;
    }

    public TextMetrics measureText(String text, float fontSize, String fontFace) {
        String safe = text == null ? "" : text;
        MsdfFont font = selectFont(fontFace);
        MetricsKey key = new MetricsKey(safe, fontSize, font);
        TextMetrics cached = metricsCache.get(key);
        if (cached != null) return cached;

        float width = 0.0f;
        for (int i = 0, len = safe.length(); i < len; ) {
            int cp = safe.codePointAt(i);
            if (cp == '\n') break;
            i += Character.charCount(cp);
            ResolvedGlyph resolved = resolveGlyph(font, cp);
            if (resolved != null) width += resolved.glyph().advance() * fontSize;
        }

        TextMetrics metrics = new TextMetrics(width, font.lineHeight() * fontSize);
        metricsCache.put(key, metrics);
        return metrics;
    }

    public void drawText(GuiGraphicsExtractor graphics, String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float opacity, boolean shadow) {
        if (text == null || text.isEmpty() || opacity <= 0.0f || color.a() == 0) return;

        MsdfFont font = selectFont(fontFace);
        int argb = color.multiplyAlpha(opacity).toArgb();

        if (shadow) {
            float shadowAlpha = 0.28f;
            float shadowOffset = 0.45f;
            int a = (int) (((argb >>> 24) & 0xFF) * shadowAlpha);
            int r = (int) (((argb >>> 16) & 0xFF) * 0.08f);
            int g = (int) (((argb >>> 8) & 0xFF) * 0.08f);
            int b = (int) ((argb & 0xFF) * 0.08f);
            int shadowColor =
                    (a << 24) |
                            (r << 16) |
                            (g << 8) |
                            b;

            drawLine(
                    graphics,
                    font,
                    text,
                    x + shadowOffset,
                    y + shadowOffset,
                    fontSize,
                    shadowColor
            );
        }

        drawLine(graphics, font, text, x, y, fontSize, argb);
    }

    private void drawLine(GuiGraphicsExtractor graphics, MsdfFont font, String text, float startX, float y, float fontSize, int color) {
        float cursorX = startX;
        float baselineY = y + font.ascender() * fontSize;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();

        Identifier activeTexture = null;
        List<MsdfTextRunRenderState.GlyphQuad> run = new ArrayList<>(Math.min(96, text.length()));

        for (int i = 0, len = text.length(); i < len; ) {
            int cp = text.codePointAt(i);
            if (cp == '\n') break;
            i += Character.charCount(cp);

            ResolvedGlyph resolved = resolveGlyph(font, cp);
            if (resolved == null) continue;
            MsdfFont glyphFont = resolved.font();
            MsdfFont.Glyph glyph = resolved.glyph();

            if (glyph.drawable()) {
                Identifier texture = glyphFont.texture();
                if (activeTexture != null && !activeTexture.equals(texture)) {
                    flushRun(graphics, pose, activeTexture, run, scissor);
                    run = new ArrayList<>(Math.min(96, Math.max(0, text.length() - i)));
                }
                activeTexture = texture;

                float x0 = cursorX + glyph.xOffset() * fontSize;
                float y0 = baselineY + glyph.yOffset() * fontSize;
                run.add(new MsdfTextRunRenderState.GlyphQuad(
                        x0,
                        y0,
                        glyph.width() * fontSize,
                        glyph.height() * fontSize,
                        glyph.u0(),
                        glyph.v0(),
                        glyph.u1(),
                        glyph.v1(),
                        color
                ));
            }

            cursorX += glyph.advance() * fontSize;
        }

        flushRun(graphics, pose, activeTexture, run, scissor);
    }

    private void flushRun(GuiGraphicsExtractor graphics, Matrix3x2f pose, Identifier texture,
                          List<MsdfTextRunRenderState.GlyphQuad> run, ScreenRectangle scissor) {
        if (texture == null || run.isEmpty()) return;
        graphics.guiRenderState.addGuiElement(new MsdfTextRunRenderState(
                pose,
                texture,
                List.copyOf(run),
                scissor
        ));
    }

    private ResolvedGlyph resolveGlyph(MsdfFont preferredFont, int codepoint) {
        MsdfFont.Glyph glyph = preferredFont.glyphOrNull(codepoint);
        if (glyph != null) return new ResolvedGlyph(preferredFont, glyph);

        if (preferredFont != regular) {
            glyph = regular.glyphOrNull(codepoint);
            if (glyph != null) return new ResolvedGlyph(regular, glyph);
        }
        if (preferredFont != medium) {
            glyph = medium.glyphOrNull(codepoint);
            if (glyph != null) return new ResolvedGlyph(medium, glyph);
        }
        if (preferredFont != semibold) {
            glyph = semibold.glyphOrNull(codepoint);
            if (glyph != null) return new ResolvedGlyph(semibold, glyph);
        }

        if (mediaBrands != null && preferredFont != mediaBrands) {
            glyph = mediaBrands.glyphOrNull(codepoint);
            if (glyph != null) return new ResolvedGlyph(mediaBrands, glyph);
        }

        glyph = fallbackSymbols.glyphOrNull(codepoint);
        if (glyph != null) return new ResolvedGlyph(fallbackSymbols, glyph);

        glyph = fallbackSymbols.fallbackGlyph();
        if (glyph != null) return new ResolvedGlyph(fallbackSymbols, glyph);

        glyph = preferredFont.fallbackGlyph();
        return glyph != null ? new ResolvedGlyph(preferredFont, glyph) : null;
    }

    private MsdfFont selectFont(String face) {
        if (face == null) return regular;
        String f = face.toLowerCase(Locale.ROOT);
        if ((f.contains("media") || f.contains("brand") || f.contains("social") || f.contains("icon")) && mediaBrands != null) return mediaBrands;
        if (f.contains("semibold") || f.contains("semi_bold") || f.contains("bold") || f.contains("title")) return semibold;
        if (f.contains("medium") || f.contains("selected")) return medium;
        return regular;
    }

    private record MetricsKey(String text, float fontSize, MsdfFont font) {}
    private record ResolvedGlyph(MsdfFont font, MsdfFont.Glyph glyph) {}
}
