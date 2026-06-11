# Custom Option Row

`customOption` / `@ConfigCustomOption` adds a fully custom, full-width row to a config section. Use it when the normal option layout is too restrictive and the developer wants to provide their own component tree without using mixins.

Unlike `@ConfigCustomList`, this is not a saved option by itself. The component owns its own layout, rendering, input handling, overlay rendering, focus, and any state binding it needs. Persistence is optional and depends on what the custom component does internally.

## When to use it

Use a custom option row for bespoke UI such as:

- a row with text and one or more buttons
- a full-width card with multiple controls
- a preview panel
- an import/export row
- a reset/cache/tools row
- a complex control that does not fit the normal label + input shape

If you need a persisted list where the framework owns add/remove/reorder and list saving, use `@ConfigCustomList` instead.

## Manual builder API

```java
ConfigScreenBuilder.create("My Config", settings)
        .section("General", section -> {
            section.customOption(
                    new Row()
                            .gap(10)
                            .fillX()
                            .add(new Label("Cache tools").flex(1))
                            .add(new Button("Reset").onClick(Cache::reset).width(90)),
                    "cache tools reset reload"
            );
        });
```

The second argument is optional search text. It is used only for config search/filtering and is not rendered.

```java
section.customOption(new MyFullWidthConfigRow());
section.customOption(new MyFullWidthConfigRow(), "search terms for this row");
```

`component(...)` is available as a shorter alias:

```java
section.component(new MyFullWidthConfigRow(), "search terms");
```

## Autoconfig annotation

`@ConfigCustomOption` can be placed on a static `Component` field:

```java
@ConfigCustomOption(searchText = "cache reset reload", tooltip = "Utilities for cache management.")
public static final Component CACHE_TOOLS = new Row()
        .gap(10)
        .fillX()
        .add(new Label("Cache tools").flex(1))
        .add(new Button("Reset").onClick(Cache::reset).width(90));
```

It can also be placed on a static `Supplier<Component>` field. Prefer this when the row should be recreated when the config screen is built:

```java
@ConfigCustomOption(searchText = "profile import export")
public static final Supplier<Component> PROFILE_TOOLS = () -> new Row()
        .gap(8)
        .fillX()
        .add(new Button("Import").onClick(ProfileTools::importProfile).width(90))
        .add(new Button("Export").onClick(ProfileTools::exportProfile).width(90));
```

The field must be static and must be either:

- `Component`
- `Supplier<Component>` where `get()` returns a `Component`

## Search behavior

Because the row is fully custom, the framework cannot automatically infer all searchable text inside it. Provide `searchText` when the row should appear in search results.

```java
section.customOption(new CacheToolsRow(), "cache reset reload clear");
```

Formatting codes in `searchText` are ignored during matching, just like normal option labels and descriptions.

## Conditions and tooltips

Custom option rows participate in the shared row behavior exposed by `SectionBuilder`:

```java
section.customOption(new AdvancedToolsRow(), "advanced tools")
        .hidden(() -> !advancedMode.get())
        .disabled(() -> reloading.get())
        .tooltip("Available in advanced mode.");
```

For autoconfig, `tooltip` is available directly on the annotation:

```java
@ConfigCustomOption(
        searchText = "advanced tools",
        tooltip = "Available in advanced mode."
)
public static final Supplier<Component> ADVANCED_TOOLS = AdvancedToolsRow::new;
```

## Persistence

`customOption` does not register a config value by itself.

If the row only runs actions, no persistence is needed:

```java
section.customOption(new Button("Reload assets").onClick(Assets::reload), "reload assets");
```

If the row edits values that should be saved, bind its internal controls to states that are registered elsewhere in the config system, or implement custom persistence in your own code.

Example using externally owned state:

```java
public static final MutableState<String> profileName = new MutableState<>("Default");

section.customOption(
        new Row()
                .gap(8)
                .fillX()
                .add(new Label("Profile").width(80))
                .add(new TextInput(profileName).fillX()),
        "profile name"
);
```

## Rendering and input handling

The supplied component is added to the config tree as a full-width row. It participates in:

- layout
- rendering
- overlay rendering
- ticking
- mouse input
- keyboard input
- focus handling
- disabled state propagation
- hidden state propagation

If your custom component opens popups or dropdowns, implement overlay rendering and input handling the same way as the built-in components.

## Custom option vs custom list

| Feature | `customOption` / `@ConfigCustomOption` | `@ConfigCustomList` |
|---|---|---|
| Owns the entire row layout | Yes | No, the framework owns the list shell |
| Saved automatically | No | Yes, as a list state |
| Add/remove/reorder built in | No | Yes |
| Good for action rows/cards | Yes | Not ideal |
| Good for persisted object lists | Possible, but manual | Yes |
| Requires normal option title/description | No | Yes |

## Notes

- Keep custom rows reasonably small. If a custom row becomes a whole screen, consider splitting it into normal options or an accordion.
- Provide `searchText` for rows that should be discoverable.
- Prefer `Supplier<Component>` in autoconfig when the component holds focus, animation, or transient UI state.
- Use `customOption` for UI shape flexibility; use normal `State`/`ConfigValue` options for automatic saving.
