package dev.someoneok.crystalconfig.autoconfig;

import dev.someoneok.crystalconfig.components.SoundSettingPicker;
import dev.someoneok.crystalconfig.models.SoundSetting;

public final class MinecraftAutoConfig {
    private static boolean registered;

    private MinecraftAutoConfig() {}

    public static synchronized void register() {
        if (registered) return;
        AutoConfig.registerComponent(ConfigSound.class, SoundSetting.class, context -> {
            ConfigSound annotation = context.annotation();
            return new SoundSettingPicker(context.state()).allowNone(annotation.allowNone());
        });
        registered = true;
    }
}
