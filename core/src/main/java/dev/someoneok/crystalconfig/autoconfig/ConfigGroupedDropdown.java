package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigGroupedDropdown {
    String key() default "";
    String label();
    String description() default "";

    /**
     * Name of a static field or no-arg static method on the same config class that returns
     * Map<G, ? extends Collection<T>>, where G extends Enum<G> and T matches the State<T> value type.
     */
    String options();
}
