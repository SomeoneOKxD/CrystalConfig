package dev.someoneok.crystalconfig.render.text;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class MsdfFont {
    public record Glyph(
            int codepoint,
            float advance,
            float planeLeft,
            float planeBottom,
            float planeRight,
            float planeTop,
            float atlasLeft,
            float atlasBottom,
            float atlasRight,
            float atlasTop,
            float xOffset,
            float yOffset,
            float width,
            float height,
            float u0,
            float v0,
            float u1,
            float v1,
            boolean drawable
    ) {}

    private final Identifier texture;
    private final Map<Integer, Glyph> glyphs;
    private final Glyph fallbackGlyph;
    private final float atlasWidth;
    private final float atlasHeight;
    private final float atlasSize;
    private final float lineHeight;
    private final float ascender;
    private final float descender;

    private MsdfFont(
            Identifier texture,
            Map<Integer, Glyph> glyphs,
            float atlasWidth,
            float atlasHeight,
            float atlasSize,
            float lineHeight,
            float ascender,
            float descender
    ) {
        this.texture = texture;
        this.glyphs = glyphs;
        this.fallbackGlyph = chooseFallback(glyphs);
        this.atlasWidth = atlasWidth;
        this.atlasHeight = atlasHeight;
        this.atlasSize = atlasSize;
        this.lineHeight = lineHeight;
        this.ascender = ascender;
        this.descender = descender;
    }

    public static Optional<MsdfFont> tryLoad(String name) {
        try {
            return Optional.of(load(name));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    public static MsdfFont load(String name) {
        try {
            Identifier jsonId = Identifier.fromNamespaceAndPath("crystalconfig", "msdf/" + name + ".json");
            Identifier textureId = Identifier.fromNamespaceAndPath("crystalconfig", "textures/msdf/" + name + ".png");

            var resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(jsonId);
            JsonObject root;
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }

            JsonObject atlas = root.getAsJsonObject("atlas");
            JsonObject metrics = root.getAsJsonObject("metrics");

            float atlasWidth = getFloat(atlas, "width", 1.0f);
            float atlasHeight = getFloat(atlas, "height", 1.0f);
            float atlasSize = getFloat(atlas, "size", 48.0f);
            float lineHeight = getFloat(metrics, "lineHeight", 1.0f);
            float ascender = getFloat(metrics, "ascender", 0.8f);
            float descender = getFloat(metrics, "descender", -0.2f);

            Map<Integer, Glyph> glyphs = new HashMap<>(256);
            JsonArray array = root.getAsJsonArray("glyphs");
            for (JsonElement element : array) {
                JsonObject g = element.getAsJsonObject();
                int unicode = g.get("unicode").getAsInt();
                float advance = getFloat(g, "advance", 0.0f);

                if (!g.has("planeBounds") || !g.has("atlasBounds")) {
                    glyphs.put(unicode, new Glyph(unicode, advance, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false));
                    continue;
                }

                JsonObject plane = g.getAsJsonObject("planeBounds");
                JsonObject atlasBounds = g.getAsJsonObject("atlasBounds");
                float planeLeft = getFloat(plane, "left", 0.0f);
                float planeBottom = getFloat(plane, "bottom", 0.0f);
                float planeRight = getFloat(plane, "right", 0.0f);
                float planeTop = getFloat(plane, "top", 0.0f);
                float atlasLeft = getFloat(atlasBounds, "left", 0.0f);
                float atlasBottom = getFloat(atlasBounds, "bottom", 0.0f);
                float atlasRight = getFloat(atlasBounds, "right", 0.0f);
                float atlasTop = getFloat(atlasBounds, "top", 0.0f);
                boolean drawable = planeLeft != planeRight && planeBottom != planeTop;

                glyphs.put(unicode, new Glyph(
                        unicode,
                        advance,
                        planeLeft,
                        planeBottom,
                        planeRight,
                        planeTop,
                        atlasLeft,
                        atlasBottom,
                        atlasRight,
                        atlasTop,
                        planeLeft,
                        -planeTop,
                        planeRight - planeLeft,
                        planeTop - planeBottom,
                        atlasLeft / atlasWidth,
                        1.0f - atlasTop / atlasHeight,
                        atlasRight / atlasWidth,
                        1.0f - atlasBottom / atlasHeight,
                        drawable
                ));
            }

            return new MsdfFont(textureId, glyphs, atlasWidth, atlasHeight, atlasSize, lineHeight, ascender, descender);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load MSDF font atlas: " + name, e);
        }
    }

    private static Glyph chooseFallback(Map<Integer, Glyph> glyphs) {
        Glyph glyph = glyphs.get(0xFFFD);
        if (glyph != null) return glyph;
        glyph = glyphs.get((int) '?');
        if (glyph != null) return glyph;
        glyph = glyphs.get((int) ' ');
        if (glyph != null) return glyph;
        return null;
    }

    private static float getFloat(JsonObject object, String key, float fallback) {
        return object != null && object.has(key) ? object.get(key).getAsFloat() : fallback;
    }

    public Identifier texture() { return texture; }
    public Glyph glyphOrNull(int codepoint) { return glyphs.get(codepoint); }
    public Glyph fallbackGlyph() { return fallbackGlyph; }
    public Glyph glyph(int codepoint) {
        Glyph glyph = glyphs.get(codepoint);
        return glyph != null ? glyph : fallbackGlyph;
    }
    public float atlasWidth() { return atlasWidth; }
    public float atlasHeight() { return atlasHeight; }
    public float atlasSize() { return atlasSize; }
    public float lineHeight() { return lineHeight; }
    public float ascender() { return ascender; }
    public float descender() { return descender; }
}
