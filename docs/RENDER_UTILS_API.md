# Renderer Utilities API

CrystalConfig exposes the same SDF rectangle batching and MSDF text renderer used by the config UI for custom mod screens. The API is intentionally rendering-only: it does not bind values, save config data, dispatch input, or own application state.

## Module boundary

The public renderer helpers live in the standalone `render-api` module:

```text
render-api
  -> core renderer-neutral draw commands/theme types

crystal-config
  -> render-api
  -> Fabric/Minecraft backend + shaders + MSDF assets
```

The published `CrystalConfig` jar still shades the renderer API, so downstream Fabric mods use the normal CrystalConfig dependency. The separate Gradle module exists to keep the public surface small and maintainable.

For a complete `Screen` subclass with input handling and every public draw-helper category, see [Using Renderer Utilities Inside a Minecraft Screen](RENDER_UTILS_SCREEN_GUIDE.md).

## Minecraft usage

Create one `MinecraftUiRenderUtils` instance per screen and reuse it across frames:

```java
import dev.someoneok.crystalconfig.render.MinecraftUiRenderUtils;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.api.render.ButtonVariant;
import dev.someoneok.crystalconfig.api.render.UiControlState;

private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create();
private boolean enabled = true;
private boolean buttonHeld;

@Override
public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
    super.extractRenderState(graphics, mouseX, mouseY, tickDelta);

    Rect panel = new Rect(24, 24, 220, 132);
    Rect button = new Rect(40, 108, 92, 28);
    Rect toggle = new Rect(184, 56, 42, 20);

    boolean buttonHovered = button.contains(mouseX, mouseY);
    UiControlState buttonState = UiControlState.of(
            buttonHovered,
            buttonHeld && buttonHovered,
            false,
            true
    );

    ui.render(graphics, width, height, draw -> {
        draw.panel(panel);
        draw.text("Custom screen", 40, 42);
        draw.button(button, "Apply", buttonState, ButtonVariant.ACCENT);
        draw.toggle(toggle, enabled, UiControlState.of(
                toggle.contains(mouseX, mouseY),
                false,
                false,
                true
        ));
    });
}
```

Input remains in the host screen. For example, a mod can update `buttonHeld` from mouse press/release callbacks and flip `enabled` when the toggle rectangle is clicked. The renderer never stores either value.

## Drawing helpers

`UiRenderUtils` currently provides:

- solid, rounded, outlined rounded rectangles, circles, points, and arbitrary-thickness lines
- horizontal/vertical line helpers and rectangular outlines
- batched horizontal and vertical gradients
- highly batched GUI textures/images, including atlas regions, normalized UVs, tinting, and nearest/linear sampling
- theme-backed panels and separators
- formatted MSDF text, plain text, centered/right-aligned text, and text measurement
- clipping scopes
- default/accent/danger buttons
- stateless text and number input rendering with caret, selection, placeholder, validation, and clipping
- a small immutable `TextInputEdit` helper for caller-owned editing state
- toggles and checkboxes, including explicit `0..1` animation amounts owned by the host
- progress bars
- rectangle hit-testing convenience through `contains`
- direct `SdfRectStyle` rendering for lower-level styling

All helpers write to the existing `DrawList`. Compatible SDF rectangles, lines, gradients, and image commands remain batched before they reach the Minecraft backend.

## Text and number inputs

Inputs follow the same renderer-only rule as the rest of the API: **your screen owns the value**. CrystalConfig only renders the field and provides optional editing helpers; it never stores or saves the setting.

A text field can be drawn with only a value and visual state:

```java
Rect nameBox = new Rect(24, 60, 180, 28);
draw.textInput(nameBox, playerName, "Player name", nameState);
```

For a real editable field, keep the caret/selection in the screen and pass them in:

```java
draw.textInput(
        nameBox,
        playerName,
        "Player name",
        nameCursor,
        nameSelectionAnchor,
        UiControlState.of(nameHovered, false, nameFocused, true)
);
```

