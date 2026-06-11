package dev.someoneok.crystalconfig.utils;

import dev.someoneok.crystalconfig.render.RenderBackend;

import java.awt.*;
import java.net.URI;

/** URL opener used by footer and link actions. */
public final class UiUrlOpener {
    private static RenderBackend activeBackend;

    private UiUrlOpener() {
    }

    public static void bindBackend(RenderBackend backend) {
        activeBackend = backend;
    }

    public static void open(String value) {
        if (value == null || value.isBlank()) return;
        String url = value.trim();

        if (RenderBackendHooks.invoke(activeBackend, "openUrl", new Class<?>[] {String.class}, url)) {
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Throwable ignored) { }
    }

}
