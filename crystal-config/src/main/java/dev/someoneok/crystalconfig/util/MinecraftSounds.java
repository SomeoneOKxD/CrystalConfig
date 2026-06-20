package dev.someoneok.crystalconfig.util;

import dev.someoneok.crystalconfig.models.SoundSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MinecraftSounds {
    private static final float PREVIEW_MAX_VOLUME = 10.0f;
    private static final float PREVIEW_MAX_PITCH = 10.0f;

    private MinecraftSounds() {}

    public static List<Identifier> availableSounds() {
        List<Identifier> ids = new ArrayList<>(BuiltInRegistries.SOUND_EVENT.keySet());
        ids.sort(Comparator.comparing(Identifier::toString));
        return ids;
    }

    public static void playPreview(Identifier id, float volume, float pitch) {
        play(id, volume, pitch, SoundSource.MASTER);
    }

    public static void play(Identifier id, float volume, float pitch, SoundSource source) {
        if (id == null) return;

        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
            if (event == null || player == null) return;

            mc.getSoundManager().play(new SimpleSoundInstance(
                    id,
                    source == null ? SoundSource.MASTER : source,
                    clamp(volume, SoundSetting.MIN_VOLUME, PREVIEW_MAX_VOLUME),
                    clamp(pitch, SoundSetting.MIN_PITCH, PREVIEW_MAX_PITCH),
                    SoundInstance.createUnseededRandom(),
                    false,
                    0,
                    SoundInstance.Attenuation.NONE,
                    0.0,
                    0.0,
                    0.0,
                    true
            ));
        });
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

}
