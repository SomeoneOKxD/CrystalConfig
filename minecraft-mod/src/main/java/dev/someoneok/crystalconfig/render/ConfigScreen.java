package dev.someoneok.crystalconfig.render;

import com.mojang.blaze3d.platform.InputConstants;
import dev.someoneok.crystalconfig.minecraft.MinecraftUiAdapter;
import dev.someoneok.crystalconfig.minecraft.MinecraftUiController;
import dev.someoneok.crystalconfig.render.text.MsdfTextRenderer;
import dev.someoneok.crystalconfig.ui.UiRoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public final class ConfigScreen extends Screen {
    private static final BooleanSupplier DEFAULT_TEXT_SHADOW_GETTER = () -> false;
    private static final IntSupplier DEFAULT_RENDER_FPS_GETTER = () -> 60;

    private final MinecraftUiController<GuiGraphicsExtractor> controller;
    private final BooleanSupplier textShadowGetter;
    private final IntSupplier renderFpsGetter;
    private final FabricAdapter fabricAdapter;
    private long lastCustomRenderNs;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private int lastRenderWidth = -1;
    private int lastRenderHeight = -1;
    private float lastRenderScale = Float.NaN;
    private boolean forceCustomRenderNextFrame = true;
    private int forceCustomRenderFrames = 0;
    private long forceUncappedRenderUntilNs = 0L;

    public ConfigScreen(UiRoot root, String title) {
        this(root, title, DEFAULT_TEXT_SHADOW_GETTER, DEFAULT_RENDER_FPS_GETTER);
    }

    public ConfigScreen(UiRoot root, String title, BooleanSupplier textShadowGetter) {
        this(root, title, textShadowGetter, DEFAULT_RENDER_FPS_GETTER);
    }

    public ConfigScreen(UiRoot root, String title, IntSupplier renderFpsGetter) {
        this(root, title, DEFAULT_TEXT_SHADOW_GETTER, renderFpsGetter);
    }

    public ConfigScreen(UiRoot root, String title, BooleanSupplier textShadowGetter, IntSupplier renderFpsGetter) {
        super(Component.literal(title));
        this.textShadowGetter = Objects.requireNonNull(textShadowGetter, "textShadowGetter");
        this.renderFpsGetter = Objects.requireNonNull(renderFpsGetter, "renderFpsGetter");
        this.fabricAdapter = new FabricAdapter();
        this.controller = new MinecraftUiController<>(root, fabricAdapter);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);

        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            controller.mouseMoved(mouseX, mouseY);
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            forceCustomRenderNextFrame = true;
            forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 2);
        }

        long now = System.nanoTime();
        if (shouldRenderCustomUi(now)) {
            float deltaSeconds = animationDeltaSeconds(now, tickDelta);
            fabricAdapter.recordNextFrame();
            controller.render(graphics, deltaSeconds);
            markRenderedNow(fabricAdapter.uiScale(), now);
            forceCustomRenderNextFrame = false;
            if (forceCustomRenderFrames > 0) forceCustomRenderFrames--;
        } else {
            fabricAdapter.replayCachedFrame(graphics);
        }
    }

    @Override
    public void onClose() {
        controller.close();
        requestClose();
    }

    @Override
    public void removed() {
        controller.close();
        super.removed();
    }

    private void requestClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        return controller.keyPressed(
                event.key(),
                event.scancode(),
                event.modifiers(),
                InputConstants.getKey(event).getDisplayName().getString()
        ) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        return controller.charTyped((char) event.codepoint(), 0)
                || super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        return controller.mouseClicked(
                event.x(),
                event.y(),
                event.buttonInfo().button(),
                event.buttonInfo().modifiers()
        ) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        return controller.mouseReleased(
                event.x(),
                event.y(),
                event.buttonInfo().button(),
                event.buttonInfo().modifiers()
        ) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        forceUncappedRenderUntilNs = Math.max(forceUncappedRenderUntilNs, System.nanoTime() + 250_000_000L);
        return controller.mouseDragged(
                event.x(),
                event.y(),
                event.buttonInfo().button(),
                dx,
                dy,
                event.buttonInfo().modifiers()
        ) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        forceUncappedRenderUntilNs = Math.max(forceUncappedRenderUntilNs, System.nanoTime() + 350_000_000L);
        return controller.mouseScrolled(x, y, scrollX, scrollY)
                || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private boolean shouldRenderCustomUi(long now) {
        float scale = fabricAdapter.uiScale();
        boolean viewportChanged = width != lastRenderWidth || height != lastRenderHeight || Float.compare(scale, lastRenderScale) != 0;
        boolean needsFreshRender = controller.root().root().needsFreshRender();

        boolean forceUncapped = forceUncappedRenderUntilNs > 0L && now <= forceUncappedRenderUntilNs;
        if (!fabricAdapter.hasCachedFrame() || forceCustomRenderNextFrame || forceCustomRenderFrames > 0 || forceUncapped || viewportChanged || needsFreshRender) {
            return true;
        }

        int fps = renderFpsGetter.getAsInt();
        if (fps <= 0) return true;

        long frameNs = 1_000_000_000L / Math.max(1, fps);
        return lastCustomRenderNs == 0L || now - lastCustomRenderNs >= frameNs;
    }

    private float animationDeltaSeconds(long now, float tickDelta) {
        int fps = renderFpsGetter.getAsInt();
        float fallback = fps > 0 ? 1.0f / Math.max(1, fps) : Math.max(1.0f / 240.0f, tickDelta / 20.0f);
        if (lastCustomRenderNs == 0L) return fallback;

        float elapsed = (now - lastCustomRenderNs) / 1_000_000_000.0f;
        if (Float.isNaN(elapsed) || Float.isInfinite(elapsed) || elapsed <= 0.0f) return fallback;

        return Math.max(1.0f / 240.0f, Math.min(0.10f, elapsed));
    }

    private void markRenderedNow(float scale, long now) {
        lastCustomRenderNs = now;
        lastRenderWidth = width;
        lastRenderHeight = height;
        lastRenderScale = scale;
    }

    private final class FabricAdapter implements MinecraftUiAdapter<GuiGraphicsExtractor> {
        private GuiGraphicsExtractor graphics;
        private boolean clipActive = false;
        private Rect activeClip;
        private boolean recording;
        private boolean replaying;
        private final List<RenderOp> cachedOps = new ArrayList<>(512);
        private final List<RenderOp> frameOps = new ArrayList<>(512);

        boolean hasCachedFrame() {
            return !cachedOps.isEmpty();
        }

        void recordNextFrame() {
            recording = true;
            frameOps.clear();
        }

        void replayCachedFrame(GuiGraphicsExtractor graphics) {
            attachContext(graphics);
            replaying = true;
            clipActive = false;
            activeClip = null;
            try {
                for (RenderOp op : cachedOps) op.apply(this);
            } finally {
                if (clipActive) {
                    graphics.disableScissor();
                    clipActive = false;
                    activeClip = null;
                }
                replaying = false;
            }
        }

        @Override
        public void attachContext(GuiGraphicsExtractor graphics) {
            this.graphics = graphics;
        }

        @Override
        public int scaledWidth() {
            return width;
        }

        @Override
        public int scaledHeight() {
            return height;
        }

        @Override
        public float uiScale() {
            return 1.0f;
        }

        @Override
        public String getClipboard() {
            return Minecraft.getInstance().keyboardHandler.getClipboard();
        }

        @Override
        public void setClipboard(String value) {
            Minecraft.getInstance().keyboardHandler.setClipboard(value);
        }

        @Override
        public void closeScreen() {
            requestClose();
        }

        @Override
        public void openUrl(String url) {
            if (url == null || url.isBlank()) return;
            Minecraft.getInstance().execute(() -> ConfirmLinkScreen.confirmLinkNow(ConfigScreen.this, url.trim(), false));
        }

        @Override
        public void beginFrame(RenderFrame frame) {
            clipActive = false;
            activeClip = null;
            if (recording && !replaying) frameOps.clear();
        }

        @Override
        public void endFrame() {
            if (clipActive) {
                graphics.disableScissor();
                clipActive = false;
                activeClip = null;
            }
            if (recording && !replaying) {
                cachedOps.clear();
                cachedOps.addAll(frameOps);
                frameOps.clear();
                recording = false;
            }
        }

        @Override
        public TextMetrics measureText(String text, float fontSize, String fontFace) {
            return MsdfTextRenderer.get().measureText(text, fontSize, fontFace);
        }

        @Override
        public void setClip(Rect clip) {
            if (recording && !replaying) frameOps.add(adapter -> adapter.setClipRaw(clip));
            setClipRaw(clip);
        }

        private void setClipRaw(Rect clip) {
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
            if (recording && !replaying) frameOps.add(FabricAdapter::clearClipRaw);
            clearClipRaw();
        }

        private void clearClipRaw() {
            if (clipActive) {
                graphics.disableScissor();
                clipActive = false;
                activeClip = null;
            }
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
            if (material == Material.SDF_RECT || material == Material.CHECKERBOARD) {
                drawSdfRectBatch(batch);
                return;
            }

            for (QuadCommand q : batch) {
                if (material == Material.HSV_SV) {
                    drawSvGradient(q);
                } else if (material == Material.HSV_HUE) {
                    drawHueGradient(q);
                } else {
                    drawSdfRectBatch(batch);
                    return;
                }
            }
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

        private void collectRect(List<SdfRectBatchRenderState.RectQuad> rects, Rect rect, ColorRGBA fill, ColorRGBA border, float radius, float borderWidth, float opacity) {
            if (borderWidth > 0 && border.a() > 0 && fill.a() == 0) {
                collectThinBorder(rects, rect, border, borderWidth, opacity);
                return;
            }

            if (borderWidth > 0 && border.a() > 0) {
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
            rects.add(new SdfRectBatchRenderState.RectQuad(
                    rect.x(), rect.y(), rect.w(), rect.h(), argb, radius
            ));
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
            float r1, g1, b1;
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

        private interface RenderOp {
            void apply(FabricAdapter adapter);
        }
    }
}
