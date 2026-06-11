package dev.someoneok.crystalconfig.containers;

import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.ui.Component;

/** A centered icon row that wraps without shrinking icon buttons. */
public class WrappingIconRow extends Component {
    private float gap = 8.0f;
    private float rowGap = 8.0f;

    public WrappingIconRow gap(float gap) { this.gap = Math.max(0, gap); markLayoutDirty(); return this; }
    public WrappingIconRow rowGap(float rowGap) { this.rowGap = Math.max(0, rowGap); markLayoutDirty(); return this; }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float availableWidth = Math.max(0, constraints.maxWidth() - padding.horizontal());
        Metrics metrics = measureRows(context, availableWidth);
        return constraints.clamp(new Size(metrics.maxWidth + padding.horizontal(), metrics.height + padding.vertical()));
    }

    @Override
    protected void layoutChildren(LayoutContext context) {
        Rect content = bounds.inset(padding.left(), padding.top(), padding.right(), padding.bottom());
        float x = content.x();
        float y = content.y();
        float rowWidth = 0.0f;
        float rowHeight = 0.0f;
        int rowStart = 0;
        int visibleInRow = 0;

        for (int i = 0; i < children.size(); i++) {
            Component child = children.get(i);
            if (!child.visible()) continue;
            Size size = child.measure(context, new Constraints(content.w(), content.h()));
            float childWidth = size.width() + child.margin().horizontal();
            float childHeight = size.height() + child.margin().vertical();
            boolean wrap = visibleInRow > 0 && rowWidth + gap + childWidth > content.w();
            if (wrap) {
                layoutRow(context, rowStart, i, content.x() + Math.max(0, content.w() - rowWidth) * 0.5f, y, rowHeight);
                y += rowHeight + rowGap;
                rowStart = i;
                rowWidth = 0.0f;
                rowHeight = 0.0f;
                visibleInRow = 0;
            }
            if (visibleInRow > 0) rowWidth += gap;
            rowWidth += childWidth;
            rowHeight = Math.max(rowHeight, childHeight);
            visibleInRow++;
        }
        if (visibleInRow > 0) {
            layoutRow(context, rowStart, children.size(), content.x() + Math.max(0, content.w() - rowWidth) * 0.5f, y, rowHeight);
        }
    }

    private void layoutRow(LayoutContext context, int startInclusive, int endExclusive, float x, float y, float rowHeight) {
        float cursor = x;
        boolean first = true;
        for (int i = startInclusive; i < endExclusive; i++) {
            Component child = children.get(i);
            if (!child.visible()) continue;
            Size size = child.measure(context, new Constraints(bounds.w(), rowHeight));
            if (!first) cursor += gap;
            first = false;
            cursor += child.margin().left();
            float childY = y + child.margin().top() + Math.max(0, rowHeight - child.margin().vertical() - size.height()) * 0.5f;
            child.layout(context, new Rect(cursor, childY, size.width(), size.height()));
            cursor += size.width() + child.margin().right();
        }
    }

    private Metrics measureRows(LayoutContext context, float availableWidth) {
        float maxWidth = 0.0f;
        float totalHeight = 0.0f;
        float rowWidth = 0.0f;
        float rowHeight = 0.0f;
        int visibleInRow = 0;
        int rowCount = 0;

        for (Component child : children) {
            if (!child.visible()) continue;
            Size size = child.measure(context, new Constraints(availableWidth, 100000));
            float childWidth = size.width() + child.margin().horizontal();
            float childHeight = size.height() + child.margin().vertical();
            boolean wrap = visibleInRow > 0 && rowWidth + gap + childWidth > availableWidth;
            if (wrap) {
                maxWidth = Math.max(maxWidth, rowWidth);
                totalHeight += rowHeight;
                rowCount++;
                rowWidth = 0.0f;
                rowHeight = 0.0f;
                visibleInRow = 0;
            }
            if (visibleInRow > 0) rowWidth += gap;
            rowWidth += childWidth;
            rowHeight = Math.max(rowHeight, childHeight);
            visibleInRow++;
        }
        if (visibleInRow > 0) {
            maxWidth = Math.max(maxWidth, rowWidth);
            totalHeight += rowHeight;
            rowCount++;
        }
        if (rowCount > 1) totalHeight += rowGap * (rowCount - 1);
        return new Metrics(maxWidth, totalHeight);
    }

    private record Metrics(float maxWidth, float height) { }
}
