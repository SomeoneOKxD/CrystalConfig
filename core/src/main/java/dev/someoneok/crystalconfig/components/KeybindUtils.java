package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.input.KeyCodes;

import java.util.Locale;

/**
 * Utilities for storing and matching raw GLFW-compatible keybind codes.
 *
 * <p>The keybind model intentionally stores only the raw GLFW code and a display
 * name. Keyboard binds store the GLFW key code. Mouse binds store the raw GLFW
 * mouse button value directly. No offset/encoding layer is used.</p>
 */
public final class KeybindUtils {
    public static final int GLFW_MOUSE_BUTTON_1 = 0;
    public static final int GLFW_MOUSE_BUTTON_2 = 1;
    public static final int GLFW_MOUSE_BUTTON_3 = 2;
    public static final int GLFW_MOUSE_BUTTON_4 = 3;
    public static final int GLFW_MOUSE_BUTTON_5 = 4;
    public static final int GLFW_MOUSE_BUTTON_6 = 5;
    public static final int GLFW_MOUSE_BUTTON_7 = 6;
    public static final int GLFW_MOUSE_BUTTON_8 = 7;
    public static final int GLFW_MOUSE_BUTTON_LEFT = GLFW_MOUSE_BUTTON_1;
    public static final int GLFW_MOUSE_BUTTON_RIGHT = GLFW_MOUSE_BUTTON_2;
    public static final int GLFW_MOUSE_BUTTON_MIDDLE = GLFW_MOUSE_BUTTON_3;

    private KeybindUtils() { }

    public static Keybind fromGlfwKey(int keyCode) {
        return fromGlfwKey(keyCode, null);
    }

    public static Keybind fromGlfwKey(int keyCode, String displayName) {
        if (keyCode == KeyCodes.UNKNOWN) return Keybind.none();
        return new Keybind(keyCode, displayName(keyCode, displayName));
    }

    public static Keybind fromGlfwMouseButton(int button) {
        if (!isAssignableGlfwMouseButton(button)) return Keybind.none();
        return new Keybind(button, mouseButtonDisplayName(button));
    }

    public static boolean isGlfwMouseButton(int button) {
        return button >= GLFW_MOUSE_BUTTON_1 && button <= GLFW_MOUSE_BUTTON_8;
    }

    public static boolean isReservedPrimaryMouseButton(int button) {
        return button == GLFW_MOUSE_BUTTON_LEFT
                || button == GLFW_MOUSE_BUTTON_RIGHT
                || button == GLFW_MOUSE_BUTTON_MIDDLE;
    }

    public static boolean isAssignableGlfwMouseButton(int button) {
        return isGlfwMouseButton(button) && !isReservedPrimaryMouseButton(button);
    }

    public static boolean matchesGlfwKey(Keybind keybind, int keyCode) {
        return keybind != null && !keybind.isNone() && keybind.keyCode() == keyCode;
    }

    public static boolean matchesGlfwMouseButton(Keybind keybind, int button) {
        return keybind != null && !keybind.isNone() && isAssignableGlfwMouseButton(button) && keybind.keyCode() == button;
    }

    public static String displayName(int keyCode, String platformDisplayName) {
        String platform = cleanPlatformName(platformDisplayName);
        if (platform != null) return platform;
        return glfwKeyDisplayName(keyCode);
    }

    private static String cleanPlatformName(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        if (cleaned.isEmpty()) return null;
        if (cleaned.startsWith("key.keyboard.")) cleaned = cleaned.substring("key.keyboard.".length());
        if (cleaned.startsWith("key.mouse.")) cleaned = cleaned.substring("key.mouse.".length());
        if (cleaned.length() == 1) return cleaned.toUpperCase(Locale.ROOT);
        return switch (cleaned) {
            case "unknown" -> null;
            case "left.shift" -> "Left Shift";
            case "right.shift" -> "Right Shift";
            case "left.control" -> "Left Ctrl";
            case "right.control" -> "Right Ctrl";
            case "left.alt" -> "Left Alt";
            case "right.alt" -> "Right Alt";
            case "left.win", "left.super" -> "Left Super";
            case "right.win", "right.super" -> "Right Super";
            case "page.up" -> "Page Up";
            case "page.down" -> "Page Down";
            default -> prettifyToken(cleaned);
        };
    }

    private static String prettifyToken(String token) {
        String[] parts = token.replace('_', '.').replace('-', '.').split("\\.");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            if (part.equals("ctrl")) out.append("Ctrl");
            else if (part.length() == 1) out.append(part.toUpperCase(Locale.ROOT));
            else out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.isEmpty() ? token : out.toString();
    }

