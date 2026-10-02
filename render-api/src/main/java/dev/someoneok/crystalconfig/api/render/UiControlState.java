package dev.someoneok.crystalconfig.api.render;

public enum UiControlState {
    NORMAL(false, false, false, true),
    HOVERED(true, false, false, true),
    PRESSED(true, true, false, true),
    FOCUSED(false, false, true, true),
    FOCUSED_HOVERED(true, false, true, true),
    FOCUSED_PRESSED(true, true, true, true),
    DISABLED(false, false, false, false);

    private final boolean hovered;
    private final boolean pressed;
    private final boolean focused;
    private final boolean enabled;

    UiControlState(boolean hovered, boolean pressed, boolean focused, boolean enabled) {
        this.hovered = hovered;
        this.pressed = pressed;
        this.focused = focused;
        this.enabled = enabled;
    }

    public boolean hovered() { return hovered; }
    public boolean pressed() { return pressed; }
    public boolean focused() { return focused; }
    public boolean enabled() { return enabled; }

    public static UiControlState of(boolean hovered, boolean pressed, boolean focused, boolean enabled) {
        if (!enabled) return DISABLED;
        if (focused) {
            if (pressed) return FOCUSED_PRESSED;
            if (hovered) return FOCUSED_HOVERED;
            return FOCUSED;
        }
        if (pressed) return PRESSED;
        if (hovered) return HOVERED;
        return NORMAL;
    }
}
