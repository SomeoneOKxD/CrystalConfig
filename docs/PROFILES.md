# User-created configuration profiles

`ProfileConfig` provides a dropdown-style selector whose entries can be created, renamed, selected, and deleted by the user. Developers explicitly link the settings that belong to the profile; every unlinked setting remains global.

## AutoConfig

Declare the settings first, then create and annotate the profile selector:

```java
@ConfigCategory(main = "Gameplay", sub = "Profiles")
public final class GameplayConfig {
    @ConfigToggle(key = "enabled", label = "Enabled")
    public static final MutableState<Boolean> enabled = new MutableState<>(true);

    @ConfigSlider(key = "scale", label = "Scale", min = 0.5, max = 2.0, step = 0.05)
    public static final MutableState<Double> scale = new MutableState<>(1.0);

    // This setting is not linked, so it remains global across every profile.
    @ConfigToggle(key = "showHints", label = "Show hints")
    public static final MutableState<Boolean> showHints = new MutableState<>(true);

    @ConfigProfile(
            key = "profiles",
            label = "Profile",
            description = "Create and switch between named gameplay profiles."
    )
    public static final ProfileConfig profiles = ProfileConfig.create("Default")
            .link("enabled", enabled, Boolean.class)
            .link("scale", scale, Double.class);
}
```

`AutoConfig` detects linked states and does not register their normal global keys. Their values are stored only inside the `profiles` document. A `State<?>` can belong to only one `ProfileConfig`.

Use `TypeToken<T>` for generic values:

```java
ProfileConfig.create()
        .link("enabledModules", enabledModules, new TypeToken<List<Module>>() {});
```

## Manual screens

Register the profile document with the same `GsonConfigStore` used by the rest of the screen, and do not separately register the linked states:

```java
ProfileConfig profiles = ProfileConfig.create("Default")
        .link("enabled", enabled, Boolean.class)
        .link("scale", scale, Double.class);

profiles.register(store, "gameplay.profiles");
store.register("gameplay.showHints", showHints, Boolean.class);

Component screen = ConfigScreenBuilder.create("My Config", settings)
        .section("Gameplay", section -> section
                .profile("Profile", profiles, "Create or select a profile.")
                .toggle("Enabled", enabled, "Profile-specific.")
                .slider("Scale", scale, 0.5, 2.0, 0.05, "Profile-specific.")
                .toggle("Show hints", showHints, "Global."))
        .build();
```

## Persistence and input safety

Profile display names are never used as JSON keys, paths, or identifiers. Each entry receives an independent backend ID such as `profile_2x8...`. User-entered names are normalized with Unicode NFKC, control and bidirectional formatting characters are removed, whitespace is collapsed, names are limited to 64 Unicode code points, and duplicate names receive a numeric suffix.

Linked setting keys are developer-controlled and accept only letters, numbers, `.`, `_`, and `-`.

Saved data has this shape:

```json
{
  "gameplay": {
    "profiles": {
      "selected": "profile_default",
      "profiles": [
        {
          "id": "profile_default",
          "name": "Default",
          "settings": {
            "enabled": true,
            "scale": 1.0
          }
        }
      ]
    }
  }
}
```

Unknown profile setting entries are preserved when the document is loaded and saved. This allows a temporarily removed linked setting to survive until the developer restores it.
