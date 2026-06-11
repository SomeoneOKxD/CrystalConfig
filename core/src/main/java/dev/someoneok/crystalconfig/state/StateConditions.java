package dev.someoneok.crystalconfig.state;

import java.util.function.BooleanSupplier;

/** Optional per-option visibility/lock predicates for auto-config state fields. */
public interface StateConditions {
    default BooleanSupplier hiddenWhen() { return () -> false; }
    default BooleanSupplier disabledWhen() { return () -> false; }
}
