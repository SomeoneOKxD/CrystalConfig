#!/usr/bin/env python3
"""Validate the explicit per-version Gradle task wiring without Gradle.

The source of truth for the version list is settings.gradle. This check
prevents a version being added to Stonecutter but omitted from our release
aggregation, and blocks obsolete Stonecutter 0.6 task APIs.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
settings = (root / "settings.gradle").read_text(encoding="utf-8")
root_build = (root / "build.gradle").read_text(encoding="utf-8")
controller = (root / "crystal-config/stonecutter.gradle.kts").read_text(encoding="utf-8")
node_build = (root / "crystal-config/build.gradle.kts").read_text(encoding="utf-8")

settings_match = re.search(r"versions\(\s*([^)]*)\)", settings)
root_match = re.search(r"def crystalConfigVersions\s*=\s*\[([^]]*)\]", root_build)
assert settings_match and root_match, "Missing Stonecutter or aggregator version lists"
parse_versions = lambda s: re.findall(r"['\"](26\.\d+)['\"]", s)
configured = parse_versions(settings_match.group(1))
aggregated = parse_versions(root_match.group(1))
assert configured and len(configured) == len(set(configured)), f"Invalid Stonecutter versions: {configured}"
assert configured == aggregated, f"Aggregation differs from Stonecutter: {configured} != {aggregated}"

for removed_api in ("registerChiseled", "stonecutter.chiseled", "ofTask("):
    # Ignore comments in the controller, search code lines only.
    active_controller = "\n".join(x for x in controller.splitlines() if not x.lstrip().startswith("//"))
    assert removed_api not in active_controller, f"Removed Stonecutter API still used: {removed_api}"
assert "stonecutter active \"26.1\"" in controller, "Incorrect checked-in version"
assert ':crystal-config:chiseled' not in root_build
assert ':crystal-config:${version}:buildAndCollect' in root_build
assert ':crystal-config:${version}:publishToMavenLocal' in root_build
assert 'register<Copy>("buildAndCollect")' in node_build
assert 'create<MavenPublication>("versioned")' in node_build

for path in ("jitpack.yml", ".github/workflows/build-mod.yml", "docs/DISTRIBUTION.md"):
    value = (root / path).read_text(encoding="utf-8")
    assert "chiseledPublishToMavenLocal" not in value, f"Obsolete task in {path}"
    assert "publishModToMavenLocal" in value, f"New publication task missing in {path}"
print("Gradle task wiring OK: " + ", ".join(configured))
