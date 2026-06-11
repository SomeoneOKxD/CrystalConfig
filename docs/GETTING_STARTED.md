# Getting Started

This guide shows the shortest path from config state to a rendered Minecraft config screen.

## 1. Define config state

Use `State<T>` values as the shared source of truth for UI, runtime code, and persistence.

```java
public final class ExampleConfig {
    public static final MutableState<Boolean> enabled = new MutableState<>(true);
    public static final MutableState<Double> scale = new MutableState<>(1.0d);
}
```

Use `Binding<T>` when the value already lives somewhere else:

```java
State<Boolean> fancyUi = Binding.of(
        () -> ClientConfig.fancyUi,
        value -> ClientConfig.fancyUi = value
);
```

## 2. Build a screen

### Manual builder

```java
ConfigUiSettings settings = ConfigUiSettings.create()
        .defaultTheme(ThemePresets.darkCrimson())
        .defaultScale(1.0d);

Component screen = ConfigScreenBuilder.create("Example Config", settings)
        .section("General", section -> section
                .toggle("Enabled", ExampleConfig.enabled, "Master switch.")
                .slider("Scale", ExampleConfig.scale, 0.5, 2.0, 0.05, "UI scale."))
        .build();

UiRoot root = new UiRoot(screen, settings);
```

### Annotation API

```java
@ConfigCategory(main = "General", sub = "Gameplay")
public final class GameplayConfig {
    @ConfigToggle(key = "enabled", label = "Enabled", description = "Master switch.")
    public static final MutableState<Boolean> enabled = new MutableState<>(true);
}
```

```java
AutoConfig.Model model = AutoConfig.of(GameplayConfig.class)
        .configureSettings(settings -> settings.defaultTheme(ThemePresets.darkCrimson()));

UiRoot root = model.root("My Mod");
```

## 3. Persist values

```java
GsonConfigStore store = GsonConfigStore.builder(configPath).build();
model.register(store);
store.loadBlocking();
```

Manual screens can register states directly:

```java
GsonConfigStore store = GsonConfigStore.builder(configPath)
        .build()
        .register("enabled", ExampleConfig.enabled, Boolean.class)
        .register("scale", ExampleConfig.scale, Double.class);

settings.register(store);
store.loadBlocking();
```

## 4. Connect to Minecraft

Minecraft screens should create a backend adapter, attach the active draw context each frame, then forward render and input calls to `UiRoot`.

```java
adapter.attachContext(drawContext);
root.render(adapter, width, height, uiScale, deltaSeconds);
```

Keep Minecraft classes in the adapter or integration module. The `core` module should stay Minecraft-free.

Close hooks can be registered when creating the root. They run once when the hosting config screen closes:

```java
UiRoot root = new UiRoot(screen, settings)
        .onClose(() -> store.close());

// Or, for manual builder screens:
UiRoot root = ConfigScreenBuilder.create("My Mod", settings)
        .onClose(() -> store.close())
        .section("General", section -> section.toggle("Enabled", enabled))
        .buildRoot();
```

## 5. Register Minecraft-only components

The Fabric module adds `@ConfigSound`. Register it during client init before creating auto-config models:

```java
MinecraftAutoConfig.register();
```

```java
@ConfigSound(label = "Alert sound", description = "Played when the alert triggers.")
public static final MutableState<SoundSetting> alertSound =
        new MutableState<>(SoundSetting.none());
```
