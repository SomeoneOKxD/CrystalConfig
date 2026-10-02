package dev.someoneok.crystalconfig.state;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * State wrapper that carries explicit UI conditions with the option itself.
 *
 * <p>This avoids string-based condition names and reflection scanning. The developer passes
 * real suppliers/states at declaration time, e.g.
 * {@code ConditionalState.of(new MutableState<>(true)).disabledWhen(() -> !enabled.get())}.</p>
 */
public final class ConditionalState<T> implements State<T>, StateConditions {
    private final State<T> delegate;
    private BooleanSupplier hiddenWhen = () -> false;
    private BooleanSupplier disabledWhen = () -> false;
    private String disabledTooltip = "";

    private ConditionalState(State<T> delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static <T> ConditionalState<T> of(State<T> delegate) {
        return new ConditionalState<>(delegate);
    }

    public static <T> ConditionalState<T> mutable(T initial) {
        return new ConditionalState<>(new MutableState<>(initial));
    }

    public ConditionalState<T> hiddenWhen(BooleanSupplier predicate) {
        this.hiddenWhen = predicate == null ? () -> false : predicate;
        return this;
    }

    public ConditionalState<T> hiddenWhen(State<Boolean> state) {
        return hiddenWhen(() -> Boolean.TRUE.equals(state.get()));
    }

    public ConditionalState<T> disabledWhen(BooleanSupplier predicate) {
        return disabledWhen(predicate, "");
    }

    public ConditionalState<T> disabledWhen(BooleanSupplier predicate, String tooltip) {
        this.disabledWhen = predicate == null ? () -> false : predicate;
        this.disabledTooltip = tooltip == null ? "" : tooltip;
        return this;
    }

    public ConditionalState<T> disabledWhen(State<Boolean> state) {
        return disabledWhen(state, "");
    }

    public ConditionalState<T> disabledWhen(State<Boolean> state, String tooltip) {
        return disabledWhen(() -> Boolean.TRUE.equals(state.get()), tooltip);
    }

    @Override
    public BooleanSupplier hiddenWhen() { return hiddenWhen; }

    @Override
    public BooleanSupplier disabledWhen() { return disabledWhen; }

    @Override
    public String disabledTooltip() { return disabledTooltip; }

    @Override
    public T get() { return delegate.get(); }

    @Override
    public void set(T value) { delegate.set(value); }

    @Override
    public AutoCloseable subscribe(Consumer<T> listener) { return delegate.subscribe(listener); }
}
