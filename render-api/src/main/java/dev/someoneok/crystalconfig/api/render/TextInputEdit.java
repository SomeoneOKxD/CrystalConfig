package dev.someoneok.crystalconfig.api.render;

import java.util.OptionalDouble;

/**
 * Small immutable helper for editing a caller-owned input value.
 *
 * <p>This object does not store anything between events. Create it from the value/caret owned by
 * your screen, apply one edit, then copy the returned value/caret back into your own fields.</p>
 */
public record TextInputEdit(String value, int cursor, int selectionAnchor) {
    public TextInputEdit {
        value = value == null ? "" : value;
        cursor = clamp(cursor, 0, value.length());
        selectionAnchor = clamp(selectionAnchor, 0, value.length());
    }

    public static TextInputEdit of(String value, int cursor, int selectionAnchor) {
        return new TextInputEdit(value, cursor, selectionAnchor);
    }

    public static TextInputEdit atEnd(String value) {
        String safe = value == null ? "" : value;
        return new TextInputEdit(safe, safe.length(), safe.length());
    }

    public boolean hasSelection() {
        return cursor != selectionAnchor;
    }

    public int selectionStart() {
        return Math.min(cursor, selectionAnchor);
    }

    public int selectionEnd() {
        return Math.max(cursor, selectionAnchor);
    }

    public String selectedText() {
        return hasSelection() ? value.substring(selectionStart(), selectionEnd()) : "";
    }

    public TextInputEdit selectAll() {
        return new TextInputEdit(value, value.length(), 0);
    }

    public TextInputEdit clearSelection() {
        return new TextInputEdit(value, cursor, cursor);
    }

    public TextInputEdit moveLeft(boolean extendSelection) {
        if (!extendSelection && hasSelection()) {
            int target = selectionStart();
            return new TextInputEdit(value, target, target);
        }
        return moveTo(Math.max(0, cursor - 1), extendSelection);
    }

    public TextInputEdit moveRight(boolean extendSelection) {
        if (!extendSelection && hasSelection()) {
            int target = selectionEnd();
            return new TextInputEdit(value, target, target);
        }
        return moveTo(Math.min(value.length(), cursor + 1), extendSelection);
    }

    public TextInputEdit home(boolean extendSelection) {
        return moveTo(0, extendSelection);
    }

    public TextInputEdit end(boolean extendSelection) {
        return moveTo(value.length(), extendSelection);
    }

    public TextInputEdit moveTo(int position, boolean extendSelection) {
        int target = clamp(position, 0, value.length());
        return new TextInputEdit(value, target, extendSelection ? selectionAnchor : target);
    }

    /** Inserts text, replacing the current selection and enforcing the supplied maximum length. */
    public TextInputEdit insert(String text, int maxLength) {
        return replaceSelection(text, maxLength);
    }

    /**
     * Inserts only simple numeric characters (digits, optional '-' and optional '.').
     * Intermediate editing values such as {@code -}, {@code .}, and {@code -.} are allowed.
     */
    public TextInputEdit insertNumber(String text, boolean allowDecimal, boolean allowNegative, int maxLength) {
        String incoming = text == null ? "" : text;
        TextInputEdit edit = this;
        for (int i = 0; i < incoming.length(); i++) {
            char c = incoming.charAt(i);
            if ((c < '0' || c > '9') && c != '-' && c != '.') continue;

            TextInputEdit candidate = edit.replaceSelection(String.valueOf(c), maxLength);
            if (!candidate.equals(edit) && isPotentialNumber(candidate.value, allowDecimal, allowNegative)) {
                edit = candidate;
            }
        }
        return edit;
    }

    public TextInputEdit replaceSelection(String replacement, int maxLength) {
        String insert = replacement == null ? "" : replacement;
        int start = selectionStart();
        int end = selectionEnd();
        int safeMaxLength = Math.max(0, maxLength);
        int baseLength = value.length() - (end - start);
        int available = Math.max(0, safeMaxLength - baseLength);
        if (insert.length() > available) insert = insert.substring(0, available);

        String next = value.substring(0, start) + insert + value.substring(end);
        int nextCursor = start + insert.length();
        return new TextInputEdit(next, nextCursor, nextCursor);
    }

    public TextInputEdit backspace() {
        if (hasSelection()) return replaceSelection("", Integer.MAX_VALUE);
        if (cursor <= 0) return this;
        return new TextInputEdit(
                value.substring(0, cursor - 1) + value.substring(cursor),
                cursor - 1,
                cursor - 1
        );
    }

    public TextInputEdit delete() {
        if (hasSelection()) return replaceSelection("", Integer.MAX_VALUE);
        if (cursor >= value.length()) return this;
        return new TextInputEdit(
                value.substring(0, cursor) + value.substring(cursor + 1),
                cursor,
                cursor
        );
    }

    /** Removes and returns the selected range as the new edit state. Use {@link #selectedText()} before this for clipboard copy. */
    public TextInputEdit cutSelection() {
        return hasSelection() ? replaceSelection("", Integer.MAX_VALUE) : this;
    }

    public static boolean isPotentialNumber(String value, boolean allowDecimal, boolean allowNegative) {
        String text = value == null ? "" : value;
        if (text.isEmpty()) return true;

        int index = 0;
        if (text.charAt(0) == '-') {
            if (!allowNegative) return false;
            index = 1;
        }

        boolean decimalSeen = false;
        for (; index < text.length(); index++) {
            char c = text.charAt(index);
            if (c >= '0' && c <= '9') continue;
            if (c == '.' && allowDecimal && !decimalSeen) {
                decimalSeen = true;
                continue;
            }
            return false;
        }
        return true;
    }

    /** Returns true only for a complete finite number inside the supplied range. */
    public static boolean isValidNumber(String value, boolean allowDecimal, double min, double max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) return false;
        if (!isPotentialNumber(text, allowDecimal, min < 0.0)) return false;
        try {
            double parsed = Double.parseDouble(text);
            return Double.isFinite(parsed) && parsed >= min && parsed <= max;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** Parses the current value when it is a complete finite decimal number. */
    public OptionalDouble parsedNumber() {
        String text = value.trim();
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) return OptionalDouble.empty();
        try {
            double parsed = Double.parseDouble(text);
            return Double.isFinite(parsed) ? OptionalDouble.of(parsed) : OptionalDouble.empty();
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
