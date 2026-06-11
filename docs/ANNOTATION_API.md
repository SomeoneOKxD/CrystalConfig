# Annotation API

`AutoConfig` builds config screens from static `State<T>` fields. Option fields are also registered with `GsonConfigStore`, so UI, runtime code, and persistence share the same value.

## Basic setup

```java
@ConfigCategory(main = "Gameplay", sub = "General")
public final class GameplayOptions {
    @ConfigToggle(key = "enabled", label = "Enabled", description = "Master switch.")
    public static final MutableState<Boolean> enabled = new MutableState<>(true);

    @ConfigNumber(key = "maxMessages", label = "Max messages", min = 30, max = 120, step = 1)
    public static final MutableState<Double> maxMessages = new MutableState<>(60.0d);
}
```

```java
AutoConfig.Model model = AutoConfig.of(GameplayOptions.class)
        .configureSettings(settings -> settings
                .defaultTheme(ThemePresets.darkCrimson())
                .defaultScale(1.0d));

GsonConfigStore store = GsonConfigStore.builder(configPath).build();
model.register(store);
store.loadBlocking();

UiRoot root = model.root("My Config", initialSearch == null ? "" : initialSearch);
```

## Categories

`@ConfigCategory` can be placed on a config class or on fields in a layout class.

```java
@ConfigCategory(main = "Render", sub = "HUD")
public final class HudConfig {
}
```

`main` is the collapsible sidebar group. `sub` is the entry inside that group.

Category conditions reference a member on the same class:

```java
@ConfigCategory(main = "Render", sub = "Debug", hiddenWhen = "hideDebug")
public final class DebugConfig {
    public static final MutableState<Boolean> hideDebug = new MutableState<>(false);
}
```

Supported condition members are static booleans, `State<Boolean>`, `BooleanSupplier`, or no-argument static methods returning boolean.

## Option annotations

Use one option annotation per field.

| Annotation | Field value type | Widget |
|---|---|---|
| `@ConfigToggle` | `Boolean` | Toggle switch |
| `@ConfigCheckbox` | `Boolean` | Checkbox |
| `@ConfigSlider` | `Double` | Slider |
| `@ConfigSliderLabel` | `Double` | Slider with value label |
| `@ConfigNumber` | `Double` | Number input |
| `@ConfigText` | `String` | Text input with optional regex filtering |
| `@ConfigColor` | `ColorRGBA` | Color picker |
| `@ConfigKeybind` | `Keybind` | Keybind selector |
| `@ConfigDropdown` | `Enum` | Dropdown |
| `@ConfigSearchableDropdown` | `Enum` | Searchable dropdown |
| `@ConfigGroupedDropdown` | `T` with `options` member returning `Map<G extends Enum<G>, ? extends Collection<T>>` | Grouped dropdown |
| `@ConfigSearchableGroupedDropdown` | `T` with `options` member returning `Map<G extends Enum<G>, ? extends Collection<T>>` | Searchable grouped dropdown |
| `@ConfigMultiSelectDropdown` | `List<T extends Enum<T>>` | Dropdown with independently enabled/disabled enum rows |
| `@ConfigSearchableMultiSelectDropdown` | `List<T extends Enum<T>>` | Searchable multi-select enum dropdown |
| `@ConfigDraggableList` | `List<T extends Enum<T>>` | Reorderable enum list |
| `@ConfigCustomList` | `List<T>` | Custom add/remove list |
| `@ConfigCustom` | `Component` | Custom widget inside normal label/description row |
| `@ConfigCustomOption` | `Component` or `Supplier<Component>` | Fully custom full-width row/card |

Option fields may be declared as `State<T>`, `MutableState<T>`, or `ConfigValue<T>`.

Most option annotations include:

| Member | Purpose |
|---|---|
| `key` | JSON key. Defaults to the category/accordion/field path. |
| `label` | Visible row title. |
| `description` | Optional text under the row title. |

## Fully custom options

Use `@ConfigCustomOption` when the normal label/description row is too restrictive and you want to own the entire row/card. The annotated static field can be a `Component` or a `Supplier<Component>`. A supplier is recommended when the screen may be rebuilt, because it creates a fresh component tree each time.

The custom component is laid out, rendered, ticked, focused, and receives mouse/keyboard events like any built-in option. It is not automatically registered with `GsonConfigStore`; persistence is optional and should be handled through any `State` / `ConfigValue` objects the custom component uses.

```java
@ConfigCustomOption(searchText = "cache reset reload")
public static final Supplier<Component> cacheTools = () -> new Row()
        .gap(10)
        .fillX()
        .add(new Label("Cache tools").flex(1))
        .add(new Button("Reset").onClick(Cache::reset).width(90));
```

Use existing conditions by making the field value implement `StateConditions`, or call builder `.hidden(...)` / `.disabled(...)` immediately after adding a custom option manually.

