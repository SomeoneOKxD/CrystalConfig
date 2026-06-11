package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.animation.AnimatedFloat;
import dev.someoneok.crystalconfig.input.KeyCodes;
import dev.someoneok.crystalconfig.input.KeyEvent;
import dev.someoneok.crystalconfig.input.MouseButton;
import dev.someoneok.crystalconfig.input.MouseButtonEvent;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.render.TextMetrics;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;

public class Checkbox extends Component {
    private final State<Boolean> value;
    private final AnimatedFloat checked;

    public Checkbox(State<Boolean> value) {
        this.value = value;
        this.checked = new AnimatedFloat(Boolean.TRUE.equals(value.get()) ? 1 : 0);
        this.focusable = true;
        size(22, 22);
    }

    @Override
    public void tick(float deltaSeconds) {
        checked.target(Boolean.TRUE.equals(value.get()) ? 1 : 0);
        checked.update(deltaSeconds);
        super.tick(deltaSeconds);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return new Size(22, 22);
    }

    @Override
    protected void renderSelf(RenderContext context) {
        float t = checked.value();
        boolean disabled = !enabled();
        ColorRGBA fill = context.theme().palette().surfaceAlt().withAlpha(disabled ? 120 : 255)
                .lerp(disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().accent(), t);
        context.rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1, disabled ? context.theme().palette().border().withAlpha(95) : focused ? context.theme().palette().accent() : context.theme().palette().border())
                .radius(context.theme().radii().sm()), z);
        if (t > 0.02f) {
            String mark = "✔";
            float font = context.theme().fonts().normal() + 1.0f;
            String face = context.theme().fonts().semibold();
            TextMetrics metrics = context.measureText(mark, font, face);
            float x = bounds.centerX() - metrics.width() * 0.5f;
            float y = bounds.centerY() - metrics.height() * 0.5f - 0.5f;
            ColorRGBA color = (disabled ? context.theme().palette().surface() : context.theme().palette().accentText()).multiplyAlpha(t);
            context.text(mark, x, y, font, face, color, z + 1);
        }
    }

    private void toggle() { value.set(!Boolean.TRUE.equals(value.get())); }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (!enabled() || event.button != MouseButton.LEFT) return false;
        pressed = true;
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        boolean was = pressed;
        pressed = false;
        if (was && enabled() && bounds.contains(event.x, event.y)) toggle();
        return was;
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled()) return false;
        if (event.keyCode == KeyCodes.SPACE || event.keyCode == KeyCodes.ENTER) {
            toggle();
            return true;
        }
        return false;
    }
}
