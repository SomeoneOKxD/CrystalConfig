package dev.someoneok.crystalconfig.theme;

import dev.someoneok.crystalconfig.render.ColorRGBA;

public final class ThemePresets {
    private ThemePresets() { }

    private static final SpacingScale SPACING = new SpacingScale(4, 8, 12, 18, 28);
    private static final FontScale FONTS = new FontScale("regular", "medium", "semibold", 9, 11, 13, 17, 24);
    private static final AnimationSpec ANIMATION = new AnimationSpec(34, 22, 12);

    public static Theme theme(String name, Palette palette, Radii radii) {
        return new Theme(name, palette, SPACING, FONTS, radii, ANIMATION);
    }

    public static Theme theme(String name, Palette palette) {
        return theme(name, palette, new Radii(3, 6, 10, 999));
    }

    public static Theme softTheme(String name, Palette palette) {
        return theme(name, palette, new Radii(4, 8, 12, 999));
    }

    public static Theme darkCrimson() {
        return theme("Dark Crimson", new Palette(
                ColorRGBA.hex("#0B0D12"),
                ColorRGBA.hex("#131720"),
                ColorRGBA.hex("#181D28"),
                ColorRGBA.hex("#222938"),
                ColorRGBA.hex("#2C3447"),
                ColorRGBA.hex("#F3F6FB"),
                ColorRGBA.hex("#9CA7B8"),
                ColorRGBA.hex("#E5484D"),
                ColorRGBA.hex("#FFFFFF"),
                ColorRGBA.hex("#313A4D"),
                ColorRGBA.hex("#FF5A5F"),
                ColorRGBA.rgba(229, 72, 77, 48)
        ));
    }

    public static Theme midnightSteel() {
        return theme("Midnight Steel", new Palette(
                ColorRGBA.hex("#090B10"),
                ColorRGBA.hex("#11141B"),
                ColorRGBA.hex("#171B24"),
                ColorRGBA.hex("#1E2430"),
                ColorRGBA.hex("#252C3A"),
                ColorRGBA.hex("#F1F5F9"),
                ColorRGBA.hex("#99A4B5"),
                ColorRGBA.hex("#5C7CFA"),
                ColorRGBA.hex("#FFFFFF"),
                ColorRGBA.hex("#2B3242"),
                ColorRGBA.hex("#FF5A6A"),
                ColorRGBA.rgba(0, 0, 0, 130)
        ));
    }

    public static Theme oceanDepths() {
        return softTheme("Ocean Depths", new Palette(
                ColorRGBA.hex("#061016"),
                ColorRGBA.hex("#0C1A22"),
                ColorRGBA.hex("#112631"),
                ColorRGBA.hex("#183545"),
                ColorRGBA.hex("#21495D"),
                ColorRGBA.hex("#F0FBFF"),
                ColorRGBA.hex("#8EB5C4"),
                ColorRGBA.hex("#28C7FA"),
                ColorRGBA.hex("#041018"),
                ColorRGBA.hex("#244353"),
                ColorRGBA.hex("#FF5F6D"),
                ColorRGBA.rgba(0, 180, 255, 46)
        ));
    }

    public static Theme emeraldGrove() {
        return softTheme("Emerald Grove", new Palette(
                ColorRGBA.hex("#07110D"),
                ColorRGBA.hex("#0D1B15"),
                ColorRGBA.hex("#13261D"),
                ColorRGBA.hex("#1A3428"),
                ColorRGBA.hex("#244737"),
                ColorRGBA.hex("#F1FFF7"),
                ColorRGBA.hex("#9BB9A8"),
                ColorRGBA.hex("#42D392"),
                ColorRGBA.hex("#04120B"),
                ColorRGBA.hex("#2A4437"),
                ColorRGBA.hex("#FF6B6B"),
                ColorRGBA.rgba(66, 211, 146, 42)
        ));
    }

