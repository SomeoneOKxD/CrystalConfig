# Source TTF files

Put the raw TTF files for MSDF generation in this folder:

```text
regular.ttf
medium.ttf
semibold.ttf
fallback-symbols.ttf
media-brands.ttf
```

Recommended mapping:

```text
GoogleSans-Regular.ttf        -> regular.ttf
GoogleSans-Medium.ttf         -> medium.ttf
GoogleSans-SemiBold.ttf       -> semibold.ttf
NotoSansSymbols2-Regular.ttf  -> fallback-symbols.ttf
media-brands.ttf
```

`media-brands.ttf` is optional at runtime but required for `generateMsdfMediaBrandsFont` and `generateMsdfFonts`. Use Font Awesome Brands for the default media icon constants. Latin text is covered by the regular, medium, and semibold faces; no separate Latin fallback atlas is generated.

The generated PNG/JSON files are used at runtime. These TTF files are build-time inputs only and are excluded from `processResources`, so they will not be bundled in the final mod jar.

The generator executable must be placed at project root:

```text
tools/msdf-atlas-gen/msdf-atlas-gen.exe
```

Then run the IntelliJ Gradle task:

```text
:crystal-config:generateMsdfFonts
```
