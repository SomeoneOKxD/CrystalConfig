package dev.someoneok.crystalconfig.render;

import com.mojang.blaze3d.platform.InputConstants;
import dev.someoneok.crystalconfig.input.KeyCodes;
import dev.someoneok.crystalconfig.input.Modifiers;

/**
 * Translates Minecraft 26.3 SDL input at the Minecraft boundary. The independent UI
 * modules deliberately retain their GLFW-compatible key/mouse/modifier contract.
 * Named scancodes use Mojang InputConstants; keypad /, - and decimal use
 * their stable SDL3 physical scancode numbers (84, 86 and 99).
 */
final class MinecraftInputCompat {
    private MinecraftInputCompat() { }

    static int keyboardKey(int physicalKey, int virtualKey) {
        //? if >=26.3 {
        /*// SDL virtual keycodes are layout-aware (used for Ctrl+A/C/X/V), while
        // SDL physical scancodes handle navigation, keypad and game-style binds.
        if (virtualKey >= 'a' && virtualKey <= 'z') return KeyCodes.KEY_A + virtualKey - 'a';
        if (virtualKey >= 'A' && virtualKey <= 'Z') return KeyCodes.KEY_A + virtualKey - 'A';
        return switch (physicalKey) {
            case InputConstants.KEY_A -> KeyCodes.KEY_A;
            case InputConstants.KEY_B -> KeyCodes.KEY_B;
            case InputConstants.KEY_C -> KeyCodes.KEY_C;
            case InputConstants.KEY_D -> KeyCodes.KEY_D;
            case InputConstants.KEY_E -> KeyCodes.KEY_E;
            case InputConstants.KEY_F -> KeyCodes.KEY_F;
            case InputConstants.KEY_G -> KeyCodes.KEY_G;
            case InputConstants.KEY_H -> KeyCodes.KEY_H;
            case InputConstants.KEY_I -> KeyCodes.KEY_I;
            case InputConstants.KEY_J -> KeyCodes.KEY_J;
            case InputConstants.KEY_K -> KeyCodes.KEY_K;
            case InputConstants.KEY_L -> KeyCodes.KEY_L;
            case InputConstants.KEY_M -> KeyCodes.KEY_M;
            case InputConstants.KEY_N -> KeyCodes.KEY_N;
            case InputConstants.KEY_O -> KeyCodes.KEY_O;
            case InputConstants.KEY_P -> KeyCodes.KEY_P;
            case InputConstants.KEY_Q -> KeyCodes.KEY_Q;
            case InputConstants.KEY_R -> KeyCodes.KEY_R;
            case InputConstants.KEY_S -> KeyCodes.KEY_S;
            case InputConstants.KEY_T -> KeyCodes.KEY_T;
            case InputConstants.KEY_U -> KeyCodes.KEY_U;
            case InputConstants.KEY_V -> KeyCodes.KEY_V;
            case InputConstants.KEY_W -> KeyCodes.KEY_W;
            case InputConstants.KEY_X -> KeyCodes.KEY_X;
            case InputConstants.KEY_Y -> KeyCodes.KEY_Y;
            case InputConstants.KEY_Z -> KeyCodes.KEY_Z;
            case InputConstants.KEY_0 -> KeyCodes.KEY_0;
            case InputConstants.KEY_1 -> KeyCodes.KEY_1;
            case InputConstants.KEY_2 -> KeyCodes.KEY_2;
            case InputConstants.KEY_3 -> KeyCodes.KEY_3;
            case InputConstants.KEY_4 -> KeyCodes.KEY_4;
            case InputConstants.KEY_5 -> KeyCodes.KEY_5;
            case InputConstants.KEY_6 -> KeyCodes.KEY_6;
            case InputConstants.KEY_7 -> KeyCodes.KEY_7;
            case InputConstants.KEY_8 -> KeyCodes.KEY_8;
            case InputConstants.KEY_9 -> KeyCodes.KEY_9;
            case InputConstants.KEY_F1 -> KeyCodes.F1;
            case InputConstants.KEY_F2 -> KeyCodes.F2;
            case InputConstants.KEY_F3 -> KeyCodes.F3;
            case InputConstants.KEY_F4 -> KeyCodes.F4;
            case InputConstants.KEY_F5 -> KeyCodes.F5;
            case InputConstants.KEY_F6 -> KeyCodes.F6;
            case InputConstants.KEY_F7 -> KeyCodes.F7;
            case InputConstants.KEY_F8 -> KeyCodes.F8;
            case InputConstants.KEY_F9 -> KeyCodes.F9;
            case InputConstants.KEY_F10 -> KeyCodes.F10;
            case InputConstants.KEY_F11 -> KeyCodes.F11;
            case InputConstants.KEY_F12 -> KeyCodes.F12;
            case InputConstants.KEY_F13 -> KeyCodes.F13;
            case InputConstants.KEY_F14 -> KeyCodes.F14;
            case InputConstants.KEY_F15 -> KeyCodes.F15;
            case InputConstants.KEY_F16 -> KeyCodes.F16;
            case InputConstants.KEY_F17 -> KeyCodes.F17;
            case InputConstants.KEY_F18 -> KeyCodes.F18;
            case InputConstants.KEY_F19 -> KeyCodes.F19;
            case InputConstants.KEY_F20 -> KeyCodes.F20;
            case InputConstants.KEY_F21 -> KeyCodes.F21;
            case InputConstants.KEY_F22 -> KeyCodes.F22;
            case InputConstants.KEY_F23 -> KeyCodes.F23;
            case InputConstants.KEY_F24 -> KeyCodes.F24;
            case InputConstants.KEY_NUMPAD0 -> KeyCodes.KP_0;
            case InputConstants.KEY_NUMPAD1 -> KeyCodes.KP_1;
            case InputConstants.KEY_NUMPAD2 -> KeyCodes.KP_2;
            case InputConstants.KEY_NUMPAD3 -> KeyCodes.KP_3;
            case InputConstants.KEY_NUMPAD4 -> KeyCodes.KP_4;
            case InputConstants.KEY_NUMPAD5 -> KeyCodes.KP_5;
            case InputConstants.KEY_NUMPAD6 -> KeyCodes.KP_6;
            case InputConstants.KEY_NUMPAD7 -> KeyCodes.KP_7;
            case InputConstants.KEY_NUMPAD8 -> KeyCodes.KP_8;
            case InputConstants.KEY_NUMPAD9 -> KeyCodes.KP_9;
            case InputConstants.KEY_SPACE -> KeyCodes.SPACE;
            case InputConstants.KEY_APOSTROPHE -> KeyCodes.APOSTROPHE;
            case InputConstants.KEY_COMMA -> KeyCodes.COMMA;
            case InputConstants.KEY_MINUS -> KeyCodes.MINUS;
            case InputConstants.KEY_PERIOD -> KeyCodes.PERIOD;
            case InputConstants.KEY_SLASH -> KeyCodes.SLASH;
            case InputConstants.KEY_SEMICOLON -> KeyCodes.SEMICOLON;
            case InputConstants.KEY_EQUALS -> KeyCodes.EQUAL;
            case InputConstants.KEY_LBRACKET -> KeyCodes.LEFT_BRACKET;
            case InputConstants.KEY_BACKSLASH -> KeyCodes.BACKSLASH;
            case InputConstants.KEY_RBRACKET -> KeyCodes.RIGHT_BRACKET;
            case InputConstants.KEY_GRAVE -> KeyCodes.GRAVE_ACCENT;
            case InputConstants.KEY_ESCAPE -> KeyCodes.ESCAPE;
            case InputConstants.KEY_RETURN -> KeyCodes.ENTER;
            case InputConstants.KEY_TAB -> KeyCodes.TAB;
            case InputConstants.KEY_BACKSPACE -> KeyCodes.BACKSPACE;
            case InputConstants.KEY_INSERT -> KeyCodes.INSERT;
            case InputConstants.KEY_DELETE -> KeyCodes.DELETE;
            case InputConstants.KEY_RIGHT -> KeyCodes.RIGHT;
            case InputConstants.KEY_LEFT -> KeyCodes.LEFT;
            case InputConstants.KEY_DOWN -> KeyCodes.DOWN;
            case InputConstants.KEY_UP -> KeyCodes.UP;
            case InputConstants.KEY_PAGEUP -> KeyCodes.PAGE_UP;
            case InputConstants.KEY_PAGEDOWN -> KeyCodes.PAGE_DOWN;
            case InputConstants.KEY_HOME -> KeyCodes.HOME;
            case InputConstants.KEY_END -> KeyCodes.END;
            case InputConstants.KEY_CAPSLOCK -> KeyCodes.CAPS_LOCK;
            case InputConstants.KEY_SCROLLLOCK -> KeyCodes.SCROLL_LOCK;
            case InputConstants.KEY_NUMLOCK -> KeyCodes.NUM_LOCK;
            case InputConstants.KEY_PRINTSCREEN -> KeyCodes.PRINT_SCREEN;
            case InputConstants.KEY_PAUSE -> KeyCodes.PAUSE;
            // SDL3 physical scancodes: keypad / = 84, - = 86, decimal point = 99.
            // Use these stable scancodes rather than layout-dependent virtual keycodes.
            case 84 -> KeyCodes.KP_DIVIDE;
            case InputConstants.KEY_MULTIPLY -> KeyCodes.KP_MULTIPLY;
            case 86 -> KeyCodes.KP_SUBTRACT;
            case 99 -> KeyCodes.KP_DECIMAL;
            case InputConstants.KEY_NUMPADENTER -> KeyCodes.KP_ENTER;
            case InputConstants.KEY_NUMPADEQUALS -> KeyCodes.KP_EQUAL;
            case InputConstants.KEY_LSHIFT -> KeyCodes.LEFT_SHIFT;
            case InputConstants.KEY_LCONTROL -> KeyCodes.LEFT_CONTROL;
            case InputConstants.KEY_LALT -> KeyCodes.LEFT_ALT;
            case InputConstants.KEY_LGUI -> KeyCodes.LEFT_SUPER;
            case InputConstants.KEY_RSHIFT -> KeyCodes.RIGHT_SHIFT;
            case InputConstants.KEY_RCONTROL -> KeyCodes.RIGHT_CONTROL;
            case InputConstants.KEY_RALT -> KeyCodes.RIGHT_ALT;
            case InputConstants.KEY_RGUI -> KeyCodes.RIGHT_SUPER;
            case InputConstants.KEY_ADD -> KeyCodes.KP_ADD;
            default -> KeyCodes.UNKNOWN;
        };
        *///?} else {
        return physicalKey;
        //?}
    }

