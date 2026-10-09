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
    // Choose the artifact matching the consuming Minecraft version.
    implementation("com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.1:<version>")
    // implementation("com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.2:<version>")
    // implementation("com.github.SomeoneOKxD.CrystalConfig:CrystalConfig-26.3:<version>")
}
```

Replace `<version>` with an official release tag from `SomeoneOKxD/CrystalConfig`, for example `v1.4`. The unqualified `CrystalConfig:<version>` artifact remains an alias for the 26.1 build.

## What the dependency resolves to

The official JitPack publication exposes one Maven artifact per Stonecutter Minecraft node.

| Artifact id | Purpose |
|---|---|
| `CrystalConfig-26.1` | Fabric mod jar for Minecraft 26.1 |
| `CrystalConfig-26.2` | Fabric mod jar for Minecraft 26.2 |
| `CrystalConfig-26.3` | Fabric mod jar for Minecraft 26.3 |
| `CrystalConfig` | Compatibility alias for the Minecraft 26.1 artifact |

Each publication also includes a combined `-sources.jar` for IDE navigation.

The main jar is the project's shaded Fabric mod jar. It contains the Minecraft integration module plus the internal `core`, `bridge-minecraft`, and `render-api` code. There is no remap jar in this project.

## Runtime dependency

If your mod requires CrystalConfig at runtime, declare the mod id in `fabric.mod.json`:

```json
{
  "depends": {
    "crystalconfig": ">=1.0"
  }
}
```

For Minecraft 26.1+, use `implementation(...)` with Fabric Loom's no-remap plugin; `modImplementation(...)` is for older remapping-based Loom projects. Do not document or rely on fork-published CrystalConfig artifacts.


## Release tags

Official release tags use this format:

```text
v<mod_version>
```

Example:

```text
v1.4
```

Each tag contains the 26.1, 26.2, and 26.3 jars. Pushes to `main` or `master` build and verify them; an official tag and GitHub release are created only by a manual workflow dispatch after all three versions pass runtime UI/shader testing. Bump `mod_version` in `gradle.properties` before publishing a new release.


> If this version was already released before the JitPack module-name fix, rerun the build workflow manually with `force_recreate_release` enabled, or publish a bumped version.
