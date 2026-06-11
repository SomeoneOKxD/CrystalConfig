# Config System Guide

The config system uses reactive state objects. A widget reads and writes the same `State<T>` instance that persistence and runtime code use.

## State types

| Type | Purpose |
|---|---|
| `State<T>` | Common read/write interface used by widgets. |
| `MutableState<T>` | In-memory state with change listeners. |
| `Binding<T>` | Adapter around an existing getter/setter pair. |
| `ConfigValue<T>` | State plus label, description, and persistence path metadata. |
| `ConditionalState<T>` | State with visibility and disabled predicates attached. |

## Conditional state

```java
MutableState<Boolean> enabled = new MutableState<>(true);

ConditionalState<Double> scale = ConditionalState
        .mutable(1.0d)
        .disabledWhen(() -> !enabled.get());
```

`ConditionalState` works on option rows. `@ConfigCategory` also supports `hiddenWhen` and `disabledWhen` members that point to a static boolean, `State<Boolean>`, `BooleanSupplier`, or no-argument static method.

Hidden sub-categories are removed from navigation and content. Disabled sub-categories stay visible but block their rows and navigation entry.

## Manual builder pattern

```java
ConfigScreenBuilder.create("My Config", settings)
        .section("Render", "General", section -> section
                .toggle("Enabled", enabled, "Master switch.")
                .slider("Scale", scale, 0.5, 2.0, 0.05, "UI scale.")
                        .tooltip("Controls the custom UI scale."))
        .hidden(() -> hideRenderTab.get())
        .disabled(() -> lockRenderTab.get())
        .build();
```

`SectionBuilder#hidden`, `disabled`, and `tooltip` apply to the row that was added immediately before the call.

## Grouped dropdowns

Use `groupedDropdown` or `searchableGroupedDropdown` when options should be visually divided into labelled groups. The input is a `Map<G, ? extends Collection<T>>` where `G extends Enum<G>`; the enum map key becomes a non-interactive group label row, and each collection item below it is selectable. Use a `LinkedHashMap` when group ordering matters. The default display text for both groups and values is `toString()`. If the collection values are complex objects, register a Gson/type adapter or serializer for the value type with the `GsonConfigStore` so saved configs can be loaded back into the same object type.

```java
MutableState<Profile> profile = new MutableState<>(Profile.PVP);

enum ProfileGroup { COMBAT, ECONOMY }

Map<ProfileGroup, List<Profile>> profiles = new LinkedHashMap<>();
profiles.put(ProfileGroup.COMBAT, List.of(Profile.PVP, Profile.DUNGEONS));
profiles.put(ProfileGroup.ECONOMY, List.of(Profile.MINING, Profile.FARMING));

ConfigScreenBuilder.create("My Config", settings)
        .section("Profiles", section -> section
                .groupedDropdown("Profile", profile, profiles, "Grouped dropdown.")
                .searchableGroupedDropdown("Searchable profile", profile, profiles, "Grouped dropdown with search."))
        .build();
```

Search keeps the group label visible for matching values. If a group label matches the query, all values in that group are shown. Clicking a group label has no effect; only value rows update the bound state.

AutoConfig supports the same widgets with `@ConfigGroupedDropdown(options = "...")` and `@ConfigSearchableGroupedDropdown(options = "...")`. The `options` member names a static field or no-arg static method on the same config class that returns the grouped map.

```java
@ConfigSearchableGroupedDropdown(label = "Profile", options = "PROFILE_OPTIONS")
public static final MutableState<Profile> profile = new MutableState<>(Profile.PVP);

public static final Map<ProfileGroup, List<Profile>> PROFILE_OPTIONS = new LinkedHashMap<>();
static {
    PROFILE_OPTIONS.put(ProfileGroup.COMBAT, List.of(Profile.PVP, Profile.DUNGEONS));
    PROFILE_OPTIONS.put(ProfileGroup.ECONOMY, List.of(Profile.MINING, Profile.FARMING));
}
```

## AutoConfig pattern

Use annotations when config definitions should stay close to the fields they control.

```java
@ConfigCategory(main = "Render", sub = "General")
public final class RenderConfig {
    @ConfigToggle(key = "enabled", label = "Enabled", description = "Master switch.")
    public static final MutableState<Boolean> enabled = new MutableState<>(true);

    @ConfigSlider(key = "scale", label = "Scale", min = 0.5, max = 2.0, step = 0.05)
    public static final MutableState<Double> scale = new MutableState<>(1.0d);
}
```

```java
AutoConfig.Model model = AutoConfig.of(RenderConfig.class);
model.register(store);
store.loadBlocking();
UiRoot root = model.root("My Config");
```

## Persistence

`GsonConfigStore` supports:

- explicit blocking or async loads
- async debounced saves
- atomic file replacement
- custom Gson/type adapters
- preservation of unknown JSON keys
- reset to values captured at registration time

Register settings with the same store when building screens manually:

```java
settings.register(store);
```

`AutoConfig.Model#register(store)` does this automatically and stores UI settings under `__configUiSettings`.

## Custom annotation-backed rows

Integrations can register annotations without changing `AutoConfig` internals.

```java
AutoConfig.registerComponent(ConfigSound.class, SoundSetting.class, context ->
        new SoundSettingPicker(context.state()).allowNone(context.annotation().allowNone())
);
```

The custom annotation must expose `label()`, `description()`, and `key()` methods. The annotated field must be a compatible `State<T>`, `MutableState<T>`, or `ConfigValue<T>`.

## Minecraft text formatting in display text

Developer-provided display strings support legacy Minecraft formatting codes. This applies to labels, descriptions, category/section text, button text, dropdown/list labels, tooltips, and other UI text rendered through the config display layer.

Supported colors:

- `§0` Black `#000000`
- `§1` Dark Blue `#0000AA`
- `§2` Dark Green `#00AA00`
- `§3` Dark Aqua `#00AAAA`
- `§4` Dark Red `#AA0000`
- `§5` Dark Purple `#AA00AA`
- `§6` Gold `#FFAA00`
- `§7` Gray `#AAAAAA`
- `§8` Dark Gray `#555555`
- `§9` Blue `#5555FF`
- `§a` Green `#55FF55`
- `§b` Aqua `#55FFFF`
- `§c` Red `#FF5555`
- `§d` Light Purple `#FF55FF`
- `§e` Yellow `#FFFF55`
- `§f` White `#FFFFFF`

Supported styles:

- `§l` bold
- `§n` underline
- `§m` strikethrough
- `§r` reset to the theme color and normal style

Color/style codes are ignored by config searching and filtering. Editable text inputs intentionally render and store typed codes as plain text. To show a formatting code literally in developer display text, escape it with a backslash, for example `\\§cHello` in Java source renders as `§cHello` instead of red `Hello`.

## Fully custom option rows

For components that do not fit the built-in label + control layout, add a full-width custom option:

```java
ConfigScreenBuilder.create("Example")
        .section("Tools", section -> section
                .customOption(new MyFullWidthConfigRow(), "tools custom row")
                .disabled(() -> busy.get()));
```

A custom option receives normal layout, rendering, overlay rendering, ticking, focus and input events from the config UI. It does not automatically save anything by itself; bind its internal controls to existing `State`/`ConfigValue` instances or handle persistence manually.

For autoconfig, use `@ConfigCustomOption` on a static `Component` or `Supplier<Component>` field. Prefer `Supplier<Component>` when possible so each rebuilt config screen gets a fresh component instance.
