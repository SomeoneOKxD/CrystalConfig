package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.input.KeyCodes;

public record Keybind(int keyCode, String displayName) {
    public static Keybind none() {
        return new Keybind(KeyCodes.UNKNOWN, "None");
    }

    public static Keybind glfwKey(int keyCode) {
        return KeybindUtils.fromGlfwKey(keyCode);
    }

    public static Keybind glfwKey(int keyCode, String displayName) {
        return KeybindUtils.fromGlfwKey(keyCode, displayName);
    }

    public static Keybind glfwMouseButton(int button) {
        return KeybindUtils.fromGlfwMouseButton(button);
    }

    public int glfwKey() {
        return keyCode;
    }

    public boolean isNone() {
        return keyCode == KeyCodes.UNKNOWN;
    }

    public boolean matchesGlfwKey(int keyCode) {
        return KeybindUtils.matchesGlfwKey(this, keyCode);
    }

    public boolean matchesGlfwMouseButton(int button) {
        return KeybindUtils.matchesGlfwMouseButton(this, button);
    }

    public String display() {
        return isNone() ? "None" : KeybindUtils.displayName(keyCode, displayName);
    }
}
