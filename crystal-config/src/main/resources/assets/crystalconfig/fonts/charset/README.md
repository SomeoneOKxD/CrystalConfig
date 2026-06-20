# MSDF charset files

`text.charset` is used for the main faces:

```text
regular.ttf
medium.ttf
semibold.ttf
```

It keeps the atlas small by only generating Basic Latin, Latin-1 Supplement, common typography punctuation, ellipsis, and the replacement glyph.

`symbols.charset` is used only for:

```text
fallback-symbols.ttf
```

It contains the UI symbols CrystalConfig actually needs, such as arrows, triangles, checkmarks, warning, gear/settings, plus/minus, and the replacement glyph.

`media-brands.charset` is used only for:

```text
media-brands.ttf
```

It is intended for a dedicated brand icon font, preferably Font Awesome Brands. It includes common creator/modding/social platforms such as Discord, Patreon, GitHub, Twitter/X, YouTube, Twitch, Reddit, Steam, PayPal, and a few CrystalConfig-reserved custom Private Use Area slots for icons Font Awesome Brands does not ship by default.

The reserved custom slots are:

```text
0xE001 Ko-fi
0xE002 Modrinth
0xE003 CurseForge
0xE004 Website/link
0xE005 Support/donation
```

Those custom slots only render if your chosen `media-brands.ttf` actually maps glyphs to those codepoints.

The settings cog uses Unicode `U+2699` (`⚙`). If you change `fallback-symbols.ttf`, make sure the font contains this glyph before regenerating the atlas.

Discord, Patreon, GitHub, Twitter/X, and similar icons should go in `media-brands.charset`, not `symbols.charset`. Keep `symbols.charset` for normal Unicode UI symbols only.
