---
layout: default
title: Widgets
description: CrystalConfig widget, annotation, and builder reference.
---

# Widgets

CrystalConfig widgets bind to `State<T>` values. In AutoConfig, fields may be `State<T>`, `MutableState<T>`, or `ConfigValue<T>`. In manual screens, pass the state object directly to `ConfigScreenBuilder` methods.

## State types

| Type | Purpose |
|---|---|
| `State<T>` | Common read/write interface used by widgets. |
| `MutableState<T>` | In-memory value with change listeners. |
| `Binding<T>` | Getter/setter wrapper around an existing config object. |
| `ConfigValue<T>` | State plus path, label, and description metadata. |
| `ConditionalState<T>` | State with hidden/disabled predicates attached. |

Example:

```java
MutableState<Boolean> enabled = new MutableState<>(true);

State<Boolean> existing = Binding.of(
        () -> ExistingConfig.enabled,
        value -> ExistingConfig.enabled = value
);

ConditionalState<Double> scale = ConditionalState
        .mutable(1.0d)
        .disabledWhen(() -> !enabled.get());
```

## Widget map

| Widget | AutoConfig annotation | Manual builder method | State type |
|---|---|---|---|
| Toggle switch | `@ConfigToggle` | `toggle(...)` | `Boolean` |
| Checkbox | `@ConfigCheckbox` | `checkbox(...)` | `Boolean` |
| Slider | `@ConfigSlider` | `slider(...)` | `Number` |
| Slider with value label | `@ConfigSliderLabel` | `sliderLabel(...)` | `Number` |
| Number input | `@ConfigNumber` | `number(...)` | `Number` |
| Text input | `@ConfigText` | `text(...)` | `String` |
| Color picker | `@ConfigColor` | `color(...)` | `ColorRGBA` |
| Keybind selector | `@ConfigKeybind` | `keybind(...)` | `Keybind` |
| Enum dropdown | `@ConfigDropdown` | `dropdown(...)` | enum |
| Searchable enum dropdown | `@ConfigSearchableDropdown` | `searchableDropdown(...)` | enum |
| Grouped dropdown | `@ConfigGroupedDropdown` | `groupedDropdown(...)` | any value from grouped map |
| Searchable grouped dropdown | `@ConfigSearchableGroupedDropdown` | `searchableGroupedDropdown(...)` | any value from grouped map |
| Multi-select enum dropdown | `@ConfigMultiSelectDropdown` | `multiSelectDropdown(...)` | `List<Enum>` |
| Searchable multi-select enum dropdown | `@ConfigSearchableMultiSelectDropdown` | `searchableMultiSelectDropdown(...)` | `List<Enum>` |
| Draggable enum list | `@ConfigDraggableList` | `draggableList(...)` | `List<Enum>` |
| Custom row inside normal option shell | `@ConfigCustom` | `custom(...)` / `customBelow(...)` | custom component |
| Full-width custom option | `@ConfigCustomOption` | `customOption(...)` / `component(...)` | custom component |
| Custom persisted object list | `@ConfigCustomList` | `CustomListOption` through `custom(...)` or `customBelow(...)` | `List<T>` |
| Info row | `@ConfigInfo` | `info(...)` | none |
| Separator | `@ConfigSeparator` | `separator()` | none |
| Labeled separator | `@ConfigLabeledSeparator` | `labeledSeparator(...)` | none |
| Spacer | `@ConfigSpacer` | `spacer(...)` | none |
| Button | `@ConfigButton` | `button(...)` | `Runnable` action |
| Accordion | `@ConfigAccordion` | `accordion(...)` | groups following rows or nested builder body |

## Toggle and checkbox

AutoConfig:

```java
@ConfigToggle(key = "enabled", label = "Enabled", description = "Master switch.")
public static final MutableState<Boolean> enabled = new MutableState<>(true);

@ConfigCheckbox(key = "showDebug", label = "Show debug", description = "Display debug labels.")
public static final MutableState<Boolean> showDebug = new MutableState<>(false);
```

Manual:

```java
section.toggle("Enabled", enabled, "Master switch.")
       .checkbox("Show debug", showDebug, "Display debug labels.");
```

## Sliders and numbers

AutoConfig:

```java
@ConfigSlider(key = "scale", label = "Scale", min = 0.5, max = 2.0, step = 0.05)
public static final MutableState<Double> scale = new MutableState<>(1.0d);

@ConfigNumber(key = "limit", label = "Limit", min = 1, max = 100, step = 1)
public static final MutableState<Integer> limit = new MutableState<>(25);
```

