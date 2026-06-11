# Minecraft Backend Checklist

Use this checklist when implementing a Fabric, Forge, or NeoForge adapter for the core UI.

## Required adapter methods

Implement `MinecraftUiAdapter<DrawContextT>`:

```java
void attachContext(DrawContextT context);
int scaledWidth();
int scaledHeight();
float uiScale();

void beginFrame(RenderFrame frame);
void endFrame();
TextMetrics measureText(String text, float fontSize, String fontFace);
void setClip(Rect clip);
void clearClip();
void drawQuads(Material material, List<QuadCommand> batch);
void drawText(TextCommand command);
```

## Optional adapter hooks

`MinecraftUiAdapter` also defines optional defaults used by core utilities:

```java
String getClipboard();
void setClipboard(String value);
void closeScreen();
void openUrl(String url);
```

Implement clipboard hooks through Minecraft's keyboard handler. Implement `openUrl` through the client's normal confirm-link flow.

## Recommended quad backend

1. Create one static unit quad mesh.
2. Create a dynamic instance buffer for `QuadCommand` data.
3. During `drawQuads`, upload all commands from the provided batch.
4. Bind the pipeline for `SDF_RECT`, `HSV_SV`, `HSV_HUE`, or `CHECKERBOARD`.
5. Apply the current scissor rectangle before drawing.
6. Issue one instanced draw call for the batch.

## Mapping table

| Core concept | Minecraft implementation |
|---|---|
| `Rect` | Scaled GUI coordinates. |
| `z` | GUI render-state depth, pose z, or vertex z. |
| `setClip` / `clearClip` | GUI scissor API. |
| `Material.SDF_RECT` | Custom UI shader/pipeline. |
| `TextCommand` | Minecraft text renderer or MSDF text renderer. |
| `RenderFrame.uiScale` | Window GUI scale factor. |
| `DrawList.flush` | Render extraction and batching boundary. |

## Porting rule

Keep config definitions, state, builders, and components unchanged. Replace only the adapter and backend code that translates draw commands to the target Minecraft version's renderer.
