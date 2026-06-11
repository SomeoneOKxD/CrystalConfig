---
layout: default
title: Minecraft sound picker
description: Using CrystalConfig's Fabric-only ConfigSound option.
---

# Minecraft sound picker

The Fabric module adds a Minecraft-specific sound picker through `@ConfigSound`. It binds to `SoundSetting`, lets users select a registered sound, and stores sound id, volume, and pitch.

## Register the Minecraft AutoConfig extension

Register before building AutoConfig models that use `@ConfigSound`:

```java
import dev.someoneok.crystalconfig.autoconfig.MinecraftAutoConfig;
import net.fabricmc.api.ClientModInitializer;

public final class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MinecraftAutoConfig.register();
    }
}
```

Registration is safe to call more than once.

## AutoConfig example

```java
package com.example.mymod.config;

import dev.someoneok.crystalconfig.autoconfig.ConfigCategory;
import dev.someoneok.crystalconfig.autoconfig.ConfigSound;
import dev.someoneok.crystalconfig.models.SoundSetting;
import dev.someoneok.crystalconfig.state.MutableState;

@ConfigCategory(main = "Audio", sub = "Alerts")
public final class AudioConfig {
    private AudioConfig() {
    }

    @ConfigSound(
            key = "alertSound",
            label = "Alert sound",
            description = "Played when an alert is triggered.",
            allowNone = true
    )
    public static final MutableState<SoundSetting> alertSound = new MutableState<>(
            SoundSetting.fromId("minecraft:block.note_block.pling")
    );

    @ConfigSound(
            key = "requiredSound",
            label = "Required sound",
            description = "This option cannot be set to None.",
            allowNone = false
    )
    public static final MutableState<SoundSetting> requiredSound = new MutableState<>(
            SoundSetting.fromId("entity.experience_orb.pickup")
    );
}
```

`SoundSetting.fromId("entity.experience_orb.pickup")` assumes the `minecraft` namespace when no namespace is present.

## Saved JSON

`SoundSetting` has a built-in Gson adapter. It saves as:

```json
{
  "sound": "minecraft:block.note_block.pling",
  "volume": 1.0,
  "pitch": 1.0
}
```

`None` saves as:

```json
{
  "sound": null,
  "volume": 1.0,
  "pitch": 1.0
}
```

## Using the selected sound

```java
import dev.someoneok.crystalconfig.models.SoundSetting;
import dev.someoneok.crystalconfig.util.MinecraftSounds;

public final class MyAlerts {
    public static void playAlert() {
        SoundSetting setting = AudioConfig.alertSound.get();
        if (setting.hasSound()) {
            MinecraftSounds.playPreview(setting.sound(), setting.volume(), setting.pitch());
        }
    }
}
```

## Manual screen usage

The sound picker is registered as an AutoConfig custom component. For manual screens, use the component directly:

```java
import dev.someoneok.crystalconfig.components.SoundSettingPicker;

section.custom(
        "Alert sound",
        new SoundSettingPicker(AudioConfig.alertSound).allowNone(true),
        "Played when an alert is triggered."
);
```

## Value limits

`SoundSetting` clamps invalid values:

| Field | Range |
|---|---|
| `volume` | `0.0` to `4.0` |
| `pitch` | `0.01` to `4.0` |

The preview helper clamps to a wider preview range internally, but your stored `SoundSetting` remains within the model limits.
