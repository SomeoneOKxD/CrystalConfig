# CrystalConfig Tester

A tiny **client-only Fabric development mod** that exercises the CrystalConfig library in the same multi-module source checkout. It is deliberately separate from the `crystal-config` mod and does not shade or redistribute CrystalConfig.

## Minecraft versions

The `config-tester` Stonecutter node has exactly the same Minecraft versions as `crystal-config`: **26.1 (base), 26.2, 26.3**. It compiles against `:crystal-config:<version>` plus the renderer-neutral modules. The active source format is 26.1; the `//? if ...` comments in Java select input/screen APIs for 26.2 and 26.3.

| Minecraft | Fabric Loader minimum | Fabric API dependency |
| --- | --- | --- |
| 26.1 | 0.18.5 | `0.145.1+26.1` |
| 26.2 | 0.19.5 | `0.161.0+26.2` |
| 26.3 | 0.19.5 | `0.162.0+26.3` |

These are test-build coordinates. You can update them in `config-tester/versions/<version>/gradle.properties` when new versions ship.

## Commands

Only the Fabric **client commands** are registered, so they work in single-player and on a server without installing this mod server-side.

| Command | Action |
| --- | --- |
| `/crystaltest` | Opens the entire annotation-driven config coverage screen |
| `/crystaltest config` | Same as above |
| `/crystaltest manual` | Opens an additional hand-built `ConfigScreenBuilder` demo |
| `/crystaltest render` | Opens the independent `UiRenderUtils` screen, with five interactive tabs |

In the render screen, click the **Shapes**, **Typography**, **Textures**, **Controls**, and **Inputs** tabs or use keys **1-5**. Try hovering the animated button; clicking Increment/Reset; toggling the switch/checkbox; testing text selection with Ctrl+A; typing an out-of-range number; and switching frame caching. The Back button returns to the config UI when opened from that UI, otherwise to gameplay.

### Annotation coverage

`config/BasicOptions.java`:
- Toggle, checkbox, plain slider, labeled slider, number input
- Plain, regex-restricted, reveal-while-editing and always-hidden text
- RGB/RGBA colors, keyboard-and-mouse keybind, restricted keyboard keybind
- Info, tooltip, separator, labeled separator, spacer

`config/SelectionOptions.java`:
- Enum dropdown, searchable dropdown
- Grouped dropdown, searchable grouped dropdown
- Multi-select dropdown, searchable multi-select dropdown
- Draggable lists, no-delete/reorder-only variant

`config/CustomOptions.java`:
- `@ConfigCustom` embedded button, `@ConfigCustomOption` full-width component
- `@ConfigCustomList` with a record-based entry and text/number editors
- Dynamically updated `@ConfigInfo` via `ConfigMarker` suppliers

`config/ProfileOptions.java`:
- `@ConfigProfile` with linked toggle/slider, separate global toggle

`config/ConditionalOptions.java` and `ConditionalCategoryOptions.java`:
- `@ConfigButton`, disabled/hidden condition bindings, `@ConfigAccordion`
- Dynamically hidden and disabled categories
- Footer button and all four footer-icon actions (`OPEN_URL`, `COPY_TO_CLIPBOARD`, `RUNNABLE`, `NONE`)
- Dynamic info-row title, tooltip and description

`config/SoundOptions.java`:
- Minecraft-specific `@ConfigSound` (allow-none and non-null variants)

The custom renderer exercises rectangles, SDF outlines/shadows, circle, lines/points, gradients, separators, progress bars, clipping, formatted/plain/center/right-aligned/metric text, entire/tinted/partial/UV textures, filters, buttons and animations, toggles, checkboxes, text inputs, and validated number inputs.

## Build and run

**Prerequisites:** Java **25**, network access for first-time Gradle/Minecraft/Fabric dependency downloads, and the included Gradle wrapper. Execute from the repository root:

```bash
# All three tester jars
bash gradlew buildConfigTester

# One version at a time
bash gradlew :config-tester:26.1:buildAndCollect
bash gradlew :config-tester:26.2:buildAndCollect
bash gradlew :config-tester:26.3:buildAndCollect

# Run client dev environment (as configured by Loom)
bash gradlew :config-tester:26.1:runClient
```

Tester outputs are gathered at `config-tester/build/libs/crystalconfig-tester-0.1.0-mc<version>.jar` by the `buildAndCollect` tasks. The installed `crystal-config` mod must match the target version; the tester's `fabric.mod.json` explicitly requires it. To run a tester jar outside Gradle, install both `CrystalConfig` and Fabric API for your selected Minecraft version. The tester jar contains *only* the tester, never the CrystalConfig implementation.

Config values persist into `<minecraft-instance>/config/crystalconfig-tester.json`; this is an intentionally separate file from your production configuration. Changing a value should also update the other screen if both use the same shared state. The test mod does not send telemetry.

## Manual verification

1. Run `/crystaltest` for 26.1, change each widget, exit/reopen, then restart the client to verify values persist.
2. Test all selection controls (search, group filtering, additions/removals, drag reordering), custom list add/edit/remove, profiles create/switch/rename/delete, the sound pickers and footer actions.
3. Lock, hide, then disable the special action/category from the conditional tab. Confirm sidebar navigation reacts correctly.
4. Run `/crystaltest manual`, change the shared slider/toggle there, close, then re-open `/crystaltest` and check both show the updated value.
5. Run `/crystaltest render` and test all five pages, texture atlas UVs, text editor keyboard events, cache mode, resizing, and back navigation.
6. Build/run repeat for Minecraft **26.2** and **26.3**. On 26.3 verify SDL key events and typed Unicode characters reach the focused text editor.

**Build validation status:** This workspace originally had only Java 21 and could not connect to Gradle's download host, so the actual Fabric/Loom `runClient`/`build` could not be executed here. The archive contains source code, **not precompiled/tested mod jars**. Follow the commands above with Java 25 and working dependency access to verify the integration against real Minecraft dependencies.
