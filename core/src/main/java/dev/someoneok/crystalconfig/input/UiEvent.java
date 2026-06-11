package dev.someoneok.crystalconfig.input;

public abstract class UiEvent {
    private boolean consumed;

    public boolean consumed() { return consumed; }

    public void consume() { consumed = true; }
}
