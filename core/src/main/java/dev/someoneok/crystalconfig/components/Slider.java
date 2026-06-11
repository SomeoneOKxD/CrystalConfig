package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.input.MouseButton;
import dev.someoneok.crystalconfig.input.MouseButtonEvent;
import dev.someoneok.crystalconfig.input.MouseDragEvent;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.render.TextMetrics;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;
import dev.someoneok.crystalconfig.utils.MathUtil;

import java.util.Locale;
import java.util.function.DoubleFunction;

public class Slider<N extends Number> extends Component {
    private static final float STACK_WIDTH = 132;
    private static final float INPUT_GAP = 10;
    private static final float INPUT_WIDTH = 62;

    private final State<N> value;
    private final Class<?> numberType;
    private final double min;
    private final double max;
    private final double step;
    private final NumberInput<N> input;
    private boolean dragging;
    private boolean inputVisible = true;
    private DoubleFunction<String> valueFormatter = value -> String.format(Locale.ROOT, "%s", trim(value));

    public Slider(State<N> value, double min, double max, double step) {
        this(value, inferType(value), min, max, step);
    }

    public Slider(State<N> value, Class<?> numberType, double min, double max, double step) {
        if (value == null) throw new NullPointerException("value");
        this.value = value;
        this.numberType = numberType == null ? inferType(value) : numberType;
        this.min = min;
        this.max = max;
        this.step = sanitizeStep(step, this.numberType);
        this.input = new NumberInput<>(value, this.numberType, min, max, this.step).width(INPUT_WIDTH);
        this.add(input);
        this.focusable = true;
        this.height(30);
        this.minSize(72, 30);
    }

    public Slider<N> showInput(boolean showInput) {
        this.inputVisible = showInput;
        this.input.visible(showInput).enabled(showInput);
        return this;
    }

    public Slider<N> valueLabel() {
        return showInput(false);
    }

    public Slider<N> valueFormatter(DoubleFunction<String> formatter) {
        this.valueFormatter = formatter == null ? value -> String.format(Locale.ROOT, "%s", trim(value)) : formatter;
        return this;
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float desiredWidth = preferredWidth >= 0 ? preferredWidth : 240;
        float width = Math.min(desiredWidth, constraints.maxWidth());
        float height = stacked(width) ? 58 : 30;
        return constraints.clamp(new Size(Math.max(1, width), height));
    }

    @Override
    protected void layoutChildren(LayoutContext context) {
        if (!inputVisible) {
            input.layout(context, Rect.ZERO);
            return;
        }
        if (stacked(bounds.w())) {
            float inputW = Math.min(INPUT_WIDTH, Math.max(44, bounds.w()));
            input.layout(context, new Rect(bounds.x() + bounds.w() - inputW, bounds.y(), inputW, 28));
        } else {
            float inputW = Math.min(INPUT_WIDTH, Math.max(44, bounds.w() * 0.42f));
            input.layout(context, new Rect(bounds.right() - inputW, bounds.y() + 1, inputW, bounds.h() - 2));
        }
    }

