package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.input.KeyCodes;

import java.util.Objects;

public final class Keybind {
    private final int keyCode;
    private final String displayName;
    private final boolean mouse;

    public Keybind(int keyCode, String displayName) {
        this(keyCode, displayName, false);
    }

    public Keybind(int keyCode, String displayName, boolean mouse) {
        this.keyCode = keyCode;
        this.mouse = mouse;
        this.displayName = mouse
                ? KeybindUtils.mouseButtonDisplayName(keyCode)
                : KeybindUtils.keyDisplayName(keyCode, displayName);
    }

    public static Keybind none() {
        return new Keybind(KeyCodes.UNKNOWN, "None", false);
    }

    public static Keybind glfwKey(int keyCode) {
        boolean mouse = KeybindUtils.isMouseButton(keyCode);
        String displayName = mouse
                ? KeybindUtils.mouseButtonDisplayName(keyCode)
                : KeybindUtils.glfwKeyDisplayName(keyCode);
        return new Keybind(keyCode, displayName, mouse);
    }

    public static Keybind glfwKey(int keyCode, String displayName) {
        boolean mouse = KeybindUtils.isMouseButton(keyCode);
        return new Keybind(keyCode, displayName, mouse);
    }

    public int keyCode() {
        return keyCode;
    }

    public String displayName() {
        return displayName;
    }

    public int glfwKey() {
        return isKeyboardKey() ? keyCode : KeyCodes.UNKNOWN;
    }

    public int glfwMouseButton() {
        return isMouseButton() ? keyCode : -1;
    }

    public boolean isKeyboardKey() {
        return !mouse && KeybindUtils.isKey(keyCode);
    }

    public boolean isMouseButton() {
        return mouse && KeybindUtils.isMouseButton(keyCode);
    }

    public boolean isNone() {
        return !mouse && keyCode == KeyCodes.UNKNOWN;
    }

    public boolean matchesKey(int key) {
        return isKeyboardKey() && keyCode == key;
    }

    public boolean matchesMouse(int button) {
        return isMouseButton() && keyCode == button;
    }

    public String display() {
        return isNone() ? "None" : displayName;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Keybind other)) return false;
        return keyCode == other.keyCode && mouse == other.mouse && Objects.equals(displayName, other.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyCode, displayName, mouse);
    }

    @Override
    public String toString() {
        return "Keybind[keyCode=" + keyCode + ", displayName=" + displayName + ", mouse=" + mouse + ']';
    }
}
