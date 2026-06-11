package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.animation.AnimatedFloat;
import dev.someoneok.crystalconfig.input.KeyCodes;
import dev.someoneok.crystalconfig.input.KeyEvent;
import dev.someoneok.crystalconfig.input.MouseButton;
import dev.someoneok.crystalconfig.input.MouseButtonEvent;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.*;
import dev.someoneok.crystalconfig.ui.Component;

public class Button extends Component {
    private String text;
    private Runnable action = () -> { };
    private final AnimatedFloat hoverAnim = new AnimatedFloat(0);
    private final AnimatedFloat pressAnim = new AnimatedFloat(0);
    private boolean accent;
    private long debounceNanos = 250_000_000L;
    private long lastActionNanos;

    public Button(String text) {
        this.text = text == null ? "" : text;
        this.focusable = true;
        this.height(28);
        this.minSize(64, 28);
    }

    public Button onClick(Runnable action) { this.action = action == null ? () -> { } : action; return this; }
    public Button text(String text) { this.text = text == null ? "" : text; return this; }
    public Button accent(boolean accent) { this.accent = accent; return this; }
    public Button debounceMillis(long millis) { this.debounceNanos = Math.max(0L, millis) * 1_000_000L; return this; }

    @Override
    public void tick(float deltaSeconds) {
        hoverAnim.target(enabled() && hovered ? 1 : 0);
        pressAnim.target(enabled() && pressed ? 1 : 0);
        hoverAnim.update(deltaSeconds);
        pressAnim.update(deltaSeconds);
        super.tick(deltaSeconds);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        TextMetrics metrics = MinecraftTextFormatting.measureText(context.backend(), context.theme(), text, context.theme().fonts().normal(), context.theme().fonts().regular());
        return constraints.clamp(new Size(Math.max(minWidth, metrics.width() + 28), Math.max(minHeight, 28)));
    }

    @Override
    protected void renderSelf(RenderContext context) {
        boolean disabled = !enabled();
        ColorRGBA base = disabled ? context.theme().palette().surfaceAlt().withAlpha(120) : accent ? context.theme().palette().accent() : context.theme().palette().surfaceAlt();
        ColorRGBA hover = disabled ? base : accent ? context.theme().palette().accent().lighten(0.14f) : context.theme().palette().surfaceHover();
        ColorRGBA active = disabled ? base : accent ? context.theme().palette().accent().darken(0.12f) : context.theme().palette().surfaceActive();
        ColorRGBA fill = base.lerp(hover, hoverAnim.value()).lerp(active, pressAnim.value());
        context.rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1, disabled ? context.theme().palette().border().withAlpha(95) : focused ? context.theme().palette().accent() : context.theme().palette().border())
                .radius(context.theme().radii().md()), z);
        float font = context.theme().fonts().normal();
        String shown = ellipsize(context, text, font, Math.max(8, bounds.w() - 16));
        TextMetrics metrics = context.measureText(shown, font);
        float tx = bounds.centerX() - metrics.width() * 0.5f;
        float ty = bounds.centerY() - metrics.height() * 0.5f;
        Rect clip = bounds.inset(8, 0, 8, 0);
        context.pushClip(clip);
        context.text(shown, tx, ty, font, disabled ? context.theme().palette().mutedText().withAlpha(135) : accent ? context.theme().palette().accentText() : context.theme().palette().text(), z + 1);
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
        if (!enabled() || event.button != MouseButton.LEFT) return false;
        pressed = true;
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        boolean wasPressed = pressed;
        pressed = false;
        if (wasPressed && enabled() && bounds.contains(event.x, event.y) && event.button == MouseButton.LEFT) {
            triggerAction();
            return true;
        }
        return wasPressed;
    }

    private void triggerAction() {
        long now = System.nanoTime();
        if (debounceNanos <= 0L || now - lastActionNanos >= debounceNanos) {
            lastActionNanos = now;
            action.run();
        }
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled()) return false;
        if (event.keyCode == KeyCodes.ENTER || event.keyCode == KeyCodes.SPACE) {
            triggerAction();
            return true;
        }
        return false;
    }
}
