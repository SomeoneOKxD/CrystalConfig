package dev.someoneok.crystalconfig.utils;

import dev.someoneok.crystalconfig.render.ColorRGBA;

public final class HsvColor {
    public final float h;
    public final float s;
    public final float v;
    public final float a;

    public HsvColor(float h, float s, float v, float a) {
        this.h = wrap(h);
        this.s = MathUtil.clamp(s, 0, 1);
        this.v = MathUtil.clamp(v, 0, 1);
        this.a = MathUtil.clamp(a, 0, 1);
    }

    public static HsvColor from(ColorRGBA color) {
        float r = color.rf();
        float g = color.gf();
        float b = color.bf();
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta == 0) h = 0;
        else if (max == r) h = 60 * (((g - b) / delta) % 6);
        else if (max == g) h = 60 * (((b - r) / delta) + 2);
        else h = 60 * (((r - g) / delta) + 4);
        if (h < 0) h += 360;
        float s = max == 0 ? 0 : delta / max;
        return new HsvColor(h, s, max, color.af());
    }

    public ColorRGBA toColor() {
        float c = v * s;
        float x = c * (1 - Math.abs((h / 60.0f) % 2 - 1));
        float m = v - c;
        float rp, gp, bp;
        if (h < 60) { rp = c; gp = x; bp = 0; }
        else if (h < 120) { rp = x; gp = c; bp = 0; }
        else if (h < 180) { rp = 0; gp = c; bp = x; }
        else if (h < 240) { rp = 0; gp = x; bp = c; }
        else if (h < 300) { rp = x; gp = 0; bp = c; }
        else { rp = c; gp = 0; bp = x; }
        return ColorRGBA.rgba(Math.round((rp + m) * 255), Math.round((gp + m) * 255), Math.round((bp + m) * 255), Math.round(a * 255));
    }

    public HsvColor withHue(float hue) { return new HsvColor(hue, s, v, a); }
    public HsvColor withSaturation(float saturation) { return new HsvColor(h, saturation, v, a); }
    public HsvColor withValue(float value) { return new HsvColor(h, s, value, a); }
    public HsvColor withAlpha(float alpha) { return new HsvColor(h, s, v, alpha); }

    private static float wrap(float hue) {
        float result = hue % 360.0f;
        return result < 0 ? result + 360.0f : result;
    }
}
