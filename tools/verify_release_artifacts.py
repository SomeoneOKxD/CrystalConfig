#!/usr/bin/env python3
"""Validate each built CrystalConfig jar and, optionally, its JitPack Maven publication.

Standard-library only. Run after buildModWithSources (and after Maven Local
publication when --maven-version is supplied). This validates packaging, not
in-game shader compilation or rendering.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys
from xml.etree import ElementTree
from zipfile import BadZipFile, ZipFile

VERSIONS = {"26.1": "0.18.5", "26.2": "0.19.5", "26.3": "0.19.5"}
SHADERS = ("msdf_text.fsh", "msdf_text.vsh", "sdf_rect.fsh", "sdf_rect.vsh")
SHADER_PREFIX = "assets/crystalconfig/shaders/core/"
JAVA_CLASSES = (
    "dev/someoneok/crystalconfig/CrystalConfig.class",
    "dev/someoneok/crystalconfig/config/ConfigScreenBuilder.class",  # shaded core
    "dev/someoneok/crystalconfig/minecraft/MinecraftUiAdapter.class",  # shaded bridge
    "dev/someoneok/crystalconfig/api/render/UiRenderUtils.class",  # shaded render-api
)
FONT_ASSETS = (
    "assets/crystalconfig/textures/msdf/regular.png",
    "assets/crystalconfig/msdf/regular.json",
)


class VerificationError(Exception):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise VerificationError(message)


def required_file(file: Path) -> None:
    require(file.is_file() and file.stat().st_size > 0, f"Missing or empty file: {file}")


def sha256(file: Path) -> str:
    return hashlib.sha256(file.read_bytes()).hexdigest()


def verify_jar(file: Path, mc: str, mod_version: str, source_root: Path, sources: bool) -> None:
    required_file(file)
    try:
        with ZipFile(file) as archive:
            corrupt = archive.testzip()
            require(corrupt is None, f"Corrupt ZIP entry in {file}: {corrupt}")
            names = archive.namelist()
            name_set = set(names)
            require(len(name_set) == len(names), f"Duplicate ZIP entries in {file}")
            for shader in SHADERS:
                entry = SHADER_PREFIX + shader
                require(entry in name_set, f"{file}: missing {entry}")
                expected_file = source_root / "crystal-config" / "src" / "compat" / mc / "resources" / entry
                required_file(expected_file)
                require(archive.read(entry) == expected_file.read_bytes(),
                        f"{file}: shader {entry} doesn't match the {mc} source")
            require("crystalconfig.accesswidener" in name_set, f"{file}: missing access widener")
            widener = archive.read("crystalconfig.accesswidener")
            expected_widener = source_root / "crystal-config" / "src" / "compat" / mc / "crystalconfig.accesswidener"
            require(widener == expected_widener.read_bytes(), f"{file}: wrong access widener for {mc}")
            require(widener.startswith(b"accessWidener v2 official"), f"{file}: access widener isn't in official namespace")
            if sources:
                for java_file in ("dev/someoneok/crystalconfig/CrystalConfig.java",
                                  "dev/someoneok/crystalconfig/config/ConfigScreenBuilder.java",
                                  "dev/someoneok/crystalconfig/minecraft/MinecraftUiAdapter.java",
                                  "dev/someoneok/crystalconfig/api/render/UiRenderUtils.java"):
                    require(java_file in name_set, f"{file}: missing combined source {java_file}")
            else:
                for clazz in JAVA_CLASSES:
                    require(clazz in name_set, f"{file}: missing runtime/shaded class {clazz}")
                for asset in FONT_ASSETS:
                    require(asset in name_set, f"{file}: missing MSDF asset {asset}")
                require("fabric.mod.json" in name_set, f"{file}: missing fabric.mod.json")
                metadata_bytes = archive.read("fabric.mod.json")
                require(b"${" not in metadata_bytes, f"{file}: unexpanded fabric.mod.json template")
                metadata = json.loads(metadata_bytes)
                require(metadata.get("id") == "crystalconfig", f"{file}: unexpected Fabric mod id")
                require(metadata.get("version") == mod_version, f"{file}: wrong mod version")
                require(metadata.get("accessWidener") == "crystalconfig.accesswidener",
                        f"{file}: wrong access widener path in metadata")
                require(metadata.get("depends", {}).get("minecraft") == f"~{mc}",
                        f"{file}: incorrect Minecraft dependency")
                require(metadata.get("depends", {}).get("fabricloader") == f">={VERSIONS[mc]}",
                        f"{file}: incorrect Fabric Loader dependency")
    except (BadZipFile, KeyError, ValueError) as exc:
        raise VerificationError(f"{file}: invalid artifact ({exc})") from exc
    print(f"OK: {file} (Minecraft {mc}; {'sources' if sources else 'binary'})")


def verify_pom(file: Path, group: str, artifact: str, version: str) -> None:
    required_file(file)
    try:
        project = ElementTree.parse(file).getroot()
    except ElementTree.ParseError as exc:
        raise VerificationError(f"{file}: invalid Maven POM: {exc}") from exc
    # Gradle's generated POMs use the Maven namespace; select by local name.
    fields = {node.tag.rsplit("}", 1)[-1]: node.text for node in project}
    for field, expected in (("groupId", group), ("artifactId", artifact), ("version", version)):
        require(fields.get(field) == expected, f"{file}: expected {field}={expected}, got {fields.get(field)!r}")
    print(f"OK: {file}")


def verify_maven(maven_repo: Path, libs: Path, mod_version: str, maven_version: str) -> None:
    for mc in VERSIONS:
        artifact = f"CrystalConfig-{mc}"
        path = maven_repo / "com/github/SomeoneOKxD/CrystalConfig" / artifact / maven_version
        binary = path / f"{artifact}-{maven_version}.jar"
        sources = path / f"{artifact}-{maven_version}-sources.jar"
        verify_pom(path / f"{artifact}-{maven_version}.pom",
                   "com.github.SomeoneOKxD.CrystalConfig", artifact, maven_version)
        for published, built in ((binary, libs / f"crystal-config-{mod_version}-mc{mc}.jar"),
                                 (sources, libs / f"crystal-config-{mod_version}-mc{mc}-sources.jar")):
            required_file(published)
            require(sha256(published) == sha256(built),
                    f"Maven artifact {published} differs from validated build {built}")
            print(f"OK: Maven artifact {published}")

    alias = "CrystalConfig"
    alias_dir = maven_repo / "com/github/SomeoneOKxD" / alias / maven_version
    verify_pom(alias_dir / f"{alias}-{maven_version}.pom",
               "com.github.SomeoneOKxD", alias, maven_version)
    for suffix in (".jar", "-sources.jar"):
        published = alias_dir / f"{alias}-{maven_version}{suffix}"
        built = libs / f"crystal-config-{mod_version}-mc26.1{suffix}"
        required_file(published)
        require(sha256(published) == sha256(built), f"Legacy Maven alias differs from 26.1: {published}")
        print(f"OK: Legacy Maven alias {published}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mod-version", required=True, help="mod_version from gradle.properties, e.g. 1.3")
    parser.add_argument("--source-root", type=Path, default=Path(__file__).resolve().parent.parent)
    parser.add_argument("--release-root", type=Path, default=Path("crystal-config/build/libs"))
    parser.add_argument("--maven-version", help="Also verify JitPack Maven Local, e.g. v1.3")
    parser.add_argument("--maven-repo", type=Path, default=Path.home() / ".m2/repository")
    args = parser.parse_args()

    try:
        expected_names = {
            f"crystal-config-{args.mod_version}-mc{mc}{suffix}.jar"
            for mc in VERSIONS
            for suffix in ("", "-sources")
        }
        # Guard release uploads against stale/unexpected distribution JARs.
        actual_names = {
            jar.name for jar in args.release_root.glob("*.jar")
            if not jar.name.endswith(("-dev.jar", "-javadoc.jar"))
        }
        require(actual_names == expected_names,
                f"Release artifact inventory differs: missing={sorted(expected_names - actual_names)}, "
                f"unexpected={sorted(actual_names - expected_names)}")
        for mc in VERSIONS:
            base = f"crystal-config-{args.mod_version}-mc{mc}"
            verify_jar(args.release_root / f"{base}.jar", mc, args.mod_version, args.source_root, sources=False)
            verify_jar(args.release_root / f"{base}-sources.jar", mc, args.mod_version,
                       args.source_root, sources=True)
        if args.maven_version:
            verify_maven(args.maven_repo, args.release_root, args.mod_version, args.maven_version)
    except (VerificationError, OSError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1
    print("CrystalConfig release artifact validation passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
