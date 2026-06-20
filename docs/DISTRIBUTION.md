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
    modImplementation("com.github.SomeoneOKxD.CrystalConfig:crystal-config:<version>")
}
```

Replace `<version>` with an official release tag from `SomeoneOKxD/CrystalConfig`.

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

CrystalConfig's distributable artifact is the `shadowJar` output from the `minecraft-mod` module. That shaded jar is the actual Fabric mod jar: it contains the Minecraft module plus the internal `core` and `bridge-minecraft` code. No remap task is used for the published artifact in this build setup.

Build the official local release artifacts with:

```bash
./gradlew buildModWithSources
```

The relevant outputs are written to `minecraft-mod/build/libs/`:

- `crystal-config-<mod_version>+mc<minecraft_version>.jar` — shaded Fabric mod jar for distribution.
- `crystal-config-<mod_version>+mc<minecraft_version>-sources.jar` — combined sources for IDE navigation.

Do not upload `*-dev.jar` or `*-javadoc.jar` as the main release artifact.

## Maintainer CI notes

The `Build CrystalConfig mod` workflow at `.github/workflows/build-mod.yml` builds the official artifacts on pushes, pull requests, tags, and manual dispatches. It uploads the shaded mod jar and sources jar as the `crystal-config-mod-and-sources` workflow artifact.

The root `jitpack.yml` prepares Java 25 and runs:

```bash
./gradlew --no-daemon :minecraft-mod:publishToMavenLocal --stacktrace
```

The `minecraft-mod` Maven publication attaches `shadowJar` as the main artifact and the combined sources jar as the sources artifact. On JitPack, the intended public coordinate is:

```text
com.github.SomeoneOKxD.CrystalConfig:crystal-config:<version>
```

Only official tags from `SomeoneOKxD/CrystalConfig` should be used for public releases.
