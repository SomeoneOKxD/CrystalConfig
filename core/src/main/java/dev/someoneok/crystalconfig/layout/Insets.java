package dev.someoneok.crystalconfig.layout;

public record Insets(float left, float top, float right, float bottom) {
    public static final Insets ZERO = new Insets(0, 0, 0, 0);

    public static Insets all(float value) { return new Insets(value, value, value, value); }
    public static Insets xy(float x, float y) { return new Insets(x, y, x, y); }
    public static Insets of(float left, float top, float right, float bottom) { return new Insets(left, top, right, bottom); }

    public float horizontal() { return left + right; }
    public float vertical() { return top + bottom; }
}
