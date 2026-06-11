package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.input.*;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;
import dev.someoneok.crystalconfig.utils.UiClipboard;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TextInput extends Component {
    /**
     * Controls how sensitive text values are displayed. Values are always stored
     * unchanged; this only affects rendering.
     */
    public enum SensitiveMode {
        NONE,
        /** Show dots while unfocused, reveal the real value while focused/editing. */
        VISIBLE_WHILE_EDITING,
        /** Always show dots, even while focused/editing. */
        ALWAYS_HIDDEN
    }

    protected final State<String> text;
    protected int cursor;
    protected int selectionAnchor;
    protected Predicate<String> validator = s -> true;
    protected Consumer<String> commitHandler = s -> { };
    protected String placeholder = "";
    protected int maxLength = 512;
    protected Pattern inputFilterPattern;
    protected String inputFilterRegex = "";
    protected SensitiveMode sensitiveMode = SensitiveMode.NONE;
    protected String maskCharacter = "•";
    protected float blink;
    private float[] cursorStops = new float[] {0};
    private boolean selecting;
    private float scrollOffset;
    public TextInput(State<String> text) {
        this.text = text;
        this.cursor = text.get() == null ? 0 : text.get().length();
        this.selectionAnchor = this.cursor;
        this.focusable = true;
        size(160, 28);
    }

    public TextInput placeholder(String placeholder) { this.placeholder = placeholder == null ? "" : placeholder; return this; }
    public TextInput validator(Predicate<String> validator) { this.validator = validator == null ? s -> true : validator; return this; }
    public TextInput onCommit(Consumer<String> handler) { this.commitHandler = handler == null ? s -> { } : handler; return this; }
    public TextInput maxLength(int maxLength) { this.maxLength = Math.max(0, maxLength); return this; }

    public TextInput filterRegex(String regex) {
        String safe = regex == null ? "" : regex.trim();
        this.inputFilterRegex = safe;
        if (safe.isEmpty()) {
            this.inputFilterPattern = null;
            return this;
        }
        try {
            this.inputFilterPattern = Pattern.compile(safe);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Invalid text input filter regex: " + safe, e);
        }
        return this;
    }

    public TextInput regex(String regex) { return filterRegex(regex); }

    public TextInput sensitive() { return sensitiveMode(SensitiveMode.VISIBLE_WHILE_EDITING); }

    public TextInput sensitive(boolean sensitive) {
        return sensitiveMode(sensitive ? SensitiveMode.VISIBLE_WHILE_EDITING : SensitiveMode.NONE);
    }

    public TextInput alwaysHidden() { return sensitiveMode(SensitiveMode.ALWAYS_HIDDEN); }

    public TextInput password() { return alwaysHidden(); }

    public TextInput sensitiveMode(SensitiveMode mode) {
        this.sensitiveMode = mode == null ? SensitiveMode.NONE : mode;
        return this;
    }

    public TextInput maskCharacter(String maskCharacter) {
        if (maskCharacter != null && !maskCharacter.isEmpty()) this.maskCharacter = maskCharacter;
        return this;
    }

    public String value() { return safe(text.get()); }

    @Override
    public void tick(float deltaSeconds) {
        blink += deltaSeconds;
        super.tick(deltaSeconds);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 160, preferredHeight >= 0 ? preferredHeight : 28));
    }

    @Override
    protected void renderSelf(RenderContext context) {
        String value = safe(text.get());
        normalizeSelection(value);
        boolean disabled = !enabled();
        boolean valid = validator.test(value) && matchesInputFilter(value);
        ColorRGBA border = disabled
                ? context.theme().palette().border().withAlpha(95)
                : !valid ? context.theme().palette().danger() : focused ? context.theme().palette().accent() : context.theme().palette().border();
        ColorRGBA fill = disabled
                ? context.theme().palette().surfaceAlt().withAlpha(120)
                : hovered || focused ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt();
        context.rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1, border)
                .radius(context.theme().radii().md()), z);
        Rect clip = bounds.inset(8, 2, 8, 2);
        context.pushClip(clip);
        ColorRGBA color = disabled
                ? context.theme().palette().mutedText().withAlpha(135)
                : value.isEmpty() && !focused ? context.theme().palette().mutedText() : context.theme().palette().text();
        float font = context.theme().fonts().normal();
        float ty = bounds.centerY() - context.lineHeight(font) * 0.5f;
        String focusedDisplayValue = displayValue(value, true);
        updateCursorStops(context, focusedDisplayValue, font);
        float textX = bounds.x() + 8;

        if (focused) {
            ensureCursorVisible(clip.w());
            textX -= scrollOffset;
            if (hasSelection()) {
                int a = selectionStart();
                int b = selectionEnd();
                float sx = bounds.x() + 8 + cursorStops[a] - scrollOffset;
                float ex = bounds.x() + 8 + cursorStops[b] - scrollOffset;
                context.rect(new Rect(sx, bounds.y() + 5, Math.max(1, ex - sx), bounds.h() - 10),
                        context.theme().palette().accent().withAlpha(70), context.theme().radii().sm(), z + 0.5f);
            }
            context.plainText(focusedDisplayValue, textX, ty, font, color, z + 1);
            if (((int)(blink * 2) % 2 == 0)) {
                float cx = bounds.x() + 8 + cursorStops[Math.max(0, Math.min(cursor, focusedDisplayValue.length()))] - scrollOffset;
                if (value.isEmpty()) cx += 1.0f;
                float caretWidth = Math.max(1.0f, 1.0f / context.scale());
                context.rect(
                        new Rect(cx, bounds.y() + 6, caretWidth, bounds.h() - 12),
                        context.theme().palette().accent(),
                        0,
                        z + 2
                );
            }
        } else {
            scrollOffset = 0;
            String display = displayValue(value, false);
            String shown = value.isEmpty() ? placeholder : ellipsizeEnd(context, display, font, clip.w());
            context.plainText(shown, textX, ty, font, color, z + 1);
        }
        context.popClip();
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (!enabled() || event.button != MouseButton.LEFT) return false;
        int target = estimateCursor(event.x);
        if (Modifiers.has(event.modifiers, Modifiers.SHIFT)) {
            cursor = target;
        } else {
            cursor = target;
            selectionAnchor = cursor;
        }
        selecting = true;
        blink = 0;
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseDragEvent event) {
        if (!enabled() || !selecting) return false;
        cursor = estimateCursor(event.x);
        blink = 0;
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        if (selecting) {
            selecting = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled()) return false;
        String value = safe(text.get());
        normalizeSelection(value);
        boolean ctrl = Modifiers.has(event.modifiers, Modifiers.CTRL) || Modifiers.has(event.modifiers, Modifiers.SUPER);
        boolean shift = Modifiers.has(event.modifiers, Modifiers.SHIFT);

        if (ctrl) {
            int key = Character.toUpperCase(event.keyCode);
            if (key == 'A') { selectAll(value); blink = 0; return true; }
            if (key == 'C') { copySelection(value); blink = 0; return true; }
            if (key == 'X') { cutSelection(value); blink = 0; return true; }
            if (key == 'V') { pasteClipboard(); blink = 0; return true; }
            if (event.keyCode == KeyCodes.LEFT) { moveCursor(wordLeft(value), shift); blink = 0; return true; }
            if (event.keyCode == KeyCodes.RIGHT) { moveCursor(wordRight(value), shift); blink = 0; return true; }
            if (event.keyCode == KeyCodes.BACKSPACE) { deleteWordLeft(value); blink = 0; return true; }
            if (event.keyCode == KeyCodes.DELETE) { deleteWordRight(value); blink = 0; return true; }
        }

        switch (event.keyCode) {
            case KeyCodes.LEFT -> moveCursor(Math.max(0, cursor - 1), shift);
            case KeyCodes.RIGHT -> moveCursor(Math.min(value.length(), cursor + 1), shift);
            case KeyCodes.HOME -> moveCursor(0, shift);
            case KeyCodes.END -> moveCursor(value.length(), shift);
            case KeyCodes.BACKSPACE -> {
                if (hasSelection()) replaceSelection(value, "");
                else if (cursor > 0) {
                    text.set(value.substring(0, cursor - 1) + value.substring(cursor));
                    cursor--;
                    selectionAnchor = cursor;
                }
            }
            case KeyCodes.DELETE -> {
                if (hasSelection()) replaceSelection(value, "");
                else if (cursor < value.length()) text.set(value.substring(0, cursor) + value.substring(cursor + 1));
            }
            case KeyCodes.ENTER -> commit();
            case KeyCodes.ESCAPE -> { return false; }
            default -> { return false; }
        }
        blink = 0;
        return true;
    }

    @Override
    public boolean onCharTyped(CharTypedEvent event) {
        if (!enabled() || Character.isISOControl(event.codePoint)) return false;
        boolean changed = insertText(String.valueOf(event.codePoint));
        blink = 0;
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        if (!focused) {
            commit();
            selecting = false;
            selectionAnchor = cursor;
        }
        blink = 0;
    }

    protected void commit() {
        String value = safe(text.get());
        if (validator.test(value) && matchesInputFilter(value)) commitHandler.accept(value);
    }

    protected void setText(String value) {
        text.set(value == null ? "" : value);
        cursor = Math.min(cursor, safe(text.get()).length());
        selectionAnchor = Math.min(selectionAnchor, safe(text.get()).length());
    }

    private boolean insertText(String rawInsert) {
        String value = safe(text.get());
        normalizeSelection(value);
        String insert = filteredInsertion(value, selectionStart(), selectionEnd(), rawInsert);
        if (insert == null) return false;
        replaceSelection(value, insert);
        return true;
    }

    private String filteredInsertion(String value, int start, int end, String rawInsert) {
        String insert = rawInsert == null ? "" : rawInsert;
        if (insert.isEmpty()) return "";
        int allowed = Math.max(0, maxLength - (value.length() - (end - start)));
        if (allowed <= 0) return null;
        if (insert.length() > allowed) insert = insert.substring(0, allowed);

        if (inputFilterPattern == null) return insert;

        String before = value.substring(0, start);
        String after = value.substring(end);
        String candidate = before + insert + after;
        if (canBecomeInputFilterMatch(candidate)) return insert;

        StringBuilder accepted = new StringBuilder(insert.length());
        String working = before + after;
        int cursorPos = start;
        for (int i = 0; i < insert.length();) {
            int codePoint = insert.codePointAt(i);
            String part = new String(Character.toChars(codePoint));
            String next = working.substring(0, cursorPos) + part + working.substring(cursorPos);
            if (canBecomeInputFilterMatch(next)) {
                accepted.append(part);
                working = next;
                cursorPos += part.length();
            }
            i += Character.charCount(codePoint);
        }
        return accepted.length() == 0 ? null : accepted.toString();
    }

    private boolean matchesInputFilter(String value) {
        return inputFilterPattern == null || inputFilterPattern.matcher(value).matches();
    }

    private boolean canBecomeInputFilterMatch(String value) {
        if (inputFilterPattern == null) return true;
        Matcher matcher = inputFilterPattern.matcher(value);
        return matcher.matches() || matcher.hitEnd();
    }

    private void moveCursor(int target, boolean extending) {
        cursor = Math.max(0, Math.min(target, safe(text.get()).length()));
        if (!extending) selectionAnchor = cursor;
    }

    private boolean hasSelection() { return selectionAnchor != cursor; }
    private int selectionStart() { return Math.min(selectionAnchor, cursor); }
    private int selectionEnd() { return Math.max(selectionAnchor, cursor); }

    private void normalizeSelection(String value) {
        int len = value.length();
        cursor = Math.max(0, Math.min(cursor, len));
        selectionAnchor = Math.max(0, Math.min(selectionAnchor, len));
    }

    private void selectAll(String value) {
        selectionAnchor = 0;
        cursor = value.length();
    }

    private void replaceSelection(String value, String insert) {
        int a = selectionStart();
        int b = selectionEnd();
        String safeInsert = insert == null ? "" : insert;
        int allowed = Math.max(0, maxLength - (value.length() - (b - a)));
        if (safeInsert.length() > allowed) safeInsert = safeInsert.substring(0, allowed);
        text.set(value.substring(0, a) + safeInsert + value.substring(b));
        cursor = a + safeInsert.length();
        selectionAnchor = cursor;
    }

    private void copySelection(String value) {
        if (!hasSelection()) return;
        setClipboard(value.substring(selectionStart(), selectionEnd()));
    }

    private void cutSelection(String value) {
        if (!hasSelection()) return;
        copySelection(value);
        replaceSelection(value, "");
    }

    private void pasteClipboard() {
        String paste = getClipboard();
        if (paste == null || paste.isEmpty()) return;
        paste = paste.replace("\r", "").replace("\n", "");
        insertText(paste);
    }

    private void deleteWordLeft(String value) {
        if (hasSelection()) { replaceSelection(value, ""); return; }
        int target = wordLeft(value);
        if (target < cursor) {
            text.set(value.substring(0, target) + value.substring(cursor));
            cursor = target;
            selectionAnchor = cursor;
        }
    }

    private void deleteWordRight(String value) {
        if (hasSelection()) { replaceSelection(value, ""); return; }
        int target = wordRight(value);
        if (target > cursor) text.set(value.substring(0, cursor) + value.substring(target));
    }

    private int wordLeft(String value) {
        int i = Math.max(0, cursor - 1);
        while (i > 0 && Character.isWhitespace(value.charAt(i))) i--;
        while (i > 0 && !Character.isWhitespace(value.charAt(i - 1))) i--;
        return i;
    }

    private int wordRight(String value) {
        int i = Math.min(value.length(), cursor);
        while (i < value.length() && Character.isWhitespace(value.charAt(i))) i++;
        while (i < value.length() && !Character.isWhitespace(value.charAt(i))) i++;
        return i;
    }

    private int estimateCursor(float mouseX) {
        String value = safe(text.get());
        if (value.isEmpty()) return 0;
        float localX = Math.max(0, mouseX - bounds.x() - 8 + scrollOffset);
        if (cursorStops != null && cursorStops.length == value.length() + 1) {
            int best = 0;
            float bestDistance = Float.MAX_VALUE;
            for (int i = 0; i < cursorStops.length; i++) {
                float distance = Math.abs(cursorStops[i] - localX);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = i;
                }
            }
            return best;
        }
        float t = Math.max(0, Math.min(1, localX / Math.max(1, bounds.w() - 16)));
        return Math.max(0, Math.min(value.length(), Math.round(t * value.length())));
    }

    private void updateCursorStops(RenderContext context, String value, float font) {
        cursorStops = new float[value.length() + 1];
        cursorStops[0] = 0;
        for (int i = 1; i <= value.length(); i++) {
            cursorStops[i] = context.measurePlainText(value.substring(0, i), font).width();
        }
    }

    private String displayValue(String value, boolean editing) {
        String safeValue = safe(value);
        if (safeValue.isEmpty()) return "";
        if (sensitiveMode == SensitiveMode.ALWAYS_HIDDEN) return masked(safeValue);
        if (sensitiveMode == SensitiveMode.VISIBLE_WHILE_EDITING && !editing) return masked(safeValue);
        return safeValue;
    }

    private String masked(String value) {
        int length = safe(value).length();
        if (length <= 0) return "";
        return maskCharacter.repeat(length);
    }

    private void ensureCursorVisible(float viewportWidth) {
        int index = Math.max(0, Math.min(cursor, cursorStops.length - 1));
        float caretX = cursorStops[index];
        float maxOffset = Math.max(0, cursorStops[cursorStops.length - 1] - viewportWidth);
        if (caretX - scrollOffset > viewportWidth) scrollOffset = caretX - viewportWidth;
        if (caretX - scrollOffset < 0) scrollOffset = caretX;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
    }

    private String ellipsizeEnd(RenderContext context, String value, float font, float maxWidth) {
        String text = value == null ? "" : value;
        if (context.measurePlainText(text, font).width() <= maxWidth) return text;
        String ellipsis = "...";
        if (context.measurePlainText(ellipsis, font).width() > maxWidth) return "";
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (context.measurePlainText(text.substring(0, mid) + ellipsis, font).width() <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return text.substring(0, lo) + ellipsis;
    }

    private static void setClipboard(String value) {
        UiClipboard.set(value);
    }

    private static String getClipboard() {
        return UiClipboard.get();
    }

    protected static String safe(String value) {
        return value == null ? "" : value;
    }
}
