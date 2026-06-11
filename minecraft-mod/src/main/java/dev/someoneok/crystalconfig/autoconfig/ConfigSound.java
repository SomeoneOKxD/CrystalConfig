package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigSound {
    String label();
    String description() default "";
    String key() default "";

    /** When false, None/Clear is disabled and a selected sound is required before committing. */
    boolean allowNone() default true;
}
