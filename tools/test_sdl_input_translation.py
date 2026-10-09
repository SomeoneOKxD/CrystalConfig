#!/usr/bin/env python3
"""Standalone version-sensitive input regression tests (no Minecraft download needed).

The stub constants below use actual SDL3 / 26.3 InputConstants values; the
Minecraft CI build still compiles against the real game, not these stubs.
"""
from __future__ import annotations

import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLASS = ROOT / "crystal-config/src/main/java/dev/someoneok/crystalconfig/render/MinecraftInputCompat.java"
INPUT_ROOT = ROOT / "core/src/main/java/dev/someoneok/crystalconfig/input"

# SDL3 scancodes referenced in com.mojang.blaze3d.platform.InputConstants.
SDL_SCANCODES = {
    **{f"KEY_{c}": n for n, c in enumerate("ABCDEFGHIJKLMNOPQRSTUVWXYZ", start=4)},
    **{f"KEY_{digit}": n for n, digit in enumerate("123456789", start=30)},
    "KEY_0": 39,
    "KEY_RETURN": 40, "KEY_ESCAPE": 41, "KEY_BACKSPACE": 42, "KEY_TAB": 43,
    "KEY_SPACE": 44, "KEY_MINUS": 45, "KEY_EQUALS": 46, "KEY_LBRACKET": 47,
    "KEY_RBRACKET": 48, "KEY_BACKSLASH": 49, "KEY_SEMICOLON": 51,
    "KEY_APOSTROPHE": 52, "KEY_GRAVE": 53, "KEY_COMMA": 54,
    "KEY_PERIOD": 55, "KEY_SLASH": 56, "KEY_CAPSLOCK": 57,
    **{f"KEY_F{i}": 57 + i for i in range(1, 13)},
    "KEY_PRINTSCREEN": 70, "KEY_SCROLLLOCK": 71, "KEY_PAUSE": 72,
    "KEY_INSERT": 73, "KEY_HOME": 74, "KEY_PAGEUP": 75,
    "KEY_DELETE": 76, "KEY_END": 77, "KEY_PAGEDOWN": 78,
    "KEY_RIGHT": 79, "KEY_LEFT": 80, "KEY_DOWN": 81, "KEY_UP": 82,
    "KEY_NUMLOCK": 83, "KEY_MULTIPLY": 85, "KEY_ADD": 87,
    "KEY_NUMPADENTER": 88,
    **{f"KEY_NUMPAD{i}": 88 + i for i in range(1, 10)},
    "KEY_NUMPAD0": 98, "KEY_NUMPADEQUALS": 103,
    **{f"KEY_F{i}": 104 + (i - 13) for i in range(13, 25)},
    "KEY_LCONTROL": 224, "KEY_LSHIFT": 225, "KEY_LALT": 226, "KEY_LGUI": 227,
    "KEY_RCONTROL": 228, "KEY_RSHIFT": 229, "KEY_RALT": 230, "KEY_RGUI": 231,
    "KEY_NUMPADCOMMA": 220,
}
SDL_INPUT = {
    **SDL_SCANCODES,
    "MOUSE_BUTTON_LEFT": 1, "MOUSE_BUTTON_MIDDLE": 2, "MOUSE_BUTTON_RIGHT": 3,
    **{f"MOUSE_BUTTON_{i}": i for i in range(4, 9)},
    "MOD_SHIFT": 3, "MOD_CONTROL": 192, "MOD_ALT": 768, "MOD_SUPER": 3072,
    "MOD_CAPS_LOCK": 8192, "MOD_NUM_LOCK": 4096,
}


def select_version(source: str) -> str:
    # Only generate the 26.3 alternative for this *standalone* compatibility test.
    # Minecraft 26.1 is tested directly from the raw source file, not this output.
    with_else = re.compile(
        r"(?m)^[ \t]*//\? if >=26\.3 \{\n(.*?)^[ \t]*\*///\?\} else \{\n(.*?)^[ \t]*//\?\}\n",
        re.S | re.M,
    )

    def choose_sdl(match: re.Match[str]) -> str:
        first = match.group(1)
        if not first.lstrip().startswith("/*"):
            raise AssertionError("26.3 branch should be inactive in checked-in source")
        if match.group(2).lstrip().startswith("/*"):
            raise AssertionError("26.1 branch should be active in checked-in source")
        return first.replace("/*", "", 1)

    result, count = with_else.subn(choose_sdl, source)
    assert count == 3, f"expected three input compatibility blocks, found {count}"
    assert "//?" not in result, "unexpected conditional marker"
    return result


