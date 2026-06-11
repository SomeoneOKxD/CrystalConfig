package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.animation.AnimatedFloat;
import dev.someoneok.crystalconfig.input.*;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.*;
import dev.someoneok.crystalconfig.state.MutableState;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;
import dev.someoneok.crystalconfig.utils.MathUtil;

import java.util.*;
import java.util.function.Function;

/**
 * Single-select dropdown that renders options under non-interactive group label rows.
 *
 * <p>The insertion order of the provided map is preserved, so pass a {@link LinkedHashMap}
 * when the visual group order matters.</p>
 */
public class GroupedDropdown<G extends Enum<G>, T> extends Component {
    private static final float OVERLAY_Z_BASE = 10000.0f;
    private static final float HEADER_HEIGHT = 30;
    private static final float PANEL_GAP = 4;
    private static final float SEARCH_HEIGHT = 26;
    private static final float SEARCH_TOP_PADDING = 6;
    private static final float SEARCH_SIDE_PADDING = 6;
    private static final float DIVIDER_TOP_GAP = 5;
    private static final float DIVIDER_HEIGHT = 1;
    private static final float LIST_TOP_GAP = 5;
    private static final float LIST_BOTTOM_PADDING = 4;

    private final State<T> value;
    private final LinkedHashMap<G, List<T>> groupedOptions = new LinkedHashMap<>();
    private final boolean searchable;
    private final MutableState<String> search = new MutableState<>("");
    private final TextInput searchInput = new TextInput(search).placeholder("Search...");
    private final List<Row<G, T>> rows = new ArrayList<>();
    private final List<T> selectableOptions = new ArrayList<>();

    private String lastSearch = null;
    private Function<T, String> labeler = String::valueOf;
    private Function<G, String> groupLabeler = String::valueOf;

    private boolean expanded;
    private final AnimatedFloat expansion = new AnimatedFloat(0).speed(20);

    private float itemHeight = 28;
    private float groupHeight = 24;
    private int maxVisibleRows = 6;
    private float scroll;
    private int hoveredRowIndex = -1;
    private boolean hoveringPanel;
    private boolean draggingScrollbar;
    private float scrollbarGrab;
    private boolean suppressTooltipUntilExit;

    public GroupedDropdown(State<T> value, Map<G, ? extends Collection<T>> groupedOptions) {
        this(value, groupedOptions, false);
    }

    public GroupedDropdown(State<T> value, Map<G, ? extends Collection<T>> groupedOptions, boolean searchable) {
        this.value = value;
        this.searchable = searchable;
        groupedOptions(groupedOptions);
        this.focusable = true;
        this.searchInput.height(SEARCH_HEIGHT);
        this.searchInput.visible(false);
        if (searchable) this.add(searchInput);
        size(180, HEADER_HEIGHT);
    }

    public GroupedDropdown<G, T> labeler(Function<T, String> labeler) {
        this.labeler = labeler == null ? String::valueOf : labeler;
        this.lastSearch = null;
        return this;
    }

    public GroupedDropdown<G, T> groupLabeler(Function<G, String> groupLabeler) {
        this.groupLabeler = groupLabeler == null ? String::valueOf : groupLabeler;
        this.lastSearch = null;
        return this;
    }

    public GroupedDropdown<G, T> groupedOptions(Map<G, ? extends Collection<T>> groupedOptions) {
        this.groupedOptions.clear();
        if (groupedOptions != null) {
            for (Map.Entry<G, ? extends Collection<T>> entry : groupedOptions.entrySet()) {
                List<T> values = new ArrayList<>();
                if (entry.getValue() != null) values.addAll(entry.getValue());
                this.groupedOptions.put(entry.getKey(), values);
            }
        }
        this.lastSearch = null;
        this.scroll = 0;
        this.hoveredRowIndex = -1;
        refreshRows();
        clampScroll();
        markLayoutDirty();
        return this;
    }

    public GroupedDropdown<G, T> itemHeight(float itemHeight) {
        this.itemHeight = Math.max(20, itemHeight);
        return this;
    }

    public GroupedDropdown<G, T> groupHeight(float groupHeight) {
        this.groupHeight = Math.max(18, groupHeight);
        return this;
    }

    public GroupedDropdown<G, T> maxVisibleRows(int rows) {
        this.maxVisibleRows = Math.max(1, rows);
        return this;
    }

