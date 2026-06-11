---
layout: default
title: Themes and UI settings
description: ConfigUiSettings usage, themes, scale, text shadow, and reset behavior.
---

# Themes and UI settings

`ConfigUiSettings` owns the settings menu state for a config screen: selected theme, UI scale, text shadow, and reset behavior. Create one settings object per config screen model.

## Basic setup

AutoConfig:

```java
AutoConfig.Model model = AutoConfig.of(GameplayConfig.class)
        .configureSettings(settings -> settings
                .defaultTheme(ThemePresets.darkCrimson())
                .defaultScale(1.0d)
                .defaultTextShadow(false));
```

Manual:

```java
ConfigUiSettings settings = ConfigUiSettings.create()
        .defaultTheme(ThemePresets.darkCrimson())
        .defaultScale(1.0d)
        .defaultTextShadow(false);

UiRoot root = ConfigScreenBuilder.create("MyMod Config", settings)
        .section("General", section -> section
                .toggle("Enabled", enabled, "Master switch."))
        .buildRoot();
```

## Built-in themes

`ConfigUiSettings.create()` includes CrystalConfig's built-in themes. Useful presets include:

```java
ThemePresets.darkCrimson();
ThemePresets.midnightSteel();
ThemePresets.oceanDepths();
ThemePresets.emeraldGrove();
ThemePresets.amethystNight();
ThemePresets.nordFrost();
ThemePresets.solarAmber();
ThemePresets.cyberMint();
ThemePresets.graphiteClean();
ThemePresets.rosePine();
ThemePresets.emberForge();
ThemePresets.auroraViolet();
ThemePresets.lagoonTeal();
ThemePresets.royalIndigo();
ThemePresets.slateBlue();
ThemePresets.softLight();
```

## Register a custom theme

```java
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.theme.*;

public final class MyThemes {
    public static final Theme VOID_PURPLE = new Theme(
            "Void Purple",
            new Palette(
                    ColorRGBA.hex("#0e1117"),
                    ColorRGBA.hex("#151a23"),
                    ColorRGBA.hex("#1b2230"),
                    ColorRGBA.hex("#222a3a"),
                    ColorRGBA.hex("#2a3346"),
                    ColorRGBA.hex("#e8eef8"),
                    ColorRGBA.hex("#a9b4c7"),
                    ColorRGBA.hex("#8b5cf6"),
                    ColorRGBA.WHITE,
                    ColorRGBA.hex("#2a3446"),
                    ColorRGBA.hex("#ef4444"),
                    ColorRGBA.rgba(0, 0, 0, 120)
            ),
            new SpacingScale(4, 8, 12, 16, 24),
            new FontScale("regular", "medium", "semibold", 10, 12, 14, 18, 24),
            new Radii(6, 10, 14, 999),
            new AnimationSpec(0.08f, 0.14f, 0.24f)
    );
}
```

Register and select it:

```java
ConfigUiSettings settings = ConfigUiSettings.create()
        .registerTheme(MyThemes.VOID_PURPLE)
        .defaultTheme(MyThemes.VOID_PURPLE);
```

## UI scale

Scale values are snapped to supported steps:

```java
ConfigUiSettings settings = ConfigUiSettings.create()
        .defaultScale(1.0d);
```

Available steps are:

```text
0.50, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25, 1.375, 1.50
```

`UiRoot` also has `globalScale(float)`, which multiplies the root scale after the user's selected settings scale:

```java
UiRoot root = model.root("MyMod Config")
        .globalScale(1.0f);
```

## Text shadow

Text shadow can be controlled by settings and/or by `ConfigScreen` constructor options.

Persisted settings value:

```java
settings.defaultTextShadow(false);
```

Screen constructor hook:

```java
new ConfigScreen(root, "MyMod Config", () -> settings.textShadow());
```

## Persist UI settings

AutoConfig does this from `model.register(store)`. Manual screens should register the settings explicitly:

```java
settings.register(store);
```

The default persistence key is:

```text
__configUiSettings
```

Use a custom key when one config file stores multiple independent CrystalConfig screens:

```java
settings.register(store, "__mainConfigUiSettings");
```

## Reset behavior

`settings.reset()` restores selected theme, scale, and text shadow to defaults. You can register callbacks:

```java
settings.onDefaultsReset(() -> MyRuntime.reloadFromConfig());
```

AutoConfig links the settings reset to `GsonConfigStore#resetRegisteredDefaults()` when you call:

```java
model.register(store);
```
