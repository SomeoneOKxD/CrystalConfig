package dev.someoneok.crystalconfigtester.config;

import dev.someoneok.crystalconfig.autoconfig.*;
import dev.someoneok.crystalconfig.components.Button;
import dev.someoneok.crystalconfig.components.CustomListOption;
import dev.someoneok.crystalconfig.components.Label;
import dev.someoneok.crystalconfig.components.NumberInput;
import dev.someoneok.crystalconfig.components.TextInput;
import dev.someoneok.crystalconfig.containers.Row;
import dev.someoneok.crystalconfig.layout.Alignment;
import dev.someoneok.crystalconfig.state.MutableState;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;

import java.util.List;
import java.util.function.Supplier;

@ConfigCategory(main = "02 / Advanced", sub = "Custom components")
public final class CustomOptions {
    private CustomOptions() { }

    public static final MutableState<Integer> clicks = new MutableState<>(0);

    @ConfigInfo(title = "Custom rows", description = "The buttons below update a dynamic info row.")
    public static final ConfigMarker info = ConfigMarker.marker()
            .description(() -> "Button clicks so far: " + clicks.get());

    @ConfigCustom(label = "Embedded component", description = "A normal labeled option with a custom button")
    public static final Component custom = new Button("Count click")
            .onClick(() -> clicks.set(clicks.get() + 1));

    @ConfigCustomOption(searchText = "custom full width layout row buttons")
    public static final Supplier<Component> customCard = () -> new Row()
            .gap(12).align(Alignment.CENTER).fillX()
            .add(new Label("Full-width custom row").flex(1))
            .add(new Button("+1 click").onClick(() -> clicks.set(clicks.get() + 1)).width(100));

    @ConfigCustomList(
            key = "custom.rules",
            label = "Custom object list",
            description = "Create/delete rules; edit text and number fields inside each entry",
            entryFactory = RuleDefaults.class,
            rowFactory = RuleRowFactory.class,
            addButtonText = "+ Add test rule",
            allowEmpty = true,
            rowGap = 8.0f
    )
    public static final MutableState<List<Rule>> rules = new MutableState<>(List.of(
            new Rule("Example", 1.0), new Rule("Second", 2.5)
    ));

    public record Rule(String name, double scale) { }

    public static final class RuleDefaults implements CustomListOption.EntryFactory<Rule> {
        @Override public Rule create() { return new Rule("New rule", 1.0); }
    }

    public static final class RuleRowFactory implements CustomListOption.RowFactory<Rule> {
        @Override public Component create(CustomListOption.RowContext<Rule> context) {
            State<Rule> row = context.state();
            MutableState<String> label = new MutableState<>(row.get().name());
            MutableState<Double> scale = new MutableState<>(row.get().scale());
            label.subscribe(value -> row.set(new Rule(value, scale.get())));
            scale.subscribe(value -> row.set(new Rule(label.get(), value)));
            return new Row().gap(8).align(Alignment.CENTER).fillX()
                    .add(CustomListOption.field("Name", new TextInput(label).fillX()).flex(1))
                    .add(CustomListOption.field("Scale", new NumberInput(scale, 0.1, 10.0, 0.1).fillX()).width(110));
        }
    }
}
