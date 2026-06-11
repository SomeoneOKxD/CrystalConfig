package dev.someoneok.crystalconfig.state;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Binding<T> implements State<T> {
    private final Supplier<T> getter;
    private final Consumer<T> setter;

    private Binding(Supplier<T> getter, Consumer<T> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public static <T> Binding<T> of(Supplier<T> getter, Consumer<T> setter) {
        return new Binding<>(getter, setter);
    }

    @Override
    public T get() { return getter.get(); }

    @Override
    public void set(T value) { setter.accept(value); }
}
