package dev.someoneok.crystalconfigtester.config;

import dev.someoneok.crystalconfig.autoconfig.*;
import dev.someoneok.crystalconfig.icons.MediaBrandIcons;
import dev.someoneok.crystalconfig.state.MutableState;

import java.util.function.BooleanSupplier;

@ConfigCategory(main = "02 / Advanced", sub = "Visibility and actions")
public final class ConditionalOptions {
    private ConditionalOptions() { }

    @ConfigToggle(key = "conditions.locked", label = "Lock special button", description = "Shows disabledWhen behavior")
    public static final MutableState<Boolean> locked = new MutableState<>(false);

    @ConfigToggle(key = "conditions.hideButton", label = "Hide special button", description = "Shows hiddenWhen behavior")
    public static final MutableState<Boolean> hidden = new MutableState<>(false);

    @ConfigToggle(key = "conditions.hideCategory", label = "Hide extra category", description = "Show or hide the Conditional Category tab")
    public static final MutableState<Boolean> hideCategory = new MutableState<>(false);

    @ConfigToggle(key = "conditions.disableCategory", label = "Disable extra category", description = "Keeps it visible but blocks navigation")
    public static final MutableState<Boolean> disableCategory = new MutableState<>(false);

    public static final BooleanSupplier buttonLocked = () -> locked.get();
    public static final BooleanSupplier buttonHidden = () -> hidden.get();

    @ConfigInfo(title = "Live annotation conditions", hiddenWhen = "buttonHidden", disabledWhen = "buttonLocked",
            description = "This info row follows the same hide/disable switches as the action button")
    public static final ConfigMarker liveInfo = ConfigMarker.marker()
            .title(() -> "State: " + (locked.get() ? "locked" : "unlocked"))
            .description(() -> "Button hidden: " + hidden.get() + " | Category hidden: " + hideCategory.get())
            .tooltip(() -> "Dynamic tooltips and titles are supported too.");

    @ConfigButton(label = "Conditional action", buttonText = "Click me", description = "Try enabling Lock and Hide above", disabledWhen = "buttonLocked", hiddenWhen = "buttonHidden")
    public static final Runnable conditionalAction = () -> CustomOptions.clicks.set(CustomOptions.clicks.get() + 1);

    @ConfigAccordion(value = "Advanced controls", description = "Expand to see additional settings and a separator")
    public static final Class<Accordion> accordion = Accordion.class;

    public static final class Accordion {
        private Accordion() { }
        @ConfigToggle(key = "conditions.accordion", label = "Accordion toggle")
        public static final MutableState<Boolean> toggle = new MutableState<>(true);

        @ConfigLabeledSeparator("Inside accordion")
        public static final ConfigMarker separator = ConfigMarker.marker();

        @ConfigText(key = "conditions.accordionText", label = "Nested text")
        public static final MutableState<String> text = new MutableState<>("Nested content");
    }

    @ConfigFooterIcon(icon = MediaBrandIcons.GITHUB, action = ConfigFooterIconAction.OPEN_URL,
            value = "https://github.com/SomeoneOKxD/CrystalConfig", tooltip = "CrystalConfig GitHub")
    public static final ConfigMarker github = ConfigMarker.marker();

    @ConfigFooterIcon(icon = MediaBrandIcons.DISCORD, action = ConfigFooterIconAction.COPY_TO_CLIPBOARD,
            value = "CrystalConfig tester", tooltip = "Copy demo text")
    public static final ConfigMarker copy = ConfigMarker.marker();

    @ConfigFooterIcon(icon = MediaBrandIcons.MODRINTH, action = ConfigFooterIconAction.RUNNABLE,
            tooltip = "Increment click counter")
    public static final Runnable actionIcon = () -> CustomOptions.clicks.set(CustomOptions.clicks.get() + 1);

    @ConfigFooterIcon(icon = MediaBrandIcons.YOUTUBE, action = ConfigFooterIconAction.NONE,
            tooltip = "Intentionally inert action variant")
    public static final ConfigMarker noActionIcon = ConfigMarker.marker();

    @ConfigFooterButton("Open renderer showcase")
    public static final Runnable renderButton = dev.someoneok.crystalconfigtester.ConfigTesterClient::openRenderer;
}
