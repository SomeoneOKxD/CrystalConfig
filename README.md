# CrystalConfig

A modern, renderer-agnostic configuration UI library for Minecraft mods.

The reusable UI framework lives in `core`. Minecraft-specific rendering, input integration, sound options, MSDF text rendering, and Fabric packaging live outside the core module so the UI model can be reused by other Minecraft backends.

## What it includes

- Annotation-driven config screens with `AutoConfig`
- Manual config screens with `ConfigScreenBuilder`
- Reactive state primitives: `State<T>`, `MutableState<T>`, `Binding<T>`, and `ConditionalState<T>`
- JSON persistence with `GsonConfigStore`
- Per-screen UI settings for theme, scale, reset behavior, and store registration
- Renderer-neutral draw commands and a small Minecraft adapter layer
- Built-in controls for toggles, checkboxes, sliders, numbers, text, colors, keybinds, dropdowns, multi-select dropdowns, grouped dropdowns, draggable enum lists, and custom object lists
- Fabric client implementation packaged as `CrystalConfig`
- Minecraft-only sound picker support through `@ConfigSound`
- MSDF font atlas generation tasks for the Fabric renderer

## Modules

| Module | Purpose |
| --- | --- |
| `core` | Reusable UI components, layout, state, annotations, themes, draw commands, and JSON persistence. Contains no Minecraft imports. |
| `bridge-minecraft` | Loader/version-neutral interfaces for Minecraft render and input backends. |
| `minecraft-mod` | Fabric client module, Minecraft renderer backend, MSDF text renderer, shaders, and Minecraft-only widgets. |
| `docs` | Developer documentation for the APIs and integration points. |
| `wiki` | GitHub Pages documentation source. |

## Requirements

- Java 25 for building the included Fabric `minecraft-mod` module
- Java 17 bytecode target for `core` and `bridge-minecraft`
- Gradle wrapper included in the repository
- Network access on the first build so Gradle can download dependencies

Project defaults are defined in `gradle.properties`:

```properties
minecraft_version=26.1
loader_version=0.18.5
mod_version=1.0
maven_group=dev.someoneok
archives_base_name=crystal-config
```

## Build

```bash
./gradlew build
```

The Fabric module produces a shaded `crystal-config` jar containing `core` and `bridge-minecraft`.

To generate MSDF font atlases from local TTF files:

```bash
./gradlew :minecraft-mod:generateMsdfFonts
```

See `docs/MSDF_FONT_PIPELINE.md` for the expected font file names and `msdf-atlas-gen` location.

## Basic usage

### Annotation-based config

```java
@ConfigCategory(main = "General", sub = "Gameplay")
public final class GameplayConfig {
    @ConfigToggle(key = "enabled", label = "Enabled", description = "Master switch.")
    public static final MutableState<Boolean> enabled = new MutableState<>(true);

    @ConfigSlider(key = "scale", label = "Scale", min = 0.5, max = 2.0, step = 0.05)
    public static final MutableState<Double> scale = new MutableState<>(1.0);
}
```

```java
AutoConfig.Model model = AutoConfig.of(GameplayConfig.class)
        .configureSettings(settings -> settings.defaultTheme(ThemePresets.darkCrimson()));

GsonConfigStore store = GsonConfigStore.builder(configPath).build();
model.register(store);
store.loadBlocking();

UiRoot root = model.root("My Mod");
```

### Manual config screen

```java
ConfigUiSettings settings = ConfigUiSettings.create()
        .defaultTheme(ThemePresets.darkCrimson())
        .defaultScale(1.0d);

Component screen = ConfigScreenBuilder.create("Example Config", settings)
        .section("General", section -> section
                .toggle("Enabled", enabled, "Master switch.")
                .slider("Scale", scale, 0.5, 2.0, 0.05, "UI scale."))
        .build();

UiRoot root = new UiRoot(screen, settings)
        .onClose(store::close);
```

### Minecraft-only options

Register Minecraft-specific AutoConfig widgets before creating models:

```java
MinecraftAutoConfig.register();
```

```java
@ConfigSound(label = "Alert sound", description = "Played when the alert triggers.")
public static final MutableState<SoundSetting> alertSound =
        new MutableState<>(SoundSetting.none());
```

## Documentation

- [Getting Started](docs/GETTING_STARTED.md) — shortest path from state to a rendered config screen
- [Architecture](docs/ARCHITECTURE.md) — module boundaries, render lifecycle, and input lifecycle
- [Annotation API](docs/ANNOTATION_API.md) — supported AutoConfig annotations
- [Config System Guide](docs/CONFIG_SYSTEM_GUIDE.md) — config state and persistence model
- [Config UI Settings](docs/CONFIG_UI_SETTINGS.md) — themes, scaling, reset behavior, and store registration
- [Custom Option](docs/CUSTOM_OPTION.md) and [Custom List Option](docs/CUSTOM_LIST_OPTION.md) — custom widget extension points
- [Backend Checklist](docs/MINECRAFT_BACKEND_CHECKLIST.md) — checklist for another Minecraft backend
- [MSDF Font Pipeline](docs/MSDF_FONT_PIPELINE.md) — font atlas generation setup

## License

This project is proprietary and all rights are reserved.

The root project and included modules contain `LICENSE` files with the same proprietary terms. The Fabric mod metadata also declares `All-Rights-Reserved`. Do not copy, modify, publish, sublicense, distribute, or use this software without prior written permission from the copyright holder.
