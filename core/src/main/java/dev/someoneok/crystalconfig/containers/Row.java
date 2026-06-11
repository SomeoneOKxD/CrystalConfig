package dev.someoneok.crystalconfig.containers;

import dev.someoneok.crystalconfig.layout.Alignment;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.ui.Component;

public class Row extends Component {
    protected float gap = 0;
    protected Alignment crossAxisAlignment = Alignment.CENTER;

    public Row gap(float gap) { this.gap = Math.max(0, gap); return this; }
    public Row align(Alignment alignment) { this.crossAxisAlignment = alignment == null ? Alignment.CENTER : alignment; return this; }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float availableHeight = Math.max(0, constraints.maxHeight());
        float totalWidth = 0;
        float maxHeight = 0;
        int count = 0;
        for (Component child : children) {
            if (!child.visible()) continue;
            Size childSize = child.measure(context, new Constraints(constraints.maxWidth(), Math.max(0, availableHeight - child.margin().vertical())));
            totalWidth += childSize.width() + child.margin().horizontal();
            maxHeight = Math.max(maxHeight, childSize.height() + child.margin().vertical());
            count++;
        }
        if (count > 1) totalWidth += gap * (count - 1);
        return constraints.clamp(new Size(totalWidth, maxHeight));
    }

    @Override
    protected void layoutChildren(LayoutContext context) {
        Rect content = bounds.inset(padding.left(), padding.top(), padding.right(), padding.bottom());
        float fixedWidth = 0;
        float totalFlex = 0;
        int visibleCount = 0;

        for (Component child : children) {
            if (!child.visible()) continue;
            visibleCount++;
            if (child.flexGrow() > 0) totalFlex += child.flexGrow();
            else {
                Size childSize = child.measure(context, new Constraints(content.w(), Math.max(0, content.h() - child.margin().vertical())));
                fixedWidth += childSize.width() + child.margin().horizontal();
            }
        }
        if (visibleCount > 1) fixedWidth += gap * (visibleCount - 1);
        float remaining = Math.max(0, content.w() - fixedWidth);
        float x = content.x();

        for (Component child : children) {
            if (!child.visible()) continue;
            Size measured = child.measure(context, new Constraints(content.w(), Math.max(0, content.h() - child.margin().vertical())));
            float childWidth = child.flexGrow() > 0 && totalFlex > 0
                    ? Math.max(child.minWidth(), remaining * child.flexGrow() / totalFlex - child.margin().horizontal())
                    : measured.width();
            float childHeight = child.fillYValue() || crossAxisAlignment == Alignment.STRETCH
                    ? Math.max(0, content.h() - child.margin().vertical())
                    : Math.min(measured.height(), Math.max(0, content.h() - child.margin().vertical()));
            float y = content.y() + child.margin().top();
            float extra = Math.max(0, content.h() - child.margin().vertical() - childHeight);
            if (crossAxisAlignment == Alignment.CENTER) y += extra * 0.5f;
            else if (crossAxisAlignment == Alignment.END) y += extra;
            x += child.margin().left();
            child.layout(context, new Rect(x, y, childWidth, childHeight));
            x += childWidth + child.margin().right() + gap;
        }
    }
}
