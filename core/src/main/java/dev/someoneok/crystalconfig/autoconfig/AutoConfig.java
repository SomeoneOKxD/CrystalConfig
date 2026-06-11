package dev.someoneok.crystalconfig.autoconfig;

import dev.someoneok.crystalconfig.components.CustomListOption;
import dev.someoneok.crystalconfig.components.Keybind;
import dev.someoneok.crystalconfig.components.TextInput;
import dev.someoneok.crystalconfig.config.ConfigScreenBuilder;
import dev.someoneok.crystalconfig.config.ConfigUiSettings;
import dev.someoneok.crystalconfig.persistence.GsonConfigStore;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.state.StateConditions;
import dev.someoneok.crystalconfig.ui.Component;
import dev.someoneok.crystalconfig.ui.UiRoot;
import dev.someoneok.crystalconfig.utils.UiClipboard;
import dev.someoneok.crystalconfig.utils.UiUrlOpener;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class AutoConfig {
    private AutoConfig() {}

    private static final Map<Class<? extends Annotation>, RegisteredComponent<?, ?>> CUSTOM_COMPONENTS = new ConcurrentHashMap<>();

    /**
     * Registers an annotation-backed config component. The annotation must expose the standard
     * metadata methods: label(), description(), and key().
     */
    public static <A extends Annotation, T> void registerComponent(
            Class<A> annotationType,
            Class<T> valueType,
            ConfigComponentFactory<A, T> factory
    ) {
        Objects.requireNonNull(annotationType, "annotationType");
        Objects.requireNonNull(valueType, "valueType");
        Objects.requireNonNull(factory, "factory");
        CUSTOM_COMPONENTS.put(annotationType, new RegisteredComponent<>(annotationType, valueType, factory));
    }

    public static <A extends Annotation> void unregisterComponent(Class<A> annotationType) {
        if (annotationType != null) CUSTOM_COMPONENTS.remove(annotationType);
    }

    public static Model of(Class<?>... classes) {
        return new Model(entries(Arrays.asList(classes)));
    }

    /**
     * Builds a model from one or more layout classes. The field declaration order in the layout
     * class controls category and sub-category order. Each layout field may contain a Class<?>,
     * Class<?>[], or Collection<Class<?>>. Put @ConfigCategory(main = "...", sub = "...") on
     * the field to override the category for those classes.
     */
    public static Model layout(Class<?>... layoutClasses) {
        List<Class<?>> layouts = Arrays.asList(layoutClasses);
        return new Model(layoutEntries(layouts), layouts);
    }

    /** Alias for {@link #layout(Class[])}. */
    public static Model ofLayout(Class<?>... layoutClasses) {
        return layout(layoutClasses);
    }

    /**
     * Scans one or more packages recursively and returns a model containing every concrete config class.
     *
     * <p>The scanner uses a built-in classpath scanner for normal development directories and jar files.
     * If ClassGraph happens to be present on the runtime classpath, it is used reflectively as an optional
     * fast path, but it is not required or bundled by this library.</p>
     */
    public static Model scanPackage(String... packageNames) {
        return new Model(entries(findConfigClasses(Arrays.asList(packageNames))));
    }

    public static Model scanPackageOf(Class<?> anchorClass) {
        Objects.requireNonNull(anchorClass, "anchorClass");
        return scanPackage(anchorClass.getPackageName());
    }

    public static Model scanPackage(Class<?> anchorClass, String packageName) {
        Objects.requireNonNull(anchorClass, "anchorClass");
        return scanPackage(packageName);
    }

    public static UiRoot root(String title, Class<?>... classes) {
        return of(classes).root(title);
    }

    public static final class Model {
        private final List<ConfigClassEntry> entries;
        private final List<Class<?>> classes;
        private final ConfigUiSettings settings;
        private final List<MainCategoryNode> mainCategories;
        private final FooterButtonNode footerButton;
        private final List<FooterIconNode> footerIcons;

        private Model(List<ConfigClassEntry> entries) {
            this(entries, List.of());
        }

        private Model(List<ConfigClassEntry> entries, Collection<Class<?>> footerSearchClasses) {
            Objects.requireNonNull(entries, "entries");
            LinkedHashMap<Class<?>, ConfigClassEntry> unique = new LinkedHashMap<>();
            for (ConfigClassEntry entry : entries) unique.putIfAbsent(entry.type(), entry);
            this.entries = new ArrayList<>(unique.values());
            this.classes = this.entries.stream().map(ConfigClassEntry::type).collect(Collectors.toList());
            this.settings = ConfigUiSettings.create();
            this.mainCategories = discover(this.entries);
            List<Class<?>> footerOwners = new ArrayList<>(this.classes);
            if (footerSearchClasses != null) footerOwners.addAll(footerSearchClasses);
            this.footerButton = discoverFooterButton(footerOwners);
            this.footerIcons = discoverFooterIcons(footerOwners);
            validate();
        }

        private void validate() {
            ConfigScreenBuilder builder = ConfigScreenBuilder.create("Config validation", settings);
            for (MainCategoryNode main : mainCategories) {
                for (CategoryNode category : main.categories) {
                    builder.section(main.title, category.title, category::addTo)
                            .hidden(category.hiddenWhen)
                            .disabled(category.disabledWhen);
                }
            }
            for (FooterIconNode icon : footerIcons) icon.addTo(builder);
            if (footerButton != null) footerButton.addTo(builder);
            builder.build();
        }

        public List<Class<?>> classes() { return List.copyOf(classes); }

        public ConfigUiSettings settings() { return settings; }

        public Model configureSettings(Consumer<ConfigUiSettings> consumer) {
            if (consumer != null) consumer.accept(settings);
            return this;
        }

        /** Register a callback that runs after this model is reset to registered defaults. */
        public Model onDefaultsReset(Runnable callback) {
            settings.onDefaultsReset(callback);
            return this;
        }

        /** Alias for {@link #onDefaultsReset(Runnable)}. */
        public Model afterDefaultsReset(Runnable callback) {
            return onDefaultsReset(callback);
        }

        public GsonConfigStore register(GsonConfigStore store) {
            for (MainCategoryNode main : mainCategories) for (CategoryNode category : main.categories) category.register(store);
            settings.register(store);
            settings.linkedDefaultsResetAction(store::resetRegisteredDefaults);
            return store;
        }

        public void loadBlocking(GsonConfigStore store) throws IOException {
            register(store);
            store.loadBlocking();
        }

        public Component screen(String title) {
            ConfigScreenBuilder builder = ConfigScreenBuilder.create(title, settings);
            for (MainCategoryNode main : mainCategories) {
                for (CategoryNode category : main.categories) {
                    builder.section(main.title, category.title, category::addTo)
                            .hidden(category.hiddenWhen)
                            .disabled(category.disabledWhen);
                }
            }
            for (FooterIconNode icon : footerIcons) icon.addTo(builder);
            if (footerButton != null) footerButton.addTo(builder);
            return builder.build();
        }

        public Component screen(String title, String initialSearch) {
            ConfigScreenBuilder builder = ConfigScreenBuilder.create(title, settings);

            for (MainCategoryNode main : mainCategories) {
                for (CategoryNode category : main.categories) {
                    builder.section(main.title, category.title, category::addTo)
                            .hidden(category.hiddenWhen)
                            .disabled(category.disabledWhen);
                }
            }
            for (FooterIconNode icon : footerIcons) icon.addTo(builder);
            if (footerButton != null) footerButton.addTo(builder);

            return builder.build(initialSearch);
        }

        public UiRoot root(String title) {
            return new UiRoot(screen(title), settings);
        }

        public UiRoot root(String title, String initialSearch) {
            return new UiRoot(screen(title, initialSearch), settings);
        }
    }

    private enum OptionKind {
        TOGGLE, CHECKBOX, SLIDER, SLIDER_LABEL, NUMBER, TEXT, COLOR, KEYBIND, DROPDOWN, SEARCHABLE_DROPDOWN, GROUPED_DROPDOWN, SEARCHABLE_GROUPED_DROPDOWN, MULTI_SELECT_DROPDOWN, SEARCHABLE_MULTI_SELECT_DROPDOWN, DRAGGABLE_LIST, CUSTOM_LIST, CUSTOM
    }

    private record OptionSpec(OptionKind kind, Annotation annotation, RegisteredComponent<?, ?> custom) {}

    private record RegisteredComponent<A extends Annotation, T>(
            Class<A> annotationType,
            Class<T> valueType,
            ConfigComponentFactory<A, T> factory
    ) {}

    @FunctionalInterface
    public interface ConfigComponentFactory<A extends Annotation, T> {
        Component create(ConfigComponentContext<A, T> context);
    }

    public record ConfigComponentContext<A extends Annotation, T>(
            Class<?> owner,
            Field field,
            A annotation,
            String key,
            String label,
            String description,
            State<T> state,
            Type valueType,
            Class<?> valueClass
    ) {}

    private record ConfigClassEntry(Class<?> type, CategorySpec categoryOverride) {}

    private record CategorySpec(String main, String sub, String hiddenWhen, String disabledWhen) {
        private static CategorySpec from(ConfigCategory category) {
            if (category == null) return null;
            return new CategorySpec(cleanName(category.main()), cleanName(category.sub()), category.hiddenWhen(), category.disabledWhen());
        }
    }

    private interface Node {
        int index();
        void register(GsonConfigStore store);
        void addTo(ConfigScreenBuilder.SectionBuilder section);
    }

    private static final class MainCategoryNode {
        private final String title;
        private final int index;
        private final List<CategoryNode> categories = new ArrayList<>();

        private MainCategoryNode(String title, int index) {
            this.title = cleanName(title);
            this.index = index;
        }
    }

    private static final class CategoryNode {
        private final String title;
        private final int index;
        private final List<Node> children = new ArrayList<>();
        private BooleanSupplier hiddenWhen = () -> false;
        private BooleanSupplier disabledWhen = () -> false;

        private CategoryNode(String title, int index) {
            this.title = cleanName(title);
            this.index = index;
        }

        private void register(GsonConfigStore store) {
            for (Node child : children) child.register(store);
        }

        private void addTo(ConfigScreenBuilder.SectionBuilder section) {
            children.sort(nodeComparator());
            for (Node child : children) child.addTo(section);
        }
    }

    private static final class AccordionNode implements Node {
        private final Class<?> owner;
        private final Field field;
        private final ConfigAccordion accordion;
        private final int index;
        private final List<Node> children = new ArrayList<>();

        private AccordionNode(Class<?> owner, Field field, ConfigAccordion accordion, int index) {
            this.owner = owner;
            this.field = field;
            this.accordion = accordion;
            this.index = index;
        }

        @Override public int index() { return index; }
        @Override public void register(GsonConfigStore store) { for (Node child : children) child.register(store); }
        @Override public void addTo(ConfigScreenBuilder.SectionBuilder section) {
            children.sort(nodeComparator());
            section.accordion(accordion.value(), accordion.description(), nested -> {
                for (Node child : children) child.addTo(nested);
            });
            applyShared(section, owner, field, accordion.tooltip());
        }
    }

    private static final class InfoNode implements Node {
        private final Class<?> owner;
        private final Field field;
        private final ConfigInfo info;
        private final int index;
        private InfoNode(Class<?> owner, Field field, ConfigInfo info, int index) { this.owner = owner; this.field = field; this.info = info; this.index = index; }
        @Override public int index() { return index; }
        @Override public void register(GsonConfigStore store) {}
        @Override public void addTo(ConfigScreenBuilder.SectionBuilder section) {
            ConfigMarker marker = readMarker(owner, field);
            section.info(
                    info.title(),
                    info.description(),
                    marker == null ? null : marker.infoTitleSupplier(),
                    marker == null ? null : marker.infoDescriptionSupplier(),
                    marker == null ? null : marker.infoTooltipSupplier()
            );
            applyAnnotationConditions(section, owner, "ConfigInfo", info.hiddenWhen(), info.disabledWhen());
            applyShared(section, owner, field, info.tooltip());
        }
    }

    private static final class ButtonNode implements Node {
        private final Class<?> owner;
        private final Field field;
        private final ConfigButton button;
        private final int index;
        private ButtonNode(Class<?> owner, Field field, ConfigButton button, int index) { this.owner = owner; this.field = field; this.button = button; this.index = index; }
        @Override public int index() { return index; }
        @Override public void register(GsonConfigStore store) {}
        @Override public void addTo(ConfigScreenBuilder.SectionBuilder section) {
            section.button(button.label(), button.buttonText(), action(owner, field, button), button.description());
            if (button.hiddenWhen() != null && !button.hiddenWhen().isBlank()) {
                section.hidden(resolveCondition(owner, button.hiddenWhen(), "ConfigButton", "hiddenWhen"));
            }
            if (button.disabledWhen() != null && !button.disabledWhen().isBlank()) {
                section.disabled(resolveCondition(owner, button.disabledWhen(), "ConfigButton", "disabledWhen"));
            }
            applyShared(section, owner, field, button.tooltip());
        }
    }

    private static final class FooterButtonNode {
        private final Class<?> owner;
        private final Field field;
        private final ConfigFooterButton button;

        private FooterButtonNode(Class<?> owner, Field field, ConfigFooterButton button) {
            this.owner = owner;
            this.field = field;
            this.button = button;
        }

        private void addTo(ConfigScreenBuilder builder) {
            builder.footerButton(button.value(), action(owner, field, button));
        }
    }

    private static final class FooterIconNode {
        private final Class<?> owner;
        private final Field field;
        private final ConfigFooterIcon icon;
        private final int index;

        private FooterIconNode(Class<?> owner, Field field, ConfigFooterIcon icon, int index) {
            this.owner = owner;
            this.field = field;
            this.icon = icon;
            this.index = index;
        }

        private int index() { return index; }

        private void addTo(ConfigScreenBuilder builder) {
            builder.footerIconButton(icon.icon(), footerIconAction(owner, field, icon), icon.tooltip());
        }
    }

    private static final class CustomNode implements Node {
        private final Class<?> owner;
        private final Field field;
        private final ConfigCustom custom;
        private final int index;
        private CustomNode(Class<?> owner, Field field, ConfigCustom custom, int index) { this.owner = owner; this.field = field; this.custom = custom; this.index = index; }
        @Override public int index() { return index; }
        @Override public void register(GsonConfigStore store) {}
        @Override public void addTo(ConfigScreenBuilder.SectionBuilder section) {
            try {
                Object value = field.get(null);
                if (!(value instanceof Component component)) throw new IllegalArgumentException("@ConfigCustom field must be a Component: " + owner.getName() + "." + field.getName());
                section.custom(custom.label(), component, custom.description());
                applyShared(section, owner, field, custom.tooltip());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read custom config component " + owner.getName() + "." + field.getName(), e);
            }
        }
    }

    private static final class CustomOptionNode implements Node {
        private final Class<?> owner;
        private final Field field;
        private final ConfigCustomOption custom;
        private final int index;
        private CustomOptionNode(Class<?> owner, Field field, ConfigCustomOption custom, int index) { this.owner = owner; this.field = field; this.custom = custom; this.index = index; }
        @Override public int index() { return index; }
        @Override public void register(GsonConfigStore store) {}
        @Override public void addTo(ConfigScreenBuilder.SectionBuilder section) {
            try {
                Object value = field.get(null);
                Component component;
                if (value instanceof Component direct) {
                    component = direct;
                } else if (value instanceof Supplier<?> supplier) {
                    Object created = supplier.get();
                    if (!(created instanceof Component supplied)) {
                        throw new IllegalArgumentException("@ConfigCustomOption Supplier must return a Component: " + owner.getName() + "." + field.getName());
                    }
                    component = supplied;
                } else {
                    throw new IllegalArgumentException("@ConfigCustomOption field must be a Component or Supplier<Component>: " + owner.getName() + "." + field.getName());
                }
                section.customOption(component, custom.searchText());
                applyShared(section, owner, field, custom.tooltip());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read custom config option " + owner.getName() + "." + field.getName(), e);
            }
        }
    }

    private static final class SeparatorNode implements Node {
        private final Class<?> owner;
        private final Field field;
        private final ConfigSeparator separator;
        private final int index;
        private SeparatorNode(Class<?> owner, Field field, ConfigSeparator separator, int index) { this.owner = owner; this.field = field; this.separator = separator; this.index = index; }
        @Override public int index() { return index; }
        @Override public void register(GsonConfigStore store) {}
        @Override public void addTo(ConfigScreenBuilder.SectionBuilder section) {
            section.separator();
            applyAnnotationConditions(section, owner, "ConfigSeparator", separator.hiddenWhen(), separator.disabledWhen());
            applyShared(section, owner, field, "");
        }
    }

    private static final class LabeledSeparatorNode implements Node {
        private final Class<?> owner;
        private final Field field;
        private final ConfigLabeledSeparator separator;
        private final int index;
        private LabeledSeparatorNode(Class<?> owner, Field field, ConfigLabeledSeparator separator, int index) { this.owner = owner; this.field = field; this.separator = separator; this.index = index; }
        @Override public int index() { return index; }
        @Override public void register(GsonConfigStore store) {}
        @Override public void addTo(ConfigScreenBuilder.SectionBuilder section) {
            section.labeledSeparator(separator.value(), separator.scale());
            applyAnnotationConditions(section, owner, "ConfigLabeledSeparator", separator.hiddenWhen(), separator.disabledWhen());
            applyShared(section, owner, field, "");
        }
    }

    private static final class SpacerNode implements Node {
        private final Class<?> owner;
        private final Field field;
        private final ConfigSpacer spacer;
        private final int index;
        private SpacerNode(Class<?> owner, Field field, ConfigSpacer spacer, int index) { this.owner = owner; this.field = field; this.spacer = spacer; this.index = index; }
        @Override public int index() { return index; }
        @Override public void register(GsonConfigStore store) {}
        @Override public void addTo(ConfigScreenBuilder.SectionBuilder section) {
            section.spacer(spacer.height());
            applyAnnotationConditions(section, owner, "ConfigSpacer", spacer.hiddenWhen(), spacer.disabledWhen());
            applyShared(section, owner, field, "");
        }
    }

    private static final class OptionNode<T> implements Node {
        private final Class<?> owner;
        private final Field field;
        private final OptionSpec spec;
        private final int index;
        private final String key;
        private final String label;
        private final String description;
        private final State<T> state;
        private final Type valueType;
        private final Class<?> valueClass;

        private OptionNode(Class<?> owner, Field field, OptionSpec spec, int index, String key, String label, String description, State<T> state, Type valueType, Class<?> valueClass) {
            this.owner = owner;
            this.field = field;
            this.spec = spec;
            this.index = index;
            this.key = key;
            this.label = label;
            this.description = description;
            this.state = state;
            this.valueType = valueType;
            this.valueClass = valueClass;
        }

        @Override public int index() { return index; }
        @Override public void register(GsonConfigStore store) { store.register(key, state, valueType); }

        @Override
        public void addTo(ConfigScreenBuilder.SectionBuilder section) {
            switch (spec.kind()) {
                case TOGGLE -> section.toggle(label, expect(Boolean.class), description);
                case CHECKBOX -> section.checkbox(label, expect(Boolean.class), description);
                case SLIDER -> {
                    ConfigSlider slider = (ConfigSlider) spec.annotation();
                    section.slider(label, numberState(), valueClass, slider.min(), slider.max(), slider.step(), description);
                }
                case SLIDER_LABEL -> {
                    ConfigSliderLabel slider = (ConfigSliderLabel) spec.annotation();
                    section.sliderLabel(label, numberState(), valueClass, slider.min(), slider.max(), slider.step(), description);
                }
                case NUMBER -> {
                    ConfigNumber number = (ConfigNumber) spec.annotation();
                    section.number(label, numberState(), valueClass, number.min(), number.max(), number.step(), description);
                }
                case TEXT -> {
                    ConfigText text = (ConfigText) spec.annotation();
                    section.text(label, expect(String.class), description, text.regex(), textSensitivity(text.sensitivity()));
                }
                case COLOR -> {
                    ConfigColor color = (ConfigColor) spec.annotation();
                    section.color(label, expect(ColorRGBA.class), color.allowAlpha(), description);
                }
                case KEYBIND -> {
                    ConfigKeybind keybind = (ConfigKeybind) spec.annotation();
                    section.keybind(label, expect(Keybind.class), description, keybind.disallowNone());
                }
                case DROPDOWN -> addDropdown(section, false);
                case SEARCHABLE_DROPDOWN -> addDropdown(section, true);
                case GROUPED_DROPDOWN -> addGroupedDropdown(section, false);
                case SEARCHABLE_GROUPED_DROPDOWN -> addGroupedDropdown(section, true);
                case MULTI_SELECT_DROPDOWN -> addMultiSelectDropdown(section, false);
                case SEARCHABLE_MULTI_SELECT_DROPDOWN -> addMultiSelectDropdown(section, true);
                case DRAGGABLE_LIST -> addDraggableList(section);
                case CUSTOM_LIST -> addCustomList(section);
                case CUSTOM -> addCustom(section);
            }
            applyShared(section, owner, field, "");
        }

        @SuppressWarnings("unchecked")
        private <U> State<U> expect(Class<U> expected) {
            if (valueClass != expected && !(expected == Boolean.class && valueClass == boolean.class)) {
                throw new IllegalArgumentException(spec.annotation().annotationType().getSimpleName() + " needs State<" + expected.getSimpleName() + ">: " + owner.getName() + "." + field.getName());
            }
            return (State<U>) state;
        }

        @SuppressWarnings("unchecked")
        private State<Double> doubleState() {
            if (valueClass != Double.class && valueClass != double.class) {
                throw new IllegalArgumentException(spec.annotation().annotationType().getSimpleName() + " needs State<Double>: " + owner.getName() + "." + field.getName());
            }
            return (State<Double>) state;
        }

        @SuppressWarnings("unchecked")
        private State<? extends Number> numberState() {
            if (valueClass != Double.class && valueClass != double.class
                    && valueClass != Float.class && valueClass != float.class
                    && valueClass != Integer.class && valueClass != int.class
                    && valueClass != Long.class && valueClass != long.class) {
                throw new IllegalArgumentException(spec.annotation().annotationType().getSimpleName() + " needs State<Double>, State<Float>, State<Integer>, or State<Long>: " + owner.getName() + "." + field.getName());
            }
            return (State<? extends Number>) state;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void addDropdown(ConfigScreenBuilder.SectionBuilder section, boolean searchable) {
            if (!valueClass.isEnum()) {
                throw new IllegalArgumentException(spec.annotation().annotationType().getSimpleName() + " needs State<Enum>: " + owner.getName() + "." + field.getName());
            }
            List options = enumChoices(valueClass);
            if (searchable) section.searchableDropdown(label, (State) state, (List) options, String::valueOf, description);
            else section.dropdown(label, (State) state, (List) options, String::valueOf, description);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void addGroupedDropdown(ConfigScreenBuilder.SectionBuilder section, boolean searchable) {
            Map groupedOptions = resolveGroupedOptions(spec.annotation(), owner, field, valueClass);
            if (searchable) section.searchableGroupedDropdown(label, (State) state, groupedOptions, description);
            else section.groupedDropdown(label, (State) state, groupedOptions, description);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void addMultiSelectDropdown(ConfigScreenBuilder.SectionBuilder section, boolean searchable) {
            Class<?> elementClass = listElementClass(valueType);
            if (!valueClass.equals(List.class) || elementClass == null || !elementClass.isEnum()) {
                throw new IllegalArgumentException(spec.annotation().annotationType().getSimpleName() + " needs State<List<Enum>>: " + owner.getName() + "." + field.getName());
            }
            List options = enumChoices(elementClass);
            if (searchable) section.searchableMultiSelectDropdown(label, (State) state, (List) options, String::valueOf, description);
            else section.multiSelectDropdown(label, (State) state, (List) options, String::valueOf, description);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void addDraggableList(ConfigScreenBuilder.SectionBuilder section) {
            ConfigDraggableList draggable = (ConfigDraggableList) spec.annotation();
            Class<?> elementClass = listElementClass(valueType);
            if (!valueClass.equals(List.class) || elementClass == null || !elementClass.isEnum()) {
                throw new IllegalArgumentException("@ConfigDraggableList needs State<List<Enum>>: " + owner.getName() + "." + field.getName());
            }
            section.draggableList(label, (State) state, (Class) elementClass, String::valueOf, draggable.allowEmpty(), draggable.allowDeleting(), description);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void addCustomList(ConfigScreenBuilder.SectionBuilder section) {
            ConfigCustomList list = (ConfigCustomList) spec.annotation();
            Type elementType = listElementType(valueType);
            if (!valueClass.equals(List.class) || elementType == null) {
                throw new IllegalArgumentException("@ConfigCustomList needs State<List<T>>: " + owner.getName() + "." + field.getName());
            }
            CustomListOption.EntryFactory entryFactory = instantiate(list.entryFactory(), "entryFactory", owner, field);
            CustomListOption.RowFactory rowFactory = instantiate(list.rowFactory(), "rowFactory", owner, field);
            Component component = new CustomListOption((State) state, entryFactory, rowFactory)
                    .addButtonText(list.addButtonText())
                    .allowEmpty(list.allowEmpty())
                    .rowGap(list.rowGap())
                    .addButtonHeight(list.addButtonHeight())
                    .fillX();
            section.customBelow(label, component, description);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void addCustom(ConfigScreenBuilder.SectionBuilder section) {
            RegisteredComponent registered = spec.custom();
            if (registered == null) throw new IllegalStateException("Missing custom component registration for " + spec.annotation().annotationType().getName());
            if (!registered.valueType().isAssignableFrom(valueClass)) {
                throw new IllegalArgumentException("@" + spec.annotation().annotationType().getSimpleName()
                        + " needs State<" + registered.valueType().getSimpleName() + ">: "
                        + owner.getName() + "." + field.getName());
            }
            ConfigComponentContext context = new ConfigComponentContext(owner, field, spec.annotation(), key, label, description, state, valueType, valueClass);
            Component component = registered.factory().create(context);
            if (component == null) throw new IllegalArgumentException("Custom config component factory returned null for " + owner.getName() + "." + field.getName());
            section.custom(label, component, description);
        }
    }

    private static Comparator<Node> nodeComparator() { return Comparator.comparingInt(Node::index); }

    private static List<MainCategoryNode> discover(List<ConfigClassEntry> entries) {
        Map<String, MainCategoryNode> byMain = new LinkedHashMap<>();
        Map<String, CategoryNode> byMainAndCategory = new LinkedHashMap<>();
        int[] index = {0};
        int classIndex = 0;
        for (ConfigClassEntry entry : entries) {
            Class<?> clazz = entry.type();
            CategorySpec category = entry.categoryOverride() != null ? entry.categoryOverride() : CategorySpec.from(clazz.getAnnotation(ConfigCategory.class));
            String mainName = category == null ? "General" : category.main();
            String categoryName = category == null ? "General" : category.sub();
            int categoryIndex = classIndex;

            MainCategoryNode mainNode = byMain.computeIfAbsent(keyName(mainName), ignored -> new MainCategoryNode(mainName, categoryIndex));
            String categoryKey = keyName(mainName) + ":" + keyName(categoryName);
            CategoryNode categoryNode = byMainAndCategory.computeIfAbsent(categoryKey, ignored -> {
                CategoryNode created = new CategoryNode(categoryName, categoryIndex);
                mainNode.categories.add(created);
                return created;
            });
            if (category != null) {
                categoryNode.hiddenWhen = combineOr(categoryNode.hiddenWhen, resolveCategoryCondition(clazz, category.hiddenWhen(), "hiddenWhen"));
                categoryNode.disabledWhen = combineOr(categoryNode.disabledWhen, resolveCategoryCondition(clazz, category.disabledWhen(), "disabledWhen"));
            }
            discoverChildren(clazz, categoryNode.children, keyPrefix(mainName) + "." + keyPrefix(categoryName), index);
            classIndex++;
        }
        List<MainCategoryNode> result = new ArrayList<>(byMain.values());
        result.sort(Comparator.comparingInt(c -> c.index));
        for (MainCategoryNode main : result) {
            main.categories.sort(Comparator.comparingInt(c -> c.index));
        }
        return result;
    }

    private static void discoverChildren(Class<?> owner, List<Node> out, String keyPrefix, int[] index) {
        for (Field field : declaredFieldsInClassFileOrder(owner)) {
            OptionSpec option = optionSpec(field);
            ConfigInfo info = field.getAnnotation(ConfigInfo.class);
            ConfigButton button = field.getAnnotation(ConfigButton.class);
            ConfigCustom custom = field.getAnnotation(ConfigCustom.class);
            ConfigCustomOption customOption = field.getAnnotation(ConfigCustomOption.class);
            ConfigSeparator separator = field.getAnnotation(ConfigSeparator.class);
            ConfigLabeledSeparator labeledSeparator = field.getAnnotation(ConfigLabeledSeparator.class);
            ConfigSpacer spacer = field.getAnnotation(ConfigSpacer.class);
            ConfigFooterButton footerButton = field.getAnnotation(ConfigFooterButton.class);
            ConfigFooterIcon footerIcon = field.getAnnotation(ConfigFooterIcon.class);
            ConfigAccordion accordion = field.getAnnotation(ConfigAccordion.class);
            int annotationCount = count(option, info, button, custom, customOption, separator, labeledSeparator, spacer, footerButton, footerIcon, accordion);
            if (annotationCount == 0) continue;
            if (annotationCount > 1) throw new IllegalArgumentException("Only one config row/option annotation is allowed on " + owner.getName() + "." + field.getName());
            if (footerButton != null || footerIcon != null) continue;
            requireStatic(field, owner);
            field.setAccessible(true);
            if (option != null) out.add(optionNode(owner, field, option, keyPrefix, index[0]++));
            else if (info != null) out.add(new InfoNode(owner, field, info, index[0]++));
            else if (button != null) out.add(new ButtonNode(owner, field, button, index[0]++));
            else if (custom != null) out.add(new CustomNode(owner, field, custom, index[0]++));
            else if (customOption != null) out.add(new CustomOptionNode(owner, field, customOption, index[0]++));
            else if (separator != null) out.add(new SeparatorNode(owner, field, separator, index[0]++));
            else if (labeledSeparator != null) out.add(new LabeledSeparatorNode(owner, field, labeledSeparator, index[0]++));
            else if (spacer != null) out.add(new SpacerNode(owner, field, spacer, index[0]++));
            else {
                Class<?> accordionOwner = accordionOwner(owner, field);
                AccordionNode node = new AccordionNode(accordionOwner, field, accordion, index[0]++);
                discoverChildren(accordionOwner, node.children, keyPrefix + "." + keyName(field.getName()), index);
                out.add(node);
            }
        }
    }

    private static FooterButtonNode discoverFooterButton(List<Class<?>> classes) {
        List<FooterButtonNode> buttons = new ArrayList<>();
        for (Class<?> clazz : classes) collectFooterButtons(clazz, buttons);
        if (buttons.size() > 1) {
            String fields = buttons.stream()
                    .map(button -> button.owner.getName() + "." + button.field.getName())
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Only one @ConfigFooterButton is allowed per config model. Found " + buttons.size() + ": " + fields);
        }
        return buttons.isEmpty() ? null : buttons.get(0);
    }

    private static List<FooterIconNode> discoverFooterIcons(List<Class<?>> classes) {
        List<FooterIconNode> icons = new ArrayList<>();
        int[] index = {0};
        for (Class<?> clazz : classes) collectFooterIcons(clazz, icons, index);
        icons.sort(Comparator.comparingInt(FooterIconNode::index));
        return icons;
    }

    private static void collectFooterIcons(Class<?> owner, List<FooterIconNode> out, int[] index) {
        for (Field field : declaredFieldsInClassFileOrder(owner)) {
            ConfigFooterIcon footerIcon = field.getAnnotation(ConfigFooterIcon.class);
            if (footerIcon == null) continue;
            if (hasAnyRowAnnotationExceptFooterControls(field, ConfigFooterIcon.class)) {
                throw new IllegalArgumentException("@ConfigFooterIcon cannot be combined with another config row/option annotation on " + owner.getName() + "." + field.getName());
            }
            requireStatic(field, owner);
            field.setAccessible(true);
            validateFooterIconField(owner, field, footerIcon);
            out.add(new FooterIconNode(owner, field, footerIcon, index[0]++));
        }
        for (Class<?> nested : declaredClassesInClassFileOrder(owner)) {
            if (!Modifier.isStatic(nested.getModifiers())) continue;
            collectFooterIcons(nested, out, index);
        }
    }

    private static void collectFooterButtons(Class<?> owner, List<FooterButtonNode> out) {
        for (Field field : declaredFieldsInClassFileOrder(owner)) {
            ConfigFooterButton footerButton = field.getAnnotation(ConfigFooterButton.class);
            if (footerButton == null) continue;
            if (hasAnyRowAnnotationExceptFooter(field)) {
                throw new IllegalArgumentException("@ConfigFooterButton cannot be combined with another config row/option annotation on " + owner.getName() + "." + field.getName());
            }
            requireStatic(field, owner);
            field.setAccessible(true);
            out.add(new FooterButtonNode(owner, field, footerButton));
        }
        for (Class<?> nested : declaredClassesInClassFileOrder(owner)) {
            if (!Modifier.isStatic(nested.getModifiers())) continue;
            collectFooterButtons(nested, out);
        }
    }

    private static boolean hasAnyRowAnnotationExceptFooter(Field field) {
        return hasAnyRowAnnotationExceptFooterControls(field, ConfigFooterButton.class);
    }

    private static boolean hasAnyRowAnnotationExceptFooterControls(Field field, Class<? extends Annotation> allowedFooterAnnotation) {
        for (Class<? extends Annotation> annotation : rowAnnotations()) {
            if (annotation != allowedFooterAnnotation && field.isAnnotationPresent(annotation)) return true;
        }
        return false;
    }

    private static OptionSpec optionSpec(Field field) {
        List<OptionSpec> specs = new ArrayList<>();
        add(specs, field, ConfigToggle.class, OptionKind.TOGGLE);
        add(specs, field, ConfigCheckbox.class, OptionKind.CHECKBOX);
        add(specs, field, ConfigSlider.class, OptionKind.SLIDER);
        add(specs, field, ConfigSliderLabel.class, OptionKind.SLIDER_LABEL);
        add(specs, field, ConfigNumber.class, OptionKind.NUMBER);
        add(specs, field, ConfigText.class, OptionKind.TEXT);
        add(specs, field, ConfigColor.class, OptionKind.COLOR);
        add(specs, field, ConfigKeybind.class, OptionKind.KEYBIND);
        add(specs, field, ConfigDropdown.class, OptionKind.DROPDOWN);
        add(specs, field, ConfigSearchableDropdown.class, OptionKind.SEARCHABLE_DROPDOWN);
        add(specs, field, ConfigGroupedDropdown.class, OptionKind.GROUPED_DROPDOWN);
        add(specs, field, ConfigSearchableGroupedDropdown.class, OptionKind.SEARCHABLE_GROUPED_DROPDOWN);
        add(specs, field, ConfigMultiSelectDropdown.class, OptionKind.MULTI_SELECT_DROPDOWN);
        add(specs, field, ConfigSearchableMultiSelectDropdown.class, OptionKind.SEARCHABLE_MULTI_SELECT_DROPDOWN);
        add(specs, field, ConfigDraggableList.class, OptionKind.DRAGGABLE_LIST);
        add(specs, field, ConfigCustomList.class, OptionKind.CUSTOM_LIST);
        for (RegisteredComponent<?, ?> registered : CUSTOM_COMPONENTS.values()) {
            Annotation annotation = field.getAnnotation(registered.annotationType());
            if (annotation != null) specs.add(new OptionSpec(OptionKind.CUSTOM, annotation, registered));
        }
        if (specs.size() > 1) throw new IllegalArgumentException("Only one option type annotation is allowed on " + field.getDeclaringClass().getName() + "." + field.getName());
        return specs.isEmpty() ? null : specs.get(0);
    }

    private static <A extends Annotation> void add(List<OptionSpec> specs, Field field, Class<A> type, OptionKind kind) {
        A annotation = field.getAnnotation(type);
        if (annotation != null) specs.add(new OptionSpec(kind, annotation, null));
    }

    private static TextInput.SensitiveMode textSensitivity(ConfigTextSensitivity sensitivity) {
        if (sensitivity == null) return TextInput.SensitiveMode.NONE;
        return switch (sensitivity) {
            case NONE -> TextInput.SensitiveMode.NONE;
            case VISIBLE_WHILE_EDITING -> TextInput.SensitiveMode.VISIBLE_WHILE_EDITING;
            case ALWAYS_HIDDEN -> TextInput.SensitiveMode.ALWAYS_HIDDEN;
        };
    }

    private static int count(Object... values) { int count = 0; for (Object value : values) if (value != null) count++; return count; }

    private static void requireStatic(Field field, Class<?> owner) {
        if (!Modifier.isStatic(field.getModifiers())) throw new IllegalArgumentException("Config field must be static: " + owner.getName() + "." + field.getName());
    }

    private static Class<?> accordionOwner(Class<?> owner, Field field) {
        try {
            Object value = field.get(null);
            if (!(value instanceof Class<?> accordionOwner)) {
                throw new IllegalArgumentException("@ConfigAccordion field must contain a Class<?> value: " + owner.getName() + "." + field.getName());
            }
            if (accordionOwner.isPrimitive() || accordionOwner.isArray() || accordionOwner.isEnum()) {
                throw new IllegalArgumentException("@ConfigAccordion field must point to a config holder class: " + owner.getName() + "." + field.getName());
            }
            return accordionOwner;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read @ConfigAccordion field " + owner.getName() + "." + field.getName(), e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static OptionNode<?> optionNode(Class<?> owner, Field field, OptionSpec spec, String keyPrefix, int index) {
        try {
            Object raw = field.get(null);
            if (!(raw instanceof State<?> state)) throw new IllegalArgumentException(spec.annotation().annotationType().getSimpleName() + " field must implement State<T>: " + owner.getName() + "." + field.getName());
            Type valueType = stateValueType(field);
            Class<?> valueClass = rawClass(valueType);
            String explicitKey = stringValue(spec.annotation(), "key").trim();
            String localKey = explicitKey.isBlank() ? field.getName() : explicitKey;
            String key = keyPrefix + "." + localKey;
            return new OptionNode(owner, field, spec, index, key, stringValue(spec.annotation(), "label"), stringValue(spec.annotation(), "description"), state, valueType, valueClass);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read config field " + owner.getName() + "." + field.getName(), e);
        }
    }

    private static String stringValue(Annotation annotation, String name) {
        try { return String.valueOf(annotation.annotationType().getMethod(name).invoke(annotation)); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Missing annotation property " + annotation.annotationType().getSimpleName() + "." + name + "()", e); }
    }

    private static Type stateValueType(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType parameterized) {
            Type raw = parameterized.getRawType();
            if (raw instanceof Class<?>) return parameterized.getActualTypeArguments()[0];
        }
        throw new IllegalArgumentException("Cannot infer State<T> type for " + field.getDeclaringClass().getName() + "." + field.getName() + ". Declare it as MutableState<Foo>, State<Foo>, or ConfigValue<Foo>.");
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) return clazz;
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> clazz) return clazz;
        throw new IllegalArgumentException("Unsupported config value type: " + type);
    }

    private static void applyAnnotationConditions(ConfigScreenBuilder.SectionBuilder section, Class<?> owner, String annotationName, String hiddenWhen, String disabledWhen) {
        if (hiddenWhen != null && !hiddenWhen.isBlank()) {
            section.hidden(resolveCondition(owner, hiddenWhen, annotationName, "hiddenWhen"));
        }
        if (disabledWhen != null && !disabledWhen.isBlank()) {
            section.disabled(resolveCondition(owner, disabledWhen, annotationName, "disabledWhen"));
        }
    }

    private static void applyShared(ConfigScreenBuilder.SectionBuilder section, Class<?> owner, Field field, String tooltipOverride) {
        StateConditions conditions = readConditions(owner, field);
        if (conditions != null) {
            section.hidden(conditions.hiddenWhen());
            section.disabled(conditions.disabledWhen());
        }

        String tooltip = field != null && field.isAnnotationPresent(ConfigTooltip.class) ? field.getAnnotation(ConfigTooltip.class).value() : tooltipOverride;
        if ((tooltip == null || tooltip.isBlank()) && owner.isAnnotationPresent(ConfigTooltip.class)) tooltip = owner.getAnnotation(ConfigTooltip.class).value();
        if (tooltip != null && !tooltip.isBlank()) section.tooltip(tooltip);
    }

    private static ConfigMarker readMarker(Class<?> owner, Field field) {
        if (field == null) return null;
        try {
            Object raw = field.get(null);
            return raw instanceof ConfigMarker marker ? marker : null;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read config marker " + owner.getName() + "." + field.getName(), e);
        }
    }

    private static StateConditions readConditions(Class<?> owner, Field field) {
        if (field == null) return null;
        try {
            Object raw = field.get(null);
            if (raw instanceof StateConditions conditions) return conditions;
            return null;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read config field conditions " + owner.getName() + "." + field.getName(), e);
        }
    }

    private static void validateFooterIconField(Class<?> owner, Field field, ConfigFooterIcon icon) {
        try {
            Object value = field.get(null);
            if (icon.action() == ConfigFooterIconAction.RUNNABLE) {
                if (!(value instanceof Runnable)) {
                    throw new IllegalArgumentException("@ConfigFooterIcon with action RUNNABLE field must be a static Runnable: " + owner.getName() + "." + field.getName());
                }
                return;
            }
            if (!(value instanceof ConfigMarker)) {
                throw new IllegalArgumentException("@ConfigFooterIcon with action " + icon.action() + " field must be a static ConfigMarker. Only RUNNABLE footer icons use Runnable fields: " + owner.getName() + "." + field.getName());
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read @ConfigFooterIcon field " + owner.getName() + "." + field.getName(), e);
        }
    }

    private static Runnable action(Class<?> owner, Field field, Annotation annotation) {
        String annotationName = annotation.annotationType().getSimpleName();
        try {
            Object value = field.get(null);
            if (value instanceof Runnable runnable) return runnable;
            throw new IllegalArgumentException("@" + annotationName + " field must be a static Runnable: " + owner.getName() + "." + field.getName());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read @" + annotationName + " runnable field " + owner.getName() + "." + field.getName(), e);
        }
    }

    private static Runnable footerIconAction(Class<?> owner, Field field, ConfigFooterIcon icon) {
        ConfigFooterIconAction type = icon.action();
        String value = icon.value();
        if (type == ConfigFooterIconAction.RUNNABLE) return action(owner, field, icon);
        if (type == ConfigFooterIconAction.COPY_TO_CLIPBOARD) {
            return () -> UiClipboard.set(value);
        }
        if (type == ConfigFooterIconAction.OPEN_URL) {
            return () -> UiUrlOpener.open(value);
        }
        return () -> { };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map resolveGroupedOptions(Annotation annotation, Class<?> owner, Field optionField, Class<?> valueClass) {
        String annotationName = annotation.annotationType().getSimpleName();
        String memberName = stringValue(annotation, "options").trim();
        if (memberName.isBlank()) {
            throw new IllegalArgumentException("@" + annotationName + " options() must name a static Map field or no-arg static method: " + owner.getName() + "." + optionField.getName());
        }

        Object raw = resolveStaticMember(owner, memberName, annotationName, "options");
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("@" + annotationName + " options member " + owner.getName() + "." + memberName + " must return Map<G, ? extends Collection<T>>");
        }

        LinkedHashMap normalized = new LinkedHashMap();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object group = entry.getKey();
            if (!(group instanceof Enum<?>)) {
                throw new IllegalArgumentException("@" + annotationName + " options map keys must be enum values. Invalid key in " + owner.getName() + "." + memberName + ": " + group);
            }
            Object valuesRaw = entry.getValue();
            if (!(valuesRaw instanceof Collection<?> values)) {
                throw new IllegalArgumentException("@" + annotationName + " options map values must be collections. Invalid group " + group + " in " + owner.getName() + "." + memberName);
            }
            ArrayList normalizedValues = new ArrayList(values.size());
            for (Object value : values) {
                if (value != null && !boxed(valueClass).isInstance(value)) {
                    throw new IllegalArgumentException("@" + annotationName + " option value " + value + " is not assignable to State<" + valueClass.getSimpleName() + "> for " + owner.getName() + "." + optionField.getName());
                }
                normalizedValues.add(value);
            }
            normalized.put(group, normalizedValues);
        }
        return normalized;
    }

    private static Object resolveStaticMember(Class<?> owner, String memberName, String annotationName, String propertyName) {
        try {
            Field field = owner.getDeclaredField(memberName);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("@" + annotationName + " " + propertyName + " member must be static: " + owner.getName() + "." + memberName);
            }
            field.setAccessible(true);
            return field.get(null);
        } catch (NoSuchFieldException ignored) {
            try {
                Method method = owner.getDeclaredMethod(memberName);
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                    throw new IllegalArgumentException("@" + annotationName + " " + propertyName + " method must be static and no-arg: " + owner.getName() + "." + memberName + "()");
                }
                method.setAccessible(true);
                return method.invoke(null);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Cannot find @" + annotationName + " " + propertyName + " field or no-arg method " + owner.getName() + "." + memberName, e);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot invoke @" + annotationName + " " + propertyName + " method " + owner.getName() + "." + memberName + "()", e);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read @" + annotationName + " " + propertyName + " field " + owner.getName() + "." + memberName, e);
        }
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        if (type == void.class) return Void.class;
        return type;
    }

    private static List<?> enumChoices(Class<?> valueClass) {
        if (valueClass.isEnum()) return Arrays.asList(valueClass.getEnumConstants());
        throw new IllegalArgumentException("Dropdown option needs enum type: " + valueClass.getName());
    }

    private static Class<?> listElementClass(Type type) {
        Type element = listElementType(type);
        return element instanceof Class<?> clazz ? clazz : null;
    }

    private static Type listElementType(Type type) {
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() == List.class) {
            return parameterized.getActualTypeArguments()[0];
        }
        return null;
    }

    private static <T> T instantiate(Class<? extends T> type, String role, Class<?> owner, Field field) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot instantiate @ConfigCustomList " + role + " " + type.getName()
                    + " for " + owner.getName() + "." + field.getName() + ". Provide a no-arg constructor.", e);
        }
    }

    private static BooleanSupplier combineOr(BooleanSupplier left, BooleanSupplier right) {
        if (left == null) return right == null ? () -> false : right;
        if (right == null) return left;
        return () -> safeBoolean(left) || safeBoolean(right);
    }

    private static boolean safeBoolean(BooleanSupplier supplier) {
        try { return supplier != null && supplier.getAsBoolean(); }
        catch (RuntimeException ignored) { return false; }
    }

    private static BooleanSupplier resolveCategoryCondition(Class<?> owner, String memberName, String propertyName) {
        return resolveCondition(owner, memberName, "ConfigCategory", propertyName);
    }

    private static BooleanSupplier resolveCondition(Class<?> owner, String memberName, String annotationName, String propertyName) {
        if (memberName == null || memberName.isBlank()) return () -> false;
        String name = memberName.trim();
        try {
            Field field = owner.getDeclaredField(name);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("@" + annotationName + " " + propertyName + " member must be static: " + owner.getName() + "." + name);
            }
            field.setAccessible(true);
            return conditionSupplier(field.get(null), owner, name, annotationName, propertyName);
        } catch (NoSuchFieldException ignored) {
            try {
                Method method = owner.getDeclaredMethod(name);
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0) {
                    throw new IllegalArgumentException("@" + annotationName + " " + propertyName + " method must be static and no-arg: " + owner.getName() + "." + name + "()");
                }
                method.setAccessible(true);
                return () -> {
                    try { return conditionValue(method.invoke(null), owner, name, annotationName, propertyName); }
                    catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot evaluate @" + annotationName + " " + propertyName + " method " + owner.getName() + "." + name + "()", e); }
                };
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Cannot find @" + annotationName + " " + propertyName + " field or no-arg method " + owner.getName() + "." + name, e);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read @" + annotationName + " " + propertyName + " field " + owner.getName() + "." + name, e);
        }
    }

    private static BooleanSupplier conditionSupplier(Object value, Class<?> owner, String memberName, String annotationName, String propertyName) {
        if (value instanceof BooleanSupplier supplier) return supplier;
        if (value instanceof State<?> state) return () -> Boolean.TRUE.equals(state.get());
        if (value instanceof Boolean bool) return () -> bool;
        throw new IllegalArgumentException("@" + annotationName + " " + propertyName + " member " + owner.getName() + "." + memberName + " must be boolean, Boolean, State<Boolean>, or BooleanSupplier");
    }

    private static boolean conditionValue(Object value, Class<?> owner, String memberName, String annotationName, String propertyName) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof State<?> state) return Boolean.TRUE.equals(state.get());
        if (value instanceof BooleanSupplier supplier) return supplier.getAsBoolean();
        throw new IllegalArgumentException("@" + annotationName + " " + propertyName + " method " + owner.getName() + "." + memberName + "() must return boolean, Boolean, State<Boolean>, or BooleanSupplier");
    }

    private static String firstNonBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) return value;
            }
        }
        return "General";
    }

    private static String keyPrefix(String category) { return keyName(category).replace('.', '_'); }

    private static Field[] declaredFieldsInClassFileOrder(Class<?> owner) {
        Field[] fields = owner.getDeclaredFields();
        Map<String, Integer> order = ClassFileOrder.fields(owner);
        Arrays.sort(fields, Comparator.comparingInt((Field field) -> order.getOrDefault(field.getName(), Integer.MAX_VALUE))
                .thenComparing(Field::getName));
        return fields;
    }

    private static Class<?>[] declaredClassesInClassFileOrder(Class<?> owner) {
        Class<?>[] classes = owner.getDeclaredClasses();
        Map<String, Integer> order = ClassFileOrder.innerClasses(owner);
        Arrays.sort(classes, Comparator.comparingInt((Class<?> nested) -> order.getOrDefault(nested.getName(), Integer.MAX_VALUE))
                .thenComparing(Class::getName));
        return classes;
    }

    private static final class ClassFileOrder {
        private static final Map<Class<?>, Map<String, Integer>> FIELD_CACHE = new ConcurrentHashMap<>();
        private static final Map<Class<?>, Map<String, Integer>> INNER_CLASS_CACHE = new ConcurrentHashMap<>();

        private static Map<String, Integer> fields(Class<?> owner) {
            return FIELD_CACHE.computeIfAbsent(owner, clazz -> read(clazz).fields);
        }

        private static Map<String, Integer> innerClasses(Class<?> owner) {
            return INNER_CLASS_CACHE.computeIfAbsent(owner, clazz -> read(clazz).innerClasses);
        }

        private static ClassFileOrders read(Class<?> owner) {
            String resourceName = owner.getSimpleName() + ".class";
            try (InputStream in = owner.getResourceAsStream(resourceName)) {
                if (in == null) return ClassFileOrders.EMPTY;
                return parse(in);
            } catch (IOException | RuntimeException e) {
                return ClassFileOrders.EMPTY;
            }
        }

        private static ClassFileOrders parse(InputStream input) throws IOException {
            DataInputStream in = new DataInputStream(input);
            if (in.readInt() != 0xCAFEBABE) return ClassFileOrders.EMPTY;
            in.readUnsignedShort(); // minor
            in.readUnsignedShort(); // major

            Object[] cp = readConstantPool(in);

            in.readUnsignedShort(); // access_flags
            in.readUnsignedShort(); // this_class
            in.readUnsignedShort(); // super_class

            int interfaces = in.readUnsignedShort();
            for (int i = 0; i < interfaces; i++) in.readUnsignedShort();

            Map<String, Integer> fields = new HashMap<>();
            int fieldCount = in.readUnsignedShort();
            for (int i = 0; i < fieldCount; i++) {
                in.readUnsignedShort(); // access_flags
                String name = utf8(cp, in.readUnsignedShort());
                in.readUnsignedShort(); // descriptor_index
                skipAttributes(in);
                fields.putIfAbsent(name, i);
            }

            int methods = in.readUnsignedShort();
            for (int i = 0; i < methods; i++) skipMember(in);

            Map<String, Integer> innerClasses = new HashMap<>();
            int classAttributes = in.readUnsignedShort();
            for (int i = 0; i < classAttributes; i++) {
                String name = utf8(cp, in.readUnsignedShort());
                int length = in.readInt();
                if ("InnerClasses".equals(name)) {
                    int count = in.readUnsignedShort();
                    for (int j = 0; j < count; j++) {
                        String innerName = className(cp, in.readUnsignedShort());
                        in.readUnsignedShort(); // outer_class_info_index
                        in.readUnsignedShort(); // inner_name_index
                        in.readUnsignedShort(); // inner_class_access_flags
                        if (innerName != null) innerClasses.putIfAbsent(innerName.replace('/', '.'), j);
                    }
                } else {
                    skipFully(in, length);
                }
            }
            return new ClassFileOrders(fields, innerClasses);
        }

        private static Object[] readConstantPool(DataInputStream in) throws IOException {
            int count = in.readUnsignedShort();
            Object[] cp = new Object[count];
            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> cp[i] = in.readUTF();
                    case 3, 4 -> in.readInt();
                    case 5, 6 -> { in.readLong(); i++; }
                    case 7, 8, 16, 19, 20 -> cp[i] = in.readUnsignedShort();
                    case 9, 10, 11, 12, 17, 18 -> { in.readUnsignedShort(); in.readUnsignedShort(); }
                    case 15 -> { in.readUnsignedByte(); in.readUnsignedShort(); }
                    default -> throw new IOException("Unsupported constant pool tag " + tag);
                }
            }
            return cp;
        }

        private static void skipMember(DataInputStream in) throws IOException {
            in.readUnsignedShort(); // access_flags
            in.readUnsignedShort(); // name_index
            in.readUnsignedShort(); // descriptor_index
            skipAttributes(in);
        }

        private static void skipAttributes(DataInputStream in) throws IOException {
            int attributes = in.readUnsignedShort();
            for (int i = 0; i < attributes; i++) {
                in.readUnsignedShort(); // attribute_name_index
                skipFully(in, in.readInt());
            }
        }

        private static void skipFully(DataInputStream in, int bytes) throws IOException {
            int remaining = bytes;
            while (remaining > 0) {
                int skipped = in.skipBytes(remaining);
                if (skipped <= 0) {
                    if (in.read() == -1) throw new IOException("Unexpected end of class file");
                    skipped = 1;
                }
                remaining -= skipped;
            }
        }

        private static String utf8(Object[] cp, int index) {
            if (index <= 0 || index >= cp.length) return null;
            return cp[index] instanceof String value ? value : null;
        }

        private static String className(Object[] cp, int index) {
            if (index <= 0 || index >= cp.length || !(cp[index] instanceof Integer nameIndex)) return null;
            return utf8(cp, nameIndex);
        }
    }

    private record ClassFileOrders(Map<String, Integer> fields, Map<String, Integer> innerClasses) {
        private static final ClassFileOrders EMPTY = new ClassFileOrders(Map.of(), Map.of());
    }

    private static String keyName(String value) {
        if (value == null || value.isBlank()) return "general";
        StringBuilder out = new StringBuilder();
        boolean dot = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c)) { out.append(Character.toLowerCase(c)); dot = false; }
            else if (!dot && !out.isEmpty()) { out.append('.'); dot = true; }
        }
        while (!out.isEmpty() && out.charAt(out.length() - 1) == '.') out.setLength(out.length() - 1);
        return out.isEmpty() ? "general" : out.toString();
    }

    private static List<ConfigClassEntry> entries(Collection<Class<?>> classes) {
        LinkedHashSet<Class<?>> unique = new LinkedHashSet<>(classes);
        List<ConfigClassEntry> out = new ArrayList<>();
        for (Class<?> clazz : unique) out.add(new ConfigClassEntry(clazz, null));
        return out;
    }

    private static List<ConfigClassEntry> layoutEntries(Collection<Class<?>> layoutClasses) {
        List<ConfigClassEntry> out = new ArrayList<>();
        for (Class<?> layoutClass : layoutClasses) {
            if (layoutClass == null) continue;
            Object instance = null;
            for (Field field : declaredFieldsInClassFileOrder(layoutClass)) {
                if (field.isAnnotationPresent(ConfigFooterButton.class) || field.isAnnotationPresent(ConfigFooterIcon.class)) continue;
                field.setAccessible(true);
                Object value;
                try {
                    Object target = Modifier.isStatic(field.getModifiers()) ? null : (instance == null ? instance = instantiateLayout(layoutClass) : instance);
                    value = field.get(target);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Cannot read config layout field " + layoutClass.getName() + "." + field.getName(), e);
                }
                CategorySpec category = CategorySpec.from(field.getAnnotation(ConfigCategory.class));
                addLayoutValue(out, layoutClass, field, value, category);
            }
        }
        return out;
    }

    private static Object instantiateLayout(Class<?> layoutClass) {
        try {
            var constructor = layoutClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Config layout class needs a no-arg constructor when it uses non-static fields: " + layoutClass.getName(), e);
        }
    }

    private static void addLayoutValue(List<ConfigClassEntry> out, Class<?> layoutClass, Field field, Object value, CategorySpec category) {
        if (value == null) return;
        if (value instanceof Class<?> clazz) {
            out.add(new ConfigClassEntry(clazz, category));
            return;
        }
        if (value instanceof Class<?>[] classes) {
            for (Class<?> clazz : classes) out.add(new ConfigClassEntry(clazz, category));
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (!(item instanceof Class<?> clazz)) {
                    throw new IllegalArgumentException("Config layout collection field " + layoutClass.getName() + "." + field.getName() + " must contain only Class<?> values");
                }
                out.add(new ConfigClassEntry(clazz, category));
            }
            return;
        }
        throw new IllegalArgumentException("Config layout field " + layoutClass.getName() + "." + field.getName() + " must be Class<?>, Class<?>[], or Collection<Class<?>>");
    }

    private static String cleanName(String value) {
        return value == null || value.isBlank() ? "General" : value;
    }

    private static List<Class<?>> findConfigClasses(Collection<String> packageNames) {
        if (packageNames == null || packageNames.isEmpty()) return List.of();
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String packageName : packageNames) {
            if (packageName == null) continue;
            String trimmed = packageName.trim();
            if (!trimmed.isEmpty()) normalized.add(trimmed);
        }
        if (normalized.isEmpty()) return List.of();

        List<Class<?>> classGraphResult = scanWithClassGraph(normalized);
        List<Class<?>> result = classGraphResult != null ? classGraphResult : scanWithClasspath(normalized);
        result.sort(Comparator.comparing(Class::getName));
        return result;
    }

    /**
     * Optional ClassGraph integration through reflection. ClassGraph is not a dependency of core; if another
     * mod or environment provides it, this path is used, otherwise the built-in scanner handles discovery.
     */
    private static List<Class<?>> scanWithClassGraph(Collection<String> packageNames) {
        try {
            Class<?> classGraphType = Class.forName("io.github.classgraph.ClassGraph");
            Object classGraph = classGraphType.getConstructor().newInstance();
            classGraphType.getMethod("enableClassInfo").invoke(classGraph);
            classGraphType.getMethod("enableAnnotationInfo").invoke(classGraph);
            classGraphType.getMethod("ignoreClassVisibility").invoke(classGraph);
            classGraphType.getMethod("acceptPackages", String[].class).invoke(classGraph, (Object) packageNames.toArray(String[]::new));
            Object scanResult = classGraphType.getMethod("scan").invoke(classGraph);
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                LinkedHashSet<Class<?>> out = new LinkedHashSet<>();
                collectClassGraphNames(scanResult, ConfigCategory.class.getName(), loader, out);
                for (Class<? extends Annotation> annotation : rowAnnotations()) {
                    collectClassGraphFieldOwners(scanResult, annotation.getName(), loader, out);
                }
                return filterConfigClasses(out);
            } finally {
                if (scanResult instanceof AutoCloseable closeable) closeable.close();
            }
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalStateException("ClassGraph package scan failed. Remove ClassGraph from the runtime classpath or use AutoConfig.of(...).", e);
        } catch (Exception e) {
            throw new IllegalStateException("ClassGraph package scan failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void collectClassGraphNames(Object scanResult, String annotationName, ClassLoader loader, LinkedHashSet<Class<?>> out) throws ReflectiveOperationException {
        Object classInfoList = scanResult.getClass().getMethod("getClassesWithAnnotation", String.class).invoke(scanResult, annotationName);
        List<String> names = (List<String>) classInfoList.getClass().getMethod("getNames").invoke(classInfoList);
        for (String name : names) loadIfConfigClass(name, loader, out);
    }

    @SuppressWarnings("unchecked")
    private static void collectClassGraphFieldOwners(Object scanResult, String annotationName, ClassLoader loader, LinkedHashSet<Class<?>> out) throws ReflectiveOperationException {
        Object classInfoList = scanResult.getClass().getMethod("getClassesWithFieldAnnotation", String.class).invoke(scanResult, annotationName);
        List<String> names = (List<String>) classInfoList.getClass().getMethod("getNames").invoke(classInfoList);
        for (String name : names) loadIfConfigClass(name, loader, out);
    }

    private static List<Class<?>> scanWithClasspath(Collection<String> packageNames) {
        LinkedHashSet<Class<?>> out = new LinkedHashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String packageName : packageNames) {
            String path = packageName.replace('.', '/');
            try {
                Enumeration<URL> resources = loader.getResources(path);
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    String protocol = url.getProtocol();
                    if ("file".equals(protocol)) scanDirectory(packageName, urlToFile(url), loader, out);
                    else if ("jar".equals(protocol)) scanJar(path, url, loader, out);
                }
            } catch (IOException ignored) {}
        }
        if (out.isEmpty()) scanJavaClassPath(packageNames, loader, out);
        return filterConfigClasses(out);
    }

    private static void scanJavaClassPath(Collection<String> packageNames, ClassLoader loader, LinkedHashSet<Class<?>> out) {
        String classPath = System.getProperty("java.class.path", "");
        if (classPath.isBlank()) return;
        String[] entries = classPath.split(File.pathSeparator);
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) continue;
            File file = new File(entry);
            if (!file.exists()) continue;
            for (String packageName : packageNames) {
                String path = packageName.replace('.', File.separatorChar);
                if (file.isDirectory()) scanDirectory(packageName, new File(file, path), loader, out);
                else if (file.isFile() && file.getName().endsWith(".jar")) scanJarFile(file, packageName.replace('.', '/'), loader, out);
            }
        }
    }

    private static File urlToFile(URL url) {
        try { return new File(url.toURI()); }
        catch (Exception ignored) { return new File(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8)); }
    }

    private static void scanDirectory(String packageName, File dir, ClassLoader loader, LinkedHashSet<Class<?>> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            if (file.isDirectory()) scanDirectory(packageName + "." + file.getName(), file, loader, out);
            else if (file.getName().endsWith(".class")) {
                String simpleName = file.getName().substring(0, file.getName().length() - 6);
                loadIfConfigClass(packageName + "." + simpleName, loader, out);
            }
        }
    }

    private static void scanJar(String path, URL url, ClassLoader loader, LinkedHashSet<Class<?>> out) {
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jar = connection.getJarFile()) { scanJarEntries(jar, path, loader, out); }
        } catch (ClassCastException | IOException ignored) {
            String external = url.toExternalForm();
            int separator = external.indexOf("!/");
            if (separator >= 0 && external.startsWith("jar:")) {
                String jarPart = external.substring(4, separator);
                try { scanJarFile(new File(URI.create(jarPart)), path, loader, out); }
                catch (Exception ignoredAgain) {}
            }
        }
    }

    private static void scanJarFile(File file, String path, ClassLoader loader, LinkedHashSet<Class<?>> out) {
        try (JarFile jar = new JarFile(file)) { scanJarEntries(jar, path, loader, out); }
        catch (IOException ignored) {}
    }

    private static void scanJarEntries(JarFile jar, String path, ClassLoader loader, LinkedHashSet<Class<?>> out) {
        Enumeration<JarEntry> entries = jar.entries();
        String prefix = path.endsWith("/") ? path : path + "/";
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (name.startsWith(prefix) && name.endsWith(".class")) {
                loadIfConfigClass(name.substring(0, name.length() - 6).replace('/', '.'), loader, out);
            }
        }
    }

    private static void loadIfConfigClass(String className, ClassLoader loader, LinkedHashSet<Class<?>> out) {
        if (className == null || className.endsWith("module-info") || className.endsWith("package-info")) return;
        try {
            Class<?> clazz = Class.forName(className, false, loader);
            if (isConfigContainer(clazz)) out.add(topLevelConfigClass(clazz));
        } catch (LinkageError | ClassNotFoundException ignored) {}
    }

    private static List<Class<?>> filterConfigClasses(Collection<Class<?>> classes) {
        LinkedHashSet<Class<?>> out = new LinkedHashSet<>();
        for (Class<?> clazz : classes) {
            Class<?> top = topLevelConfigClass(clazz);
            if (isConfigContainer(top)) out.add(top);
        }
        return new ArrayList<>(out);
    }

    private static Class<?> topLevelConfigClass(Class<?> clazz) {
        Class<?> current = clazz;
        while (current.getDeclaringClass() != null) current = current.getDeclaringClass();
        return current;
    }

    private static boolean isConfigContainer(Class<?> clazz) {
        if (clazz == null || clazz.isSynthetic() || clazz.isAnonymousClass() || clazz.isLocalClass()) return false;
        if (clazz.isAnnotationPresent(ConfigCategory.class)) return true;
        for (Field field : clazz.getDeclaredFields()) if (isConfigRowField(field)) return true;
        for (Class<?> nested : clazz.getDeclaredClasses()) {
            if (isConfigContainer(nested)) return true;
        }
        return false;
    }

    private static boolean isConfigRowField(Field field) {
        for (Class<? extends Annotation> annotation : rowAnnotations()) {
            if (field.isAnnotationPresent(annotation)) return true;
        }
        return false;
    }

    private static List<Class<? extends Annotation>> rowAnnotations() {
        List<Class<? extends Annotation>> annotations = new ArrayList<>(List.of(
                ConfigToggle.class,
                ConfigCheckbox.class,
                ConfigSlider.class,
                ConfigSliderLabel.class,
                ConfigNumber.class,
                ConfigText.class,
                ConfigColor.class,
                ConfigKeybind.class,
                ConfigDropdown.class,
                ConfigSearchableDropdown.class,
                ConfigGroupedDropdown.class,
                ConfigSearchableGroupedDropdown.class,
                ConfigMultiSelectDropdown.class,
                ConfigSearchableMultiSelectDropdown.class,
                ConfigDraggableList.class,
                ConfigCustomList.class,
                ConfigInfo.class,
                ConfigButton.class,
                ConfigCustom.class,
                ConfigCustomOption.class,
                ConfigSeparator.class,
                ConfigLabeledSeparator.class,
                ConfigSpacer.class,
                ConfigFooterButton.class,
                ConfigFooterIcon.class,
                ConfigAccordion.class
        ));
        for (RegisteredComponent<?, ?> registered : CUSTOM_COMPONENTS.values()) annotations.add(registered.annotationType());
        return annotations;
    }
}
