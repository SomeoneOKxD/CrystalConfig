# CrystalConfig

A modern, renderer-agnostic configuration UI library for Minecraft mods.

The reusable UI framework lives in `core`. Minecraft-specific rendering, input integration, sound options, MSDF text rendering, and Fabric packaging live outside the core module so the UI model can be reused by other Minecraft backends.

## What it includes

- Annotation-driven config screens with `AutoConfig`
- Manual config screens with `ConfigScreenBuilder`
- Reactive state primitives: `State<T>`, `MutableState<T>`, `Binding<T>`, and `ConditionalState<T>`
- JSON persistence with `GsonConfigStore`
- User-created named profiles with explicit per-profile setting links and stable generated backend IDs
- Per-screen UI settings for theme, scale, reset behavior, and store registration
- Renderer-neutral draw commands and a small Minecraft adapter layer
- Stateless custom-screen rendering utilities backed by the same batched SDF/MSDF renderer
- Built-in controls for toggles, checkboxes, sliders, numbers, text, colors, keybinds, dropdowns, multi-select dropdowns, grouped dropdowns, draggable enum lists, and custom object lists
- Fabric client implementation packaged as `CrystalConfig`
- Minecraft-only sound picker support through `@ConfigSound`, including active resource-pack sounds and persisted fallbacks
- MSDF font atlas generation tasks for the Fabric renderer

## Modules

| Module | Purpose |
| --- | --- |
| `core` | Reusable UI components, layout, state, annotations, themes, draw commands, and JSON persistence. Contains no Minecraft imports. |
| `bridge-minecraft` | Loader/version-neutral interfaces for Minecraft render and input backends. |
| `render-api` | Small, renderer-only public API for custom screens. No config persistence or input ownership. |
| `crystal-config` | Published Fabric client module, Minecraft renderer backend, MSDF text renderer, shaders, and Minecraft-only widgets. Stonecutter builds this module for Minecraft 26.1, 26.2, and 26.3. |
| `config-tester` | Standalone client-only Fabric testing mod; all config annotations and a five-page custom rendering showcase. Uses Stonecutter for 26.1, 26.2, and 26.3. See [tester README](config-tester/README.md). |
| `docs` | Source-maintainer notes and API references for working on this repository. |
| `wiki` | GitHub Pages developer guide for using CrystalConfig from another mod. |

## Requirements

- Java 25 for building the included Fabric `crystal-config` module
- Global Java 25 toolchain and bytecode target across all modules
- Gradle wrapper included in the repository
- Network access on the first build so Gradle can download dependencies

Project defaults are defined in `gradle.properties`; the Minecraft targets are declared in `settings.gradle` through Stonecutter:

```properties
loom_version=1.18.3
shadow_version=8.3.11
mod_version=1.4
maven_group=dev.someoneok
archives_base_name=crystal-config
```

The supported Minecraft nodes are `26.1`, `26.2`, and `26.3`, with `26.1` kept as the checked-in active Stonecutter source representation. Fabric Loader minimums are version-specific: **0.18.5** for 26.1, and **0.19.5** for 26.2/26.3. Edit `crystal-config/versions/<minecraft-version>/gradle.properties` to update these requirements; do not set a shared `loader_version` in the root properties.

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
    // Pick the artifact matching your Minecraft version.
    implementation("com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.1:<version>")
    // implementation("com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.2:<version>")
    // implementation("com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.3:<version>")
}
```

Replace `<version>` with an official CrystalConfig release tag from the repository, for example `v1.4`. One release tag contains all supported Minecraft builds. JitPack uses the `com.github.SomeoneOKxD.CrystalConfig` group for version-specific modules; the legacy `com.github.SomeoneOKxD:CrystalConfig` publication remains a 26.1 compatibility alias. Do not use forked repositories, mirrored repositories, alternate Maven repositories, or alternate JitPack coordinates.

When CrystalConfig is a separate runtime dependency, also add `crystalconfig` to your mod's `fabric.mod.json` dependencies. See [Official Distribution](docs/DISTRIBUTION.md) for the official artifact details.

## Build

Build the distributable mod jar and combined sources jar:

```bash
./gradlew buildModWithSources
```

The outputs for all supported Minecraft versions are collected in `crystal-config/build/libs/`. Each Stonecutter node produces a shaded mod jar plus a combined sources jar. Run `python3 tools/verify_release_artifacts.py --mod-version 1.3` to check the shaded classes, metadata, bundled version-specific resources, and sources jars. CI also compiles/links all six GLSL programs with `glslangValidator`. Real in-game rendering still needs the manual test checklist in [Official Distribution](docs/DISTRIBUTION.md) before a release is tagged.

A full Gradle build is still available when you want every standard verification task:

```bash
./gradlew build
```

To generate MSDF font atlases from local TTF files:

```bash
./gradlew :crystal-config:26.1:generateMsdfFonts
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
@ConfigSound(
        label = "Alert sound",
        description = "Played when the alert triggers.",
        fallback = "minecraft:block.note_block.pling"
)
public static final MutableState<SoundSetting> alertSound =
        new MutableState<>(SoundSetting.fromId("minecraft:block.note_block.pling"));
```

## Documentation

- [Getting Started](docs/GETTING_STARTED.md) — shortest path from dependency setup to a rendered config screen
- [Official Distribution](docs/DISTRIBUTION.md) — official JitPack coordinates, CI artifacts, and release build details
- [Architecture](docs/ARCHITECTURE.md) — module boundaries, render lifecycle, and input lifecycle
- [Annotation API](docs/ANNOTATION_API.md) — supported AutoConfig annotations
- [Config System Guide](docs/CONFIG_SYSTEM_GUIDE.md) — config state and persistence model
- [User-created Profiles](docs/PROFILES.md) — named selectors and explicit profile-scoped settings
- [Config UI Settings](docs/CONFIG_UI_SETTINGS.md) — themes, scaling, reset behavior, and store registration
- [Custom Option](docs/CUSTOM_OPTION.md) and [Custom List Option](docs/CUSTOM_LIST_OPTION.md) — custom widget extension points
- [Backend Checklist](docs/MINECRAFT_BACKEND_CHECKLIST.md) — checklist for another Minecraft backend
- [MSDF Font Pipeline](docs/MSDF_FONT_PIPELINE.md) — font atlas generation setup
- [Renderer Utilities API](docs/RENDER_UTILS_API.md) — use the config renderer primitives in custom screens without config state/persistence
- [Renderer Utilities Screen Guide](docs/RENDER_UTILS_SCREEN_GUIDE.md) — complete `Screen` subclass, input handling, lifecycle, and draw-helper reference

## License

This project is proprietary and all rights are reserved.

The root project and included modules contain `LICENSE` files with the same proprietary terms. The Fabric mod metadata also declares `All-Rights-Reserved`. Do not copy, modify, publish, sublicense, distribute, or use this software without prior written permission from the copyright holder.


> JitPack note: if a version tag already exists from an older broken build, rerun the `Build and release CrystalConfig mod` workflow manually with `force_recreate_release` enabled, or bump `mod_version` before pushing.
