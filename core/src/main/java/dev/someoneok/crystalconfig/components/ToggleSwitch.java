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
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;

public class ToggleSwitch extends Component {
    private final State<Boolean> value;
    private final AnimatedFloat amount;

    public ToggleSwitch(State<Boolean> value) {
        this.value = value;
        this.amount = new AnimatedFloat(Boolean.TRUE.equals(value.get()) ? 1 : 0);
        this.focusable = true;
        size(42, 20);
    }

    @Override
    public void tick(float deltaSeconds) {
        amount.target(Boolean.TRUE.equals(value.get()) ? 1 : 0);
        amount.update(deltaSeconds);
        super.tick(deltaSeconds);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return new Size(42, 20);
    }

    @Override
    protected void renderSelf(RenderContext context) {
        float t = amount.value();
        boolean disabled = !enabled();
        ColorRGBA off = context.theme().palette().surfaceAlt();
        ColorRGBA on = disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().accent();
        ColorRGBA fill = disabled ? off.withAlpha(120).lerp(on, t) : off.lerp(on, t);
        context.rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1, disabled ? context.theme().palette().border().withAlpha(95) : focused ? context.theme().palette().accent() : context.theme().palette().border())
                .radius(bounds.h() * 0.5f), z);
        float knob = bounds.h() - 6;
        float x = bounds.x() + 3 + (bounds.w() - knob - 6) * t;
        Rect knobRect = new Rect(x, bounds.y() + 3, knob, knob);
        context.rect(knobRect, SdfRectStyle.create()
                .fill(disabled ? context.theme().palette().mutedText().withAlpha(150) : ColorRGBA.WHITE)
                .shadow(8, 0, 1, context.theme().palette().shadow())
                .radius(knob * 0.5f), z + 1);
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
