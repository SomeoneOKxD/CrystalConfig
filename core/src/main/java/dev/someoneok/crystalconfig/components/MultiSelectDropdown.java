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

public class MultiSelectDropdown<T> extends Component {
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
    private static final String CHECKMARK = "✔";

    private final State<List<T>> value;
    private final List<T> options;
    private final boolean searchable;
    private final MutableState<String> search = new MutableState<>("");
    private final TextInput searchInput = new TextInput(search).placeholder("Search...");
    private final List<T> filteredOptions = new ArrayList<>();

    private Function<T, String> labeler = String::valueOf;
    private String lastSearch = null;
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

    public MultiSelectDropdown(State<List<T>> value, List<T> options) {
        this(value, options, false);
    }

    public MultiSelectDropdown(State<List<T>> value, List<T> options, boolean searchable) {
        this.value = Objects.requireNonNull(value, "value");
        this.options = new ArrayList<>(Objects.requireNonNull(options, "options"));
        this.searchable = searchable;
        this.focusable = true;
        this.searchInput.height(SEARCH_HEIGHT);
        this.searchInput.visible(false);
        if (searchable) this.add(searchInput);
        size(180, HEADER_HEIGHT);
    }

    public MultiSelectDropdown<T> labeler(Function<T, String> labeler) {
        this.labeler = labeler == null ? String::valueOf : labeler;
        this.lastSearch = null;
        return this;
    }

    public MultiSelectDropdown<T> options(List<T> options) {
        this.options.clear();
        if (options != null) this.options.addAll(options);
        this.lastSearch = null;
        refreshFilter();
        syncSelectionToOptions();
        clampScroll();
        markLayoutDirty();
        return this;
    }

    public MultiSelectDropdown<T> itemHeight(float itemHeight) {
        this.itemHeight = Math.max(20, itemHeight);
        return this;
    }

    public MultiSelectDropdown<T> maxVisibleRows(int rows) {
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
        refreshFilter();
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
        ColorRGBA fill = disabled ? context.theme().palette().surfaceAlt().withAlpha(120) : hovered || focused ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt();
        context.rect(base, SdfRectStyle.create()
                .fill(fill)
                .border(1, disabled ? context.theme().palette().border().withAlpha(95) : focused ? context.theme().palette().accent() : context.theme().palette().border())
                .radius(context.theme().radii().md()), z);

        float font = context.theme().fonts().normal();
        float textY = base.centerY() - context.lineHeight(font) * 0.5f;
        String shown = ellipsize(context, headerLabel(), font, Math.max(8, base.w() - 36));
        Rect labelClip = new Rect(base.x() + 8, base.y(), Math.max(1, base.w() - 34), base.h());
        context.pushClip(labelClip);
        context.text(shown, base.x() + 10, textY, font, disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().text(), z + 1);
        context.popClip();
        context.text(expanded ? "▲" : "▼", base.right() - 18, textY, font, disabled ? context.theme().palette().mutedText().withAlpha(110) : context.theme().palette().mutedText(), z + 1);

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
        renderItemsOrEmptyState(context, panel, font, opened);
        renderScrollbar(context, panel, opened);
        context.popClip();
    }

    private void renderDivider(RenderContext context, Rect panel, float opened) {
        Rect divider = dividerRect(panel);
        context.rect(divider, SdfRectStyle.create()
                .fill(context.theme().palette().border().withAlpha((int) (120 * opened)))
                .radius(0), z + OVERLAY_Z_BASE + 52);
    }

