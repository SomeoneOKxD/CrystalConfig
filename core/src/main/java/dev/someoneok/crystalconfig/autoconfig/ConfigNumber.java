package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigNumber {
    String key() default "";
    String label();
    String description() default "";
    double min() default -1.7976931348623157E308;
    double max() default 1.7976931348623157E308;
    double step() default 1.0;
}
