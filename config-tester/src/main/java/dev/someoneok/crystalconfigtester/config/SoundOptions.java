package dev.someoneok.crystalconfigtester.config;

import dev.someoneok.crystalconfig.autoconfig.ConfigCategory;
import dev.someoneok.crystalconfig.autoconfig.ConfigInfo;
import dev.someoneok.crystalconfig.autoconfig.ConfigMarker;
import dev.someoneok.crystalconfig.autoconfig.ConfigSound;
import dev.someoneok.crystalconfig.models.SoundSetting;
import dev.someoneok.crystalconfig.state.MutableState;

@ConfigCategory(main = "02 / Advanced", sub = "Minecraft sounds")
public final class SoundOptions {
    private SoundOptions() { }

    @ConfigInfo(title = "Minecraft-only widget", description = "Change or preview sound IDs, including resource-pack sounds.")
    public static final ConfigMarker info = ConfigMarker.marker();

    @ConfigSound(key = "sounds.notification", label = "Notification sound", description = "Allows None and provides a fallback sound", fallback = "minecraft:block.note_block.pling")
    public static final MutableState<SoundSetting> notification = new MutableState<>(SoundSetting.fromId("minecraft:block.note_block.pling"));

    @ConfigSound(key = "sounds.required", label = "Required sound", description = "None cannot be chosen", allowNone = false, fallback = "minecraft:entity.experience_orb.pickup")
    public static final MutableState<SoundSetting> required = new MutableState<>(SoundSetting.fromId("minecraft:entity.experience_orb.pickup"));
}
