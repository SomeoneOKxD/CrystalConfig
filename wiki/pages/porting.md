---
layout: default
title: Advanced adapters
description: Advanced notes for hosting CrystalConfig core UI outside the included Fabric screen wrapper.
---

# Advanced adapters

Most mod developers should use the included Fabric `ConfigScreen`. This page is for advanced integrations that need to host CrystalConfig's core UI with another Minecraft screen backend or a custom renderer.

This is not a distribution path. CrystalConfig should still be consumed from the official `SomeoneOKxD/CrystalConfig` artifact.

## Module roles

| Module | Role |
|---|---|
| `core` | Renderer-neutral components, layout, state, AutoConfig, persistence, themes, and draw commands. |
| `bridge-minecraft` | Small adapter surface for Minecraft integrations. |
| `minecraft-mod` | Fabric client screen, render backend, MSDF text renderer, assets, and Minecraft-only widgets. |
| `wiki` | This GitHub Pages developer wiki. |

## Adapter contract

Implement `MinecraftUiAdapter<DrawContextT>`, which extends `RenderBackend`:

```java
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
```

`RenderBackend` must provide:

```java
void beginFrame(RenderFrame frame);
void endFrame();
TextMetrics measureText(String text, float fontSize, String fontFace);
void setClip(Rect clip);
void clearClip();
void drawQuads(Material material, List<QuadCommand> batch);
void drawText(TextCommand command);
```

## Controller usage

`MinecraftUiController` forwards render and input calls into `UiRoot`:

```java
MinecraftUiController<MyDrawContext> controller = new MinecraftUiController<>(root, adapter);

adapter.attachContext(drawContext);
controller.render(drawContext, deltaSeconds);

controller.mouseMoved(mouseX, mouseY);
controller.mouseClicked(mouseX, mouseY, button, modifiers);
controller.mouseReleased(mouseX, mouseY, button, modifiers);
controller.mouseDragged(mouseX, mouseY, button, deltaX, deltaY, modifiers);
controller.mouseScrolled(mouseX, mouseY, horizontal, vertical);
controller.keyPressed(keyCode, scanCode, modifiers, displayName);
controller.charTyped(chr, modifiers);
```

## Draw command model

Components submit renderer-neutral draw commands. Your backend translates them to the target renderer:

| Core command/material | Backend responsibility |
|---|---|
| `Material.SDF_RECT` | Rounded rectangles, borders, shadows, and widget chrome. |
| `Material.HSV_SV` | Saturation/value square used by the color picker. |
| `Material.HSV_HUE` | Hue strip used by the color picker. |
| `Material.CHECKERBOARD` | Transparency preview backgrounds. |
| `TextCommand` | Text rendering using your text renderer. |
| `setClip` / `clearClip` | Scissor/clipping support. |

## Adapter rule

Keep config classes, state objects, `AutoConfig`, `ConfigScreenBuilder`, and persistence unchanged. Replace only the adapter and render backend that translate draw commands into the target Minecraft version or loader.

## Minimal custom screen flow

```java
public final class MyConfigHostScreen {
    private final UiRoot root;
    private final MyAdapter adapter = new MyAdapter();
    private final MinecraftUiController<MyDrawContext> controller;

    public MyConfigHostScreen(UiRoot root) {
        this.root = root;
        this.controller = new MinecraftUiController<>(root, adapter);
    }

    public void render(MyDrawContext context, float deltaSeconds) {
        controller.render(context, deltaSeconds);
    }

    public void close() {
        controller.close();
    }
}
```

Call `controller.close()` when the host screen closes so `UiRoot` close callbacks, including config store flushing, run correctly.
