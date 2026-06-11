package dev.someoneok.crystalconfig.autoconfig;

import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.state.StateConditions;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Placeholder type for annotation-only rows such as info, separator, spacer, and button rows. */
public final class ConfigMarker implements StateConditions {
    private BooleanSupplier hiddenWhen = () -> false;
    private BooleanSupplier disabledWhen = () -> false;
    private Supplier<String> infoTitleSupplier;
    private Supplier<String> infoDescriptionSupplier;
    private Supplier<String> infoTooltipSupplier;

    private ConfigMarker() {}

    public static ConfigMarker marker() { return new ConfigMarker(); }

    public ConfigMarker hiddenWhen(BooleanSupplier predicate) {
        this.hiddenWhen = predicate == null ? () -> false : predicate;
        return this;
    }

    public ConfigMarker hiddenWhen(State<Boolean> state) {
        return hiddenWhen(() -> Boolean.TRUE.equals(state.get()));
    }

    public ConfigMarker disabledWhen(BooleanSupplier predicate) {
        this.disabledWhen = predicate == null ? () -> false : predicate;
        return this;
    }

    public ConfigMarker disabledWhen(State<Boolean> state) {
        return disabledWhen(() -> Boolean.TRUE.equals(state.get()));
    }

    /**
     * Provide a dynamic title for @ConfigInfo rows backed by this marker.
     * The supplier is cached by the rendered info row and only triggers layout/render
     * updates when the returned value changes.
     */
    public ConfigMarker title(Supplier<String> supplier) {
        this.infoTitleSupplier = supplier;
        return this;
    }

    /** Provide a dynamic description for @ConfigInfo rows backed by this marker. */
    public ConfigMarker description(Supplier<String> supplier) {
        this.infoDescriptionSupplier = supplier;
        return this;
    }

    /** Provide a dynamic tooltip for @ConfigInfo rows backed by this marker. */
    public ConfigMarker tooltip(Supplier<String> supplier) {
        this.infoTooltipSupplier = supplier;
        return this;
    }

    /** Convenience method for dynamic info title + description. */
    public ConfigMarker info(Supplier<String> titleSupplier, Supplier<String> descriptionSupplier) {
        this.infoTitleSupplier = titleSupplier;
        this.infoDescriptionSupplier = descriptionSupplier;
        return this;
    }

    /** Convenience method for dynamic info title + description + tooltip. */
    public ConfigMarker info(Supplier<String> titleSupplier, Supplier<String> descriptionSupplier, Supplier<String> tooltipSupplier) {
        this.infoTitleSupplier = titleSupplier;
        this.infoDescriptionSupplier = descriptionSupplier;
        this.infoTooltipSupplier = tooltipSupplier;
        return this;
    }

    public Supplier<String> infoTitleSupplier() {
        return infoTitleSupplier;
    }

    public Supplier<String> infoDescriptionSupplier() {
        return infoDescriptionSupplier;
    }

    public Supplier<String> infoTooltipSupplier() {
        return infoTooltipSupplier;
    }

    @Override
    public BooleanSupplier hiddenWhen() {
        return hiddenWhen;
    }

    @Override
    public BooleanSupplier disabledWhen() {
        return disabledWhen;
    }
}
