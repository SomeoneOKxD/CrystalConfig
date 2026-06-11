# Config UI Settings

`ConfigUiSettings` stores the UI preferences for one config model or one manually built screen. It is not global state.

Use one settings instance per screen/model so mods do not overwrite each other's default theme, scale, or text-shadow preference.

## AutoConfig

```java
AutoConfig.Model model = AutoConfig.of(GeneralOptions.class)
        .configureSettings(settings -> settings
                .registerTheme(MyThemes.KUUDRA)
                .registerTheme(MyThemes.DARK_RED)
                .defaultTheme(MyThemes.KUUDRA)
                .defaultScale(1.0d)
                .defaultTextShadow(false));

UiRoot root = model.root("Kuudra Config", initialSearch == null ? "" : initialSearch);
```

You can also configure the settings object directly:

```java
AutoConfig.Model model = AutoConfig.of(GeneralOptions.class);
model.settings()
        .registerTheme(MyThemes.KUUDRA)
        .defaultTheme(MyThemes.KUUDRA);
```

## Manual builder

```java
ConfigUiSettings settings = ConfigUiSettings.create()
        .registerTheme(MyThemes.KUUDRA)
        .defaultTheme(MyThemes.KUUDRA)
        .defaultScale(1.0d)
        .defaultTextShadow(false);

Component screen = ConfigScreenBuilder.create("Kuudra Config", settings)
        .section("General", section -> {
            // Add rows here.
        })
        .build();

UiRoot root = new UiRoot(screen, settings);
```

## Persistence

`AutoConfig.Model#register(store)` registers config fields and the model settings with the same `GsonConfigStore`.

Manual screens should register the settings explicitly before loading the store:

```java
settings.register(store);
store.loadBlocking();
```

The selected theme, UI scale, and text-shadow toggle are stored under `__configUiSettings` unless a custom key is passed to `settings.register(store, key)`.

## Reset behavior

`settings.reset()` clears the selected theme and restores the developer default scale and text-shadow preference.

```java
settings.defaultTheme(MyThemes.KUUDRA);
settings.defaultScale(1.15d);
settings.defaultTextShadow(true);
settings.reset();
```

When settings are linked through `AutoConfig.Model#register(store)`, the settings popup reset action also resets the registered config values to their registration defaults.

## Text shadows

The settings popup includes a **Text shadow** toggle. When enabled, the renderer marks config text commands with shadow support; when disabled, no extra shadow draw is requested for normal config text. This keeps the disabled path cheap and avoids a second text draw unless the user or developer explicitly enables shadows.

Developers can choose the initial value per config model:

```java
ConfigUiSettings settings = ConfigUiSettings.create()
        .defaultTextShadow(true);

settings.textShadow(false); // user/runtime override
```

## Theme list

The theme dropdown contains built-in themes plus themes registered on that settings instance. If the default theme is already registered, it is not duplicated in the dropdown.

## Running code after reset-to-defaults

Developers can register callbacks that run after the settings popup resets the linked config back to its registered defaults. This is useful for rebuilding caches, reloading derived runtime state, reconnecting services, or refreshing UI state after the stored config values have been restored.

```java
ConfigUiSettings settings = ConfigUiSettings.create()
        .onDefaultsReset(() -> RuntimeCache.rebuildFromConfig());
```

The same hook is also available from the screen builder:

```java
UiRoot root = ConfigScreenBuilder.create("My Config", settings)
        .onDefaultsReset(() -> RuntimeCache.rebuildFromConfig())
        .section("General", section -> {
            // options
        })
        .buildRoot();
```

For AutoConfig models, register it during model setup:

```java
AutoConfig.Model model = AutoConfig.of(MyConfig.class)
        .onDefaultsReset(() -> RuntimeCache.rebuildFromConfig());
```

The callback is invoked only after the reset action has completed. Multiple callbacks may be registered; they run in registration order.
