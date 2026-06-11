package dev.someoneok.crystalconfig.state;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigValue<T> implements State<T> {
    private final String path;
    private final String label;
    private final String description;
    private final State<T> delegate;

    public ConfigValue(String path, String label, String description, State<T> delegate) {
        this.path = path;
        this.label = label;
        this.description = description;
        this.delegate = delegate;
    }

    public static <T> ConfigValue<T> mutable(String path, String label, String description, T initial) {
        return new ConfigValue<>(path, label, description, new MutableState<>(initial));
    }

    public static <T> ConfigValue<T> binding(String path, String label, String description, Supplier<T> getter, Consumer<T> setter) {
        return new ConfigValue<>(path, label, description, Binding.of(getter, setter));
    }

    public String path() { return path; }
    public String label() { return label; }
    public String description() { return description; }

    @Override
    public T get() { return delegate.get(); }

    @Override
    public void set(T value) { delegate.set(value); }

    @Override
    public AutoCloseable subscribe(Consumer<T> listener) { return delegate.subscribe(listener); }
}
