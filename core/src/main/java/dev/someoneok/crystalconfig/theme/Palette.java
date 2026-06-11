package dev.someoneok.crystalconfig.theme;

import dev.someoneok.crystalconfig.render.ColorRGBA;

public record Palette(
        ColorRGBA background,
        ColorRGBA surface,
        ColorRGBA surfaceAlt,
        ColorRGBA surfaceHover,
        ColorRGBA surfaceActive,
        ColorRGBA text,
        ColorRGBA mutedText,
        ColorRGBA accent,
        ColorRGBA accentText,
        ColorRGBA border,
        ColorRGBA danger,
        ColorRGBA shadow
) {}
