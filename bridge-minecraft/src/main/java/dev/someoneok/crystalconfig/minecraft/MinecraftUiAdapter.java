package dev.someoneok.crystalconfig.minecraft;

import dev.someoneok.crystalconfig.render.RenderBackend;

public interface MinecraftUiAdapter<DrawContextT> extends RenderBackend {
    void attachContext(DrawContextT context);

    int scaledWidth();
    int scaledHeight();
    float uiScale();

    default String getClipboard() { return ""; }
    default void setClipboard(String value) { }
    default void closeScreen() { }
    default void openUrl(String url) { }
}
