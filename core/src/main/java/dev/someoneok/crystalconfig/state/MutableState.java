package dev.someoneok.crystalconfig.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class MutableState<T> implements State<T> {
    private T value;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private final List<Consumer<T>> pendingRemovals = new ArrayList<>();
    private boolean notifying;

    public MutableState(T value) {
        this.value = value;
    }

    @Override
    public T get() { return value; }

    @Override
    public void set(T value) {
        if (Objects.equals(this.value, value)) return;
        this.value = value;
        if (listeners.isEmpty()) return;
        notifying = true;
        try {
            for (int i = 0, size = listeners.size(); i < size; i++) {
                listeners.get(i).accept(value);
            }
        } finally {
            notifying = false;
            if (!pendingRemovals.isEmpty()) {
                listeners.removeAll(pendingRemovals);
                pendingRemovals.clear();
            }
        }
    }

    @Override
    public AutoCloseable subscribe(Consumer<T> listener) {
        listeners.add(listener);
        return () -> {
            if (notifying) pendingRemovals.add(listener);
            else listeners.remove(listener);
        };
    }
}
