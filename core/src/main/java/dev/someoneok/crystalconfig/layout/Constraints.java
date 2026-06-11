package dev.someoneok.crystalconfig.layout;

public record Constraints(float maxWidth, float maxHeight) {
    public static Constraints loose(float maxWidth, float maxHeight) {
        return new Constraints(maxWidth, maxHeight);
    }

    public Size clamp(Size size) {
        return new Size(Math.min(size.width(), maxWidth), Math.min(size.height(), maxHeight));
    }
}
