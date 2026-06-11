package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a fully custom, full-width config row. The annotated static field may be
 * a Component or a Supplier<Component>. The component owns its own layout,
 * rendering, input handling, and any state/persistence it needs.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigCustomOption {
    /** Text used only for config search/filter matching. Formatting codes are ignored. */
    String searchText() default "";
    String tooltip() default "";
}
