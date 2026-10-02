---
layout: default
title: Custom screen rendering utils
description: Reuse CrystalConfig's batched rounded rectangles, MSDF text, buttons, toggles, and other visual primitives in your own Minecraft screens.
---

# Custom screen rendering utils

CrystalConfig exposes the same batched SDF rectangle and MSDF text renderer used by its config screens for rendering your own UI. This API is rendering-only: your mod owns values, mouse/key handling, animations, and any persistence.

The stable helper surface lives in the `render-api` Gradle module under `dev.someoneok.crystalconfig.api.render`. The normal published CrystalConfig jar already shades that module, so downstream mods keep using the standard dependency.

For a full Minecraft `Screen` subclass, normal mouse input callbacks, and the complete helper categories, see [Renderer utils inside a Screen]({{ '/pages/render-utils-screen/' | relative_url }}).

## Minecraft entry point

Create one `MinecraftUiRenderUtils` per screen and reuse it:

```java
import dev.someoneok.crystalconfig.api.render.ButtonVariant;
import dev.someoneok.crystalconfig.api.render.UiControlState;
import dev.someoneok.crystalconfig.render.MinecraftUiRenderUtils;
import dev.someoneok.crystalconfig.render.Rect;

private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create();
```

Inside the screen render-state extraction:

```java
@Override
public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
    super.extractRenderState(graphics, mouseX, mouseY, tickDelta);

    Rect panel = new Rect(24, 24, 220, 132);
    Rect button = new Rect(40, 108, 92, 28);
    Rect toggle = new Rect(184, 56, 42, 20);

    UiControlState buttonState = UiControlState.of(
            button.contains(mouseX, mouseY),
            buttonHeld && button.contains(mouseX, mouseY),
            false,
            true
    );

    ui.render(graphics, width, height, draw -> {
        draw.panel(panel);
        draw.text("Custom screen", 40, 42);
        draw.button(button, "Apply", buttonState, ButtonVariant.ACCENT);
        draw.toggle(
                toggle,
                enabled,
                UiControlState.of(toggle.contains(mouseX, mouseY), false, false, true)
        );
    });
}
```

`buttonHeld` and `enabled` belong to your screen/mod. Update them from normal Minecraft input callbacks. CrystalConfig does not bind or save them.

## Helpers

`UiRenderUtils` includes rounded/outlined rectangles, circles, points, arbitrary/horizontal/vertical lines, batched gradients, themed panels, clipping, formatted and plain MSDF text, text measurement/alignment, default/accent/danger buttons, text/number inputs, toggles, checkboxes, separators, progress bars, and batched textures/images.

Texture drawing uses the same Minecraft `GUI_TEXTURED` pipeline as normal GUI blits, while consecutive sprites sharing a texture/filter/clip are grouped into one render state:

```java
TextureRef icons = TextureRef.of("mymod", "textures/gui/icons.png");

draw.image(icons, new Rect(20, 20, 32, 32));
draw.blit("mymod:textures/gui/icons.png", 60, 20, 0, 32, 16, 16, 256, 256);
draw.imageRegion(icons, new Rect(84, 20, 32, 32), 16, 32, 16, 16, 256, 256);
```

For scaled artwork, `imageUv(...)` and the full image overload support `TextureFilter.LINEAR`; normal Minecraft GUI sprites default to `TextureFilter.NEAREST`.

Controls also accept explicit `0..1` animation amounts when your mod owns animation state:

```java
draw.button(buttonBounds, "Apply", hoverAmount, pressAmount, false, true, ButtonVariant.ACCENT);
draw.toggle(toggleBounds, toggleAmount, UiControlState.HOVERED);
draw.checkbox(checkBounds, checkAmount, UiControlState.NORMAL);
```

## Text and number inputs

The input helpers render fields but never own or save the value:

```java
draw.textInput(nameBounds, name, "Name", cursor, anchor, nameState);

draw.numberInput(
        amountBounds,
        amountText,
        "0",
        amountCursor,
        amountAnchor,
        amountState,
        TextInputEdit.isValidNumber(amountText, true, 0, 1000)
);
```

Use the immutable `TextInputEdit` helper from your normal screen input callbacks:

```java
TextInputEdit edit = TextInputEdit.of(name, cursor, anchor).insert(typed, 64);
name = edit.value();
cursor = edit.cursor();
anchor = edit.selectionAnchor();
```

For numbers, `insertNumber(...)` filters simple numeric input while `isValidNumber(...)` and `parsedNumber()` let the host decide when to commit/save. The renderer owns no persistent input value.

## Themes

The Minecraft wrapper defaults to `ThemePresets.darkCrimson()` and can use any CrystalConfig theme:

```java
private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create()
        .theme(ThemePresets.midnightSteel())
        .scale(1.1f)
        .textShadow(false);
```

## Performance model

The helper API writes into CrystalConfig's existing `DrawList`. Compatible SDF rectangles are still z-sorted and batched before the Fabric backend receives them, and text uses the same cached MSDF font renderer as config screens. Reuse the renderer instance instead of constructing one every frame.

For mostly-static screens, cached-frame replay is also available:

```java
private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create()
        .frameCaching(true);

// Whenever something visible changes:
ui.invalidate();
```

The first render records the already-batched backend operations. Unchanged frames replay them without rerunning the draw callback or rebuilding/sorting the command list. Viewport/theme/scale/text-shadow changes rebuild automatically. Invalidate mouse-dependent hover state and every advancing animation frame yourself; application updates should stay outside the draw callback because cached replay skips it.
