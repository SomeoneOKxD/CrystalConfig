package dev.someoneok.crystalconfig.input;

public enum MouseButton {
    LEFT,
    RIGHT,
    MIDDLE,
    OTHER;

    public static MouseButton fromIndex(int button) {
        return switch (button) {
            case 0 -> LEFT;
            case 1 -> RIGHT;
            case 2 -> MIDDLE;
            default -> OTHER;
        };
    }
}
