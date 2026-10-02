package dev.someoneok.crystalconfig.render;

import java.util.Objects;

/**
 * Renderer-neutral Minecraft-style resource identifier.
 *
 * <p>This intentionally does not depend on Minecraft's {@code Identifier}, keeping the public
 * render API usable from the renderer-neutral module. The Minecraft backend resolves it only when
 * the image batch is submitted.</p>
 */
public record TextureRef(String namespace, String path) {
    public TextureRef {
        namespace = requirePart(namespace, "namespace");
        path = requirePart(path, "path");
    }

    public static TextureRef of(String namespace, String path) {
        return new TextureRef(namespace, path);
    }

    /** Parses {@code namespace:path}. Identifiers without a namespace use {@code minecraft}. */
    public static TextureRef of(String identifier) {
        String value = Objects.requireNonNull(identifier, "identifier").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("identifier cannot be blank");
        int split = value.indexOf(':');
        if (split < 0) return new TextureRef("minecraft", value);
        if (split == 0 || split == value.length() - 1) {
            throw new IllegalArgumentException("identifier must be namespace:path");
        }
        return new TextureRef(value.substring(0, split), value.substring(split + 1));
    }

    public String identifier() {
        return namespace + ':' + path;
    }

    private static String requirePart(String value, String name) {
        String part = Objects.requireNonNull(value, name).trim();
        if (part.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank");
        return part;
    }
}
