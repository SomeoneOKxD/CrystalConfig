package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.input.CharTypedEvent;
import dev.someoneok.crystalconfig.input.KeyCodes;
import dev.someoneok.crystalconfig.input.KeyEvent;
import dev.someoneok.crystalconfig.state.MutableState;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.utils.MathUtil;

import java.util.Locale;

public class NumberInput<N extends Number> extends TextInput {
    private enum NumberKind { DOUBLE, FLOAT, INTEGER, LONG }

    private final State<N> number;
    private final NumberKind kind;
    private final double min;
    private final double max;
    private final double step;

    public NumberInput(State<N> number, double min, double max, double step) {
        this(number, inferType(number), min, max, step);
    }

    public NumberInput(State<N> number, Class<?> numberType, double min, double max, double step) {
        super(new MutableState<>(format(valueAsDouble(number == null ? null : number.get()), kindOf(numberType, number == null ? null : number.get()))));
        if (number == null) throw new NullPointerException("number");
        this.number = number;
        this.kind = kindOf(numberType, number.get());
        this.min = min;
        this.max = max;
        this.step = sanitizeStep(step, this.kind);
        this.width(64);
        this.validator(this::isValidNumber);
        this.onCommit(this::commitNumber);
    }

    @Override
    public void tick(float deltaSeconds) {
        if (!focused) {
            String formatted = format(currentDouble(), kind);
            if (!formatted.equals(value())) {
                setText(formatted);
                cursor = formatted.length();
                selectionAnchor = cursor;
            }
        }
        super.tick(deltaSeconds);
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled()) return false;
        if (event.keyCode == KeyCodes.UP) {
            setNumber(currentDouble() + step);
            return true;
        }
        if (event.keyCode == KeyCodes.DOWN) {
            setNumber(currentDouble() - step);
            return true;
        }
        return super.onKeyPressed(event);
    }

    @Override
    public boolean onCharTyped(CharTypedEvent event) {
        if (!enabled()) return false;
        return super.onCharTyped(event);
    }

    private boolean isValidNumber(String text) {
        try {
            double value = parse(text);
            return value >= min && value <= max && Double.isFinite(value);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    protected void commit() {
        commitNumber(value());
    }

    private void commitNumber(String text) {
        try {
            setNumber(parse(text));
        } catch (NumberFormatException ignored) {
            setText(format(currentDouble(), kind));
            cursor = value().length();
            selectionAnchor = cursor;
        }
    }

    private void setNumber(double value) {
        double snapped = MathUtil.snap(MathUtil.clamp(value, min, max), step);
        N converted = convert(snapped);
        number.set(converted);
        String formatted = format(valueAsDouble(converted), kind);
        setText(formatted);
        cursor = value().length();
        selectionAnchor = cursor;
    }

    private double parse(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty() || normalized.equals("-") || normalized.equals("+") || normalized.equals(".")) {
            throw new NumberFormatException("Empty number");
        }
        return switch (kind) {
            case INTEGER, LONG -> {
                if (normalized.contains(".") || normalized.contains("e") || normalized.contains("E")) {
                    throw new NumberFormatException("Integer number expected");
                }
                yield Long.parseLong(normalized);
            }
            case FLOAT, DOUBLE -> Double.parseDouble(normalized);
        };
    }

    @SuppressWarnings("unchecked")
    private N convert(double value) {
        return switch (kind) {
            case INTEGER -> (N) Integer.valueOf((int) Math.round(MathUtil.clamp(value, Integer.MIN_VALUE, Integer.MAX_VALUE)));
            case LONG -> (N) Long.valueOf(Math.round(value));
            case FLOAT -> (N) Float.valueOf((float) value);
            case DOUBLE -> (N) Double.valueOf(value);
        };
    }

    private double currentDouble() {
        return valueAsDouble(number.get());
    }

    private static double valueAsDouble(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static double sanitizeStep(double step, NumberKind kind) {
        if (!Double.isFinite(step) || step <= 0) return 1.0;
        if (kind == NumberKind.INTEGER || kind == NumberKind.LONG) return Math.max(1.0, Math.rint(step));
        return step;
    }

    private static Class<?> inferType(State<? extends Number> state) {
        Number value = state == null ? null : state.get();
        return value == null ? Double.class : value.getClass();
    }

    private static NumberKind kindOf(Class<?> numberType, Number currentValue) {
        Class<?> type = numberType == null && currentValue != null ? currentValue.getClass() : numberType;
        if (type == Integer.class || type == int.class) return NumberKind.INTEGER;
        if (type == Long.class || type == long.class) return NumberKind.LONG;
        if (type == Float.class || type == float.class) return NumberKind.FLOAT;
        return NumberKind.DOUBLE;
    }

    public static String format(double value) {
        return format(value, NumberKind.DOUBLE);
    }

    private static String format(double value, NumberKind kind) {
        if (!Double.isFinite(value)) return "0";
        return switch (kind) {
            case INTEGER, LONG -> Long.toString(Math.round(value));
            case FLOAT -> trim(String.format(Locale.ROOT, "%.6f", value));
            case DOUBLE -> trim(String.format(Locale.ROOT, "%.4f", value));
        };
    }

    private static String trim(String s) {
        while (s.contains(".") && s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
