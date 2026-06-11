package dev.someoneok.crystalconfig.input;

public final class CharTypedEvent extends UiEvent {
    public final char codePoint;
    public final int modifiers;

    public CharTypedEvent(char codePoint, int modifiers) {
        this.codePoint = codePoint;
        this.modifiers = modifiers;
    }
}
