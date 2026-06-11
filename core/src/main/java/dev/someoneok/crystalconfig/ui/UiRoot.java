package dev.someoneok.crystalconfig.ui;

import dev.someoneok.crystalconfig.config.ConfigUiSettings;
import dev.someoneok.crystalconfig.input.*;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.render.*;
import dev.someoneok.crystalconfig.theme.Theme;
import dev.someoneok.crystalconfig.utils.UiClipboard;
import dev.someoneok.crystalconfig.utils.UiUrlOpener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class UiRoot {
    private final Component root;
    private final ConfigUiSettings settings;
    private final Supplier<Theme> theme;
    private final List<Runnable> closeCallbacks = new ArrayList<>();
    private boolean closeCallbacksFired;
    private final TooltipManager tooltipManager = new TooltipManager();
    private Component hovered;
    private final List<Component> hoverPath = new ArrayList<>();
    private Component focused;
    private Component captured;
    private float mouseX;
    private float mouseY;
    private boolean debugBounds;
    private float globalScale = 1.0f;
    private float lastEffectiveScale = 1.0f;
    private final DrawList drawList = new DrawList();
    private int lastLayoutWidth = -1;
    private int lastLayoutHeight = -1;
    private float lastLayoutScale = -1.0f;

    public UiRoot(Component root) {
        this(root, ConfigUiSettings.create());
    }

    public UiRoot(Component root, ConfigUiSettings settings) {
        this.root = root;
        this.settings = settings == null ? ConfigUiSettings.create() : settings;
        this.theme = this.settings::defaultTheme;
    }

    public Component root() {
        return root;
    }

    public ConfigUiSettings settings() {
        return settings;
    }

    public UiRoot debugBounds(boolean debugBounds) {
        this.debugBounds = debugBounds;
        return this;
    }

    /** Additional app-controlled scale multiplied with the scale passed to {@link #render}. */
    public UiRoot globalScale(float globalScale) {
        float next = clampScale(globalScale);
        if (this.globalScale != next) {
            this.globalScale = next;
            root.markLayoutDirty();
        }
        return this;
    }

    public float globalScale() {
        return globalScale;
    }

    public Component focused() {
        return focused;
    }

    public Component hovered() {
        return hovered;
    }

    /**
     * Register a callback that is invoked once when the hosting config screen closes.
     * This is intentionally runtime/builder friendly and does not require autoconfig annotations.
     */
    public UiRoot onClose(Runnable callback) {
        if (callback != null) closeCallbacks.add(callback);
        return this;
    }

    /**
     * Notify the UI root that its host screen closed. Callbacks are fired at most once per root instance.
     */
    public void close() {
        if (closeCallbacksFired) return;
        closeCallbacksFired = true;
        for (Runnable callback : List.copyOf(closeCallbacks)) {
            try {
                callback.run();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    public void render(RenderBackend backend, int width, int height, float uiScale, float deltaSeconds) {
        UiClipboard.bindBackend(backend);
        UiUrlOpener.bindBackend(backend);
        lastEffectiveScale = clampScale(uiScale) * globalScale * clampScale((float) settings.scale());
        Theme activeTheme = settings.resolveTheme(theme.get());
        backend.beginFrame(new RenderFrame(width, height, uiScale, deltaSeconds));
        drawList.clear();
        LayoutContext layoutContext = new LayoutContext(activeTheme, backend);
        RenderContext renderContext = new RenderContext(drawList, backend, activeTheme, deltaSeconds, debugBounds, lastEffectiveScale, settings.textShadow());

        float logicalWidth = width / lastEffectiveScale;
        float logicalHeight = height / lastEffectiveScale;
        root.tick(deltaSeconds);
        boolean layoutChanged = width != lastLayoutWidth || height != lastLayoutHeight || lastEffectiveScale != lastLayoutScale || root.layoutDirty();
        if (layoutChanged) {
            root.layout(layoutContext, new Rect(0, 0, logicalWidth, logicalHeight));
            root.clearLayoutDirtyDeep();
            lastLayoutWidth = width;
            lastLayoutHeight = height;
            lastLayoutScale = lastEffectiveScale;
        }
        root.render(renderContext);
        root.renderOverlay(renderContext);
        tooltipManager.update(hovered, deltaSeconds);
        tooltipManager.render(renderContext, mouseX, mouseY, Math.round(logicalWidth), Math.round(logicalHeight));
        drawList.flush(backend);
        backend.endFrame();
    }

    public boolean mouseMoved(float x, float y) {
        x /= lastEffectiveScale;
        y /= lastEffectiveScale;
        float dx = x - mouseX;
        float dy = y - mouseY;
        mouseX = x;
        mouseY = y;
        Component hit = root.hitTest(x, y);
        setHovered(hit);
        final float eventX = x;
        final float eventY = y;
        final float eventDx = dx;
        final float eventDy = dy;
        if (hit != null) {
            bubble(hit, c -> c.onMouseMove(new MouseMoveEvent(eventX, eventY, eventDx, eventDy)));
            return true;
        }
        return false;
    }

    public boolean mousePressed(float x, float y, MouseButton button, int modifiers) {
        return mousePressed(x, y, button, -1, modifiers);
    }

    public boolean mousePressed(float x, float y, MouseButton button, int rawButton, int modifiers) {
        x /= lastEffectiveScale;
        y /= lastEffectiveScale;
        mouseX = x;
        mouseY = y;
        Component hit = root.hitTest(x, y);
        setHovered(hit);
        if (hit == null) {
            setFocused(null);
            return false;
        }
        if (hit.focusable()) setFocused(hit);
        else if (!isFocusedOrDescendant(hit)) setFocused(null);
        final float eventX = x;
        final float eventY = y;
        MouseButtonEvent pressedEvent = new MouseButtonEvent(eventX, eventY, button, rawButton, modifiers);
        boolean captureConsumed = capture(hit, c -> c.onMousePressedCapture(pressedEvent));
        boolean consumed = captureConsumed || bubble(hit, c -> c.onMousePressed(pressedEvent));
        captured = hit;
        return consumed || hit != null;
    }

    public boolean mouseReleased(float x, float y, MouseButton button, int modifiers) {
        return mouseReleased(x, y, button, -1, modifiers);
    }

    public boolean mouseReleased(float x, float y, MouseButton button, int rawButton, int modifiers) {
        x /= lastEffectiveScale;
        y /= lastEffectiveScale;
        mouseX = x;
        mouseY = y;
        Component target = captured != null ? captured : root.hitTest(x, y);
        final float eventX = x;
        final float eventY = y;
        boolean consumed = false;
        if (target != null) {
            consumed = bubble(target, c -> c.onMouseReleased(new MouseButtonEvent(eventX, eventY, button, rawButton, modifiers)));
        }
        captured = null;
        setHovered(root.hitTest(x, y));
        return consumed || target != null;
    }

    public boolean mouseDragged(float x, float y, float dx, float dy, MouseButton button, int modifiers) {
        x /= lastEffectiveScale;
        y /= lastEffectiveScale;
        dx /= lastEffectiveScale;
        dy /= lastEffectiveScale;
        mouseX = x;
        mouseY = y;
        Component target = captured != null ? captured : root.hitTest(x, y);
        final float eventX = x;
        final float eventY = y;
        final float eventDx = dx;
        final float eventDy = dy;
        if (target != null) {
            bubble(target, c -> c.onMouseDragged(new MouseDragEvent(eventX, eventY, eventDx, eventDy, button, modifiers)));
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(float x, float y, float amountX, float amountY) {
        x /= lastEffectiveScale;
        y /= lastEffectiveScale;
        mouseX = x;
        mouseY = y;
        Component hit = root.hitTest(x, y);
        setHovered(hit);
        final float eventX = x;
        final float eventY = y;
        if (hit != null) {
            bubble(hit, c -> c.onMouseScrolled(new MouseScrollEvent(eventX, eventY, amountX, amountY)));
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers, String displayName) {
        if (focused != null) {
            if (!isInteractable(focused)) {
                setFocused(null);
                return false;
            }
            return bubble(focused, c -> c.onKeyPressed(new KeyEvent(keyCode, scanCode, modifiers, displayName)));
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (focused != null) {
            if (!isInteractable(focused)) {
                setFocused(null);
                return false;
            }
            return bubble(focused, c -> c.onCharTyped(new CharTypedEvent(codePoint, modifiers)));
        }
        return false;
    }

    public void setFocused(Component next) {
        if (focused == next) return;
        if (focused != null) focused.setFocused(false);
        focused = next;
        if (focused != null) focused.setFocused(true);
    }

    private void setHovered(Component next) {
        if (hovered == next && hoverPathMatches(next)) return;

        for (Component component : hoverPath) {
            component.setHovered(false);
        }
        hoverPath.clear();

        hovered = next;
        for (Component c = next; c != null; c = c.parent()) {
            hoverPath.add(c);
        }
        for (Component component : hoverPath) {
            component.setHovered(true);
        }
    }

    private boolean hoverPathMatches(Component next) {
        if (hoverPath.isEmpty()) return next == null;
        Component c = next;
        int index = 0;
        while (c != null && index < hoverPath.size()) {
            if (hoverPath.get(index) != c) return false;
            c = c.parent();
            index++;
        }
        return c == null && index == hoverPath.size();
    }

    private boolean bubble(Component start, Function<Component, Boolean> handler) {
        for (Component c = start; c != null; c = c.parent()) {
            if (handler.apply(c)) return true;
        }
        return false;
    }

    private boolean capture(Component target, Function<Component, Boolean> handler) {
        java.util.ArrayList<Component> path = new java.util.ArrayList<>();
        for (Component c = target; c != null; c = c.parent()) path.add(0, c);
        for (Component c : path) {
            if (handler.apply(c)) return true;
        }
        return false;
    }

    private float clampScale(float scale) {
        if (Float.isNaN(scale) || Float.isInfinite(scale)) return 1.0f;
        return Math.max(0.5f, Math.min(3.0f, scale));
    }

    private boolean isFocusedOrDescendant(Component component) {
        if (focused == null || component == null) return false;
        for (Component c = component; c != null; c = c.parent()) {
            if (c == focused) return true;
        }
        return false;
    }

    private boolean isInteractable(Component component) {
        for (Component c = component; c != null; c = c.parent()) {
            if (!c.visible() || !c.enabled()) return false;
        }
        return true;
    }
}
