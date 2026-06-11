package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.animation.AnimatedFloat;
import dev.someoneok.crystalconfig.icons.MediaBrandIcons;
import dev.someoneok.crystalconfig.input.KeyCodes;
import dev.someoneok.crystalconfig.input.KeyEvent;
import dev.someoneok.crystalconfig.input.MouseButton;
import dev.someoneok.crystalconfig.input.MouseButtonEvent;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.*;
import dev.someoneok.crystalconfig.ui.Component;

/** Small square icon-only button for sidebar/footer actions. */
public class IconButton extends Component {
    private String icon;
    private String fontFace = MediaBrandIcons.FONT_FACE;
    private Runnable action = () -> { };
    private final AnimatedFloat hoverAnim = new AnimatedFloat(0).speed(18);
    private final AnimatedFloat pressAnim = new AnimatedFloat(0).speed(22);
    private static final float DEFAULT_SIZE = 37.5f;

    private long debounceNanos = 250_000_000L;
    private long lastActionNanos;

    public IconButton(String icon) {
        this.icon = icon == null ? "" : icon;
        this.focusable = false;
        this.size(DEFAULT_SIZE, DEFAULT_SIZE);
        this.minSize(DEFAULT_SIZE, DEFAULT_SIZE);
    }

    public IconButton icon(String icon) { this.icon = icon == null ? "" : icon; return this; }
    public IconButton fontFace(String fontFace) { this.fontFace = fontFace == null || fontFace.isBlank() ? MediaBrandIcons.FONT_FACE : fontFace; return this; }
    public IconButton onClick(Runnable action) { this.action = action == null ? () -> { } : action; return this; }
    public IconButton debounceMillis(long millis) { this.debounceNanos = Math.max(0L, millis) * 1_000_000L; return this; }

    @Override
    public void tick(float deltaSeconds) {
        hoverAnim.target(hovered ? 1.0f : 0.0f);
        pressAnim.target(pressed ? 1.0f : 0.0f);
        hoverAnim.update(deltaSeconds);
        pressAnim.update(deltaSeconds);
        super.tick(deltaSeconds);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float width = preferredWidth >= 0 ? preferredWidth : DEFAULT_SIZE;
        float height = preferredHeight >= 0 ? preferredHeight : DEFAULT_SIZE;
        return constraints.clamp(new Size(Math.max(minWidth, width), Math.max(minHeight, height)));
    }

    @Override
    protected void renderSelf(RenderContext context) {
        ColorRGBA fill = context.theme().palette().surfaceAlt()
                .lerp(context.theme().palette().surfaceHover(), hoverAnim.value())
                .lerp(context.theme().palette().surfaceActive(), pressAnim.value());
        ColorRGBA border = focused ? context.theme().palette().accent() : context.theme().palette().border().withAlpha(180);
        context.rect(bounds, SdfRectStyle.create()
                .fill(fill.withAlpha((int)(120 + 55 * hoverAnim.value())))
                .border(1, border)
                .radius(context.theme().radii().md()), z);

        if (icon == null || icon.isEmpty()) return;
        float font = Math.max(16.0f, Math.min(bounds.w(), bounds.h()) * 0.52f);
        ColorRGBA color = context.theme().palette().mutedText().lerp(context.theme().palette().accent(), hoverAnim.value() * 0.8f);
        TextMetrics metrics = context.measureText(icon, font, fontFace);
        float tx = bounds.centerX() - metrics.width() * 0.5f;
        float ty = bounds.centerY() - metrics.height() * 0.5f + font * 0.05f;
        Rect clip = bounds.inset(3, 3, 3, 3);
        context.pushClip(clip);
        context.text(icon, tx, ty, font, fontFace, color, z + 2);
        context.popClip();
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (event.button != MouseButton.LEFT) return false;
        pressed = true;
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        boolean wasPressed = pressed;
        pressed = false;
        pressAnim.snap(0.0f);
        if (wasPressed && event.button == MouseButton.LEFT && bounds.contains(event.x, event.y)) {
            triggerAction();
            return true;
        }
        return wasPressed;
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (event.keyCode == KeyCodes.ENTER || event.keyCode == KeyCodes.SPACE) {
            triggerAction();
            return true;
        }
        return false;
    }

    private void triggerAction() {
        long now = System.nanoTime();
        if (debounceNanos <= 0L || now - lastActionNanos >= debounceNanos) {
            lastActionNanos = now;
            action.run();
        }
    }
}
