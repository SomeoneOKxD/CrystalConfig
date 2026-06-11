package dev.someoneok.crystalconfig.theme;

public enum PresetTheme {
    DARK_CRIMSON(ThemePresets.darkCrimson()),
    EMBER_FORGE(ThemePresets.emberForge()),
    SOLAR_AMBER(ThemePresets.solarAmber()),
    EMERALD_GROVE(ThemePresets.emeraldGrove()),
    CYBER_MINT(ThemePresets.cyberMint()),
    LAGOON_TEAL(ThemePresets.lagoonTeal()),
    OCEAN_DEPTHS(ThemePresets.oceanDepths()),
    NORD_FROST(ThemePresets.nordFrost()),
    GRAPHITE_CLEAN(ThemePresets.graphiteClean()),
    SLATE_BLUE(ThemePresets.slateBlue()),
    MIDNIGHT_STEEL(ThemePresets.midnightSteel()),
    ROYAL_INDIGO(ThemePresets.royalIndigo()),
    AURORA_VIOLET(ThemePresets.auroraViolet()),
    AMETHYST_NIGHT(ThemePresets.amethystNight()),
    ROSE_PINE(ThemePresets.rosePine()),
    SOFT_LIGHT(ThemePresets.softLight());

    private final Theme theme;

    PresetTheme(Theme theme) {
        this.theme = theme;
    }

    public Theme theme() {
        return theme;
    }

    @Override
    public String toString() {
        return theme.name();
    }
}
