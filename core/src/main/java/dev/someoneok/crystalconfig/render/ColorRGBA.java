package dev.someoneok.crystalconfig.render;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(ColorRGBA.Serializer.class)
public record ColorRGBA(int r, int g, int b, int a) {
    public static final ColorRGBA TRANSPARENT = rgba(0, 0, 0, 0);
    public static final ColorRGBA WHITE = rgb(255, 255, 255);
    public static final ColorRGBA BLACK = rgb(0, 0, 0);

    public ColorRGBA {
        r = clamp8(r);
        g = clamp8(g);
        b = clamp8(b);
        a = clamp8(a);
    }

    public ColorRGBA(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public ColorRGBA(int r, int g, int b, float a) {
        this(r, g, b, alphaToInt(a));
    }

    public static ColorRGBA rgb(int r, int g, int b) {
        return new ColorRGBA(r, g, b);
    }

    public static ColorRGBA rgba(int r, int g, int b, int a) {
        return new ColorRGBA(r, g, b, a);
    }

    public static ColorRGBA rgba(int r, int g, int b, float a) {
        return new ColorRGBA(r, g, b, a);
    }

    public static ColorRGBA fromArgb(int argb) {
        return new ColorRGBA(
                (argb >>> 16) & 0xFF,
                (argb >>> 8) & 0xFF,
                argb & 0xFF,
                (argb >>> 24) & 0xFF
        );
    }

    public static ColorRGBA fromRgba(int rgba) {
        return new ColorRGBA(
                (rgba >>> 24) & 0xFF,
                (rgba >>> 16) & 0xFF,
                (rgba >>> 8) & 0xFF,
                rgba & 0xFF
        );
    }

    public static ColorRGBA hex(String hex) {
        String value = hex.startsWith("#") ? hex.substring(1) : hex;

        if (value.length() == 6) {
            return new ColorRGBA(
                    Integer.parseInt(value.substring(0, 2), 16),
                    Integer.parseInt(value.substring(2, 4), 16),
                    Integer.parseInt(value.substring(4, 6), 16)
            );
        }

        if (value.length() == 8) {
            return new ColorRGBA(
                    Integer.parseInt(value.substring(0, 2), 16),
                    Integer.parseInt(value.substring(2, 4), 16),
                    Integer.parseInt(value.substring(4, 6), 16),
                    Integer.parseInt(value.substring(6, 8), 16)
            );
        }

        throw new IllegalArgumentException("Expected #RRGGBB or #RRGGBBAA: " + hex);
    }

    public float rf() {
        return r / 255.0f;
    }

    public float gf() {
        return g / 255.0f;
    }

    public float bf() {
        return b / 255.0f;
    }

    public float af() {
        return a / 255.0f;
    }

    public int toArgb() {
        return ((a & 0xFF) << 24)
                | ((r & 0xFF) << 16)
                | ((g & 0xFF) << 8)
                | (b & 0xFF);
    }

    public int getArgb() {
        return toArgb();
    }

    public int toRgba() {
        return ((r & 0xFF) << 24)
                | ((g & 0xFF) << 16)
                | ((b & 0xFF) << 8)
                | (a & 0xFF);
    }

    public int getRgba() {
        return toRgba();
    }

    public ColorRGBA withAlpha(int alpha) {
        return new ColorRGBA(r, g, b, alpha);
    }

    public ColorRGBA withAlpha(float alpha) {
        return new ColorRGBA(r, g, b, alpha);
    }

    public ColorRGBA multiplyAlpha(float alpha) {
        return withAlpha(Math.round(a * clamp01(alpha)));
    }

    public ColorRGBA lighten(float amount) {
        return lerp(WHITE, amount);
    }

    public ColorRGBA darken(float amount) {
        return lerp(BLACK, amount);
    }

    public ColorRGBA lerp(ColorRGBA other, float t) {
        float clamped = clamp01(t);

        return new ColorRGBA(
                Math.round(r + (other.r - r) * clamped),
                Math.round(g + (other.g - g) * clamped),
                Math.round(b + (other.b - b) * clamped),
                Math.round(a + (other.a - a) * clamped)
        );
    }

    public boolean isTransparent() {
        return a == 0;
    }

    public String hex(boolean includeAlpha) {
        if (includeAlpha) return String.format("%02X%02X%02X%02X", r, g, b, a);
        return String.format("%02X%02X%02X", r, g, b);
    }

    private static int alphaToInt(float alpha) {
        return Math.round(clamp01(alpha) * 255.0f);
    }

    private static int clamp8(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    @Override
    public String toString() {
        return "ColorRGBA{" + r + "," + g + "," + b + "," + a + '}';
    }

    public static final class Serializer
            implements JsonSerializer<ColorRGBA>, JsonDeserializer<ColorRGBA> {

        @Override
        public JsonElement serialize(
                ColorRGBA src,
                Type typeOfSrc,
                JsonSerializationContext context
        ) {
            return new JsonPrimitive("#" + src.hex(true));
        }

        @Override
        public ColorRGBA deserialize(
                JsonElement json,
                Type typeOfT,
                JsonDeserializationContext context
        ) throws JsonParseException {
            return ColorRGBA.hex(json.getAsString());
        }
    }
}
