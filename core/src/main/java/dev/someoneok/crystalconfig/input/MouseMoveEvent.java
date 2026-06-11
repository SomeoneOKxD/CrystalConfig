package dev.someoneok.crystalconfig.input;

public final class MouseMoveEvent extends UiEvent {
    public final float x;
    public final float y;
    public final float dx;
    public final float dy;

    public MouseMoveEvent(float x, float y, float dx, float dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }
}
