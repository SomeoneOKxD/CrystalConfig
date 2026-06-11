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

import java.util.*;
import java.util.function.Function;

public class DraggableEnumList<T extends Enum<T>> extends Component {
    private static final float OVERLAY_Z_BASE = 10000.0f;
    private static final String MOVE_ICON = "☰";
    private static final String TRASH_ICON = "✕";

    private final State<List<T>> value;
    private final List<T> allOptions;
    private Function<T, String> labeler = String::valueOf;
    private boolean allowEmpty = true;
    private boolean allowDeleting = true;
    private float rowHeight = 26;
    private float gap = 5;
    private float addButtonHeight = 26;
    private float collapsedHeight = 32;
    private int maxVisibleAddRows = 6;
    private float addScroll;
    private int hoveredRow = -1;
    private int hoveredTrashRow = -1;
    private boolean hoveredAddButton;
    private int hoveredAddOption = -1;
    private boolean hoveringAddPanel;
    private boolean addOpen;
    private final AnimatedFloat addExpansion = new AnimatedFloat(0).speed(20);
    private boolean expanded;
    private int draggingIndex = -1;
    private float dragY;

    public DraggableEnumList(State<List<T>> value, Class<T> enumType) {
        this(value, Arrays.asList(enumType.getEnumConstants()));
    }

    public DraggableEnumList(State<List<T>> value, List<T> options) {
        this.value = Objects.requireNonNull(value, "value");
        this.allOptions = new ArrayList<>(Objects.requireNonNull(options, "options"));
        this.focusable = true;
        width(220);
    }

    public DraggableEnumList<T> labeler(Function<T, String> labeler) { this.labeler = labeler == null ? String::valueOf : labeler; return this; }
    public DraggableEnumList<T> allowEmpty(boolean allowEmpty) { this.allowEmpty = allowEmpty; return this; }
    public DraggableEnumList<T> allowDeleting(boolean allowDeleting) { this.allowDeleting = allowDeleting; if (!allowDeleting) addOpen = false; markLayoutDirty(); return this; }
    public DraggableEnumList<T> rowHeight(float rowHeight) { this.rowHeight = Math.max(20, rowHeight); markLayoutDirty(); return this; }
    public DraggableEnumList<T> collapsedHeight(float collapsedHeight) { this.collapsedHeight = Math.max(24, collapsedHeight); markLayoutDirty(); return this; }
    public DraggableEnumList<T> maxVisibleAddRows(int rows) { this.maxVisibleAddRows = Math.max(1, rows); return this; }

