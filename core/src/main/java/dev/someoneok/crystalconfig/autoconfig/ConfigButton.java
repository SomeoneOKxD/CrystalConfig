package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigButton {
    String label();
    String buttonText();
    String description() default "";
    String tooltip() default "";

    /**
     * Optional name of a static BooleanSupplier, boolean field, State<Boolean>, or no-arg boolean method
     * on the owning class. The button row is hidden while it returns true.
     *
     * <p>For lambda-style autoconfig, prefer making the annotated field a
     * dev.someoneok.crystalconfig.state.ConditionalRunnable and call .hiddenWhen(() -> ...)</p>
     */
    String hiddenWhen() default "";

    /**
     * Optional name of a static BooleanSupplier, boolean field, State<Boolean>, or no-arg boolean method
     * on the owning class. The button row is disabled while it returns true.
     *
     * <p>For lambda-style autoconfig, prefer making the annotated field a
     * dev.someoneok.crystalconfig.state.ConditionalRunnable and call .disabledWhen(() -> ...)</p>
     */
    String disabledWhen() default "";
}