    @Override
    protected void renderSelf(RenderContext context) {
        Rect track = trackRect();
        float t = normalized();
        float radius = context.theme().radii().sm();
        boolean disabled = !enabled();
        context.rect(track, SdfRectStyle.create()
                .fill(disabled ? context.theme().palette().surfaceAlt().withAlpha(120) : context.theme().palette().surfaceAlt())
                .radius(radius), z);
        context.rect(new Rect(track.x(), track.y(), Math.max(2, track.w() * t), track.h()), SdfRectStyle.create()
                .fill(disabled ? context.theme().palette().mutedText().withAlpha(135) : context.theme().palette().accent())
                .radius(radius), z + 1);

        float knobSize = 10;
        Rect knob = new Rect(track.x() + track.w() * t - knobSize * 0.5f, track.centerY() - knobSize * 0.5f, knobSize, knobSize);
        if (!disabled && (focused || hovered || dragging)) {
            float halo = dragging ? 6 : 4;
            context.rect(new Rect(knob.x() - halo * 0.5f, knob.y() - halo * 0.5f, knob.w() + halo, knob.h() + halo), SdfRectStyle.create()
                    .fill(context.theme().palette().accent().withAlpha(dragging ? 60 : 40))
                    .radius((knob.w() + halo) * 0.5f), z + 2);
        }
        context.rect(knob, SdfRectStyle.create()
                .fill(disabled ? context.theme().palette().mutedText().withAlpha(150) : context.theme().palette().accent())
                .radius(knobSize * 0.5f), z + 3);

        if (!inputVisible) {
            Rect labelRect = valueSlotRect();
            String shown = valueFormatter.apply(valueAsDouble(value.get()));
            float font = context.theme().fonts().small();
            TextMetrics metrics = context.measureText(shown, font);
            float tx = labelRect.centerX() - metrics.width() * 0.5f;
            float ty = labelRect.centerY() - metrics.height() * 0.5f;
            context.text(shown, tx, ty, font, disabled ? context.theme().palette().mutedText().withAlpha(120) : context.theme().palette().mutedText(), z + 4);
        }
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (!enabled() || event.button != MouseButton.LEFT) return false;
        if (trackHitArea().contains(event.x, event.y)) {
            dragging = true;
            updateFromMouse(event.x);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDragged(MouseDragEvent event) {
        if (!enabled() || !dragging) return false;
        updateFromMouse(event.x);
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    private boolean stacked(float width) {
        return width < STACK_WIDTH;
    }

    private Rect trackRect() {
        if (stacked(bounds.w())) {
            return new Rect(bounds.x(), bounds.y() + 41, Math.max(1, bounds.w()), 5);
        }
        float inputW = Math.min(INPUT_WIDTH, Math.max(44, bounds.w() * 0.42f));
        float rightPadding = inputW + INPUT_GAP;
        return new Rect(bounds.x(), bounds.centerY() - 2.5f, Math.max(1, bounds.w() - rightPadding), 5);
    }

    private Rect valueSlotRect() {
        if (stacked(bounds.w())) {
            float w = Math.min(INPUT_WIDTH, Math.max(44, bounds.w()));
            return new Rect(bounds.x() + bounds.w() - w, bounds.y(), w, 28);
        }
        float w = Math.min(INPUT_WIDTH, Math.max(44, bounds.w() * 0.42f));
        return new Rect(bounds.right() - w, bounds.y() + 1, w, bounds.h() - 2);
    }

    private Rect trackHitArea() {
        Rect t = trackRect();
        return new Rect(t.x(), stacked(bounds.w()) ? bounds.y() + 32 : bounds.y(), t.w(), stacked(bounds.w()) ? 22 : bounds.h());
    }

    private float normalized() {
        return (float)((MathUtil.clamp(valueAsDouble(value.get()), min, max) - min) / Math.max(0.000001, max - min));
    }

    private static String trim(double value) {
        if (!Double.isFinite(value)) return String.valueOf(value);
        String text = String.format(Locale.ROOT, "%.6f", value);
        while (text.contains(".") && text.endsWith("0")) text = text.substring(0, text.length() - 1);
        if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
        return text;
    }

    private double valueAsDouble(Number number) {
        return number == null ? 0.0 : number.doubleValue();
    }

    @SuppressWarnings("unchecked")
    private N convert(double value) {
        if (numberType == Integer.class || numberType == int.class) {
            return (N) Integer.valueOf((int) Math.round(MathUtil.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE)));
        }
        if (numberType == Long.class || numberType == long.class) {
            return (N) Long.valueOf(Math.round(value));
        }
        if (numberType == Float.class || numberType == float.class) {
            return (N) Float.valueOf((float) value);
        }
        return (N) Double.valueOf(value);
    }

    private static double sanitizeStep(double step, Class<?> numberType) {
        if (!Double.isFinite(step) || step <= 0) return 1.0;
        if (numberType == Integer.class || numberType == int.class || numberType == Long.class || numberType == long.class) {
            return Math.max(1.0, Math.rint(step));
        }
        return step;
    }

    private static Class<?> inferType(State<? extends Number> state) {
        Number value = state == null ? null : state.get();
        return value == null ? Double.class : value.getClass();
    }

    private void updateFromMouse(float mouseX) {
        Rect track = trackRect();
        float t = MathUtil.clamp((mouseX - track.x()) / Math.max(1, track.w()), 0, 1);
        double next = min + (max - min) * t;
        next = MathUtil.snap(next, step);
        value.set(convert(MathUtil.clamp(next, min, max)));
    }
}