def main() -> None:
    original = CLASS.read_text(encoding="utf-8")
    names = set(re.findall(r"InputConstants\.(\w+)", original))
    absent = names - SDL_INPUT.keys()
    if absent:
        raise AssertionError(f"InputConstants fixtures missing fields: {sorted(absent)}")
    stub = "package com.mojang.blaze3d.platform;\npublic final class InputConstants {\n"
    stub += "\n".join(f"  public static final int {k} = {SDL_INPUT[k]};" for k in sorted(names))
    stub += "\n}\n"
    harness = r'''package dev.someoneok.crystalconfig.render;
import dev.someoneok.crystalconfig.input.*;
final class InputContractTest {
    private static void eq(int actual, int expected, String message) {
        if (actual != expected) throw new AssertionError(message + ": " + actual + " != " + expected);
    }
    public static void main(String[] args) {
        boolean sdl = Boolean.parseBoolean(args[0]);
        if (sdl) {
            eq(MinecraftInputCompat.mouseButton(1), 0, "SDL left");
            eq(MinecraftInputCompat.mouseButton(3), 1, "SDL right");
            eq(MinecraftInputCompat.mouseButton(2), 2, "SDL middle");
            eq(MinecraftInputCompat.mouseButton(8), 7, "SDL last aux");
            eq(MinecraftInputCompat.mouseButton(0), -1, "SDL unknown");
            if (MouseButton.fromIndex(MinecraftInputCompat.mouseButton(1)) != MouseButton.LEFT)
                throw new AssertionError("left-click must stay left-click");
            if (MouseButton.fromIndex(MinecraftInputCompat.mouseButton(3)) != MouseButton.RIGHT)
                throw new AssertionError("right-click must stay right-click");
            eq(MinecraftInputCompat.modifiers(1), Modifiers.SHIFT, "SDL left shift");
            eq(MinecraftInputCompat.modifiers(2), Modifiers.SHIFT, "SDL right shift");
            eq(MinecraftInputCompat.modifiers(64), Modifiers.CTRL, "SDL left ctrl");
            eq(MinecraftInputCompat.modifiers(128), Modifiers.CTRL, "SDL right ctrl");
            eq(MinecraftInputCompat.modifiers(192 | 3 | 768),
               Modifiers.CTRL | Modifiers.SHIFT | Modifiers.ALT, "combined modifiers");
            eq(MinecraftInputCompat.modifiers(8192 | 4096), Modifiers.CAPS_LOCK | Modifiers.NUM_LOCK, "locks");
            eq(MinecraftInputCompat.keyboardKey(4, 97), KeyCodes.KEY_A, "SDL A");
            eq(MinecraftInputCompat.keyboardKey(4, 122), KeyCodes.KEY_Z, "layout-aware shortcut");
            eq(MinecraftInputCompat.keyboardKey(41, 27), KeyCodes.ESCAPE, "escape");
            eq(MinecraftInputCompat.keyboardKey(42, 8), KeyCodes.BACKSPACE, "backspace");
            eq(MinecraftInputCompat.keyboardKey(80, 1073741904), KeyCodes.LEFT, "left arrow");
            eq(MinecraftInputCompat.keyboardKey(81, 1073741905), KeyCodes.DOWN, "down arrow");
            eq(MinecraftInputCompat.keyboardKey(62, 1073741886), KeyCodes.F5, "F5");
            eq(MinecraftInputCompat.keyboardKey(89, 1073741913), KeyCodes.KP_1, "keypad 1");
            eq(MinecraftInputCompat.keyboardKey(84, 0), KeyCodes.KP_DIVIDE, "SDL keypad divide");
            eq(MinecraftInputCompat.keyboardKey(86, 0), KeyCodes.KP_SUBTRACT, "SDL keypad minus");
            eq(MinecraftInputCompat.keyboardKey(99, 0), KeyCodes.KP_DECIMAL, "SDL keypad decimal");
            eq(MinecraftInputCompat.keyboardKey(225, 1073742049), KeyCodes.LEFT_SHIFT, "left shift");
            eq(MinecraftInputCompat.keyboardKey(228, 1073742052), KeyCodes.RIGHT_CONTROL, "right ctrl");
            eq(MinecraftInputCompat.keyboardKey(0, 0), KeyCodes.UNKNOWN, "unknown key");
        } else {
            eq(MinecraftInputCompat.mouseButton(0), 0, "GLFW left");
            eq(MinecraftInputCompat.mouseButton(1), 1, "GLFW right");
            eq(MinecraftInputCompat.modifiers(2), Modifiers.CTRL, "GLFW ctrl");
            eq(MinecraftInputCompat.keyboardKey(KeyCodes.KEY_A, 123), KeyCodes.KEY_A, "GLFW key");
        }
        System.out.println("Input contract passed: " + (sdl ? "26.3 SDL" : "26.1/26.2 GLFW"));
    }
}
'''
    with tempfile.TemporaryDirectory(prefix="crystal-sdl-test-") as tmp:
        folder = Path(tmp)
        java = folder / "InputConstants.java"
        java.write_text(stub)
        for sdl in (False, True):
            if sdl:
                source = folder / "MinecraftInputCompat.java"
                source.write_text(select_version(original))
            else:
                # The active 26.1 Stonecutter node compiles the actual shared
                # source; never preprocess it in a test or regressions can hide.
                source = CLASS
            driver = folder / "InputContractTest.java"
            driver.write_text(harness)
            classes = folder / ("sdl" if sdl else "glfw")
            classes.mkdir()
            subprocess.run([
                "javac", "-encoding", "UTF-8", "-d", str(classes),
                str(java), str(source), str(driver),
                *(str(INPUT_ROOT / n) for n in ("KeyCodes.java", "Modifiers.java", "MouseButton.java")),
            ], check=True)
            subprocess.run(["java", "-cp", str(classes),
                            "dev.someoneok.crystalconfig.render.InputContractTest",
                            str(sdl).lower()], check=True)


if __name__ == "__main__":
    main()
