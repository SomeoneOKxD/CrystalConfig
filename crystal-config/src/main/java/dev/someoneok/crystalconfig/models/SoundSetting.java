package dev.someoneok.crystalconfig.models;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;

@JsonAdapter(SoundSetting.SoundSerializer.class)
public record SoundSetting(@Nullable Identifier sound, float volume, float pitch, @Nullable Identifier fallback) {
    public static final float DEFAULT_VOLUME = 1.0f;
    public static final float DEFAULT_PITCH = 1.0f;
    public static final float MIN_VOLUME = 0.0f;
    public static final float MAX_VOLUME = 4.0f;
    public static final float MIN_PITCH = 0.01f;
    public static final float MAX_PITCH = 4.0f;

    public SoundSetting(@Nullable Identifier sound, float volume, float pitch) {
        this(sound, volume, pitch, null);
    }

    public SoundSetting {
        volume = clampFinite(volume, MIN_VOLUME, MAX_VOLUME, DEFAULT_VOLUME);
        pitch = clampFinite(pitch, MIN_PITCH, MAX_PITCH, DEFAULT_PITCH);
    }

    public static SoundSetting none() {
        return new SoundSetting(null, DEFAULT_VOLUME, DEFAULT_PITCH, null);
    }

    public static SoundSetting of(@Nullable Identifier sound) {
        return new SoundSetting(sound, DEFAULT_VOLUME, DEFAULT_PITCH, null);
    }

    public SoundSetting withSound(@Nullable Identifier sound) {
        return new SoundSetting(sound, volume, pitch, fallback);
    }

    public SoundSetting withVolume(float volume) {
        return new SoundSetting(sound, volume, pitch, fallback);
    }

    public SoundSetting withPitch(float pitch) {
        return new SoundSetting(sound, volume, pitch, fallback);
    }

    public SoundSetting withFallback(@Nullable Identifier fallback) {
        return new SoundSetting(sound, volume, pitch, fallback);
    }

    public boolean hasSound() {
        return sound != null;
    }

    public String displayName() {
        return sound == null ? "None" : sound.toShortString();
    }

    public static SoundSetting fromId(@Nullable String id) {
        return fromId(id, true);
    }

    public static SoundSetting fromId(@Nullable String id, boolean assumeMinecraftNamespace) {
        Identifier parsed = parseIdentifier(id, assumeMinecraftNamespace);
        return parsed == null ? none() : of(parsed);
    }

    @Nullable
    public static Identifier parseSoundId(@Nullable String id, boolean assumeMinecraftNamespace) {
        return parseIdentifier(id, assumeMinecraftNamespace);
    }

    @Nullable
    private static Identifier parseIdentifier(@Nullable String id, boolean assumeMinecraftNamespace) {
        if (id == null) return null;
        String value = id.trim();
        if (value.isEmpty()) return null;
        if (assumeMinecraftNamespace && value.indexOf(':') < 0) value = "minecraft:" + value;
        return Identifier.tryParse(value);
    }

    private static float clampFinite(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    public static final class SoundSerializer implements JsonSerializer<SoundSetting>, JsonDeserializer<SoundSetting> {
        @Override
        public JsonElement serialize(SoundSetting src, Type typeOfSrc, JsonSerializationContext ctx) {
            SoundSetting safe = src == null ? SoundSetting.none() : src;
            JsonObject object = new JsonObject();
            if (safe.sound() == null) object.add("sound", JsonNull.INSTANCE);
            else object.addProperty("sound", safe.sound().toString());
            object.addProperty("volume", safe.volume());
            object.addProperty("pitch", safe.pitch());
            if (safe.fallback() != null) object.addProperty("fallback", safe.fallback().toString());
            return object;
        }

        @Override
        public SoundSetting deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            if (json == null || json.isJsonNull()) return SoundSetting.none();
            if (json.isJsonPrimitive()) return SoundSetting.fromId(json.getAsString());
            if (!json.isJsonObject()) throw new JsonParseException("SoundSetting must be an object, string, or null");

            JsonObject object = json.getAsJsonObject();
            Identifier sound = readIdentifier(object, "sound");
            Identifier fallback = readIdentifier(object, "fallback");
            float volume = readFloat(object, "volume", DEFAULT_VOLUME);
            float pitch = readFloat(object, "pitch", DEFAULT_PITCH);
            return new SoundSetting(sound, volume, pitch, fallback);
        }

        @Nullable
        private static Identifier readIdentifier(JsonObject object, String key) {
            if (!object.has(key) || object.get(key).isJsonNull()) return null;
            try { return parseIdentifier(object.get(key).getAsString(), true); }
            catch (RuntimeException ignored) { return null; }
        }

        private static float readFloat(JsonObject object, String key, float fallback) {
            if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
            try {
                return object.get(key).getAsFloat();
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
    }
}
