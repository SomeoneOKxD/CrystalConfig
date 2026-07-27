package dev.someoneok.crystalconfig.util;

import dev.someoneok.crystalconfig.models.SoundSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

public final class MinecraftSounds {
    private static final float PREVIEW_MAX_VOLUME = 10.0f;
    private static final float PREVIEW_MAX_PITCH = 10.0f;

    private MinecraftSounds() {}

    public static List<Identifier> availableSounds() {
        Minecraft mc = Minecraft.getInstance();
        LinkedHashSet<Identifier> ids = new LinkedHashSet<>(mc.getSoundManager().getAvailableSounds());
        List<Identifier> result = new ArrayList<>(ids);
        result.sort(Comparator.comparing(Identifier::toString));
        return result;
    }

    public static boolean isAvailable(Identifier id) {
        return id != null && Minecraft.getInstance().getSoundManager().getSoundEvent(id) != null;
    }

    public static Identifier resolve(Identifier selected, Identifier fallback) {
        if (selected == null) return null;
        if (isAvailable(selected)) return selected;
        return isAvailable(fallback) ? fallback : null;
    }

    public static void playPreview(Identifier id, float volume, float pitch) {
        playPreview(id, null, volume, pitch);
    }

    public static void playPreview(Identifier id, Identifier fallback, float volume, float pitch) {
        play(id, fallback, volume, pitch, SoundSource.MASTER);
    }

    public static void play(SoundSetting setting, SoundSource source) {
        if (setting == null) return;
        play(setting.sound(), setting.fallback(), setting.volume(), setting.pitch(), source);
    }

    public static void play(Identifier id, float volume, float pitch, SoundSource source) {
        play(id, null, volume, pitch, source);
    }

    public static void play(Identifier id, Identifier fallback, float volume, float pitch, SoundSource source) {
        if (id == null) return;

        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            Identifier resolved = resolve(id, fallback);
            if (resolved == null) return;

            mc.getSoundManager().play(new SimpleSoundInstance(
                    resolved,
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
