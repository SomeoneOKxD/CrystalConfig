package dev.someoneok.crystalconfig.render;

public abstract class DrawCommand {
    int order;
    Rect clip;
    public final float z;

    protected DrawCommand(float z) {
        this.z = z;
    }

    public int order() { return order; }
    public Rect clip() { return clip; }
    public abstract Material material();
}
