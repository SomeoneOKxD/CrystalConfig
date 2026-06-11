package dev.someoneok.crystalconfig.config;

import dev.someoneok.crystalconfig.persistence.GsonConfigStore;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.theme.PresetTheme;
import dev.someoneok.crystalconfig.theme.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Per-screen/per-model settings for the config UI.
 *
 * <p>Do not store mod defaults in static fields. Create one settings instance for your
 * config model and pass/use that instance everywhere. AutoConfig.Model already owns one
 * settings instance, so the common API is:</p>
 *
 * <pre>{@code
 * AutoConfig.Model model = AutoConfig.of(MyConfig.class)
 *         .configureSettings(settings -> settings
 *                 .defaultTheme(MyThemes.KUUDRA)
 *                 .defaultScale(1.0d));
 *
 * UiRoot root = model.root("My Mod", search);
 * }</pre>
 */
public final class ConfigUiSettings {
    public static final double[] SCALE_STEPS = {0.50d, 0.625d, 0.75d, 0.875d, 1.0d, 1.125d, 1.25d, 1.375d, 1.50d};

    private static final List<Theme> BUILTIN_THEMES = new ArrayList<>();

    static {
        for (PresetTheme preset : PresetTheme.values()) {
            Theme theme = preset.theme();
            if (theme != null) BUILTIN_THEMES.add(theme);
        }
    }

    public static final String DEFAULT_PERSISTENCE_KEY = "__configUiSettings";

    private final List<Theme> themes = new ArrayList<>();
    private Theme defaultTheme;
    private Theme selectedTheme;
    private double defaultScale = 1.0d;
    private double scale = 1.0d;
    private boolean defaultTextShadow;
    private boolean textShadow;
    private long version;
    private Runnable linkedDefaultsResetAction;
    private final List<Runnable> defaultsResetCallbacks = new ArrayList<>();
    private final List<Consumer<PersistedSettings>> persistenceListeners = new ArrayList<>();

    private ConfigUiSettings() {
        for (Theme theme : BUILTIN_THEMES) registerTheme(theme);
        if (!themes.isEmpty()) defaultTheme = themes.get(0);
    }

    /** Creates an isolated settings object. Changes here do not affect other mods/screens. */
    public static ConfigUiSettings create() {
        return new ConfigUiSettings();
    }

    /** Returns a copy of the built-in themes that new settings instances start with. */
    public static synchronized List<Theme> builtinThemes() {
        return List.copyOf(BUILTIN_THEMES);
    }

    public synchronized ConfigUiSettings registerTheme(Theme theme) {
        if (theme == null) return this;

        for (int i = 0; i < themes.size(); i++) {
            Theme existing = themes.get(i);
            if (Objects.equals(existing.name(), theme.name())) {
                themes.set(i, theme);

                if (defaultTheme != null && Objects.equals(defaultTheme.name(), theme.name())) {
                    defaultTheme = theme;
                }

                if (selectedTheme != null && Objects.equals(selectedTheme.name(), theme.name())) {
                    selectedTheme = theme;
                }

                changed();
                return this;
            }
        }

        themes.add(theme);
        if (defaultTheme == null) defaultTheme = theme;
        changed();
        return this;
    }

    public synchronized ConfigUiSettings registerThemes(Iterable<Theme> themes) {
        if (themes == null) return this;
        for (Theme theme : themes) registerTheme(theme);
        return this;
    }

    public synchronized List<Theme> themes() {
        return Collections.unmodifiableList(new ArrayList<>(themes));
    }

    public synchronized Theme selectedTheme() {
        return selectedTheme;
    }

    public synchronized Theme defaultTheme() {
        if (defaultTheme != null) return defaultTheme;
        if (themes.isEmpty()) return null;
        return themes.get(0);
    }

    /** Fluent alias for {@link #setDefaultTheme(Theme)}. */
    public synchronized ConfigUiSettings defaultTheme(Theme theme) {
        return setDefaultTheme(theme);
    }

    public synchronized ConfigUiSettings setDefaultTheme(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("Default theme cannot be null");
        }

