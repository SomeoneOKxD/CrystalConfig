package dev.someoneok.crystalconfig.render;

import dev.someoneok.crystalconfig.minecraft.MinecraftUiAdapter;
import dev.someoneok.crystalconfig.render.text.MsdfTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Internal reusable Fabric/Minecraft implementation of CrystalConfig's renderer-neutral backend.
 *
 * <p>This class owns no UI state. A host supplies the current viewport and draw context before
 * rendering. Optional frame recording is used by {@link ConfigScreen} to replay unchanged frames.</p>
 */
final class FabricUiRenderBackend implements MinecraftUiAdapter<GuiGraphicsExtractor> {
    private static final BooleanSupplier NO_TEXT_SHADOW = () -> false;
    private static final Runnable NO_CLOSE_HANDLER = () -> { };
    private static final Consumer<String> NO_URL_HANDLER = url -> { };

    private GuiGraphicsExtractor graphics;
    private int scaledWidth;
    private int scaledHeight;
    private float uiScale = 1.0f;
    private BooleanSupplier textShadowGetter = NO_TEXT_SHADOW;
    private Runnable closeHandler = NO_CLOSE_HANDLER;
    private Consumer<String> urlHandler = NO_URL_HANDLER;

    private boolean clipActive;
    private Rect activeClip;
    private boolean recording;
    private boolean replaying;
    private boolean cachedFrameValid;
    private final List<RenderOp> cachedOps = new ArrayList<>(512);
    private final List<RenderOp> frameOps = new ArrayList<>(512);
    private final Map<TextureRef, Identifier> textureIdentifiers = new HashMap<>(16);

    public FabricUiRenderBackend viewport(int width, int height, float uiScale) {
        this.scaledWidth = Math.max(0, width);
        this.scaledHeight = Math.max(0, height);
        this.uiScale = sanitizeScale(uiScale);
        return this;
    }

    public FabricUiRenderBackend textShadow(BooleanSupplier getter) {
        this.textShadowGetter = getter == null ? NO_TEXT_SHADOW : getter;
        return this;
    }

    public FabricUiRenderBackend onClose(Runnable handler) {
        this.closeHandler = handler == null ? NO_CLOSE_HANDLER : handler;
        return this;
    }

    public FabricUiRenderBackend onOpenUrl(Consumer<String> handler) {
        this.urlHandler = handler == null ? NO_URL_HANDLER : handler;
        return this;
    }

    public boolean hasCachedFrame() {
        return cachedFrameValid;
    }

    public void recordNextFrame() {
        recording = true;
        frameOps.clear();
    }

    public void clearCachedFrame() {
        cachedOps.clear();
        frameOps.clear();
        cachedFrameValid = false;
        recording = false;
    }

    public void replayCachedFrame(GuiGraphicsExtractor graphics) {
        attachContext(graphics);
        replaying = true;
        clipActive = false;
        activeClip = null;
        try {
            for (RenderOp op : cachedOps) op.apply(this);
        } finally {
            clearClipRaw();
            replaying = false;
        }
    }

