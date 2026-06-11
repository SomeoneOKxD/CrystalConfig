package dev.someoneok.crystalconfig.containers;

import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;

public class Panel extends Column {
    private boolean shadow = true;

    public Panel() {
        padding(14);
        gap(8);
        fillX();
    }

    public Panel shadow(boolean shadow) { this.shadow = shadow; return this; }

    @Override
    protected void renderSelf(RenderContext context) {
        SdfRectStyle style = SdfRectStyle.create()
                .fill(context.theme().palette().surface())
                .border(1, context.theme().palette().border())
                .radius(context.theme().radii().lg());
        if (shadow) {
            style.shadow(22, 0, 4, context.theme().palette().shadow());
        }
        context.rect(bounds, style, z - 1);
    }
}
