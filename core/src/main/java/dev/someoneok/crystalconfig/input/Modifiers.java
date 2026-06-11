package dev.someoneok.crystalconfig.input;

/** GLFW-compatible modifier bit flags. */
public final class Modifiers {
    public static final int SHIFT = 0x0001;
    public static final int CTRL = 0x0002;
    public static final int ALT = 0x0004;
    public static final int SUPER = 0x0008;
    public static final int CAPS_LOCK = 0x0010;
    public static final int NUM_LOCK = 0x0020;

    private Modifiers() { }

    public static boolean has(int modifiers, int flag) {
        return (modifiers & flag) != 0;
    }
}
