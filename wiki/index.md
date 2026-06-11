---
layout: default
title: Overview
description: CrystalConfig developer wiki overview.
---

# CrystalConfig developer wiki

<div class="hero">
  <p><strong>CrystalConfig</strong> is a modern client-side config UI library for Minecraft mods. Use it when your mod needs polished config screens, persisted settings, searchable categories, custom rows, theming, and Minecraft-specific controls such as a sound picker.</p>
</div>

This wiki is for developers adding CrystalConfig to their own mod. It covers dependency setup, two complete integration paths, persistence, widgets, themes, custom options, and the Fabric screen wrapper.

<div class="grid">
  <div class="tile">
    <h3><a href="{{ '/pages/autoconfig/' | relative_url }}">AutoConfig setup</a></h3>
    <p>Use annotations on static config fields and let CrystalConfig build, register, and search the screen model.</p>
  </div>
  <div class="tile">
    <h3><a href="{{ '/pages/manual/' | relative_url }}">Non-AutoConfig setup</a></h3>
    <p>Build config screens directly with <code>ConfigScreenBuilder</code> and explicit state registration.</p>
  </div>
  <div class="tile">
    <h3><a href="{{ '/pages/widgets/' | relative_url }}">Widget reference</a></h3>
    <p>See the supported annotations, manual builder methods, expected state types, and example snippets.</p>
  </div>
</div>

## What the library provides

| Area | What you use |
|---|---|
| Config screen generation | `AutoConfig` annotations or `ConfigScreenBuilder` |
| State | `State<T>`, `MutableState<T>`, `Binding<T>`, `ConfigValue<T>`, `ConditionalState<T>` |
| Persistence | `GsonConfigStore` with debounced async saves, blocking or async loads, migrations, and custom Gson adapters |
| Fabric screen | `dev.someoneok.crystalconfig.render.ConfigScreen` |
| Minecraft-only AutoConfig widgets | `@ConfigSound` and `SoundSetting` from the Fabric module |
| Customization | Themes, UI scale, text shadow, custom rows, custom list rows, footer buttons, and footer icon buttons |

## Choose an integration style

Use **AutoConfig** when your config is mostly normal settings and you want the config definition to live beside the state fields.

Use **Non-AutoConfig** when you want full control over layout, construct rows dynamically, or already have custom state objects you want to bind manually.

Both paths can use the same persistence store, state objects, themes, and Fabric `ConfigScreen` wrapper.

## Package names used by the examples

Most imports come from:

```java
import dev.someoneok.crystalconfig.autoconfig.*;
import dev.someoneok.crystalconfig.config.*;
import dev.someoneok.crystalconfig.persistence.GsonConfigStore;
import dev.someoneok.crystalconfig.render.ConfigScreen;
import dev.someoneok.crystalconfig.state.*;
import dev.someoneok.crystalconfig.theme.ThemePresets;
import dev.someoneok.crystalconfig.ui.UiRoot;
```

Minecraft-only controls additionally use:

```java
import dev.someoneok.crystalconfig.autoconfig.ConfigSound;
import dev.someoneok.crystalconfig.autoconfig.MinecraftAutoConfig;
import dev.someoneok.crystalconfig.models.SoundSetting;
```

## Recommended reading order

1. [Add CrystalConfig to your mod]({{ '/pages/install/' | relative_url }})
2. Pick [AutoConfig setup]({{ '/pages/autoconfig/' | relative_url }}) or [Non-AutoConfig setup]({{ '/pages/manual/' | relative_url }})
3. Wire the result into Minecraft with [Opening screens]({{ '/pages/opening-screens/' | relative_url }})
4. Add [Persistence]({{ '/pages/persistence/' | relative_url }}) and [Themes & UI settings]({{ '/pages/ui-settings/' | relative_url }})
5. Use the [Widget reference]({{ '/pages/widgets/' | relative_url }}) for individual option types
