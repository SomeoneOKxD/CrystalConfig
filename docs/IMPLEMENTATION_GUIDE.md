# Implementation Guide

## Rendering model

The core UI never calls Minecraft rendering APIs. Components emit draw commands through `RenderContext`:

```java
context.rect(bounds, SdfRectStyle.create()
        .fill(context.theme().palette().surface())
        .border(1, context.theme().palette().border())
        .shadow(18, 0, 4, context.theme().palette().shadow())
        .radius(context.theme().radii().lg()), z);
```

The backend receives those commands during `DrawList.flush(RenderBackend)`.

## Draw batching

`DrawList` sorts commands by `z` when needed. Consecutive `QuadCommand`s with the same `Material` and clip rectangle are sent as one batch to `RenderBackend#drawQuads`.

Text commands are flushed separately so the backend can use Minecraft text rendering or the MSDF text renderer.

## Materials

| Material | Used for |
|---|---|
| `SDF_RECT` | Panels, buttons, sliders, checkboxes, dropdown rows, shadows, and borders. |
| `HSV_SV` | Color picker saturation/value square. |
| `HSV_HUE` | Color picker hue strip. |
| `CHECKERBOARD` | Transparency preview backgrounds. |

Rounded corners, borders, and soft shadows are shader responsibilities. Widgets submit rectangles instead of tessellated arcs.

## Component model

A `Component` owns bounds, size hints, visibility/enabled state, focus, hover, pressed state, tooltip text, child components, and input handlers.

```java
public final class MyWidget extends Component {
    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return constraints.clamp(new Size(120, 28));
    }

    @Override
    protected void renderSelf(RenderContext context) {
        context.rect(bounds, context.theme().palette().surfaceAlt(), context.theme().radii().md(), z);
    }
}
```

Use containers for layout:

- `Row` for horizontal flex-like layout
- `Column` for vertical layout
- `Stack` for absolute/fill positioning
- `ScrollContainer` for clipped vertical scrolling
- `Panel` for themed column surfaces
- `WrappingIconRow` for footer icon rows

## Input dispatch

`UiRoot` performs hit testing and dispatches events through the component tree.

```text
mouse press -> topmost hit component -> capture phase -> bubble phase
mouse drag  -> captured component
key/char    -> focused component
scroll      -> hovered component -> bubble phase
```

Return `true` from an event handler to consume the event.

## State binding

Widgets operate on `State<T>`, not on config files or Minecraft classes.

```java
State<Boolean> fancyUi = Binding.of(
        () -> ClientConfig.fancyUi,
        value -> ClientConfig.fancyUi = value
);
```

Use `MutableState<T>` for values owned by the UI/config model.

## Themes

`Theme` contains:

- `Palette`
- `SpacingScale`
- `FontScale`
- `Radii`
- `AnimationSpec`

Register custom themes on `ConfigUiSettings`:

```java
model.configureSettings(settings -> settings
        .registerTheme(MyThemes.KUUDRA)
        .defaultTheme(MyThemes.KUUDRA));
```

## Performance notes

- Keep component instances stable and update state instead of rebuilding the tree every frame.
- Use `markLayoutDirty()` when a size-affecting property changes.
- Prefer batched quad materials over custom per-widget rendering paths.
- Cache expensive backend measurements where possible. `RenderContext` already caches text metrics for the current frame.
- Avoid allocating inside render loops in backend code.

## Minecraft-only sound option

The Fabric module includes a sound picker for `models.someoneok.crystalconfig.SoundSetting`.

Register it once during client init:

```java
MinecraftAutoConfig.register();
```

```java
import state.someoneok.crystalconfig.MutableState;
import autoconfig.someoneok.crystalconfig.ConfigSound;
import models.someoneok.crystalconfig.SoundSetting;

public final class AudioConfig {
    @ConfigSound(label = "Alert sound", description = "Played when the alert triggers.")
    public static final MutableState<SoundSetting> alertSound =
            new MutableState<>(SoundSetting.none());
}
```

Disable `None` for required sounds:

```java
@ConfigSound(label = "Alert sound", allowNone = false)
public static final MutableState<SoundSetting> alertSound =
        new MutableState<>(SoundSetting.none());
```

`SoundSetting` persists as:

```json
{
  "sound": "minecraft:block.note_block.pling",
  "volume": 1.0,
  "pitch": 1.0
}
```

The picker uses `BuiltInRegistries.SOUND_EVENT.keySet()` for choices and `SimpleSoundInstance` for preview playback.

## Custom annotations

Register a custom annotation-backed component with `AutoConfig.registerComponent`:

```java
AutoConfig.registerComponent(ConfigExample.class, ExampleValue.class, context ->
        new ExampleComponent(context.state())
);
```

The annotation must provide `label()`, `description()`, and `key()` methods. The field type must be compatible with the registered value type and wrapped in `State<T>`, `MutableState<T>`, or `ConfigValue<T>`.
