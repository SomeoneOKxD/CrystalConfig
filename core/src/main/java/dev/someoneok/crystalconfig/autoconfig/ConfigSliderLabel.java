package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigSliderLabel {
    String key() default "";
    String label();
    String description() default "";
    double min();
    double max();
    double step() default 1.0;
}
