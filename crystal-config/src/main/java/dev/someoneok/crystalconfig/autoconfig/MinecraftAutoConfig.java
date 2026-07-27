package dev.someoneok.crystalconfig.autoconfig;

import dev.someoneok.crystalconfig.components.SoundSettingPicker;
import dev.someoneok.crystalconfig.models.SoundSetting;
import dev.someoneok.crystalconfig.state.State;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public final class MinecraftAutoConfig {
    private static final Map<State<?>, Identifier> INITIAL_SOUND_FALLBACKS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static boolean registered;

    private MinecraftAutoConfig() {}

    public static synchronized void register() {
        if (registered) return;
        AutoConfig.registerComponent(ConfigSound.class, SoundSetting.class, context -> {
            ConfigSound annotation = context.annotation();
            SoundSetting current = context.state().get() == null ? SoundSetting.none() : context.state().get();
            Identifier fallback = resolveFallback(
                    annotation,
                    current,
                    context.state(),
                    context.owner().getName() + "." + context.field().getName()
            );
            if (!Objects.equals(current.fallback(), fallback)) {
                current = current.withFallback(fallback);
                context.state().set(current);
            }
            return new SoundSettingPicker(context.state())
                    .fallback(fallback)
                    .allowNone(annotation.allowNone());
        });
        registered = true;
    }

    private static Identifier resolveFallback(
            ConfigSound annotation,
            SoundSetting current,
            State<SoundSetting> state,
            String fieldName
    ) {
        String explicit = annotation.fallback() == null ? "" : annotation.fallback().trim();
        if (!explicit.isEmpty()) {
            Identifier parsed = SoundSetting.parseSoundId(explicit, true);
            if (parsed == null) throw new IllegalArgumentException("Invalid @ConfigSound fallback on " + fieldName + ": " + explicit);
            return parsed;
        }

        if (current.fallback() != null) return current.fallback();
        synchronized (INITIAL_SOUND_FALLBACKS) {
            if (!INITIAL_SOUND_FALLBACKS.containsKey(state)) {
                INITIAL_SOUND_FALLBACKS.put(state, current.sound());
            }
            return INITIAL_SOUND_FALLBACKS.get(state);
        }
    }
}
