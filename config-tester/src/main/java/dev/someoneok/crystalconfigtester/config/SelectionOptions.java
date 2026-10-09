package dev.someoneok.crystalconfigtester.config;

import dev.someoneok.crystalconfig.autoconfig.*;
import dev.someoneok.crystalconfig.state.MutableState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.someoneok.crystalconfigtester.config.TestChoices.Category;
import static dev.someoneok.crystalconfigtester.config.TestChoices.Mode;

@ConfigCategory(main = "01 / Controls", sub = "Dropdowns and ordering")
public final class SelectionOptions {
    private SelectionOptions() { }

    @ConfigInfo(title = "Every selection variant", description = "Test search, grouped sections, selection, removal, drag reordering, and saving.")
    public static final ConfigMarker info = ConfigMarker.marker();

    @ConfigDropdown(key = "select.standard", label = "Dropdown")
    public static final MutableState<Mode> dropdown = new MutableState<>(Mode.SIMPLE);

    @ConfigSearchableDropdown(key = "select.searchable", label = "Searchable dropdown")
    public static final MutableState<Mode> searchable = new MutableState<>(Mode.DEBUG);

    public static final Map<Category, List<Mode>> GROUPS = groupedOptions();

    private static Map<Category, List<Mode>> groupedOptions() {
        Map<Category, List<Mode>> options = new LinkedHashMap<>();
        options.put(Category.GENERAL, List.of(Mode.OFF, Mode.SIMPLE));
        options.put(Category.ADVANCED, List.of(Mode.FANCY, Mode.DEBUG, Mode.BENCHMARK));
        return options;
    }

    @ConfigGroupedDropdown(key = "select.grouped", label = "Grouped dropdown", options = "GROUPS")
    public static final MutableState<Mode> grouped = new MutableState<>(Mode.FANCY);

    @ConfigSearchableGroupedDropdown(key = "select.searchableGrouped", label = "Searchable grouped dropdown", options = "GROUPS")
    public static final MutableState<Mode> searchableGrouped = new MutableState<>(Mode.BENCHMARK);

    @ConfigMultiSelectDropdown(key = "select.multi", label = "Multi-select dropdown")
    public static final MutableState<List<Mode>> multiSelect = new MutableState<>(List.of(Mode.SIMPLE, Mode.DEBUG));

    @ConfigSearchableMultiSelectDropdown(key = "select.searchableMulti", label = "Searchable multi-select")
    public static final MutableState<List<Mode>> searchableMulti = new MutableState<>(List.of(Mode.FANCY));

    @ConfigDraggableList(key = "select.drag", label = "Draggable enum list", description = "Remove, add, and reorder", allowEmpty = false)
    public static final MutableState<List<Mode>> draggable = new MutableState<>(List.of(Mode.SIMPLE, Mode.DEBUG, Mode.FANCY));

    @ConfigDraggableList(key = "select.reorderOnly", label = "Reorder-only list", allowDeleting = false)
    public static final MutableState<List<Mode>> reorderOnly = new MutableState<>(List.of(Mode.OFF, Mode.SIMPLE, Mode.FANCY));
}