For manual screens, use:

```java
section.customOption(new MyCustomConfigRow(), "search aliases here");
// or
section.component(new MyCustomConfigRow());
```

`@ConfigCustom` still exists for simpler custom widgets that should be placed inside the standard option row with a label and description.

## Number and slider ranges

```java
@ConfigNumber(key = "opacity", label = "Opacity", min = 0, max = 1, step = 0.05)
public static final MutableState<Double> opacity = new MutableState<>(0.5d);

@ConfigSlider(key = "scale", label = "Scale", min = 0.5, max = 2.0, step = 0.05)
public static final MutableState<Double> scale = new MutableState<>(1.0d);
```

## Text input regex filtering

`@ConfigText(regex = "...")` uses a precompiled full-value regex for text input validation and insertion filtering. While editing, intermediate values are allowed when they can still become a valid match, so anchored full-value patterns such as UUID regexes remain typeable. Impossible inserted characters are rejected, deletions are always allowed so users can recover, and the input border becomes invalid until the current value fully matches the regex.

```java
@ConfigText(
        key = "profileName",
        label = "Profile name",
        description = "Letters, numbers, underscores and dashes only.",
        regex = "[A-Za-z0-9_-]*"
)
public static final MutableState<String> profileName = new MutableState<>("default");

@ConfigText(
        key = "uuid",
        label = "UUID",
        description = "Type or paste a UUID v4.",
        regex = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
)
public static final MutableState<String> uuid = new MutableState<>("");
```

## Keybind none handling

`@ConfigKeybind(disallowNone = true)` prevents Escape, Backspace, and Delete from changing the binding to `None`. The default is `false`, preserving the existing behavior where those keys clear the binding.

```java
@ConfigKeybind(
        key = "openMenu",
        label = "Open menu",
        description = "Must always have a real keybind.",
        disallowNone = true
)
public static final MutableState<Keybind> openMenu = new MutableState<>(Keybind.none());
```

## Dropdowns

Dropdowns use enum values. Override `toString()` for display text.

Grouped dropdowns are available through both AutoConfig annotations and the manual `ConfigScreenBuilder` API. For AutoConfig, use `@ConfigGroupedDropdown(options = "...")` or `@ConfigSearchableGroupedDropdown(options = "...")`. The `options` value must name a static field or no-arg static method on the same config class that returns `Map<G, ? extends Collection<T>>`, where `G extends Enum<G>`. Group keys render as non-interactive divider rows; values below each group are selectable. Complex collection values need a registered Gson/type adapter or serializer for persistence.

```java
enum ProfileGroup { COMBAT, ECONOMY }

public static final Map<ProfileGroup, List<Profile>> PROFILE_OPTIONS = new LinkedHashMap<>();
static {
    PROFILE_OPTIONS.put(ProfileGroup.COMBAT, List.of(Profile.PVP, Profile.DUNGEONS));
    PROFILE_OPTIONS.put(ProfileGroup.ECONOMY, List.of(Profile.MINING, Profile.FARMING));
}

@ConfigSearchableGroupedDropdown(key = "profile", label = "Profile", options = "PROFILE_OPTIONS")
public static final MutableState<Profile> profile = new MutableState<>(Profile.DEFAULT);

enum Profile {
    DEFAULT("Default"),
    PVP("PvP"),
    MINING("Mining");

    private final String label;

    Profile(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
```

## Multi-select enum dropdowns

Multi-select dropdowns store the active enum values in a `List<T>`. Selected rows stay in the enum declaration order and display a checkbox checkmark without shifting the row text.

```java
@ConfigSearchableMultiSelectDropdown(key = "enabledModules", label = "Enabled modules")
public static final MutableState<List<Module>> enabledModules = new MutableState<>(List.of(Module.PVP, Module.MINING));

enum Module {
    PVP("PvP"),
    MINING("Mining"),
    FARMING("Farming");

    private final String label;

    Module(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
```

Use `@ConfigMultiSelectDropdown` when search is not needed.

## Draggable enum lists

Use `@ConfigDraggableList` for ordered enum subsets.

```java
@ConfigDraggableList(
        key = "profit.lines",
        label = "Profit tracker lines",
        description = "Controls which tracker lines are shown.",
        allowEmpty = false
)
public static final MutableState<List<ProfitLine>> profitLines = new MutableState<>(List.of(
        ProfitLine.CHESTS,
        ProfitLine.RUNS,
        ProfitLine.PROFIT
));
```

`allowEmpty = false` keeps at least one row. `allowDeleting = false` makes the list reorder-only. The widget is compact while unfocused and expands for editing when clicked/focused, so long ordered lists do not permanently consume vertical space.

## Custom object lists

Use `@ConfigCustomList` when each list entry needs custom row content.

