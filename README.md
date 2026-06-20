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
| `docs` | Source-maintainer notes and API references for working on this repository. |
| `wiki` | GitHub Pages developer guide for using CrystalConfig from another mod. |

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

## Use from another mod

CrystalConfig is only distributed from the official repository:

```text
https://github.com/SomeoneOKxD/CrystalConfig
```

Use JitPack with the official coordinates:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    modImplementation("com.github.SomeoneOKxD.CrystalConfig:crystal-config:<version>")
}
```

Replace `<version>` with an official CrystalConfig release tag from the repository, for example `v1.0+mc26.1`. Release tags are generated from `mod_version` and `minecraft_version` in `gradle.properties`, so the same CrystalConfig version can be released for different Minecraft versions. Do not use forked repositories, mirrored repositories, alternate Maven repositories, or alternate JitPack coordinates.

When CrystalConfig is a separate runtime dependency, also add `crystalconfig` to your mod's `fabric.mod.json` dependencies. See [Official Distribution](docs/DISTRIBUTION.md) for the official artifact details.

## Build

Build the distributable mod jar and combined sources jar:

```bash
./gradlew buildModWithSources
```

The outputs are written to `minecraft-mod/build/libs/`. The main jar is produced by `:minecraft-mod:shadowJar`; `*-dev.jar` is the plain unshaded development jar.

A full Gradle build is still available when you want every standard verification task:

```bash
./gradlew build
```

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

- [Getting Started](docs/GETTING_STARTED.md) — shortest path from dependency setup to a rendered config screen
- [Official Distribution](docs/DISTRIBUTION.md) — official JitPack coordinates, CI artifacts, and release build details
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