Manual:

```java
section.slider("Scale", scale, 0.5, 2.0, 0.05, "UI scale.")
       .number("Limit", limit, Integer.class, 1, 100, 1, "Maximum entries.");
```

Use the overload with `numberType` when the state is not `Double`.

## Text input and sensitive text

`@ConfigText(regex = "...")` filters input with a full-value regex. Intermediate values are allowed when they can still become valid.

```java
@ConfigText(
        key = "uuid",
        label = "UUID",
        description = "Type or paste a UUID v4.",
        regex = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
)
public static final MutableState<String> uuid = new MutableState<>("");
```

Sensitive text changes rendering only; the saved value remains unchanged:

```java
@ConfigText(
        key = "token",
        label = "API token",
        sensitivity = ConfigTextSensitivity.VISIBLE_WHILE_EDITING
)
public static final MutableState<String> token = new MutableState<>("");
```

Manual:

```java
section.text("Profile", profileName, "Display profile name.", "[A-Za-z0-9_-]*")
       .password("API token", token, "Hidden secret value.");
```

## Color picker

```java
@ConfigColor(key = "accent", label = "Accent color", description = "Main UI accent.", allowAlpha = true)
public static final MutableState<ColorRGBA> accent = new MutableState<>(ColorRGBA.hex("#8b5cf6"));
```

Manual:

```java
section.color("Accent color", accent, true, "Main UI accent.");
```

`ColorRGBA` has helpers such as `rgb(r, g, b)`, `rgba(r, g, b, a)`, `hex("#RRGGBB")`, `fromArgb(int)`, and `fromRgba(int)`.

## Keybind selector

```java
@ConfigKeybind(key = "openMenu", label = "Open menu", description = "Must always have a real key.", disallowNone = true)
public static final MutableState<Keybind> openMenu = new MutableState<>(Keybind.none());
```

Manual:

```java
section.keybind("Open menu", openMenu, "Must always have a real key.", true);
```

`disallowNone = true` prevents Escape, Backspace, and Delete from clearing the binding.

## Dropdowns

Dropdowns use enum values by default. Override `toString()` for display labels.

```java
enum Mode {
    PERFORMANCE("Performance"), BALANCED("Balanced"), QUALITY("Quality");

    private final String label;

    Mode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

@ConfigSearchableDropdown(key = "mode", label = "Mode", description = "Runtime preset.")
public static final MutableState<Mode> mode = new MutableState<>(Mode.BALANCED);
```

Manual:

```java
section.searchableDropdown(
        "Mode",
        mode,
        Arrays.asList(Mode.values()),
        Mode::toString,
        "Runtime preset."
);
```

## Multi-select dropdown and draggable list

```java
enum Module { HUD, ALERTS, OVERLAY }

@ConfigSearchableMultiSelectDropdown(key = "modules", label = "Enabled modules")
public static final MutableState<List<Module>> modules = new MutableState<>(List.of(Module.HUD));

@ConfigDraggableList(key = "moduleOrder", label = "Module order", allowEmpty = false)
public static final MutableState<List<Module>> moduleOrder = new MutableState<>(List.of(Module.HUD, Module.ALERTS));
```

Manual:

```java
section.searchableMultiSelectDropdown(
        "Enabled modules",
        modules,
        Arrays.asList(Module.values()),
        Module::toString,
        "Choose active modules."
);

section.draggableList(
        "Module order",
        moduleOrder,
        Module.class,
        Module::toString,
        false,
        true,
        "Drag to reorder."
);
```

## Info, separators, buttons, and accordions

AutoConfig declaration order controls the row order:

```java
@ConfigInfo(title = "HUD", description = "Options that affect the overlay.")
public static final ConfigMarker hudInfo = ConfigMarker.marker();

@ConfigLabeledSeparator("Display")
public static final ConfigMarker displaySeparator = ConfigMarker.marker();

@ConfigButton(label = "Cache", buttonText = "Clear", description = "Clear cached data.")
public static final Runnable clearCache = MyRuntime::clearCache;
```

Manual:

```java
section.info("HUD", "Options that affect the overlay.")
       .labeledSeparator("Display")
       .button("Cache", "Clear", MyRuntime::clearCache, "Clear cached data.")
       .accordion("Advanced", advanced -> advanced
               .toggle("Debug", debug, "Extra logging."));
```
