package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigKeybind {
    String key() default "";
    String label();
    String description() default "";

    /**
     * When true, Escape/Backspace/Delete cancel key listening without changing the value to None.
     */
    boolean disallowNone() default false;
}
