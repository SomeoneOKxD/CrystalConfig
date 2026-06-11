package dev.someoneok.crystalconfig.render;

import java.util.List;

public interface RenderBackend {
    void beginFrame(RenderFrame frame);
    void endFrame();

    TextMetrics measureText(String text, float fontSize, String fontFace);

    void setClip(Rect clip);
    void clearClip();

    void drawQuads(Material material, List<QuadCommand> batch);
    void drawText(TextCommand command);
}
