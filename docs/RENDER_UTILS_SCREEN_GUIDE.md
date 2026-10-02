# Using Renderer Utilities Inside a Minecraft Screen

This guide shows how to keep CrystalConfig's renderer as a normal field on a Minecraft `Screen` and use it from the screen lifecycle. It is intended for custom screens that want CrystalConfig's SDF shapes, MSDF text, and stateless controls without using the config/state system.

For the lower-level API/module overview, see [Renderer Utilities API](RENDER_UTILS_API.md).

## The screen owns the renderer

Create one `MinecraftUiRenderUtils` instance as a field on the screen and reuse it for the lifetime of that screen:

```java
private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create();
```

Do not create the renderer every frame. The instance owns reusable renderer/backend buffers, but it does not own your values, click state, focus state, animation values, or persistence.

`MinecraftUiRenderUtils` is the Minecraft frame wrapper. Drawing methods are exposed through the `UiRenderUtils draw` object passed to `ui.render(...)`:

```java
ui.render(graphics, width, height, draw -> {
    draw.roundedRect(...);
    draw.text(...);
    draw.button(...);
});
```

This is intentional. Drawing commands are only valid between the renderer's begin/end-frame calls, so the callback prevents drawing outside a valid render frame.

## Complete `Screen` example

The example below targets the same Minecraft 26.1 screen API used by CrystalConfig.

```java
package com.example.mod.client.screen;

import dev.someoneok.crystalconfig.api.render.ButtonVariant;
import dev.someoneok.crystalconfig.api.render.UiControlState;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.MinecraftUiRenderUtils;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.theme.ThemePresets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class ExampleRendererScreen extends Screen {
    private final Screen parent;

    // Keep one renderer for this screen and reuse it across frames.
    private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create()
            .theme(ThemePresets.darkCrimson())
            .scale(1.0f)
            .textShadow(false);

    // These are normal screen/application values. CrystalConfig does not save them.
    private boolean enabled = true;
    private boolean rememberChoice;
    private boolean actionHeld;
    private float progress = 0.65f;

    public ExampleRendererScreen(Screen parent) {
        super(Component.literal("Renderer example"));
        this.parent = parent;
    }

    private Rect panelBounds() {
        return new Rect((width - 300.0f) * 0.5f, (height - 190.0f) * 0.5f, 300.0f, 190.0f);
    }

    private Rect actionButtonBounds() {
        Rect panel = panelBounds();
        return new Rect(panel.x() + 20.0f, panel.bottom() - 48.0f, 110.0f, 28.0f);
    }

    private Rect toggleBounds() {
        Rect panel = panelBounds();
        return new Rect(panel.right() - 62.0f, panel.y() + 56.0f, 42.0f, 20.0f);
    }

    private Rect checkboxBounds() {
        Rect panel = panelBounds();
        return new Rect(panel.right() - 40.0f, panel.y() + 94.0f, 20.0f, 20.0f);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float tickDelta
    ) {
        // Keep this if the screen also contains vanilla Renderable widgets.
        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);

        Rect panel = panelBounds();
        Rect actionButton = actionButtonBounds();
        Rect toggle = toggleBounds();
        Rect checkbox = checkboxBounds();

        boolean actionHovered = actionButton.contains(mouseX, mouseY);
        boolean toggleHovered = toggle.contains(mouseX, mouseY);
        boolean checkboxHovered = checkbox.contains(mouseX, mouseY);

        UiControlState actionState = UiControlState.of(
                actionHovered,
                actionHeld && actionHovered,
                false,
                true
        );

        // Minecraft supplies a partial-tick style value here. The renderer overload expects seconds.
        float deltaSeconds = Math.max(0.0f, Math.min(0.25f, tickDelta / 20.0f));

        ui.render(graphics, width, height, deltaSeconds, draw -> {
            draw.panel(panel);

            draw.text("Custom mod screen", panel.x() + 20.0f, panel.y() + 18.0f);
            draw.text(
                    "This screen owns all state and input.",
                    panel.x() + 20.0f,
                    panel.y() + 38.0f,
                    12.0f,
                    draw.theme().palette().mutedText()
            );

            draw.separator(new Rect(panel.x() + 20.0f, panel.y() + 50.0f, panel.w() - 40.0f, 1.0f));

            draw.text("Enabled", panel.x() + 20.0f, panel.y() + 60.0f);
            draw.toggle(
                    toggle,
                    enabled,
                    UiControlState.of(toggleHovered, false, false, true)
            );

            draw.text("Remember choice", panel.x() + 20.0f, panel.y() + 98.0f);
            draw.checkbox(
                    checkbox,
                    rememberChoice,
                    UiControlState.of(checkboxHovered, false, false, true)
            );

            Rect progressBounds = new Rect(panel.x() + 20.0f, panel.y() + 128.0f, panel.w() - 40.0f, 10.0f);
            draw.progressBar(progressBounds, progress);

            draw.button(actionButton, "Run action", actionState, ButtonVariant.ACCENT);

            draw.roundedRect(
                    panel.right() - 54.0f,
                    panel.bottom() - 43.0f,
                    34.0f,
                    18.0f,
                    ColorRGBA.hex("#FFFFFF18"),
                    6.0f
            );
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.buttonInfo().button() == 0) {
            float mouseX = (float) event.x();
            float mouseY = (float) event.y();

            if (toggleBounds().contains(mouseX, mouseY)) {
                enabled = !enabled;
                return true;
            }

            if (checkboxBounds().contains(mouseX, mouseY)) {
                rememberChoice = !rememberChoice;
                return true;
            }

            if (actionButtonBounds().contains(mouseX, mouseY)) {
                actionHeld = true;
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.buttonInfo().button() == 0 && actionHeld) {
            boolean activate = actionButtonBounds().contains((float) event.x(), (float) event.y());
            actionHeld = false;

            if (activate) {
                runAction();
            }
            return true;
        }

        return super.mouseReleased(event);
    }

    private void runAction() {
        // Your mod logic goes here.
        progress = progress >= 1.0f ? 0.0f : Math.min(1.0f, progress + 0.1f);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
```

