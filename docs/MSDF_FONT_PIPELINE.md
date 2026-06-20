# CrystalConfig MSDF font pipeline

CrystalConfig keeps its custom `MsdfTextRenderer`. Font replacement is handled at build/dev time by generating MSDF PNG/JSON atlases from raw TTF files.

## Required local tool

Place the Windows executable here:

```text
tools/msdf-atlas-gen/msdf-atlas-gen.exe
```

The Gradle task uses this exact project-local file. It does not use PATH, CMD, environment variables, or IntelliJ terminal configuration.

## Source fonts

Place raw TTF files here:

```text
assets/crystalconfig/fonts/source/regular.ttf
assets/crystalconfig/fonts/source/medium.ttf
assets/crystalconfig/fonts/source/semibold.ttf
assets/crystalconfig/fonts/source/fallback-symbols.ttf
assets/crystalconfig/fonts/source/media-brands.ttf
```

Recommended mapping:

```text
GoogleSans-Regular.ttf              -> regular.ttf
GoogleSans-Medium.ttf               -> medium.ttf
GoogleSans-SemiBold.ttf             -> semibold.ttf
NotoSansSymbols2-Regular.ttf        -> fallback-symbols.ttf
FontAwesomeBrands-Regular.ttf       -> media-brands.ttf
```

`media-brands.ttf` is the dedicated social/media icon font. Use Font Awesome Brands for the built-in icon constants. Do not put brand icons into the normal text or fallback-symbols font.

## Generate atlases

Run from IntelliJ Gradle:

```text
:crystal-config:generateMsdfFonts
```

This generates:

```text
crystal-config/src/main/resources/assets/crystalconfig/msdf/*.json
crystal-config/src/main/resources/assets/crystalconfig/textures/msdf/*.png
```

You can also generate one face at a time:

```text
:crystal-config:generateMsdfRegularFont
:crystal-config:generateMsdfMediumFont
:crystal-config:generateMsdfSemiboldFont
:crystal-config:generateMsdfFallbackSymbolsFont
:crystal-config:generateMsdfMediaBrandsFont
```

## Charset files

The charset files are already in the exact `msdf-atlas-gen -charset` syntax:

```text
assets/crystalconfig/fonts/charset/text.charset
assets/crystalconfig/fonts/charset/symbols.charset
assets/crystalconfig/fonts/charset/media-brands.charset
```

Do not write comments or `U+XXXX` lines in these files. `msdf-atlas-gen` rejects that. Use only entries like:

```text
[0x0020, 0x007E]
0xFFFD
```

## UI symbol glyphs

The fallback symbols atlas is generated from:

```text
assets/crystalconfig/fonts/charset/symbols.charset
```

The config settings button uses the gear glyph `⚙` (`U+2699`), so `symbols.charset` includes:

```text
0x2699
```

After changing the charset, run:

```text
:crystal-config:generateMsdfFallbackSymbolsFont
```

## Media / brand icon glyphs

The brand icon atlas is generated from:

```text
assets/crystalconfig/fonts/charset/media-brands.charset
```

Use this source font name:

```text
assets/crystalconfig/fonts/source/media-brands.ttf
```

The intended default source is Font Awesome Brands. The built-in Java constants are in:

```text
icons.someoneok.crystalconfig.MediaBrandIcons
```

Example usage from rendering code:

```java
context.text(
        MediaBrandIcons.DISCORD,
        x,
        y,
        13.0f,
        MediaBrandIcons.FONT_FACE,
        theme.palette().text(),
        z
);
```

Common built-in constants include:

```text
DISCORD
PATREON
GITHUB
TWITTER
X_TWITTER
YOUTUBE
TWITCH
REDDIT
MASTODON
STEAM
PAYPAL
TELEGRAM
WHATSAPP
INSTAGRAM
FACEBOOK
LINKEDIN
GOOGLE_PLAY
ITCH_IO
```

Some Minecraft/modding creator platforms are not part of Font Awesome Brands by default. CrystalConfig reserves custom Private Use Area slots for them:

```text
KOFI       -> 0xE001
MODRINTH   -> 0xE002
CURSEFORGE -> 0xE003
WEBSITE    -> 0xE004
SUPPORT    -> 0xE005
```

These custom icons only render if your chosen `media-brands.ttf` maps glyphs to those slots. If you use unmodified Font Awesome Brands, the standard Font Awesome icons render, while the reserved custom icons will fall back/miss.

## Runtime fallback chain

`MsdfTextRenderer` loads these generated atlases:

1. `regular`
2. `medium`
3. `semibold`
4. `fallback-symbols`
5. `media-brands` if the generated assets exist

When a glyph is missing from the selected face, the renderer checks:

1. selected/preferred face
2. regular
3. medium
4. semibold
5. media-brands, if present
6. fallback-symbols
7. replacement glyph

No Minecraft text rendering fallback is used.

## Production jar

The raw TTF files are only build inputs. The runtime uses only generated PNG/JSON MSDF assets. `processResources` excludes source TTF files from the final mod jar.
