# Official Distribution

CrystalConfig is proprietary and all rights are reserved. The only supported source for the dependency is the official repository:

```text
https://github.com/SomeoneOKxD/CrystalConfig
```

Do not publish CrystalConfig from forks, mirrors, copied repositories, alternate Maven repositories, or alternate JitPack coordinates. Downstream projects should resolve the official artifact only.

## Official JitPack dependency

Add JitPack to the consuming build:

```kotlin
repositories {
    maven("https://jitpack.io")
}
```

Depend on the official CrystalConfig artifact:

```kotlin
dependencies {
    // Pick exactly one artifact for the Minecraft version you target.
    implementation("com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.1:<version>")
    // implementation("com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.2:<version>")
    // implementation("com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.3:<version>")
}
```

Replace `<version>` with an official release tag from `SomeoneOKxD/CrystalConfig`, for example `v1.4`. A single release tag publishes all three Minecraft variants. `CrystalConfig:<version>` is retained as a compatibility alias for the 26.1 artifact. For Minecraft 26.1+ use `implementation(...)` with Fabric Loom's no-remap plugin; the old `modImplementation(...)` convention is for older remapping-based projects.

CrystalConfig requires Fabric Loader **0.18.5+** on 26.1 and **0.19.5+** on 26.2/26.3. The metadata is generated from each Stonecutter node's `gradle.properties`.

## Fabric metadata

When CrystalConfig is required at runtime, add it to the consuming mod's `fabric.mod.json`:

```json
{
  "depends": {
    "fabricloader": ">=0.18.5",
    "minecraft": "~26.1",
    "crystalconfig": ">=1.0"
  }
}
```

## Official release artifacts

CrystalConfig's distributable artifacts are the `shadowJar` outputs from the Stonecutter nodes under `crystal-config`. Each shaded jar is an actual Fabric mod jar for one Minecraft version and contains the Minecraft module plus the internal `core`, `bridge-minecraft`, and `render-api` code. No remap task is used for the published artifacts in this build setup.

Build the official local release artifacts with:

```bash
./gradlew buildModWithSources
```

The relevant outputs are written to `crystal-config/build/libs/`:

For each supported Minecraft version (`26.1`, `26.2`, and `26.3`):

- `crystal-config-<mod_version>-mc<minecraft_version>.jar` — shaded Fabric mod jar for distribution.
- `crystal-config-<mod_version>-mc<minecraft_version>-sources.jar` — combined sources for IDE navigation.

Do not upload `*-dev.jar` or `*-javadoc.jar` as the main release artifact.

## Maintainer CI notes

The `Build and release CrystalConfig mod` workflow at `.github/workflows/build-mod.yml` builds all three Stonecutter library nodes and all three config-tester nodes on pushes, pull requests, tags, and manual dispatches. It compiles the tester as a consumer against each library version and compiles and links the GLSL shaders using `glslangValidator`, validates generated mod metadata, shaded classes, version-specific shaders/access wideners, sources, and JitPack Maven Local publications, then uploads the artifacts. See `tools/validate_shader_sources.py`, `tools/test_sdl_input_translation.py`, and `tools/verify_release_artifacts.py`. Build jobs (including pull requests) have read-only repository permissions; only the gated release job receives `contents: write`. These checks **do not** execute Minecraft's runtime renderer.

The root `jitpack.yml` prepares Java 25, builds all six distributable jars, publishes the Maven modules, and independently verifies the published POMs and jar hashes on the JitPack builder. Its Gradle command is:

```bash
./gradlew --no-daemon buildModWithSources publishModToMavenLocal --stacktrace
```

Each Stonecutter node publishes `shadowJar` as the main artifact and its combined sources jar as the sources artifact. On JitPack, the public coordinates are:

```text
com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.1:<version>
com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.2:<version>
com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.3:<version>
```

JitPack is expected to expose individual modules under `com.github.SomeoneOKxD.CrystalConfig` after the release tag has been pushed and built. Verify the remote POM and JAR for each module on jitpack.io; local Maven publication tests do not prove JitPack has served them. The `com.github.SomeoneOKxD:CrystalConfig:<version>` Maven Local publication is intended as a 26.1 compatibility alias, but the remote root coordinate also needs checking because JitPack can synthesize an aggregate module for multi-module builds.

## Runtime test checklist (required before publishing)

CI shader checks catch source-level GLSL syntax/linkage problems, but they cannot verify real game rendering, texture sampling, GPU pipelines, or scissoring. For **each** of Minecraft 26.1, 26.2, and 26.3:

1. Launch a clean Fabric client with Java 25, the correct Minecraft version, and the matching minimum Fabric Loader (26.1: 0.18.5; 26.2/26.3: 0.19.5). Load that version's built shaded CrystalConfig jar, together with a development/consumer mod capable of opening a CrystalConfig config screen.
2. Open a screen that exercises normal and MSDF text, rounded SDF rectangles, gradients, lines/borders, images, text input, and nested clipped/scrolling areas. Resize the window and change GUI scale while the screen is open.
3. Exercise SDL/GLFW inputs: left/right/middle click, drag, wheel, arrow keys, Backspace/Delete, Enter/Escape, and Ctrl+A/C/V/X in the main text field, search bar, searchable dropdowns, profile name input, sound picker numeric inputs, and color-picker hex editor. Check that keybind recording captures keyboard and mouse input and that Shift, Ctrl, and Super work on both sides of the keyboard. On 26.3 test with a non-US keyboard layout and IME, if available; confirm character input resumes when focusing a text field and stops when leaving it or closing the screen.
4. Check `latest.log` for shader compilation, linkage, access-widener, mixin, or renderer errors; verify that the UI is visible, correctly clipped, interactive, and free of obvious rendering corruption. Where both graphics backends are supported, test them both.
5. Save the logs and record the tested Minecraft, Loader, and graphics backend versions. Do not approve the release until each version passes.

For local validation, after `./gradlew buildModWithSources`, run:

```bash
python3 tools/validate_shader_sources.py              # requires glslangValidator (glslang-tools)
python3 tools/test_sdl_input_translation.py             # requires javac
python3 tools/verify_release_artifacts.py --mod-version 1.4
```

## Publishing a release

Pushes to `main`/`master` **build and verify**, but **do not automatically create a tag**. This prevents publishing untested rendering changes. Once the runtime checklist passes, use **Actions → Build and release CrystalConfig mod → Run workflow**, selecting `main` or `master`, and enable both `publish_release` and `runtime_tested`. The workflow will build again, check JitPack publications, then create an annotated `v<mod_version>` tag and GitHub release (for example `v1.4`). Only official tags from `SomeoneOKxD/CrystalConfig` should be used for public releases.

### Recreating an existing JitPack tag

JitPack builds from the Git tag. If a broken multi-version tag such as `v1.3` already exists, either bump `mod_version` or run the workflow manually with **`publish_release`**, **`runtime_tested`**, and **`force_recreate_release`** enabled after retesting all three builds. Moving an existing tag may need a JitPack rebuild or cache invalidation; publishing a new version is safer.
