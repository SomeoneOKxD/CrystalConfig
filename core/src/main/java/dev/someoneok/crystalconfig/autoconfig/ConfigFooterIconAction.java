package dev.someoneok.crystalconfig.autoconfig;

/** Built-in actions for {@link ConfigFooterIcon}. */
public enum ConfigFooterIconAction {
    /** Open the action value as a URL using the platform desktop/browser fallback. */
    OPEN_URL,

    /** Copy the action value to the system clipboard. */
    COPY_TO_CLIPBOARD,

    /** Invoke the annotated field. The field must be a static Runnable. */
    RUNNABLE,

    /** Render the icon button without doing anything when clicked. */
    NONE
}
