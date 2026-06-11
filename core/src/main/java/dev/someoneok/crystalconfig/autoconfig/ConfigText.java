package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigText {
    String key() default "";
    String label();
    String description() default "";

    /**
     * Optional full-value regex used to filter user insertion and paste.
     * Use partial-friendly patterns such as "[A-Za-z0-9_]*" for character filters.
     */
    String regex() default "";

    /**
     * Sensitive display behavior. The value is always stored as plain text.
     */
    ConfigTextSensitivity sensitivity() default ConfigTextSensitivity.NONE;
}