Open it like any other Minecraft screen:

```java
Minecraft.getInstance().setScreen(new ExampleRendererScreen(Minecraft.getInstance().screen));
```

## How this fits the `Screen` lifecycle

For this Minecraft version, `Screen.extractRenderStateWithTooltipAndSubtitles(...)` calls the background extraction first, then calls `extractRenderState(...)`, then extracts deferred elements. A custom screen therefore only needs to override `extractRenderState(...)` for normal foreground UI rendering.

The base `Screen.extractRenderState(...)` renders objects registered through Minecraft's normal renderable/widget lists. If your custom screen mixes CrystalConfig drawing with vanilla widgets, call `super.extractRenderState(...)`. If the screen has no vanilla renderables, the call is harmless and keeping it makes future widget additions safer.

Input stays in the normal `Screen` callbacks such as `mouseClicked`, `mouseReleased`, `mouseDragged`, `mouseScrolled`, `keyPressed`, and `charTyped`. Feed the resulting visual state into `UiControlState` or your own `0..1` animation amounts when rendering.

## Methods available inside `ui.render(...)`

The callback receives `UiRenderUtils`, which currently exposes the following rendering surface.

### Shapes and containers

- `rect(...)` - solid rectangles, with `Rect` or x/y/width/height overloads and optional z.
- `roundedRect(...)` - rounded solid rectangles, plus a low-level `SdfRectStyle` overload.
- `outlinedRoundedRect(...)` - fill + border + radius, with optional z.
- `outlineRect(...)` - simple rectangular outline.
- `circle(...)` and `point(...)` - basic point/circle helpers.
- `line(...)` - arbitrary anti-aliased line with thickness, optional round caps, and optional z.
- `horizontalLine(...)` / `verticalLine(...)` - axis-aligned convenience helpers.
- `verticalGradient(...)` / `horizontalGradient(...)` - batched two-color gradients.
- `panel(...)` - theme-backed surface/border panel.
- `separator(...)` - theme-backed one-pixel separator.
- `progressBar(...)` - theme-backed progress value clamped to `0..1`.


### Images and textures

- `image(...)` - full-texture image by `TextureRef` or `namespace:path` string.
- `blit(...)` - familiar Minecraft-style pixel-space u/v helper.
- `imageRegion(...)` - atlas region where destination size can differ from source-region size.
- `imageUv(...)` - normalized UVs with tint, `TextureFilter.NEAREST`/`LINEAR`, and z.
- `texture(...)` - alias for full-texture `image(...)`.

Example:

```java
private static final TextureRef ICONS =
        TextureRef.of("mymod", "textures/gui/icons.png");

ui.render(graphics, width, height, draw -> {
    draw.image(ICONS, new Rect(24, 24, 32, 32));
    draw.imageRegion(ICONS, new Rect(64, 24, 64, 64), 32, 0, 16, 16, 256, 256);
});
```

The Minecraft backend uses `RenderPipelines.GUI_TEXTURED`, matching normal GUI blit semantics. Consecutive images with the same texture/filter/clip are collapsed into one textured render state. Keep atlas sprites adjacent when possible to maximize batching without changing z/draw order.

### Stateless controls

- `button(...)` - default, accent, or danger button visuals.
- `textInput(...)` / `numberInput(...)` - caller-owned input values with caret/selection/validation rendering.
- `toggle(...)` - boolean value or explicit host-owned `0..1` animation amount.
- `checkbox(...)` - boolean value or explicit host-owned `0..1` animation amount.
- `UiControlState.of(hovered, pressed, focused, enabled)` - builds the visual state supplied to controls.

