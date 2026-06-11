package dev.someoneok.crystalconfig.render;

public final class TextCommand extends DrawCommand {
    public final String text;
    public final float x;
    public final float y;
    public final float fontSize;
    public final String fontFace;
    public final ColorRGBA color;
    public final float opacity;
    public final boolean shadow;

    public TextCommand(String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float opacity, boolean shadow, float z) {
        super(z);
        this.text = text == null ? "" : text;
        this.x = x;
        this.y = y;
        this.fontSize = fontSize;
        this.fontFace = fontFace;
        this.color = color;
        this.opacity = opacity;
        this.shadow = shadow;
    }

    @Override
    public Material material() {
        return Material.TEXT;
    }
}