`TextInputEdit` keeps event handling small without becoming a state container:

```java
TextInputEdit edit = TextInputEdit.of(playerName, nameCursor, nameSelectionAnchor)
        .insert(typedText, 64);

playerName = edit.value();
nameCursor = edit.cursor();
nameSelectionAnchor = edit.selectionAnchor();
ui.invalidate(); // when cached-frame replay is enabled
```

It also includes `backspace()`, `delete()`, `moveLeft(...)`, `moveRight(...)`, `home(...)`, `end(...)`, `selectAll()`, `selectedText()`, `cutSelection()`, and `replaceSelection(...)`.

Number inputs use a string draft while the user is typing. This permits temporary values such as `-` or `12.` without forcing the renderer to own a numeric model:

```java
boolean valid = TextInputEdit.isValidNumber(amountText, true, 0.0, 1000.0);

draw.numberInput(
        amountBox,
        amountText,
        "0",
        amountCursor,
        amountSelectionAnchor,
        UiControlState.of(amountHovered, false, amountFocused, true),
        valid
);
```

Filter typed/pasted input with:

```java
TextInputEdit edit = TextInputEdit.of(amountText, amountCursor, amountSelectionAnchor)
        .insertNumber(typedText, true, false, 16); // decimals allowed, negatives disallowed
```

When the dev wants to commit/save the number, `parsedNumber()` returns an `OptionalDouble`; the mod decides what to do with it. `isValidNumber(...)` can enforce decimal/integer mode and a min/max range. An invalid field gets the theme danger border.

The focused caret is intentionally static instead of blinking, so a focused but unchanged field remains compatible with whole-frame caching. Invalidate only when the value, focus, selection, hover state, or other visible state changes.

## Basic primitives

The lower-level primitive helpers cover the common operations normally expected from a GUI renderer:

```java
draw.rect(new Rect(20, 20, 120, 30), ColorRGBA.rgba(20, 20, 24, 255));
draw.outlineRect(new Rect(20, 20, 120, 30), ColorRGBA.WHITE, 1.0f);

draw.line(20, 70, 180, 105, 2.0f, true, ColorRGBA.WHITE);
draw.horizontalLine(20, 180, 120, 1.0f, ColorRGBA.WHITE);
draw.verticalLine(200, 20, 120, 1.0f, ColorRGBA.WHITE);
draw.point(220, 70, 4.0f, ColorRGBA.WHITE);

draw.verticalGradient(bounds, topColor, bottomColor);
draw.horizontalGradient(bounds, leftColor, rightColor);
```

Arbitrary lines use a batched SDF quad path, so many consecutive lines with the same clip become one Minecraft GUI render state instead of one state per line. Set `roundCaps` to `true` for anti-aliased rounded endpoints.

## Images and textures

The image API uses the Minecraft `GUI_TEXTURED` pipeline used by normal GUI blits, but batches consecutive draws that share texture, filter, and clip into one `GuiElementRenderState`. This is intentionally more efficient than blindly issuing one `GuiGraphicsExtractor.blit(...)` call for every sprite.

Whole texture:

```java
TextureRef logo = TextureRef.of("mymod", "textures/gui/logo.png");
draw.image(logo, new Rect(24, 24, 128, 64));

// Convenient string form; UiRenderUtils caches the parsed TextureRef.
draw.image("mymod:textures/gui/logo.png", new Rect(24, 24, 128, 64));
```

Minecraft-style `blit` source coordinates:

```java
draw.blit(
        "mymod:textures/gui/widgets.png",
        24, 100,      // destination x/y
        0, 32,        // source u/v in pixels
        64, 16,       // destination + source-region width/height
        256, 256      // full texture width/height
);
```

When the destination size differs from the atlas region size, use `imageRegion(...)`:

```java
draw.imageRegion(
        TextureRef.of("mymod:textures/gui/widgets.png"),
        new Rect(24, 100, 128, 32), // destination
        0, 32,                      // source u/v
        64, 16,                     // source region size
        256, 256                    // texture size
);
```

