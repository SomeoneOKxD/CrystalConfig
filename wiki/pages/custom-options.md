---
layout: default
title: Custom options
description: Custom CrystalConfig rows, components, and custom list options.
---

# Custom options

CrystalConfig lets you insert your own components into normal option rows, full-width rows, or persisted list rows.

## Normal custom widget row

Use `@ConfigCustom` when you want the framework to render the normal label and description, but you provide the widget.

```java
@ConfigCustom(label = "Reload cache", description = "Refreshes runtime data.")
public static final Button reloadCache = new Button("Reload")
        .onClick(MyRuntime::reloadCache);
```

Manual equivalent:

```java
section.custom(
        "Reload cache",
        new Button("Reload").onClick(MyRuntime::reloadCache),
        "Refreshes runtime data."
);
```

Use `customBelow(...)` when the custom component should sit below the label/description area.

## Full-width custom option

Use `@ConfigCustomOption` when you want to own the whole row/card. A `Supplier<Component>` is recommended when the screen can be rebuilt.

```java
@ConfigCustomOption(searchText = "cache reload reset")
public static final Supplier<Component> cacheTools = () -> new Row()
        .gap(10)
        .align(Alignment.CENTER)
        .fillX()
        .add(new Label("Cache tools").flex(1))
        .add(new Button("Reload").onClick(MyRuntime::reloadCache).width(90))
        .add(new Button("Reset").onClick(MyRuntime::resetCache).width(90));
```

Manual equivalent:

```java
section.customOption(
        new Row()
                .gap(10)
                .align(Alignment.CENTER)
                .fillX()
                .add(new Label("Cache tools").flex(1))
                .add(new Button("Reload").onClick(MyRuntime::reloadCache).width(90)),
        "cache reload"
);
```

## Persisted custom object list

`@ConfigCustomList` is for `List<T>` state where each row needs custom controls. CrystalConfig owns add, remove, reorder, row shell, option label, and persistence registration. You provide the default entry and row component.

```java
public record ReplacementRule(String original, String replacement, double scale) {
}
```

```java
public final class ReplacementDefaults implements CustomListOption.EntryFactory<ReplacementRule> {
    @Override
    public ReplacementRule create() {
        return new ReplacementRule("Player", "Allay", 1.0d);
    }
}
```

```java
public final class ReplacementRowFactory implements CustomListOption.RowFactory<ReplacementRule> {
    @Override
    public Component create(CustomListOption.RowContext<ReplacementRule> context) {
        State<ReplacementRule> row = context.state();

        MutableState<String> original = new MutableState<>(row.get().original());
        MutableState<String> replacement = new MutableState<>(row.get().replacement());
        MutableState<Double> scale = new MutableState<>(row.get().scale());

        original.subscribe(value -> row.set(new ReplacementRule(value, replacement.get(), scale.get())));
        replacement.subscribe(value -> row.set(new ReplacementRule(original.get(), value, scale.get())));
        scale.subscribe(value -> row.set(new ReplacementRule(original.get(), replacement.get(), value)));

        return new Row()
                .gap(8)
                .align(Alignment.CENTER)
                .fillX()
                .add(CustomListOption.field("Original", new TextInput(original).fillX()).flex(1))
                .add(CustomListOption.field("Replacement", new TextInput(replacement).fillX()).flex(1))
                .add(CustomListOption.field("Scale", new NumberInput<>(scale, 0.1, 10.0, 0.1).fillX()).width(90));
    }
}
```

AutoConfig usage:

```java
@ConfigCustomList(
        key = "replacements",
        label = "Replacements",
        description = "Custom replacement rules.",
        entryFactory = ReplacementDefaults.class,
        rowFactory = ReplacementRowFactory.class,
        addButtonText = "+ Add rule",
        allowEmpty = true
)
public static final MutableState<List<ReplacementRule>> replacements = new MutableState<>(List.of(
        new ReplacementRule("Player", "Allay", 2.0d)
));
```

Manual equivalent:

```java
MutableState<List<ReplacementRule>> replacements = new MutableState<>(List.of(
        new ReplacementRule("Player", "Allay", 2.0d)
));

section.customBelow(
        "Replacements",
        new CustomListOption<>(replacements, new ReplacementDefaults(), new ReplacementRowFactory())
                .addButtonText("+ Add rule")
                .allowEmpty(true),
        "Custom replacement rules."
);
```

## Custom annotation-backed rows

Integrations can register new annotations with AutoConfig:

```java
AutoConfig.registerComponent(ConfigExample.class, ExampleValue.class, context ->
        new ExampleComponent(context.state())
);
```

The annotation must expose `label()`, `description()`, and `key()` methods. The annotated field must be compatible with the registered value type and wrapped in `State<T>`, `MutableState<T>`, or `ConfigValue<T>`.

## Practical notes

- Prefer immutable row values, such as records, and replace the row value after edits.
- Provide `searchText` for full-width custom rows so users can find them.
- Use `Supplier<Component>` in AutoConfig for components with focus, animation, or transient state.
- Use normal states and store registration for persistence; `@ConfigCustomOption` itself is not auto-persisted.
