package dev.someoneok.crystalconfig.minecraft;

import dev.someoneok.crystalconfig.input.MouseButton;
import dev.someoneok.crystalconfig.ui.UiRoot;

public final class MinecraftUiController<DrawContextT> {
    private final UiRoot root;
    private final MinecraftUiAdapter<DrawContextT> adapter;

    public MinecraftUiController(UiRoot root, MinecraftUiAdapter<DrawContextT> adapter) {
        this.root = root;
        this.adapter = adapter;
    }

    public UiRoot root() {
        return root;
    }

    public MinecraftUiAdapter<DrawContextT> adapter() {
        return adapter;
    }

    public void close() {
        root.close();
    }

    public MinecraftUiController<DrawContextT> onClose(Runnable callback) {
        root.onClose(callback);
        return this;
    }

    public void render(DrawContextT context, float tickDeltaSeconds) {
        adapter.attachContext(context);
        root.render(adapter, adapter.scaledWidth(), adapter.scaledHeight(), adapter.uiScale(), tickDeltaSeconds);
    }

    public boolean mouseMoved(double mouseX, double mouseY) {
        return root.mouseMoved((float) mouseX, (float) mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int modifiers) {
        return root.mousePressed((float) mouseX, (float) mouseY, MouseButton.fromIndex(button), button, modifiers);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button, int modifiers) {
        return root.mouseReleased((float) mouseX, (float) mouseY, MouseButton.fromIndex(button), button, modifiers);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, int modifiers) {
        return root.mouseDragged((float) mouseX, (float) mouseY, (float) deltaX, (float) deltaY, MouseButton.fromIndex(button), modifiers);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return root.mouseScrolled((float) mouseX, (float) mouseY, (float) horizontal, (float) vertical);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers, String displayName) {
        return root.keyPressed(keyCode, scanCode, modifiers, displayName);
    }

    public boolean charTyped(char chr, int modifiers) {
        return root.charTyped(chr, modifiers);
    }
}
