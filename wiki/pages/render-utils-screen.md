---
layout: default
title: Renderer utils inside a Screen
description: Use MinecraftUiRenderUtils as a reusable field in a Minecraft Screen, with rendering and normal Screen input callbacks.
---

# Renderer utils inside a `Screen`

Yes. A normal Minecraft `Screen` is the intended host for `MinecraftUiRenderUtils`. Keep one renderer instance as a field on the screen, reuse it across frames, and draw inside `extractRenderState(...)`.

For the renderer/module overview, see [Custom screen rendering utils]({{ '/pages/render-utils/' | relative_url }}).

## Basic pattern

```java
public final class MyScreen extends Screen {
    private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create();
    private final Rect buttonBounds = new Rect(40, 108, 100, 28);
    private final Rect toggleBounds = new Rect(200, 56, 42, 20);
    private boolean enabled = true;
    private boolean buttonHeld;

    public MyScreen() {
        super(Component.literal("My screen"));
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickDelta
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);

        Rect panel = new Rect(24, 24, 240, 140);
        boolean buttonHovered = buttonBounds.contains(mouseX, mouseY);
        float deltaSeconds = Math.max(0.0f, Math.min(0.25f, tickDelta / 20.0f));

        ui.render(graphics, width, height, deltaSeconds, draw -> {
            draw.panel(panel);
            draw.text("Custom UI", 40, 42);
            draw.button(
                    buttonBounds,
                    "Apply",
                    UiControlState.of(buttonHovered, buttonHeld && buttonHovered, false, true),
                    ButtonVariant.ACCENT
            );
            draw.toggle(
                    toggleBounds,
                    enabled,
                    UiControlState.of(toggleBounds.contains(mouseX, mouseY), false, false, true)
            );
        });
    }
}
```

`MinecraftUiRenderUtils` is the frame wrapper. The actual drawing methods are on the `draw` object passed into `ui.render(...)`. This keeps drawing inside a valid begin/end-frame scope and lets the renderer batch commands correctly.

## Input is still normal `Screen` input

Buttons/toggles from the renderer API are visual helpers, not Minecraft widgets. Handle clicks in the screen and update your own state:

```java
@Override
public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    if (event.buttonInfo().button() == 0) {
        float mouseX = (float) event.x();
        float mouseY = (float) event.y();

        if (toggleBounds.contains(mouseX, mouseY)) {
            enabled = !enabled;
            return true;
        }

        if (buttonBounds.contains(mouseX, mouseY)) {
            buttonHeld = true;
            return true;
        }
    }

    return super.mouseClicked(event, doubleClick);
}

@Override
public boolean mouseReleased(MouseButtonEvent event) {
    if (event.buttonInfo().button() == 0 && buttonHeld) {
        boolean activate = buttonBounds.contains((float) event.x(), (float) event.y());
        buttonHeld = false;
        if (activate) runAction();
        return true;
    }

    return super.mouseReleased(event);
}
```

The same applies to `mouseDragged`, `mouseScrolled`, `keyPressed`, and `charTyped`: the screen/controller owns the input, then passes visual state to the renderer.

## Available draw helpers

Inside `ui.render(...)`, `UiRenderUtils` provides:

- `rect`, `roundedRect`, `outlinedRoundedRect`, `outlineRect`, `circle`, and `point`
- arbitrary `line`, `horizontalLine`, and `verticalLine` primitives
- `verticalGradient` and `horizontalGradient`
- `image`, `texture`, `blit`, `imageRegion`, and `imageUv` for batched GUI textures
- `panel`, `separator`, and `progressBar`
- `button`, `toggle`, `checkbox`, `textInput`, and `numberInput`
- `text`, `plainText`, `centeredText`, and `rightAlignedText`
- `measureText`
- `clip`
- `contains`
- `theme`
- optional explicit z overloads for draw ordering
- low-level `SdfRectStyle` rendering through `roundedRect(Rect, SdfRectStyle, z)`

Buttons support `ButtonVariant.DEFAULT`, `ACCENT`, and `DANGER`. `UiControlState` can represent normal, hovered, pressed, focused, and disabled visual states. Texture helpers accept renderer-neutral `TextureRef` values; `imageUv(...)` also supports tinting and `TextureFilter.NEAREST`/`LINEAR`. Consecutive sprites with the same texture/filter/clip are submitted as one Minecraft `GUI_TEXTURED` render state.

