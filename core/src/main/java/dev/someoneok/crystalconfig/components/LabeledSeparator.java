package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.*;
import dev.someoneok.crystalconfig.ui.Component;

public class LabeledSeparator extends Component {
    private final String text;
    private final float textScale;
    private static final float HEIGHT = 28.0f;
    private static final float BOX_PAD_X = 10.0f;
    private static final float BOX_PAD_Y = 3.0f;
    private static final float LINE_GAP = 12.0f;

    public LabeledSeparator(String text) {
        this(text, 1.0f);
    }

    public LabeledSeparator(String text, float textScale) {
        this.text = text == null ? "" : text;
        this.textScale = clampScale(textScale);
        height(HEIGHT);
        fillX();
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return constraints.clamp(new Size(constraints.maxWidth(), HEIGHT));
    }

    @Override
    protected void renderSelf(RenderContext context) {
        float font = context.theme().fonts().small() * textScale;
        String face = context.theme().fonts().semibold();
        String shown = ellipsize(context, text, font, face, Math.max(8, bounds.w() - 42));
        TextMetrics metrics = context.measureText(shown, font, face);
        float boxW = Math.min(bounds.w(), Math.max(36, metrics.width() + BOX_PAD_X * 2));
        float boxH = Math.max(18, metrics.height() + BOX_PAD_Y * 2);
        Rect box = new Rect(bounds.centerX() - boxW * 0.5f, bounds.centerY() - boxH * 0.5f, boxW, boxH);
        float lineY = bounds.centerY();
        float leftW = Math.max(0, box.x() - LINE_GAP - bounds.x());
        float rightX = box.right() + LINE_GAP;
        float rightW = Math.max(0, bounds.right() - rightX);
        ColorRGBA line = context.theme().palette().border().withAlpha(150);
        if (leftW > 1) context.rect(new Rect(bounds.x(), lineY, leftW, 1), SdfRectStyle.create().fill(line).radius(0), z);
        if (rightW > 1) context.rect(new Rect(rightX, lineY, rightW, 1), SdfRectStyle.create().fill(line).radius(0), z);
        context.rect(box, SdfRectStyle.create()
                .fill(context.theme().palette().surfaceAlt().withAlpha(170))
                .border(1, context.theme().palette().border().withAlpha(185))
                .radius(context.theme().radii().md()), z + 1);
        context.text(shown, box.centerX() - metrics.width() * 0.5f, box.centerY() - context.lineHeight(font, face) * 0.5f, font, face, context.theme().palette().mutedText(), z + 2);
    }

    private static float clampScale(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 1.0f;
        return Math.max(0.5f, Math.min(3.0f, value));
    }

    private static String ellipsize(RenderContext context, String value, float font, String face, float maxWidth) {
        String s = value == null ? "" : value;
        if (context.measureText(s, font, face).width() <= maxWidth) return s;
        String ellipsis = "...";
        if (context.measureText(ellipsis, font, face).width() > maxWidth) return "";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (context.measureText(s.substring(0, mid) + ellipsis, font, face).width() <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }
}
