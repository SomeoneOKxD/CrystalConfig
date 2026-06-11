package dev.someoneok.crystalconfig.containers;

import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.ui.Component;

public class Stack extends Component {
    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float maxW = 0;
        float maxH = 0;
        for (Component child : children) {
            if (!child.visible()) continue;
            Size childSize = child.measure(context, constraints);
            maxW = Math.max(maxW, childSize.width() + child.margin().horizontal());
            maxH = Math.max(maxH, childSize.height() + child.margin().vertical());
        }
        return constraints.clamp(new Size(maxW, maxH));
    }

    @Override
    protected void layoutChildren(LayoutContext context) {
        Rect content = bounds.inset(padding.left(), padding.top(), padding.right(), padding.bottom());
        for (Component child : children) {
            if (!child.visible()) continue;
            Size measured = child.measure(context, new Constraints(content.w(), content.h()));
            if (child.absoluteLayout()) {
                float w = child.absoluteWidth() >= 0 ? child.absoluteWidth() : measured.width();
                float h = child.absoluteHeight() >= 0 ? child.absoluteHeight() : measured.height();
                child.layout(context, new Rect(content.x() + child.absoluteX(), content.y() + child.absoluteY(), w, h));
            } else {
                child.layout(context, new Rect(
                        content.x() + child.margin().left(),
                        content.y() + child.margin().top(),
                        child.fillXValue() ? Math.max(0, content.w() - child.margin().horizontal()) : measured.width(),
                        child.fillYValue() ? Math.max(0, content.h() - child.margin().vertical()) : measured.height()
                ));
            }
        }
    }
}
