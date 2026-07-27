---
layout: default
title: Minecraft sound picker
description: Use registered and resource-pack sounds with persistent fallback behavior.
---

# Minecraft sound picker

The Fabric module adds a Minecraft-specific sound picker through `@ConfigSound`. It binds to `SoundSetting`, discovers vanilla/mod sound events plus sound events supplied by active resource packs, and stores the selected sound ID, volume, pitch, and fallback.

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
@ConfigCategory(main = "Audio", sub = "Alerts")
public final class AudioConfig {
    @ConfigSound(
            key = "alertSound",
            label = "Alert sound",
            description = "Texture/resource-pack sounds are supported.",
            allowNone = true,
            fallback = "minecraft:block.note_block.pling"
    )
    public static final MutableState<SoundSetting> alertSound = new MutableState<>(
            SoundSetting.fromId("minecraft:block.note_block.pling")
    );
}
```

A sound ID without a namespace assumes `minecraft`. When `fallback` is blank, the field's initial selected sound is captured as its fallback. Set an explicit built-in sound when the default selection may itself come from a resource pack.

## Missing resource packs

If a user selects `my_pack:custom_alert` and later disables or removes that resource pack:

- the selected ID remains in the JSON and remains visible as missing in the picker;
- CrystalConfig does not replace or clear the user's choice;
- preview and playback use the available fallback;
- reinstalling the pack makes the original selection work again automatically.

## Saved JSON

```json
{
  "sound": "my_pack:custom_alert",
  "volume": 1.0,
  "pitch": 1.0,
  "fallback": "minecraft:block.note_block.pling"
}
```

Older string/object/null formats remain readable. `fallback` is omitted when none is configured.

## Using the selected sound

Use the `SoundSetting` overload so runtime playback gets the same missing-sound fallback behavior as the picker preview:

```java
import dev.someoneok.crystalconfig.util.MinecraftSounds;
import net.minecraft.sounds.SoundSource;

public static void playAlert() {
    MinecraftSounds.play(AudioConfig.alertSound.get(), SoundSource.MASTER);
}
```

`MinecraftSounds.resolve(selected, fallback)` is also available when another playback system needs the resolved ID.

## Manual screen usage

```java
Identifier fallback = SoundSetting.parseSoundId("minecraft:block.note_block.pling", true);

section.custom(
        "Alert sound",
        new SoundSettingPicker(AudioConfig.alertSound)
                .fallback(fallback)
                .allowNone(true),
        "Played when an alert is triggered."
);
```

The picker refreshes the active sound list while open. Missing persisted selections are retained even when they no longer appear in the active sound manager.

## Value limits

| Field | Range |
|---|---|
| `volume` | `0.0` to `4.0` |
| `pitch` | `0.01` to `4.0` |
