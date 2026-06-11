package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigDraggableList {
    String key() default "";
    String label();
    String description() default "";
    /** Set to false to keep at least one item in the list. */
    boolean allowEmpty() default true;
    /** Set to false to prevent removing active items. */
    boolean allowDeleting() default true;
}