    public static Theme amethystNight() {
        return softTheme("Amethyst Night", new Palette(
                ColorRGBA.hex("#100B16"),
                ColorRGBA.hex("#181021"),
                ColorRGBA.hex("#21162E"),
                ColorRGBA.hex("#2C1F3D"),
                ColorRGBA.hex("#39294F"),
                ColorRGBA.hex("#FBF7FF"),
                ColorRGBA.hex("#B5A4C8"),
                ColorRGBA.hex("#A879FF"),
                ColorRGBA.hex("#FFFFFF"),
                ColorRGBA.hex("#3C2F4E"),
                ColorRGBA.hex("#FF5C8A"),
                ColorRGBA.rgba(168, 121, 255, 48)
        ));
    }

    public static Theme nordFrost() {
        return theme("Nord Frost", new Palette(
                ColorRGBA.hex("#0B1118"),
                ColorRGBA.hex("#111A24"),
                ColorRGBA.hex("#182432"),
                ColorRGBA.hex("#213246"),
                ColorRGBA.hex("#2B4059"),
                ColorRGBA.hex("#F3F8FF"),
                ColorRGBA.hex("#9DAEC0"),
                ColorRGBA.hex("#88C0D0"),
                ColorRGBA.hex("#071018"),
                ColorRGBA.hex("#304255"),
                ColorRGBA.hex("#BF616A"),
                ColorRGBA.rgba(136, 192, 208, 40)
        ));
    }

    public static Theme solarAmber() {
        return softTheme("Solar Amber", new Palette(
                ColorRGBA.hex("#120D07"),
                ColorRGBA.hex("#1D160C"),
                ColorRGBA.hex("#2A1F12"),
                ColorRGBA.hex("#3A2B19"),
                ColorRGBA.hex("#4C3820"),
                ColorRGBA.hex("#FFF8EA"),
                ColorRGBA.hex("#C3AA82"),
                ColorRGBA.hex("#FFB84D"),
                ColorRGBA.hex("#1A1004"),
                ColorRGBA.hex("#51402A"),
                ColorRGBA.hex("#FF6257"),
                ColorRGBA.rgba(255, 184, 77, 42)
        ));
    }

    public static Theme cyberMint() {
        return theme("Cyber Mint", new Palette(
                ColorRGBA.hex("#050D10"),
                ColorRGBA.hex("#0B171B"),
                ColorRGBA.hex("#102329"),
                ColorRGBA.hex("#17343B"),
                ColorRGBA.hex("#204852"),
                ColorRGBA.hex("#EFFFFF"),
                ColorRGBA.hex("#8FB8BE"),
                ColorRGBA.hex("#31F7C8"),
                ColorRGBA.hex("#021311"),
                ColorRGBA.hex("#28505A"),
                ColorRGBA.hex("#FF4D7D"),
                ColorRGBA.rgba(49, 247, 200, 46)
        ));
    }

    public static Theme graphiteClean() {
        return theme("Graphite Clean", new Palette(
                ColorRGBA.hex("#0D0F12"),
                ColorRGBA.hex("#15181D"),
                ColorRGBA.hex("#1D2229"),
                ColorRGBA.hex("#272D36"),
                ColorRGBA.hex("#333B46"),
                ColorRGBA.hex("#F4F6F8"),
                ColorRGBA.hex("#A3ABB5"),
                ColorRGBA.hex("#8EA4B8"),
                ColorRGBA.hex("#071018"),
                ColorRGBA.hex("#363E49"),
                ColorRGBA.hex("#FF5F57"),
                ColorRGBA.rgba(0, 0, 0, 125)
        ));
    }

    public static Theme rosePine() {
        return softTheme("Rose Pine", new Palette(
                ColorRGBA.hex("#120D12"),
                ColorRGBA.hex("#1B141B"),
                ColorRGBA.hex("#261C26"),
                ColorRGBA.hex("#332733"),
                ColorRGBA.hex("#443344"),
                ColorRGBA.hex("#FFF4FA"),
                ColorRGBA.hex("#BBA6B4"),
                ColorRGBA.hex("#EB8AB8"),
                ColorRGBA.hex("#1A0710"),
                ColorRGBA.hex("#493849"),
                ColorRGBA.hex("#FF6B6B"),
                ColorRGBA.rgba(235, 138, 184, 44)
        ));
    }

