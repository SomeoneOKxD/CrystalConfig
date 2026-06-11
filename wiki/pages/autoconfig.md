---
layout: default
title: AutoConfig setup
description: Annotation-driven CrystalConfig screen setup.
---

# AutoConfig setup

AutoConfig builds a config screen from annotated static fields. It also registers those fields with `GsonConfigStore`, so UI, runtime code, and saved JSON all share the same state objects.

Use this path when your settings can be declared as fields and grouped with annotations.

## 1. Define an annotated config class

```java
package com.example.mymod.config;

import dev.someoneok.crystalconfig.autoconfig.ConfigCategory;
import dev.someoneok.crystalconfig.autoconfig.ConfigDropdown;
import dev.someoneok.crystalconfig.autoconfig.ConfigSlider;
import dev.someoneok.crystalconfig.autoconfig.ConfigText;
import dev.someoneok.crystalconfig.autoconfig.ConfigToggle;
import dev.someoneok.crystalconfig.state.MutableState;

@ConfigCategory(main = "General", sub = "Gameplay")
public final class GameplayConfig {
    private GameplayConfig() {
    }

    @ConfigToggle(
            key = "enabled",
            label = "Enabled",
            description = "Master switch for this mod."
    )
    public static final MutableState<Boolean> enabled = new MutableState<>(true);

    @ConfigSlider(
            key = "hudScale",
            label = "HUD scale",
            description = "Scales this mod's HUD elements.",
            min = 0.5,
            max = 2.0,
            step = 0.05
    )
    public static final MutableState<Double> hudScale = new MutableState<>(1.0d);

    @ConfigDropdown(
            key = "profile",
            label = "Profile",
            description = "Preset used by the mod."
    )
    public static final MutableState<Profile> profile = new MutableState<>(Profile.DEFAULT);

    @ConfigText(
            key = "nickname",
            label = "Nickname",
            description = "Letters, numbers, underscores and dashes only.",
            regex = "[A-Za-z0-9_-]*"
    )
    public static final MutableState<String> nickname = new MutableState<>("default");

    public enum Profile {
        DEFAULT("Default"),
        PERFORMANCE("Performance"),
        QUALITY("Quality");

        private final String label;

        Profile(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
```

`key` is the JSON key. If it is blank, CrystalConfig derives a key from the field/category path. Explicit keys are recommended when you care about stable config files.

## 2. Create the model, store, and root

```java
package com.example.mymod.config;

import dev.someoneok.crystalconfig.autoconfig.AutoConfig;
import dev.someoneok.crystalconfig.persistence.GsonConfigStore;
import dev.someoneok.crystalconfig.theme.ThemePresets;
import dev.someoneok.crystalconfig.ui.UiRoot;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;

public final class MyModConfigScreenFactory {
    private MyModConfigScreenFactory() {
    }

    public static UiRoot createRoot() {
        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("mymod.json");

        AutoConfig.Model model = AutoConfig.of(GameplayConfig.class)
                .configureSettings(settings -> settings
                        .defaultTheme(ThemePresets.darkCrimson())
                        .defaultScale(1.0d)
                        .defaultTextShadow(false));

        GsonConfigStore store = GsonConfigStore.builder(configPath).build();
        model.register(store);

        try {
            store.loadBlocking();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load MyMod config", e);
        }

        return model.root("MyMod Config")
                .onClose(store::close);
    }
}
```

## 3. Open the Fabric screen

```java
import dev.someoneok.crystalconfig.render.ConfigScreen;
import net.minecraft.client.Minecraft;

Minecraft.getInstance().setScreen(new ConfigScreen(
        MyModConfigScreenFactory.createRoot(),
        "MyMod Config"
));
```

See [Opening screens]({{ '/pages/opening-screens/' | relative_url }}) for constructor options and lifecycle notes.

## Multiple categories

Use one or more classes. Class order controls screen order:

```java
AutoConfig.Model model = AutoConfig.of(
        GameplayConfig.class,
        HudConfig.class,
        AudioConfig.class
);
```

Each class can choose its sidebar grouping:

```java
@ConfigCategory(main = "Visuals", sub = "HUD")
public final class HudConfig {
    // fields
}
```

## Layout class option

When you want explicit ordering without putting everything in one class, use `AutoConfig.layout` or `AutoConfig.ofLayout`. Layout fields may contain a single class, a class array, or a collection of classes.

```java
public final class ConfigLayout {
    @ConfigCategory(main = "General", sub = "Gameplay")
    public static final Class<?> gameplay = GameplayConfig.class;

    @ConfigCategory(main = "Visuals", sub = "HUD")
    public static final Class<?> hud = HudConfig.class;
}

AutoConfig.Model model = AutoConfig.layout(ConfigLayout.class);
```

## Package scanning

If your config classes are grouped in a package, you can scan them:

```java
AutoConfig.Model model = AutoConfig.scanPackageOf(GameplayConfig.class);
```

Use explicit `AutoConfig.of(...)` when stable ordering matters most.

## Conditional visibility and disabled state

Category annotations can reference a static condition member on the same class. Supported condition members are static booleans, `State<Boolean>`, `BooleanSupplier`, or no-argument static methods returning boolean.

```java
@ConfigCategory(main = "Debug", sub = "Advanced", hiddenWhen = "hideAdvanced")
public final class AdvancedConfig {
    public static final MutableState<Boolean> hideAdvanced = new MutableState<>(false);

    @ConfigToggle(key = "logPackets", label = "Log packets", description = "Verbose client logging.")
    public static final MutableState<Boolean> logPackets = new MutableState<>(false);
}
```

For row-level conditions, wrap a state with `ConditionalState`:

```java
public static final MutableState<Boolean> enabled = new MutableState<>(true);

@ConfigSlider(key = "range", label = "Range", min = 1, max = 64, step = 1)
public static final ConditionalState<Double> range = ConditionalState
        .mutable(16.0d)
        .disabledWhen(() -> !enabled.get());
```

## Grouped dropdown example

Grouped dropdowns use a `Map<G, ? extends Collection<T>>`, where the group key is an enum. Group labels are visual dividers; only values are selectable.

```java
public enum ProfileGroup { COMBAT, ECONOMY }

public enum Profile {
    PVP("PvP"),
    DUNGEONS("Dungeons"),
    MINING("Mining"),
    FARMING("Farming");

    private final String label;

    Profile(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}

public static final Map<ProfileGroup, List<Profile>> PROFILE_OPTIONS = new LinkedHashMap<>();
static {
    PROFILE_OPTIONS.put(ProfileGroup.COMBAT, List.of(Profile.PVP, Profile.DUNGEONS));
    PROFILE_OPTIONS.put(ProfileGroup.ECONOMY, List.of(Profile.MINING, Profile.FARMING));
}

@ConfigSearchableGroupedDropdown(
        key = "profile",
        label = "Profile",
        description = "Search grouped profiles.",
        options = "PROFILE_OPTIONS"
)
public static final MutableState<Profile> profile = new MutableState<>(Profile.PVP);
```

## Footer buttons and icons

Footer button actions are declared as static `Runnable` fields:

```java
@ConfigFooterButton("Open support")
public static final Runnable support = () -> MyLinks.openSupportPage();
```

Footer icons can open URLs, copy text, run code, or render as non-clickable markers:

```java
import dev.someoneok.crystalconfig.autoconfig.ConfigFooterIcon;
import dev.someoneok.crystalconfig.autoconfig.ConfigFooterIconAction;
import dev.someoneok.crystalconfig.autoconfig.ConfigMarker;
import dev.someoneok.crystalconfig.icons.MediaBrandIcons;

@ConfigFooterIcon(
        icon = MediaBrandIcons.GITHUB,
        action = ConfigFooterIconAction.OPEN_URL,
        value = "https://github.com/example/mymod",
        tooltip = "GitHub"
)
public static final ConfigMarker github = ConfigMarker.marker();
```

## When AutoConfig is not the best fit

Use [Non-AutoConfig setup]({{ '/pages/manual/' | relative_url }}) when rows are created dynamically, when you need unusual layout control, or when your config values already live in non-static objects that are easier to bind manually.