These helpers render controls only. They do not register widgets or dispatch clicks.

### Text and number inputs

Inputs are still normal screen-owned values. A minimal screen can keep just the value, caret, selection anchor, and focus flag:

```java
private String name = "Kuudra";
private int nameCursor = name.length();
private int nameAnchor = nameCursor;
private boolean nameFocused;

private String amountText = "10";
private int amountCursor = amountText.length();
private int amountAnchor = amountCursor;
private boolean amountFocused;
```

Render them like any other control:

```java
Rect nameBox = new Rect(panel.x() + 20, panel.y() + 60, 180, 28);
Rect amountBox = new Rect(panel.x() + 20, panel.y() + 100, 90, 28);

boolean amountValid = TextInputEdit.isValidNumber(amountText, true, 0, 1000);

draw.textInput(
        nameBox, name, "Name", nameCursor, nameAnchor,
        UiControlState.of(nameBox.contains(mouseX, mouseY), false, nameFocused, true)
);

draw.numberInput(
        amountBox, amountText, "0", amountCursor, amountAnchor,
        UiControlState.of(amountBox.contains(mouseX, mouseY), false, amountFocused, true),
        amountValid
);
```

When a field receives text, apply the edit result back to your own fields:

```java
private void typeName(String typed) {
    TextInputEdit edit = TextInputEdit.of(name, nameCursor, nameAnchor)
            .insert(typed, 64);
    name = edit.value();
    nameCursor = edit.cursor();
    nameAnchor = edit.selectionAnchor();
    ui.invalidate();
}

private void typeAmount(String typed) {
    TextInputEdit edit = TextInputEdit.of(amountText, amountCursor, amountAnchor)
            .insertNumber(typed, true, false, 16);
    amountText = edit.value();
    amountCursor = edit.cursor();
    amountAnchor = edit.selectionAnchor();
    ui.invalidate();
}
```

Backspace, delete, arrows, home/end, and select-all are the same pattern:

```java
TextInputEdit edit = TextInputEdit.of(name, nameCursor, nameAnchor).backspace();
// or .delete(), .moveLeft(shiftDown), .moveRight(shiftDown), .home(shiftDown), .end(shiftDown), .selectAll()
```

On mouse click, focus the field in your normal `mouseClicked(...)` callback. For a deliberately simple implementation, place the caret at the end when focus is gained:

```java
if (nameBox.contains((float) event.x(), (float) event.y())) {
    nameFocused = true;
    nameCursor = name.length();
    nameAnchor = nameCursor;
    ui.invalidate();
    return true;
}
```

The helper does not call your config system or save anything. For a number, validate/parse when your screen decides to commit:

```java
TextInputEdit edit = TextInputEdit.of(amountText, amountCursor, amountAnchor);
edit.parsedNumber().ifPresent(value -> saveAmount(value));
```

If cached-frame replay is enabled, call `ui.invalidate()` after input/focus/selection changes. The caret does not blink automatically, which keeps an unchanged focused field cacheable.

### Text

- `text(...)` - formatted MSDF text using the current theme/font defaults or explicit font parameters.
- `plainText(...)` - plain MSDF text without formatted-display parsing.
- `centeredText(...)` - centers measured text inside a `Rect`.
- `rightAlignedText(...)` - positions text from a right edge.
- `measureText(...)` - returns `TextMetrics` for layout.

### Utility/frame helpers

- `theme()` - returns the active `Theme` while inside the render callback.
- `contains(...)` - rectangle hit-test convenience.
- `clip(bounds, draw -> { ... })` - scoped clipping; the previous clip is restored automatically.

Many drawing helpers also expose an explicit `z` overload. Use z only when draw ordering requires it; otherwise the default is enough.

### `Rect` layout helpers

The renderer uses the core `Rect` record for bounds. It includes `right()`, `bottom()`, `centerX()`, `centerY()`, `contains(...)`, `intersects(...)`, `intersect(...)`, `inset(...)`, `move(...)`, `withWidth(...)`, `withHeight(...)`, and `isEmpty()`. These helpers are useful for building screen-relative layouts and sharing the exact same bounds between rendering and input hit tests.

## `MinecraftUiRenderUtils` methods

The screen-owned wrapper exposes:

```java
MinecraftUiRenderUtils.create();
ui.theme(theme);
ui.theme();
ui.scale(scale);
ui.scale();
ui.textShadow(enabled);
ui.textShadow();
ui.frameCaching(enabled);
ui.frameCaching();
ui.invalidate();
ui.clearFrameCache();
ui.hasValidCachedFrame();

ui.render(graphics, width, height, draw -> { ... });
ui.render(graphics, width, height, deltaSeconds, draw -> { ... });
ui.render(graphics, width, height, uiScale, deltaSeconds, draw -> { ... });
```

