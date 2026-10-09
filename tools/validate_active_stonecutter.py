#!/usr/bin/env python3
"""Validate Stonecutter's *checked-in* source for the active Minecraft version.

Stonecutter compiles its active version directly from src/main/java; only inactive
version nodes use generated/preprocessed sources. A perfectly reasonable-looking
`//? if` directive can therefore still break the active version when the wrong
branch was left uncommented. This test checks each directive's real Java comment
state, not merely a simulated preprocessed copy.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "crystal-config/src/main/java"
CONTROLLER = ROOT / "crystal-config/stonecutter.gradle.kts"
START = re.compile(r"^\s*//\? if (.+?) \{$")
ELSE = re.compile(r"^\s*(\*/)?//\?\} else \{$")
END = re.compile(r"^\s*(\*/)?//\?\}$")
COMPARISON = re.compile(r"(>=|<=|==|!=|>|<)?\s*(\d+(?:\.\d+)*)$")


def version_tuple(version: str) -> tuple[int, ...]:
    return tuple(int(x) for x in version.split("."))


def matches(condition: str, active: str) -> bool:
    # All directives in this project currently use comparisons joined by &&.
    # Reject unexpected expression syntax rather than silently passing it.
    for term in condition.split("&&"):
        match = COMPARISON.fullmatch(term.strip())
        if not match:
            raise ValueError(f"unsupported Stonecutter condition: {condition!r}")
        operator, target = match.groups()
        a, b = version_tuple(active), version_tuple(target)
        valid = {
            None: a == b, "==": a == b, "!=": a != b,
            ">": a > b, ">=": a >= b, "<": a < b, "<=": a <= b,
        }[operator]
        if not valid:
            return False
    return True


def check_branch(path: Path, lines: list[str], start: int, end: int,
                 enabled: bool, closing_has_comment: bool) -> None:
    content = "".join(lines[start:end])
    if not content.strip():
        raise AssertionError(f"{path}:{start+1}: empty conditional branch")
    has_open_comment = content.lstrip().startswith("/*")
    if enabled and (has_open_comment or closing_has_comment):
        raise AssertionError(
            f"{path}:{start+1}: active branch is commented out (should be live)")
    if not enabled and (not has_open_comment or not closing_has_comment):
        raise AssertionError(
            f"{path}:{start+1}: inactive branch is live (should be block-commented)")


def check_file(path: Path, active: str) -> int:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    blocks = []
    for index, line in enumerate(lines):
        stripped = line.rstrip("\r\n")
        start = START.fullmatch(stripped)
        alt = ELSE.fullmatch(stripped)
        end = END.fullmatch(stripped)
        if start:
            if blocks:
                raise AssertionError(f"{path}:{index+1}: nested directives require extending this checker")
            blocks.append({"condition": matches(start.group(1), active), "begin": index + 1})
        elif alt:
            if not blocks or "else" in blocks[-1]:
                raise AssertionError(f"{path}:{index+1}: unmatched or duplicate else")
            block = blocks[-1]
            check_branch(path, lines, block["begin"], index,
                         block["condition"], bool(alt.group(1)))
            block["else"] = True
            block["begin"] = index + 1
        elif end:
            if not blocks:
                raise AssertionError(f"{path}:{index+1}: unmatched end")
            block = blocks.pop()
            enabled = not block["condition"] if "else" in block else block["condition"]
            check_branch(path, lines, block["begin"], index,
                         enabled, bool(end.group(1)))
        elif "//?" in line:
            raise AssertionError(f"{path}:{index+1}: unsupported Stonecutter directive: {stripped}")
    if blocks:
        raise AssertionError(f"{path}: unclosed Stonecutter conditional")
    return sum(bool(START.fullmatch(line.rstrip("\r\n"))) for line in lines)


def main() -> None:
    controller = CONTROLLER.read_text(encoding="utf-8")
    found = re.search(r'stonecutter\s+active\s+"([^"]+)"', controller)
    if not found:
        raise AssertionError("cannot read active Stonecutter version")
    active = found.group(1)
    if active != "26.1":
        raise AssertionError(f"expected checked-in source to target 26.1, got {active}")
    total = 0
    paths = sorted(p for p in SOURCE.rglob("*.java") if "//?" in p.read_text(encoding="utf-8"))
    if not paths:
        raise AssertionError("no Stonecutter conditional Java sources found")
    for path in paths:
        total += check_file(path, active)
    if total < 15:
        raise AssertionError(f"only {total} conditionals checked; expected 15 or more")
    print(f"Active Stonecutter source validated for Minecraft {active}: "
          f"{total} conditional blocks in {len(paths)} Java files")


if __name__ == "__main__":
    main()
