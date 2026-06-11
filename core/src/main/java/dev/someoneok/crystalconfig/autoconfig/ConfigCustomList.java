package dev.someoneok.crystalconfig.autoconfig;

import dev.someoneok.crystalconfig.components.CustomListOption;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigCustomList {
    String key() default "";
    String label();
    String description() default "";

    /** Creates the new object inserted when the user presses the add button. */
    Class<? extends CustomListOption.EntryFactory<?>> entryFactory();

    /** Creates the component rendered inside each row. */
    Class<? extends CustomListOption.RowFactory<?>> rowFactory();

    String addButtonText() default "+ Add entry";
    boolean allowEmpty() default true;
    float rowGap() default 6.0f;
    float addButtonHeight() default 30.0f;
}
