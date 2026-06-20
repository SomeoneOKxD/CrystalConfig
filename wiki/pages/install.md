---
layout: default
title: Add CrystalConfig to your mod
description: Official JitPack dependency and Fabric metadata setup for using CrystalConfig from another mod.
---

# Add CrystalConfig to your mod

CrystalConfig is exposed as a Fabric client mod with id `crystalconfig`.

Use the official repository only:

```text
https://github.com/SomeoneOKxD/CrystalConfig
```

The official JitPack coordinate is:

```text
com.github.SomeoneOKxD.CrystalConfig:crystal-config:<version>
```

Replace `<version>` with an official release tag from `SomeoneOKxD/CrystalConfig`, for example `v1.0-mc26.1`. Do not use forked repositories, mirrored repositories, alternate Maven repositories, or alternate JitPack coordinates.

The mod metadata in this project targets Minecraft `26.1` and Fabric Loader `0.18.5` or newer.

## Gradle dependency

Add JitPack after your normal Fabric/Maven repositories:

```kotlin
repositories {
    maven("https://maven.fabricmc.net/")
    mavenCentral()
    maven("https://jitpack.io")
}
```

Depend on the official Fabric mod artifact:

```kotlin
dependencies {
    modImplementation("com.github.SomeoneOKxD.CrystalConfig:crystal-config:<version>")
}
```

## Fabric metadata

When CrystalConfig is required at runtime, add it to your `fabric.mod.json`:

```json
{
  "depends": {
    "fabricloader": ">=0.18.5",
    "minecraft": ">=26.1",
    "crystalconfig": ">=1.0"
  }
}
```

If the config screen is client-only, keep your own screen-opening code in a client initializer or client-only integration class.

## Client initializer

You do not need to initialize the normal builder API. If you use Minecraft-specific AutoConfig annotations such as `@ConfigSound`, register the Minecraft extension before building any AutoConfig models:

```java
package com.example.mymod.client;

import dev.someoneok.crystalconfig.autoconfig.MinecraftAutoConfig;
import net.fabricmc.api.ClientModInitializer;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MinecraftAutoConfig.register();
    }
}
```

The `crystalconfig` mod also calls `MinecraftAutoConfig.register()` from its own client initializer, but calling it from your mod is safe because registration is idempotent.

## Sources in IDEs

The official JitPack publication attaches a combined sources jar containing `minecraft-mod`, `core`, and `bridge-minecraft` sources. Gradle-aware IDEs can use that sources jar for navigation when dependency sources are downloaded.

## What to import

For most screens:

```java
import dev.someoneok.crystalconfig.config.ConfigScreenBuilder;
import dev.someoneok.crystalconfig.config.ConfigUiSettings;
import dev.someoneok.crystalconfig.persistence.GsonConfigStore;
import dev.someoneok.crystalconfig.render.ConfigScreen;
import dev.someoneok.crystalconfig.state.MutableState;
import dev.someoneok.crystalconfig.theme.ThemePresets;
import dev.someoneok.crystalconfig.ui.UiRoot;
```

For annotation-driven screens:

```java
import dev.someoneok.crystalconfig.autoconfig.AutoConfig;
import dev.someoneok.crystalconfig.autoconfig.ConfigCategory;
import dev.someoneok.crystalconfig.autoconfig.ConfigSlider;
import dev.someoneok.crystalconfig.autoconfig.ConfigToggle;
```
