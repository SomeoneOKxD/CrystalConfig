package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the single footer button for an auto-config screen.
 *
 * <p>The annotated field must be a static {@link Runnable}. At most one
 * {@code @ConfigFooterButton} may be present in a discovered config model;
 * model creation fails immediately when more than one is found.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigFooterButton {
    String value();
}