    static int mouseButton(int nativeButton) {
        //? if >=26.3 {
        /*return switch (nativeButton) {
            case InputConstants.MOUSE_BUTTON_LEFT -> 0;
            case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> 2;
            case InputConstants.MOUSE_BUTTON_4 -> 3;
            case InputConstants.MOUSE_BUTTON_5 -> 4;
            case InputConstants.MOUSE_BUTTON_6 -> 5;
            case InputConstants.MOUSE_BUTTON_7 -> 6;
            case InputConstants.MOUSE_BUTTON_8 -> 7;
            default -> -1;
        };
        *///?} else {
        return nativeButton;
        //?}
    }

    static int modifiers(int nativeModifiers) {
        //? if >=26.3 {
        /*int result = 0;
        if ((nativeModifiers & InputConstants.MOD_SHIFT) != 0) result |= Modifiers.SHIFT;
        if ((nativeModifiers & InputConstants.MOD_CONTROL) != 0) result |= Modifiers.CTRL;
        if ((nativeModifiers & InputConstants.MOD_ALT) != 0) result |= Modifiers.ALT;
        if ((nativeModifiers & InputConstants.MOD_SUPER) != 0) result |= Modifiers.SUPER;
        if ((nativeModifiers & InputConstants.MOD_CAPS_LOCK) != 0) result |= Modifiers.CAPS_LOCK;
        if ((nativeModifiers & InputConstants.MOD_NUM_LOCK) != 0) result |= Modifiers.NUM_LOCK;
        return result;
        *///?} else {
        return nativeModifiers;
        //?}
    }
}
