package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.animation.AnimatedFloat;
import dev.someoneok.crystalconfig.input.*;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.*;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;
import dev.someoneok.crystalconfig.utils.HsvColor;
import dev.someoneok.crystalconfig.utils.MathUtil;
import dev.someoneok.crystalconfig.utils.UiClipboard;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorPicker extends Component {
    private static final float OVERLAY_Z_BASE = 10000.0f;
    private final State<ColorRGBA> color;
    private final AnimatedFloat expansion = new AnimatedFloat(0).speed(18);
    private boolean expanded;
    private boolean allowAlpha;
    private HsvColor hsv;
    private DragMode dragMode = DragMode.NONE;
    private String inputText;
    private int inputCursor;
    private int inputSelectionAnchor;
    private float[] inputCursorStops = new float[] {0};
    private float inputScrollOffset;
    private boolean inputFocused;
    private boolean inputSelecting;
    private boolean inputValid = true;
    private float inputBlink;
    private float lastMouseX = Float.NaN;
    private float lastMouseY = Float.NaN;

    private static final Pattern FUNCTION = Pattern.compile("(?i)^([a-z]+)\\s*\\((.*)\\)$");

    private enum DragMode { NONE, SV, HUE, ALPHA }

    public ColorPicker(State<ColorRGBA> color) {
        this.color = color;
        this.hsv = HsvColor.from(stripAlphaIfNeeded(color.get()));
        this.inputText = formatColor(stripAlphaIfNeeded(color.get()), false);
        this.inputCursor = inputText.length();
        this.inputSelectionAnchor = this.inputCursor;
        this.focusable = true;
        size(220, 30);
    }

    /** Enables or disables alpha editing. When disabled, any parsed or dragged alpha is forced to 255. */
    public ColorPicker allowAlpha(boolean allowAlpha) {
        this.allowAlpha = allowAlpha;
        applyColor(color.get());
        syncInputText();
        return this;
    }

    @Override
    public void tick(float deltaSeconds) {
        if (!enabled()) {
            expanded = false;
            dragMode = DragMode.NONE;
            inputFocused = false;
            inputSelecting = false;
            setFocused(false);
            setHovered(false);
        }
        inputBlink += deltaSeconds;
        expansion.target(enabled() && expanded ? 1 : 0);
        expansion.update(deltaSeconds);
        ColorRGBA current = stripAlphaIfNeeded(color.get());
        if (!current.equals(color.get())) color.set(current);
        if (dragMode == DragMode.NONE) hsv = HsvColor.from(current);
        if (!inputFocused && !safe(inputText).equals(formatColor(current, allowAlpha))) syncInputText();
        super.tick(deltaSeconds);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 220, 30));
    }

    @Override
    protected void renderSelf(RenderContext context) {
        boolean disabled = !enabled();
        Rect header = headerRect();
        ColorRGBA headerFill = disabled
                ? context.theme().palette().surfaceAlt().withAlpha(120)
                : hovered || focused ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt();
        ColorRGBA headerBorder = disabled
                ? context.theme().palette().border().withAlpha(95)
                : focused ? context.theme().palette().accent() : context.theme().palette().border();
        context.rect(header, SdfRectStyle.create()
                .fill(headerFill)
                .border(1, headerBorder)
                .radius(context.theme().radii().md()), z);
        Rect swatch = new Rect(header.x() + 8, header.y() + 6, 18, 18);
        ColorRGBA preview = stripAlphaIfNeeded(color.get());
        context.rect(swatch, SdfRectStyle.create()
                .fill(context.theme().palette().surfaceAlt())
                .border(1, disabled ? context.theme().palette().border().withAlpha(95) : context.theme().palette().border())
                .radius(context.theme().radii().sm()), z + 1);
        Rect swatchInner = swatch.inset(2);
        if (preview.a() < 255) {
            context.rect(swatchInner, context.theme().palette().surfaceActive(), Math.max(0, context.theme().radii().sm() - 1), z + 1.5f);
        }
        context.rect(swatchInner, SdfRectStyle.create()
                .fill(disabled ? preview.withAlpha(Math.min(preview.a(), 95)) : preview)
                .border(0, ColorRGBA.TRANSPARENT)
                .radius(Math.max(0, context.theme().radii().sm() - 1)), z + 2);
        float font = context.theme().fonts().normal();
        context.text(formatColor(stripAlphaIfNeeded(color.get()), allowAlpha), header.x() + 34, header.centerY() - context.lineHeight(font) * 0.5f, font, disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().text(), z + 1);
        context.text(expanded ? "▲" : "▼", header.right() - 18, header.centerY() - context.lineHeight(font) * 0.5f, font, disabled ? context.theme().palette().mutedText().withAlpha(110) : context.theme().palette().mutedText(), z + 1);

        if (!disabled && expansion.value() > 0.001f) {
            Rect panel = panelRect(expansion.value());
            context.rect(panel, SdfRectStyle.create()
                    .fill(context.theme().palette().surface())
                    .border(1, context.theme().palette().border())
                    .shadow(10, 0, 2, context.theme().palette().shadow().multiplyAlpha(expansion.value() * 0.6f))
                    .radius(context.theme().radii().md())
                    .opacity(expansion.value()), z + OVERLAY_Z_BASE + 30);
            context.pushClip(panel);
            renderPickerBody(context, bodyAreaForPanel(panel), expansion.value());
            context.popClip();
        }
    }

    private void renderPickerBody(RenderContext context, Rect area, float opened) {
        Rect sv = svRect(area);
        Rect hue = hueRect(area);
        context.specialQuad(Material.HSV_SV, sv, ColorRGBA.WHITE, hsv.h, 0, z + OVERLAY_Z_BASE + 31);
        context.rect(sv, SdfRectStyle.create().fill(ColorRGBA.TRANSPARENT).border(1, context.theme().palette().border()).radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 32);
        context.specialQuad(Material.HSV_HUE, hue, ColorRGBA.WHITE, 0, 0, z + OVERLAY_Z_BASE + 31);
        context.rect(hue, SdfRectStyle.create().fill(ColorRGBA.TRANSPARENT).border(1, context.theme().palette().border()).radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 32);

        float kx = sv.x() + hsv.s * sv.w();
        float ky = sv.y() + (1.0f - hsv.v) * sv.h();
        ColorRGBA selected = stripAlphaIfNeeded(hsv.toColor());
        context.rect(new Rect(kx - 7, ky - 7, 14, 14), SdfRectStyle.create()
                .fill(context.theme().palette().shadow().multiplyAlpha(0.55f))
                .border(0, ColorRGBA.TRANSPARENT)
                .radius(7), z + OVERLAY_Z_BASE + 33);
        context.rect(new Rect(kx - 6, ky - 6, 12, 12), SdfRectStyle.create()
                .fill(ColorRGBA.WHITE)
                .border(0, ColorRGBA.TRANSPARENT)
                .radius(6), z + OVERLAY_Z_BASE + 34);
        context.rect(new Rect(kx - 4, ky - 4, 8, 8), SdfRectStyle.create()
                .fill(selected)
                .border(1, ColorRGBA.BLACK.withAlpha(120))
                .radius(4), z + OVERLAY_Z_BASE + 35);
        float hy = hue.y() + (hsv.h / 360.0f) * hue.h();
        context.rect(new Rect(hue.x() - 2, hy - 2, hue.w() + 4, 4), SdfRectStyle.create().fill(ColorRGBA.WHITE).border(1, context.theme().palette().border()).radius(2), z + OVERLAY_Z_BASE + 33);

        if (allowAlpha) {
            Rect alpha = alphaRect(area);
            int segments = 12;
            ColorRGBA opaque = hsv.withAlpha(1.0f).toColor();
            context.rect(alpha, SdfRectStyle.create()
                    .fill(context.theme().palette().surfaceAlt())
                    .border(1, context.theme().palette().border())
                    .radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 31);
            for (int i = 0; i < segments; i++) {
                float y0 = alpha.y() + alpha.h() * i / segments;
                float y1 = alpha.y() + alpha.h() * (i + 1) / segments;
                float t = 1.0f - (i + 0.5f) / segments;
                int a = Math.round(255.0f * t);
                context.rect(new Rect(alpha.x() + 1, y0, Math.max(1, alpha.w() - 2), Math.max(1, y1 - y0)), opaque.withAlpha(a), 0, z + OVERLAY_Z_BASE + 32);
            }
            context.rect(alpha, SdfRectStyle.create().fill(ColorRGBA.TRANSPARENT).border(1, context.theme().palette().border()).radius(context.theme().radii().sm()), z + OVERLAY_Z_BASE + 33);
            float ay = alpha.y() + (1.0f - hsv.a) * alpha.h();
            context.rect(new Rect(alpha.x() - 3, ay - 2, alpha.w() + 6, 4), SdfRectStyle.create().fill(ColorRGBA.WHITE).border(1, context.theme().palette().border()).radius(2), z + OVERLAY_Z_BASE + 34);
        }

        float inputReveal = revealProgress(opened, 0.62f, 1.0f);
        Rect inputBase = inputRect(area);
        float inputSlide = -(1.0f - inputReveal) * 14.0f;
        Rect input = new Rect(inputBase.x(), inputBase.y() + inputSlide, inputBase.w(), inputBase.h());
        ColorRGBA border = !inputValid ? context.theme().palette().danger() : inputFocused ? context.theme().palette().accent() : context.theme().palette().border();
        context.rect(input, SdfRectStyle.create()
                .fill(inputFocused ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt())
                .border(1, border)
                .radius(context.theme().radii().md())
                .opacity(inputReveal), z + OVERLAY_Z_BASE + 34);
        Rect clip = input.inset(8, 2, 8, 2);
        context.pushClip(clip);
        float font = context.theme().fonts().normal();
        float ty = input.centerY() - context.lineHeight(font) * 0.5f;
        String text = safe(inputText);
        updateInputCursorStops(context, text, font);
        ColorRGBA textColor = inputValid ? context.theme().palette().text() : context.theme().palette().danger();
        normalizeInputSelection(text);
        float textX = input.x() + 8;
        if (inputFocused) {
            ensureInputCursorVisible(clip.w());
            textX -= inputScrollOffset;
            if (hasInputSelection()) {
                int a = inputSelectionStart();
                int b = inputSelectionEnd();
                float sx = input.x() + 8 + inputCursorStops[a] - inputScrollOffset;
                float ex = input.x() + 8 + inputCursorStops[b] - inputScrollOffset;
                context.rect(new Rect(sx, input.y() + 5, Math.max(1, ex - sx), input.h() - 10),
                        context.theme().palette().accent().withAlpha(Math.round(70.0f * inputReveal)), context.theme().radii().sm(), z + OVERLAY_Z_BASE + 35);
            }
            context.plainText(text, textX, ty, font, textColor.multiplyAlpha(inputReveal), z + OVERLAY_Z_BASE + 36);
            if (((int)(inputBlink * 2) % 2 == 0)) {
                float cx = input.x() + 8 + inputCursorStops[Math.max(0, Math.min(inputCursor, text.length()))] - inputScrollOffset;
                if (text.isEmpty()) {
                    cx += 1.0f;
                }
                float caretWidth = Math.max(1.0f, 1.0f / context.scale());
                context.rect(new Rect(cx, input.y() + 6, caretWidth, input.h() - 12), context.theme().palette().accent().multiplyAlpha(inputReveal), 0, z + OVERLAY_Z_BASE + 37);
            }
        } else {
            inputScrollOffset = 0;
            String placeholder = allowAlpha ? "#RRGGBBAA / rgba(...)" : "#RRGGBB / rgb(...)";
            String shown = text.isEmpty() ? placeholder : ellipsizeInputEnd(context, text, font, clip.w());
            context.plainText(shown, textX, ty, font, text.isEmpty() ? context.theme().palette().mutedText().multiplyAlpha(inputReveal) : textColor.multiplyAlpha(inputReveal), z + OVERLAY_Z_BASE + 36);
        }
        context.popClip();
    }

    @Override
    public Component hitTest(float x, float y) {
        if (!visible() || !enabled()) return null;
        if (headerRect().contains(x, y)) return this;
        if (expanded && panelRect(1.0f).contains(x, y)) return this;
        return null;
    }

    @Override
    public Component hitTestOverlay(float x, float y) {
        if (!visible() || !enabled()) return null;
        if (expanded && panelRect(1.0f).contains(x, y)) return this;
        return super.hitTestOverlay(x, y);
    }

    @Override
    public String tooltip() {
        if (expanded && (headerRect().contains(lastMouseX, lastMouseY) || panelRect(1.0f).contains(lastMouseX, lastMouseY))) {
            return "";
        }
        return super.tooltip();
    }

    @Override
    public boolean onMouseMove(MouseMoveEvent event) {
        if (!enabled()) return false;
        lastMouseX = event.x;
        lastMouseY = event.y;
        return expanded && panelRect(1.0f).contains(event.x, event.y);
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (!enabled()) return false;
        lastMouseX = event.x;
        lastMouseY = event.y;
        if (event.button != MouseButton.LEFT) return false;
        Rect header = headerRect();
        if (header.contains(event.x, event.y)) {
            expanded = !expanded;
            inputFocused = false;
            return true;
        }
        if (!expanded) return false;
        Rect area = bodyArea();
        Rect input = inputRect(area);
        if (input.contains(event.x, event.y)) {
            inputFocused = true;
            inputBlink = 0;
            int target = estimateInputCursor(event.x);
            if (Modifiers.has(event.modifiers, Modifiers.SHIFT)) {
                inputCursor = target;
            } else {
                inputCursor = target;
                inputSelectionAnchor = inputCursor;
            }
            inputSelecting = true;
            inputValid = isParseable(inputText);
            return true;
        }
        inputFocused = false;
        inputSelecting = false;
        if (svRect(area).contains(event.x, event.y)) {
            dragMode = DragMode.SV;
            updateSv(event.x, event.y, area);
            return true;
        }
        if (hueRect(area).contains(event.x, event.y)) {
            dragMode = DragMode.HUE;
            updateHue(event.y, area);
            return true;
        }
        if (allowAlpha && alphaRect(area).contains(event.x, event.y)) {
            dragMode = DragMode.ALPHA;
            updateAlpha(event.y, area);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDragged(MouseDragEvent event) {
        if (!enabled()) return false;
        lastMouseX = event.x;
        lastMouseY = event.y;
        if (inputSelecting) {
            inputCursor = estimateInputCursor(event.x);
            inputBlink = 0;
            return true;
        }
        if (dragMode == DragMode.NONE) return false;
        Rect area = bodyArea();
        if (dragMode == DragMode.SV) updateSv(event.x, event.y, area);
        else if (dragMode == DragMode.HUE) updateHue(event.y, area);
        else updateAlpha(event.y, area);
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        if (!enabled()) return false;
        if (inputSelecting) {
            inputSelecting = false;
            return true;
        }
        if (dragMode != DragMode.NONE) {
            dragMode = DragMode.NONE;
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled()) return false;
        if (!inputFocused) {
            if (event.keyCode == KeyCodes.ENTER || event.keyCode == KeyCodes.SPACE) {
                expanded = !expanded;
                return true;
            }
            if (event.keyCode == KeyCodes.ESCAPE && expanded) {
                expanded = false;
                return true;
            }
            return false;
        }
        String value = safe(inputText);
        normalizeInputSelection(value);
        boolean ctrl = Modifiers.has(event.modifiers, Modifiers.CTRL) || Modifiers.has(event.modifiers, Modifiers.SUPER);
        boolean shift = Modifiers.has(event.modifiers, Modifiers.SHIFT);

        if (ctrl) {
            int key = Character.toUpperCase(event.keyCode);
            if (key == 'A') { selectAllInput(value); inputBlink = 0; return true; }
            if (key == 'C') { copyInputSelection(value); inputBlink = 0; return true; }
            if (key == 'X') { cutInputSelection(value); inputValid = isParseable(inputText); inputBlink = 0; return true; }
            if (key == 'V') { pasteInputClipboard(); inputValid = isParseable(inputText); inputBlink = 0; return true; }
            if (event.keyCode == KeyCodes.LEFT) { moveInputCursor(wordLeft(value), shift); inputBlink = 0; return true; }
            if (event.keyCode == KeyCodes.RIGHT) { moveInputCursor(wordRight(value), shift); inputBlink = 0; return true; }
            if (event.keyCode == KeyCodes.BACKSPACE) { deleteInputWordLeft(value); inputValid = isParseable(inputText); inputBlink = 0; return true; }
            if (event.keyCode == KeyCodes.DELETE) { deleteInputWordRight(value); inputValid = isParseable(inputText); inputBlink = 0; return true; }
        }

        switch (event.keyCode) {
            case KeyCodes.LEFT -> moveInputCursor(Math.max(0, inputCursor - 1), shift);
            case KeyCodes.RIGHT -> moveInputCursor(Math.min(value.length(), inputCursor + 1), shift);
            case KeyCodes.HOME -> moveInputCursor(0, shift);
            case KeyCodes.END -> moveInputCursor(value.length(), shift);
            case KeyCodes.BACKSPACE -> {
                if (hasInputSelection()) replaceInputSelection(value, "");
                else if (inputCursor > 0) replaceInputRange(value, inputCursor - 1, inputCursor, "");
                inputValid = isParseable(inputText);
            }
            case KeyCodes.DELETE -> {
                if (hasInputSelection()) replaceInputSelection(value, "");
                else if (inputCursor < value.length()) replaceInputRange(value, inputCursor, inputCursor + 1, "");
                inputValid = isParseable(inputText);
            }
            case KeyCodes.ENTER -> commitInput();
            case KeyCodes.ESCAPE -> {
                inputFocused = false;
                inputSelecting = false;
                syncInputText();
            }
            default -> { return false; }
        }
        inputBlink = 0;
        return true;
    }

    @Override
    public boolean onCharTyped(CharTypedEvent event) {
        if (!enabled()) return false;
        if (!inputFocused || Character.isISOControl(event.codePoint)) return false;
        String value = safe(inputText);
        normalizeInputSelection(value);
        String insert = String.valueOf(event.codePoint);
        if (hasInputSelection()) {
            if (value.length() - (inputSelectionEnd() - inputSelectionStart()) + insert.length() > 64) return true;
            replaceInputSelection(value, insert);
        } else {
            if (value.length() + insert.length() > 64) return true;
            replaceInputRange(value, inputCursor, inputCursor, insert);
        }
        inputValid = isParseable(inputText);
        inputBlink = 0;
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        inputBlink = 0;
        if (!focused) {
            if (inputFocused) commitInput();
            expanded = false;
            dragMode = DragMode.NONE;
            inputFocused = false;
            inputSelecting = false;
            inputSelectionAnchor = inputCursor;
        }
    }

    private Rect headerRect() { return new Rect(bounds.x(), bounds.y(), bounds.w(), 30); }

    private float revealProgress(float value, float start, float end) {
        float t = MathUtil.clamp((value - start) / Math.max(0.0001f, end - start), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private float fullPanelHeight() { return 162; }

    private Rect panelRect(float opened) {
        float fullHeight = fullPanelHeight();
        float height = Math.max(0, fullHeight * opened);
        Rect available = availableBounds();
        float belowY = bounds.y() + 34;
        float aboveY = bounds.y() - 4 - height;
        float belowSpace = available.bottom() - belowY;
        float aboveSpace = bounds.y() - available.y();
        boolean openAbove = belowSpace < fullHeight && aboveSpace > belowSpace;
        float y = openAbove ? aboveY : belowY;
        y = Math.max(available.y(), Math.min(y, available.bottom() - height));
        return new Rect(bounds.x(), y, bounds.w(), height);
    }

    private Rect bodyArea() { return bodyAreaForPanel(panelRect(1.0f)); }
    private Rect bodyAreaForPanel(Rect panel) { return new Rect(panel.x() + 10, panel.y() + 10, panel.w() - 20, Math.max(0, panel.h() - 20)); }

    private Rect availableBounds() {
        for (Component c = parent(); c != null; c = c.parent()) {
            if ("ScrollContainer".equals(c.getClass().getSimpleName())) return c.bounds();
        }
        Component root = this;
        while (root.parent() != null) root = root.parent();
        return root.bounds();
    }

    private Rect svRect(Rect area) { return new Rect(area.x(), area.y(), Math.max(48, area.w() - (allowAlpha ? 50 : 28)), 88); }
    private Rect hueRect(Rect area) { Rect sv = svRect(area); return new Rect(sv.right() + 10, sv.y(), 12, sv.h()); }
    private Rect alphaRect(Rect area) { Rect hue = hueRect(area); return new Rect(hue.right() + 10, hue.y(), 12, hue.h()); }
    private Rect inputRect(Rect area) {
        return new Rect(area.x(), svRect(area).bottom() + 14, area.w(), 28);
    }

    private void updateSv(float x, float y, Rect area) {
        Rect sv = svRect(area);
        float s = MathUtil.clamp((x - sv.x()) / Math.max(1, sv.w()), 0, 1);
        float v = 1.0f - MathUtil.clamp((y - sv.y()) / Math.max(1, sv.h()), 0, 1);
        hsv = new HsvColor(hsv.h, s, v, allowAlpha ? hsv.a : 1.0f);
        applyColor(hsv.toColor());
        syncInputText();
    }

    private void updateHue(float y, Rect area) {
        Rect hue = hueRect(area);
        float h = MathUtil.clamp((y - hue.y()) / Math.max(1, hue.h()), 0, 1) * 360.0f;
        hsv = new HsvColor(h, hsv.s, hsv.v, allowAlpha ? hsv.a : 1.0f);
        applyColor(hsv.toColor());
        syncInputText();
    }

    private void updateAlpha(float y, Rect area) {
        Rect alpha = alphaRect(area);
        float a = 1.0f - MathUtil.clamp((y - alpha.y()) / Math.max(1, alpha.h()), 0, 1);
        hsv = hsv.withAlpha(allowAlpha ? a : 1.0f);
        applyColor(hsv.toColor());
        syncInputText();
    }

    private void applyColor(ColorRGBA next) {
        ColorRGBA applied = stripAlphaIfNeeded(next);
        color.set(applied);
        hsv = HsvColor.from(applied);
    }

    private ColorRGBA stripAlphaIfNeeded(ColorRGBA value) {
        if (value == null) return allowAlpha ? ColorRGBA.TRANSPARENT : ColorRGBA.BLACK;
        return allowAlpha ? value : value.withAlpha(255);
    }

    private void syncInputText() {
        inputText = formatColor(stripAlphaIfNeeded(color.get()), allowAlpha);
        inputCursor = Math.min(inputCursor, inputText.length());
        inputSelectionAnchor = inputCursor;
        inputValid = true;
    }

    private void commitInput() {
        ColorRGBA parsed = parseColor(inputText);
        if (parsed == null) {
            inputValid = false;
            return;
        }
        applyColor(parsed);
        syncInputText();
        inputFocused = false;
    }

    private boolean isParseable(String text) { return parseColor(text) != null; }

    private ColorRGBA parseColor(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        try {
            ColorRGBA parsed;
            String lower = s.toLowerCase(Locale.ROOT);
            if (lower.equals("transparent")) parsed = ColorRGBA.rgba(0, 0, 0, 0);
            else if (lower.equals("white")) parsed = ColorRGBA.WHITE;
            else if (lower.equals("black")) parsed = ColorRGBA.BLACK;
            else if (lower.startsWith("#") || lower.matches("(?i)^[0-9a-f]{3,8}$") || lower.startsWith("0x")) parsed = parseHex(s);
            else {
                Matcher matcher = FUNCTION.matcher(s);
                if (matcher.matches()) parsed = parseFunction(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2));
                else if (s.contains(",")) parsed = parseRgbParts(s.split("\\s*,\\s*"));
                else return null;
            }
            return stripAlphaIfNeeded(parsed);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private ColorRGBA parseHex(String raw) {
        String h = raw.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.startsWith("0x") || h.startsWith("0X")) h = h.substring(2);
        if (h.length() == 3 || h.length() == 4) {
            StringBuilder expanded = new StringBuilder();
            for (int i = 0; i < h.length(); i++) {
                expanded.append(h.charAt(i)).append(h.charAt(i));
            }
            h = expanded.toString();
        }
        if (h.length() == 6) {
            return ColorRGBA.rgb(hexByte(h, 0), hexByte(h, 2), hexByte(h, 4));
        }
        if (h.length() == 8) {
            return ColorRGBA.rgba(hexByte(h, 0), hexByte(h, 2), hexByte(h, 4), hexByte(h, 6));
        }
        throw new IllegalArgumentException("Bad hex color");
    }

    private ColorRGBA parseFunction(String name, String body) {
        String normalized = body.replace('/', ',');
        String[] parts = normalized.split("\\s*,\\s*|\\s+");
        parts = compact(parts);
        return switch (name) {
            case "rgb", "rgba" -> parseRgbParts(parts);
            case "hsl", "hsla" -> parseHslParts(parts);
            default -> null;
        };
    }

    private ColorRGBA parseRgbParts(String[] parts) {
        if (parts.length < 3 || parts.length > 4) return null;
        int r = parseChannel(parts[0]);
        int g = parseChannel(parts[1]);
        int b = parseChannel(parts[2]);
        int a = parts.length == 4 ? parseAlpha(parts[3]) : 255;
        return ColorRGBA.rgba(r, g, b, a);
    }

    private ColorRGBA parseHslParts(String[] parts) {
        if (parts.length < 3 || parts.length > 4) return null;
        float h = parseHue(parts[0]);
        float s = parsePercent01(parts[1]);
        float l = parsePercent01(parts[2]);
        int a = parts.length == 4 ? parseAlpha(parts[3]) : 255;
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60.0f) % 2 - 1));
        float m = l - c / 2;
        float rp, gp, bp;
        if (h < 60) { rp = c; gp = x; bp = 0; }
        else if (h < 120) { rp = x; gp = c; bp = 0; }
        else if (h < 180) { rp = 0; gp = c; bp = x; }
        else if (h < 240) { rp = 0; gp = x; bp = c; }
        else if (h < 300) { rp = x; gp = 0; bp = c; }
        else { rp = c; gp = 0; bp = x; }
        return ColorRGBA.rgba(Math.round((rp + m) * 255), Math.round((gp + m) * 255), Math.round((bp + m) * 255), a);
    }

    private static String[] compact(String[] parts) {
        return java.util.Arrays.stream(parts).filter(p -> p != null && !p.isBlank()).toArray(String[]::new);
    }

    private static int hexByte(String value, int start) { return Integer.parseInt(value.substring(start, start + 2), 16); }

    private static int parseChannel(String raw) {
        String s = raw.trim();
        if (s.endsWith("%")) return clamp8(Math.round(Float.parseFloat(s.substring(0, s.length() - 1)) * 2.55f));
        return clamp8(Math.round(Float.parseFloat(s)));
    }

    private static int parseAlpha(String raw) {
        String s = raw.trim();
        if (s.endsWith("%")) return clamp8(Math.round(Float.parseFloat(s.substring(0, s.length() - 1)) * 2.55f));
        float value = Float.parseFloat(s);
        if (value <= 1.0f) return clamp8(Math.round(value * 255));
        return clamp8(Math.round(value));
    }

    private static float parseHue(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        float multiplier = 1.0f;
        if (s.endsWith("turn")) { multiplier = 360.0f; s = s.substring(0, s.length() - 4); }
        else if (s.endsWith("rad")) { multiplier = 57.29578f; s = s.substring(0, s.length() - 3); }
        else if (s.endsWith("deg")) { s = s.substring(0, s.length() - 3); }
        float h = Float.parseFloat(s) * multiplier;
        h %= 360.0f;
        return h < 0 ? h + 360.0f : h;
    }

    private static float parsePercent01(String raw) {
        String s = raw.trim();
        float value;
        if (s.endsWith("%")) value = Float.parseFloat(s.substring(0, s.length() - 1)) / 100.0f;
        else value = Float.parseFloat(s);
        return MathUtil.clamp(value, 0, 1);
    }

    private void moveInputCursor(int target, boolean extending) {
        inputCursor = Math.max(0, Math.min(target, safe(inputText).length()));
        if (!extending) inputSelectionAnchor = inputCursor;
    }

    private boolean hasInputSelection() { return inputSelectionAnchor != inputCursor; }
    private int inputSelectionStart() { return Math.min(inputSelectionAnchor, inputCursor); }
    private int inputSelectionEnd() { return Math.max(inputSelectionAnchor, inputCursor); }

    private void normalizeInputSelection(String value) {
        int len = value.length();
        inputCursor = Math.max(0, Math.min(inputCursor, len));
        inputSelectionAnchor = Math.max(0, Math.min(inputSelectionAnchor, len));
    }

    private void selectAllInput(String value) {
        inputSelectionAnchor = 0;
        inputCursor = value.length();
    }

    private void replaceInputSelection(String value, String insert) {
        replaceInputRange(value, inputSelectionStart(), inputSelectionEnd(), insert);
    }

    private void replaceInputRange(String value, int start, int end, String insert) {
        String safeInsert = insert == null ? "" : insert;
        int allowed = Math.max(0, 64 - (value.length() - (end - start)));
        if (safeInsert.length() > allowed) safeInsert = safeInsert.substring(0, allowed);
        inputText = value.substring(0, start) + safeInsert + value.substring(end);
        inputCursor = start + safeInsert.length();
        inputSelectionAnchor = inputCursor;
    }

    private void copyInputSelection(String value) {
        if (!hasInputSelection()) return;
        setClipboard(value.substring(inputSelectionStart(), inputSelectionEnd()));
    }

    private void cutInputSelection(String value) {
        if (!hasInputSelection()) return;
        copyInputSelection(value);
        replaceInputSelection(value, "");
    }

    private void pasteInputClipboard() {
        String paste = getClipboard();
        if (paste == null || paste.isEmpty()) return;
        paste = normalizePastedColorText(paste);
        if (paste.isEmpty()) return;

        ColorRGBA pastedColor = parseColor(paste);
        if (pastedColor != null) {
            applyColor(pastedColor);
            inputText = formatColor(stripAlphaIfNeeded(color.get()), allowAlpha);
            inputCursor = inputText.length();
            inputSelectionAnchor = inputCursor;
            inputValid = true;
            return;
        }

        String value = safe(inputText);
        int start = hasInputSelection() ? inputSelectionStart() : inputCursor;
        int end = hasInputSelection() ? inputSelectionEnd() : inputCursor;
        int allowed = Math.max(0, 64 - (value.length() - (end - start)));
        String insert = paste.length() > allowed ? paste.substring(0, allowed) : paste;
        String combined = value.substring(0, start) + insert + value.substring(end);
        ColorRGBA combinedColor = parseColor(combined);
        if (combinedColor != null) {
            applyColor(combinedColor);
            inputText = formatColor(stripAlphaIfNeeded(color.get()), allowAlpha);
            inputCursor = inputText.length();
            inputSelectionAnchor = inputCursor;
            inputValid = true;
            return;
        }

        replaceInputRange(value, start, end, insert);
    }

    private static String normalizePastedColorText(String text) {
        String s = text.replace("\r", "").replace("\n", "").trim();
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                s = s.substring(1, s.length() - 1).trim();
            }
        }
        return s;
    }

    private void deleteInputWordLeft(String value) {
        if (hasInputSelection()) { replaceInputSelection(value, ""); return; }
        int target = wordLeft(value);
        if (target < inputCursor) replaceInputRange(value, target, inputCursor, "");
    }

    private void deleteInputWordRight(String value) {
        if (hasInputSelection()) { replaceInputSelection(value, ""); return; }
        int target = wordRight(value);
        if (target > inputCursor) replaceInputRange(value, inputCursor, target, "");
    }

    private int wordLeft(String value) {
        int i = Math.max(0, inputCursor - 1);
        while (i > 0 && Character.isWhitespace(value.charAt(i))) i--;
        while (i > 0 && !Character.isWhitespace(value.charAt(i - 1))) i--;
        return i;
    }

    private int wordRight(String value) {
        int i = Math.min(value.length(), inputCursor);
        while (i < value.length() && Character.isWhitespace(value.charAt(i))) i++;
        while (i < value.length() && !Character.isWhitespace(value.charAt(i))) i++;
        return i;
    }

    private static void setClipboard(String value) {
        UiClipboard.set(value);
    }

    private static String getClipboard() {
        return UiClipboard.get();
    }

    private int estimateInputCursor(float mouseX) {
        String value = safe(inputText);
        if (value.isEmpty()) return 0;
        float local = Math.max(0, mouseX - inputRect(bodyArea()).x() - 8 + inputScrollOffset);
        if (inputCursorStops != null && inputCursorStops.length == value.length() + 1) {
            int best = 0;
            float bestDistance = Float.MAX_VALUE;
            for (int i = 0; i < inputCursorStops.length; i++) {
                float distance = Math.abs(inputCursorStops[i] - local);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = i;
                }
            }
            return best;
        }
        float t = Math.max(0, Math.min(1, local / Math.max(1, inputRect(bodyArea()).w() - 16)));
        return Math.max(0, Math.min(value.length(), Math.round(t * value.length())));
    }

    private void updateInputCursorStops(RenderContext context, String value, float font) {
        inputCursorStops = new float[value.length() + 1];
        inputCursorStops[0] = 0;
        for (int i = 1; i <= value.length(); i++) {
            inputCursorStops[i] = context.measurePlainText(value.substring(0, i), font).width();
        }
    }

    private void ensureInputCursorVisible(float viewportWidth) {
        int index = Math.max(0, Math.min(inputCursor, inputCursorStops.length - 1));
        float caretX = inputCursorStops[index];
        float maxOffset = Math.max(0, inputCursorStops[inputCursorStops.length - 1] - viewportWidth);
        if (caretX - inputScrollOffset > viewportWidth) inputScrollOffset = caretX - viewportWidth;
        if (caretX - inputScrollOffset < 0) inputScrollOffset = caretX;
        inputScrollOffset = Math.max(0, Math.min(inputScrollOffset, maxOffset));
    }

    private String ellipsizeInputEnd(RenderContext context, String value, float font, float maxWidth) {
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

    private static int clamp8(int value) { return Math.max(0, Math.min(255, value)); }
    private static String safe(String value) { return value == null ? "" : value; }

    private static String formatColor(ColorRGBA color, boolean includeAlpha) {
        if (includeAlpha) return String.format("#%02X%02X%02X%02X", color.r(), color.g(), color.b(), color.a());
        return String.format("#%02X%02X%02X", color.r(), color.g(), color.b());
    }
}
