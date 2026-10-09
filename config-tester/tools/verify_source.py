#!/usr/bin/env python3
"""Dependency-free source/asset sanity check, NOT a substitute for Gradle + Minecraft."""

from __future__ import annotations

import json
import re
import struct
import zlib
from pathlib import Path

root = Path(__file__).resolve().parents[2]
tester = root / "config-tester"
java = tester / "src/main/java/dev/someoneok/crystalconfigtester"
config = java / "config"
files = [file for file in config.glob("*.java")]
body = "\n".join(path.read_text(encoding="utf-8") for path in files)

# All built-in annotation classes (including the Minecraft-only @ConfigSound).
annotations = set()
for path in (root / "core/src/main/java/dev/someoneok/crystalconfig/autoconfig").glob("*.java"):
    if "@interface Config" in path.read_text(encoding="utf-8"):
        annotations.add(path.stem)
annotations.add("ConfigSound")
used = set(re.findall(r"@(?:(Config[A-Za-z]+))\b", body))
missing = sorted(annotations - used)
assert not missing, f"Missing config annotation coverage: {missing}"

# Each persisted setting key must be unique, including profile settings.
keys = re.findall(r"@Config\w+\s*\([^)]*?\bkey\s*=\s*\"([^\"]+)\"", body, flags=re.S)
assert len(keys) == len(set(keys)), f"Duplicated explicit state keys: {keys}"
assert len(keys) >= 30, f"Unexpectedly low option coverage: {len(keys)}"

renderer = (java / "screen/RenderShowcaseScreen.java").read_text(encoding="utf-8")
utils = (root / "render-api/src/main/java/dev/someoneok/crystalconfig/api/render/UiRenderUtils.java").read_text(encoding="utf-8")
public = set(re.findall(r"\bpublic\s+(?:void|TextMetrics|Theme)\s+(\w+)\s*\(", utils))
# `render()` is called by the Minecraft adapter, not by a frame callback.
for method in sorted(public - {"render"}):
    assert re.search(r"\b(?:d|draw|sc)\." + re.escape(method) + r"\s*\(", renderer), f"Renderer sample missing {method}()"

assert 'stonecutter active "26.1"' in (tester / "stonecutter.gradle.kts").read_text()
settings = (root / "settings.gradle").read_text()
assert "create('config-tester')" in settings
for version in ("26.1", "26.2", "26.3"):
    props = (tester / f"versions/{version}/gradle.properties").read_text()
    assert "loader_version=" in props and f"+{version}" in props
assert "buildConfigTester" in (root / "build.gradle").read_text()
assert "//? if >=26.3" in renderer
assert "//? if >=26.2" in (java / "ConfigTesterClient.java").read_text()
assert all(x in (java / "ConfigTesterClient.java").read_text() for x in ("crystaltest", "manual", "render"))

# Validate resource structure and that all IDAT chunks can be decompressed.
asset = tester / "src/main/resources/assets/crystalconfigtester/textures/gui/test_atlas.png"
png = asset.read_bytes()
assert png[:8] == b"\x89PNG\r\n\x1a\n"
offset = 8
idat = b""
width = height = 0
while offset < len(png):
    length = struct.unpack_from(">I", png, offset)[0]
    chunk_type = png[offset + 4:offset + 8]
    data = png[offset + 8:offset + 8 + length]
    crc = struct.unpack_from(">I", png, offset + 8 + length)[0]
    assert (zlib.crc32(chunk_type + data) & 0xFFFFFFFF) == crc
    if chunk_type == b"IHDR":
        width, height = struct.unpack_from(">II", data)
    if chunk_type == b"IDAT":
        idat += data
    offset += 12 + length
assert (width, height) == (64, 64)
assert len(zlib.decompress(idat)) > 64 * 64

metadata = (tester / "src/main/resources/fabric.mod.json").read_text()
for version in ("26.1", "26.2", "26.3"):
    data = json.loads(metadata.replace("${version}", "0.1.0")
                      .replace("${minecraft_version}", version)
                      .replace("${loader_version}", "0.19.5"))
    assert data["id"] == "crystalconfigtester" and data["environment"] == "client"
    assert data["depends"]["minecraft"] == f"~{version}"
    assert "crystalconfig" in data["depends"] and "fabric-api" in data["depends"]

print(f"PASS: all {len(annotations)} config annotations exercised; {len(keys)} distinct persistence keys")
print(f"PASS: all {len(public) - 1} public UiRenderUtils methods other than render() are called")
print("PASS: Stonecutter 26.1 (base), 26.2, 26.3; Fabric mod JSON; 64x64 PNG atlas")
print("NOTE: Java 25 / Gradle / real Minecraft compilation and runtime testing are separate.")
