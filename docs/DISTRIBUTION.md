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
    modImplementation("com.github.SomeoneOKxD:CrystalConfig:<version>")
}
```

Replace `<version>` with an official release tag from `SomeoneOKxD/CrystalConfig`, for example `v1.0-mc26.1`.

## Fabric metadata

When CrystalConfig is required at runtime, add it to the consuming mod's `fabric.mod.json`:

```json
{
  "depends": {
    "fabricloader": ">=0.18.5",
    "minecraft": ">=26.1",
    "crystalconfig": ">=1.0"
  }
}
```

## Official release artifacts

CrystalConfig's distributable artifact is the `shadowJar` output from the `crystal-config` module. That shaded jar is the actual Fabric mod jar: it contains the Minecraft module plus the internal `core` and `bridge-minecraft` code. No remap task is used for the published artifact in this build setup.

Build the official local release artifacts with:

```bash
./gradlew buildModWithSources
```

The relevant outputs are written to `crystal-config/build/libs/`:

- `crystal-config-<mod_version>-mc<minecraft_version>.jar` — shaded Fabric mod jar for distribution.
- `crystal-config-<mod_version>-mc<minecraft_version>-sources.jar` — combined sources for IDE navigation.

Do not upload `*-dev.jar` or `*-javadoc.jar` as the main release artifact.

## Maintainer CI notes

The `Build and release CrystalConfig mod` workflow at `.github/workflows/build-mod.yml` builds the official artifacts on pushes, pull requests, tags, and manual dispatches. It uploads the shaded mod jar and sources jar as a workflow artifact named from `mod_version` and `minecraft_version`.

The root `jitpack.yml` prepares Java 25 and runs:

```bash
./gradlew --no-daemon :crystal-config:publishToMavenLocal --stacktrace
```

The `crystal-config` Maven publication attaches `shadowJar` as the main artifact and the combined sources jar as the sources artifact. On JitPack, the public coordinate uses the JitPack multi-module format, where the artifact id is the Gradle module name:

```text
com.github.SomeoneOKxD:CrystalConfig:<version>
```

On pushes to `main` or `master`, the workflow creates an official annotated tag and GitHub release when that version tag does not already exist. The tag format is `v<mod_version>-mc<minecraft_version>`, for example `v1.0-mc26.1`. This lets the same `mod_version` be published for multiple Minecraft versions. Only official tags from `SomeoneOKxD/CrystalConfig` should be used for public releases.


### Recreating an existing JitPack tag

JitPack builds from the Git tag. If a broken tag such as `v1.0-mc26.1` already exists, either bump `mod_version`/`minecraft_version` or run the `Build and release CrystalConfig mod` workflow manually with `force_recreate_release` enabled. That deletes and recreates the GitHub release/tag so JitPack can rebuild from the corrected commit.