The wrapper intentionally does not expose `button`, `text`, `roundedRect`, and the other primitives directly. Those methods are only callable on the callback's `draw` object while a render frame is active.

## Opt-in cached-frame replay

For mostly-static custom screens, enable cached replay once on the screen-owned renderer:

```java
private final MinecraftUiRenderUtils ui = MinecraftUiRenderUtils.create()
        .frameCaching(true);

private int lastMouseX = Integer.MIN_VALUE;
private int lastMouseY = Integer.MIN_VALUE;
```

Then invalidate only when something that changes pixels changes. Hover is a common example:

```java
@Override
public void extractRenderState(
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float tickDelta
) {
    super.extractRenderState(graphics, mouseX, mouseY, tickDelta);

    if (mouseX != lastMouseX || mouseY != lastMouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        ui.invalidate();
    }

    ui.render(graphics, width, height, tickDelta / 20.0f, draw -> {
        Rect button = actionButtonBounds();
        boolean hovered = button.contains(mouseX, mouseY);

        draw.panel(panelBounds());
        draw.button(
                button,
                "Apply",
                UiControlState.of(hovered, actionHeld && hovered, false, true),
                ButtonVariant.ACCENT
        );
    });
}
```

After input changes state, invalidate as part of that event:

```java
if (toggleBounds().contains((float) event.x(), (float) event.y())) {
    enabled = !enabled;
    ui.invalidate();
    return true;
}
```

The first render after invalidation runs the draw callback normally and records the resulting batched backend operations. Unchanged frames replay those operations directly and do not invoke the callback. Width/height/UI-scale changes rebuild automatically, and calling `theme(...)`, `scale(...)`, or `textShadow(...)` with a changed value invalidates automatically.

### Animations with caching

While a host-owned animation is moving, invalidate every frame so the callback can emit its new animation amount:

```java
if (!animation.finished()) {
    animation.update(deltaSeconds);
    ui.invalidate();
}
```

Once the animation reaches its resting value, stop invalidating and the final frame becomes replayable. Keep animation/application updates outside the `ui.render(...)` callback; the callback is intentionally skipped on cached frames.

### Manual cache control

- `frameCaching(true/false)` enables or disables replay. It is off by default.
- `invalidate()` marks the recorded frame stale but lets the backend reuse its allocated lists.
- `clearFrameCache()` immediately drops the recorded operations and forces a rebuild.
- `hasValidCachedFrame()` reports whether the current renderer revision has a recorded frame for the last rendered viewport.

The cached data is not a framebuffer screenshot. It is the already-batched shape/text/clip operation stream, replayed into the current `GuiGraphicsExtractor`. This keeps the normal Minecraft render pipeline while avoiding repeated host draw-code execution and command-list rebuilding.

## Clipping example

```java
ui.render(graphics, width, height, draw -> {
    Rect viewport = new Rect(20, 20, 180, 90);
    draw.panel(viewport);

    draw.clip(viewport.inset(8), clipped -> {
        clipped.text("This text is clipped to the inner viewport", 32, 36);
        clipped.roundedRect(32, 58, 260, 24, clipped.theme().palette().accent(), 6);
    });
});
```

## Custom animation example

The renderer accepts animation amounts but does not update them for you:

```java
float hoverAmount = buttonHoverAnimation; // host-owned 0..1 value
float pressAmount = buttonPressAnimation; // host-owned 0..1 value
float toggleAmount = toggleAnimation;     // host-owned 0..1 value

ui.render(graphics, width, height, draw -> {
    draw.button(
            buttonBounds,
            "Animated",
            hoverAmount,
            pressAmount,
            false,
            true,
            ButtonVariant.ACCENT
    );
    draw.toggle(toggleBounds, toggleAmount, UiControlState.HOVERED);
});
```

## Rules to keep the renderer fast and safe

1. Keep one `MinecraftUiRenderUtils` instance per screen instead of recreating it every frame.
2. Call drawing methods only inside the `ui.render(...)` callback.
3. Do not keep or use the callback's `draw` reference after the callback returns.
4. Do not nest `ui.render(...)` calls on the same renderer instance.
5. Keep state/input/animation ownership in the screen or your own controller.
6. Prefer one `ui.render(...)` call per screen frame so compatible commands can batch together.
7. Call `super.extractRenderState(...)` when the screen also uses vanilla `Renderable` widgets.
8. Use `clip(...)` for scroll/view regions instead of manually trying to trim every primitive.
9. Pass seconds, not raw ticks, to the `deltaSeconds` render overload.
10. Treat a renderer instance as render-thread-only; it is not thread-safe.
11. If cached replay is enabled, call `invalidate()` whenever host state, hover, layout, or other pixels change.
12. While an animation is advancing, update it outside the draw callback and invalidate each frame; stop invalidating when it settles.
