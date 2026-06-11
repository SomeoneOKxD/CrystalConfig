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

public class KeybindSelector extends Component {
    private final State<Keybind> value;
    private boolean listening;
    private boolean allowNone = true;

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
        this.allowNone = !disallowNone;
        return this;
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
                : listening || hovered || focused
                ? context.theme().palette().surfaceHover()
                : context.theme().palette().surfaceAlt();
        context.rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1, disabled ? context.theme().palette().border().withAlpha(95) : listening || focused ? context.theme().palette().accent() : context.theme().palette().border())
                .radius(context.theme().radii().md()), z);
        Keybind current = value.get() == null ? Keybind.none() : value.get();
        String label = listening ? "Press a key or mouse..." : current.display();
        float font = context.theme().fonts().normal();
        String shown = ellipsize(context, label, font, Math.max(8, bounds.w() - 14));
        TextMetrics m = context.measureText(shown, font);
        Rect clip = bounds.inset(7, 0, 7, 0);
        context.pushClip(clip);
        context.text(
                shown,
                bounds.centerX() - m.width() * 0.5f,
                bounds.centerY() - m.height() * 0.5f,
                font,
                disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().text(),
                z + 1
        );
        context.popClip();
    }

    private static String ellipsize(RenderContext context, String value, float font, float maxWidth) {
        String s = value == null ? "" : value;
        if (context.measureText(s, font).width() <= maxWidth) return s;
        String ellipsis = "...";
        if (context.measureText(ellipsis, font).width() > maxWidth) return "";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (context.measureText(s.substring(0, mid) + ellipsis, font).width() <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (!enabled()) return false;
        if (listening) {
            if (event.rawButton >= 0) {
                if (!KeybindUtils.isAssignableGlfwMouseButton(event.rawButton)) {
                    return true;
                }
                value.set(KeybindUtils.fromGlfwMouseButton(event.rawButton));
                listening = false;
                return true;
            }
            return false;
        }
        if (event.button != MouseButton.LEFT) return false;
        listening = true;
        return true;
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled() || !listening) return false;
        if (event.keyCode == KeyCodes.ESCAPE || event.keyCode == KeyCodes.BACKSPACE || event.keyCode == KeyCodes.DELETE) {
            if (allowNone) value.set(Keybind.none());
        } else {
            value.set(KeybindUtils.fromGlfwKey(event.keyCode, event.displayName));
        }
        listening = false;
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        if (!focused) listening = false;
    }
}
