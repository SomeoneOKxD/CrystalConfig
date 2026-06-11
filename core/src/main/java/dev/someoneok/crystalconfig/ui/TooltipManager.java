package dev.someoneok.crystalconfig.ui;

import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.render.TextMetrics;
import dev.someoneok.crystalconfig.theme.Theme;

final class TooltipManager {
    private Component current;
    private float time;

    void update(Component hovered, float deltaSeconds) {
        if (hovered != current) {
            current = hovered;
            time = 0;
        } else if (hovered != null) {
            time += deltaSeconds;
        }
    }

    void render(RenderContext context, float mouseX, float mouseY, int viewportWidth, int viewportHeight) {
        if (current == null || current.tooltip() == null || current.tooltip().isBlank() || time < 0.35f) return;
        Theme theme = context.theme();
        String text = current.tooltip();
        float font = theme.fonts().small();
        TextMetrics metrics = context.measureText(text, font);
        float pad = theme.spacing().sm();
        float w = metrics.width() + pad * 2;
        float h = metrics.height() + pad * 2;
        float x = Math.min(mouseX + 14, viewportWidth - w - 8);
        float y = Math.min(mouseY + 16, viewportHeight - h - 8);
        float alpha = Math.min(1.0f, (time - 0.35f) * 8.0f);
        context.rect(new Rect(x, y, w, h), SdfRectStyle.create()
                .fill(theme.palette().surfaceAlt().multiplyAlpha(alpha))
                .border(1, theme.palette().border().multiplyAlpha(alpha))
                .shadow(16, 0, 4, theme.palette().shadow().multiplyAlpha(alpha))
                .radius(theme.radii().md()), 9000);
        context.text(text, x + pad, y + pad, font, theme.palette().text().multiplyAlpha(alpha), 1.0f, false, 9001);
    }
}
