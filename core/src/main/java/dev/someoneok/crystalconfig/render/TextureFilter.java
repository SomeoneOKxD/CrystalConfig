package dev.someoneok.crystalconfig.render;

/** Texture sampling mode used by renderer-neutral image commands. */
public enum TextureFilter {
    /** Crisp sampling for Minecraft GUI sprites and pixel art. */
    NEAREST,
    /** Linear sampling for photographs, scaled artwork, and smooth icons. */
    LINEAR
}
