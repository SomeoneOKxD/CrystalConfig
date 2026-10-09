package dev.someoneok.crystalconfigtester.config;

import dev.someoneok.crystalconfig.autoconfig.*;
import dev.someoneok.crystalconfig.state.MutableState;

import java.util.function.BooleanSupplier;

@ConfigCategory(main = "02 / Advanced", sub = "Conditional Category", hiddenWhen = "isHidden", disabledWhen = "isDisabled")
public final class ConditionalCategoryOptions {
    private ConditionalCategoryOptions() { }

    public static final BooleanSupplier isHidden = () -> ConditionalOptions.hideCategory.get();
    public static final BooleanSupplier isDisabled = () -> ConditionalOptions.disableCategory.get();

    @ConfigInfo(title = "Condition-based navigation", description = "The entire subcategory can be hidden from the sidebar by a separate setting.")
    public static final ConfigMarker info = ConfigMarker.marker();

    @ConfigCheckbox(key = "conditions.visibleCategoryFlag", label = "Visible category checkbox")
    public static final MutableState<Boolean> flag = new MutableState<>(true);

    @ConfigSpacer(height = 14)
    public static final ConfigMarker spacer = ConfigMarker.marker();
}