```java
@ConfigCustomList(
        key = "replacements",
        label = "Replacements",
        description = "Custom replacement rules.",
        entryFactory = ReplacementDefaults.class,
        rowFactory = ReplacementRowFactory.class
)
public static final MutableState<List<ReplacementRule>> replacements = new MutableState<>(List.of(
        new ReplacementRule("Player", "Allay", 2.0d)
));
```

Factories must have no-argument constructors. See [Custom List Option](CUSTOM_LIST_OPTION.md) for a full row-factory example.

## Marker rows

Marker rows are UI-only and are not persisted.

| Annotation | Purpose |
|---|---|
| `@ConfigInfo` | Text block. |
| `@ConfigSeparator` | Horizontal separator. |
| `@ConfigLabeledSeparator` | Separator with centered label. |
| `@ConfigSpacer` | Vertical gap. |
| `@ConfigButton` | Action button. Field must be a static `Runnable`. Supports `hiddenWhen` and `disabledWhen` member-name predicates. |
| `@ConfigAccordion` | Groups rows from a static config holder class referenced by a `Class<?>` field. |
| `@ConfigFooterButton` | Single sidebar footer button. Field must be a static `Runnable`. |
| `@ConfigFooterIcon` | Compact sidebar footer icon. |
| `@ConfigTooltip` | Tooltip for a field or category. |

Button visibility/disabled predicates can point at a static `BooleanSupplier`, static boolean field, or static no-arg boolean method on the same holder class:

```java
private static final BooleanSupplier advancedLocked = () -> !advancedEnabled.get();

@ConfigButton(
        label = "Reset advanced cache",
        buttonText = "Reset",
        description = "Clears advanced cache state.",
        disabledWhen = "advancedLocked"
)
public static final Runnable resetAdvancedCache = () -> {
    // action
};
```

Example footer icon:

```java
@ConfigFooterIcon(
        icon = MediaBrandIcons.DISCORD,
        action = ConfigFooterIconAction.OPEN_URL,
        value = "https://discord.gg/example",
        tooltip = "Discord"
)
public static final ConfigMarker discord = ConfigMarker.marker();
```

Use a static `Runnable` field when `action = RUNNABLE`.

## Accordions

Accordions are static `Class<?>` fields inside a category. The field order controls the accordion order.

```java
@ConfigCategory(main = "General", sub = "Tweaks")
public final class TweakConfig {
    @ConfigAccordion("Advanced")
    public static final Class<Advanced> advanced = Advanced.class;

    public static final class Advanced {
        @ConfigToggle(label = "Unsafe mode")
        public static final MutableState<Boolean> unsafeMode = new MutableState<>(false);
    }
}
```

Nested accordions are rejected.

## Layout classes

Use layout classes to control class order and category placement without relying on package scanning order.

```java
public final class ConfigLayout {
    @ConfigCategory(main = "General", sub = "Gameplay")
    public static final Class<?> gameplay = GameplayOptions.class;

    @ConfigCategory(main = "General", sub = "Audio")
    public static final Class<?> audio = AudioOptions.class;
}

AutoConfig.Model model = AutoConfig.layout(ConfigLayout.class);
```

A layout field may contain `Class<?>`, `Class<?>[]`, or `Collection<Class<?>>`.

## Package scanning

```java
AutoConfig.Model model = AutoConfig.scanPackage("com.example.mymod.config");
```

Anchor scanning avoids hard-coded package names:

```java
AutoConfig.Model model = AutoConfig.scanPackageOf(MyModConfig.class);
```

ClassGraph is optional. If it is present at runtime, `AutoConfig` uses it as a faster scanner. Otherwise it falls back to its built-in classpath scanner.

## Custom annotation-backed components

```java
AutoConfig.registerComponent(ConfigSound.class, SoundSetting.class, context ->
        new SoundSettingPicker(context.state()).allowNone(context.annotation().allowNone())
);
```

The custom annotation must define `label()`, `description()`, and `key()`. The context exposes the owner class, field, annotation, resolved key, label, description, state, generic value type, and raw value class.

### Conditional buttons with lambdas

For `@ConfigButton`, annotations can still use `hiddenWhen = "memberName"` and `disabledWhen = "memberName"`, but lambda-style autoconfig is also supported by using `ConditionalRunnable` as the annotated field value:

```java
import state.someoneok.crystalconfig.ConditionalRunnable;

@ConfigButton(label = "Reset Cache", buttonText = "Reset")
public static final ConditionalRunnable RESET_CACHE = ConditionalRunnable.of(MyConfig::resetCache)
        .hiddenWhen(() -> !advancedMode.get())
        .disabledWhen(() -> reloading.get());
```

`ConditionalRunnable` implements both `Runnable` and `StateConditions`, so autoconfig treats it as the button action and applies its `hiddenWhen` / `disabledWhen` predicates to the generated row.
