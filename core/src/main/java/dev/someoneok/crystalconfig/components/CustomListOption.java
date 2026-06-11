package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.containers.Column;
import dev.someoneok.crystalconfig.input.MouseButton;
import dev.someoneok.crystalconfig.input.MouseButtonEvent;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** List option shell for rows supplied by the caller. */
public class CustomListOption<T> extends Component {
    private final State<List<T>> value;
    private final EntryFactory<T> entryFactory;
    private final RowFactory<T> rowFactory;
    private final Column content = new Column().fillX();
    private String addButtonText = "+ Add entry";
    private boolean allowEmpty = true;
    private float rowGap = 6.0f;
    private float addButtonHeight = 30.0f;
    private boolean rebuilding;
    private boolean suppressSubscriptionRebuild;
    @SuppressWarnings("FieldCanBeLocal")
    private final AutoCloseable subscription;

    public CustomListOption(State<List<T>> value, EntryFactory<T> entryFactory, RowFactory<T> rowFactory) {
        this.value = Objects.requireNonNull(value, "value");
        this.entryFactory = Objects.requireNonNull(entryFactory, "entryFactory");
        this.rowFactory = Objects.requireNonNull(rowFactory, "rowFactory");
        add(content);
        subscription = value.subscribe(ignored -> {
            if (!rebuilding && !suppressSubscriptionRebuild) rebuild();
        });
        rebuild();
    }

    public CustomListOption<T> addButtonText(String addButtonText) {
        this.addButtonText = addButtonText == null || addButtonText.isBlank() ? "+ Add entry" : addButtonText;
        rebuild();
        return this;
    }

    public CustomListOption<T> allowEmpty(boolean allowEmpty) {
        this.allowEmpty = allowEmpty;
        rebuild();
        return this;
    }

    public CustomListOption<T> rowGap(float rowGap) {
        this.rowGap = Math.max(0, rowGap);
        rebuild();
        return this;
    }

    public CustomListOption<T> addButtonHeight(float addButtonHeight) {
        this.addButtonHeight = Math.max(22, addButtonHeight);
        rebuild();
        return this;
    }

    public State<List<T>> state() { return value; }

    /** Wraps a row input with a compact label. */
    public static Component field(String name, Component input) {
        Objects.requireNonNull(input, "input");
        return new FieldColumn(name, input).fillX();
    }

    /** Alias for {@link #field(String, Component)}. */
    public static Component labeled(String name, Component input) {
        return field(name, input);
    }

    private void rebuild() {
        rebuilding = true;
        try {
            content.clearChildren();
            content.gap(rowGap);

            List<T> items = current();
            for (int i = 0; i < items.size(); i++) {
                content.add(createRow(i, items.get(i)));
            }

            content.add(new Button(addButtonText)
                    .onClick(this::addEntry)
                    .height(addButtonHeight)
                    .fillX());
            markLayoutDirty();
        } finally {
            rebuilding = false;
        }
    }

    private Component createRow(int index, T initialValue) {
        RowState rowState = new RowState(index, initialValue);
        RowContext<T> context = new RowContext<>(index, rowState, this::removeEntry);
        Component rowContent = rowFactory.create(context);
        if (rowContent == null) throw new IllegalArgumentException("Custom list row factory returned null at index " + index);

        DeleteIconButton deleteButton = new DeleteIconButton(() -> removeEntry(index))
                .enabled(allowEmpty || current().size() > 1)
                .tooltip("Remove");
        return new EntryRow(rowContent, deleteButton).fillX();
    }

    private void addEntry() {
        List<T> next = current();
        next.add(entryFactory.create());
        commit(next, true);
    }

    private void removeEntry(int index) {
        List<T> next = current();
        if (index < 0 || index >= next.size()) return;
        if (!allowEmpty && next.size() <= 1) return;
        next.remove(index);
        commit(next, true);
    }

    private List<T> current() {
        List<T> raw = value.get();
        return raw == null ? new ArrayList<>() : new ArrayList<>(raw);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return content.measure(context, constraints);
    }

    @Override
    protected void layoutChildren(LayoutContext context) {
        content.layout(context, bounds.inset(padding.left(), padding.top(), padding.right(), padding.bottom()));
    }

    private final class RowState implements State<T> {
        private final int index;
        private T snapshot;

        private RowState(int index, T snapshot) {
            this.index = index;
            this.snapshot = snapshot;
        }

        @Override
        public T get() {
            List<T> items = current();
            if (index >= 0 && index < items.size()) {
                snapshot = items.get(index);
            }
            return snapshot;
        }

        @Override
        public void set(T newValue) {
            List<T> items = current();
            if (index < 0 || index >= items.size()) return;
            snapshot = newValue;
            items.set(index, newValue);
            commit(items, false);
        }
    }

    private void commit(List<T> items, boolean rebuildAfterCommit) {
        suppressSubscriptionRebuild = true;
        try {
            value.set(new ArrayList<>(items));
        } finally {
            suppressSubscriptionRebuild = false;
        }
        if (rebuildAfterCommit) rebuild();
    }

    private static final class FieldColumn extends Column {
        private final String name;
        private final Component input;
        private static final float LABEL_HEIGHT = 13.0f;
        private static final float GAP = 4.0f;

