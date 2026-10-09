package dev.someoneok.crystalconfig.render;

import com.mojang.blaze3d.platform.InputConstants;
import dev.someoneok.crystalconfig.minecraft.MinecraftUiController;
import dev.someoneok.crystalconfig.ui.UiRoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public final class ConfigScreen extends Screen {
    private static final BooleanSupplier DEFAULT_TEXT_SHADOW_GETTER = () -> false;
    private static final IntSupplier DEFAULT_RENDER_FPS_GETTER = () -> 60;

    private final MinecraftUiController<GuiGraphicsExtractor> controller;
    private final IntSupplier renderFpsGetter;
    private final FabricUiRenderBackend fabricAdapter;
    private long lastCustomRenderNs;
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;
    private int lastRenderWidth = -1;
    private int lastRenderHeight = -1;
    private float lastRenderScale = Float.NaN;
    private long lastSettingsVersion = Long.MIN_VALUE;
    private boolean forceCustomRenderNextFrame = true;
    //? if >=26.3 {
    /*private boolean sdlTextInputFocused;
    *///?}
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
        BooleanSupplier resolvedTextShadowGetter = Objects.requireNonNull(textShadowGetter, "textShadowGetter");
        this.renderFpsGetter = Objects.requireNonNull(renderFpsGetter, "renderFpsGetter");
        this.fabricAdapter = new FabricUiRenderBackend()
                .textShadow(resolvedTextShadowGetter)
                .onClose(this::requestClose)
                .onOpenUrl(url -> Minecraft.getInstance().execute(() -> {
                    //? if >=26.3 {
                    /*ConfirmLinkScreen.confirmLinkNow(ConfigScreen.this, java.net.URI.create(url), false);
                    *///?} else {
                    ConfirmLinkScreen.confirmLinkNow(ConfigScreen.this, url, false);
                    //?}
                }));
        this.controller = new MinecraftUiController<>(root, fabricAdapter);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
        fabricAdapter.viewport(width, height, 1.0f);

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
        // Focus can also change from animation ticks, callbacks and state updates.
        syncTextInputFocus();
    }

    @Override
    public void onClose() {
        releaseTextInputFocus();
        controller.close();
        requestClose();
    }

    @Override
    public void removed() {
        releaseTextInputFocus();
        controller.close();
        super.removed();
    }

    private void requestClose() {
        //? if <26.2 {
        Minecraft.getInstance().setScreen(null);
        //?} else {
        /*Minecraft.getInstance().gui.setScreen(null);
        *///?}
    }

    // CrystalConfig owns its own component tree, so Minecraft's GuiEventListener
    // focus tracking cannot identify its nested custom editable widgets.
    private void syncTextInputFocus() {
        //? if >=26.3 {
        /*boolean focused = controller.root().hasActiveTextInput();
        if (focused != sdlTextInputFocused) {
            Minecraft.getInstance().textInputManager().onTextInputFocusChange(this, focused);
            sdlTextInputFocused = focused;
        }
        *///?}
    }

    private void releaseTextInputFocus() {
        //? if >=26.3 {
        /*if (sdlTextInputFocused) {
            Minecraft.getInstance().textInputManager().onTextInputFocusChange(this, false);
            sdlTextInputFocused = false;
        }
        *///?}
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        //? if >=26.3 {
        /*int keyCode = MinecraftInputCompat.keyboardKey(event.key(), event.keycode());
        int scanCode = event.key();
        *///?} else {
        int keyCode = event.key();
        int scanCode = event.scancode();
        //?}
        boolean consumed = controller.keyPressed(
                keyCode,
                scanCode,
                MinecraftInputCompat.modifiers(event.modifiers()),
                InputConstants.getKey(event).getDisplayName().getString()
        );
        syncTextInputFocus();
        return consumed || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        boolean consumed = controller.charTyped((char) event.codepoint(), 0);
        syncTextInputFocus();
        return consumed || super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        boolean consumed = controller.mouseClicked(
                event.x(), event.y(),
                MinecraftInputCompat.mouseButton(event.buttonInfo().button()),
                MinecraftInputCompat.modifiers(event.buttonInfo().modifiers())
        );
        syncTextInputFocus();
        return consumed || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        boolean consumed = controller.mouseReleased(
                event.x(), event.y(),
                MinecraftInputCompat.mouseButton(event.buttonInfo().button()),
                MinecraftInputCompat.modifiers(event.buttonInfo().modifiers())
        );
        syncTextInputFocus();
        return consumed || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        forceUncappedRenderUntilNs = Math.max(forceUncappedRenderUntilNs, System.nanoTime() + 250_000_000L);
        boolean consumed = controller.mouseDragged(
                event.x(), event.y(),
                MinecraftInputCompat.mouseButton(event.buttonInfo().button()),
                dx, dy,
                MinecraftInputCompat.modifiers(event.buttonInfo().modifiers())
        );
        syncTextInputFocus();
        return consumed || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        forceCustomRenderNextFrame = true;
        forceCustomRenderFrames = Math.max(forceCustomRenderFrames, 3);
        forceUncappedRenderUntilNs = Math.max(forceUncappedRenderUntilNs, System.nanoTime() + 350_000_000L);
        boolean consumed = controller.mouseScrolled(x, y, scrollX, scrollY);
        syncTextInputFocus();
        return consumed || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private boolean shouldRenderCustomUi(long now) {
        float scale = fabricAdapter.uiScale();
        boolean viewportChanged = width != lastRenderWidth || height != lastRenderHeight || Float.compare(scale, lastRenderScale) != 0;
        boolean settingsChanged = controller.root().settings().version() != lastSettingsVersion;
        boolean needsFreshRender = controller.root().root().needsFreshRender();

        boolean forceUncapped = forceUncappedRenderUntilNs > 0L && now <= forceUncappedRenderUntilNs;
        if (!fabricAdapter.hasCachedFrame() || forceCustomRenderNextFrame || forceCustomRenderFrames > 0 || forceUncapped || viewportChanged || settingsChanged || needsFreshRender) {
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
        lastSettingsVersion = controller.root().settings().version();
    }

}
