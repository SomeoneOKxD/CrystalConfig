package dev.someoneok.crystalconfig.api.render;

import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.DrawList;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderBackend;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.RenderFrame;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.render.TextMetrics;
import dev.someoneok.crystalconfig.render.TextureFilter;
import dev.someoneok.crystalconfig.render.TextureRef;
import dev.someoneok.crystalconfig.theme.Theme;
import dev.someoneok.crystalconfig.theme.ThemePresets;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Lightweight, stateless drawing helpers backed by CrystalConfig's batched SDF/MSDF renderer.
 *
 * <p>The host owns all values and input handling. This class only keeps a reusable command buffer;
 * it does not persist control state, config values, hover state, or click state.</p>
 *
 * <p>Reuse one instance for a screen and call {@link #render} once per rendered frame. Draw methods
 * are only valid from inside the render callback. Instances are not thread-safe and are intended to
 * be used from the host renderer thread.</p>
 */
public final class UiRenderUtils {
    private static final float DEFAULT_Z = 0.0f;
    private static final Theme DEFAULT_THEME = ThemePresets.darkCrimson();

    private final DrawList drawList = new DrawList();
    private final Map<String, TextureRef> textureRefs = new HashMap<>(16);
    private RenderContext context;
    private Theme theme;
    private boolean rendering;

    public void render(RenderBackend backend, RenderFrame frame, Consumer<UiRenderUtils> draw) {
        render(backend, frame, DEFAULT_THEME, 1.0f, false, draw);
    }

    public void render(RenderBackend backend, RenderFrame frame, Theme theme, Consumer<UiRenderUtils> draw) {
        render(backend, frame, theme, 1.0f, false, draw);
    }

    public void render(
            RenderBackend backend,
            RenderFrame frame,
            Theme theme,
            float scale,
            boolean textShadow,
            Consumer<UiRenderUtils> draw
    ) {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(draw, "draw");
        if (rendering) throw new IllegalStateException("UiRenderUtils.render cannot be nested");

        rendering = true;
        this.theme = theme;
        drawList.clear();
        context = new RenderContext(drawList, backend, theme, frame.deltaSeconds(), false, sanitizeScale(scale), textShadow);
        boolean frameBegun = false;
        try {
            backend.beginFrame(frame);
            frameBegun = true;
            draw.accept(this);
            drawList.flush(backend);
        } finally {
            // flush() clears on success; clear here as well so exceptions never leak commands into the next frame.
            drawList.clear();
            try {
                if (frameBegun) backend.endFrame();
            } finally {
                context = null;
                this.theme = null;
                rendering = false;
            }
        }
    }

    public Theme theme() {
        requireFrame();
        return theme;
    }

    public void rect(Rect bounds, ColorRGBA color) {
        rect(bounds, color, DEFAULT_Z);
    }

    public void rect(Rect bounds, ColorRGBA color, float z) {
        requireFrame().rect(bounds, color, 0.0f, z);
    }

    public void rect(float x, float y, float width, float height, ColorRGBA color) {
        rect(new Rect(x, y, width, height), color, DEFAULT_Z);
    }

    public void rect(float x, float y, float width, float height, ColorRGBA color, float z) {
        rect(new Rect(x, y, width, height), color, z);
    }

    public void roundedRect(Rect bounds, ColorRGBA color, float radius) {
        roundedRect(bounds, color, radius, DEFAULT_Z);
    }

    public void roundedRect(Rect bounds, ColorRGBA color, float radius, float z) {
        requireFrame().rect(bounds, color, radius, z);
    }

    public void roundedRect(float x, float y, float width, float height, ColorRGBA color, float radius) {
        roundedRect(new Rect(x, y, width, height), color, radius, DEFAULT_Z);
    }

    public void roundedRect(float x, float y, float width, float height, ColorRGBA color, float radius, float z) {
        roundedRect(new Rect(x, y, width, height), color, radius, z);
    }

    public void circle(float centerX, float centerY, float radius, ColorRGBA color, float z) {
        float safeRadius = Math.max(0.0f, radius);
        float diameter = safeRadius * 2.0f;
        roundedRect(new Rect(centerX - safeRadius, centerY - safeRadius, diameter, diameter), color, safeRadius, z);
    }

    public void circle(float centerX, float centerY, float radius, ColorRGBA color) {
        circle(centerX, centerY, radius, color, DEFAULT_Z);
    }

    /** Draws a one-pixel, non-rounded line between two points. */
    public void line(float x1, float y1, float x2, float y2, ColorRGBA color) {
        line(x1, y1, x2, y2, 1.0f, false, color, DEFAULT_Z);
    }

    public void line(float x1, float y1, float x2, float y2, float thickness, ColorRGBA color) {
        line(x1, y1, x2, y2, thickness, false, color, DEFAULT_Z);
    }

    public void line(float x1, float y1, float x2, float y2, float thickness, boolean roundCaps, ColorRGBA color) {
        line(x1, y1, x2, y2, thickness, roundCaps, color, DEFAULT_Z);
    }

    /**
     * Draws an arbitrary batched line. Coordinates are line-center endpoints; rounded caps extend
     * visually by half the thickness beyond each endpoint.
     */
    public void line(float x1, float y1, float x2, float y2, float thickness, boolean roundCaps, ColorRGBA color, float z) {
        requireFrame().line(x1, y1, x2, y2, thickness, color, roundCaps, z);
    }

    public void horizontalLine(float x1, float x2, float y, ColorRGBA color) {
        horizontalLine(x1, x2, y, 1.0f, color, DEFAULT_Z);
    }

    public void horizontalLine(float x1, float x2, float y, float thickness, ColorRGBA color) {
        horizontalLine(x1, x2, y, thickness, color, DEFAULT_Z);
    }

    public void horizontalLine(float x1, float x2, float y, float thickness, ColorRGBA color, float z) {
        line(x1, y, x2, y, thickness, false, color, z);
    }

    public void verticalLine(float x, float y1, float y2, ColorRGBA color) {
        verticalLine(x, y1, y2, 1.0f, color, DEFAULT_Z);
    }

    public void verticalLine(float x, float y1, float y2, float thickness, ColorRGBA color) {
        verticalLine(x, y1, y2, thickness, color, DEFAULT_Z);
    }

    public void verticalLine(float x, float y1, float y2, float thickness, ColorRGBA color, float z) {
        line(x, y1, x, y2, thickness, false, color, z);
    }

    /** Draws a square point centered on {@code x,y}. */
    public void point(float x, float y, float size, ColorRGBA color) {
        point(x, y, size, color, DEFAULT_Z);
    }

    public void point(float x, float y, float size, ColorRGBA color, float z) {
        float safeSize = Math.max(0.0f, size);
        rect(new Rect(x - safeSize * 0.5f, y - safeSize * 0.5f, safeSize, safeSize), color, z);
    }

    public void outlineRect(Rect bounds, ColorRGBA color, float thickness) {
        outlineRect(bounds, color, thickness, DEFAULT_Z);
    }

    public void outlineRect(Rect bounds, ColorRGBA color, float thickness, float z) {
        requireFrame().rect(bounds, SdfRectStyle.create()
                .fill(ColorRGBA.TRANSPARENT)
                .border(Math.max(0.0f, thickness), color)
                .radius(0.0f), z);
    }

    public void verticalGradient(Rect bounds, ColorRGBA top, ColorRGBA bottom) {
        verticalGradient(bounds, top, bottom, DEFAULT_Z);
    }

    public void verticalGradient(Rect bounds, ColorRGBA top, ColorRGBA bottom, float z) {
        requireFrame().gradient(bounds, top, bottom, false, z);
    }

    public void horizontalGradient(Rect bounds, ColorRGBA left, ColorRGBA right) {
        horizontalGradient(bounds, left, right, DEFAULT_Z);
    }

    public void horizontalGradient(Rect bounds, ColorRGBA left, ColorRGBA right, float z) {
        requireFrame().gradient(bounds, left, right, true, z);
    }

    /** Draws an entire texture into {@code bounds} using crisp Minecraft-style nearest sampling. */
    public void image(String texture, Rect bounds) {
        image(textureRef(texture), bounds, ColorRGBA.WHITE, TextureFilter.NEAREST, DEFAULT_Z);
    }

    public void image(TextureRef texture, Rect bounds) {
        image(texture, bounds, ColorRGBA.WHITE, TextureFilter.NEAREST, DEFAULT_Z);
    }

    public void image(TextureRef texture, Rect bounds, float z) {
        image(texture, bounds, ColorRGBA.WHITE, TextureFilter.NEAREST, z);
    }

    public void image(TextureRef texture, Rect bounds, ColorRGBA tint) {
        image(texture, bounds, tint, TextureFilter.NEAREST, DEFAULT_Z);
    }

    public void image(TextureRef texture, Rect bounds, ColorRGBA tint, TextureFilter filter, float z) {
        imageUv(texture, bounds, 0.0f, 0.0f, 1.0f, 1.0f, tint, filter, z);
    }

    public void image(String texture, float x, float y, float width, float height) {
        image(textureRef(texture), new Rect(x, y, width, height));
    }

    /**
     * Familiar Minecraft-style blit helper. Source-region width/height match the destination size,
     * like {@code GuiGraphicsExtractor.blit(..., u, v, width, height, textureWidth, textureHeight)}.
     */
    public void blit(
            String texture,
            float x,
            float y,
            float u,
            float v,
            float width,
            float height,
            float textureWidth,
            float textureHeight
    ) {
        blit(textureRef(texture), x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void blit(
            TextureRef texture,
            float x,
            float y,
            float u,
            float v,
            float width,
            float height,
            float textureWidth,
            float textureHeight
    ) {
        imageRegion(
                texture,
                new Rect(x, y, width, height),
                u, v, width, height, textureWidth, textureHeight
        );
    }

    /**
     * Draws a pixel-space atlas region, matching the common {@code GuiGraphicsExtractor.blit}
     * source-region model while still going through the batched renderer.
     */
    public void imageRegion(
            TextureRef texture,
            Rect bounds,
            float u,
            float v,
            float regionWidth,
            float regionHeight,
            float textureWidth,
            float textureHeight
    ) {
        imageRegion(texture, bounds, u, v, regionWidth, regionHeight, textureWidth, textureHeight,
                ColorRGBA.WHITE, TextureFilter.NEAREST, DEFAULT_Z);
    }

    public void imageRegion(
            TextureRef texture,
            Rect bounds,
            float u,
            float v,
            float regionWidth,
            float regionHeight,
            float textureWidth,
            float textureHeight,
            ColorRGBA tint,
            TextureFilter filter,
            float z
    ) {
        if (!Float.isFinite(textureWidth) || !Float.isFinite(textureHeight) || textureWidth <= 0.0f || textureHeight <= 0.0f) {
            throw new IllegalArgumentException("textureWidth and textureHeight must be finite and > 0");
        }
        imageUv(
                texture,
                bounds,
                u / textureWidth,
                v / textureHeight,
                (u + regionWidth) / textureWidth,
                (v + regionHeight) / textureHeight,
                tint,
                filter,
                z
        );
    }

    /** Draws normalized UVs. This is the lowest-overhead atlas/sprite entry point. */
    public void imageUv(
            TextureRef texture,
            Rect bounds,
            float u0,
            float v0,
            float u1,
            float v1,
            ColorRGBA tint,
            TextureFilter filter,
            float z
    ) {
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(tint, "tint");
        requireFrame().image(
                texture,
                bounds,
                new Rect(u0, v0, u1 - u0, v1 - v0),
                tint,
                filter == null ? TextureFilter.NEAREST : filter,
                z
        );
    }

    /** Alias for callers that prefer texture terminology over image terminology. */
    public void texture(TextureRef texture, Rect bounds) {
        image(texture, bounds);
    }

    public void roundedRect(Rect bounds, SdfRectStyle style, float z) {
        requireFrame().rect(bounds, style, z);
    }

    public void outlinedRoundedRect(Rect bounds, ColorRGBA fill, ColorRGBA border, float borderWidth, float radius) {
        outlinedRoundedRect(bounds, fill, border, borderWidth, radius, DEFAULT_Z);
    }

    public void outlinedRoundedRect(Rect bounds, ColorRGBA fill, ColorRGBA border, float borderWidth, float radius, float z) {
        requireFrame().rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(borderWidth, border)
                .radius(radius), z);
    }

    public void panel(Rect bounds) {
        panel(bounds, DEFAULT_Z);
    }

    public void panel(Rect bounds, float z) {
        Theme t = theme();
        outlinedRoundedRect(bounds, t.palette().surface(), t.palette().border(), 1.0f, t.radii().lg(), z);
    }

    public void separator(Rect bounds) {
        separator(bounds, DEFAULT_Z);
    }

    public void separator(Rect bounds, float z) {
        requireFrame().rect(new Rect(bounds.x(), bounds.centerY(), bounds.w(), 1.0f), theme().palette().border(), 0.0f, z);
    }

    public void progressBar(Rect bounds, float progress) {
        progressBar(bounds, progress, DEFAULT_Z);
    }

    public void progressBar(Rect bounds, float progress, float z) {
        float value = clamp01(progress);
        Theme t = theme();
        float radius = bounds.h() * 0.5f;
        requireFrame().rect(bounds, SdfRectStyle.create()
                .fill(t.palette().surfaceAlt())
                .border(1.0f, t.palette().border())
                .radius(radius), z);
        if (value > 0.0f) {
            Rect fill = new Rect(bounds.x(), bounds.y(), bounds.w() * value, bounds.h());
            requireFrame().rect(fill, t.palette().accent(), radius, z + 0.1f);
        }
    }

    public void button(Rect bounds, String label, UiControlState state) {
        button(bounds, label, state, ButtonVariant.DEFAULT, DEFAULT_Z);
    }

    public void button(Rect bounds, String label, boolean hovered, boolean pressed) {
        button(bounds, label, UiControlState.of(hovered, pressed, false, true), ButtonVariant.DEFAULT, DEFAULT_Z);
    }

    public void button(float x, float y, float width, float height, String label, UiControlState state) {
        button(new Rect(x, y, width, height), label, state, ButtonVariant.DEFAULT, DEFAULT_Z);
    }

    public void button(Rect bounds, String label, UiControlState state, ButtonVariant variant) {
        button(bounds, label, state, variant, DEFAULT_Z);
    }

    public void button(Rect bounds, String label, UiControlState state, ButtonVariant variant, float z) {
        UiControlState s = state == null ? UiControlState.NORMAL : state;
        button(
                bounds,
                label,
                s.hovered() ? 1.0f : 0.0f,
                s.pressed() ? 1.0f : 0.0f,
                s.focused(),
                s.enabled(),
                variant,
                z
        );
    }

    public void button(
            Rect bounds,
            String label,
            float hoverAmount,
            float pressAmount,
            boolean focused,
            boolean enabled,
            ButtonVariant variant
    ) {
        button(bounds, label, hoverAmount, pressAmount, focused, enabled, variant, DEFAULT_Z);
    }

    /**
     * Draws a button using host-owned 0..1 hover/press animation amounts.
     */
    public void button(
            Rect bounds,
            String label,
            float hoverAmount,
            float pressAmount,
            boolean focused,
            boolean enabled,
            ButtonVariant variant,
            float z
    ) {
        Objects.requireNonNull(bounds, "bounds");
        ButtonVariant v = variant == null ? ButtonVariant.DEFAULT : variant;
        Theme t = theme();

        ColorRGBA base;
        ColorRGBA hover;
        ColorRGBA active;
        ColorRGBA text;

        if (!enabled) {
            base = t.palette().surfaceAlt().withAlpha(120);
            hover = base;
            active = base;
            text = t.palette().mutedText().withAlpha(135);
        } else if (v == ButtonVariant.ACCENT) {
            base = t.palette().accent();
            hover = base.lighten(0.14f);
            active = base.darken(0.12f);
            text = t.palette().accentText();
        } else if (v == ButtonVariant.DANGER) {
            base = t.palette().danger();
            hover = base.lighten(0.12f);
            active = base.darken(0.14f);
            text = ColorRGBA.WHITE;
        } else {
            base = t.palette().surfaceAlt();
            hover = t.palette().surfaceHover();
            active = t.palette().surfaceActive();
            text = t.palette().text();
        }

        ColorRGBA fill = base.lerp(hover, clamp01(hoverAmount)).lerp(active, clamp01(pressAmount));
        ColorRGBA border = !enabled
                ? t.palette().border().withAlpha(95)
                : focused ? t.palette().accent() : t.palette().border();

        requireFrame().rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1.0f, border)
                .radius(t.radii().md()), z);

        String shown = ellipsize(label == null ? "" : label, t.fonts().normal(), t.fonts().regular(), Math.max(8.0f, bounds.w() - 16.0f));
        TextMetrics metrics = measureText(shown, t.fonts().normal(), t.fonts().regular());
        float tx = bounds.centerX() - metrics.width() * 0.5f;
        float ty = bounds.centerY() - metrics.height() * 0.5f;
        RenderContext ctx = requireFrame();
        ctx.pushClip(bounds.inset(8.0f, 0.0f, 8.0f, 0.0f));
        try {
            ctx.text(shown, tx, ty, t.fonts().normal(), t.fonts().regular(), text, z + 1.0f);
        } finally {
            ctx.popClip();
        }
    }

    public void toggle(Rect bounds, boolean value, UiControlState state) {
        toggle(bounds, value ? 1.0f : 0.0f, state, DEFAULT_Z);
    }

    public void toggle(Rect bounds, float amount, UiControlState state) {
        toggle(bounds, amount, state, DEFAULT_Z);
    }

    public void toggle(float x, float y, float width, float height, boolean value, UiControlState state) {
        toggle(new Rect(x, y, width, height), value ? 1.0f : 0.0f, state, DEFAULT_Z);
    }

    public void toggle(Rect bounds, float amount, UiControlState state, float z) {
        UiControlState s = state == null ? UiControlState.NORMAL : state;
        Theme t = theme();
        float value = clamp01(amount);
        ColorRGBA off = t.palette().surfaceAlt();
        ColorRGBA on = s.enabled() ? t.palette().accent() : t.palette().mutedText().withAlpha(135);
        ColorRGBA fill = s.enabled() ? off.lerp(on, value) : off.withAlpha(120).lerp(on, value);
        ColorRGBA border = !s.enabled()
                ? t.palette().border().withAlpha(95)
                : s.focused() ? t.palette().accent() : t.palette().border();

        requireFrame().rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1.0f, border)
                .radius(bounds.h() * 0.5f), z);

        float knob = Math.max(0.0f, bounds.h() - 6.0f);
        float knobX = bounds.x() + 3.0f + Math.max(0.0f, bounds.w() - knob - 6.0f) * value;
        Rect knobRect = new Rect(knobX, bounds.y() + 3.0f, knob, knob);
        requireFrame().rect(knobRect, SdfRectStyle.create()
                .fill(s.enabled() ? ColorRGBA.WHITE : t.palette().mutedText().withAlpha(150))
                .radius(knob * 0.5f), z + 1.0f);
    }

    public void checkbox(Rect bounds, boolean checked, UiControlState state) {
        checkbox(bounds, checked ? 1.0f : 0.0f, state, DEFAULT_Z);
    }

    public void checkbox(Rect bounds, float amount, UiControlState state) {
        checkbox(bounds, amount, state, DEFAULT_Z);
    }

    public void checkbox(float x, float y, float width, float height, boolean checked, UiControlState state) {
        checkbox(new Rect(x, y, width, height), checked ? 1.0f : 0.0f, state, DEFAULT_Z);
    }

    public void checkbox(Rect bounds, float amount, UiControlState state, float z) {
        UiControlState s = state == null ? UiControlState.NORMAL : state;
        Theme t = theme();
        float value = clamp01(amount);
        ColorRGBA checkedColor = s.enabled() ? t.palette().accent() : t.palette().mutedText().withAlpha(135);
        ColorRGBA fill = t.palette().surfaceAlt().withAlpha(s.enabled() ? 255 : 120).lerp(checkedColor, value);
        ColorRGBA border = !s.enabled()
                ? t.palette().border().withAlpha(95)
                : s.focused() ? t.palette().accent() : t.palette().border();

        requireFrame().rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1.0f, border)
                .radius(t.radii().sm()), z);

        if (value > 0.02f) {
            String mark = "✔";
            float font = t.fonts().normal() + 1.0f;
            String face = t.fonts().semibold();
            TextMetrics metrics = measureText(mark, font, face);
            float x = bounds.centerX() - metrics.width() * 0.5f;
            float y = bounds.centerY() - metrics.height() * 0.5f - 0.5f;
            ColorRGBA color = (s.enabled() ? t.palette().accentText() : t.palette().surface()).multiplyAlpha(value);
            text(mark, x, y, font, face, color, z + 1.0f);
        }
    }

    /**
     * Draws a stateless text input. The host owns {@code value}, focus, caret, selection, and saving.
     * This convenience overload places the caret at the end when focused.
     */
    public void textInput(Rect bounds, String value, String placeholder, UiControlState state) {
        String safe = safeInput(value);
        textInput(bounds, safe, placeholder, safe.length(), safe.length(), state, true, DEFAULT_Z);
    }

    public void textInput(
            Rect bounds,
            String value,
            String placeholder,
            int cursor,
            int selectionAnchor,
            UiControlState state
    ) {
        textInput(bounds, value, placeholder, cursor, selectionAnchor, state, true, DEFAULT_Z);
    }

    /**
     * Draws a text input with caller-owned caret/selection and validation state.
     * Set {@code valid} to false to show the danger border.
     */
    public void textInput(
            Rect bounds,
            String value,
            String placeholder,
            int cursor,
            int selectionAnchor,
            UiControlState state,
            boolean valid
    ) {
        textInput(bounds, value, placeholder, cursor, selectionAnchor, state, valid, DEFAULT_Z);
    }

    public void textInput(
            Rect bounds,
            String value,
            String placeholder,
            int cursor,
            int selectionAnchor,
            UiControlState state,
            boolean valid,
            float z
    ) {
        inputField(bounds, value, placeholder, cursor, selectionAnchor, state, valid, z);
    }

    /**
     * Draws a numeric text input. Use a string draft while editing so temporary values such as
     * {@code -} or {@code 12.} can be displayed; parse/save the number in the host screen.
     */
    public void numberInput(Rect bounds, String value, String placeholder, UiControlState state) {
        String safe = safeInput(value);
        boolean valid = safe.isEmpty() || TextInputEdit.isValidNumber(safe, true, -Double.MAX_VALUE, Double.MAX_VALUE);
        numberInput(bounds, safe, placeholder, safe.length(), safe.length(), state, valid, DEFAULT_Z);
    }

    public void numberInput(
            Rect bounds,
            String value,
            String placeholder,
            int cursor,
            int selectionAnchor,
            UiControlState state,
            boolean valid
    ) {
        numberInput(bounds, value, placeholder, cursor, selectionAnchor, state, valid, DEFAULT_Z);
    }

    public void numberInput(
            Rect bounds,
            String value,
            String placeholder,
            int cursor,
            int selectionAnchor,
            UiControlState state,
            boolean valid,
            float z
    ) {
        inputField(bounds, value, placeholder, cursor, selectionAnchor, state, valid, z);
    }

    private void inputField(
            Rect bounds,
            String value,
            String placeholder,
            int cursor,
            int selectionAnchor,
            UiControlState state,
            boolean valid,
            float z
    ) {
        Objects.requireNonNull(bounds, "bounds");
        UiControlState s = state == null ? UiControlState.NORMAL : state;
        Theme t = theme();
        String safeValue = safeInput(value);
        String safePlaceholder = safeInput(placeholder);
        int caret = clampIndex(cursor, safeValue.length());
        int anchor = clampIndex(selectionAnchor, safeValue.length());

        ColorRGBA border = !s.enabled()
                ? t.palette().border().withAlpha(95)
                : !valid ? t.palette().danger()
                : s.focused() ? t.palette().accent()
                : t.palette().border();
        ColorRGBA fill = !s.enabled()
                ? t.palette().surfaceAlt().withAlpha(120)
                : (s.hovered() || s.focused()) ? t.palette().surfaceHover()
                : t.palette().surfaceAlt();

        RenderContext ctx = requireFrame();
        ctx.rect(bounds, SdfRectStyle.create()
                .fill(fill)
                .border(1.0f, border)
                .radius(t.radii().md()), z);

        Rect clip = bounds.inset(8.0f, 2.0f, 8.0f, 2.0f);
        float fontSize = t.fonts().normal();
        String fontFace = t.fonts().regular();
        float textY = bounds.centerY() - ctx.lineHeight(fontSize, fontFace) * 0.5f;
        ColorRGBA valueColor = s.enabled() ? t.palette().text() : t.palette().mutedText().withAlpha(135);
        ColorRGBA placeholderColor = s.enabled() ? t.palette().mutedText() : t.palette().mutedText().withAlpha(110);

        ctx.pushClip(clip);
        try {
            if (!s.focused()) {
                String shown = safeValue.isEmpty() ? safePlaceholder : safeValue;
                ColorRGBA color = safeValue.isEmpty() ? placeholderColor : valueColor;
                shown = ellipsizePlain(shown, fontSize, fontFace, clip.w());
                ctx.plainText(shown, bounds.x() + 8.0f, textY, fontSize, fontFace, color, z + 1.0f);
                return;
            }

            float fullWidth = plainWidth(safeValue, fontSize, fontFace);
            float caretOffset = plainWidth(safeValue.substring(0, caret), fontSize, fontFace);
            float maxScroll = Math.max(0.0f, fullWidth - Math.max(0.0f, clip.w() - 1.0f));
            float scrollOffset = Math.max(0.0f, Math.min(maxScroll, caretOffset - Math.max(0.0f, clip.w() - 2.0f)));
            float textX = bounds.x() + 8.0f - scrollOffset;

            if (caret != anchor) {
                int selectionStart = Math.min(caret, anchor);
                int selectionEnd = Math.max(caret, anchor);
                float selectionX = textX + plainWidth(safeValue.substring(0, selectionStart), fontSize, fontFace);
                float selectionEndX = textX + plainWidth(safeValue.substring(0, selectionEnd), fontSize, fontFace);
                ctx.rect(
                        new Rect(selectionX, bounds.y() + 5.0f, Math.max(1.0f, selectionEndX - selectionX), Math.max(0.0f, bounds.h() - 10.0f)),
                        t.palette().accent().withAlpha(70),
                        t.radii().sm(),
                        z + 0.5f
                );
            }

            ctx.plainText(safeValue, textX, textY, fontSize, fontFace, valueColor, z + 1.0f);
            float caretX = textX + caretOffset + (safeValue.isEmpty() ? 1.0f : 0.0f);
            float caretWidth = Math.max(1.0f, 1.0f / ctx.scale());
            ctx.rect(
                    new Rect(caretX, bounds.y() + 6.0f, caretWidth, Math.max(0.0f, bounds.h() - 12.0f)),
                    t.palette().accent(),
                    0.0f,
                    z + 2.0f
            );
        } finally {
            ctx.popClip();
        }
    }

    public void text(String text, float x, float y) {
        Theme t = theme();
        text(text, x, y, t.fonts().normal(), t.fonts().regular(), t.palette().text(), DEFAULT_Z);
    }

    public void text(String text, float x, float y, float fontSize, ColorRGBA color) {
        text(text, x, y, fontSize, theme().fonts().regular(), color, DEFAULT_Z);
    }

    public void text(String text, float x, float y, float fontSize, ColorRGBA color, float z) {
        text(text, x, y, fontSize, theme().fonts().regular(), color, z);
    }

    public void text(String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float z) {
        requireFrame().text(text, x, y, fontSize, fontFace, color, z);
    }

    public void plainText(String text, float x, float y, float fontSize, String fontFace, ColorRGBA color, float z) {
        requireFrame().plainText(text, x, y, fontSize, fontFace, color, z);
    }

    public void centeredText(String text, Rect bounds, float fontSize, ColorRGBA color) {
        centeredText(text, bounds, fontSize, theme().fonts().regular(), color, DEFAULT_Z);
    }

    public void centeredText(String text, Rect bounds, float fontSize, String fontFace, ColorRGBA color, float z) {
        TextMetrics metrics = measureText(text, fontSize, fontFace);
        text(text, bounds.centerX() - metrics.width() * 0.5f, bounds.centerY() - metrics.height() * 0.5f, fontSize, fontFace, color, z);
    }

    public void rightAlignedText(String text, float rightX, float y, float fontSize, String fontFace, ColorRGBA color, float z) {
        TextMetrics metrics = measureText(text, fontSize, fontFace);
        text(text, rightX - metrics.width(), y, fontSize, fontFace, color, z);
    }

    public TextMetrics measureText(String text, float fontSize) {
        return requireFrame().measureText(text, fontSize);
    }

    public TextMetrics measureText(String text, float fontSize, String fontFace) {
        return requireFrame().measureText(text, fontSize, fontFace);
    }

    public boolean contains(Rect bounds, float mouseX, float mouseY) {
        return bounds != null && bounds.contains(mouseX, mouseY);
    }

    public void clip(Rect bounds, Consumer<UiRenderUtils> draw) {
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(draw, "draw");
        RenderContext ctx = requireFrame();
        ctx.pushClip(bounds);
        try {
            draw.accept(this);
        } finally {
            ctx.popClip();
        }
    }

    private TextureRef textureRef(String identifier) {
        String key = Objects.requireNonNull(identifier, "texture").trim();
        return textureRefs.computeIfAbsent(key, TextureRef::of);
    }

    private float plainWidth(String value, float fontSize, String fontFace) {
        return requireFrame().measurePlainText(safeInput(value), fontSize, fontFace).width();
    }

    private String ellipsizePlain(String value, float fontSize, String fontFace, float maxWidth) {
        String text = safeInput(value);
        if (plainWidth(text, fontSize, fontFace) <= maxWidth) return text;
        if (maxWidth <= 0.0f) return "";

        final String suffix = "...";
        if (plainWidth(suffix, fontSize, fontFace) > maxWidth) return "";

        int low = 0;
        int high = text.codePointCount(0, text.length());
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            int end = text.offsetByCodePoints(0, middle);
            String candidate = text.substring(0, end) + suffix;
            if (plainWidth(candidate, fontSize, fontFace) <= maxWidth) low = middle;
            else high = middle - 1;
        }
        return text.substring(0, text.offsetByCodePoints(0, low)) + suffix;
    }

    private static String safeInput(String value) {
        return value == null ? "" : value;
    }

    private static int clampIndex(int value, int length) {
        return Math.max(0, Math.min(length, value));
    }

    private String ellipsize(String value, float fontSize, String fontFace, float maxWidth) {
        String text = value == null ? "" : value;
        if (measureText(text, fontSize, fontFace).width() <= maxWidth) return text;
        if (maxWidth <= 0.0f) return "";

        final String suffix = "...";
        if (measureText(suffix, fontSize, fontFace).width() > maxWidth) return "";

        int low = 0;
        int high = text.codePointCount(0, text.length());
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            int end = text.offsetByCodePoints(0, middle);
            String candidate = text.substring(0, end) + suffix;
            if (measureText(candidate, fontSize, fontFace).width() <= maxWidth) low = middle;
            else high = middle - 1;
        }
        return text.substring(0, text.offsetByCodePoints(0, low)) + suffix;
    }

    private RenderContext requireFrame() {
        if (!rendering || context == null) {
            throw new IllegalStateException("Draw methods may only be called from inside UiRenderUtils.render");
        }
        return context;
    }

    private static float sanitizeScale(float value) {
        if (!Float.isFinite(value) || value <= 0.0f) return 1.0f;
        return value;
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
