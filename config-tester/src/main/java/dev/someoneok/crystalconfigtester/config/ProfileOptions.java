package dev.someoneok.crystalconfigtester.config;

import dev.someoneok.crystalconfig.autoconfig.*;
import dev.someoneok.crystalconfig.config.ProfileConfig;
import dev.someoneok.crystalconfig.state.MutableState;

@ConfigCategory(main = "02 / Advanced", sub = "Profiles")
public final class ProfileOptions {
    private ProfileOptions() { }

    @ConfigInfo(title = "Profile persistence", description = "Create, rename, switch or remove a profile. The next two fields are linked; the final toggle is global.")
    public static final ConfigMarker info = ConfigMarker.marker();

    @ConfigToggle(key = "profiles.enabled", label = "Profile-specific enabled")
    public static final MutableState<Boolean> enabled = new MutableState<>(true);

    @ConfigSlider(key = "profiles.scale", label = "Profile-specific scale", min = 0.25, max = 2.0, step = 0.05)
    public static final MutableState<Double> scale = new MutableState<>(1.0);

    @ConfigProfile(key = "profiles.sets", label = "Named profile", description = "Tests profile-linked settings and stable IDs")
    public static final ProfileConfig profiles = ProfileConfig.create("Default")
            .link("enabled", enabled, Boolean.class)
            .link("scale", scale, Double.class);

    @ConfigToggle(key = "profiles.global", label = "Global setting", description = "Does not change when switching profiles")
    public static final MutableState<Boolean> global = new MutableState<>(true);
}
