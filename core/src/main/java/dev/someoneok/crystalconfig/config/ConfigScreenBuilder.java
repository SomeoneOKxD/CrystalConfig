package dev.someoneok.crystalconfig.config;

import dev.someoneok.crystalconfig.animation.AnimatedFloat;
import dev.someoneok.crystalconfig.components.*;
import dev.someoneok.crystalconfig.containers.Column;
import dev.someoneok.crystalconfig.containers.Row;
import dev.someoneok.crystalconfig.containers.ScrollContainer;
import dev.someoneok.crystalconfig.containers.WrappingIconRow;
import dev.someoneok.crystalconfig.input.*;
import dev.someoneok.crystalconfig.layout.Alignment;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.*;
import dev.someoneok.crystalconfig.state.MutableState;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.theme.Theme;
import dev.someoneok.crystalconfig.ui.Component;
import dev.someoneok.crystalconfig.ui.UiRoot;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ConfigScreenBuilder {
    private final String title;
    private final ConfigUiSettings settings;
    private final List<ConfigSection> sections = new ArrayList<>();
    private Component footerButton;
    private final List<Component> footerIconButtons = new ArrayList<>();
    private final List<Runnable> closeCallbacks = new ArrayList<>();
    private String initialSearch = "";
    private final MutableState<Integer> selectedMainCategory = new MutableState<>(0);
    private final MutableState<Integer> selectedSection = new MutableState<>(0);
    private ConfigSection lastSection;

    private ConfigScreenBuilder(String title, ConfigUiSettings settings) {
        this.title = title == null ? "Config" : title;
        this.settings = settings == null ? ConfigUiSettings.create() : settings;
    }

    public static ConfigScreenBuilder create(String title) {
        return new ConfigScreenBuilder(title, ConfigUiSettings.create());
    }

    public static ConfigScreenBuilder create(String title, ConfigUiSettings settings) {
        return new ConfigScreenBuilder(title, settings);
    }

    public ConfigUiSettings settings() {
        return settings;
    }

    public ConfigScreenBuilder section(String title, Consumer<SectionBuilder> body) {
        return section("General", title, body);
    }

    public ConfigScreenBuilder section(String mainCategory, String title, Consumer<SectionBuilder> body) {
        ConfigSection section = new ConfigSection(mainCategory, title == null ? "General" : title);
        SectionBuilder builder = new SectionBuilder(section.content);
        body.accept(builder);
        sections.add(section);
        lastSection = section;
        return this;
    }

    /** Hide the previously added sub-category while the predicate returns true. */
    public ConfigScreenBuilder hidden(BooleanSupplier predicate) {
        if (lastSection != null) lastSection.hiddenWhen(predicate);
        return this;
    }

    /** Disable the previously added sub-category while the predicate returns true. */
    public ConfigScreenBuilder disabled(BooleanSupplier predicate) {
        if (lastSection != null) lastSection.disabledWhen(predicate);
        return this;
    }

    public ConfigScreenBuilder footerButton(String text, Runnable action) {
        this.footerButton = new Button(text).onClick(action).fillX().height(34);
        return this;
    }

    public ConfigScreenBuilder footerIconButton(String icon, Runnable action) {
        return footerIconButton(icon, action, null);
    }

    public ConfigScreenBuilder footerIconButton(String icon, Runnable action, String tooltip) {
        IconButton button = new IconButton(icon).onClick(action);
        if (tooltip != null && !tooltip.isBlank()) button.tooltip(tooltip);
        this.footerIconButtons.add(button);
        return this;
    }

    public ConfigScreenBuilder footer(Component component) {
        this.footerButton = component;
        return this;
    }

    /** Register a callback that runs once when the config screen closes. */
    public ConfigScreenBuilder onClose(Runnable callback) {
        if (callback != null) closeCallbacks.add(callback);
        return this;
    }

    /** Register a callback that runs after the reset-defaults button resets the config. */
    public ConfigScreenBuilder onDefaultsReset(Runnable callback) {
        settings.onDefaultsReset(callback);
        return this;
    }

    /** Alias for {@link #onDefaultsReset(Runnable)}. */
    public ConfigScreenBuilder afterDefaultsReset(Runnable callback) {
        return onDefaultsReset(callback);
    }

    /**
     * Pre-populate and open the config search UI when the screen is first shown.
     * Useful for hotkeys or command integrations that jump directly to a setting.
     */
    public ConfigScreenBuilder initialSearch(String searchValue) {
        this.initialSearch = searchValue == null ? "" : searchValue;
        return this;
    }

    public Component build() {
        return build(initialSearch);
    }

    /** Build the screen with the search box already visible and filled. */
    public Component build(String searchValue) {
        if (sections.isEmpty()) {
            sections.add(new ConfigSection("General", "General"));
        }
        return new AnimatedScreenRoot(new ConfigShell(title, sections, selectedMainCategory, selectedSection, footerButton, footerIconButtons, searchValue, settings).fill()).fill();
    }

    /** Build a complete UI root and attach any configured close callbacks. */
    public UiRoot buildRoot() {
        return buildRoot(initialSearch);
    }

    /** Build a complete UI root with the search box already visible and filled. */
    public UiRoot buildRoot(String searchValue) {
        UiRoot root = new UiRoot(build(searchValue), settings);
        for (Runnable callback : closeCallbacks) root.onClose(callback);
        return root;
    }

    private static final class AnimatedScreenRoot extends Component {
        private final Component child;
        private final AnimatedFloat visibility = new AnimatedFloat(0).speed(16);
        private AnimatedScreenRoot(Component child) {
            this.child = child;
            visibility.target(1.0f);
            add(child);
        }

        @Override
        public void tick(float deltaSeconds) {
            float before = visibility.value();
            visibility.target(1.0f);
            visibility.update(deltaSeconds);
            if (Math.abs(before - visibility.value()) > 0.0005f) {
                markLayoutDirty();
            }
            super.tick(deltaSeconds);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return constraints.clamp(new Size(constraints.maxWidth(), constraints.maxHeight()));
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            float t = visibility.value();
            float eased = 1.0f - (1.0f - t) * (1.0f - t);
            float scale = 0.965f + 0.035f * eased;
            float w = bounds.w() * scale;
            float h = bounds.h() * scale;
            float x = bounds.x() + (bounds.w() - w) * 0.5f;
            float y = bounds.y() + (bounds.h() - h) * 0.5f + (1.0f - eased) * 10.0f;
            child.layout(context, new Rect(x, y, w, h));
        }

    }

    private static final class ConfigSection {
        private final String mainCategory;
        private final String title;
        private final Column content = new Column().gap(16).padding(20, 18).fillX();
        private BooleanSupplier hiddenWhen = () -> false;
        private BooleanSupplier disabledWhen = () -> false;

        private ConfigSection(String mainCategory, String title) {
            this.mainCategory = mainCategory == null || mainCategory.isBlank() ? "General" : mainCategory;
            this.title = title == null || title.isBlank() ? "General" : title;
        }

        private void hiddenWhen(BooleanSupplier predicate) { this.hiddenWhen = predicate == null ? () -> false : predicate; }
        private void disabledWhen(BooleanSupplier predicate) { this.disabledWhen = predicate == null ? () -> false : predicate; }
        private boolean hidden() { try { return hiddenWhen.getAsBoolean(); } catch (RuntimeException ignored) { return false; } }
        private boolean disabled() { try { return disabledWhen.getAsBoolean(); } catch (RuntimeException ignored) { return false; } }
    }

    private static List<String> wrapLines(LayoutContext context, String text, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        if (maxWidth <= 8) {
            lines.add(text);
            return lines;
        }
        String[] paragraphs = text.split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (measure(context, candidate, fontSize) <= maxWidth || line.isEmpty()) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return lines;
    }

    private static List<String> wrapLines(RenderContext context, String text, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        if (maxWidth <= 8) {
            lines.add(text);
            return lines;
        }
        String[] paragraphs = text.split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                lines.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.trim().split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (context.measureText(candidate, fontSize, regular(context)).width() <= maxWidth || line.isEmpty()) {
                    line.setLength(0);
                    line.append(candidate);
                } else {
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return lines;
    }

    private static float measure(LayoutContext context, String text, float fontSize) {
        TextMetrics metrics = MinecraftTextFormatting.measureText(
                context.backend(),
                context.theme(),
                text == null ? "" : text,
                fontSize,
                regular(context)
        );
        return metrics.width();
    }

    private static String ellipsize(RenderContext context, String value, float fontSize, String fontFace, float maxWidth) {
        String text = value == null ? "" : value;
        if (context.measureText(text, fontSize, fontFace).width() <= maxWidth) return text;

        String ellipsis = "...";
        if (context.measureText(ellipsis, fontSize, fontFace).width() > maxWidth) return "";

        int lo = 0;
        int hi = text.length();

        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (context.measureText(text.substring(0, mid) + ellipsis, fontSize, fontFace).width() <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }

        return text.substring(0, lo) + ellipsis;
    }

    private static String regular(RenderContext context) {
        return context.theme().fonts().regular();
    }

    private static String medium(RenderContext context) {
        return context.theme().fonts().medium();
    }

    private static String semibold(RenderContext context) {
        return context.theme().fonts().semibold();
    }

    private static String regular(LayoutContext context) {
        return context.theme().fonts().regular();
    }

    private static String medium(LayoutContext context) {
        return context.theme().fonts().medium();
    }

    private static String semibold(LayoutContext context) {
        return context.theme().fonts().semibold();
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && needle != null && !needle.isEmpty()
                && MinecraftTextFormatting.normalizeForSearch(haystack).contains(needle);
    }

    private static boolean childrenMatch(Component component, String query, String sectionTitle) {
        if (component instanceof SearchAware aware && aware.searchMatches(query, sectionTitle)) return true;
        for (Component child : component.children()) {
            if (childrenMatch(child, query, sectionTitle)) return true;
        }
        return false;
    }

    private static final class ConfigShell extends Component {
        private static final float TOP_BAR_HEIGHT = 58;
        private static final float COMPACT_TOP_BAR_HEIGHT = TOP_BAR_HEIGHT;
        private static final float SIDEBAR_WIDTH = 250;
        private static final float FOOTER_AREA_HEIGHT = 58;

        private final String title;
        private final List<ConfigSection> sections;
        private final List<MainCategoryGroup> mainGroups;
        private final State<Integer> selectedMain;
        private final State<Integer> selected;
        private final Column sidebarList = new Column().gap(8).padding(14, 14).fillX();
        private final ScrollContainer sidebarScroll = new ScrollContainer(sidebarList).fill();
        private final ScrollContainer contentScroll;
        private final Component footerButton;
        private final Component footerArea;
        private final MutableState<String> searchQuery = new MutableState<>("");
        private final SearchControl searchControl;
        private final ConfigUiSettings settings;
        private final SettingsButton settingsButton = new SettingsButton();
        private final SettingsPopup settingsPopup;
        private String lastAppliedSearch = null;
        private String lastSidebarSignature = null;
        private boolean suppressNextExpandedGroupAnimation = false;
        private int expandedMainCategory = 0;
        private int lastObservedMainIndex = -1;

        private ConfigShell(String title, List<ConfigSection> sections, State<Integer> selectedMain, State<Integer> selected, Component footerButton, List<Component> footerIconButtons, String initialSearch, ConfigUiSettings settings) {
            this.title = title;
            this.sections = sections;
            this.mainGroups = groupSections(sections);
            this.selectedMain = selectedMain;
            this.selected = selected;
            this.footerButton = footerButton;
            this.footerArea = createFooterArea(footerButton, footerIconButtons);
            this.settings = settings == null ? ConfigUiSettings.create() : settings;
            this.settingsPopup = new SettingsPopup(settingsButton, this.settings);
            this.searchQuery.set(initialSearch == null ? "" : initialSearch.trim());
            this.searchControl = new SearchControl(searchQuery).width(260).height(34);
            this.expandedMainCategory = Math.max(0, Math.min(Math.max(0, this.mainGroups.size() - 1), selectedMain.get()));
            if (!this.searchQuery.get().isEmpty()) this.searchControl.openImmediately();
            this.contentScroll = new ScrollContainer(sections.get(0).content).fill();
            add(sidebarScroll);
            add(contentScroll);
            add(searchControl);
            add(settingsButton);
            add(settingsPopup);
            if (footerArea != null) add(footerArea);
            rebuildSidebar();
            applySearch(true);
        }

        private static Component createFooterArea(Component footerButton, List<Component> footerIconButtons) {
            boolean hasIcons = footerIconButtons != null && !footerIconButtons.isEmpty();
            if (!hasIcons) return footerButton;

            WrappingIconRow iconRow = new WrappingIconRow().gap(8).rowGap(8).fillX();
            for (Component iconButton : footerIconButtons) iconRow.add(iconButton);
            if (footerButton == null) return iconRow;

            Column footer = new Column().gap(8).fillX();
            footer.add(iconRow);
            footer.add(footerButton);
            return footer;
        }

        private void rebuildSidebar() {
            sidebarList.clearChildren();
            String q = normalizedSearch();
            clampMainIndex();
            lastSidebarSignature = sidebarSignature(q);
            for (int groupIndex : visibleMainIndexes()) {
                MainCategoryGroup group = mainGroups.get(groupIndex);
                boolean hasSearchHit = q.isEmpty() || mainMatches(group, q);
                if (!hasSearchHit) continue;
                boolean mainDisabled = isMainDisabled(groupIndex);
                boolean expanded = !mainDisabled && (isMainExpanded(groupIndex) || !q.isEmpty());
                CategoryHeader header = new CategoryHeader(
                        group.title,
                        groupIndex,
                        () -> selectedMain.get() == groupIndex,
                        () -> expanded,
                        () -> mainDisabled,
                        () -> openMainCategory(groupIndex),
                        () -> { }
                ).fillX();
                if (expanded) {
                    boolean snapExpanded = suppressNextExpandedGroupAnimation && selectedMain.get() == groupIndex;
                    CategoryGroupPanel groupPanel = new CategoryGroupPanel(() -> selectedMain.get() == groupIndex, snapExpanded).fillX();
                    groupPanel.add(header);
                    groupPanel.add(new Spacer().height(6).fillX());
                    for (int sectionIndex : group.sectionIndexes) {
                        ConfigSection section = sections.get(sectionIndex);
                        if (!section.hidden() && (q.isEmpty() || sectionMatches(section, q))) {
                            groupPanel.add(new CategoryButton(section.title, selected, sectionIndex, section::disabled).fillX());
                        }
                    }
                    groupPanel.add(new Spacer().height(6).fillX());
                    sidebarList.add(groupPanel);
                } else {
                    sidebarList.add(header);
                }
            }
            suppressNextExpandedGroupAnimation = false;
        }

        private boolean isMainExpanded(int groupIndex) {
            return groupIndex >= 0 && groupIndex < mainGroups.size() && groupIndex == expandedMainCategory;
        }

        private void openMainCategory(int groupIndex) {
            if (groupIndex < 0 || groupIndex >= mainGroups.size() || isMainDisabled(groupIndex)) return;
            boolean wasSelected = selectedMain.get() == groupIndex;
            boolean wasExpanded = expandedMainCategory == groupIndex;
            boolean selectedSectionInGroup = mainGroups.get(groupIndex).sectionIndexes.contains(selected.get());

            if (wasSelected && wasExpanded && selectedSectionInGroup) return;

            expandedMainCategory = groupIndex;
            selectedMain.set(groupIndex);
            lastObservedMainIndex = groupIndex;
            if (!wasSelected || !selectedSectionInGroup) {
                selected.set(firstVisibleSectionForMain(groupIndex, normalizedSearch()));
            }
            suppressNextExpandedGroupAnimation = false;
            rebuildSidebar();
            markLayoutDirty();
        }

        private void focusMainCategory(int groupIndex) {
        }

        private int clampMainIndex() {
            List<Integer> visible = visibleMainIndexes();
            if (visible.isEmpty()) {
                if (selectedMain.get() != 0) selectedMain.set(0);
                return 0;
            }
            int requested = Math.max(0, Math.min(mainGroups.size() - 1, selectedMain.get()));
            if (!visible.contains(requested)) requested = visible.get(0);
            if (requested != selectedMain.get()) selectedMain.set(requested);
            return requested;
        }

        private List<Integer> visibleMainIndexes() {
            List<Integer> visible = new ArrayList<>();
            for (int i = 0; i < mainGroups.size(); i++) {
                if (hasVisibleSection(mainGroups.get(i))) visible.add(i);
            }
            return visible;
        }

        private boolean hasVisibleSection(MainCategoryGroup group) {
            for (int sectionIndex : group.sectionIndexes) {
                if (!sections.get(sectionIndex).hidden()) return true;
            }
            return false;
        }

        private boolean hasEnabledSection(MainCategoryGroup group) {
            for (int sectionIndex : group.sectionIndexes) {
                ConfigSection section = sections.get(sectionIndex);
                if (!section.hidden() && !section.disabled()) return true;
            }
            return false;
        }

        private boolean isMainDisabled(int groupIndex) {
            return groupIndex < 0 || groupIndex >= mainGroups.size() || !hasEnabledSection(mainGroups.get(groupIndex));
        }

        private int firstVisibleSectionForMain(int mainIdx, String q) {
            MainCategoryGroup group = mainGroups.get(Math.max(0, Math.min(mainGroups.size() - 1, mainIdx)));
            for (int sectionIndex : group.sectionIndexes) {
                ConfigSection section = sections.get(sectionIndex);
                if (!section.hidden() && !section.disabled() && (q == null || q.isEmpty() || sectionMatches(section, q))) return sectionIndex;
            }
            for (int sectionIndex : group.sectionIndexes) {
                ConfigSection section = sections.get(sectionIndex);
                if (!section.hidden() && (q == null || q.isEmpty() || sectionMatches(section, q))) return sectionIndex;
            }
            for (int sectionIndex : group.sectionIndexes) if (!sections.get(sectionIndex).hidden()) return sectionIndex;
            return firstVisibleSection();
        }

        private int mainIndexForSection(int sectionIndex) {
            for (int i = 0; i < mainGroups.size(); i++) {
                if (mainGroups.get(i).sectionIndexes.contains(sectionIndex)) return i;
            }
            return 0;
        }

        private int firstVisibleSection() {
            for (int i = 0; i < sections.size(); i++) if (!sections.get(i).hidden() && !sections.get(i).disabled()) return i;
            for (int i = 0; i < sections.size(); i++) if (!sections.get(i).hidden()) return i;
            return 0;
        }

        private static List<MainCategoryGroup> groupSections(List<ConfigSection> sections) {
            List<MainCategoryGroup> groups = new ArrayList<>();
            for (int i = 0; i < sections.size(); i++) {
                ConfigSection section = sections.get(i);
                MainCategoryGroup group = null;
                for (MainCategoryGroup candidate : groups) {
                    if (candidate.title.equals(section.mainCategory)) { group = candidate; break; }
                }
                if (group == null) {
                    group = new MainCategoryGroup(section.mainCategory);
                    groups.add(group);
                }
                group.sectionIndexes.add(i);
            }
            if (groups.isEmpty()) groups.add(new MainCategoryGroup("General"));
            return groups;
        }

        private static final class MainCategoryGroup {
            private final String title;
            private final List<Integer> sectionIndexes = new ArrayList<>();
            private MainCategoryGroup(String title) { this.title = title == null || title.isBlank() ? "General" : title; }
        }

        private String normalizedSearch() {
            String q = searchQuery.get();
            return q == null ? "" : MinecraftTextFormatting.normalizeForSearch(q.trim());
        }

        private void applySearch(boolean allowSelectFirstMatch) {
            String q = normalizedSearch();
            boolean queryChanged = lastAppliedSearch == null || !lastAppliedSearch.equals(q);
            if (queryChanged) {
                lastAppliedSearch = q;
                for (ConfigSection section : sections) applySearchToChildren(section.content, q, section.title);
            }

            int selectedIdx = Math.max(0, Math.min(sections.size() - 1, selected.get()));
            int mainIdx = clampMainIndex();
            boolean selectedHidden = sections.get(selectedIdx).hidden();
            boolean selectedDisabled = sections.get(selectedIdx).disabled();
            boolean selectedInMain = mainGroups.get(mainIdx).sectionIndexes.contains(selectedIdx);
            boolean selectedMatches = sectionMatches(sections.get(selectedIdx), q);
            boolean selectionChanged = false;

            if (allowSelectFirstMatch && (selectedHidden || selectedDisabled || !selectedInMain || (!q.isEmpty() && !selectedMatches))) {
                for (int i : visibleMainIndexes()) {
                    int candidate = firstVisibleSectionForMain(i, q);
                    if (!sections.get(candidate).hidden() && !sections.get(candidate).disabled() && (q.isEmpty() || sectionMatches(sections.get(candidate), q))) {
                        if (selectedMain.get() != i) selectedMain.set(i);
                        if (expandedMainCategory != i) expandedMainCategory = i;
                        if (selected.get() != candidate) selected.set(candidate);
                        selectionChanged = true;
                        break;
                    }
                }
            }

            String signature = sidebarSignature(q);
            if (queryChanged || selectionChanged || lastSidebarSignature == null || !lastSidebarSignature.equals(signature)) {
                suppressNextExpandedGroupAnimation = !queryChanged
                        && lastSidebarSignature != null
                        && expandedMainCategory == selectedMain.get();
                lastSidebarSignature = signature;
                rebuildSidebar();
                markLayoutDirty();
            }
        }

        private String sidebarSignature(String q) {
            StringBuilder builder = new StringBuilder(96 + sections.size() * 8);
            builder.append(q == null ? "" : q).append('|')
                    .append(expandedMainCategory).append('|')
                    .append(selectedMain.get());
            for (ConfigSection section : sections) {
                builder.append('|')
                        .append(section.hidden() ? '1' : '0')
                        .append(section.disabled() ? '1' : '0');
            }
            return builder.toString();
        }

        private static void applySearchToChildren(Component component, String query, String sectionTitle) {
            if (component instanceof SearchAware aware) aware.search(query, sectionTitle);
            for (Component child : component.children()) applySearchToChildren(child, query, sectionTitle);
        }

        private static boolean sectionMatches(ConfigSection section, String query) {
            if (section.hidden()) return false;
            if (query == null || query.isEmpty()) return true;
            if (contains(section.title, query) || contains(section.mainCategory, query)) return true;
            return childrenMatch(section.content, query, section.title);
        }

        private static boolean childrenMatch(Component component, String query, String sectionTitle) {
            if (component instanceof SearchAware aware && aware.searchMatches(query, sectionTitle)) return true;
            for (Component child : component.children()) {
                if (childrenMatch(child, query, sectionTitle)) return true;
            }
            return false;
        }

        private static boolean contains(String haystack, String needle) {
            return haystack != null && needle != null && !needle.isEmpty()
                    && MinecraftTextFormatting.normalizeForSearch(haystack).contains(needle);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return constraints.clamp(new Size(constraints.maxWidth(), constraints.maxHeight()));
        }

        @Override
        public void tick(float deltaSeconds) {
            syncMainSelection();
            applySearch(true);
            super.tick(deltaSeconds);
        }

        private void syncMainSelection() {
            int idx = clampMainIndex();
            if (isMainDisabled(idx)) {
                for (int candidate : visibleMainIndexes()) {
                    if (!isMainDisabled(candidate)) { idx = candidate; break; }
                }
                if (selectedMain.get() != idx) selectedMain.set(idx);
            }
            if (idx == lastObservedMainIndex) return;
            lastObservedMainIndex = idx;
            expandedMainCategory = idx;
            selected.set(firstVisibleSectionForMain(idx, normalizedSearch()));
            rebuildSidebar();
            markLayoutDirty();
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            applySearch(false);
            int idx = Math.max(0, Math.min(sections.size() - 1, selected.get()));
            if (sections.get(idx).hidden() || sections.get(idx).disabled()) {
                idx = firstVisibleSectionForMain(clampMainIndex(), normalizedSearch());
                selected.set(idx);
            }
            setSectionContentEnabled(sections.get(idx), !sections.get(idx).disabled());
            if (contentScroll.content() != sections.get(idx).content) {
                contentScroll.setContent(sections.get(idx).content);
            }

            Rect frame = frameRect(bounds);
            boolean compactHeader = compactHeader(frame.w());
            float topHeight = topBarHeight(frame.w());
            float sidebarWidth = sidebarWidth(frame.w());
            Rect sidebarRect = new Rect(frame.x(), frame.y() + topHeight, sidebarWidth, frame.h() - topHeight);
            Rect contentRect = new Rect(frame.x() + sidebarWidth + 1, frame.y() + topHeight + 1, frame.w() - sidebarWidth - 1, frame.h() - topHeight - 1);

            float searchWidth = searchWidth(frame.w(), compactHeader);
            float settingsSize = 34;
            float settingsGap = 8;
            float searchX = frame.right() - 24 - settingsSize - settingsGap - searchWidth;
            searchControl.width(searchWidth).height(34);
            searchControl.layout(context, new Rect(searchX, frame.y() + 12, searchWidth, 34));
            settingsButton.width(settingsSize).height(settingsSize);
            settingsButton.layout(context, new Rect(searchX + searchWidth + settingsGap, frame.y() + 12, settingsSize, settingsSize));
            settingsPopup.layout(context, bounds);
            if (footerArea != null) {
                float footerInsetX = 14.0f;
                float footerTopGap = 10.0f;
                float footerBottomGap = 14.0f;
                float footerWidth = Math.max(0, sidebarRect.w() - footerInsetX * 2.0f);
                Size footerSize = footerArea.measure(context, new Constraints(footerWidth, sidebarRect.h()));
                float reserved = Math.min(sidebarRect.h(), footerSize.height() + footerTopGap + footerBottomGap);
                Rect footerRect = new Rect(sidebarRect.x() + footerInsetX, sidebarRect.bottom() - footerBottomGap - footerSize.height(), footerWidth, footerSize.height());
                Rect sidebarContent = new Rect(sidebarRect.x(), sidebarRect.y(), sidebarRect.w(), Math.max(0, sidebarRect.h() - reserved));
                sidebarScroll.layout(context, sidebarContent);
                footerArea.layout(context, footerRect);
            } else {
                sidebarScroll.layout(context, sidebarRect);
            }
            contentScroll.layout(context, contentRect);
        }

        @Override
        protected void renderSelf(RenderContext context) {
            Rect frame = frameRect(bounds);
            boolean compactHeader = compactHeader(frame.w());
            float topHeight = topBarHeight(frame.w());
            float sidebarWidth = sidebarWidth(frame.w());
            float radius = context.theme().radii().lg();
            float innerX = frame.x() + 1;
            float innerY = frame.y() + 1;
            float innerRight = frame.right() - 1;
            float innerBottom = frame.bottom() - 1;
            Rect top = new Rect(innerX, innerY, Math.max(0, frame.w() - 2), topHeight - 1);
            Rect side = new Rect(innerX, frame.y() + topHeight + 1, Math.max(0, sidebarWidth - 1), Math.max(0, frame.h() - topHeight - 2));
            Rect content = new Rect(frame.x() + sidebarWidth + 1, frame.y() + topHeight + 1, Math.max(0, frame.w() - sidebarWidth - 2), Math.max(0, frame.h() - topHeight - 2));

            ColorRGBA frameFill = context.theme().palette().surface().darken(0.18f);
            ColorRGBA topFill = context.theme().palette().surface().darken(0.08f);
            ColorRGBA sideFill = context.theme().palette().surface().darken(0.13f);
            ColorRGBA contentFill = context.theme().palette().background().darken(0.04f);

            context.rect(frame, SdfRectStyle.create()
                    .fill(frameFill)
                    .border(1, context.theme().palette().border())
                    .shadow(14, 0, 3, context.theme().palette().shadow().multiplyAlpha(0.55f))
                    .radius(radius), z - 10);

            context.rect(top, SdfRectStyle.create()
                    .fill(topFill)
                    .border(0, ColorRGBA.TRANSPARENT)
                    .radius(Math.max(0, radius - 1)), z - 9);
            context.rect(new Rect(innerX, frame.y() + topHeight, Math.max(0, frame.w() - 2), 1), context.theme().palette().border(), 0, z - 8);

            float r = Math.max(0, radius - 1);
            float d = r * 2;

            context.rect(new Rect(side.x(), side.y(), side.w(), Math.max(0, side.h() - r)), SdfRectStyle.create().fill(sideFill).radius(0), z - 8.9f);
            context.rect(new Rect(side.x() + r, side.y(), Math.max(0, side.w() - r), side.h()), SdfRectStyle.create().fill(sideFill).radius(0), z - 8.85f);
            if (d > 0 && side.h() >= d) {
                context.rect(new Rect(side.x(), side.bottom() - d, d, d), SdfRectStyle.create().fill(sideFill).radius(r), z - 8.95f);
            }

            context.rect(new Rect(content.x(), content.y(), content.w(), Math.max(0, content.h() - r)), SdfRectStyle.create().fill(contentFill).radius(0), z - 8.9f);
            context.rect(new Rect(content.x(), content.y(), Math.max(0, content.w() - r), content.h()), SdfRectStyle.create().fill(contentFill).radius(0), z - 8.85f);
            if (d > 0 && content.h() >= d) {
                context.rect(new Rect(content.right() - d, content.bottom() - d, d, d), SdfRectStyle.create().fill(contentFill).radius(r), z - 8.95f);
            }

            context.rect(new Rect(frame.x() + sidebarWidth, frame.y() + topHeight + 1, 1, Math.max(0, frame.h() - topHeight - 2)), context.theme().palette().border(), 0, z - 8);

            float titleSize = context.theme().fonts().title();
            String titleFace = semibold(context);
            float titleX = top.x() + 24;
            float titleLimitX = frame.right() - 24 - searchWidth(frame.w(), compactHeader) - 44;
            float titleMaxW = Math.max(60, titleLimitX - titleX - 28);
            String visibleTitle = ellipsize(context, title, titleSize, titleFace, titleMaxW);
            TextMetrics titleMetrics = context.measureText(visibleTitle, titleSize, titleFace);
            float titleY = (frame.y() + TOP_BAR_HEIGHT * 0.5f) - context.lineHeight(titleSize, titleFace) * 0.5f;
            context.text(visibleTitle, titleX, titleY, titleSize, titleFace, context.theme().palette().accent(), z + 1);
            context.rect(new Rect(titleX, top.bottom() - 2, Math.max(1, titleMetrics.width()), 2), context.theme().palette().accent(), 0, z + 2);
        }

        private boolean compactHeader(float frameWidth) {
            return false;
        }

        private float topBarHeight(float frameWidth) {
            return TOP_BAR_HEIGHT;
        }

        private float searchWidth(float frameWidth, boolean compactHeader) {
            return Math.max(190, Math.min(320, frameWidth * 0.22f));
        }

        private boolean mainMatches(MainCategoryGroup group, String query) {
            if (contains(group.title, query)) return true;
            for (int sectionIndex : group.sectionIndexes) if (sectionMatches(sections.get(sectionIndex), query)) return true;
            return false;
        }

        @Override
        public boolean onMousePressedCapture(MouseButtonEvent event) {
            if (event.button == MouseButton.LEFT && settingsButton.open()) {
                boolean insidePopup = settingsPopup.popupRect().contains(event.x, event.y);
                boolean insideButton = settingsButton.bounds().contains(event.x, event.y);
                boolean insidePopupOverlay = settingsPopup.hitTestOverlay(event.x, event.y) != null;
                if (!insidePopup && !insideButton && !insidePopupOverlay) settingsButton.open(false);
            }
            return false;
        }

        @Override
        public boolean onMousePressed(MouseButtonEvent event) {
            return false;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent event) {
            return false;
        }

        @Override
        public boolean onMouseScrolled(MouseScrollEvent event) {
            return false;
        }

        private static void setSectionContentEnabled(ConfigSection section, boolean enabled) {
            setEnabledDeep(section.content, enabled);
        }

        private static void setEnabledDeep(Component component, boolean enabled) {
            component.enabled(enabled);
            for (Component child : component.children()) setEnabledDeep(child, enabled);
        }

        private float sidebarWidth(float frameWidth) {
            if (frameWidth <= 0) return 0;
            return Math.max(132, Math.min(SIDEBAR_WIDTH, frameWidth * 0.26f));
        }

        private Rect frameRect(Rect bounds) {
            float marginX = Math.max(36, Math.min(140, bounds.w() * 0.10f));
            float marginY = Math.max(32, Math.min(110, bounds.h() * 0.10f));
            return new Rect(bounds.x() + marginX, bounds.y() + marginY, Math.max(0, bounds.w() - marginX * 2), Math.max(0, bounds.h() - marginY * 2));
        }
    }

    private static float smoothstep(float value) {
        float t = Math.max(0.0f, Math.min(1.0f, value));
        return t * t * (3.0f - 2.0f * t);
    }

    private interface SearchAware {
        void search(String query, String sectionTitle);
        boolean searchMatches(String query, String sectionTitle);
    }

    private static final class SearchControl extends Component {
        private final MutableState<String> query;
        private final TextInput input;
        private final AnimatedFloat open = new AnimatedFloat(0).speed(18);
        private boolean active;

        private SearchControl(MutableState<String> query) {
            this.query = query;
            this.input = new TextInput(query).maxLength(96).focusable(false);
            this.focusable = true;
            add(input);
        }

        private void openImmediately() {
            active = true;
            open.snap(1.0f);
            input.setFocused(true);
        }

        @Override
        public void tick(float deltaSeconds) {
            open.target(active || !safeQuery().isEmpty() ? 1.0f : 0.0f);
            float before = open.value();
            open.update(deltaSeconds);
            input.setFocused(focused && active);
            if (Math.abs(before - open.value()) > 0.0005f) markLayoutDirty();
            super.tick(deltaSeconds);
        }

        @Override
        public boolean needsFreshRender() {
            return super.needsFreshRender() || Math.abs(open.target() - open.value()) > 0.0005f;
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 260, preferredHeight >= 0 ? preferredHeight : 34));
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            float t = open.value();
            float iconSize = 34;
            float inputW = Math.max(iconSize, bounds.w() * t);
            Rect inputRect = new Rect(bounds.right() - inputW, bounds.y(), inputW, bounds.h());
            input.layout(context, inputRect);
        }

        @Override
        protected void renderSelf(RenderContext context) {
            float t = open.value();
            if (t < 0.985f) {
                float iconAlpha = 255.0f * (1.0f - t);
                Rect icon = new Rect(bounds.right() - 34, bounds.y(), 34, bounds.h());
                context.rect(icon, SdfRectStyle.create()
                        .fill(context.theme().palette().surfaceHover().withAlpha((int)(120 * (1.0f - t))))
                        .border(1, context.theme().palette().border().withAlpha((int)(180 * (1.0f - t))))
                        .radius(context.theme().radii().md()), z + 4);
                drawMagnifier(context, icon, context.theme().palette().text().withAlpha((int) iconAlpha), z + 6);
            }
        }

        @Override
        public void render(RenderContext context, Rect clip) {
            if (!visible()) return;
            renderSelf(context);
            if (open.value() > 0.025f) input.render(context, clip);
        }

        private float visibleWidth(float fullWidth) {
            float iconSize = 34;
            return iconSize + Math.max(0, fullWidth - iconSize) * open.value();
        }

        private Rect visibleRect() {
            float w = visibleWidth(bounds.w());
            return new Rect(bounds.right() - w, bounds.y(), w, bounds.h());
        }

        @Override
        public Component hitTest(float x, float y) {
            if (!visible() || !enabled()) return null;
            Rect visible = visibleRect();
            if (!visible.contains(x, y)) return null;
            return this;
        }

        @Override
        public boolean onMousePressed(MouseButtonEvent event) {
            if (event.button != MouseButton.LEFT) return false;
            active = true;
            input.setFocused(true);
            if (open.value() > 0.15f) input.onMousePressed(event);
            markLayoutDirty();
            return true;
        }

        @Override
        public boolean onMouseDragged(MouseDragEvent event) {
            return focused && active && input.onMouseDragged(event);
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent event) {
            return focused && active && input.onMouseReleased(event);
        }

        @Override
        public boolean onKeyPressed(KeyEvent event) {
            if (!focused || !active) return false;
            if (event.keyCode == KeyCodes.ESCAPE) {
                if (!safeQuery().isEmpty()) query.set("");
                else active = false;
                markLayoutDirty();
                return true;
            }
            if (Modifiers.has(event.modifiers, Modifiers.CTRL) || Modifiers.has(event.modifiers, Modifiers.SUPER)) {
                int key = Character.toUpperCase(event.keyCode);
                if (key == 'F') { input.setFocused(true); return true; }
            }
            return input.onKeyPressed(event);
        }

        @Override
        public boolean onCharTyped(CharTypedEvent event) {
            return focused && active && input.onCharTyped(event);
        }

        @Override
        protected void onFocusChanged(boolean focused) {
            if (focused) {
                active = true;
                input.setFocused(true);
            } else {
                input.setFocused(false);
                if (safeQuery().isEmpty()) active = false;
            }
            markLayoutDirty();
        }

        private String safeQuery() {
            String value = query.get();
            return value == null ? "" : value;
        }

        private static void drawMagnifier(RenderContext context, Rect rect, ColorRGBA color, float z) {
            float font = context.theme().fonts().large();
            String face = semibold(context);
            String glyph = "\uD83D\uDD0D";
            TextMetrics metrics = context.measureText(glyph, font, face);
            context.text(glyph, rect.centerX() - metrics.width() * 0.5f, rect.centerY() - context.lineHeight(font, face) * 0.5f, font, face, color, z);
        }
    }

    private static final class SettingsButton extends Component {
        private final AnimatedFloat hoverAnim = new AnimatedFloat(0).speed(18);
        private boolean open;

        private SettingsButton() {
            this.focusable = true;
            this.size(34, 34);
            this.tooltip("Config screen settings");
        }

        private boolean open() { return open; }
        private void open(boolean open) { if (this.open != open) { this.open = open; markLayoutDirty(); } }

        @Override
        public void tick(float deltaSeconds) {
            hoverAnim.target((hovered || focused || open) ? 1.0f : 0.0f);
            hoverAnim.update(deltaSeconds);
            super.tick(deltaSeconds);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 34, preferredHeight >= 0 ? preferredHeight : 34));
        }

        @Override
        protected void renderSelf(RenderContext context) {
            ColorRGBA fill = context.theme().palette().surfaceHover().withAlpha((int)(105 + 70 * hoverAnim.value()));
            ColorRGBA border = open || focused ? context.theme().palette().accent() : context.theme().palette().border();
            context.rect(bounds, SdfRectStyle.create().fill(fill).border(1, border).radius(context.theme().radii().md()), z + 4);
            ColorRGBA iconColor = open ? context.theme().palette().accent() : context.theme().palette().text();
            String glyph = "\uD83D\uDEE0";
            float font = context.theme().fonts().normal() + 2.0f;
            String face = semibold(context);
            TextMetrics metrics = context.measureText(glyph, font, face);
            if (metrics.width() > 0.0f) {
                context.text(glyph, bounds.centerX() - metrics.width() * 0.5f,
                        bounds.centerY() - context.lineHeight(font, face) * 0.5f,
                        font, face, iconColor, z + 6);
            }
        }

        @Override
        public boolean onMousePressed(MouseButtonEvent event) {
            if (event.button != MouseButton.LEFT) return false;
            pressed = true;
            return true;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent event) {
            boolean was = pressed;
            pressed = false;
            if (was && bounds.contains(event.x, event.y) && event.button == MouseButton.LEFT) {
                open(!open);
                return true;
            }
            return was;
        }
    }

    private static final class SettingsPopup extends Component {
        private final SettingsButton anchor;
        private final ConfigUiSettings settings;
        private final MutableState<Theme> themeState = new MutableState<>(null);
        private final MutableState<Double> scalePreview;
        private final Dropdown<Theme> themeDropdown;
        private final CommitScaleSlider scaleSlider;
        private final ToggleSwitch textShadowToggle;
        private boolean syncingTheme;
        private final Button resetButton;
        private final AnimatedFloat openAnim = new AnimatedFloat(0).speed(20);
        private long observedSettingsVersion = -1;

        private SettingsPopup(SettingsButton anchor, ConfigUiSettings settings) {
            this.anchor = anchor;
            this.settings = settings == null ? ConfigUiSettings.create() : settings;
            this.scalePreview = new MutableState<>(this.settings.scale());
            this.themeState.set(currentThemeValue());
            this.themeDropdown = new Dropdown<>(themeState, themeOptions())
                    .labeler(theme -> theme == null ? "Default" : theme.name())
                    .itemHeight(26)
                    .maxVisibleRows(6)
                    .height(30);
            this.scaleSlider = new CommitScaleSlider(scalePreview, this.settings::scale).height(32);
            this.textShadowToggle = new ToggleSwitch(new State<Boolean>() {
                @Override public Boolean get() { return SettingsPopup.this.settings.textShadow(); }
                @Override public void set(Boolean value) { SettingsPopup.this.settings.textShadow(Boolean.TRUE.equals(value)); }
                @Override public AutoCloseable subscribe(Consumer<Boolean> listener) { return () -> { }; }
            });
            this.resetButton = new Button("Reset config defaults").onClick(() -> {
                this.settings.resetLinkedDefaults();
                syncFromSettings(true);
                anchor.open(false);
            }).height(30).fillX();
            this.themeState.subscribe(theme -> {
                if (!syncingTheme) this.settings.selectedTheme(theme);
            });
            add(themeDropdown);
            add(scaleSlider);
            add(textShadowToggle);
            add(resetButton);
        }

        private List<Theme> themeOptions() {
            List<Theme> options = new ArrayList<>();
            for (Theme theme : settings.themes()) {
                boolean exists = false;
                for (Theme existing : options) {
                    if (Objects.equals(existing.name(), theme.name())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) options.add(theme);
            }
            Theme active = settings.activeTheme();
            if (active != null) {
                boolean exists = false;
                for (Theme existing : options) {
                    if (Objects.equals(existing.name(), active.name())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) options.add(0, active);
            }
            return options;
        }

        private Theme currentThemeValue() {
            return settings.activeTheme();
        }

        private void syncFromSettings(boolean force) {
            long version = settings.version();
            if (!force && version == observedSettingsVersion) return;
            observedSettingsVersion = version;
            themeDropdown.options(themeOptions());
            Theme current = settings.activeTheme();
            syncingTheme = true;
            try {
                themeState.set(current);
            } finally {
                syncingTheme = false;
            }
            scalePreview.set(settings.scale());
            scaleSlider.snapPreview(settings.scale());
            markLayoutDirty();
        }

        @Override
        public void tick(float deltaSeconds) {
            syncFromSettings(false);
            float before = openAnim.value();
            openAnim.target(anchor.open() ? 1.0f : 0.0f);
            openAnim.update(deltaSeconds);
            if (Math.abs(before - openAnim.value()) > 0.0005f) markLayoutDirty();
            super.tick(deltaSeconds);
        }

        @Override
        public boolean visible() {
            return super.visible() && (anchor.open() || openAnim.value() > 0.01f);
        }

        @Override
        public boolean needsFreshRender() {
            return super.needsFreshRender() || Math.abs(openAnim.target() - openAnim.value()) > 0.0005f;
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return constraints.clamp(new Size(292, 252));
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            Rect popup = popupRect();
            this.bounds = popup;
            float padding = 16;
            float x = popup.x() + padding;
            float w = popup.w() - padding * 2;

            themeDropdown.width(w).height(30);
            themeDropdown.layout(context, new Rect(x, popup.y() + 40, w, 30));

            textShadowToggle.width(42).height(20);
            textShadowToggle.layout(context, new Rect(popup.right() - padding - 42, popup.y() + 92, 42, 20));

            scaleSlider.width(w).height(32);
            scaleSlider.layout(context, new Rect(x, popup.y() + 154, w, 32));

            resetButton.width(w).height(30);
            resetButton.layout(context, new Rect(x, popup.bottom() - padding - 30, w, 30));
        }

        private Rect popupRect() {
            float w = 292;
            float h = 252;
            float x = Math.max(8, anchor.bounds().right() - w);
            float y = anchor.bounds().bottom() + 8;
            if (parent != null) {
                Rect root = parent.bounds();
                if (x + w > root.right() - 8) x = root.right() - 8 - w;
                if (y + h > root.bottom() - 8) y = Math.max(8, anchor.bounds().y() - h - 8);
            }
            return new Rect(x, y, w, h);
        }

        private Rect animatedPopupRect() {
            return popupRect(openAnim.value());
        }

        private Rect popupRect(float opened) {
            Rect base = popupRect();
            float t = smoothstep(opened);
            float h = Math.max(0.0f, base.h() * t);
            return new Rect(base.x(), base.y(), base.w(), h);
        }

        @Override
        protected void renderSelf(RenderContext context) { }

        @Override
        public void render(RenderContext context, Rect clip) { }

        @Override
        public void renderOverlay(RenderContext context) {
            if (!visible()) return;
            float t = smoothstep(openAnim.value());
            if (t <= 0.001f) return;
            Rect full = popupRect();
            Rect reveal = popupRect(openAnim.value());
            this.bounds = full;
            int alpha = Math.max(0, Math.min(255, Math.round(255.0f * t)));
            context.rect(reveal, SdfRectStyle.create()
                    .fill(context.theme().palette().surface().darken(0.03f).withAlpha(alpha))
                    .border(1, context.theme().palette().border().withAlpha(Math.round(255.0f * t)))
                    .shadow(16, 0, 4, context.theme().palette().shadow().multiplyAlpha(0.52f * t))
                    .radius(context.theme().radii().lg())
                    .opacity(t), z + 8000);

            boolean clipOpening = t < 0.999f;
            if (clipOpening) context.pushClip(reveal.inset(1, 1, 1, 1));
            float padding = 16;
            float labelFont = context.theme().fonts().small();
            context.text("Theme", full.x() + padding, full.y() + padding, labelFont, regular(context),
                    context.theme().palette().mutedText().withAlpha(alpha), z + 8002);
            context.text("Text shadow", full.x() + padding, full.y() + 92, labelFont, regular(context),
                    context.theme().palette().mutedText().withAlpha(alpha), z + 8002);
            context.text("Draws a subtle shadow behind config text", full.x() + padding, full.y() + 109, labelFont, regular(context),
                    context.theme().palette().mutedText().withAlpha(Math.round(175.0f * t)), z + 8002);
            context.text("HUD scale", full.x() + padding, full.y() + 138, labelFont, regular(context),
                    context.theme().palette().mutedText().withAlpha(alpha), z + 8002);
            float separatorY = full.bottom() - padding - 30 - 13;
            context.rect(new Rect(full.x() + padding, separatorY, full.w() - padding * 2, 1),
                    SdfRectStyle.create()
                            .fill(context.theme().palette().border().withAlpha(Math.round(145.0f * t)))
                            .radius(0),
                    z + 8002);
            themeDropdown.z(z + 8050);
            scaleSlider.z(z + 8050);
            textShadowToggle.z(z + 8050);
            resetButton.z(z + 8050);
            themeDropdown.render(context);
            scaleSlider.render(context);
            textShadowToggle.render(context);
            resetButton.render(context);
            if (clipOpening) context.popClip();
            themeDropdown.renderOverlay(context);
        }

        @Override
        public Component hitTestOverlay(float x, float y) {
            if (!visible() || openAnim.value() < 0.08f) return null;
            Rect popup = popupRect();
            this.bounds = popup;
            Component hit = themeDropdown.hitTestOverlay(x, y);
            if (hit != null) return hit;
            for (int i = children.size() - 1; i >= 0; i--) {
                hit = children.get(i).hitTest(x, y);
                if (hit != null) return hit;
            }
            return popup.contains(x, y) ? this : null;
        }

        @Override
        public boolean onMousePressed(MouseButtonEvent event) {
            return visible() && openAnim.value() >= 0.08f && popupRect().contains(event.x, event.y);
        }
    }

    private static final class CommitScaleSlider extends Component {
        private final MutableState<Double> preview;
        private final java.util.function.DoubleConsumer commit;
        private boolean dragging;

        private CommitScaleSlider(MutableState<Double> preview, java.util.function.DoubleConsumer commit) {
            this.preview = preview;
            this.commit = commit == null ? value -> { } : commit;
            this.focusable = true;
            this.height(32);
        }

        private void snapPreview(double value) {
            preview.set(ConfigUiSettings.nearestScale(value));
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 240, preferredHeight >= 0 ? preferredHeight : 32));
        }

        @Override
        protected void renderSelf(RenderContext context) {
            Rect track = trackRect();
            context.rect(track, SdfRectStyle.create().fill(context.theme().palette().surfaceAlt()).radius(context.theme().radii().sm()), z);
            double[] steps = ConfigUiSettings.SCALE_STEPS;
            int index = ConfigUiSettings.scaleIndex(preview.get());
            float t = steps.length <= 1 ? 0.0f : index / (float)(steps.length - 1);
            context.rect(new Rect(track.x(), track.y(), Math.max(2, track.w() * t), track.h()), SdfRectStyle.create().fill(context.theme().palette().accent()).radius(context.theme().radii().sm()), z + 1);
            for (int i = 0; i < steps.length; i++) {
                float x = track.x() + track.w() * (steps.length == 1 ? 0.0f : i / (float)(steps.length - 1));
                context.rect(new Rect(x - 2, track.centerY() - 2, 4, 4), SdfRectStyle.create().fill(i <= index ? context.theme().palette().accentText() : context.theme().palette().border()).radius(2), z + 2);
            }
            float knobSize = 12;
            Rect knob = new Rect(track.x() + track.w() * t - knobSize * 0.5f, track.centerY() - knobSize * 0.5f, knobSize, knobSize);
            context.rect(knob, SdfRectStyle.create().fill(context.theme().palette().accent()).radius(knobSize * 0.5f), z + 3);
            String label = Math.round(preview.get() * 100.0d) + "%";
            float font = context.theme().fonts().small();
            TextMetrics metrics = context.measureText(label, font, medium(context));
            context.text(label, bounds.right() - metrics.width(), bounds.y(), font, medium(context), context.theme().palette().mutedText(), z + 4);
        }

        private Rect trackRect() {
            return new Rect(bounds.x(), bounds.y() + 18, Math.max(1, bounds.w()), 5);
        }

        @Override
        public boolean onMousePressed(MouseButtonEvent event) {
            if (event.button != MouseButton.LEFT) return false;
            if (!bounds.contains(event.x, event.y)) return false;
            dragging = true;
            updatePreview(event.x);
            return true;
        }

        @Override
        public boolean onMouseDragged(MouseDragEvent event) {
            if (!dragging) return false;
            updatePreview(event.x);
            return true;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent event) {
            if (!dragging) return false;
            dragging = false;
            updatePreview(event.x);
            commit.accept(preview.get());
            return true;
        }

        private void updatePreview(float mouseX) {
            Rect track = trackRect();
            double[] steps = ConfigUiSettings.SCALE_STEPS;
            float t = Math.max(0, Math.min(1, (mouseX - track.x()) / Math.max(1, track.w())));
            int index = Math.round(t * (steps.length - 1));
            index = Math.max(0, Math.min(steps.length - 1, index));
            preview.set(steps[index]);
            markLayoutDirty();
        }
    }

    private static final class CategoryGroupPanel extends Column {
        private final BooleanSupplier activeWhen;
        private final AnimatedFloat expandAnim = new AnimatedFloat(0).speed(18);

        private CategoryGroupPanel(BooleanSupplier activeWhen, boolean initiallyExpanded) {
            this.activeWhen = activeWhen == null ? () -> false : activeWhen;
            if (initiallyExpanded) this.expandAnim.snap(1.0f);
            this.padding(0, 0);
            this.gap(0);
        }

        @Override
        public void tick(float deltaSeconds) {
            expandAnim.target(active() ? 1.0f : 0.0f);
            float before = expandAnim.value();
            expandAnim.update(deltaSeconds);
            if (Math.abs(before - expandAnim.value()) > 0.0005f) markLayoutDirty();
            super.tick(deltaSeconds);
        }

        @Override
        public boolean needsFreshRender() {
            return super.needsFreshRender() || Math.abs(expandAnim.target() - expandAnim.value()) > 0.0005f;
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            Size full = super.measureSelf(context, constraints);
            float collapsed = collapsedHeight(context, constraints);
            float t = smoothstep(expandAnim.value());
            return constraints.clamp(new Size(full.width(), collapsed + Math.max(0.0f, full.height() - collapsed) * t));
        }

        private float collapsedHeight(LayoutContext context, Constraints constraints) {
            if (children.isEmpty()) return padding.vertical();
            Component header = children.get(0);
            Size headerSize = header.measure(context, new Constraints(Math.max(0, constraints.maxWidth() - padding.horizontal() - header.margin().horizontal()), constraints.maxHeight()));
            return padding.vertical() + headerSize.height() + header.margin().vertical();
        }

        @Override
        public void render(RenderContext context, Rect clip) {
            if (!visible()) return;
            Rect ownClip = clip == null ? bounds : bounds.intersect(clip);
            if (ownClip.isEmpty()) return;
            context.pushClip(ownClip);
            super.render(context, ownClip);
            context.popClip();
        }

        @Override
        protected void renderSelf(RenderContext context) {
            float t = smoothstep(expandAnim.value());
            if (t <= 0.001f) return;
            Component firstEntry = null;
            Component lastEntry = null;
            for (int i = 1; i < children.size(); i++) {
                Component child = children.get(i);
                if (!child.visible() || child instanceof Spacer) continue;
                if (firstEntry == null) firstEntry = child;
                lastEntry = child;
            }
            if (firstEntry == null) return;

            float headerRowX = bounds.x() + 4;
            float headerRowW = Math.max(0, bounds.w() - 6);
            float shelfY = Math.max(bounds.y() + 40, firstEntry.bounds().y() - 6);
            float shelfBottom = Math.min(bounds.bottom(), lastEntry.bounds().bottom() + 6);
            Rect shelf = new Rect(headerRowX, shelfY, headerRowW, Math.max(0, shelfBottom - shelfY));
            if (shelf.h() <= 4) return;

            float radius = context.theme().radii().md();
            ColorRGBA fill = context.theme().palette().surface().darken(0.105f).withAlpha(Math.round(132.0f * t));
            ColorRGBA border = context.theme().palette().border().withAlpha(Math.round(34.0f * t));

            context.rect(shelf, SdfRectStyle.create()
                    .fill(fill)
                    .border(1, border)
                    .radius(radius), z - 2);
            context.rect(new Rect(shelf.x() + 11, shelf.y() + 8, 1, Math.max(0, shelf.h() - 16)), SdfRectStyle.create()
                    .fill(context.theme().palette().border().withAlpha(Math.round(78.0f * t)))
                    .radius(1), z - 1);
        }

        private boolean active() {
            try { return activeWhen.getAsBoolean(); } catch (RuntimeException ignored) { return false; }
        }
    }

    private static final class CategoryHeader extends Component {
        private final String label;
        private final int index;
        private final BooleanSupplier activeWhen;
        private final BooleanSupplier expandedWhen;
        private final BooleanSupplier disabledWhen;
        private final Runnable clickAction;
        private final Runnable focusAction;
        private final AnimatedFloat hoverAnim = new AnimatedFloat(0).speed(10);
        private final AnimatedFloat pressAnim = new AnimatedFloat(0).speed(22);
        private boolean armed;

        private CategoryHeader(String label, int index, BooleanSupplier activeWhen, BooleanSupplier expandedWhen, BooleanSupplier disabledWhen, Runnable clickAction, Runnable focusAction) {
            this.label = label == null ? "" : label;
            this.index = index;
            this.activeWhen = activeWhen == null ? () -> false : activeWhen;
            this.expandedWhen = expandedWhen == null ? () -> false : expandedWhen;
            this.disabledWhen = disabledWhen == null ? () -> false : disabledWhen;
            this.clickAction = clickAction == null ? () -> { } : clickAction;
            this.focusAction = focusAction == null ? () -> { } : focusAction;
            this.focusable = true;
            this.height(40);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 210, 40));
        }

        @Override
        public void tick(float deltaSeconds) {
            boolean interactive = !disabled();
            hoverAnim.target((hovered && interactive) ? 1.0f : 0.0f);
            pressAnim.target((armed && interactive) ? 1.0f : 0.0f);
            hoverAnim.update(deltaSeconds);
            pressAnim.update(deltaSeconds);
            super.tick(deltaSeconds);
        }

        @Override
        public boolean needsFreshRender() {
            return super.needsFreshRender()
                    || Math.abs(hoverAnim.target() - hoverAnim.value()) > 0.00005f
                    || Math.abs(pressAnim.target() - pressAnim.value()) > 0.00005f;
        }

        @Override
        protected void renderSelf(RenderContext context) {
            boolean disabled = disabled();
            boolean active = active();
            boolean expanded = expanded();
            boolean open = active || expanded;
            float hover = smoothstep(hoverAnim.value());
            float press = smoothstep(pressAnim.value());
            float radius = context.theme().radii().md();
            Rect row = bounds.inset(4, 0, 2, 0);

            if (open) {
                ColorRGBA fill = context.theme().palette().surfaceHover().withAlpha(178);
                ColorRGBA border = context.theme().palette().border().withAlpha(82);
                drawCategoryRow(context, row, fill, border, radius, z);
            }
            if (hover > 0.002f) {
                ColorRGBA fill = context.theme().palette().surfaceHover().lighten(0.075f).withAlpha(Math.round((open ? 82 : 190) * hover));
                ColorRGBA border = context.theme().palette().border().lighten(0.18f).withAlpha(Math.round(118 * hover));
                drawCategoryRow(context, row, fill, border, radius, z + 0.12f);
            }
            if (press > 0.002f) {
                ColorRGBA fill = context.theme().palette().accent().withAlpha(Math.round((open ? 72 : 46) * press));
                ColorRGBA border = context.theme().palette().accent().withAlpha(Math.round(165 * press));
                drawCategoryRow(context, row, fill, border, radius, z + 0.24f);
            }

            if (active) {
                context.rect(new Rect(row.x() + 8, row.y() + 9, 3, Math.max(0, row.h() - 18)), SdfRectStyle.create()
                        .fill(context.theme().palette().accent().withAlpha(230))
                        .radius(2), z + 2);
            }

            float font = context.theme().fonts().normal() + 2.0f;
            String face = semibold(context);
            ColorRGBA baseColor = disabled ? context.theme().palette().mutedText().withAlpha(112) : (active ? context.theme().palette().text() : context.theme().palette().mutedText().lighten(0.08f));
            ColorRGBA color = disabled ? baseColor : baseColor.lerp(context.theme().palette().text(), hover * 0.75f);
            float textX = active ? bounds.x() + 24 : bounds.x() + 20;
            String shown = ellipsize(context, label, font, face, Math.max(8, bounds.right() - textX - 10));
            Rect clip = new Rect(textX, bounds.y(), Math.max(0, bounds.right() - textX - 10), bounds.h());
            context.pushClip(clip);
            context.text(shown, textX, bounds.centerY() - context.lineHeight(font, face) * 0.5f, font, face, color, z + 3);
            context.popClip();
        }

        private static void drawCategoryRow(RenderContext context, Rect row, ColorRGBA fill, ColorRGBA border, float radius, float z) {
            if (fill.a() <= 0 && border.a() <= 0) return;
            context.rect(row, SdfRectStyle.create()
                    .fill(fill)
                    .border(border.a() <= 0 ? 0 : 1, border)
                    .radius(radius), z);
        }

        @Override
        protected void onFocusChanged(boolean focused) {
            if (focused && !hovered) focusAction.run();
        }

        @Override
        public boolean onMousePressed(MouseButtonEvent event) {
            if (disabled() || event.button != MouseButton.LEFT) return false;
            armed = true;
            return true;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent event) {
            boolean wasArmed = armed;
            armed = false;
            if (!disabled() && wasArmed && bounds.contains(event.x, event.y)) {
                clickAction.run();
                return true;
            }
            return wasArmed;
        }

        private boolean active() {
            try { return activeWhen.getAsBoolean(); } catch (RuntimeException ignored) { return false; }
        }

        private boolean expanded() {
            try { return expandedWhen.getAsBoolean(); } catch (RuntimeException ignored) { return false; }
        }

        private boolean disabled() {
            try { return disabledWhen.getAsBoolean(); } catch (RuntimeException ignored) { return false; }
        }
    }

    private static final class CategoryButton extends Component {
        private final String label;
        private final State<Integer> selected;
        private final int index;
        private final BooleanSupplier disabledWhen;
        private final AnimatedFloat hoverAnim = new AnimatedFloat(0).speed(10);
        private final AnimatedFloat pressAnim = new AnimatedFloat(0).speed(22);

        private CategoryButton(String label, State<Integer> selected, int index, BooleanSupplier disabledWhen) {
            this.label = label == null ? "" : label;
            this.selected = selected;
            this.index = index;
            this.disabledWhen = disabledWhen == null ? () -> false : disabledWhen;
            this.focusable = true;
            this.height(32);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 210, 32));
        }

        @Override
        public void tick(float deltaSeconds) {
            boolean interactive = !disabled();
            hoverAnim.target((hovered && interactive) ? 1.0f : 0.0f);
            pressAnim.target((pressed && interactive) ? 1.0f : 0.0f);
            hoverAnim.update(deltaSeconds);
            pressAnim.update(deltaSeconds);
            super.tick(deltaSeconds);
        }

        @Override
        public boolean needsFreshRender() {
            return super.needsFreshRender()
                    || Math.abs(hoverAnim.target() - hoverAnim.value()) > 0.00005f
                    || Math.abs(pressAnim.target() - pressAnim.value()) > 0.00005f;
        }

        @Override
        protected void renderSelf(RenderContext context) {
            boolean disabled = disabled();
            boolean active = selected.get() == index;
            float hover = smoothstep(hoverAnim.value());
            float press = smoothstep(pressAnim.value());
            Rect row = bounds.inset(20, 1, 4, 1);
            float radius = context.theme().radii().md();

            if (active) {
                context.rect(row, SdfRectStyle.create()
                        .fill(context.theme().palette().surfaceActive().withAlpha(198))
                        .border(1, context.theme().palette().border().withAlpha(78))
                        .radius(radius), z);
            }
            if (!disabled && hover > 0.002f) {
                context.rect(row, SdfRectStyle.create()
                        .fill(context.theme().palette().surfaceHover().lighten(0.065f).withAlpha(Math.round((active ? 80 : 205) * hover)))
                        .border(1, context.theme().palette().border().lighten(0.16f).withAlpha(Math.round(96 * hover)))
                        .radius(radius), z + 0.1f);
            }
            if (!disabled && press > 0.002f) {
                context.rect(row, SdfRectStyle.create()
                        .fill(context.theme().palette().accent().withAlpha(Math.round((active ? 55 : 38) * press)))
                        .border(1, context.theme().palette().accent().withAlpha(Math.round(120 * press)))
                        .radius(radius), z + 0.2f);
            }

            if (active) {
                context.rect(new Rect(row.x() + 8, row.centerY() - 3, 6, 6), SdfRectStyle.create()
                        .fill(context.theme().palette().accent())
                        .radius(3), z + 1);
            }

            float font = context.theme().fonts().normal();
            ColorRGBA baseColor = disabled ? context.theme().palette().mutedText().withAlpha(112) : (active ? context.theme().palette().text() : context.theme().palette().mutedText().lighten(0.06f));
            ColorRGBA color = disabled ? baseColor : baseColor.lerp(context.theme().palette().text(), hover * 0.7f);
            String face = active ? medium(context) : regular(context);
            String shown = ellipsize(context, label, font, face, Math.max(8, bounds.w() - 58));
            float textX = bounds.x() + 44;
            Rect clip = new Rect(textX, bounds.y(), Math.max(0, bounds.right() - textX - 8), bounds.h());
            context.pushClip(clip);
            context.text(shown, textX, bounds.centerY() - context.lineHeight(font, face) * 0.5f, font, face, color, z + 2);
            context.popClip();
        }

        @Override
        public boolean onMousePressed(MouseButtonEvent event) {
            if (disabled() || event.button != MouseButton.LEFT) return false;
            pressed = true;
            return true;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent event) {
            boolean was = pressed;
            pressed = false;
            if (!disabled() && was && bounds.contains(event.x, event.y)) {
                selected.set(index);
                markLayoutDirty();
                return true;
            }
            return was;
        }

        private boolean disabled() {
            try { return disabledWhen.getAsBoolean(); } catch (RuntimeException ignored) { return false; }
        }
    }

    public static final class SectionBuilder {
        private final Column content;
        private final boolean allowAccordions;
        private float labelWidth = 260;
        private float widgetWidth = 260;
        private float compactFieldWidth = 92;
        private ConditionalEntry lastEntry;

        private SectionBuilder(Column content) {
            this(content, true);
        }

        private SectionBuilder(Column content, boolean allowAccordions) {
            this.content = content;
            this.allowAccordions = allowAccordions;
        }

        public SectionBuilder labelWidth(float labelWidth) { this.labelWidth = labelWidth; return this; }
        public SectionBuilder widgetWidth(float widgetWidth) { this.widgetWidth = widgetWidth; return this; }
        public SectionBuilder compactFieldWidth(float compactFieldWidth) { this.compactFieldWidth = compactFieldWidth; return this; }

        public SectionBuilder hidden(BooleanSupplier predicate) {
            if (lastEntry != null) lastEntry.hiddenWhen(predicate);
            return this;
        }

        public SectionBuilder disabled(BooleanSupplier predicate) {
            if (lastEntry != null) lastEntry.disabledWhen(predicate);
            return this;
        }

        public SectionBuilder tooltip(String text) {
            if (lastEntry != null) lastEntry.tooltipText(text);
            return this;
        }

        public SectionBuilder toggle(String label, State<Boolean> state, String description) {
            return row(label, description, new ToggleSwitch(state), false);
        }

        public SectionBuilder checkbox(String label, State<Boolean> state, String description) {
            return row(label, description, new Checkbox(state), false);
        }

        public <N extends Number> SectionBuilder slider(String label, State<N> state, double min, double max, double step, String description) {
            return row(label, description, new Slider<>(state, min, max, step).width(widgetWidth), false);
        }

        public <N extends Number> SectionBuilder slider(String label, State<N> state, Class<?> numberType, double min, double max, double step, String description) {
            return row(label, description, new Slider<>(state, numberType, min, max, step).width(widgetWidth), false);
        }

        public <N extends Number> SectionBuilder sliderLabel(String label, State<N> state, double min, double max, double step, String description) {
            return row(label, description, new Slider<>(state, min, max, step).valueLabel().width(widgetWidth), false);
        }

        public <N extends Number> SectionBuilder sliderLabel(String label, State<N> state, Class<?> numberType, double min, double max, double step, String description) {
            return row(label, description, new Slider<>(state, numberType, min, max, step).valueLabel().width(widgetWidth), false);
        }

        public <N extends Number> SectionBuilder number(String label, State<N> state, double min, double max, double step, String description) {
            return row(label, description, new NumberInput<>(state, min, max, step).width(widgetWidth), false);
        }

        public <N extends Number> SectionBuilder number(String label, State<N> state, Class<?> numberType, double min, double max, double step, String description) {
            return row(label, description, new NumberInput<>(state, numberType, min, max, step).width(widgetWidth), false);
        }

        public SectionBuilder text(String label, State<String> state, String description, String regex) {
            return text(label, state, description, regex, TextInput.SensitiveMode.NONE);
        }

        public SectionBuilder text(String label, State<String> state, String description, String regex, TextInput.SensitiveMode sensitiveMode) {
            return row(label, description, new TextInput(state).filterRegex(regex).sensitiveMode(sensitiveMode).width(widgetWidth), false);
        }

        public SectionBuilder sensitiveText(String label, State<String> state, String description) {
            return text(label, state, description, "", TextInput.SensitiveMode.VISIBLE_WHILE_EDITING);
        }

        public SectionBuilder sensitiveText(String label, State<String> state, String description, String regex) {
            return text(label, state, description, regex, TextInput.SensitiveMode.VISIBLE_WHILE_EDITING);
        }

        public SectionBuilder password(String label, State<String> state, String description) {
            return text(label, state, description, "", TextInput.SensitiveMode.ALWAYS_HIDDEN);
        }

        public SectionBuilder password(String label, State<String> state, String description, String regex) {
            return text(label, state, description, regex, TextInput.SensitiveMode.ALWAYS_HIDDEN);
        }

        public <T extends Enum<T>> SectionBuilder dropdown(String label, State<T> state, List<T> options, Function<T, String> labeler, String description) {
            return row(label, description, new Dropdown<>(state, options).labeler(labeler).width(widgetWidth), false);
        }

        public <T extends Enum<T>> SectionBuilder searchableDropdown(String label, State<T> state, List<T> options, Function<T, String> labeler, String description) {
            return row(label, description, new SearchableDropdown<>(state, options).labeler(labeler).width(widgetWidth), false);
        }

        public <G extends Enum<G>, T> SectionBuilder groupedDropdown(String label, State<T> state, Map<G, ? extends Collection<T>> groupedOptions, String description) {
            return row(label, description, new GroupedDropdown<>(state, groupedOptions).width(widgetWidth), false);
        }

        public <G extends Enum<G>, T> SectionBuilder searchableGroupedDropdown(String label, State<T> state, Map<G, ? extends Collection<T>> groupedOptions, String description) {
            return row(label, description, new GroupedDropdown<>(state, groupedOptions, true).width(widgetWidth), false);
        }

        public <T extends Enum<T>> SectionBuilder multiSelectDropdown(String label, State<List<T>> state, List<T> options, Function<T, String> labeler, String description) {
            return row(label, description, new MultiSelectDropdown<>(state, options).labeler(labeler).width(widgetWidth), false);
        }

        public <T extends Enum<T>> SectionBuilder searchableMultiSelectDropdown(String label, State<List<T>> state, List<T> options, Function<T, String> labeler, String description) {
            return row(label, description, new MultiSelectDropdown<>(state, options, true).labeler(labeler).width(widgetWidth), false);
        }

        public <T extends Enum<T>> SectionBuilder draggableList(String label, State<List<T>> state, Class<T> enumType, Function<T, String> labeler, boolean allowEmpty, boolean allowDeleting, String description) {
            return row(label, description, new DraggableEnumList<>(state, enumType)
                    .labeler(labeler)
                    .allowEmpty(allowEmpty)
                    .allowDeleting(allowDeleting)
                    .width(widgetWidth), false);
        }

        @SafeVarargs
        public final <T extends Enum<T>> SectionBuilder dropdown(String label, State<T> state, Function<T, String> labeler, String description, T... options) {
            return dropdown(label, state, Arrays.asList(options), labeler, description);
        }

        @SafeVarargs
        public final <T extends Enum<T>> SectionBuilder searchableDropdown(String label, State<T> state, Function<T, String> labeler, String description, T... options) {
            return searchableDropdown(label, state, Arrays.asList(options), labeler, description);
        }

        @SafeVarargs
        public final <T extends Enum<T>> SectionBuilder multiSelectDropdown(String label, State<List<T>> state, Function<T, String> labeler, String description, T... options) {
            return multiSelectDropdown(label, state, Arrays.asList(options), labeler, description);
        }

        @SafeVarargs
        public final <T extends Enum<T>> SectionBuilder searchableMultiSelectDropdown(String label, State<List<T>> state, Function<T, String> labeler, String description, T... options) {
            return searchableMultiSelectDropdown(label, state, Arrays.asList(options), labeler, description);
        }

        public SectionBuilder color(String label, State<ColorRGBA> state, String description) {
            return row(label, description, new ColorPicker(state).width(widgetWidth), false);
        }

        public SectionBuilder color(String label, State<ColorRGBA> state, boolean allowAlpha, String description) {
            return row(label, description, new ColorPicker(state).allowAlpha(allowAlpha).width(widgetWidth), false);
        }

        public SectionBuilder button(String label, String buttonText, Runnable action, String description) {
            return row(label, description, new Button(buttonText).onClick(action).width(widgetWidth), false);
        }

        public SectionBuilder keybind(String label, State<Keybind> state, String description) {
            return keybind(label, state, description, false);
        }

        public SectionBuilder keybind(String label, State<Keybind> state, String description, boolean disallowNone) {
            return row(label, description, new KeybindSelector(state).disallowNone(disallowNone).width(widgetWidth), false);
        }

        public SectionBuilder text(String label, State<String> state, String description) {
            return text(label, state, description, "", TextInput.SensitiveMode.NONE);
        }

        public SectionBuilder custom(String label, Component widget, String description) {
            return row(label, description, widget, true);
        }

        public SectionBuilder customBelow(String label, Component widget, String description) {
            return row(label, description, widget, true, true);
        }

        public SectionBuilder customOption(Component component) {
            return customOption(component, "");
        }

        public SectionBuilder customOption(Component component, String searchText) {
            CustomOptionRow row = new CustomOptionRow(component, searchText).fillX();
            content.add(row);
            lastEntry = row;
            return this;
        }

        /** Alias for {@link #customOption(Component)}. */
        public SectionBuilder component(Component component) {
            return customOption(component);
        }

        /** Alias for {@link #customOption(Component, String)}. */
        public SectionBuilder component(Component component, String searchText) {
            return customOption(component, searchText);
        }

        public SectionBuilder info(String title, String description) {
            InfoRow row = new InfoRow(title, description).fillX();
            content.add(row);
            lastEntry = row;
            return this;
        }

        public SectionBuilder info(String title, String description, Supplier<String> titleSupplier, Supplier<String> descriptionSupplier, Supplier<String> tooltipSupplier) {
            InfoRow row = new InfoRow(title, description, titleSupplier, descriptionSupplier, tooltipSupplier).fillX();
            content.add(row);
            lastEntry = row;
            return this;
        }

        public SectionBuilder separator() {
            Separator separator = new Separator().fillX();
            MarkerRow row = new MarkerRow(separator).fillX();
            content.add(row);
            lastEntry = row;
            return this;
        }

        public SectionBuilder labeledSeparator(String text) {
            return labeledSeparator(text, 1.0f);
        }

        public SectionBuilder labeledSeparator(String text, float textScale) {
            LabeledSeparator separator = new LabeledSeparator(text, textScale).fillX();
            MarkerRow row = new MarkerRow(separator).fillX();
            content.add(row);
            lastEntry = row;
            return this;
        }

        public SectionBuilder spacer(float height) {
            Spacer spacer = new Spacer().height(Math.max(0, height)).fillX();
            MarkerRow row = new MarkerRow(spacer).fillX();
            content.add(row);
            lastEntry = row;
            return this;
        }

        public SectionBuilder accordion(String title, Consumer<SectionBuilder> body) {
            if (!allowAccordions) {
                throw new IllegalStateException("Nested config accordions are not supported. Max accordion depth is 1.");
            }
            Column accordionContent = new Column().gap(16).fillX();
            SectionBuilder nested = new SectionBuilder(accordionContent, false)
                    .labelWidth(labelWidth)
                    .widgetWidth(widgetWidth)
                    .compactFieldWidth(compactFieldWidth);
            body.accept(nested);
            Accordion accordion = new Accordion(title, accordionContent).fillX();
            content.add(accordion);
            lastEntry = accordion;
            return this;
        }

        public SectionBuilder accordion(String title, String description, Consumer<SectionBuilder> body) {
            if (!allowAccordions) {
                throw new IllegalStateException("Nested config accordions are not supported. Max accordion depth is 1.");
            }
            Column accordionContent = new Column().gap(16).fillX();
            SectionBuilder nested = new SectionBuilder(accordionContent, false)
                    .labelWidth(labelWidth)
                    .widgetWidth(widgetWidth)
                    .compactFieldWidth(compactFieldWidth);
            body.accept(nested);
            Accordion accordion = new Accordion(title, description, accordionContent).fillX();
            content.add(accordion);
            lastEntry = accordion;
            return this;
        }

        private SectionBuilder row(String label, String description, Component widget, boolean flexible) {
            return row(label, description, widget, flexible, false);
        }

        private SectionBuilder row(String label, String description, Component widget, boolean flexible, boolean forceBelowDescription) {
            OptionRow row = new OptionRow(label, description, labelWidth, forceBelowDescription);
            row.gap(16);
            row.align(Alignment.CENTER);
            row.fillX();
            row.minSize(0, hasDescription(description) ? 56 : 44);
            row.add(new Spacer().flex(1).fillY());
            if (flexible) widget.flex(1);
            row.add(widget);
            content.add(row);
            lastEntry = row;
            return this;
        }

        private static boolean hasDescription(String description) {
            return description != null && !description.isBlank();
        }
    }

    private interface ConditionalEntry {
        void hiddenWhen(BooleanSupplier predicate);
        void disabledWhen(BooleanSupplier predicate);
        void tooltipText(String text);
    }

    private abstract static class ConditionalComponent extends Component implements ConditionalEntry {
        private BooleanSupplier hiddenWhen = () -> false;
        private BooleanSupplier disabledWhen = () -> false;
        private Boolean lastHiddenState;
        private Boolean lastDisabledState;

        @Override
        public void hiddenWhen(BooleanSupplier predicate) {
            this.hiddenWhen = predicate == null ? () -> false : predicate;
            lastHiddenState = null;
            markLayoutDirty();
        }

        @Override
        public void disabledWhen(BooleanSupplier predicate) {
            this.disabledWhen = predicate == null ? () -> false : predicate;
            lastDisabledState = null;
            markLayoutDirty();
        }

        @Override
        public void tooltipText(String text) {
            this.tooltip(text);
        }

        protected boolean isHiddenByPredicate() {
            try { return hiddenWhen.getAsBoolean(); }
            catch (RuntimeException ignored) { return false; }
        }

        protected boolean isDisabledByPredicate() {
            try { return disabledWhen.getAsBoolean(); }
            catch (RuntimeException ignored) { return false; }
        }

        private boolean refreshConditionState() {
            boolean hidden = isHiddenByPredicate();
            boolean disabled = isDisabledByPredicate();
            boolean changed = (lastHiddenState != null && lastHiddenState != hidden)
                    || (lastDisabledState != null && lastDisabledState != disabled);
            lastHiddenState = hidden;
            lastDisabledState = disabled;
            if (changed) markLayoutDirty();
            return changed;
        }

        @Override
        public boolean visible() {
            return super.visible() && !isHiddenByPredicate();
        }

        @Override
        public boolean needsFreshRender() {
            boolean changed = refreshConditionState();
            if (!super.visible()) return changed;
            for (Component child : children) {
                if (child.needsFreshRender()) changed = true;
            }
            return changed;
        }

        @Override
        public Component hitTest(float x, float y) {
            if (!visible() || !enabled()) return null;
            if (isDisabledByPredicate()) return bounds.contains(x, y) ? this : null;
            return super.hitTest(x, y);
        }

        @Override public boolean onMousePressed(MouseButtonEvent event) { return isDisabledByPredicate() && bounds.contains(event.x, event.y); }
        @Override public boolean onMouseReleased(MouseButtonEvent event) { return isDisabledByPredicate() && bounds.contains(event.x, event.y); }
        @Override public boolean onMouseScrolled(MouseScrollEvent event) { return false; }
    }

    private static final class MarkerRow extends ConditionalComponent {
        private final Component content;

        private MarkerRow(Component content) {
            if (content == null) throw new IllegalArgumentException("Marker row component cannot be null");
            this.content = content;
            add(content);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return content.measure(context, constraints);
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            content.layout(context, bounds);
        }
    }

    private static final class CustomOptionRow extends ConditionalComponent implements SearchAware {
        private final Component content;
        private final String searchText;
        private String searchQuery = "";
        private String searchSectionTitle = "";
        private static final float MIN_HEIGHT = 24;

        private CustomOptionRow(Component content, String searchText) {
            if (content == null) throw new IllegalArgumentException("Custom config option component cannot be null");
            this.content = content;
            this.searchText = searchText == null ? "" : searchText;
            this.minSize(0, MIN_HEIGHT);
            add(content);
        }

        @Override public void search(String query, String sectionTitle) { this.searchQuery = query == null ? "" : query; this.searchSectionTitle = sectionTitle == null ? "" : sectionTitle; markLayoutDirty(); }
        @Override public boolean searchMatches(String query, String sectionTitle) { return matchesSearch(query, sectionTitle); }
        @Override public boolean visible() { return super.visible() && matchesSearch(searchQuery, searchSectionTitle); }

        private boolean matchesSearch(String query, String sectionTitle) {
            if (query == null || query.isEmpty()) return true;
            return contains(searchText, query) || contains(tooltip, query) || contains(sectionTitle, query) || childrenMatch(content, query, sectionTitle);
        }

        private void syncEnabledState() {
            syncEnabledState(isDisabledByPredicate());
        }

        private void syncEnabledState(boolean disabled) {
            setEnabledDeep(content, !disabled);
        }

        private static void setEnabledDeep(Component component, boolean enabled) {
            component.enabled(enabled);
            if (!enabled) {
                component.setFocused(false);
                component.setHovered(false);
            }
            for (Component child : component.children()) setEnabledDeep(child, enabled);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            Size measured = content.measure(context, new Constraints(constraints.maxWidth(), constraints.maxHeight()));
            return constraints.clamp(new Size(constraints.maxWidth(), Math.max(MIN_HEIGHT, measured.height())));
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            syncEnabledState();
            Size measured = content.measure(context, new Constraints(bounds.w(), bounds.h()));
            float height = Math.max(0, measured.height());
            content.layout(context, new Rect(bounds.x(), bounds.y(), bounds.w(), height));
        }

        @Override
        public Component hitTest(float x, float y) {
            syncEnabledState();
            if (!visible() || !enabled()) return null;
            if (isDisabledByPredicate()) return bounds.contains(x, y) ? this : null;
            return super.hitTest(x, y);
        }

        @Override public boolean onMousePressed(MouseButtonEvent event) { return isDisabledByPredicate() && bounds.contains(event.x, event.y); }
        @Override public boolean onMouseReleased(MouseButtonEvent event) { return isDisabledByPredicate() && bounds.contains(event.x, event.y); }

        @Override
        public void render(RenderContext context) {
            if (!visible()) return;
            syncEnabledState();
            for (Component child : children) child.render(context);
            if (isDisabledByPredicate()) {
                context.rect(bounds, SdfRectStyle.create()
                        .fill(context.theme().palette().surface().withAlpha(132))
                        .border(1, context.theme().palette().border().withAlpha(115))
                        .radius(context.theme().radii().md())
                        .opacity(0.82f), z + 250);
            }
            if (context.debugBounds()) {
                context.rect(bounds, SdfRectStyle.create().fill(ColorRGBA.TRANSPARENT).border(1, ColorRGBA.rgba(255, 0, 180, 120)).radius(0), z + 5000);
            }
        }
    }

    private static final class OptionRow extends Row implements ConditionalEntry, SearchAware {
        private final String label;
        private final String description;
        private final float labelWidth;
        private final boolean forceBelowDescription;
        private BooleanSupplier hiddenWhen = () -> false;
        private BooleanSupplier disabledWhen = () -> false;
        private Boolean lastHiddenState;
        private Boolean lastDisabledState;
        private String searchQuery = "";
        private String searchSectionTitle = "";

        private static final float OUTER_X = 14;
        private static final float OUTER_Y = 12;
        private static final float CONTROL_RIGHT_PADDING = 22;
        private static final float CONTROL_GAP = 16;
        private static final float STACK_BREAKPOINT = 390;
        private static final float MIN_CONTROL_WIDTH = 72;

        private OptionRow(String label, String description, float labelWidth, boolean forceBelowDescription) {
            this.label = label == null ? "" : label;
            this.description = description == null ? "" : description;
            this.labelWidth = labelWidth;
            this.forceBelowDescription = forceBelowDescription;
        }

        @Override public void hiddenWhen(BooleanSupplier predicate) { this.hiddenWhen = predicate == null ? () -> false : predicate; lastHiddenState = null; markLayoutDirty(); }
        @Override public void disabledWhen(BooleanSupplier predicate) { this.disabledWhen = predicate == null ? () -> false : predicate; lastDisabledState = null; markLayoutDirty(); }
        @Override public void tooltipText(String text) { this.tooltip(text); for (Component child : children) child.tooltip(text); }
        @Override public void search(String query, String sectionTitle) { this.searchQuery = query == null ? "" : query; this.searchSectionTitle = sectionTitle == null ? "" : sectionTitle; markLayoutDirty(); }
        @Override public boolean searchMatches(String query, String sectionTitle) { return matchesSearch(query, sectionTitle); }

        private boolean hiddenByPredicate() { try { return hiddenWhen.getAsBoolean(); } catch (RuntimeException ignored) { return false; } }
        private boolean disabledByPredicate() { try { return disabledWhen.getAsBoolean(); } catch (RuntimeException ignored) { return false; } }

        private boolean refreshConditionState() {
            boolean hidden = hiddenByPredicate();
            boolean disabled = disabledByPredicate();
            boolean changed = (lastHiddenState != null && lastHiddenState != hidden)
                    || (lastDisabledState != null && lastDisabledState != disabled);
            lastHiddenState = hidden;
            lastDisabledState = disabled;
            if (changed) markLayoutDirty();
            return changed;
        }

        @Override
        public boolean visible() { return super.visible() && !hiddenByPredicate() && matchesSearch(searchQuery, searchSectionTitle); }

        private boolean matchesSearch(String query, String sectionTitle) {
            if (query == null || query.isEmpty()) return true;
            return contains(label, query) || contains(description, query) || contains(sectionTitle, query) || contains(tooltip, query);
        }

        @Override
        public boolean needsFreshRender() {
            boolean changed = refreshConditionState();
            if (!super.visible()) return changed;
            for (Component child : children) {
                if (child.needsFreshRender()) changed = true;
            }
            return changed;
        }

        @Override
        public Component hitTest(float x, float y) {
            syncEnabledState();
            if (!visible() || !enabled()) return null;
            if (disabledByPredicate()) return bounds.contains(x, y) ? this : null;
            return super.hitTest(x, y);
        }

        @Override public boolean onMousePressed(MouseButtonEvent event) { return disabledByPredicate() && bounds.contains(event.x, event.y); }
        @Override public boolean onMouseReleased(MouseButtonEvent event) { return disabledByPredicate() && bounds.contains(event.x, event.y); }
        @Override public boolean onMouseScrolled(MouseScrollEvent event) { return false; }

        @Override public boolean focusable() { return disabledByPredicate(); }

        private void syncEnabledState() {
            syncEnabledState(disabledByPredicate());
        }

        private void syncEnabledState(boolean disabled) {
            for (Component child : children) setEnabledDeep(child, !disabled);
        }

        private static void setEnabledDeep(Component component, boolean enabled) {
            component.enabled(enabled);
            if (!enabled) {
                component.setFocused(false);
                component.setHovered(false);
            }
            for (Component child : component.children()) setEnabledDeep(child, enabled);
        }

        private Component widgetChild() {
            for (int i = children.size() - 1; i >= 0; i--) {
                Component child = children.get(i);
                if (child.visible() && !(child instanceof Spacer)) return child;
            }
            return null;
        }

        private boolean stacked(float width) {
            return width < STACK_BREAKPOINT;
        }

        private boolean fixedSizeControl(Component widget) {
            return widget instanceof Checkbox || widget instanceof ToggleSwitch;
        }

        private boolean stacked(Component widget, float width) {
            return forceBelowDescription || (!fixedSizeControl(widget) && stacked(width));
        }

        private float controlRightPadding(Component widget, float rowWidth) {
            return stacked(widget, rowWidth) ? 0 : CONTROL_RIGHT_PADDING;
        }

        private float usableWidth(Component widget, float rowWidth) {
            return Math.max(1, rowWidth - OUTER_X * 2 - controlRightPadding(widget, rowWidth));
        }

        private float desiredControlWidth(LayoutContext context, Component widget, float maxWidth, float maxHeight) {
            if (widget == null) return 0;
            Size measured = widget.measure(context, new Constraints(Math.max(1, maxWidth), Math.max(1, maxHeight)));
            if (fixedSizeControl(widget)) return measured.width();
            return Math.max(MIN_CONTROL_WIDTH, Math.min(maxWidth, measured.width()));
        }

        private float controlWidth(LayoutContext context, Component widget, float rowWidth, float maxHeight) {
            float usable = usableWidth(widget, rowWidth);
            if (stacked(widget, rowWidth)) return Math.max(MIN_CONTROL_WIDTH, usable);

            float desired = desiredControlWidth(context, widget, usable, maxHeight);
            if (fixedSizeControl(widget)) return desired;
            float minText = Math.max(150, Math.min(labelWidth, rowWidth * 0.52f));
            float available = usable - CONTROL_GAP - minText;
            if (available < MIN_CONTROL_WIDTH) {
                available = Math.max(MIN_CONTROL_WIDTH, usable * 0.38f);
            }
            return Math.max(MIN_CONTROL_WIDTH, Math.min(desired, available));
        }

        private float textAreaWidth(LayoutContext context, Component widget, float rowWidth, float maxHeight) {
            float usable = usableWidth(widget, rowWidth);
            if (stacked(widget, rowWidth)) return usable;
            float cw = controlWidth(context, widget, rowWidth, maxHeight);
            return Math.max(40, usable - cw - CONTROL_GAP);
        }

        private float descriptionWidth(LayoutContext context, Component widget, float rowWidth, float maxHeight) {
            return Math.max(40, textAreaWidth(context, widget, rowWidth, maxHeight) * 0.90f);
        }

        private float textAreaWidth(RenderContext context, Component widget, float rowWidth, float maxHeight) {
            float usable = usableWidth(widget, rowWidth);
            if (stacked(widget, rowWidth)) return usable;
            float desired = widget == null ? 0 : Math.max(fixedSizeControl(widget) ? 0 : MIN_CONTROL_WIDTH, Math.min(usable, widget.bounds().w() > 0 ? widget.bounds().w() : widget.preferredWidth() > 0 ? widget.preferredWidth() : usable * 0.35f));
            return Math.max(40, usable - desired - CONTROL_GAP);
        }

        private float descriptionWidth(RenderContext context, Component widget, float rowWidth, float maxHeight) {
            return Math.max(40, textAreaWidth(context, widget, rowWidth, maxHeight) * 0.90f);
        }

        private float textBlockHeight(LayoutContext context, Component widget, float rowWidth, float maxHeight) {
            float labelFont = context.theme().fonts().normal();
            float labelHeight = context.backend().measureText("Ag", labelFont, medium(context)).height();
            if (description.isBlank()) return labelHeight;
            float descFont = context.theme().fonts().small();
            float lineHeight = Math.max(descFont + 4, context.backend().measureText("Ag", descFont, regular(context)).height() + 2);
            int lines = Math.max(1, wrapLines(context, description, descFont, descriptionWidth(context, widget, rowWidth, maxHeight)).size());
            return 21 + lines * lineHeight;
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            Component widget = widgetChild();
            float rowWidth = constraints.maxWidth();
            float availableHeight = Math.max(1, constraints.maxHeight());
            float cw = controlWidth(context, widget, rowWidth, availableHeight);
            Size widgetSize = widget == null ? Size.ZERO : widget.measure(context, new Constraints(cw, availableHeight));
            float textHeight = textBlockHeight(context, widget, rowWidth, availableHeight);
            float height;
            if (stacked(widget, rowWidth)) {
                height = OUTER_Y + textHeight + 10 + widgetSize.height() + OUTER_Y;
            } else {
                height = OUTER_Y + Math.max(textHeight, widgetSize.height()) + OUTER_Y;
            }
            return constraints.clamp(new Size(rowWidth, Math.max(description.isBlank() ? 44 : 58, height)));
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            boolean disabled = disabledByPredicate();
            syncEnabledState(disabled);
            Rect content = bounds.inset(padding.left(), padding.top(), padding.right(), padding.bottom());
            Component widget = widgetChild();
            if (widget == null) return;

            float cw = controlWidth(context, widget, content.w(), content.h());
            Size measured = widget.measure(context, new Constraints(cw, Math.max(1, content.h())));
            float widgetHeight = measured.height();

            for (Component child : children) {
                if (child instanceof Spacer) {
                    child.layout(context, Rect.ZERO);
                }
            }

            if (stacked(widget, content.w())) {
                float x = content.x() + OUTER_X;
                float y = content.bottom() - OUTER_Y - widgetHeight;
                widget.layout(context, new Rect(x, y, Math.max(MIN_CONTROL_WIDTH, usableWidth(widget, content.w())), widgetHeight));
            } else {
                float layoutW = fixedSizeControl(widget) ? measured.width() : cw;
                float x = content.right() - controlRightPadding(widget, content.w()) - layoutW;
                float y = content.centerY() - widgetHeight * 0.5f;
                widget.layout(context, new Rect(x, y, layoutW, widgetHeight));
            }
        }

        @Override
        protected void renderSelf(RenderContext context) {
            boolean disabled = disabledByPredicate();
            syncEnabledState(disabled);
            Component widget = widgetChild();
            ColorRGBA fill = context.theme().palette().surfaceAlt().withAlpha(disabled ? 52 : 88);
            ColorRGBA labelColor = disabled ? context.theme().palette().mutedText().withAlpha(150) : context.theme().palette().text();
            ColorRGBA descColor = context.theme().palette().mutedText().withAlpha(disabled ? 115 : 255);
            context.rect(bounds, SdfRectStyle.create().fill(fill).border(0, ColorRGBA.TRANSPARENT).radius(context.theme().radii().lg() + 2), z - 1);

            float labelFont = context.theme().fonts().normal();
            float descFont = context.theme().fonts().small();
            String labelFace = medium(context);
            String descFace = regular(context);

            float labelHeight = context.lineHeight(labelFont, labelFace);
            float x = bounds.x() + OUTER_X;
            float y = stacked(widget, bounds.w())
                    ? bounds.y() + OUTER_Y
                    : description.isBlank()
                    ? bounds.centerY() - labelHeight * 0.5f
                    : bounds.y() + OUTER_Y;
            context.text(label, x, y, labelFont, labelFace, labelColor, z + 1);
            if (!description.isBlank()) {
                float lineHeight = Math.max(descFont + 4, context.lineHeight(descFont, descFace) + 2);
                List<String> lines = wrapLines(context, description, descFont, descriptionWidth(context, widget, bounds.w(), bounds.h()));
                float descY = bounds.y() + OUTER_Y + 21;
                for (String line : lines) {
                    context.text(line, x, descY, descFont, descFace, descColor, z + 1);
                    descY += lineHeight;
                }
            }
        }

        @Override
        public void render(RenderContext context) {
            if (!visible()) return;
            syncEnabledState();
            renderSelf(context);
            for (Component child : children) {
                child.render(context);
            }
            if (disabledByPredicate()) {
                Component widget = widgetChild();
                if (widget != null) {
                    Rect widgetBounds = widget.bounds();
                    context.rect(widgetBounds, SdfRectStyle.create()
                            .fill(context.theme().palette().surface().withAlpha(150))
                            .border(1, context.theme().palette().border().withAlpha(130))
                            .radius(context.theme().radii().md())
                            .opacity(0.86f), z + 250);
                }
            }
            if (context.debugBounds()) {
                context.rect(bounds, SdfRectStyle.create().fill(ColorRGBA.TRANSPARENT).border(1, ColorRGBA.rgba(255, 0, 180, 120)).radius(0), z + 5000);
            }
        }
    }

    private static final class InfoRow extends ConditionalComponent implements SearchAware {
        private final String fallbackTitle;
        private final String fallbackDescription;
        private final Supplier<String> titleSupplier;
        private final Supplier<String> descriptionSupplier;
        private final Supplier<String> tooltipSupplier;
        private String title;
        private String description;
        private String dynamicTooltip = "";
        private String searchQuery = "";
        private String searchSectionTitle = "";
        private static final float PAD_X = 14;
        private static final float PAD_Y = 12;

        private InfoRow(String title, String description) {
            this(title, description, null, null, null);
        }

        private InfoRow(String title, String description, Supplier<String> titleSupplier, Supplier<String> descriptionSupplier, Supplier<String> tooltipSupplier) {
            this.fallbackTitle = title == null ? "" : title;
            this.fallbackDescription = description == null ? "" : description;
            this.titleSupplier = titleSupplier;
            this.descriptionSupplier = descriptionSupplier;
            this.tooltipSupplier = tooltipSupplier;
            this.title = this.fallbackTitle;
            this.description = this.fallbackDescription;
            syncDynamicText(false);
            this.minSize(0, this.description.isBlank() ? 42 : 62);
        }

        private String supplied(Supplier<String> supplier, String fallback) {
            if (supplier == null) return fallback;
            try {
                String value = supplier.get();
                return value == null ? "" : value;
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }

        private boolean syncDynamicText(boolean markDirty) {
            String nextTitle = supplied(titleSupplier, fallbackTitle);
            String nextDescription = supplied(descriptionSupplier, fallbackDescription);
            String nextTooltip = supplied(tooltipSupplier, tooltip);
            boolean layoutChanged = !Objects.equals(title, nextTitle) || !Objects.equals(description, nextDescription);
            boolean tooltipChanged = !Objects.equals(dynamicTooltip, nextTooltip);
            if (layoutChanged) {
                title = nextTitle;
                description = nextDescription;
                minSize(0, description.isBlank() ? 42 : 62);
                if (markDirty) markLayoutDirty();
            }
            if (tooltipChanged) dynamicTooltip = nextTooltip;
            return layoutChanged || tooltipChanged;
        }

        private float textWidth(float width) {
            return Math.max(40, width - PAD_X * 2);
        }

        @Override public void search(String query, String sectionTitle) { this.searchQuery = query == null ? "" : query; this.searchSectionTitle = sectionTitle == null ? "" : sectionTitle; markLayoutDirty(); }
        @Override public boolean searchMatches(String query, String sectionTitle) { syncDynamicText(false); return matchesSearch(query, sectionTitle); }
        @Override public boolean visible() { syncDynamicText(false); return super.visible() && matchesSearch(searchQuery, searchSectionTitle); }
        @Override public String tooltip() { syncDynamicText(false); return dynamicTooltip == null || dynamicTooltip.isBlank() ? super.tooltip() : dynamicTooltip; }

        @Override
        public boolean needsFreshRender() {
            boolean changed = syncDynamicText(true);
            return super.needsFreshRender() || changed;
        }

        private boolean matchesSearch(String query, String sectionTitle) {
            if (query == null || query.isEmpty()) return true;
            return contains(title, query) || contains(description, query) || contains(sectionTitle, query) || contains(tooltip(), query);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            syncDynamicText(true);
            float titleFont = context.theme().fonts().normal();
            float titleH = context.backend().measureText("Ag", titleFont, medium(context)).height();
            if (description.isBlank()) return constraints.clamp(new Size(constraints.maxWidth(), Math.max(42, PAD_Y * 2 + titleH)));
            float descFont = context.theme().fonts().small();
            float lineHeight = Math.max(descFont + 4, context.backend().measureText("Ag", descFont, regular(context)).height() + 2);
            int lines = Math.max(1, wrapLines(context, description, descFont, textWidth(constraints.maxWidth())).size());
            float height = PAD_Y + titleH + 8 + lines * lineHeight + PAD_Y;
            return constraints.clamp(new Size(constraints.maxWidth(), Math.max(62, height)));
        }

        @Override
        protected void renderSelf(RenderContext context) {
            syncDynamicText(true);
            boolean disabled = isDisabledByPredicate();
            context.rect(bounds, SdfRectStyle.create()
                    .fill(context.theme().palette().accent().withAlpha(disabled ? 16 : 28))
                    .border(1, context.theme().palette().accent().withAlpha(disabled ? 38 : 70))
                    .radius(context.theme().radii().lg() + 2), z - 1);
            float titleFont = context.theme().fonts().normal();
            float descFont = context.theme().fonts().small();
            ColorRGBA titleColor = disabled ? context.theme().palette().mutedText().withAlpha(145) : context.theme().palette().text();
            ColorRGBA descColor = context.theme().palette().mutedText().withAlpha(disabled ? 115 : 255);
            String titleFace = medium(context);
            String descFace = regular(context);

            float titleH = context.lineHeight(titleFont, titleFace);
            float titleY = description.isBlank() ? bounds.centerY() - titleH * 0.5f : bounds.y() + PAD_Y;
            context.text(title, bounds.x() + PAD_X, titleY, titleFont, titleFace, titleColor, z + 1);
            if (!description.isBlank()) {
                float lineHeight = Math.max(descFont + 4, context.lineHeight(descFont, descFace) + 2);
                float y = titleY + titleH + 8;
                for (String line : wrapLines(context, description, descFont, textWidth(bounds.w()))) {
                    context.text(line, bounds.x() + PAD_X, y, descFont, descFace, descColor, z + 1);
                    y += lineHeight;
                }
            }
        }
    }

    private static final class Accordion extends ConditionalComponent implements SearchAware {
        private final String title;
        private final String description;
        private final Component content;
        private boolean open = false;
        private String searchQuery = "";
        private String searchSectionTitle = "";
        private final AnimatedFloat expansion = new AnimatedFloat(0).speed(16);
        private float currentHeaderHeight = COMPACT_HEADER_HEIGHT;
        private float measuredContentHeight = 0;
        private static final float COMPACT_HEADER_HEIGHT = 42;
        private static final float DESCRIBED_DESC_TOP = 27;
        private static final float CONTENT_TOP_PADDING = 10;
        private static final float CONTENT_BOTTOM_PADDING = 12;

        private Accordion(String title, Component content) {
            this(title, "", content);
        }

        private Accordion(String title, String description, Component content) {
            this.title = title == null ? "" : title;
            this.description = description == null ? "" : description;
            this.content = content;
            this.focusable = true;
            syncContentVisibility();
            add(content);
        }

        @Override public void search(String query, String sectionTitle) { this.searchQuery = query == null ? "" : query; this.searchSectionTitle = sectionTitle == null ? "" : sectionTitle; markLayoutDirty(); }
        @Override public boolean searchMatches(String query, String sectionTitle) {
            if (query == null || query.isEmpty()) return true;
            if (contains(title, query) || contains(description, query) || contains(sectionTitle, query) || contains(tooltip, query)) return true;
            for (Component child : content.children()) {
                if (childrenMatch(child, query, sectionTitle)) return true;
            }
            return false;
        }
        @Override public boolean visible() { return super.visible() && searchMatches(searchQuery, searchSectionTitle); }

        private float descriptionWidth(float width) {
            return Math.max(40, width - 48);
        }

        private float computeHeaderHeight(LayoutContext context, float width) {
            if (description.isBlank()) return COMPACT_HEADER_HEIGHT;
            float descFont = context.theme().fonts().small();
            float lineHeight = Math.max(descFont + 4, context.backend().measureText("Ag", descFont, regular(context)).height() + 2);
            int lines = Math.max(1, wrapLines(context, description, descFont, descriptionWidth(width)).size());
            return Math.max(52, DESCRIBED_DESC_TOP + lines * lineHeight + 8);
        }

        private void syncContentVisibility() {
            content.visible((open || expansion.value() > 0.001f) && !isHiddenByPredicate());
        }

        @Override
        public void tick(float deltaSeconds) {
            boolean searching = searchQuery != null && !searchQuery.isEmpty();
            expansion.target((open || searching) && !isHiddenByPredicate() ? 1.0f : 0.0f);
            float before = expansion.value();
            expansion.update(deltaSeconds);
            syncContentVisibility();
            if (Math.abs(before - expansion.value()) > 0.0005f) {
                markLayoutDirty();
            }
            super.tick(deltaSeconds);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            syncContentVisibility();
            currentHeaderHeight = computeHeaderHeight(context, constraints.maxWidth());
            Size child = content.measure(context, new Constraints(Math.max(0, constraints.maxWidth() - 24), constraints.maxHeight()));
            measuredContentHeight = child.height();
            float t = expansion.value();
            float extra = t <= 0.001f ? 0 : (CONTENT_TOP_PADDING + measuredContentHeight + CONTENT_BOTTOM_PADDING) * t;
            return constraints.clamp(new Size(constraints.maxWidth(), currentHeaderHeight + extra));
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            syncContentVisibility();
            currentHeaderHeight = computeHeaderHeight(context, bounds.w());
            Size measured = content.measure(context, new Constraints(Math.max(0, bounds.w() - 24), Math.max(0, bounds.h())));
            measuredContentHeight = measured.height();
            float t = expansion.value();
            if (t <= 0.001f) {
                content.layout(context, Rect.ZERO);
                return;
            }
            Rect inner = bounds.inset(12, currentHeaderHeight + CONTENT_TOP_PADDING, 12, CONTENT_BOTTOM_PADDING);
            content.layout(context, new Rect(inner.x(), inner.y(), inner.w(), measuredContentHeight));
        }

        @Override
        protected void renderSelf(RenderContext context) {
            boolean disabled = isDisabledByPredicate();
            context.rect(bounds, SdfRectStyle.create()
                    .fill(context.theme().palette().surfaceAlt().withAlpha(disabled ? 54 : 86))
                    .border(0, ColorRGBA.TRANSPARENT)
                    .radius(context.theme().radii().lg() + 2), z - 2);
            syncContentVisibility();
            float headerHeight = currentHeaderHeight <= 0 ? COMPACT_HEADER_HEIGHT : currentHeaderHeight;
            Rect header = new Rect(bounds.x(), bounds.y(), bounds.w(), headerHeight);
            context.rect(header, SdfRectStyle.create()
                    .fill(context.theme().palette().surfaceActive().withAlpha(disabled ? 55 : 95))
                    .border(1, context.theme().palette().border().withAlpha(disabled ? 70 : 130))
                    .radius(context.theme().radii().lg() + 2), z - 1);
            float font = context.theme().fonts().normal();
            String titleFace = medium(context);
            String descFace = regular(context);
            float fontHeight = context.lineHeight(font, titleFace);
            ColorRGBA color = disabled ? context.theme().palette().mutedText().withAlpha(145) : context.theme().palette().text();
            context.text(expansion.value() > 0.5f ? "▼" : "▶", header.x() + 12, header.centerY() - fontHeight * 0.5f, font, regular(context), context.theme().palette().mutedText(), z + 1);
            context.text(
                    title,
                    header.x() + 32,
                    description.isBlank() ? header.centerY() - fontHeight * 0.5f : header.y() + 8,
                    font,
                    titleFace,
                    color,
                    z + 1
            );
            if (!description.isBlank()) {
                float descFont = context.theme().fonts().small();
                float lineHeight = Math.max(descFont + 4, context.lineHeight(descFont, descFace) + 2);
                List<String> lines = wrapLines(context, description, descFont, descriptionWidth(header.w()));
                float y = header.y() + DESCRIBED_DESC_TOP;
                for (String line : lines) {
                    context.text(line, header.x() + 32, y, descFont, descFace, context.theme().palette().mutedText(), z + 1);
                    y += lineHeight;
                }
            }
        }

        @Override
        public void render(RenderContext context, Rect clip) {
            if (!visible()) return;
            if (clip != null && !bounds.intersects(clip)) return;

            renderSelf(context);

            float t = expansion.value();
            if (t > 0.001f && content.visible()) {
                Rect contentClip = new Rect(
                        bounds.x(),
                        bounds.y() + currentHeaderHeight,
                        bounds.w(),
                        Math.max(0, bounds.h() - currentHeaderHeight)
                );
                if (clip != null) {
                    contentClip = contentClip.intersect(clip);
                }
                if (contentClip.w() > 0 && contentClip.h() > 0) {
                    context.pushClip(contentClip);
                    content.render(context, contentClip);
                    context.popClip();
                }
            }

            if (context.debugBounds()) {
                context.rect(bounds, SdfRectStyle.create()
                        .fill(ColorRGBA.TRANSPARENT)
                        .border(1, ColorRGBA.rgba(255, 0, 180, 120))
                        .radius(0), z + 5000);
            }
        }

        @Override
        public boolean needsFreshRender() {
            return super.needsFreshRender();
        }

        @Override
        public Component hitTest(float x, float y) {
            if (!visible() || !enabled()) return null;
            if (isDisabledByPredicate()) return bounds.contains(x, y) ? this : null;
            syncContentVisibility();
            Rect header = new Rect(bounds.x(), bounds.y(), bounds.w(), currentHeaderHeight);
            if (header.contains(x, y)) return this;
            if (expansion.value() > 0.001f && content.visible()) {
                Component hit = content.hitTest(x, y);
                if (hit != null) return hit;
            }
            return bounds.contains(x, y) ? this : null;
        }

        @Override
        public boolean onMousePressed(MouseButtonEvent event) {
            if (isDisabledByPredicate()) return bounds.contains(event.x, event.y);
            if (event.button != MouseButton.LEFT) return false;
            syncContentVisibility();
            Rect header = new Rect(bounds.x(), bounds.y(), bounds.w(), currentHeaderHeight);
            if (header.contains(event.x, event.y)) {
                pressed = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent event) {
            if (isDisabledByPredicate()) return bounds.contains(event.x, event.y);
            boolean was = pressed;
            pressed = false;
            syncContentVisibility();
            Rect header = new Rect(bounds.x(), bounds.y(), bounds.w(), currentHeaderHeight);
            if (was && header.contains(event.x, event.y)) {
                open = !open;
                syncContentVisibility();
                markLayoutDirty();
                return true;
            }
            return was;
        }
    }
}
