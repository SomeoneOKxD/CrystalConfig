package dev.someoneok.crystalconfig.layout;

import dev.someoneok.crystalconfig.render.RenderBackend;
import dev.someoneok.crystalconfig.theme.Theme;

public record LayoutContext(Theme theme, RenderBackend backend) {}
