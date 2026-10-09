package dev.someoneok.crystalconfigtester.config;

import dev.someoneok.crystalconfig.autoconfig.*;
import dev.someoneok.crystalconfig.components.Keybind;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.state.MutableState;

@ConfigCategory(main = "01 / Controls", sub = "Toggles and values")
@ConfigTooltip("Basic controls, different presentation modes, and editing restrictions.")
public final class BasicOptions {
    private BasicOptions() { }

    @ConfigInfo(title = "Core widget coverage", description = "Edit each value, exit, then reopen to check JSON persistence.")
    public static final ConfigMarker header = ConfigMarker.marker();

    @ConfigToggle(key = "basic.enabled", label = "Toggle", description = "Standard on/off switch")
    @ConfigTooltip("Try toggling this on and off.")
    public static final MutableState<Boolean> enabled = new MutableState<>(true);

    @ConfigCheckbox(key = "basic.checkbox", label = "Checkbox", description = "Independent boolean checkbox")
    public static final MutableState<Boolean> checked = new MutableState<>(false);

    @ConfigSeparator
    public static final ConfigMarker separator = ConfigMarker.marker();

    @ConfigSlider(key = "basic.slider", label = "Slider", min = 0.0, max = 100.0, step = 5.0)
    public static final MutableState<Double> slider = new MutableState<>(50.0);

    @ConfigSliderLabel(key = "basic.sliderLabel", label = "Slider + value label", min = 0.5, max = 3.0, step = 0.1)
    public static final MutableState<Double> labeledSlider = new MutableState<>(1.25);

    @ConfigNumber(key = "basic.number", label = "Number field", description = "Range -50 to 50; step 0.5", min = -50, max = 50, step = 0.5)
    public static final MutableState<Double> number = new MutableState<>(12.5);

    @ConfigLabeledSeparator("Text fields")
    public static final ConfigMarker textHeading = ConfigMarker.marker();

    @ConfigText(key = "basic.text", label = "Ordinary text", description = "Free-form String")
    public static final MutableState<String> text = new MutableState<>("Hello CrystalConfig!");

    @ConfigText(key = "basic.regex", label = "Regex restricted", description = "Letters, digits and underscores only", regex = "[A-Za-z0-9_]*")
    public static final MutableState<String> restrictedText = new MutableState<>("valid_name_123");

    @ConfigText(key = "basic.revealText", label = "Reveal while editing", sensitivity = ConfigTextSensitivity.VISIBLE_WHILE_EDITING)
    public static final MutableState<String> revealText = new MutableState<>("demo-not-a-secret");

    @ConfigText(key = "basic.hiddenText", label = "Always hidden", sensitivity = ConfigTextSensitivity.ALWAYS_HIDDEN)
    public static final MutableState<String> hiddenText = new MutableState<>("demo-not-a-secret");

    @ConfigSpacer(height = 12)
    public static final ConfigMarker spacer = ConfigMarker.marker();

    @ConfigColor(key = "basic.colorAlpha", label = "RGBA color", description = "Color with alpha channel", allowAlpha = true)
    public static final MutableState<ColorRGBA> alphaColor = new MutableState<>(ColorRGBA.hex("#AE456BBF"));

    @ConfigColor(key = "basic.colorOpaque", label = "RGB color", description = "Alpha disabled", allowAlpha = false)
    public static final MutableState<ColorRGBA> opaqueColor = new MutableState<>(ColorRGBA.hex("#39CABB"));

    @ConfigKeybind(key = "basic.keybind", label = "Keybind", description = "Keyboard, mouse, and None")
    public static final MutableState<Keybind> keybind = new MutableState<>(Keybind.none());

    @ConfigKeybind(key = "basic.keybindRestricted", label = "Keyboard-only keybind", disallowNone = true, allowMouseButtons = false)
    public static final MutableState<Keybind> restrictedKeybind = new MutableState<>(Keybind.glfwKey(75, "K"));
}
