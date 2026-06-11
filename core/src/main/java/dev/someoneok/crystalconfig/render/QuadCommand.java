package dev.someoneok.crystalconfig.render;

public final class QuadCommand extends DrawCommand {
    public final Material material;
    public final Rect rect;
    public final ColorRGBA fill;
    public final ColorRGBA border;
    public final ColorRGBA shadow;
    public final float radius;
    public final float borderWidth;
    public final float shadowRadius;
    public final float shadowOffsetX;
    public final float shadowOffsetY;
    public final float opacity;
    public final float data0;
    public final float data1;

    public QuadCommand(Material material, Rect rect, ColorRGBA fill, ColorRGBA border, ColorRGBA shadow,
                       float radius, float borderWidth, float shadowRadius, float shadowOffsetX, float shadowOffsetY,
                       float opacity, float data0, float data1, float z) {
        super(z);
        this.material = material;
        this.rect = rect;
        this.fill = fill;
        this.border = border;
        this.shadow = shadow;
        this.radius = radius;
        this.borderWidth = borderWidth;
        this.shadowRadius = shadowRadius;
        this.shadowOffsetX = shadowOffsetX;
        this.shadowOffsetY = shadowOffsetY;
        this.opacity = opacity;
        this.data0 = data0;
        this.data1 = data1;
    }

    @Override
    public Material material() {
        return material;
    }
}