    public static Theme emberForge() {
        return theme("Ember Forge", new Palette(
                ColorRGBA.hex("#120807"),
                ColorRGBA.hex("#1B100E"),
                ColorRGBA.hex("#281815"),
                ColorRGBA.hex("#39221D"),
                ColorRGBA.hex("#4D2D25"),
                ColorRGBA.hex("#FFF4EF"),
                ColorRGBA.hex("#C5A197"),
                ColorRGBA.hex("#FF7A45"),
                ColorRGBA.hex("#1A0904"),
                ColorRGBA.hex("#51382F"),
                ColorRGBA.hex("#FF4D4F"),
                ColorRGBA.rgba(255, 122, 69, 45)
        ));
    }

    public static Theme auroraViolet() {
        return softTheme("Aurora Violet", new Palette(
                ColorRGBA.hex("#070B14"),
                ColorRGBA.hex("#101626"),
                ColorRGBA.hex("#171E34"),
                ColorRGBA.hex("#202A49"),
                ColorRGBA.hex("#2B3862"),
                ColorRGBA.hex("#F8FBFF"),
                ColorRGBA.hex("#A8B2D1"),
                ColorRGBA.hex("#7C5CFF"),
                ColorRGBA.hex("#FFFFFF"),
                ColorRGBA.hex("#354066"),
                ColorRGBA.hex("#FF5470"),
                ColorRGBA.rgba(124, 92, 255, 52)
        ));
    }

    public static Theme lagoonTeal() {
        return softTheme("Lagoon Teal", new Palette(
                ColorRGBA.hex("#061211"),
                ColorRGBA.hex("#0C1E1C"),
                ColorRGBA.hex("#122B28"),
                ColorRGBA.hex("#1A3B37"),
                ColorRGBA.hex("#24514B"),
                ColorRGBA.hex("#EFFFFB"),
                ColorRGBA.hex("#94BAB4"),
                ColorRGBA.hex("#2DD4BF"),
                ColorRGBA.hex("#031413"),
                ColorRGBA.hex("#2A4C48"),
                ColorRGBA.hex("#FB7185"),
                ColorRGBA.rgba(45, 212, 191, 42)
        ));
    }

    public static Theme royalIndigo() {
        return theme("Royal Indigo", new Palette(
                ColorRGBA.hex("#0A0B18"),
                ColorRGBA.hex("#11132A"),
                ColorRGBA.hex("#181B3A"),
                ColorRGBA.hex("#222650"),
                ColorRGBA.hex("#2D3368"),
                ColorRGBA.hex("#F5F6FF"),
                ColorRGBA.hex("#A4A9D0"),
                ColorRGBA.hex("#6D8CFF"),
                ColorRGBA.hex("#FFFFFF"),
                ColorRGBA.hex("#353B71"),
                ColorRGBA.hex("#FF5C7A"),
                ColorRGBA.rgba(109, 140, 255, 48)
        ));
    }

    public static Theme slateBlue() {
        return theme("Slate Blue", new Palette(
                ColorRGBA.hex("#0A1018"),
                ColorRGBA.hex("#121A25"),
                ColorRGBA.hex("#1A2635"),
                ColorRGBA.hex("#243347"),
                ColorRGBA.hex("#30435C"),
                ColorRGBA.hex("#F2F7FF"),
                ColorRGBA.hex("#9DAFC4"),
                ColorRGBA.hex("#4EA1FF"),
                ColorRGBA.hex("#FFFFFF"),
                ColorRGBA.hex("#33465D"),
                ColorRGBA.hex("#FF6B6B"),
                ColorRGBA.rgba(78, 161, 255, 44)
        ));
    }

    public static Theme softLight() {
        return softTheme("Soft Light", new Palette(
                ColorRGBA.hex("#EEF2F7"),
                ColorRGBA.hex("#F8FAFC"),
                ColorRGBA.hex("#FFFFFF"),
                ColorRGBA.hex("#E9EEF6"),
                ColorRGBA.hex("#DDE6F1"),
                ColorRGBA.hex("#172033"),
                ColorRGBA.hex("#64748B"),
                ColorRGBA.hex("#3B82F6"),
                ColorRGBA.hex("#FFFFFF"),
                ColorRGBA.hex("#CBD5E1"),
                ColorRGBA.hex("#E11D48"),
                ColorRGBA.rgba(15, 23, 42, 38)
        ));
    }
}
