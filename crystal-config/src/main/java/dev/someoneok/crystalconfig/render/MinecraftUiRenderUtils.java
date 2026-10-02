package dev.someoneok.crystalconfig.render;

import dev.someoneok.crystalconfig.api.render.UiRenderUtils;
import dev.someoneok.crystalconfig.theme.Theme;
import dev.someoneok.crystalconfig.theme.ThemePresets;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Minecraft/Fabric convenience wrapper for the renderer-only public API.
 *
 * <p>Create one instance per screen and reuse it across frames. It owns the reusable render
 * backend/command buffers and optional recorded-frame cache; application values and input state
 * remain entirely in the host mod.</p>
 */
public final class MinecraftUiRenderUtils {
    private final FabricUiRenderBackend backend = new FabricUiRenderBackend();
    private final UiRenderUtils renderer = new UiRenderUtils();
    private Theme theme = ThemePresets.darkCrimson();
    private float scale = 1.0f;
    private boolean textShadow;
    private boolean frameCaching;
    private long cacheRevision = 1L;
    private long renderedCacheRevision;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private float cachedUiScale = Float.NaN;

    private MinecraftUiRenderUtils() { }

    public static MinecraftUiRenderUtils create() {
        return new MinecraftUiRenderUtils();
    }

    public MinecraftUiRenderUtils theme(Theme theme) {
        Theme next = Objects.requireNonNull(theme, "theme");
        if (!Objects.equals(this.theme, next)) {
            this.theme = next;
            invalidate();
        }
        return this;
    }

    public Theme theme() {
        return theme;
    }

    public MinecraftUiRenderUtils scale(float scale) {
        if (!Float.isFinite(scale) || scale <= 0.0f) {
            throw new IllegalArgumentException("scale must be finite and > 0");
        }
        if (Float.compare(this.scale, scale) != 0) {
            this.scale = scale;
            invalidate();
        }
        return this;
    }

    public float scale() {
        return scale;
    }

    public MinecraftUiRenderUtils textShadow(boolean textShadow) {
        if (this.textShadow != textShadow) {
            this.textShadow = textShadow;
            invalidate();
        }
        return this;
    }

    public boolean textShadow() {
        return textShadow;
    }

    public MinecraftUiRenderUtils frameCaching(boolean enabled) {
        if (this.frameCaching != enabled) {
            this.frameCaching = enabled;
            clearFrameCache();
        }
        return this;
    }

    public boolean frameCaching() {
        return frameCaching;
    }

    public MinecraftUiRenderUtils invalidate() {
        if (cacheRevision == Long.MAX_VALUE) {
            cacheRevision = 1L;
            renderedCacheRevision = 0L;
            backend.clearCachedFrame();
        } else {
            cacheRevision++;
        }
        return this;
    }

    public MinecraftUiRenderUtils clearFrameCache() {
        backend.clearCachedFrame();
        renderedCacheRevision = 0L;
        cachedWidth = -1;
        cachedHeight = -1;
        cachedUiScale = Float.NaN;
        return invalidate();
    }

    public boolean hasValidCachedFrame() {
        return frameCaching
                && backend.hasCachedFrame()
                && renderedCacheRevision == cacheRevision;
    }

    public void render(
            GuiGraphicsExtractor graphics,
            int width,
            int height,
            Consumer<UiRenderUtils> draw
    ) {
        render(graphics, width, height, 1.0f, 0.0f, draw);
    }

    public void render(
            GuiGraphicsExtractor graphics,
            int width,
            int height,
            float deltaSeconds,
            Consumer<UiRenderUtils> draw
    ) {
        render(graphics, width, height, 1.0f, deltaSeconds, draw);
    }

    public void render(
            GuiGraphicsExtractor graphics,
            int width,
            int height,
            float uiScale,
            float deltaSeconds,
            Consumer<UiRenderUtils> draw
    ) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(draw, "draw");

        float safeUiScale = sanitizeScale(uiScale);
        backend.viewport(width, height, safeUiScale);
        backend.attachContext(graphics);

        if (!frameCaching) {
            renderer.render(
                    backend,
                    new RenderFrame(width, height, safeUiScale, sanitizeDelta(deltaSeconds)),
                    theme,
                    scale,
                    textShadow,
                    draw
            );
            return;
        }

        boolean viewportChanged = width != cachedWidth
                || height != cachedHeight
                || Float.compare(safeUiScale, cachedUiScale) != 0;
        boolean rebuild = viewportChanged
                || !backend.hasCachedFrame()
                || renderedCacheRevision != cacheRevision;

        if (!rebuild) {
            backend.replayCachedFrame(graphics);
            return;
        }

        long revisionAtStart = cacheRevision;
        backend.recordNextFrame();
        renderer.render(
                backend,
                new RenderFrame(width, height, safeUiScale, sanitizeDelta(deltaSeconds)),
                theme,
                scale,
                textShadow,
                draw
        );
        cachedWidth = width;
        cachedHeight = height;
        cachedUiScale = safeUiScale;
        renderedCacheRevision = revisionAtStart;
    }

    private static float sanitizeScale(float scale) {
        if (!Float.isFinite(scale) || scale <= 0.0f) return 1.0f;
        return scale;
    }

    private static float sanitizeDelta(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) return 0.0f;
        return Math.min(deltaSeconds, 0.25f);
    }
}
