---
layout: default
title: User-created profiles
description: Add named profile selectors and explicitly bind profile-specific settings.
---

# User-created profiles

`ProfileConfig` adds a dropdown-style selector where users can create, rename, select, and delete profiles. Only settings explicitly linked by the developer change when the selected profile changes; all other settings remain global.

## AutoConfig example

```java
@ConfigCategory(main = "Gameplay", sub = "Profiles")
public final class GameplayConfig {
    @ConfigToggle(key = "enabled", label = "Enabled")
    public static final MutableState<Boolean> enabled = new MutableState<>(true);

    @ConfigSlider(key = "scale", label = "Scale", min = 0.5, max = 2.0, step = 0.05)
    public static final MutableState<Double> scale = new MutableState<>(1.0);

    @ConfigToggle(key = "showHints", label = "Show hints")
    public static final MutableState<Boolean> showHints = new MutableState<>(true);

    @ConfigProfile(key = "profiles", label = "Profile")
    public static final ProfileConfig profiles = ProfileConfig.create("Default")
            .link("enabled", enabled, Boolean.class)
            .link("scale", scale, Double.class);
}
```

`enabled` and `scale` are stored per profile. `showHints` is not linked and stays global. AutoConfig automatically excludes linked states from their normal global persistence keys.

For generic setting types, pass a Gson `TypeToken<T>`:

```java
.link("modules", modules, new TypeToken<List<Module>>() {})
```

## Manual screen example

```java
ProfileConfig profiles = ProfileConfig.create("Default")
        .link("enabled", enabled, Boolean.class);

profiles.register(store, "gameplay.profiles");

section.profile("Profile", profiles, "Create or select a profile.");
section.toggle("Enabled", enabled, "Stored in the selected profile.");
```

Do not separately register a linked state in a manual setup.

## Safe names and stable IDs

The visible name is display-only. CrystalConfig generates a separate backend ID for every profile, so punctuation, Unicode, duplicate names, or later renaming cannot change JSON paths. Input is Unicode-normalized, control and bidirectional formatting characters are removed, whitespace is collapsed, and names are capped at 64 code points. Developer-provided linked-setting keys are restricted to letters, numbers, `.`, `_`, and `-`.

Deleting the last remaining profile is prevented. Creating a profile clones the currently selected profile, giving the user a useful starting point.