    @Override
    public String tooltip() {
        return expanded || hoveringPanel || suppressTooltipUntilExit ? "" : super.tooltip();
    }

    @Override
    public void tick(float deltaSeconds) {
        expansion.target(expanded ? 1 : 0);
        expansion.update(deltaSeconds);
        refreshRows();
        clampScroll();
        super.tick(deltaSeconds);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 180, HEADER_HEIGHT));
    }

    @Override
    protected void layoutChildren(LayoutContext context) {
        if (!searchable) return;
        Rect panel = panelRect(1.0f);
        if (expanded) {
            searchInput.visible(true);
            searchInput.layout(context, searchRect(panel));
        } else {
            searchInput.visible(false);
            searchInput.layout(context, Rect.ZERO);
        }
    }

    @Override
    public void render(RenderContext context) {
        if (!visible()) return;
        renderSelf(context);
        if (context.debugBounds()) {
            context.rect(bounds, SdfRectStyle.create()
                    .fill(ColorRGBA.TRANSPARENT)
                    .border(1, ColorRGBA.rgba(255, 0, 180, 120))
                    .radius(0), z + 5000);
        }
    }

    @Override
    protected void renderSelf(RenderContext context) {
        if (searchable) {
            searchInput.z(z + OVERLAY_Z_BASE + 60);
            searchInput.visible(expanded);
        }

        Rect base = headerRect();
        boolean disabled = !enabled();
        ColorRGBA fill = disabled
                ? context.theme().palette().surfaceAlt().withAlpha(120)
                : hovered || focused
                ? context.theme().palette().surfaceHover()
                : context.theme().palette().surfaceAlt();

        context.rect(base, SdfRectStyle.create()
                .fill(fill)
                .border(1, disabled ? context.theme().palette().border().withAlpha(95) : focused ? context.theme().palette().accent() : context.theme().palette().border())
                .radius(context.theme().radii().md()), z);

        float font = context.theme().fonts().normal();
        float textY = base.centerY() - context.lineHeight(font) * 0.5f;
        String shown = ellipsize(context, labeler.apply(value.get()), font, Math.max(8, base.w() - 36));

        Rect labelClip = new Rect(base.x() + 8, base.y(), Math.max(1, base.w() - 34), base.h());
        context.pushClip(labelClip);
        context.text(shown, base.x() + 10, textY, font,
                disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().text(), z + 1);
        context.popClip();

        context.text(expanded ? "▲" : "▼", base.right() - 18, textY, font,
                disabled ? context.theme().palette().mutedText().withAlpha(110) : context.theme().palette().mutedText(), z + 1);
    }

    @Override
    public void renderOverlay(RenderContext context) {
        super.renderOverlay(context);
        renderPanelOverlay(context);
    }

    private void renderPanelOverlay(RenderContext context) {
        float opened = expansion.value();
        if (opened <= 0.001f) return;
        refreshRows();
        clampScroll();

        float font = context.theme().fonts().normal();
        Rect panel = panelRect(opened);
        context.rect(panel, SdfRectStyle.create()
                .fill(context.theme().palette().surface())
                .border(1, context.theme().palette().border())
                .shadow(10, 0, 2, context.theme().palette().shadow().multiplyAlpha(opened * 0.6f))
                .radius(context.theme().radii().md())
                .opacity(opened), z + OVERLAY_Z_BASE + 50);

        Rect panelClip = panel.inset(1, 1, 1, 1);
        context.pushClip(panelClip);

        if (searchable && expanded) {
            searchInput.visible(true);
            searchInput.layout(null, searchRect(panel));
            searchInput.render(context);
            renderDivider(context, panel, opened);
        }

        renderRowsOrEmptyState(context, panel, font, opened);
        renderScrollbar(context, panel, opened);

        context.popClip();
    }

    private void renderDivider(RenderContext context, Rect panel, float opened) {
        Rect divider = dividerRect(panel);
        context.rect(divider, SdfRectStyle.create()
                .fill(context.theme().palette().border().withAlpha((int) (120 * opened)))
                .radius(0), z + OVERLAY_Z_BASE + 52);
    }

    private void renderRowsOrEmptyState(RenderContext context, Rect panel, float font, float opened) {
        Rect viewport = itemsViewport(panel);
        context.pushClip(viewport);

        if (rows.isEmpty()) {
            String query = searchable && search.get() != null ? search.get().trim() : "";
            String emptyText = query.isEmpty() ? "No items" : "No results found";
            float textWidth = context.measureText(emptyText, font).width();
            float textHeight = context.lineHeight(font);
            context.text(emptyText,
                    viewport.centerX() - textWidth * 0.5f,
                    viewport.centerY() - textHeight * 0.5f,
                    font,
                    context.theme().palette().mutedText().withAlpha((int) (135 * opened)),
                    z + OVERLAY_Z_BASE + 53);
            context.popClip();
            return;
        }

        int start = rowIndexAtOffset(scroll);
        float rowY = viewport.y() - (scroll - offsetForRow(start));

        for (int i = start; i < rows.size() && rowY <= viewport.bottom(); i++) {
            Row<G, T> row = rows.get(i);
            float rowHeight = row.height(groupHeight, itemHeight);
            Rect item = new Rect(viewport.x(), rowY, viewport.w(), rowHeight);
            rowY += rowHeight;

            if (item.bottom() < viewport.y() || item.y() > viewport.bottom()) continue;

            if (row.group) {
                renderGroupRow(context, item, row, font, opened);
            } else {
                renderValueRow(context, item, row, i, font, opened);
            }
        }

        context.popClip();
    }

    private void renderGroupRow(RenderContext context, Rect item, Row<G, T> row, float font, float opened) {
        Rect divider = new Rect(item.x() + 6, item.bottom() - 1, Math.max(1, item.w() - 12), 1);
        context.rect(divider, SdfRectStyle.create()
                .fill(context.theme().palette().border().withAlpha((int) (70 * opened)))
                .radius(0), z + OVERLAY_Z_BASE + 51);

        String groupLabel = ellipsize(context, groupLabeler.apply(row.groupKey), font, Math.max(8, item.w() - 16));
        context.text(groupLabel,
                item.x() + 8,
                item.centerY() - context.lineHeight(font) * 0.5f,
                font,
                context.theme().fonts().semibold(),
                context.theme().palette().mutedText().withAlpha((int) (190 * opened)),
                opened,
                false,
                z + OVERLAY_Z_BASE + 53);
    }

    private void renderValueRow(RenderContext context, Rect item, Row<G, T> row, int index, float font, float opened) {
        boolean selected = Objects.equals(row.value, value.get());
        boolean itemHovered = index == hoveredRowIndex && hoveringPanel;

        if (itemHovered) {
            context.rect(item, SdfRectStyle.create()
                    .fill(context.theme().palette().surfaceHover().withAlpha(170))
                    .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 51);
        }

        if (selected) {
            context.rect(item, SdfRectStyle.create()
                    .fill(context.theme().palette().accent().withAlpha(itemHovered ? 70 : 42))
                    .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 52);
        }

        String itemLabel = ellipsize(context, labeler.apply(row.value), font, Math.max(8, item.w() - 16));
        context.text(itemLabel,
                item.x() + 8,
                item.centerY() - context.lineHeight(font) * 0.5f,
                font,
                selected ? context.theme().palette().accent() : context.theme().palette().text(),
                opened,
                false,
                z + OVERLAY_Z_BASE + 53);
    }

    @Override
    public Component hitTest(float x, float y) {
        if (!visible() || !enabled()) return null;
        hoveringPanel = expanded && panelRect(1.0f).contains(x, y);
        if (!hoveringPanel) hoveredRowIndex = -1;

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
        if (!hoveringPanel) hoveredRowIndex = -1;
        if (hoveringPanel) return this;
        return super.hitTestOverlay(x, y);
    }

    @Override
    public boolean onMouseMove(MouseMoveEvent event) {
        if (!enabled()) return false;
        hoveringPanel = expanded && panelRect(1.0f).contains(event.x, event.y);
        if (!expanded && !headerRect().contains(event.x, event.y)) suppressTooltipUntilExit = false;
        hoveredRowIndex = hoveringPanel ? selectableRowIndexAt(event.x, event.y) : -1;
        return hoveringPanel;
    }

    @Override
    public boolean onMouseDragged(MouseDragEvent event) {
        if (!enabled()) return false;
        if (draggingScrollbar && needsScrollbar()) {
            Rect panel = panelRect(1.0f);
            Rect track = scrollbarTrack(panel);
            Rect thumb = scrollbarThumb(panel);
            float travel = Math.max(1, track.h() - thumb.h());
            float t = MathUtil.clamp((event.y - track.y() - scrollbarGrab) / travel, 0, 1);
            scroll = t * maxScroll();
            clampScroll();
            hoveredRowIndex = -1;
            hoveringPanel = true;
            return true;
        }

        if (searchable && expanded && searchInput.focused() && searchInput.bounds().contains(event.x, event.y)) {
            return searchInput.onMouseDragged(event);
        }

        return false;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        if (!enabled()) return false;
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScrolled(MouseScrollEvent event) {
        if (!enabled() || !expanded || !panelRect(1.0f).contains(event.x, event.y) || !needsScrollbar()) return false;
        scroll -= event.amountY * itemHeight;
        clampScroll();
        hoveredRowIndex = selectableRowIndexAt(event.x, event.y);
        hoveringPanel = true;
        return true;
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (!enabled() || event.button != MouseButton.LEFT) return false;

        if (headerRect().contains(event.x, event.y)) {
            expanded = !expanded;
            if (searchable) {
                if (expanded) {
                    search.set("");
                    lastSearch = null;
                    refreshRows();
                    searchInput.visible(true);
                    searchInput.setFocused(true);
                } else {
                    searchInput.setFocused(false);
                    searchInput.visible(false);
                }
            }
            hoveringPanel = false;
            hoveredRowIndex = -1;
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
                        hoveredRowIndex = -1;
                        hoveringPanel = true;
                        if (searchable) searchInput.setFocused(false);
                        return true;
                    }
                }

                if (searchable && searchInput.bounds().contains(event.x, event.y)) {
                    searchInput.setFocused(true);
                    return searchInput.onMousePressed(event);
                }

                int index = selectableRowIndexAt(event.x, event.y);
                if (index >= 0 && index < rows.size()) {
                    Row<G, T> row = rows.get(index);
                    if (!row.group) {
                        value.set(row.value);
                        closeDropdown();
                        suppressTooltipUntilExit = true;
                    }
                }
                return true;
            }
        }

        closeDropdown();
        return false;
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled()) return false;

        if (searchable && expanded && searchInput.focused()) {
            if (event.keyCode == KeyCodes.ESCAPE) {
                closeDropdown();
                return true;
            }
            if (event.keyCode == KeyCodes.DOWN || event.keyCode == KeyCodes.UP) {
                searchInput.setFocused(false);
            } else if (searchInput.onKeyPressed(event)) {
                refreshRows();
                return true;
            }
        }

        refreshRows();
        int idx = Math.max(0, selectableOptions.indexOf(value.get()));

        if (event.keyCode == KeyCodes.ENTER || event.keyCode == KeyCodes.SPACE) {
            expanded = !expanded;
            if (searchable) {
                if (expanded) {
                    search.set("");
                    lastSearch = null;
                    refreshRows();
                    searchInput.visible(true);
                    searchInput.setFocused(true);
                } else {
                    searchInput.setFocused(false);
                    searchInput.visible(false);
                }
            }
            return true;
        }

        if (event.keyCode == KeyCodes.DOWN && !selectableOptions.isEmpty()) {
            int next = Math.min(selectableOptions.size() - 1, idx + 1);
            T nextValue = selectableOptions.get(next);
            value.set(nextValue);
            ensureValueVisible(nextValue);
            return true;
        }

        if (event.keyCode == KeyCodes.UP && !selectableOptions.isEmpty()) {
            int next = Math.max(0, idx - 1);
            T nextValue = selectableOptions.get(next);
            value.set(nextValue);
            ensureValueVisible(nextValue);
            return true;
        }

        if (event.keyCode == KeyCodes.ESCAPE && expanded) {
            closeDropdown();
            return true;
        }

        return false;
    }

    @Override
    public boolean onCharTyped(CharTypedEvent event) {
        if (!enabled() || !searchable || !expanded || !searchInput.focused()) return false;
        boolean consumed = searchInput.onCharTyped(event);
        refreshRows();
        return consumed;
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        if (!focused) closeDropdown();
    }

    private void closeDropdown() {
        expanded = false;
        hoveringPanel = false;
        draggingScrollbar = false;
        hoveredRowIndex = -1;
        if (searchable) {
            searchInput.setFocused(false);
            searchInput.visible(false);
            searchInput.layout(null, Rect.ZERO);
        }
    }

    private void refreshRows() {
        String query = searchable && search.get() != null
                ? MinecraftTextFormatting.normalizeForSearch(search.get().trim())
                : "";
        if (Objects.equals(query, lastSearch)) return;

        lastSearch = query;
        rows.clear();
        selectableOptions.clear();

        for (Map.Entry<G, List<T>> entry : groupedOptions.entrySet()) {
            G group = entry.getKey();
            String groupLabel = groupLabeler.apply(group);
            boolean groupMatches = !query.isEmpty()
                    && groupLabel != null
                    && MinecraftTextFormatting.normalizeForSearch(groupLabel).contains(query);

            List<T> matching = new ArrayList<>();
            for (T option : entry.getValue()) {
                String optionLabel = labeler.apply(option);
                if (query.isEmpty()
                        || groupMatches
                        || (optionLabel != null && MinecraftTextFormatting.normalizeForSearch(optionLabel).contains(query))) {
                    matching.add(option);
                }
            }

            if (!matching.isEmpty()) {
                rows.add(Row.group(group));
                for (T option : matching) {
                    rows.add(Row.value(option));
                    selectableOptions.add(option);
                }
            }
        }

        scroll = 0;
        hoveredRowIndex = -1;
    }

    private Rect headerRect() {
        return new Rect(bounds.x(), bounds.y(), bounds.w(), HEADER_HEIGHT);
    }

    private int visibleRows() {
        refreshRows();
        return Math.min(rows.size(), maxVisibleRows);
    }

    private int panelRows() {
        refreshRows();
        return Math.max(1, Math.min(rows.size(), maxVisibleRows));
    }

    private boolean needsScrollbar() {
        return totalRowsHeight() > visibleRowsHeight() + 0.5f;
    }

    private float fullPanelHeight() {
        float listHeight = panelRows() <= 0 ? itemHeight : Math.min(totalRowsHeight(), visibleRowsHeight());
        if (!searchable) return 8 + listHeight;
        return SEARCH_TOP_PADDING
                + SEARCH_HEIGHT
                + DIVIDER_TOP_GAP
                + DIVIDER_HEIGHT
                + LIST_TOP_GAP
                + listHeight
                + LIST_BOTTOM_PADDING;
    }

    private Rect panelRect(float opened) {
        float fullHeight = fullPanelHeight();
        float height = Math.max(0, fullHeight * opened);
        Rect available = availableBounds();

        float belowY = bounds.y() + HEADER_HEIGHT + PANEL_GAP;
        float aboveY = bounds.y() - PANEL_GAP - height;
        float belowSpace = available.bottom() - belowY;
        float aboveSpace = bounds.y() - available.y();
        boolean openAbove = belowSpace < fullHeight && aboveSpace > belowSpace;

        float y = openAbove ? aboveY : belowY;
        y = Math.max(available.y(), Math.min(y, available.bottom() - height));
        return new Rect(bounds.x(), y, bounds.w(), height);
    }

    private Rect searchRect(Rect panel) {
        return new Rect(
                panel.x() + SEARCH_SIDE_PADDING,
                panel.y() + SEARCH_TOP_PADDING,
                Math.max(1, panel.w() - SEARCH_SIDE_PADDING * 2),
                SEARCH_HEIGHT
        );
    }

    private Rect dividerRect(Rect panel) {
        float y = panel.y() + SEARCH_TOP_PADDING + SEARCH_HEIGHT + DIVIDER_TOP_GAP;
        return new Rect(panel.x() + 6, y, Math.max(1, panel.w() - 12), DIVIDER_HEIGHT);
    }

    private Rect itemsViewport(Rect panel) {
        float rightPad = needsScrollbar() ? 12 : 4;
        if (!searchable) return panel.inset(4, 4, rightPad, 4);
        float y = panel.y()
                + SEARCH_TOP_PADDING
                + SEARCH_HEIGHT
                + DIVIDER_TOP_GAP
                + DIVIDER_HEIGHT
                + LIST_TOP_GAP;
        return new Rect(
                panel.x() + 4,
                y,
                Math.max(1, panel.w() - 4 - rightPad),
                Math.max(0, panel.h() - (y - panel.y()) - LIST_BOTTOM_PADDING)
        );
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
        Rect viewport = itemsViewport(panel);
        return new Rect(panel.right() - 9, viewport.y(), 5, Math.max(0, viewport.h()));
    }

    private Rect scrollbarThumb(Rect panel) {
        Rect track = scrollbarTrack(panel);
        float max = maxScroll();
        if (max <= 0) return Rect.ZERO;

        float total = totalRowsHeight();
        float visible = visibleRowsHeight();
        float ratio = MathUtil.clamp(visible / Math.max(1, total), 0.12f, 1.0f);
        float h = Math.max(12, track.h() * ratio);
        float y = track.y() + (scroll / max) * Math.max(0, track.h() - h);
        return new Rect(track.x(), y, track.w(), h);
    }

    private int selectableRowIndexAt(float x, float y) {
        int index = rowIndexAt(x, y);
        if (index < 0 || index >= rows.size() || rows.get(index).group) return -1;
        return index;
    }

    private int rowIndexAt(float x, float y) {
        if (rows.isEmpty()) return -1;
        Rect panel = panelRect(1.0f);
        Rect viewport = itemsViewport(panel);
        if (!viewport.contains(x, y)) return -1;
        return rowIndexAtOffset(y - viewport.y() + scroll);
    }

    private int rowIndexAtOffset(float offset) {
        float y = 0;
        for (int i = 0; i < rows.size(); i++) {
            float h = rows.get(i).height(groupHeight, itemHeight);
            if (offset < y + h) return i;
            y += h;
        }
        return Math.max(0, rows.size() - 1);
    }

    private float offsetForRow(int rowIndex) {
        float y = 0;
        int end = Math.max(0, Math.min(rowIndex, rows.size()));
        for (int i = 0; i < end; i++) y += rows.get(i).height(groupHeight, itemHeight);
        return y;
    }

    private float totalRowsHeight() {
        refreshRows();
        float total = 0;
        for (Row<G, T> row : rows) total += row.height(groupHeight, itemHeight);
        return total;
    }

    private float visibleRowsHeight() {
        refreshRows();
        float visible = 0;
        int count = Math.min(rows.size(), maxVisibleRows);
        for (int i = 0; i < count; i++) visible += rows.get(i).height(groupHeight, itemHeight);
        if (visible <= 0) visible = itemHeight;
        return visible;
    }

    private float maxScroll() {
        return Math.max(0, totalRowsHeight() - visibleRowsHeight());
    }

    private void clampScroll() {
        scroll = MathUtil.clamp(scroll, 0, maxScroll());
    }

    private void ensureValueVisible(T option) {
        for (int i = 0; i < rows.size(); i++) {
            Row<G, T> row = rows.get(i);
            if (!row.group && Objects.equals(row.value, option)) {
                ensureRowVisible(i);
                return;
            }
        }
    }

    private void ensureRowVisible(int index) {
        float y = offsetForRow(index);
        float bottom = y + rows.get(index).height(groupHeight, itemHeight);
        float visible = visibleRowsHeight();
        if (y < scroll) scroll = y;
        else if (bottom > scroll + visible) scroll = bottom - visible;
        clampScroll();
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

    private Rect availableBounds() {
        for (Component c = parent(); c != null; c = c.parent()) {
            if ("ScrollContainer".equals(c.getClass().getSimpleName())) return c.bounds();
        }
        Component root = this;
        while (root.parent() != null) root = root.parent();
        return root.bounds();
    }

    private static final class Row<G extends Enum<G>, T> {
        private final boolean group;
        private final G groupKey;
        private final T value;

        private Row(boolean group, G groupKey, T value) {
            this.group = group;
            this.groupKey = groupKey;
            this.value = value;
        }

        private static <G extends Enum<G>, T> Row<G, T> group(G groupKey) {
            return new Row<>(true, groupKey, null);
        }

        private static <G extends Enum<G>, T> Row<G, T> value(T value) {
            return new Row<>(false, null, value);
        }

        private float height(float groupHeight, float itemHeight) {
            return group ? groupHeight : itemHeight;
        }
    }
}
