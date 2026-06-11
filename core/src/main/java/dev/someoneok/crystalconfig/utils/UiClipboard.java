package dev.someoneok.crystalconfig.utils;

import dev.someoneok.crystalconfig.render.RenderBackend;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

/** Clipboard bridge used by text-editing widgets. */
public final class UiClipboard {
    private static RenderBackend activeBackend;
    private static String fallbackClipboard = "";

    private UiClipboard() {
    }

    public static void bindBackend(RenderBackend backend) {
        activeBackend = backend;
    }

    public static void set(String value) {
        String text = value == null ? "" : value;
        fallbackClipboard = text;

        if (RenderBackendHooks.invoke(activeBackend, "setClipboard", new Class<?>[] {String.class}, text)) {
            return;
        }

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        } catch (Throwable ignored) { }
    }

    public static String get() {
        RenderBackendHooks.Result result = RenderBackendHooks.invokeNullable(activeBackend, "getClipboard", new Class<?>[0]);
        if (result.value() instanceof String text) {
            fallbackClipboard = text;
            return text;
        }

        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data instanceof String s) {
                fallbackClipboard = s;
                return s;
            }
        } catch (Throwable ignored) { }
        return fallbackClipboard == null ? "" : fallbackClipboard;
    }
}
