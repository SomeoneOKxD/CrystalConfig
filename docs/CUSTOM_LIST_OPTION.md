# Custom List Option

`@ConfigCustomList` renders a list setting where every entry needs custom controls. Use it for object lists such as replacement rules, HUD rows, routing rules, filters, and per-entry command mappings.

The framework owns the option title, description, add button, row frame, ordering, delete button, hidden/disabled predicates, and persistence registration. Your code provides the default entry and the component rendered inside each row.

## Annotation

```java
@ConfigCustomList(
        key = "replacements",
        label = "Replacements",
        description = "Custom replacement rules.",
        entryFactory = ReplacementDefaults.class,
        rowFactory = ReplacementRowFactory.class,
        addButtonText = "+ Add rule"
)
public static final MutableState<List<ReplacementRule>> replacements = new MutableState<>(List.of(
        new ReplacementRule("Player", "Allay", 2.0d)
));
```

The field must be a `State<List<T>>`, `MutableState<List<T>>`, or `ConfigValue<List<T>>` with a concrete generic list element type.

## Entry model

Immutable row values are easiest to reason about:

```java
public record ReplacementRule(String original, String replacement, double scale) {
}
```

For mutable classes, replace the row value after edits instead of mutating the existing object silently.

## Entry factory

```java
public final class ReplacementDefaults implements CustomListOption.EntryFactory<ReplacementRule> {
    @Override
    public ReplacementRule create() {
        return new ReplacementRule("Player", "Allay", 1.0d);
    }
}
```

The factory class must have a no-argument constructor. Static nested classes are supported.

## Row factory

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
                .add(CustomListOption.field("Scale", new NumberInput(scale, 0.1, 10.0, 0.1).fillX()).width(90));
    }
}
```

`RowContext#state()` returns a writable state for the row value. `RowContext#index()` exposes the row index at creation time. `RowContext#remove()` can be used by custom internal controls; most rows should rely on the built-in delete button.

## Labeled fields

Use `CustomListOption.field(label, component)` when a row input needs a compact label above it. `CustomListOption.labeled(...)` is an alias.

## Options

| Property | Purpose |
|---|---|
| `key` | Optional JSON key. Defaults to the category/accordion/field path. |
| `label` | Visible option title. |
| `description` | Optional text under the title. |
| `order` | Sort order inside the category or accordion. |
| `entryFactory` | Creates new list entries. |
| `rowFactory` | Creates row content for each entry. |
| `addButtonText` | Add button text. Defaults to `+ Add entry`. |
| `allowEmpty` | Allows deleting the last row when `true`. |
| `rowGap` | Vertical gap between row shells. |
| `addButtonHeight` | Add button height. |