    @Override
    public void tick(float deltaSeconds) {
        addExpansion.target(addOpen ? 1 : 0);
        addExpansion.update(deltaSeconds);
        clampAddScroll(missingOptions().size());
        super.tick(deltaSeconds);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float h = expandedView() ? expandedHeight() : collapsedHeight;
        return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 220, h));
    }

    private float expandedHeight() {
        int rows = current().size();
        float h = showAddButton() ? addButtonHeight : 0;
        if (rows > 0) h += (h > 0 ? gap : 0) + rows * rowHeight + Math.max(0, rows - 1) * gap;
        if (rows == 0 && !showAddButton()) h = addButtonHeight;
        return Math.max(collapsedHeight, h);
    }

    @Override
    protected void renderSelf(RenderContext context) {
        List<T> items = current();
        if (!expandedView()) {
            renderCollapsed(context, items);
            return;
        }

        float font = context.theme().fonts().normal();

        if (showAddButton()) renderAddButton(context, missingOptions());

        for (int i = 0; i < items.size(); i++) {
            if (i == draggingIndex) continue;
            renderRow(context, rowRect(i), items.get(i), i, false, i == hoveredRow);
        }
        if (draggingIndex >= 0 && draggingIndex < items.size()) {
            Rect row = rowRect(draggingIndex);
            Rect ghost = new Rect(row.x(), clampedDragY(items.size()) - row.h() / 2.0f, row.w(), row.h());
            renderRow(context, ghost, items.get(draggingIndex), draggingIndex, true, true);
        }
        if (items.isEmpty() && missingOptions().isEmpty()) {
            Rect empty = new Rect(bounds.x(), rowsStartY(), bounds.w(), addButtonHeight);
            context.rect(empty, SdfRectStyle.create().fill(context.theme().palette().surfaceAlt().withAlpha(110)).border(1, context.theme().palette().border()).radius(context.theme().radii().md()), z);
            context.text("No entries", empty.x() + 9, empty.centerY() - context.lineHeight(font) * 0.5f, font, context.theme().palette().mutedText(), z + 1);
        }
    }

    @Override
    public void renderOverlay(RenderContext context) {
        super.renderOverlay(context);
        if (showAddButton() && (addOpen || addExpansion.value() > 0.001f) && !missingOptions().isEmpty()) renderAddDropdown(context, missingOptions());
    }

    private void renderCollapsed(RenderContext context, List<T> items) {
        float font = context.theme().fonts().normal();
        boolean disabled = !enabled();
        Rect box = new Rect(bounds.x(), bounds.y(), bounds.w(), Math.min(bounds.h(), collapsedHeight));
        ColorRGBA fill = disabled
                ? context.theme().palette().surfaceAlt().withAlpha(115)
                : hovered ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt().withAlpha(210);
        ColorRGBA border = disabled
                ? context.theme().palette().border().withAlpha(95)
                : hovered || focused ? context.theme().palette().accent().withAlpha(185) : context.theme().palette().border();
        ColorRGBA primary = disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().text();
        ColorRGBA muted = disabled ? context.theme().palette().mutedText().withAlpha(120) : context.theme().palette().mutedText();
        context.rect(box, SdfRectStyle.create()
                .fill(fill)
                .border(1, border)
                .radius(context.theme().radii().md()), z);

        String count = items.isEmpty() ? "No entries" : items.size() + (items.size() == 1 ? " entry" : " entries");
        float textY = box.centerY() - context.lineHeight(font) * 0.5f;
        context.text(count, box.x() + 9, textY, font, primary, z + 1);

        String action = "Show more";
        float actionWidth = context.measureText(action, font).width();
        float arrowWidth = context.measureText("▼", font).width();
        float actionX = Math.max(box.x() + 9, box.right() - actionWidth - arrowWidth - 20);
        context.text(action, actionX, textY, font, muted, z + 1);
        context.text("▼", box.right() - arrowWidth - 8, textY, font, muted, z + 1);
    }

    private void renderAddButton(RenderContext context, List<T> missing) {
        boolean disabled = missing.isEmpty();
        float font = context.theme().fonts().normal();
        Rect add = addButtonRect();
        ColorRGBA fill = disabled
                ? context.theme().palette().surfaceAlt().withAlpha(110)
                : (hoveredAddButton || addOpen ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt()).withAlpha(210);
        ColorRGBA text = disabled ? context.theme().palette().mutedText().withAlpha(120) : context.theme().palette().mutedText();
        context.rect(add, SdfRectStyle.create()
                .fill(fill)
                .border(1, addOpen ? context.theme().palette().accent() : context.theme().palette().border())
                .radius(context.theme().radii().md()), z);
        context.text(ellipsize(context, "+ Add", font, Math.max(8, add.w() - 34)), add.x() + 9,
                add.centerY() - context.lineHeight(font) * 0.5f, font, text, z + 1);
        context.text(addOpen ? "▲" : "▼", add.right() - 18, add.centerY() - context.lineHeight(font) * 0.5f, font, text, z + 1);
    }

    private void renderAddDropdown(RenderContext context, List<T> missing) {
        float opened = addExpansion.value();
        if (opened <= 0.001f) return;
        clampAddScroll(missing.size());
        float font = context.theme().fonts().normal();
        Rect panel = addPanelRect(opened);
        context.rect(panel, SdfRectStyle.create()
                .fill(context.theme().palette().surface())
                .border(1, context.theme().palette().border())
                .shadow(10, 0, 2, context.theme().palette().shadow().multiplyAlpha(opened * 0.6f))
                .radius(context.theme().radii().md())
                .opacity(opened), z + OVERLAY_Z_BASE + 50);

        Rect viewport = addItemsViewport(panel);
        context.pushClip(viewport);
        int start = Math.max(0, (int)(addScroll / rowHeight));
        int end = Math.min(missing.size(), start + visibleAddRows() + 2);
        float scrollOffset = addScroll - start * rowHeight;
        for (int i = start; i < end; i++) {
            Rect option = new Rect(viewport.x(), viewport.y() + (i - start) * rowHeight - scrollOffset, viewport.w(), rowHeight);
            if (option.bottom() < viewport.y() || option.y() > viewport.bottom()) continue;
            if (i == hoveredAddOption && hoveringAddPanel) {
                context.rect(option, SdfRectStyle.create()
                        .fill(context.theme().palette().surfaceHover().withAlpha(170))
                        .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 51);
            }
            context.text(ellipsize(context, labeler.apply(missing.get(i)), font, Math.max(8, option.w() - 16)), option.x() + 8,
                    option.centerY() - context.lineHeight(font) * 0.5f, font, context.theme().palette().text(), opened, false, z + OVERLAY_Z_BASE + 52);
        }
        context.popClip();

        if (needsAddScrollbar(missing)) {
            Rect track = addScrollbarTrack(panel);
            Rect thumb = addScrollbarThumb(panel, missing.size());
            context.rect(track, SdfRectStyle.create()
                    .fill(context.theme().palette().surfaceAlt().withAlpha(60))
                    .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 53);
            context.rect(thumb, SdfRectStyle.create()
                    .fill(context.theme().palette().surfaceActive().withAlpha(185))
                    .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 54);
        }
    }

    private void renderRow(RenderContext context, Rect row, T item, int index, boolean floating, boolean hover) {
        float font = context.theme().fonts().normal();
        ColorRGBA fill = hover ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt();
        context.rect(row, SdfRectStyle.create()
                .fill(fill.withAlpha(floating ? 235 : 210))
                .border(1, floating ? context.theme().palette().accent() : context.theme().palette().border())
                .shadow(floating ? 10 : 0, 0, 2, context.theme().palette().shadow().withAlpha(floating ? 120 : 0))
                .radius(context.theme().radii().md()), z + (floating ? 20 : 0));
        context.text(MOVE_ICON, row.x() + 8, row.centerY() - context.lineHeight(font) * 0.5f, font, context.theme().palette().mutedText(), z + (floating ? 21 : 1));
        context.text(ellipsize(context, labeler.apply(item), font, Math.max(8, row.w() - 62)), row.x() + 28,
                row.centerY() - context.lineHeight(font) * 0.5f, font, context.theme().palette().text(), z + (floating ? 21 : 1));
        if (canDelete(index)) {
            String face = context.theme().fonts().semibold();
            ColorRGBA trash = index == hoveredTrashRow && !floating ? context.theme().palette().accent() : context.theme().palette().mutedText();
            context.text(
                    TRASH_ICON,
                    row.right() - 20,
                    row.centerY() - context.lineHeight(font) * 0.5f,
                    font,
                    face,
                    trash,
                    z + (floating ? 21 : 1)
            );
        }
    }

    @Override
    public boolean needsFreshRender() { return draggingIndex >= 0 || super.needsFreshRender(); }

    @Override
    public Component hitTest(float x, float y) {
        if (!visible() || !enabled()) return null;
        if (!expandedView()) return bounds.contains(x, y) ? this : null;
        if (showAddButton() && addButtonRect().contains(x, y)) return this;
        for (int i = 0; i < current().size(); i++) if (rowRect(i).contains(x, y)) return this;
        return null;
    }

    @Override
    public Component hitTestOverlay(float x, float y) {
        if (!visible() || !enabled() || !expandedView()) return null;
        hoveringAddPanel = showAddButton() && addOpen && addPanelRect(1.0f).contains(x, y);
        if (!hoveringAddPanel) hoveredAddOption = -1;
        return hoveringAddPanel ? this : super.hitTestOverlay(x, y);
    }

    @Override
    public boolean onMouseMove(MouseMoveEvent event) {
        if (!expandedView()) {
            hoveredAddButton = false;
            hoveringAddPanel = false;
            hoveredAddOption = -1;
            hoveredRow = -1;
            hoveredTrashRow = -1;
            return bounds.contains(event.x, event.y);
        }
        hoveredAddButton = showAddButton() && addButtonRect().contains(event.x, event.y) && !missingOptions().isEmpty();
        hoveringAddPanel = showAddButton() && addOpen && addPanelRect(1.0f).contains(event.x, event.y);
        hoveredAddOption = hoveringAddPanel ? addOptionIndexAt(event.x, event.y) : -1;
        hoveredRow = rowIndexAt(event.x, event.y);
        hoveredTrashRow = trashIndexAt(event.x, event.y);
        return hoveredAddButton || hoveringAddPanel || hoveredRow >= 0 || hoveredTrashRow >= 0;
    }

    @Override
    public boolean onMouseScrolled(MouseScrollEvent event) {
        if (!expandedView()) return false;
        List<T> missing = missingOptions();
        if (!showAddButton() || !addOpen || !addPanelRect(1.0f).contains(event.x, event.y) || !needsAddScrollbar(missing)) return false;
        addScroll -= event.amountY * rowHeight;
        clampAddScroll(missing.size());
        hoveredAddOption = addOptionIndexAt(event.x, event.y);
        hoveringAddPanel = true;
        return true;
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (event.button != MouseButton.LEFT) return false;
        if (!expandedView()) {
            setExpanded(true);
            return bounds.contains(event.x, event.y);
        }
        List<T> items = current();
        List<T> missing = missingOptions();
        if (showAddButton() && addButtonRect().contains(event.x, event.y)) {
            if (!missing.isEmpty()) {
                addOpen = !addOpen;
                clampAddScroll(missing.size());
            }
            return true;
        }
        if (showAddButton() && addOpen) {
            if (addPanelRect(1.0f).contains(event.x, event.y)) {
                int option = addOptionIndexAt(event.x, event.y);
                if (option >= 0 && option < missing.size()) {
                    items.add(missing.get(option));
                    commit(items);
                    List<T> remaining = missingOptions();
                    addOpen = !remaining.isEmpty();
                    clampAddScroll(remaining.size());
                    markLayoutDirty();
                }
                return true;
            }
            addOpen = false;
        }
        int row = rowIndexAt(event.x, event.y);
        if (row >= 0 && row < items.size()) {
            Rect rr = rowRect(row);
            if (canDelete(row) && trashRect(rr).contains(event.x, event.y)) {
                items.remove(row);
                commit(items);
                markLayoutDirty();
                return true;
            }
            draggingIndex = row;
            dragY = clampedDragY(items.size(), event.y);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDragged(MouseDragEvent event) {
        if (event.button != MouseButton.LEFT || draggingIndex < 0) return false;
        List<T> items = current();
        if (draggingIndex >= items.size()) return true;
        dragY = clampedDragY(items.size(), event.y);
        int target = dragTargetIndexAt(dragY, items.size());
        if (target != draggingIndex) {
            T moved = items.remove(draggingIndex);
            items.add(target, moved);
            draggingIndex = target;
            commit(items);
        }
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        if (draggingIndex >= 0) {
            draggingIndex = -1;
            return true;
        }
        return false;
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        if (!focused) {
            setExpanded(false);
            addOpen = false;
            hoveringAddPanel = false;
            hoveredAddOption = -1;
            hoveredAddButton = false;
            hoveredRow = -1;
            hoveredTrashRow = -1;
        }
        markLayoutDirty();
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (event.keyCode == KeyCodes.ENTER || event.keyCode == KeyCodes.SPACE) {
            setExpanded(!expandedView());
            if (!expanded) addOpen = false;
            return true;
        }
        if (event.keyCode == KeyCodes.ESCAPE && expandedView()) {
            setExpanded(false);
            addOpen = false;
            return true;
        }
        return false;
    }

    private boolean expandedView() {
        return expanded || focused || draggingIndex >= 0 || addOpen;
    }

    private void setExpanded(boolean expanded) {
        if (this.expanded == expanded) return;
        this.expanded = expanded;
        markLayoutDirty();
    }

    private boolean showAddButton() { return allowDeleting; }

    private boolean canDelete(int index) {
        return allowDeleting && (allowEmpty || current().size() > 1) && index >= 0;
    }

    private List<T> current() {
        List<T> raw = value.get();
        Set<T> seen = new LinkedHashSet<>();
        if (raw != null) {
            for (T item : raw) if (item != null && allOptions.contains(item)) seen.add(item);
        }
        return new ArrayList<>(seen);
    }

    private List<T> missingOptions() {
        List<T> items = current();
        List<T> missing = new ArrayList<>();
        for (T option : allOptions) if (!items.contains(option)) missing.add(option);
        return missing;
    }

    private void commit(List<T> items) { value.set(new ArrayList<>(items)); }

    private Rect addButtonRect() {
        return new Rect(bounds.x(), bounds.y(), bounds.w(), addButtonHeight);
    }

    private float rowsStartY() {
        return bounds.y() + (showAddButton() ? addButtonHeight + gap : 0);
    }

    private Rect rowRect(int index) {
        return new Rect(bounds.x(), rowsStartY() + index * (rowHeight + gap), bounds.w(), rowHeight);
    }

    private Rect trashRect(Rect row) {
        return new Rect(row.right() - 32, row.y(), 32, row.h());
    }

    private int rowIndexAt(float x, float y) {
        float localY = y - rowsStartY();
        if (localY < 0) return -1;
        int idx = (int)(localY / (rowHeight + gap));
        List<T> items = current();
        if (idx < 0 || idx >= items.size()) return -1;
        Rect rr = rowRect(idx);
        return rr.contains(x, y) ? idx : -1;
    }

    private int dragTargetIndexAt(float y, int itemCount) {
        if (itemCount <= 0) return -1;
        float firstCenter = rowsStartY() + rowHeight / 2.0f;
        float step = rowHeight + gap;
        int target = Math.round((y - firstCenter) / step);
        return Math.max(0, Math.min(itemCount - 1, target));
    }

    private int trashIndexAt(float x, float y) {
        int idx = rowIndexAt(x, y);
        return idx >= 0 && canDelete(idx) && trashRect(rowRect(idx)).contains(x, y) ? idx : -1;
    }

    private int visibleAddRows() {
        return Math.min(missingOptions().size(), maxVisibleAddRows);
    }

    private boolean needsAddScrollbar(List<T> missing) {
        return missing.size() > Math.min(missing.size(), maxVisibleAddRows);
    }

    private float maxAddScroll(int optionCount) {
        return Math.max(0, optionCount * rowHeight - Math.min(optionCount, maxVisibleAddRows) * rowHeight);
    }

    private void clampAddScroll(int optionCount) {
        addScroll = MathUtil.clamp(addScroll, 0, maxAddScroll(optionCount));
    }

    private Rect addPanelRect(float opened) {
        float fullHeight = fullAddPanelHeight();
        float height = Math.max(0, fullHeight * opened);
        Rect available = availableBounds();
        float belowY = bounds.y() + addButtonHeight + 4;
        float aboveY = bounds.y() - 4 - height;
        float belowSpace = available.bottom() - belowY;
        float aboveSpace = bounds.y() - available.y();
        boolean openAbove = belowSpace < fullHeight && aboveSpace > belowSpace;
        float y = openAbove ? aboveY : belowY;
        y = Math.max(available.y(), Math.min(y, available.bottom() - height));
        return new Rect(bounds.x(), y, bounds.w(), height);
    }

    private float fullAddPanelHeight() {
        return 8 + visibleAddRows() * rowHeight;
    }

    private Rect addItemsViewport(Rect panel) {
        float rightPad = needsAddScrollbar(missingOptions()) ? 12 : 4;
        return panel.inset(4, 4, rightPad, 4);
    }

    private Rect addScrollbarTrack(Rect panel) {
        return new Rect(panel.right() - 9, panel.y() + 4, 5, Math.max(0, panel.h() - 8));
    }

    private Rect addScrollbarThumb(Rect panel, int optionCount) {
        Rect track = addScrollbarTrack(panel);
        float max = maxAddScroll(optionCount);
        if (max <= 0) return Rect.ZERO;
        float total = optionCount * rowHeight;
        float visible = Math.min(optionCount, maxVisibleAddRows) * rowHeight;
        float ratio = MathUtil.clamp(visible / Math.max(1, total), 0.12f, 1.0f);
        float h = Math.max(12, track.h() * ratio);
        float y = track.y() + (addScroll / max) * Math.max(0, track.h() - h);
        return new Rect(track.x(), y, track.w(), h);
    }

    private int addOptionIndexAt(float x, float y) {
        List<T> missing = missingOptions();
        if (!showAddButton() || !addOpen || missing.isEmpty()) return -1;
        Rect panel = addPanelRect(1.0f);
        Rect viewport = addItemsViewport(panel);
        if (!viewport.contains(x, y)) return -1;
        int idx = (int)((y - viewport.y() + addScroll) / rowHeight);
        return idx >= 0 && idx < missing.size() ? idx : -1;
    }

    private float clampedDragY(int itemCount) { return clampedDragY(itemCount, dragY); }

    private float clampedDragY(int itemCount, float y) {
        if (itemCount <= 0) return y;
        float min = rowsStartY() + rowHeight / 2.0f;
        float max = rowRect(itemCount - 1).centerY();
        return MathUtil.clamp(y, min, max);
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

    private static String ellipsize(RenderContext context, String value, float fontSize, float maxWidth) {
        String text = value == null ? "" : value;
        if (context.measureText(text, fontSize, context.theme().fonts().regular()).width() <= maxWidth) return text;
        String ellipsis = "...";
        if (context.measureText(ellipsis, fontSize, context.theme().fonts().regular()).width() > maxWidth) return "";
        int lo = 0, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (context.measureText(text.substring(0, mid) + ellipsis, fontSize, context.theme().fonts().regular()).width() <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return text.substring(0, lo) + ellipsis;
    }
}
