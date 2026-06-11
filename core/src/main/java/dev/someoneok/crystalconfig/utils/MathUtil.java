package dev.someoneok.crystalconfig.utils;

public final class MathUtil {
    private MathUtil() { }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0, 1);
    }

    public static double snap(double value, double step) {
        if (step <= 0) return value;
        return Math.round(value / step) * step;
    }
}