    private void renderItemsOrEmptyState(RenderContext context, Rect panel, float font, float opened) {
        Rect viewport = itemsViewport(panel);
        context.pushClip(viewport);

        if (filteredOptions.isEmpty()) {
            String query = search.get() == null ? "" : search.get().trim();
            String emptyText = searchable && !query.isEmpty() ? "No results found" : "No items";
            float textWidth = context.measureText(emptyText, font).width();
            float textHeight = context.lineHeight(font);
            context.text(emptyText, viewport.centerX() - textWidth * 0.5f, viewport.centerY() - textHeight * 0.5f,
                    font, context.theme().palette().mutedText().withAlpha((int) (135 * opened)), z + OVERLAY_Z_BASE + 53);
            context.popClip();
            return;
        }

        Set<T> selected = selectedSet();
        int start = Math.max(0, (int) (scroll / itemHeight));
        int end = Math.min(filteredOptions.size(), start + visibleRows() + 2);
        float scrollOffset = scroll - start * itemHeight;
        for (int i = start; i < end; i++) {
            T option = filteredOptions.get(i);
            Rect item = new Rect(viewport.x(), viewport.y() + (i - start) * itemHeight - scrollOffset, viewport.w(), itemHeight);
            if (item.bottom() < viewport.y() || item.y() > viewport.bottom()) continue;

            boolean selectedItem = selected.contains(option);
            boolean itemHovered = i == hoveredIndex && hoveringPanel;
            if (itemHovered) {
                context.rect(item, SdfRectStyle.create()
                        .fill(context.theme().palette().surfaceHover().withAlpha(170))
                        .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 51);
            }
            if (selectedItem) {
                context.rect(item, SdfRectStyle.create()
                        .fill(context.theme().palette().accent().withAlpha(itemHovered ? 70 : 42))
                        .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 52);
            }

            float textY = item.centerY() - context.lineHeight(font) * 0.5f;
            if (selectedItem) {
                float markFont = context.theme().fonts().normal() + 1.0f;
                String markFace = context.theme().fonts().semibold();
                float markY = item.centerY() - context.lineHeight(markFont, markFace) * 0.5f - 0.5f;
                context.text(CHECKMARK, item.x() + 8, markY, markFont, markFace, context.theme().palette().accent(), opened, false, z + OVERLAY_Z_BASE + 53);
            }
            String itemLabel = ellipsize(context, labeler.apply(option), font, Math.max(8, item.w() - 34));
            context.text(itemLabel, item.x() + 28, textY, font,
                    selectedItem ? context.theme().palette().accent() : context.theme().palette().text(),
                    opened, false, z + OVERLAY_Z_BASE + 53);
        }
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
            hoveredIndex = -1;
            hoveringPanel = true;
            return true;
        }
        return searchable && expanded && searchInput.focused() && searchInput.bounds().contains(event.x, event.y) && searchInput.onMouseDragged(event);
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
        hoveredIndex = itemIndexAt(event.x, event.y);
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
                    refreshFilter();
                    searchInput.visible(true);
                    searchInput.setFocused(true);
                } else {
                    searchInput.setFocused(false);
                    searchInput.visible(false);
                }
            }
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
                        if (searchable) searchInput.setFocused(false);
                        return true;
                    }
                }
                if (searchable && searchInput.bounds().contains(event.x, event.y)) {
                    searchInput.setFocused(true);
                    return searchInput.onMousePressed(event);
                }
                int index = itemIndexAt(event.x, event.y);
                if (index >= 0 && index < filteredOptions.size()) {
                    toggle(filteredOptions.get(index));
                    suppressTooltipUntilExit = true;
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
                refreshFilter();
                return true;
            }
        }
        refreshFilter();
        if (event.keyCode == KeyCodes.ENTER || event.keyCode == KeyCodes.SPACE) {
            expanded = !expanded;
            if (searchable) {
                if (expanded) {
                    search.set("");
                    lastSearch = null;
                    refreshFilter();
                    searchInput.visible(true);
                    searchInput.setFocused(true);
                } else {
                    searchInput.setFocused(false);
                    searchInput.visible(false);
                }
            }
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
        refreshFilter();
        return consumed;
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        if (!focused) closeDropdown();
    }

    private void toggle(T option) {
        List<T> current = value.get() == null ? List.of() : value.get();
        Set<T> selected = new LinkedHashSet<>(current);
        if (selected.contains(option)) selected.remove(option);
        else selected.add(option);

        List<T> next = new ArrayList<>();
        for (T candidate : options) {
            if (selected.contains(candidate)) next.add(candidate);
        }
        value.set(next);
    }

    private void syncSelectionToOptions() {
        List<T> current = value.get();
        if (current == null || current.isEmpty()) return;
        Set<T> selected = new LinkedHashSet<>(current);
        List<T> next = new ArrayList<>();
        for (T candidate : options) if (selected.contains(candidate)) next.add(candidate);
        if (!next.equals(current)) value.set(next);
    }

    private Set<T> selectedSet() {
        List<T> current = value.get();
        return current == null ? Set.of() : new LinkedHashSet<>(current);
    }

    private String headerLabel() {
        List<T> current = value.get();
        if (current == null || current.isEmpty()) return "None";
        if (current.size() == 1) return labeler.apply(current.get(0));
        return current.size() + " selected";
    }

    private void closeDropdown() {
        expanded = false;
        hoveringPanel = false;
        draggingScrollbar = false;
        hoveredIndex = -1;
        if (searchable) {
            searchInput.setFocused(false);
            searchInput.visible(false);
            searchInput.layout(null, Rect.ZERO);
        }
    }

    private static String ellipsize(RenderContext context, String value, float font, float maxWidth) {
        String s = value == null ? "" : value;
        if (context.measureText(s, font).width() <= maxWidth) return s;
        String ellipsis = "...";
        if (context.measureText(ellipsis, font).width() > maxWidth) return "";
        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (context.measureText(s.substring(0, mid) + ellipsis, font).width() <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }

    private Rect headerRect() {
        return new Rect(bounds.x(), bounds.y(), bounds.w(), HEADER_HEIGHT);
    }

    private int visibleRows() {
        refreshFilter();
        return Math.min(filteredOptions.size(), maxVisibleRows);
    }

    private int panelRows() {
        refreshFilter();
        return Math.max(1, Math.min(filteredOptions.size(), maxVisibleRows));
    }

    private boolean needsScrollbar() {
        return filteredOptions.size() > visibleRows();
    }

    private float fullPanelHeight() {
        if (!searchable) return panelRows() * itemHeight + LIST_BOTTOM_PADDING;
        return SEARCH_TOP_PADDING + SEARCH_HEIGHT + DIVIDER_TOP_GAP + DIVIDER_HEIGHT + LIST_TOP_GAP + panelRows() * itemHeight + LIST_BOTTOM_PADDING;
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
        return new Rect(panel.x() + SEARCH_SIDE_PADDING, panel.y() + SEARCH_TOP_PADDING,
                Math.max(1, panel.w() - SEARCH_SIDE_PADDING * 2), SEARCH_HEIGHT);
    }

    private Rect dividerRect(Rect panel) {
        float y = panel.y() + SEARCH_TOP_PADDING + SEARCH_HEIGHT + DIVIDER_TOP_GAP;
        return new Rect(panel.x() + 6, y, Math.max(1, panel.w() - 12), DIVIDER_HEIGHT);
    }

    private Rect itemsViewport(Rect panel) {
        float rightPad = needsScrollbar() ? 12 : 4;
        float y = panel.y() + (searchable ? SEARCH_TOP_PADDING + SEARCH_HEIGHT + DIVIDER_TOP_GAP + DIVIDER_HEIGHT + LIST_TOP_GAP : 0);
        return new Rect(panel.x() + 4, y, Math.max(1, panel.w() - 4 - rightPad), Math.max(0, panel.h() - (y - panel.y()) - LIST_BOTTOM_PADDING));
    }

    private void refreshFilter() {
        String query = searchable && search.get() != null ? MinecraftTextFormatting.normalizeForSearch(search.get().trim()) : "";
        if (Objects.equals(query, lastSearch)) return;
        lastSearch = query;
        filteredOptions.clear();
        if (query.isEmpty()) {
            filteredOptions.addAll(options);
        } else {
            for (T option : options) {
                String label = labeler.apply(option);
                if (label != null && MinecraftTextFormatting.normalizeForSearch(label).contains(query)) filteredOptions.add(option);
            }
        }
        scroll = 0;
        hoveredIndex = -1;
    }

    private void renderScrollbar(RenderContext context, Rect panel, float opened) {
        if (!needsScrollbar()) return;
        Rect track = scrollbarTrack(panel);
        Rect thumb = scrollbarThumb(panel);
        context.rect(track, SdfRectStyle.create()
                .fill(context.theme().palette().surfaceAlt().withAlpha((int) (60 * opened)))
                .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 54);
        context.rect(thumb, SdfRectStyle.create()
                .fill(context.theme().palette().surfaceActive().withAlpha(draggingScrollbar ? Math.max((int) (185 * opened), 220) : (int) (185 * opened)))
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
        float total = filteredOptions.size() * itemHeight;
        float visible = visibleRows() * itemHeight;
        float ratio = MathUtil.clamp(visible / Math.max(1, total), 0.12f, 1.0f);
        float h = Math.max(12, track.h() * ratio);
        float y = track.y() + (scroll / max) * Math.max(0, track.h() - h);
        return new Rect(track.x(), y, track.w(), h);
    }

    private int itemIndexAt(float x, float y) {
        if (filteredOptions.isEmpty()) return -1;
        Rect panel = panelRect(1.0f);
        Rect viewport = itemsViewport(panel);
        if (!viewport.contains(x, y)) return -1;
        int index = (int) ((y - viewport.y() + scroll) / itemHeight);
        return index >= 0 && index < filteredOptions.size() ? index : -1;
    }

    private float maxScroll() {
        return Math.max(0, filteredOptions.size() * itemHeight - visibleRows() * itemHeight);
    }

    private void clampScroll() {
        scroll = MathUtil.clamp(scroll, 0, maxScroll());
    }

    private Rect availableBounds() {
        for (Component c = parent(); c != null; c = c.parent()) {
            if ("ScrollContainer".equals(c.getClass().getSimpleName())) return c.bounds();
        }
        Component root = this;
        while (root.parent() != null) root = root.parent();
        return root.bounds();
    }
}
