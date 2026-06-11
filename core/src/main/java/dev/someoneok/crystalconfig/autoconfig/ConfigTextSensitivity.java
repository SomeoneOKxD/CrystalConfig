package dev.someoneok.crystalconfig.autoconfig;

/**
 * Display behavior for sensitive text config values. This only affects rendering;
 * the stored value remains unchanged.
 */
public enum ConfigTextSensitivity {
    NONE,
    /** Dot the value while unfocused and reveal it while the input is focused. */
    VISIBLE_WHILE_EDITING,
    /** Dot the value at all times, even while focused. */
    ALWAYS_HIDDEN
}
