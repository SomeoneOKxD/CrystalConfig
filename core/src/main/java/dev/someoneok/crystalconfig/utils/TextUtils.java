package dev.someoneok.crystalconfig.utils;

import dev.someoneok.crystalconfig.render.RenderContext;

import java.util.Objects;
import java.util.function.ToDoubleFunction;

public final class TextUtils {
    private static final String ELLIPSIS = "...";

    private TextUtils() { }

    public static String ellipsize(RenderContext context, String value, float fontSize, float maxWidth) {
        Objects.requireNonNull(context, "context");
        return ellipsize(value, maxWidth, text -> context.measureText(text, fontSize).width());
    }

    public static String ellipsize(RenderContext context, String value, float fontSize, String fontFace, float maxWidth) {
        Objects.requireNonNull(context, "context");
        return ellipsize(value, maxWidth, text -> context.measureText(text, fontSize, fontFace).width());
    }

    public static String ellipsizePlain(RenderContext context, String value, float fontSize, float maxWidth) {
        Objects.requireNonNull(context, "context");
        return ellipsize(value, maxWidth, text -> context.measurePlainText(text, fontSize).width());
    }

    private static String ellipsize(String value, float maxWidth, ToDoubleFunction<String> measureWidth) {
        String text = value == null ? "" : value;
        if (measureWidth.applyAsDouble(text) <= maxWidth) return text;
        if (maxWidth <= 0 || measureWidth.applyAsDouble(ELLIPSIS) > maxWidth) return "";

        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (measureWidth.applyAsDouble(text.substring(0, middle) + ELLIPSIS) <= maxWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return text.substring(0, low) + ELLIPSIS;
    }
}
