package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.ui.Component;

public class Separator extends Component {
    public Separator() {
        height(1);
        fillX();
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return new Size(constraints.maxWidth(), 1);
    }

    @Override
    protected void renderSelf(RenderContext context) {
        context.rect(new Rect(bounds.x(), bounds.centerY(), bounds.w(), 1), SdfRectStyle.create().fill(context.theme().palette().border()), z);
    }
}
