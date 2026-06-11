package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface ConfigCategory {
    /** Main category shown as a collapsible group in the left navigation. */
    String main() default "General";

    /** Sub-category shown as a nested entry inside the main category group. */
    String sub() default "General";

    /**
     * Optional static boolean / State<Boolean> / BooleanSupplier field or no-arg method name.
     * When it returns true, this sub-category is removed from navigation and content.
     */
    String hiddenWhen() default "";

    /**
     * Optional static boolean / State<Boolean> / BooleanSupplier field or no-arg method name.
     * When it returns true, this sub-category remains visible but its rows and navigation entry are disabled.
     */
    String disabledWhen() default "";
}