    public static String mouseButtonDisplayName(int button) {
        return switch (button) {
            case GLFW_MOUSE_BUTTON_LEFT -> "Mouse Left";
            case GLFW_MOUSE_BUTTON_RIGHT -> "Mouse Right";
            case GLFW_MOUSE_BUTTON_MIDDLE -> "Mouse Middle";
            case GLFW_MOUSE_BUTTON_4 -> "Mouse 4";
            case GLFW_MOUSE_BUTTON_5 -> "Mouse 5";
            case GLFW_MOUSE_BUTTON_6 -> "Mouse 6";
            case GLFW_MOUSE_BUTTON_7 -> "Mouse 7";
            case GLFW_MOUSE_BUTTON_8 -> "Mouse 8";
            default -> "Mouse " + (button + 1);
        };
    }

    public static String glfwKeyDisplayName(int keyCode) {
        if (keyCode >= KeyCodes.KEY_0 && keyCode <= KeyCodes.KEY_9) return String.valueOf((char) keyCode);
        if (keyCode >= KeyCodes.KEY_A && keyCode <= KeyCodes.KEY_Z) return String.valueOf((char) keyCode);
        if (keyCode >= KeyCodes.F1 && keyCode <= KeyCodes.F25) return "F" + (keyCode - KeyCodes.F1 + 1);
        if (keyCode >= KeyCodes.KP_0 && keyCode <= KeyCodes.KP_9) return "Numpad " + (keyCode - KeyCodes.KP_0);
        if (isGlfwMouseButton(keyCode)) return mouseButtonDisplayName(keyCode);

        return switch (keyCode) {
            case KeyCodes.UNKNOWN -> "Unknown";
            case KeyCodes.SPACE -> "Space";
            case KeyCodes.APOSTROPHE -> "Apostrophe";
            case KeyCodes.COMMA -> "Comma";
            case KeyCodes.MINUS -> "Minus";
            case KeyCodes.PERIOD -> "Period";
            case KeyCodes.SLASH -> "Slash";
            case KeyCodes.SEMICOLON -> "Semicolon";
            case KeyCodes.EQUAL -> "Equal";
            case KeyCodes.LEFT_BRACKET -> "Left Bracket";
            case KeyCodes.BACKSLASH -> "Backslash";
            case KeyCodes.RIGHT_BRACKET -> "Right Bracket";
            case KeyCodes.GRAVE_ACCENT -> "Grave Accent";
            case KeyCodes.WORLD_1 -> "World 1";
            case KeyCodes.WORLD_2 -> "World 2";
            case KeyCodes.ESCAPE -> "Escape";
            case KeyCodes.ENTER -> "Enter";
            case KeyCodes.TAB -> "Tab";
            case KeyCodes.BACKSPACE -> "Backspace";
            case KeyCodes.INSERT -> "Insert";
            case KeyCodes.DELETE -> "Delete";
            case KeyCodes.RIGHT -> "Right";
            case KeyCodes.LEFT -> "Left";
            case KeyCodes.DOWN -> "Down";
            case KeyCodes.UP -> "Up";
            case KeyCodes.PAGE_UP -> "Page Up";
            case KeyCodes.PAGE_DOWN -> "Page Down";
            case KeyCodes.HOME -> "Home";
            case KeyCodes.END -> "End";
            case KeyCodes.CAPS_LOCK -> "Caps Lock";
            case KeyCodes.SCROLL_LOCK -> "Scroll Lock";
            case KeyCodes.NUM_LOCK -> "Num Lock";
            case KeyCodes.PRINT_SCREEN -> "Print Screen";
            case KeyCodes.PAUSE -> "Pause";
            case KeyCodes.KP_DECIMAL -> "Numpad Decimal";
            case KeyCodes.KP_DIVIDE -> "Numpad Divide";
            case KeyCodes.KP_MULTIPLY -> "Numpad Multiply";
            case KeyCodes.KP_SUBTRACT -> "Numpad Subtract";
            case KeyCodes.KP_ADD -> "Numpad Add";
            case KeyCodes.KP_ENTER -> "Numpad Enter";
            case KeyCodes.KP_EQUAL -> "Numpad Equal";
            case KeyCodes.LEFT_SHIFT -> "Left Shift";
            case KeyCodes.LEFT_CONTROL -> "Left Ctrl";
            case KeyCodes.LEFT_ALT -> "Left Alt";
            case KeyCodes.LEFT_SUPER -> "Left Super";
            case KeyCodes.RIGHT_SHIFT -> "Right Shift";
            case KeyCodes.RIGHT_CONTROL -> "Right Ctrl";
            case KeyCodes.RIGHT_ALT -> "Right Alt";
            case KeyCodes.RIGHT_SUPER -> "Right Super";
            case KeyCodes.MENU -> "Menu";
            default -> "Key " + keyCode;
        };
    }
}
