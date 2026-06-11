package dev.someoneok.crystalconfig.render;

public final class SdfRectStyle {
    private ColorRGBA fill = ColorRGBA.TRANSPARENT;
    private ColorRGBA border = ColorRGBA.TRANSPARENT;
    private ColorRGBA shadow = ColorRGBA.TRANSPARENT;
    private float radius = 0;
    private float borderWidth = 0;
    private float shadowRadius = 0;
    private float shadowOffsetX = 0;
    private float shadowOffsetY = 0;
    private float opacity = 1.0f;

    public static SdfRectStyle create() { return new SdfRectStyle(); }

    public SdfRectStyle fill(ColorRGBA fill) { this.fill = fill; return this; }
    public SdfRectStyle border(float width, ColorRGBA color) { this.borderWidth = width; this.border = color; return this; }
    public SdfRectStyle radius(float radius) { this.radius = radius; return this; }
    public SdfRectStyle shadow(float radius, float offsetX, float offsetY, ColorRGBA color) { this.shadowRadius = radius; this.shadowOffsetX = offsetX; this.shadowOffsetY = offsetY; this.shadow = color; return this; }
    public SdfRectStyle opacity(float opacity) { this.opacity = opacity; return this; }

    public ColorRGBA fill() { return fill; }
    public ColorRGBA border() { return border; }
    public ColorRGBA shadow() { return shadow; }
    public float radius() { return radius; }
    public float borderWidth() { return borderWidth; }
    public float shadowRadius() { return shadowRadius; }
    public float shadowOffsetX() { return shadowOffsetX; }
    public float shadowOffsetY() { return shadowOffsetY; }
    public float opacity() { return opacity; }
}
