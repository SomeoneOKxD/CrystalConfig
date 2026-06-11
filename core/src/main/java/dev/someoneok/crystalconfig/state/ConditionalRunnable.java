package dev.someoneok.crystalconfig.state;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Runnable action that can carry autoconfig visibility/disabled predicates.
 *
 * <p>This is mostly useful for @ConfigButton fields, because annotation values cannot
 * contain lambdas. Use this wrapper when you want the action and its conditions to live
 * together in code instead of referencing method names from annotation strings.</p>
 *
 * <pre>{@code
 * @ConfigButton(label = "Reset Cache", buttonText = "Reset")
 * public static final ConditionalRunnable RESET_CACHE = ConditionalRunnable.of(MyConfig::resetCache)
 *         .hiddenWhen(() -> !advancedMode.get())
 *         .disabledWhen(() -> reloading.get());
 * }</pre>
 */
public final class ConditionalRunnable implements Runnable, StateConditions {
    private final Runnable delegate;
    private BooleanSupplier hiddenWhen = () -> false;
    private BooleanSupplier disabledWhen = () -> false;

    private ConditionalRunnable(Runnable delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static ConditionalRunnable of(Runnable delegate) {
        return new ConditionalRunnable(delegate);
    }

    public ConditionalRunnable hiddenWhen(BooleanSupplier predicate) {
        this.hiddenWhen = predicate == null ? () -> false : predicate;
        return this;
    }

    public ConditionalRunnable hiddenWhen(State<Boolean> state) {
        return hiddenWhen(() -> Boolean.TRUE.equals(state.get()));
    }

    public ConditionalRunnable disabledWhen(BooleanSupplier predicate) {
        this.disabledWhen = predicate == null ? () -> false : predicate;
        return this;
    }

    public ConditionalRunnable disabledWhen(State<Boolean> state) {
        return disabledWhen(() -> Boolean.TRUE.equals(state.get()));
    }

    @Override
    public BooleanSupplier hiddenWhen() {
        return hiddenWhen;
    }

    @Override
    public BooleanSupplier disabledWhen() {
        return disabledWhen;
    }

    @Override
    public void run() {
        delegate.run();
    }
}
