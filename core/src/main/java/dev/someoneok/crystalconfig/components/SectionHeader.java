package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.MinecraftTextFormatting;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.TextMetrics;
import dev.someoneok.crystalconfig.ui.Component;

public class SectionHeader extends Component {
    private final String title;

    public SectionHeader(String title) {
        this.title = title == null ? "" : title;
        height(28);
        fillX();
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        TextMetrics metrics = MinecraftTextFormatting.measureText(context.backend(), context.theme(), title, context.theme().fonts().large(), context.theme().fonts().regular());
        return new Size(Math.min(constraints.maxWidth(), metrics.width()), 28);
    }

    @Override
    protected void renderSelf(RenderContext context) {
        context.text(title, bounds.x(), bounds.y() + 2, context.theme().fonts().large(), context.theme().palette().text(), z);
        context.rect(new Rect(bounds.x(), bounds.bottom() - 2, bounds.w(), 1), context.theme().palette().border(), 0, z);
    }
}