    @Override
    public void attachContext(GuiGraphicsExtractor graphics) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
    }

    @Override
    public int scaledWidth() {
        return scaledWidth;
    }

    @Override
    public int scaledHeight() {
        return scaledHeight;
    }

    @Override
    public float uiScale() {
        return uiScale;
    }

    @Override
    public String getClipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String value) {
        Minecraft.getInstance().keyboardHandler.setClipboard(value == null ? "" : value);
    }

    @Override
    public void closeScreen() {
        closeHandler.run();
    }

    @Override
    public void openUrl(String url) {
        if (url == null || url.isBlank()) return;
        urlHandler.accept(url.trim());
    }

    @Override
    public void beginFrame(RenderFrame frame) {
        requireGraphics();
        clipActive = false;
        activeClip = null;
        if (recording && !replaying) frameOps.clear();
    }

    @Override
    public void endFrame() {
        clearClipRaw();
        if (recording && !replaying) {
            cachedOps.clear();
            cachedOps.addAll(frameOps);
            frameOps.clear();
            cachedFrameValid = true;
            recording = false;
        }
    }

    @Override
    public TextMetrics measureText(String text, float fontSize, String fontFace) {
        return MsdfTextRenderer.get().measureText(text, fontSize, fontFace);
    }

    @Override
    public void setClip(Rect clip) {
        Objects.requireNonNull(clip, "clip");
        if (recording && !replaying) frameOps.add(adapter -> adapter.setClipRaw(clip));
        setClipRaw(clip);
    }

    private void setClipRaw(Rect clip) {
        requireGraphics();
        if (clipActive && sameClip(activeClip, clip)) return;
        if (clipActive) graphics.disableScissor();

        graphics.enableScissor(
                Math.round(clip.x()),
                Math.round(clip.y()),
                Math.round(clip.right()),
                Math.round(clip.bottom())
        );

        clipActive = true;
        activeClip = clip;
    }

    @Override
    public void clearClip() {
        if (recording && !replaying) frameOps.add(FabricUiRenderBackend::clearClipRaw);
        clearClipRaw();
    }

    private void clearClipRaw() {
        if (clipActive && graphics != null) graphics.disableScissor();
        clipActive = false;
        activeClip = null;
    }

    @Override
    public void drawQuads(Material material, List<QuadCommand> batch) {
        if (recording && !replaying) {
            List<QuadCommand> copy = new ArrayList<>(batch);
            frameOps.add(adapter -> adapter.drawQuadsRaw(material, copy));
        }
        drawQuadsRaw(material, batch);
    }

    private void drawQuadsRaw(Material material, List<QuadCommand> batch) {
        requireGraphics();
        if (material == Material.SDF_RECT || material == Material.CHECKERBOARD) {
            drawSdfRectBatch(batch);
            return;
        }
        if (material == Material.SDF_LINE) {
            drawLineBatch(batch);
            return;
        }
        if (material == Material.GRADIENT) {
            drawGradientBatch(batch);
            return;
        }
        if (material == Material.TEXTURE) {
            drawTextureBatch(batch);
            return;
        }

        for (QuadCommand q : batch) {
            if (material == Material.HSV_SV) {
                drawSvGradient(q);
            } else if (material == Material.HSV_HUE) {
                drawHueGradient(q);
            }
        }
    }

    private void drawLineBatch(List<QuadCommand> batch) {
        List<SdfLineBatchRenderState.LineQuad> lines = new ArrayList<>(batch.size());
        for (QuadCommand q : batch) {
            float thickness = q.data0;
            if (thickness <= 0.0f || q.fill.a() == 0) continue;
            lines.add(new SdfLineBatchRenderState.LineQuad(
                    q.rect.x(),
                    q.rect.y(),
                    q.rect.w(),
                    q.rect.h(),
                    thickness,
                    q.fill.toArgb(),
                    q.data1 > 0.5f
            ));
        }
        if (lines.isEmpty()) return;
        graphics.guiRenderState.addGuiElement(new SdfLineBatchRenderState(
                new Matrix3x2f(graphics.pose()),
                lines,
                graphics.scissorStack.peek()
        ));
    }

    private void drawGradientBatch(List<QuadCommand> batch) {
        List<GradientRectBatchRenderState.GradientQuad> gradients = new ArrayList<>(batch.size());
        for (QuadCommand q : batch) {
            if (q.rect.w() <= 0.0f || q.rect.h() <= 0.0f) continue;
            gradients.add(new GradientRectBatchRenderState.GradientQuad(
                    q.rect.x(),
                    q.rect.y(),
                    q.rect.w(),
                    q.rect.h(),
                    q.fill.toArgb(),
                    q.border.toArgb(),
                    q.data0 > 0.5f
            ));
        }
        if (gradients.isEmpty()) return;
        graphics.guiRenderState.addGuiElement(new GradientRectBatchRenderState(
                new Matrix3x2f(graphics.pose()),
                gradients,
                graphics.scissorStack.peek()
        ));
    }

    /**
     * Emits one GUI textured render state for each consecutive texture/filter run. This preserves
     * draw order while collapsing the common many-sprites-from-one-atlas case into a single state.
     */
    private void drawTextureBatch(List<QuadCommand> batch) {
        TextureRef activeTexture = null;
        TextureFilter activeFilter = null;
        List<TextureBatchRenderState.TextureQuad> run = null;

        for (QuadCommand q : batch) {
            if (q.texture == null || q.rect.w() <= 0.0f || q.rect.h() <= 0.0f || q.fill.a() == 0) continue;

            if (!Objects.equals(activeTexture, q.texture) || activeFilter != q.textureFilter) {
                flushTextureRun(activeTexture, activeFilter, run);
                activeTexture = q.texture;
                activeFilter = q.textureFilter;
                run = new ArrayList<>(Math.min(64, batch.size()));
            }

            Rect uv = q.uv;
            run.add(new TextureBatchRenderState.TextureQuad(
                    q.rect.x(),
                    q.rect.y(),
                    q.rect.w(),
                    q.rect.h(),
                    uv.x(),
                    uv.y(),
                    uv.x() + uv.w(),
                    uv.y() + uv.h(),
                    q.fill.toArgb()
            ));
        }

        flushTextureRun(activeTexture, activeFilter, run);
    }

    private void flushTextureRun(
            TextureRef texture,
            TextureFilter filter,
            List<TextureBatchRenderState.TextureQuad> run
    ) {
        if (texture == null || run == null || run.isEmpty()) return;
        Identifier identifier = textureIdentifiers.computeIfAbsent(
                texture,
                ref -> Identifier.fromNamespaceAndPath(ref.namespace(), ref.path())
        );
        graphics.guiRenderState.addGuiElement(new TextureBatchRenderState(
                new Matrix3x2f(graphics.pose()),
                identifier,
                filter == null ? TextureFilter.NEAREST : filter,
                run,
                graphics.scissorStack.peek()
        ));
    }

    private void drawSdfRectBatch(List<QuadCommand> batch) {
        List<SdfRectBatchRenderState.RectQuad> rects = new ArrayList<>(batch.size());

        for (QuadCommand q : batch) {
            collectRect(rects, q.rect, q.fill, q.border, q.radius, q.borderWidth, q.opacity);
        }

        if (rects.isEmpty()) return;

        graphics.guiRenderState.addGuiElement(new SdfRectBatchRenderState(
                new Matrix3x2f(graphics.pose()),
                rects,
                graphics.scissorStack.peek()
        ));
    }

    private void collectRect(
            List<SdfRectBatchRenderState.RectQuad> rects,
            Rect rect,
            ColorRGBA fill,
            ColorRGBA border,
            float radius,
            float borderWidth,
            float opacity
    ) {
        if (borderWidth > 0.0f && border.a() > 0 && fill.a() == 0) {
            collectThinBorder(rects, rect, border, borderWidth, opacity);
            return;
        }

        if (borderWidth > 0.0f && border.a() > 0) {
            collectSolidRect(rects, rect, border, radius, opacity);
            collectSolidRect(
                    rects,
                    new Rect(
                            rect.x() + borderWidth,
                            rect.y() + borderWidth,
                            Math.max(0.0f, rect.w() - borderWidth * 2.0f),
                            Math.max(0.0f, rect.h() - borderWidth * 2.0f)
                    ),
                    fill,
                    Math.max(0.0f, radius - borderWidth),
                    opacity
            );
        } else {
            collectSolidRect(rects, rect, fill, radius, opacity);
        }
    }

    private void collectThinBorder(List<SdfRectBatchRenderState.RectQuad> rects, Rect rect, ColorRGBA color, float width, float opacity) {
        float bw = Math.max(1.0f, width);
        collectSolidRect(rects, new Rect(rect.x(), rect.y(), rect.w(), bw), color, 0.0f, opacity);
        collectSolidRect(rects, new Rect(rect.x(), rect.bottom() - bw, rect.w(), bw), color, 0.0f, opacity);
        collectSolidRect(rects, new Rect(rect.x(), rect.y(), bw, rect.h()), color, 0.0f, opacity);
        collectSolidRect(rects, new Rect(rect.right() - bw, rect.y(), bw, rect.h()), color, 0.0f, opacity);
    }

    private void collectSolidRect(List<SdfRectBatchRenderState.RectQuad> rects, Rect rect, ColorRGBA fill, float radius, float opacity) {
        if (rect.w() <= 0.0f || rect.h() <= 0.0f || fill.a() == 0 || opacity <= 0.0f) return;
        int argb = fill.multiplyAlpha(opacity).toArgb();
        rects.add(new SdfRectBatchRenderState.RectQuad(rect.x(), rect.y(), rect.w(), rect.h(), argb, radius));
    }

    private void drawHueGradient(QuadCommand q) {
        if (q.rect.w() <= 0.0f || q.rect.h() <= 0.0f || q.opacity <= 0.0f) return;

        final int segments = 12;
        final float x = q.rect.x();
        final float w = q.rect.w();
        final float y = q.rect.y();
        final float h = q.rect.h();
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        var scissor = graphics.scissorStack.peek();

        for (int i = 0; i < segments; i++) {
            float y0 = y + h * i / segments;
            float y1 = y + h * (i + 1) / segments;
            int c0 = hsvArgb(360.0f * i / segments, 1.0f, 1.0f, q.opacity);
            int c1 = hsvArgb(360.0f * (i + 1) / segments, 1.0f, 1.0f, q.opacity);
            graphics.guiRenderState.addGuiElement(new GradientRectRenderState(
                    pose, x, y0, w, Math.max(1.0f, y1 - y0), c0, c0, c1, c1, scissor
            ));
        }
    }

    private void drawSvGradient(QuadCommand q) {
        if (q.rect.w() <= 0.0f || q.rect.h() <= 0.0f || q.opacity <= 0.0f) return;

        float hue = q.data0;
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        var scissor = graphics.scissorStack.peek();
        float x = q.rect.x();
        float y = q.rect.y();
        float w = q.rect.w();
        float h = q.rect.h();

        int hueColor = hsvArgb(hue, 1.0f, 1.0f, q.opacity);
        int white = ColorRGBA.WHITE.multiplyAlpha(q.opacity).toArgb();
        int transparentWhite = ColorRGBA.WHITE.withAlpha(0).toArgb();
        int black = ColorRGBA.BLACK.multiplyAlpha(q.opacity).toArgb();
        int transparentBlack = ColorRGBA.BLACK.withAlpha(0).toArgb();

        graphics.guiRenderState.addGuiElement(new GradientRectRenderState(
                pose, x, y, w, h, hueColor, hueColor, hueColor, hueColor, scissor
        ));
        graphics.guiRenderState.addGuiElement(new GradientRectRenderState(
                pose, x, y, w, h, white, transparentWhite, transparentWhite, white, scissor
        ));
        graphics.guiRenderState.addGuiElement(new GradientRectRenderState(
                pose, x, y, w, h, transparentBlack, transparentBlack, black, black, scissor
        ));
    }

    private int hsvArgb(float h, float s, float v, float opacity) {
        return hsv(h, s, v, Math.round(255.0f * opacity)).toArgb();
    }

    private ColorRGBA hsv(float h, float s, float v, int alpha) {
        h = ((h % 360.0f) + 360.0f) % 360.0f;
        float c = v * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - c;
        float r1;
        float g1;
        float b1;
        if (h < 60.0f) { r1 = c; g1 = x; b1 = 0.0f; }
        else if (h < 120.0f) { r1 = x; g1 = c; b1 = 0.0f; }
        else if (h < 180.0f) { r1 = 0.0f; g1 = c; b1 = x; }
        else if (h < 240.0f) { r1 = 0.0f; g1 = x; b1 = c; }
        else if (h < 300.0f) { r1 = x; g1 = 0.0f; b1 = c; }
        else { r1 = c; g1 = 0.0f; b1 = x; }
        return ColorRGBA.rgba(
                Math.round((r1 + m) * 255.0f),
                Math.round((g1 + m) * 255.0f),
                Math.round((b1 + m) * 255.0f),
                alpha
        );
    }

    private boolean sameClip(Rect a, Rect b) {
        return a != null && b != null
                && Math.round(a.x()) == Math.round(b.x())
                && Math.round(a.y()) == Math.round(b.y())
                && Math.round(a.right()) == Math.round(b.right())
                && Math.round(a.bottom()) == Math.round(b.bottom());
    }

    @Override
    public void drawText(TextCommand command) {
        if (recording && !replaying) frameOps.add(adapter -> adapter.drawTextRaw(command));
        drawTextRaw(command);
    }

    private void drawTextRaw(TextCommand command) {
        requireGraphics();
        MsdfTextRenderer.get().drawText(
                graphics,
                command.text,
                command.x,
                command.y,
                command.fontSize,
                command.fontFace,
                command.color,
                command.opacity,
                command.shadow || textShadowGetter.getAsBoolean()
        );
    }

    private void requireGraphics() {
        if (graphics == null) throw new IllegalStateException("No GuiGraphicsExtractor is attached to the render backend");
    }

    private static float sanitizeScale(float scale) {
        if (!Float.isFinite(scale) || scale <= 0.0f) return 1.0f;
        return scale;
    }

    private interface RenderOp {
        void apply(FabricUiRenderBackend adapter);
    }
}
