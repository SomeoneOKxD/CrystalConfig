package dev.someoneok.crystalconfig.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds a compact icon button to the config screen sidebar footer.
 *
 * <p>Use constants from {@code MediaBrandIcons} for {@link #icon()} when possible.
 * The icon row is placed above {@link ConfigFooterButton}; when no footer button is
 * present it occupies the footer area by itself. Multiple annotated fields are
 * rendered in declaration order and automatically wrap to centered rows.</p>
 *
 * <p>{@link ConfigFooterIconAction#OPEN_URL},
 * {@link ConfigFooterIconAction#COPY_TO_CLIPBOARD}, and
 * {@link ConfigFooterIconAction#NONE} are marker-only actions, so their fields
 * should be {@code static final ConfigMarker field = ConfigMarker.marker();}.
 * {@link ConfigFooterIconAction#RUNNABLE} is the only action that requires the
 * annotated field to be a static {@link Runnable}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ConfigFooterIcon {
    /** Glyph to draw inside the icon button, usually from MediaBrandIcons. */
    String icon();

    /** Built-in click action. Only RUNNABLE requires the field itself to be a Runnable. */
    ConfigFooterIconAction action() default ConfigFooterIconAction.OPEN_URL;

    /** Action input, for example a URL or clipboard text. */
    String value() default "";

    /** Optional hover tooltip. */
    String tooltip() default "";
}
