package dev.someoneok.crystalconfig.state;

import java.util.function.Consumer;

public interface State<T> {
    T get();
    void set(T value);

    default AutoCloseable subscribe(Consumer<T> listener) {
        return () -> { };
    }
}
