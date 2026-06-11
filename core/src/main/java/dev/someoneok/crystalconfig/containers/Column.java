package dev.someoneok.crystalconfig.containers;

import dev.someoneok.crystalconfig.layout.Alignment;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.ui.Component;

public class Column extends Component {
    protected float gap = 0;
    protected Alignment crossAxisAlignment = Alignment.STRETCH;

    public Column gap(float gap) { this.gap = Math.max(0, gap); return this; }
    public Column align(Alignment alignment) { this.crossAxisAlignment = alignment == null ? Alignment.STRETCH : alignment; return this; }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float availableWidth = Math.max(0, constraints.maxWidth());
        float maxWidth = 0;
        float totalHeight = 0;
        int count = 0;
        for (Component child : children) {
            if (!child.visible()) continue;
            Size childSize = child.measure(context, new Constraints(Math.max(0, availableWidth - child.margin().horizontal()), constraints.maxHeight()));
            maxWidth = Math.max(maxWidth, childSize.width() + child.margin().horizontal());
            totalHeight += childSize.height() + child.margin().vertical();
            count++;
        }
        if (count > 1) totalHeight += gap * (count - 1);
        return constraints.clamp(new Size(maxWidth, totalHeight));
    }

    @Override
    protected void layoutChildren(LayoutContext context) {
        Rect content = bounds.inset(padding.left(), padding.top(), padding.right(), padding.bottom());
        float availableWidth = content.w();
        float fixedHeight = 0;
        float totalFlex = 0;
        int visibleCount = 0;

        for (Component child : children) {
            if (!child.visible()) continue;
            visibleCount++;
            if (child.flexGrow() > 0) totalFlex += child.flexGrow();
            else {
                Size childSize = child.measure(context, new Constraints(Math.max(0, availableWidth - child.margin().horizontal()), content.h()));
                fixedHeight += childSize.height() + child.margin().vertical();
            }
        }
        if (visibleCount > 1) fixedHeight += gap * (visibleCount - 1);
        float remaining = Math.max(0, content.h() - fixedHeight);
        float y = content.y();

        for (Component child : children) {
            if (!child.visible()) continue;
            Size measured = child.measure(context, new Constraints(Math.max(0, availableWidth - child.margin().horizontal()), content.h()));
            float childHeight = child.flexGrow() > 0 && totalFlex > 0
                    ? Math.max(child.minHeight(), remaining * child.flexGrow() / totalFlex - child.margin().vertical())
                    : measured.height();
            float childWidth = child.fillXValue() || crossAxisAlignment == Alignment.STRETCH
                    ? Math.max(0, availableWidth - child.margin().horizontal())
                    : Math.min(measured.width(), Math.max(0, availableWidth - child.margin().horizontal()));
            float x = content.x() + child.margin().left();
            float extra = Math.max(0, availableWidth - child.margin().horizontal() - childWidth);
            if (crossAxisAlignment == Alignment.CENTER) x += extra * 0.5f;
            else if (crossAxisAlignment == Alignment.END) x += extra;
            y += child.margin().top();
            child.layout(context, new Rect(x, y, childWidth, childHeight));
            y += childHeight + child.margin().bottom() + gap;
        }
    }
}