        registerTheme(theme);
        Theme resolved = themeByName(theme.name());
        if (!Objects.equals(defaultTheme, resolved)) {
            defaultTheme = resolved;
            changed();
        }
        return this;
    }

    public synchronized Theme activeTheme() {
        return selectedTheme == null ? defaultTheme() : selectedTheme;
    }

    public synchronized ConfigUiSettings selectedTheme(Theme theme) {
        if (theme != null) registerTheme(theme);
        if (!Objects.equals(selectedTheme, theme)) {
            selectedTheme = theme == null ? null : themeByName(theme.name());
            changed();
        }
        return this;
    }

    public synchronized Theme resolveTheme(Theme fallback) {
        if (selectedTheme != null) return selectedTheme;
        if (defaultTheme() != null) return defaultTheme();
        return fallback;
    }

    public synchronized double defaultScale() {
        return defaultScale;
    }

    /** Fluent alias for {@link #setDefaultScale(double)}. */
    public synchronized ConfigUiSettings defaultScale(double requested) {
        return setDefaultScale(requested);
    }

    public synchronized ConfigUiSettings setDefaultScale(double requested) {
        double nearest = nearestScale(requested);
        if (Double.compare(defaultScale, nearest) != 0) {
            defaultScale = nearest;
            changed();
        }
        if (Double.compare(scale, nearest) != 0) {
            scale = nearest;
            changed();
        }
        return this;
    }

    public synchronized double scale() {
        return scale;
    }

    public synchronized boolean defaultTextShadow() {
        return defaultTextShadow;
    }

    /** Fluent alias for {@link #setDefaultTextShadow(boolean)}. */
    public synchronized ConfigUiSettings defaultTextShadow(boolean enabled) {
        return setDefaultTextShadow(enabled);
    }

    public synchronized ConfigUiSettings setDefaultTextShadow(boolean enabled) {
        if (defaultTextShadow != enabled) {
            defaultTextShadow = enabled;
            changed();
        }
        if (textShadow != enabled) {
            textShadow = enabled;
            changed();
        }
        return this;
    }

    public synchronized boolean textShadow() {
        return textShadow;
    }

    public synchronized ConfigUiSettings textShadow(boolean enabled) {
        if (textShadow != enabled) {
            textShadow = enabled;
            changed();
        }
        return this;
    }

    public synchronized ConfigUiSettings scale(double requested) {
        double nearest = nearestScale(requested);
        if (Double.compare(scale, nearest) != 0) {
            scale = nearest;
            changed();
        }
        return this;
    }

    /** Clears user-selected screen settings and returns to this instance's developer defaults. */
    public synchronized ConfigUiSettings reset() {
        selectedTheme = null;
        scale = defaultScale;
        textShadow = defaultTextShadow;
        changed();
        return this;
    }

    /**
     * Sets the reset action used by the settings popup. AutoConfig wires this to the
     * registered config store so the popup can reset the owning mod back to the
     * values captured before config-file loading.
     */
    public synchronized ConfigUiSettings linkedDefaultsResetAction(Runnable action) {
        this.linkedDefaultsResetAction = action;
        return this;
    }

    /** Register a callback that runs after the linked config has been reset to defaults. */
    public synchronized ConfigUiSettings onDefaultsReset(Runnable callback) {
        if (callback != null) defaultsResetCallbacks.add(callback);
        return this;
    }

    /** Alias for {@link #onDefaultsReset(Runnable)}. */
    public ConfigUiSettings afterDefaultsReset(Runnable callback) {
        return onDefaultsReset(callback);
    }

    /** Resets the linked mod config when available, otherwise resets only UI settings. */
    public void resetLinkedDefaults() {
        Runnable action;
        List<Runnable> callbacks;
        synchronized (this) {
            action = linkedDefaultsResetAction;
            callbacks = new ArrayList<>(defaultsResetCallbacks);
        }
        if (action != null) action.run();
        else reset();
        for (Runnable callback : callbacks) callback.run();
    }

    public ConfigUiSettings register(GsonConfigStore store) {
        return register(store, DEFAULT_PERSISTENCE_KEY);
    }

    public ConfigUiSettings register(GsonConfigStore store, String key) {
        Objects.requireNonNull(store, "store");
        store.register(key == null || key.isBlank() ? DEFAULT_PERSISTENCE_KEY : key, persistentState(), (java.lang.reflect.Type) PersistedSettings.class);
        return this;
    }

    private synchronized State<PersistedSettings> persistentState() {
        return new State<>() {
            @Override public PersistedSettings get() { return snapshot(); }
            @Override public void set(PersistedSettings value) { apply(value); }
            @Override public AutoCloseable subscribe(Consumer<PersistedSettings> listener) {
                if (listener == null) return () -> { };
                synchronized (ConfigUiSettings.this) { persistenceListeners.add(listener); }
                return () -> { synchronized (ConfigUiSettings.this) { persistenceListeners.remove(listener); } };
            }
        };
    }

    private synchronized PersistedSettings snapshot() {
        PersistedSettings out = new PersistedSettings();
        out.selectedTheme = selectedTheme == null ? null : selectedTheme.name();
        out.scale = scale;
        out.textShadow = textShadow;
        return out;
    }

    private synchronized void apply(PersistedSettings in) {
        if (in == null) return;
        selectedTheme = in.selectedTheme == null || in.selectedTheme.isBlank() ? null : themeByName(in.selectedTheme);
        scale = nearestScale(in.scale);
        textShadow = in.textShadow;
        changed();
    }

    private void changed() {
        version++;
        if (persistenceListeners.isEmpty()) return;
        PersistedSettings snapshot = snapshot();
        List<Consumer<PersistedSettings>> listeners = new ArrayList<>(persistenceListeners);
        for (Consumer<PersistedSettings> listener : listeners) listener.accept(snapshot);
    }

    public static final class PersistedSettings {
        public String selectedTheme;
        public double scale = 1.0d;
        public boolean textShadow;
    }

    public synchronized long version() {
        return version;
    }

    private Theme themeByName(String name) {
        for (Theme theme : themes) {
            if (Objects.equals(theme.name(), name)) return theme;
        }
        return null;
    }

    public static double nearestScale(double requested) {
        if (!Double.isFinite(requested)) return 1.0d;

        double best = SCALE_STEPS[0];
        double bestDelta = Math.abs(requested - best);

        for (double step : SCALE_STEPS) {
            double delta = Math.abs(requested - step);
            if (delta < bestDelta) {
                best = step;
                bestDelta = delta;
            }
        }

        return best;
    }

    public static int scaleIndex(double requested) {
        double nearest = nearestScale(requested);

        for (int i = 0; i < SCALE_STEPS.length; i++) {
            if (Double.compare(SCALE_STEPS[i], nearest) == 0) return i;
        }

        return 1;
    }
}
