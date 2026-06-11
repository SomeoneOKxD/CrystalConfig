---
layout: default
title: Add CrystalConfig to your mod
description: Dependency and Fabric metadata setup for using CrystalConfig from another mod.
---

# Add CrystalConfig to your mod

CrystalConfig is exposed as a Fabric client mod with id `crystalconfig`. The published Maven coordinates used by this project are:

```text
group:    dev.someoneok
artifact: crystal-config
version:  1.0
```

The mod metadata in this project targets Minecraft `26.1` and Fabric Loader `0.18.5` or newer.

## Gradle dependency

Add the Maven repository that hosts CrystalConfig and depend on the Fabric mod artifact:

```kotlin
repositories {
    maven("https://your.maven.repository/releases")
}

dependencies {
    modImplementation("dev.someoneok:crystal-config:1.0")
}
```

If you intentionally want to bundle the library into your mod distribution instead of requiring users to install CrystalConfig separately, use Loom's `include` pattern:

```kotlin
dependencies {
    modImplementation("dev.someoneok:crystal-config:1.0")
    include("dev.someoneok:crystal-config:1.0")
}
```

<div class="callout warning">
  <strong>Choose one distribution model.</strong> If CrystalConfig is a separate required mod, keep only <code>modImplementation</code> and declare it in <code>fabric.mod.json</code>. If you bundle it, make sure your release process and license expectations match that choice.
</div>

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
