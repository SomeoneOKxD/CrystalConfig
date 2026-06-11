package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.MinecraftTextFormatting;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.TextMetrics;
import dev.someoneok.crystalconfig.ui.Component;

public class Label extends Component {
    private String text;
    private float fontSize = -1;
    private ColorRGBA color;

    public Label(String text) {
        this.text = text == null ? "" : text;
    }

    public Label text(String text) { this.text = text == null ? "" : text; return this; }
    public Label fontSize(float fontSize) { this.fontSize = fontSize; return this; }
    public Label color(ColorRGBA color) { this.color = color; return this; }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float size = fontSize > 0 ? fontSize : context.theme().fonts().normal();
        TextMetrics metrics = MinecraftTextFormatting.measureText(context.backend(), context.theme(), text, size, context.theme().fonts().regular());
        return constraints.clamp(new Size(metrics.width(), metrics.height()));
    }

    @Override
    protected void renderSelf(RenderContext context) {
        float size = fontSize > 0 ? fontSize : context.theme().fonts().normal();
        ColorRGBA c = color != null ? color : context.theme().palette().text();
        context.text(text, bounds.x(), bounds.y(), size, c, z);
    }
}
