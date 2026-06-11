package dev.someoneok.crystalconfig.input;

public final class MouseScrollEvent extends UiEvent {
    public final float x;
    public final float y;
    public final float amountX;
    public final float amountY;

    public MouseScrollEvent(float x, float y, float amountX, float amountY) {
        this.x = x;
        this.y = y;
        this.amountX = amountX;
        this.amountY = amountY;
    }
}
