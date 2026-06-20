# Architecture

## Layers

### `core`

`core` owns the reusable UI framework:

- component tree and layout containers
- input dispatch, focus, hover, and capture state
- reactive state and conditional visibility/disabled predicates
- themes and animation helpers
- config annotations and screen builders
- renderer-neutral draw commands
- Gson-backed persistence

`core` does not import Minecraft classes.

### Draw commands

Components do not render directly. They submit commands to `DrawList`:

- `Material.SDF_RECT` for rounded rectangles, borders, shadows, and most widget chrome
- `Material.HSV_SV` and `Material.HSV_HUE` for the color picker
- `TextCommand` for text runs

`DrawList.flush(RenderBackend)` sorts by `z`, keeps insertion order for equal `z`, batches compatible quads, and applies clip changes only when the clip rectangle changes.

### Minecraft bridge

`bridge-minecraft` defines the small surface a version-specific backend must implement:

- current GUI size and scale
- text measurement
- clip/scissor state
- quad and text submission
- optional clipboard and URL hooks discovered by the core utilities

### Minecraft implementation

`crystal-config` contains the Fabric client entrypoint, backend implementation, MSDF font renderer, shaders, and Minecraft-only widgets such as the sound picker. Porting to another loader should replace this module, not the core UI model.

## Frame lifecycle

```text
screen.render(...)
  adapter.attachContext(drawContext)
  root.render(adapter, width, height, scale, delta)
    backend.beginFrame(...)
    root.tick(delta)
    layout when size/scale/dirty state changes
    render components into DrawList
    render overlays and tooltips
    DrawList.flush(backend)
    backend.endFrame()
```

## Input lifecycle

```text
mouse moved   -> hit test -> hover path update -> bubble move event
mouse pressed -> hit test -> focus/capture -> capture phase -> bubble phase
mouse dragged -> captured component, or current hit component
mouse release -> captured component, then capture reset
key/char      -> focused component
scroll        -> hovered component -> bubble phase
```
