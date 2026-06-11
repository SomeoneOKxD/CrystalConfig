---
layout: default
title: Persistence
description: Saving and loading CrystalConfig state with GsonConfigStore.
---

# Persistence

`GsonConfigStore` persists `State<T>` values to JSON. Loads are explicit. Saves are debounced, asynchronous, atomic, and preserve unknown JSON keys.

## AutoConfig registration

AutoConfig registers annotated values and UI settings for you:

```java
AutoConfig.Model model = AutoConfig.of(GameplayConfig.class);

GsonConfigStore store = GsonConfigStore.builder(configPath).build();
model.register(store);
store.loadBlocking();

UiRoot root = model.root("MyMod Config")
        .onClose(store::close);
```

`model.register(store)` also registers `ConfigUiSettings` under `__configUiSettings`.

## Manual registration

Manual screens must register each state explicitly:

```java
GsonConfigStore store = GsonConfigStore.builder(configPath).build()
        .register("enabled", enabled, Boolean.class)
        .register("scale", scale, Double.class)
        .register("name", name, String.class);

settings.register(store);
store.loadBlocking();
```

Use `TypeToken` for generic values:

```java
store.register("enabledModules", enabledModules, new TypeToken<List<Module>>() {});
```

## Nested JSON keys

Keys containing dots are written as nested objects unless the key starts with `__`:

```java
store.register("hud.enabled", hudEnabled, Boolean.class);
store.register("hud.scale", hudScale, Double.class);
```

Saved JSON shape:

```json
{
  "hud": {
    "enabled": true,
    "scale": 1.0
  },
  "__configVersion": 1
}
```

## Custom Gson adapters

Register adapters before `build()` when a value type needs custom JSON conversion:

```java
GsonConfigStore store = GsonConfigStore.builder(configPath)
        .typeAdapter(Profile.class, new ProfileTypeAdapter())
        .build()
        .register("profile", selectedProfile, Profile.class);
```

This is especially useful for grouped dropdown values that are complex objects rather than enums or strings.

## Migrations

Use config versions when persisted JSON keys or shapes change:

```java
GsonConfigStore store = GsonConfigStore.builder(configPath)
        .configVersion(2)
        .migration(1, root -> {
            if (root.has("oldScale")) {
                root.add("hudScale", root.remove("oldScale"));
            }
        })
        .build();
```

When a migration runs, CrystalConfig creates a backup beside the config file under a `backup` directory before writing the migrated JSON.

## Save timing

`State<T>` changes call `requestSave()` through the store subscription. The default debounce is 350 ms.

```java
GsonConfigStore store = GsonConfigStore.builder(configPath)
        .debounceMillis(500)
        .build();
```

Use `saveNow()` when you need an immediate save request, and always call `close()` from the config screen close callback to flush pending work.

## Reset behavior

The store captures registered defaults at registration time. Calling `resetRegisteredDefaults()` restores those values and requests a save.

AutoConfig links the settings reset action to the store reset action:

```java
model.register(store);
```

Manual screens can provide their own reset button or call:

```java
store.resetRegisteredDefaults();
```

## Load error handling

`loadBlocking()` throws `IOException`. Async `load()` records failures in `lastError()`:

```java
store.load();
Throwable problem = store.lastError();
```

For config screens, the most predictable integration is to call `loadBlocking()` before building the `UiRoot`.
