package dev.someoneok.crystalconfig.input;

public final class MouseButtonEvent extends UiEvent {
    public final float x;
    public final float y;
    public final MouseButton button;
    public final int rawButton;
    public final int modifiers;

    public MouseButtonEvent(float x, float y, MouseButton button, int modifiers) {
        this(x, y, button, -1, modifiers);
    }

    public MouseButtonEvent(float x, float y, MouseButton button, int rawButton, int modifiers) {
        this.x = x;
        this.y = y;
        this.button = button;
        this.rawButton = rawButton;
        this.modifiers = modifiers;
    }
}