        private FieldColumn(String name, Component input) {
            this.name = name == null ? "" : name;
            this.input = input;
            gap(GAP);
            add(input.fillX());
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            Size inputSize = input.measure(context, new Constraints(constraints.maxWidth(), constraints.maxHeight()));
            float height = inputSize.height() + (name.isBlank() ? 0 : LABEL_HEIGHT + GAP);
            return constraints.clamp(new Size(inputSize.width(), height));
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            float labelOffset = name.isBlank() ? 0 : LABEL_HEIGHT + GAP;
            input.layout(context, new Rect(bounds.x(), bounds.y() + labelOffset, bounds.w(), Math.max(0, bounds.h() - labelOffset)));
        }

        @Override
        protected void renderSelf(RenderContext context) {
            if (name.isBlank()) return;
            context.text(name, bounds.x() + 2, bounds.y(), context.theme().fonts().small(), context.theme().palette().mutedText(), z + 1);
        }
    }

    private static final class DeleteIconButton extends Component {
        private static final String TRASH_ICON = "✕";
        private final Runnable action;

        private DeleteIconButton(Runnable action) {
            this.action = action == null ? () -> { } : action;
            width(22);
            height(22);
            minSize(22, 22);
            focusable(true);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            return constraints.clamp(new Size(Math.max(minWidth, preferredWidth), Math.max(minHeight, preferredHeight)));
        }

        @Override
        protected void renderSelf(RenderContext context) {
            float font = context.theme().fonts().normal() + 2.0f;
            String face = context.theme().fonts().semibold();
            ColorRGBA color = !enabled()
                    ? context.theme().palette().mutedText().withAlpha(90)
                    : hovered() ? context.theme().palette().accent() : context.theme().palette().mutedText();

            context.text(
                    TRASH_ICON,
                    bounds.centerX() - context.measureText(TRASH_ICON, font, face).width() * 0.5f,
                    bounds.centerY() - context.lineHeight(font, face) * 0.5f,
                    font,
                    face,
                    color,
                    z + 1
            );
        }

        @Override
        public boolean onMousePressed(MouseButtonEvent event) {
            if (!enabled() || event.button != MouseButton.LEFT) return false;
            pressed = true;
            return true;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent event) {
            boolean wasPressed = pressed;
            pressed = false;
            if (wasPressed && enabled() && event.button == MouseButton.LEFT && bounds.contains(event.x, event.y)) {
                action.run();
                return true;
            }
            return wasPressed;
        }
    }

    private static final class EntryRow extends Component {
        private static final float PAD_X = 6.0f;
        private static final float PAD_Y = 5.0f;
        private static final float DELETE_SIZE = 22.0f;
        private static final float DELETE_TOP = 6.0f;
        private static final float DELETE_RIGHT = 8.0f;
        private static final float CONTENT_RIGHT_RESERVE = 26.0f;
        private final Component rowContent;
        private final Component deleteButton;

        private EntryRow(Component rowContent, Component deleteButton) {
            this.rowContent = Objects.requireNonNull(rowContent, "rowContent");
            this.deleteButton = Objects.requireNonNull(deleteButton, "deleteButton");
            add(rowContent.fillX());
            add(deleteButton);
        }

        @Override
        protected Size measureSelf(LayoutContext context, Constraints constraints) {
            float contentWidth = Math.max(0, constraints.maxWidth() - PAD_X * 2 - CONTENT_RIGHT_RESERVE);
            Size contentSize = rowContent.measure(context, new Constraints(contentWidth, constraints.maxHeight()));
            float width = preferredWidth >= 0 ? preferredWidth : contentSize.width() + PAD_X * 2 + CONTENT_RIGHT_RESERVE;
            float height = Math.max(contentSize.height() + PAD_Y * 2, DELETE_TOP + DELETE_SIZE + PAD_Y);
            return constraints.clamp(new Size(width, height));
        }

        @Override
        protected void layoutChildren(LayoutContext context) {
            float contentX = bounds.x() + PAD_X;
            float contentY = bounds.y() + PAD_Y;
            float contentW = Math.max(0, bounds.w() - PAD_X * 2 - CONTENT_RIGHT_RESERVE);
            float contentH = Math.max(0, bounds.h() - PAD_Y * 2);
            rowContent.layout(context, new Rect(contentX, contentY, contentW, contentH));

            float deleteX = bounds.right() - DELETE_RIGHT - DELETE_SIZE;
            float deleteY = bounds.y() + DELETE_TOP;
            deleteButton.layout(context, new Rect(deleteX, deleteY, DELETE_SIZE, DELETE_SIZE));
        }

        @Override
        protected void renderSelf(RenderContext context) {
            ColorRGBA fill = hovered()
                    ? context.theme().palette().surfaceHover().withAlpha(190)
                    : context.theme().palette().surfaceAlt().withAlpha(155);
            context.rect(bounds, SdfRectStyle.create()
                    .fill(fill)
                    .border(1, context.theme().palette().border())
                    .radius(context.theme().radii().md()), z);
        }
    }

    @FunctionalInterface
    public interface EntryFactory<T> {
        T create();
    }

    @FunctionalInterface
    public interface RowFactory<T> {
        Component create(RowContext<T> context);
    }

    public record RowContext<T>(int index, State<T> state, RowRemoveAction removeAction) {
        public void remove() { removeAction.remove(index); }
    }

    @FunctionalInterface
    public interface RowRemoveAction {
        void remove(int index);
    }
}