`Rect` is also useful for screen layout and hit testing. It provides `right()`, `bottom()`, `centerX()`, `centerY()`, `contains(...)`, `intersects(...)`, `intersect(...)`, `inset(...)`, `move(...)`, `withWidth(...)`, `withHeight(...)`, and `isEmpty()`.

## Simple editable inputs

Keep the value and caret in the screen, then pass them to the renderer:

```java
private String search = "";
private int searchCursor;
private int searchAnchor;
private boolean searchFocused;

// render callback
draw.textInput(
        searchBounds,
        search,
        "Search...",
        searchCursor,
        searchAnchor,
        UiControlState.of(searchBounds.contains(mouseX, mouseY), false, searchFocused, true)
);
```

Apply normal keyboard/character events with `TextInputEdit`:

```java
TextInputEdit edit = TextInputEdit.of(search, searchCursor, searchAnchor)
        .insert(typedText, 128);
search = edit.value();
searchCursor = edit.cursor();
searchAnchor = edit.selectionAnchor();
ui.invalidate();
```

The same helper provides backspace/delete/caret movement/selection. `insertNumber(...)`, `isValidNumber(...)`, and `parsedNumber()` cover the common numeric-input case without introducing a saved/config-bound value.

## Wrapper configuration

Configure the renderer once when the screen is created:

```java
private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create()
        .theme(ThemePresets.midnightSteel())
        .scale(1.0f)
        .textShadow(false);
```

The wrapper exposes theme, scale, text-shadow settings, cached-frame controls, and three `render(...)` overloads. Shape/text/control methods intentionally remain on the render callback's `draw` object.

## Cached-frame replay

Caching is opt-in and works well for screens that sit unchanged for many Minecraft frames:

```java
private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create()
        .frameCaching(true);

private int lastMouseX = Integer.MIN_VALUE;
private int lastMouseY = Integer.MIN_VALUE;

@Override
public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
    super.extractRenderState(graphics, mouseX, mouseY, tickDelta);

    if (mouseX != lastMouseX || mouseY != lastMouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        ui.invalidate();
    }

    ui.render(graphics, width, height, tickDelta / 20.0f, draw -> {
        // This callback is skipped when the cached frame is replayed.
        draw.panel(panelBounds());
        // ...
    });
}
```

Call `ui.invalidate()` after any click/key/scroll/state change that changes the pixels. While an animation is advancing, update it outside `ui.render(...)` and invalidate every frame; once it settles, stop invalidating and the final frame will be replayed. Width/height/UI-scale changes and changed theme/scale/text-shadow settings rebuild automatically.

The public cache controls are `frameCaching(boolean)`, `frameCaching()`, `invalidate()`, `clearFrameCache()`, and `hasValidCachedFrame()`. The cache stores batched render operations rather than a screenshot, so replay still goes through the current Minecraft render-state extraction.

## Mixing with vanilla widgets

Call:

```java
super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
```

when the screen also contains vanilla `Renderable` widgets. Minecraft's base `Screen.extractRenderState(...)` renders the registered renderable list; your CrystalConfig render call can then add the custom batched UI.

## Important rules

- Reuse one `MinecraftUiRenderUtils` per screen.
- Prefer one `ui.render(...)` call per frame.
- Never save the callback `draw` object and use it later.
- Do not call drawing methods outside `ui.render(...)`.
- Do not nest `ui.render(...)` on the same renderer instance.
- Keep values, clicks, focus, and animations in your own screen/controller.
- Pass seconds to the delta-time overload; `tickDelta / 20.0f` is a suitable conversion for the screen value used here.
- Treat the renderer as render-thread-only and not thread-safe.
- With cached replay enabled, invalidate every change that affects pixels.
- Do not update application/animation state inside the draw callback when caching is enabled because replay skips that callback.

For a complete screen with panel layout, button press/release handling, toggle, checkbox, progress bar, theme setup, and closing back to a parent screen, see the repository documentation page `docs/RENDER_UTILS_SCREEN_GUIDE.md`.