The lowest-level variant accepts normalized UVs, tint, filter, and z:

```java
draw.imageUv(
        atlas,
        destination,
        u0, v0, u1, v1,
        ColorRGBA.WHITE,
        TextureFilter.LINEAR,
        2.0f
);
```

Use `TextureFilter.NEAREST` for normal Minecraft GUI sprites/pixel art and `TextureFilter.LINEAR` when scaled artwork should be smoothed. Prefer reusing a `TextureRef` field in very hot dynamic screens; the string overload also keeps a small per-renderer parsed-ID cache. The Minecraft backend separately caches resolved `Identifier` instances and texture/sampler setups, while detecting texture-view replacement after a resource reload.

Texture commands also participate in `frameCaching(true)`. On an unchanged cached screen, the host draw callback and image command construction are skipped just like shapes and text.

## Themes and scale

The Minecraft wrapper defaults to `ThemePresets.darkCrimson()` and scale `1.0`:

```java
private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create()
        .theme(ThemePresets.midnightSteel())
        .scale(1.1f)
        .textShadow(false);
```

A custom `Theme` can be supplied exactly like a config screen theme.

## Opt-in whole-frame caching

`MinecraftUiRenderUtils` can record the fully batched backend operations for a frame and replay them on later Minecraft frames without invoking the draw callback again. This skips host draw-code execution, command allocation, z sorting, and batch construction for unchanged UI.

Caching is deliberately opt-in so existing screens keep immediate hover/animation behavior:

```java
private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create()
        .frameCaching(true);
```

When any host-owned value that affects pixels changes, invalidate the cache:

```java
enabled = !enabled;
ui.invalidate();
```

The next `ui.render(...)` call rebuilds and records the frame. Later calls replay it until the next invalidation. Width, height, UI scale, theme, renderer scale, and text-shadow changes invalidate/rebuild automatically.

Useful cache methods are:

```java
ui.frameCaching(true);      // enable cached replay
ui.frameCaching(false);     // disable it and drop the recorded frame
ui.frameCaching();          // current setting
ui.invalidate();            // rebuild on the next render
ui.clearFrameCache();       // drop recorded operations immediately
ui.hasValidCachedFrame();   // valid cache for the last rendered viewport/settings
```

If hover visuals depend on `mouseX`/`mouseY`, invalidate when the mouse position changes. If an animation is advancing, invalidate on each frame for the duration of that animation. Do not advance application state from inside the draw callback when caching is enabled because the callback is skipped during replay.

The cache stores already-batched renderer operations, not a screenshot or texture. Replay still submits the shapes/text into the current `GuiGraphicsExtractor`, preserving the normal Minecraft render-state pipeline while avoiding rebuilding the UI command list.

## Host-owned animations

The utility layer deliberately does not keep animation state. If a host already has an animation value, pass it directly:

```java
float toggleAmount = animationValue; // 0..1, owned by the host mod

draw.button(buttonBounds, "Apply", hoverAmount, pressAmount, false, true, ButtonVariant.ACCENT);
draw.toggle(toggleBounds, toggleAmount, UiControlState.HOVERED);
draw.checkbox(checkBounds, checkAmount, UiControlState.NORMAL);
```

This keeps the API useful for rendering without turning it into another widget/state framework.

## Renderer-neutral usage

The `render-api` module itself does not import Minecraft. Other backends can use `UiRenderUtils` directly with any `RenderBackend`:

```java
UiRenderUtils renderer = new UiRenderUtils();

renderer.render(
        backend,
        new RenderFrame(width, height, uiScale, deltaSeconds),
        theme,
        1.0f,
        false,
        draw -> {
            draw.roundedRect(10, 10, 120, 40, theme.palette().surface(), 8);
            draw.text("Hello", 24, 23);
        }
);
```

The callback form guarantees that queued commands are flushed and the backend frame is ended even when host drawing code throws.
