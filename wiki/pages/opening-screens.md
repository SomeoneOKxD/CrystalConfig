---
layout: default
title: Opening screens
description: How to open CrystalConfig screens from a Fabric mod.
---

# Opening screens

Both setup paths end with a `UiRoot`. The Fabric module provides `dev.someoneok.crystalconfig.render.ConfigScreen`, a Minecraft `Screen` wrapper around that root.

## Basic open call

```java
import dev.someoneok.crystalconfig.render.ConfigScreen;
import dev.someoneok.crystalconfig.ui.UiRoot;
import net.minecraft.client.Minecraft;

public final class MyModScreens {
    public static void openConfig() {
        UiRoot root = MyModConfigScreenFactory.createRoot();
        Minecraft.getInstance().setScreen(new ConfigScreen(root, "MyMod Config"));
    }
}
```

Call `openConfig()` from your own keybinding, mod menu integration, command, button, or debug UI.

## Constructor options

`ConfigScreen` has four constructors:

```java
new ConfigScreen(root, "Title");
new ConfigScreen(root, "Title", textShadowGetter);
new ConfigScreen(root, "Title", renderFpsGetter);
new ConfigScreen(root, "Title", textShadowGetter, renderFpsGetter);
```

Use `textShadowGetter` when you want Minecraft text shadow to follow your own runtime toggle:

```java
new ConfigScreen(
        root,
        "MyMod Config",
        () -> MyModConfig.textShadow.get()
);
```

Use `renderFpsGetter` to cap the custom UI render refresh while idle. Values less than or equal to zero mean uncapped:

```java
new ConfigScreen(
        root,
        "MyMod Config",
        () -> 60
);
```

The screen temporarily forces fresh frames while the mouse moves, users type, widgets animate, scroll panels move, or layouts change.

## Close lifecycle

Always close the `GsonConfigStore` when the config screen closes so any pending debounced save is flushed:

```java
UiRoot root = model.root("MyMod Config")
        .onClose(store::close);
```

For manual builder screens:

```java
UiRoot root = ConfigScreenBuilder.create("MyMod Config", settings)
        .onClose(store::close)
        .section("General", section -> section
                .toggle("Enabled", enabled, "Master switch."))
        .buildRoot();
```

`UiRoot#close()` callbacks run at most once per root instance. `ConfigScreen` calls them from `onClose()` and `removed()`.

## Initial search text

AutoConfig and manual screens can start with a prefilled search query:

```java
UiRoot autoRoot = model.root("MyMod Config", "hud");

UiRoot manualRoot = ConfigScreenBuilder.create("MyMod Config", settings)
        .section("General", section -> section
                .toggle("Enabled", enabled, "Master switch."))
        .buildRoot("hud");
```

## Keeping screen code client-only

`ConfigScreen` references Minecraft client classes. Keep factories that create `ConfigScreen` instances in client-only source or client-only initializer code.

Config definitions that only use `core` types such as `MutableState`, `AutoConfig`, and `ConfigScreenBuilder` can live outside the screen-opening class, but do not load Minecraft client classes from common/server-only entrypoints.
