---
layout: default
title: JitPack artifact
description: Official CrystalConfig JitPack coordinate and published artifact details for downstream mods.
---

# JitPack artifact

CrystalConfig is consumed from the official repository only:

```text
https://github.com/SomeoneOKxD/CrystalConfig
```

Do not use forked repositories, mirrored repositories, copied repositories, alternate Maven repositories, or alternate JitPack coordinates.

## Official coordinate

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    modImplementation("com.github.SomeoneOKxD.CrystalConfig:crystal-config:<version>")
}
```

Replace `<version>` with an official release tag from `SomeoneOKxD/CrystalConfig`, for example `v1.0-mc26.1`.

## What the dependency resolves to

The official JitPack publication exposes the `minecraft-mod` artifact as `crystal-config`.

| Artifact | Purpose |
|---|---|
| `crystal-config-<version>.jar` | Main Fabric mod jar used by downstream mods |
| `crystal-config-<version>-sources.jar` | Combined sources for IDE navigation |

The main jar is the project's shaded Fabric mod jar. It contains the Minecraft integration module plus the internal `core` and `bridge-minecraft` code. There is no remap jar in this project.

## Runtime dependency

If your mod requires CrystalConfig at runtime, declare the mod id in `fabric.mod.json`:

```json
{
  "depends": {
    "crystalconfig": ">=1.0"
  }
}
```

Use `modImplementation(...)` for normal development and runtime resolution. Do not document or rely on fork-published CrystalConfig artifacts.


## Release tags

Official release tags use this format:

```text
v<mod_version>-mc<minecraft_version>
```

Example:

```text
v1.0-mc26.1
```

The build workflow creates the tag and GitHub release automatically on pushes to `main` or `master` when that tag does not already exist. Bump either `mod_version` or `minecraft_version` in `gradle.properties` before publishing a new release.
