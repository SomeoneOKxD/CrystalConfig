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
    public final TextureRef texture;
    public final Rect uv;
    public final TextureFilter textureFilter;

    public QuadCommand(Material material, Rect rect, ColorRGBA fill, ColorRGBA border, ColorRGBA shadow,
                       float radius, float borderWidth, float shadowRadius, float shadowOffsetX, float shadowOffsetY,
                       float opacity, float data0, float data1, float z) {
        this(material, rect, fill, border, shadow, radius, borderWidth, shadowRadius, shadowOffsetX,
                shadowOffsetY, opacity, data0, data1, null, Rect.ZERO, TextureFilter.NEAREST, z);
    }

    public QuadCommand(Material material, Rect rect, ColorRGBA fill, ColorRGBA border, ColorRGBA shadow,
                       float radius, float borderWidth, float shadowRadius, float shadowOffsetX, float shadowOffsetY,
                       float opacity, float data0, float data1, TextureRef texture, Rect uv,
                       TextureFilter textureFilter, float z) {
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
        this.texture = texture;
        this.uv = uv == null ? Rect.ZERO : uv;
        this.textureFilter = textureFilter == null ? TextureFilter.NEAREST : textureFilter;
    }

    public static QuadCommand texture(TextureRef texture, Rect rect, Rect uv, ColorRGBA tint,
                                      TextureFilter filter, float z) {
        return new QuadCommand(
                Material.TEXTURE,
                rect,
                tint,
                ColorRGBA.TRANSPARENT,
                ColorRGBA.TRANSPARENT,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                texture,
                uv,
                filter,
                z
        );
    }

    @Override
    public Material material() {
        return material;
    }
}
