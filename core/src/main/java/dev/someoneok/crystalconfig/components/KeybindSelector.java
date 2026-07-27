package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.input.KeyCodes;
import dev.someoneok.crystalconfig.input.KeyEvent;
import dev.someoneok.crystalconfig.input.MouseButton;
import dev.someoneok.crystalconfig.input.MouseButtonEvent;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.*;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;

import static dev.someoneok.crystalconfig.utils.TextUtils.ellipsize;

public class KeybindSelector extends Component {
    private final State<Keybind> value;
    private boolean recording;
    private boolean allowNone = true;
    private boolean allowMouseButtons = true;

    public KeybindSelector(State<Keybind> value) {
        this.value = value;
        this.focusable = true;
        size(126, 28);
    }

    public KeybindSelector allowNone(boolean allowNone) {
        this.allowNone = allowNone;
        return this;
    }

    public KeybindSelector disallowNone(boolean disallowNone) {
        return allowNone(!disallowNone);
    }

    public KeybindSelector allowMouseButtons(boolean allowMouseButtons) {
        this.allowMouseButtons = allowMouseButtons;
        return this;
    }

    public KeybindSelector disallowMouseButtons(boolean disallowMouseButtons) {
        return allowMouseButtons(!disallowMouseButtons);
    }

    public boolean recording() {
        return recording;
    }

    public void cancelRecording() {
        recording = false;
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 126, 28));
    }

    @Override
    protected void renderSelf(RenderContext context) {
        boolean disabled = !enabled();
        ColorRGBA fill = disabled
                ? context.theme().palette().surfaceAlt().withAlpha(120)
                : recording || hovered || focused
                ? context.theme().palette().surfaceHover()
                : context.theme().palette().surfaceAlt();

        context.rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1, disabled
                        ? context.theme().palette().border().withAlpha(95)
                        : recording || focused
                        ? context.theme().palette().accent()
                        : context.theme().palette().border())
                .radius(context.theme().radii().md()), z);

        Keybind current = value.get() == null ? Keybind.none() : value.get();
        String label = recording
                ? allowMouseButtons ? "Press any key or mouse" : "Press any key"
                : current.display();
        float font = recording ? context.theme().fonts().small() : context.theme().fonts().normal();
        String shown = ellipsize(context, label, font, Math.max(8, bounds.w() - 14));
        TextMetrics metrics = context.measureText(shown, font);
        Rect clip = bounds.inset(7, 0, 7, 0);
        context.pushClip(clip);
        context.text(shown,
                bounds.centerX() - metrics.width() * 0.5f,
                bounds.centerY() - metrics.height() * 0.5f,
                font,
                disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().text(),
                z + 1);
        context.popClip();
    }

    @Override
    public boolean onMousePressedCapture(MouseButtonEvent event) {
        if (!enabled() || !recording) return false;
        return recordMouseButton(event.rawButton);
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (!enabled()) return false;

        if (recording) return recordMouseButton(event.rawButton);

        if (event.button == MouseButton.LEFT) {
            recording = true;
            return true;
        }

        if (event.button == MouseButton.RIGHT && allowNone) {
            value.set(Keybind.none());
            return true;
        }
        return false;
    }

    private boolean recordMouseButton(int button) {
        if (!allowMouseButtons) return true;
        if (!KeybindUtils.isMouseButton(button)) return true;
        value.set(Keybind.glfwKey(button));
        recording = false;
        return true;
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled() || !recording) return false;

        if (event.keyCode == KeyCodes.ESCAPE) {
            recording = false;
            return true;
        }

        if (allowNone && (event.keyCode == KeyCodes.BACKSPACE || event.keyCode == KeyCodes.DELETE)) {
            value.set(Keybind.none());
            recording = false;
            return true;
        }

        value.set(Keybind.glfwKey(event.keyCode, event.displayName));
        recording = false;
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        if (!focused) cancelRecording();
    }
}
