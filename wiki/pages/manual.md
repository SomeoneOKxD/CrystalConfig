---
layout: default
title: Non-AutoConfig setup
description: Manual CrystalConfig screen setup with ConfigScreenBuilder.
---

# Non-AutoConfig setup

The non-AutoConfig path uses `ConfigScreenBuilder` directly. You own the layout, row order, state registration, and any dynamic behavior.

Use this path when your config is object-based, generated from runtime data, or needs custom section layout.

## 1. Define state

```java
package com.example.mymod.config;

import dev.someoneok.crystalconfig.components.Keybind;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.state.MutableState;

import java.util.List;

public final class MyModConfig {
    private MyModConfig() {
    }

    public static final MutableState<Boolean> enabled = new MutableState<>(true);
    public static final MutableState<Double> hudScale = new MutableState<>(1.0d);
    public static final MutableState<String> profileName = new MutableState<>("default");
    public static final MutableState<ColorRGBA> accentColor = new MutableState<>(ColorRGBA.hex("#8b5cf6"));
    public static final MutableState<Keybind> openMenu = new MutableState<>(Keybind.none());
    public static final MutableState<Mode> mode = new MutableState<>(Mode.BALANCED);
    public static final MutableState<List<Module>> enabledModules = new MutableState<>(List.of(Module.HUD));

    public enum Mode {
        PERFORMANCE("Performance"),
        BALANCED("Balanced"),
        QUALITY("Quality");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Module {
        HUD("HUD"), ALERTS("Alerts"), OVERLAY("Overlay");

        private final String label;

        Module(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
```

Use `Binding<T>` instead of `MutableState<T>` when the value already lives in another object:

```java
State<Boolean> enabled = Binding.of(
        () -> ExistingClientConfig.enabled,
        value -> ExistingClientConfig.enabled = value
);
```

## 2. Register persistence

Manual screens do not auto-register state. Register each persisted value yourself and register the UI settings if you want selected theme, scale, and text shadow to persist.

```java
package com.example.mymod.config;

import com.google.gson.reflect.TypeToken;
import dev.someoneok.crystalconfig.persistence.GsonConfigStore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class MyModConfigStore {
    private MyModConfigStore() {
    }

    public static GsonConfigStore create() {
        Path path = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("mymod.json");

        return GsonConfigStore.builder(path)
                .configVersion(1)
                .build()
                .register("enabled", MyModConfig.enabled, Boolean.class)
                .register("hudScale", MyModConfig.hudScale, Double.class)
                .register("profileName", MyModConfig.profileName, String.class)
                .register("accentColor", MyModConfig.accentColor, dev.someoneok.crystalconfig.render.ColorRGBA.class)
                .register("openMenu", MyModConfig.openMenu, dev.someoneok.crystalconfig.components.Keybind.class)
                .register("mode", MyModConfig.mode, MyModConfig.Mode.class)
                .register("enabledModules", MyModConfig.enabledModules, new TypeToken<List<MyModConfig.Module>>() {});
    }

    public static void load(GsonConfigStore store) {
        try {
            store.loadBlocking();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load MyMod config", e);
        }
    }
}
```

## 3. Build the screen

```java
package com.example.mymod.config;

import dev.someoneok.crystalconfig.config.ConfigScreenBuilder;
import dev.someoneok.crystalconfig.config.ConfigUiSettings;
import dev.someoneok.crystalconfig.persistence.GsonConfigStore;
import dev.someoneok.crystalconfig.theme.ThemePresets;
import dev.someoneok.crystalconfig.ui.UiRoot;

import java.util.Arrays;

public final class MyModManualScreenFactory {
    private MyModManualScreenFactory() {
    }

    public static UiRoot createRoot() {
        ConfigUiSettings settings = ConfigUiSettings.create()
                .defaultTheme(ThemePresets.darkCrimson())
                .defaultScale(1.0d)
                .defaultTextShadow(false);

        GsonConfigStore store = MyModConfigStore.create();
        settings.register(store);
        MyModConfigStore.load(store);

        return ConfigScreenBuilder.create("MyMod Config", settings)
                .onClose(store::close)
                .section("General", section -> section
                        .toggle("Enabled", MyModConfig.enabled, "Master switch for this mod.")
                        .slider("HUD scale", MyModConfig.hudScale, 0.5, 2.0, 0.05, "Scales this mod's HUD elements.")
                        .text("Profile name", MyModConfig.profileName, "Letters, numbers, underscores and dashes only.", "[A-Za-z0-9_-]*")
                        .dropdown(
                                "Mode",
                                MyModConfig.mode,
                                Arrays.asList(MyModConfig.Mode.values()),
                                MyModConfig.Mode::toString,
                                "Runtime behavior preset."
                        ))
                .section("Controls", section -> section
                        .keybind("Open menu", MyModConfig.openMenu, "Key used to open this mod's menu.", true)
                        .multiSelectDropdown(
                                "Enabled modules",
                                MyModConfig.enabledModules,
                                Arrays.asList(MyModConfig.Module.values()),
                                MyModConfig.Module::toString,
                                "Choose which modules are active."
                        )
                        .color("Accent color", MyModConfig.accentColor, true, "Main accent color."))
                .footerButton("Reset runtime cache", MyModRuntime::resetCache)
                .buildRoot();
    }
}
```

## 4. Open the Fabric screen

```java
import dev.someoneok.crystalconfig.render.ConfigScreen;
import net.minecraft.client.Minecraft;

Minecraft.getInstance().setScreen(new ConfigScreen(
        MyModManualScreenFactory.createRoot(),
        "MyMod Config"
));
```

## Dedicated non-AutoConfig example with grouped dropdowns

```java
MutableState<Profile> profile = new MutableState<>(Profile.PVP);

Map<ProfileGroup, List<Profile>> profiles = new LinkedHashMap<>();
profiles.put(ProfileGroup.COMBAT, List.of(Profile.PVP, Profile.DUNGEONS));
profiles.put(ProfileGroup.ECONOMY, List.of(Profile.MINING, Profile.FARMING));

UiRoot root = ConfigScreenBuilder.create("Profiles", settings)
        .section("Profiles", section -> section
                .groupedDropdown("Profile", profile, profiles, "Choose a grouped profile.")
                .searchableGroupedDropdown("Searchable profile", profile, profiles, "Search grouped profiles."))
        .buildRoot();
```

## Row modifiers

`hidden`, `disabled`, and `tooltip` apply to the row or section added immediately before the call.

```java
ConfigScreenBuilder.create("MyMod Config", settings)
        .section("Advanced", section -> section
                .toggle("Enabled", MyModConfig.enabled, "Master switch.")
                .slider("Experimental scale", MyModConfig.hudScale, 0.5, 2.0, 0.05, "Advanced value.")
                        .disabled(() -> !MyModConfig.enabled.get())
                        .tooltip("Only available while the mod is enabled."))
        .buildRoot();
```

Use `ConditionalState<T>` when the condition should travel with the state and work in either AutoConfig or manual screens.

## Manual builder strengths

The builder is best when you need to:

- create rows from runtime data;
- place custom components exactly where you want them;
- register persistence keys explicitly;
- mix normal widgets, custom rows, and footer controls in one screen;
- reuse states that are owned by an existing config object.
