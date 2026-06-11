package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.animation.AnimatedFloat;
import dev.someoneok.crystalconfig.input.*;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;
import dev.someoneok.crystalconfig.utils.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Dropdown<T> extends Component {
    private static final float OVERLAY_Z_BASE = 10000.0f;
    private final State<T> value;
    private final List<T> options;
    private Function<T, String> labeler = String::valueOf;
    private boolean expanded;
    private final AnimatedFloat expansion = new AnimatedFloat(0).speed(20);
    private float itemHeight = 28;
    private int maxVisibleRows = 6;
    private float scroll;
    private int hoveredIndex = -1;
    private boolean hoveringPanel;
    private boolean draggingScrollbar;
    private float scrollbarGrab;
    private boolean suppressTooltipUntilExit;

    public Dropdown(State<T> value, List<T> options) {
        this.value = value;
        this.options = new ArrayList<>(options);
        this.focusable = true;
        size(180, 30);
    }

    public Dropdown<T> labeler(Function<T, String> labeler) { this.labeler = labeler == null ? String::valueOf : labeler; return this; }
    public Dropdown<T> options(List<T> options) {
        this.options.clear();
        if (options != null) this.options.addAll(options);
        hoveredIndex = -1;
        clampScroll();
        markLayoutDirty();
        return this;
    }
    public Dropdown<T> itemHeight(float itemHeight) { this.itemHeight = Math.max(20, itemHeight); return this; }
    public Dropdown<T> maxVisibleRows(int rows) { this.maxVisibleRows = Math.max(1, rows); return this; }

    @Override
    public String tooltip() {
        return expanded || hoveringPanel || suppressTooltipUntilExit ? "" : super.tooltip();
    }

    @Override
    public void tick(float deltaSeconds) {
        expansion.target(expanded ? 1 : 0);
        expansion.update(deltaSeconds);
        clampScroll();
        super.tick(deltaSeconds);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 180, preferredHeight >= 0 ? preferredHeight : 30));
    }

    @Override
    protected void renderSelf(RenderContext context) {
        Rect base = headerRect();
        boolean disabled = !enabled();
        ColorRGBA fill = disabled ? context.theme().palette().surfaceAlt().withAlpha(120) : hovered || focused ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt();
        context.rect(base, SdfRectStyle.create()
                .fill(fill)
                .border(1, disabled ? context.theme().palette().border().withAlpha(95) : focused ? context.theme().palette().accent() : context.theme().palette().border())
                .radius(context.theme().radii().md()), z);
        String label = labeler.apply(value.get());
        float font = context.theme().fonts().normal();
        float ty = base.centerY() - context.lineHeight(font) * 0.5f;
        String shown = ellipsize(context, label, font, Math.max(8, base.w() - 36));
        Rect labelClip = new Rect(base.x() + 8, base.y(), Math.max(1, base.w() - 34), base.h());
        context.pushClip(labelClip);
        context.text(shown, base.x() + 10, ty, font, disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().text(), z + 1);
        context.popClip();
        context.text(expanded ? "▲" : "▼", base.right() - 18, ty, font, disabled ? context.theme().palette().mutedText().withAlpha(110) : context.theme().palette().mutedText(), z + 1);

    }

    @Override
    public void renderOverlay(RenderContext context) {
        super.renderOverlay(context);
        renderPanelOverlay(context);
    }

    private void renderPanelOverlay(RenderContext context) {
        float opened = expansion.value();
        if (opened <= 0.001f) return;
        clampScroll();
        Rect panel = panelRect(opened);
        float font = context.theme().fonts().normal();
        context.rect(panel, SdfRectStyle.create()
                .fill(context.theme().palette().surface())
                .border(1, context.theme().palette().border())
                .shadow(10, 0, 2, context.theme().palette().shadow().multiplyAlpha(opened * 0.6f))
                .radius(context.theme().radii().md())
                .opacity(opened), z + OVERLAY_Z_BASE + 50);

        Rect panelClip = panel.inset(1, 1, 1, 1);
        context.pushClip(panelClip);

        Rect viewport = itemsViewport(panel);
        context.pushClip(viewport);
        int start = Math.max(0, (int)(scroll / itemHeight));
        int end = Math.min(options.size(), start + visibleRows() + 2);
        float scrollOffset = scroll - start * itemHeight;
        for (int i = start; i < end; i++) {
            T option = options.get(i);
            Rect item = new Rect(viewport.x(), viewport.y() + (i - start) * itemHeight - scrollOffset, viewport.w(), itemHeight);
            if (item.bottom() < viewport.y() || item.y() > viewport.bottom()) continue;
            boolean selected = Objects.equals(option, value.get());
            boolean itemHovered = i == hoveredIndex && hoveringPanel;
            if (itemHovered) {
                context.rect(item, SdfRectStyle.create().fill(context.theme().palette().surfaceHover().withAlpha(170)).radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 51);
            }
            if (selected) {
                context.rect(item, SdfRectStyle.create().fill(context.theme().palette().accent().withAlpha(itemHovered ? 70 : 42)).radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 52);
            }
            String itemLabel = ellipsize(context, labeler.apply(option), font, Math.max(8, item.w() - 16));
            context.text(itemLabel, item.x() + 8, item.centerY() - context.lineHeight(font) * 0.5f, font,
                    selected ? context.theme().palette().accent() : context.theme().palette().text(), opened, false, z + OVERLAY_Z_BASE + 53);
        }
        context.popClip();

        renderScrollbar(context, panel, opened);
        context.popClip();
    }

    @Override
    public Component hitTest(float x, float y) {
        if (!visible() || !enabled()) return null;
        hoveringPanel = expanded && panelRect(1.0f).contains(x, y);
        if (!hoveringPanel) hoveredIndex = -1;
        if (headerRect().contains(x, y)) {
            if (!expanded) suppressTooltipUntilExit = false;
            return this;
        }
        if (hoveringPanel) return this;
        return null;
    }

    @Override
    public Component hitTestOverlay(float x, float y) {
        if (!visible() || !enabled()) return null;
        hoveringPanel = expanded && panelRect(1.0f).contains(x, y);
        if (!hoveringPanel) hoveredIndex = -1;
        if (hoveringPanel) return this;
        return super.hitTestOverlay(x, y);
    }

    @Override
    public boolean onMouseMove(MouseMoveEvent event) {
        if (!enabled()) return false;
        hoveringPanel = expanded && panelRect(1.0f).contains(event.x, event.y);
        if (!expanded && !headerRect().contains(event.x, event.y)) suppressTooltipUntilExit = false;
        hoveredIndex = hoveringPanel ? itemIndexAt(event.x, event.y) : -1;
        return hoveringPanel;
    }

    @Override
    public boolean onMouseScrolled(MouseScrollEvent event) {
        if (!enabled() || !expanded || !panelRect(1.0f).contains(event.x, event.y) || !needsScrollbar()) return false;
        scroll -= event.amountY * itemHeight;
        clampScroll();
        hoveredIndex = itemIndexAt(event.x, event.y);
        hoveringPanel = true;
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseDragEvent event) {
        if (!enabled() || !draggingScrollbar || !needsScrollbar()) return false;
        Rect panel = panelRect(1.0f);
        Rect track = scrollbarTrack(panel);
        Rect thumb = scrollbarThumb(panel);
        float travel = Math.max(1, track.h() - thumb.h());
        float t = MathUtil.clamp((event.y - track.y() - scrollbarGrab) / travel, 0, 1);
        scroll = t * maxScroll();
        clampScroll();
        hoveredIndex = -1;
        hoveringPanel = true;
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (!enabled() || event.button != MouseButton.LEFT) return false;
        if (headerRect().contains(event.x, event.y)) {
            expanded = !expanded;
            hoveringPanel = false;
            draggingScrollbar = false;
            hoveredIndex = -1;
            return true;
        }
        if (expanded) {
            Rect panel = panelRect(1.0f);
            if (panel.contains(event.x, event.y)) {
                if (needsScrollbar()) {
                    Rect thumb = scrollbarThumb(panel);
                    if (!thumb.isEmpty() && thumb.contains(event.x, event.y)) {
                        draggingScrollbar = true;
                        scrollbarGrab = event.y - thumb.y();
                        hoveredIndex = -1;
                        hoveringPanel = true;
                        return true;
                    }
                }

                int index = itemIndexAt(event.x, event.y);
                if (index >= 0 && index < options.size()) {
                    value.set(options.get(index));
                    expanded = false;
                    hoveringPanel = false;
                    hoveredIndex = -1;
                    suppressTooltipUntilExit = true;
                }
                return true;
            }
        }
        expanded = false;
        hoveringPanel = false;
        draggingScrollbar = false;
        hoveredIndex = -1;
        return false;
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled()) return false;
        int idx = Math.max(0, options.indexOf(value.get()));
        if (event.keyCode == KeyCodes.ENTER || event.keyCode == KeyCodes.SPACE) {
            expanded = !expanded;
            return true;
        }
        if (event.keyCode == KeyCodes.DOWN && !options.isEmpty()) {
            int next = Math.min(options.size() - 1, idx + 1);
            value.set(options.get(next));
            ensureVisible(next);
            return true;
        }
        if (event.keyCode == KeyCodes.UP && !options.isEmpty()) {
            int next = Math.max(0, idx - 1);
            value.set(options.get(next));
            ensureVisible(next);
            return true;
        }
        if (event.keyCode == KeyCodes.ESCAPE && expanded) {
            expanded = false;
            return true;
        }
        return false;
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        if (!focused) {
            expanded = false;
            hoveringPanel = false;
            draggingScrollbar = false;
            hoveredIndex = -1;
        }
    }

    private static String ellipsize(RenderContext context, String value, float font, float maxWidth) {
        String s = value == null ? "" : value;
        if (context.measureText(s, font).width() <= maxWidth) return s;
        String ellipsis = "...";
        if (context.measureText(ellipsis, font).width() > maxWidth) return "";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (context.measureText(s.substring(0, mid) + ellipsis, font).width() <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }

    private Rect headerRect() {
        return new Rect(bounds.x(), bounds.y(), bounds.w(), bounds.h());
    }

    private int visibleRows() {
        return Math.min(options.size(), maxVisibleRows);
    }

    private boolean needsScrollbar() {
        return options.size() > visibleRows();
    }

    private float fullPanelHeight() {
        return 8 + visibleRows() * itemHeight;
    }

    private Rect panelRect(float opened) {
        float fullHeight = fullPanelHeight();
        float height = Math.max(0, fullHeight * opened);
        Rect available = availableBounds();
        float belowY = bounds.y() + bounds.h() + 4;
        float aboveY = bounds.y() - 4 - height;
        float belowSpace = available.bottom() - belowY;
        float aboveSpace = bounds.y() - available.y();
        boolean openAbove = belowSpace < fullHeight && aboveSpace > belowSpace;
        float y = openAbove ? aboveY : belowY;
        y = Math.max(available.y(), Math.min(y, available.bottom() - height));
        return new Rect(bounds.x(), y, bounds.w(), height);
    }

    private Rect itemsViewport(Rect panel) {
        float rightPad = needsScrollbar() ? 12 : 4;
        return panel.inset(4, 4, rightPad, 4);
    }

    private void renderScrollbar(RenderContext context, Rect panel, float opened) {
        if (!needsScrollbar()) return;

        Rect track = scrollbarTrack(panel);
        Rect thumb = scrollbarThumb(panel);
        int trackAlpha = (int) (60 * opened);
        int thumbAlpha = (int) (185 * opened);

        context.rect(track, SdfRectStyle.create()
                .fill(context.theme().palette().surfaceAlt().withAlpha(trackAlpha))
                .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 54);
        context.rect(thumb, SdfRectStyle.create()
                .fill(context.theme().palette().surfaceActive().withAlpha(draggingScrollbar ? Math.max(thumbAlpha, 220) : thumbAlpha))
                .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 55);
    }
    private Rect scrollbarTrack(Rect panel) {
        return new Rect(panel.right() - 9, panel.y() + 4, 5, Math.max(0, panel.h() - 8));
    }

    private Rect scrollbarThumb(Rect panel) {
        Rect track = scrollbarTrack(panel);
        float max = maxScroll();
        if (max <= 0) return Rect.ZERO;
        float total = options.size() * itemHeight;
        float visible = visibleRows() * itemHeight;
        float ratio = MathUtil.clamp(visible / Math.max(1, total), 0.12f, 1.0f);
        float h = Math.max(12, track.h() * ratio);
        float y = track.y() + (scroll / max) * Math.max(0, track.h() - h);
        return new Rect(track.x(), y, track.w(), h);
    }

    private int itemIndexAt(float x, float y) {
        Rect panel = panelRect(1.0f);
        Rect viewport = itemsViewport(panel);
        if (!viewport.contains(x, y)) return -1;
        int index = (int)((y - viewport.y() + scroll) / itemHeight);
        return index >= 0 && index < options.size() ? index : -1;
    }

    private float maxScroll() {
        return Math.max(0, options.size() * itemHeight - visibleRows() * itemHeight);
    }

    private void clampScroll() {
        scroll = MathUtil.clamp(scroll, 0, maxScroll());
    }

    private void ensureVisible(int index) {
        float y = index * itemHeight;
        float bottom = y + itemHeight;
        float visible = visibleRows() * itemHeight;
        if (y < scroll) scroll = y;
        else if (bottom > scroll + visible) scroll = bottom - visible;
        clampScroll();
    }

    private Rect availableBounds() {
        for (Component c = parent(); c != null; c = c.parent()) {
            if ("ScrollContainer".equals(c.getClass().getSimpleName())) {
                return c.bounds();
            }
        }
        Component root = this;
        while (root.parent() != null) root = root.parent();
        return root.bounds();
    }
}
