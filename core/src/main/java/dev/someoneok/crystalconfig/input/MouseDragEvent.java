package dev.someoneok.crystalconfig.input;

public final class MouseDragEvent extends UiEvent {
    public final float x;
    public final float y;
    public final float dx;
    public final float dy;
    public final MouseButton button;
    public final int modifiers;

    public MouseDragEvent(float x, float y, float dx, float dy, MouseButton button, int modifiers) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.button = button;
        this.modifiers = modifiers;
    }
}
