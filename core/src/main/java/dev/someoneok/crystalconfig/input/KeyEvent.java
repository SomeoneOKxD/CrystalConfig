package dev.someoneok.crystalconfig.input;

public final class KeyEvent extends UiEvent {
    public final int keyCode;
    public final int scanCode;
    public final int modifiers;
    public final String displayName;

    public KeyEvent(int keyCode, int scanCode, int modifiers, String displayName) {
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
        this.displayName = displayName == null ? String.valueOf(keyCode) : displayName;
    }
}
