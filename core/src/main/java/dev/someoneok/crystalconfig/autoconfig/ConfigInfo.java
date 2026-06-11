package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigInfo {
    String title() default "";
    String description() default "";
    String tooltip() default "";
    String hiddenWhen() default "";
    String disabledWhen() default "";
}
