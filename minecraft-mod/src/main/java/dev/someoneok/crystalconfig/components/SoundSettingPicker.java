package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.input.*;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.models.SoundSetting;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.state.MutableState;
import dev.someoneok.crystalconfig.state.State;
import dev.someoneok.crystalconfig.ui.Component;
import dev.someoneok.crystalconfig.util.MinecraftSounds;
import dev.someoneok.crystalconfig.utils.MathUtil;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SoundSettingPicker extends Component {
    private static final float OVERLAY_Z_BASE = 10000.0f;
    private static final float HEADER_HEIGHT = 30;
    private static final float PANEL_GAP = 5;
    private static final float PANEL_WIDTH = 430;
    private static final float PANEL_PADDING = 10;
    private static final float SEARCH_HEIGHT = 28;
    private static final float NUMBER_HEIGHT = 28;
    private static final float ITEM_HEIGHT = 24;
    private static final float SEPARATOR_HEIGHT = 1;
    private static final float SCROLLBAR_W = 6;
    private static final int MAX_VISIBLE_ROWS = 6;
    private static final float ANIMATION_SPEED = 18.0f;

    private enum ButtonId { CLEAR, PLAY, SELECT }

    private final State<SoundSetting> value;
    private final MutableState<String> search = new MutableState<>("");
    private final MutableState<Double> volumeState = new MutableState<>((double) SoundSetting.DEFAULT_VOLUME);
    private final MutableState<Double> pitchState = new MutableState<>((double) SoundSetting.DEFAULT_PITCH);
    private final TextInput searchInput = new TextInput(search).placeholder("Search sounds... e.g. ui.button");
    private final NumberInput<Double> volumeInput = new NumberInput<>(volumeState, SoundSetting.MIN_VOLUME, SoundSetting.MAX_VOLUME, 0.05);
    private final NumberInput<Double> pitchInput = new NumberInput<>(pitchState, SoundSetting.MIN_PITCH, SoundSetting.MAX_PITCH, 0.05);
    private final List<Identifier> allSounds = new ArrayList<>();
    private final List<Identifier> filtered = new ArrayList<>();

    private boolean allowNone = true;
    private boolean expanded;
    private float overlayAnim;
    private float scroll;
    private int hoveredIndex = -1;
    private Identifier draftSound;
    private String lastSearch = null;
    private long lastRefreshNanos;
    private boolean draggingScrollbar;
    private float scrollbarGrabY;
    private ButtonId pressedButton;
    private float mouseX;
    private float mouseY;

    public SoundSettingPicker(State<SoundSetting> value) {
        this.value = Objects.requireNonNull(value, "value");
        this.focusable = true;
        searchInput.height(SEARCH_HEIGHT);
        volumeInput.size(92, NUMBER_HEIGHT);
        pitchInput.size(92, NUMBER_HEIGHT);
        searchInput.visible(false);
        volumeInput.visible(false);
        pitchInput.visible(false);
        add(searchInput);
        add(volumeInput);
        add(pitchInput);
        size(260, HEADER_HEIGHT);
    }

    public SoundSettingPicker allowNone(boolean allowNone) {
        this.allowNone = allowNone;
        return this;
    }

    public SoundSettingPicker requireSound() {
        return allowNone(false);
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return constraints.clamp(new Size(preferredWidth >= 0 ? preferredWidth : 260, preferredHeight >= 0 ? preferredHeight : HEADER_HEIGHT));
    }

    @Override
    protected void layoutChildren(LayoutContext context) {
        layoutOverlayInputs(panelRect(1.0f));
    }

    @Override
    public void tick(float deltaSeconds) {
        if (!enabled()) {
            expanded = false;
            overlayAnim = 0.0f;
            hoveredIndex = -1;
            pressedButton = null;
            draggingScrollbar = false;
            focusOnly(null);
            setFocused(false);
            setHovered(false);
            searchInput.visible(false);
            volumeInput.visible(false);
            pitchInput.visible(false);
        }
        if (enabled() && expanded) ensureSoundsLoaded(false);
        refreshFilter();
        clampScroll();
        float target = expanded ? 1.0f : 0.0f;
        float step = Math.max(0.0f, deltaSeconds) * ANIMATION_SPEED;
        overlayAnim += (target - overlayAnim) * MathUtil.clamp(step, 0.0f, 1.0f);
        if (!expanded && overlayAnim < 0.01f) overlayAnim = 0.0f;
        if (expanded && overlayAnim > 0.99f) overlayAnim = 1.0f;
        syncDraftFromNumberInputs();
        super.tick(deltaSeconds);
    }

    @Override
    public boolean needsFreshRender() {
        return super.needsFreshRender() || expanded || overlayAnim > 0.0f || draggingScrollbar;
    }

    @Override
    public void render(RenderContext context) {
        if (!visible()) return;
        renderSelf(context);
        if (context.debugBounds()) {
            context.rect(bounds, SdfRectStyle.create()
                    .fill(ColorRGBA.TRANSPARENT)
                    .border(1, ColorRGBA.rgba(255, 0, 180, 120))
                    .radius(0), z + 5000);
        }
    }

    @Override
    protected void renderSelf(RenderContext context) {
        boolean disabled = !enabled();
        Rect header = headerRect();
        ColorRGBA fill = disabled
                ? context.theme().palette().surfaceAlt().withAlpha(120)
                : hovered || focused || expanded ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt();
        ColorRGBA border = disabled
                ? context.theme().palette().border().withAlpha(95)
                : expanded || focused ? context.theme().palette().accent() : context.theme().palette().border();
        context.rect(header, SdfRectStyle.create()
                .fill(fill)
                .border(1, border)
                .radius(context.theme().radii().md()), z);

        float font = context.theme().fonts().normal();
        String label = currentLabel();
        if (!allowNone && (value.get() == null || value.get().sound() == null)) label = "Select a sound...";
        String shown = ellipsize(context, label, font, Math.max(8, header.w() - 20));
        float y = header.centerY() - context.lineHeight(font) * 0.5f;
        context.pushClip(header.inset(8, 0, 8, 0));
        context.text(shown, header.x() + 10, y, font,
                disabled || (!allowNone && (value.get() == null || value.get().sound() == null))
                        ? context.theme().palette().mutedText().withAlpha(disabled ? 135 : 255)
                        : context.theme().palette().text(), z + 1);
        context.popClip();
    }

    @Override
    public void renderOverlay(RenderContext context) {
        super.renderOverlay(context);
        if (!enabled() || overlayAnim <= 0.0f) return;
        renderPanel(context, context.theme().fonts().normal(), easeOutCubic(overlayAnim));
    }

    private void renderPanel(RenderContext context, float font, float anim) {
        Rect panel = panelRect(anim);
        if (panel.h() <= 0.5f) return;
        int alpha = Math.round(255.0f * anim);
        float panelZ = z + OVERLAY_Z_BASE + 50;
        ColorRGBA surface = context.theme().palette().surface().withAlpha(alpha);
        ColorRGBA border = context.theme().palette().border().withAlpha(alpha);
        context.rect(panel, SdfRectStyle.create()
                .fill(surface)
                .border(1, border)
                .shadow(12 * anim, 0, 3, context.theme().palette().shadow().multiplyAlpha(0.62f * anim))
                .radius(context.theme().radii().md()), panelZ);

        layoutOverlayInputs(panel);
        searchInput.z(panelZ + 5);
        volumeInput.z(panelZ + 5);
        pitchInput.z(panelZ + 5);
        boolean showInputs = expanded;
        searchInput.visible(showInputs);
        volumeInput.visible(showInputs);
        pitchInput.visible(showInputs);

        Rect panelClip = panel.inset(1, 1, 1, 1);
        context.pushClip(panelClip);
        searchInput.render(context);

        Rect list = listRect(panel);
        context.rect(list, SdfRectStyle.create().fill(context.theme().palette().surfaceAlt().withAlpha(Math.round(95 * anim))).radius(context.theme().radii().sm()), panelZ + 1);
        context.pushClip(list);
        if (filtered.isEmpty()) {
            String empty = allSounds.isEmpty() ? "No sounds available" : "No results";
            float w = context.measureText(empty, font).width();
            context.text(empty, list.centerX() - w * 0.5f, list.centerY() - context.lineHeight(font) * 0.5f, font, context.theme().palette().mutedText().withAlpha(alpha), panelZ + 3);
        } else {
            int start = Math.max(0, (int) (scroll / ITEM_HEIGHT));
            int end = Math.min(filtered.size(), start + visibleRows() + 2);
            float offset = scroll - start * ITEM_HEIGHT;
            for (int i = start; i < end; i++) {
                Identifier id = filtered.get(i);
                Rect item = new Rect(list.x(), list.y() + (i - start) * ITEM_HEIGHT - offset, list.w(), ITEM_HEIGHT);
                if (item.bottom() < list.y() || item.y() > list.bottom()) continue;
                boolean hoveredItem = i == hoveredIndex;
                boolean selected = Objects.equals(id, draftSound);
                if (hoveredItem) context.rect(item.inset(2, 1, 2, 1), SdfRectStyle.create().fill(context.theme().palette().surfaceHover().withAlpha(Math.round(170 * anim))).radius(context.theme().radii().sm()), panelZ + 2);
                if (selected) context.rect(item.inset(2, 1, 2, 1), SdfRectStyle.create().fill(context.theme().palette().accent().withAlpha(Math.round(58 * anim))).radius(context.theme().radii().sm()), panelZ + 3);
                String text = ellipsize(context, id.toShortString(), font, Math.max(8, item.w() - 16));
                context.text(text, item.x() + 8, item.centerY() - context.lineHeight(font) * 0.5f, font, (selected ? context.theme().palette().accent() : context.theme().palette().text()).withAlpha(alpha), panelZ + 4);
            }
        }
        context.popClip();

        if (needsScrollbar()) renderScrollbar(context, panel, panelZ + 7, anim);

        Rect separator = separatorRect(panel);
        context.rect(separator, SdfRectStyle.create().fill(context.theme().palette().border().withAlpha(Math.round(170 * anim))).radius(0), panelZ + 5);

        String selectedText = draftSound == null
                ? (allowNone ? "None selected" : "Sound required")
                : draftSound.toShortString();
        context.text(ellipsize(context, selectedText, font, Math.max(8, panel.w() - PANEL_PADDING * 2)),
                panel.x() + PANEL_PADDING, selectedLabelY(panel), font,
                draftSound == null ? context.theme().palette().mutedText().withAlpha(alpha) : context.theme().palette().accent().withAlpha(alpha), panelZ + 5);

        renderNumberLabel(context, "Vol", volumeInput.bounds(), font, panelZ + 5, alpha);
        renderNumberLabel(context, "Pitch", pitchInput.bounds(), font, panelZ + 5, alpha);
        volumeInput.render(context);
        pitchInput.render(context);
        renderButtons(context, panel, font, panelZ + 6, anim);
        context.popClip();
    }

    private void renderScrollbar(RenderContext context, Rect panel, float z, float anim) {
        Rect track = scrollbarTrack(panel);
        Rect thumb = scrollbarThumb(panel);
        context.rect(track, SdfRectStyle.create().fill(context.theme().palette().surfaceAlt().withAlpha(Math.round(115 * anim))).radius(context.theme().radii().sm()), z);
        context.rect(thumb, SdfRectStyle.create().fill((draggingScrollbar ? context.theme().palette().accent() : context.theme().palette().surfaceActive()).withAlpha(Math.round(210 * anim))).radius(context.theme().radii().sm()), z + 1);
    }

    private void renderNumberLabel(RenderContext context, String label, Rect field, float font, float z, int alpha) {
        context.text(label, field.x(), field.y() - context.lineHeight(font) - 3, font, context.theme().palette().mutedText().withAlpha(alpha), z);
    }

    private void renderButtons(RenderContext context, Rect panel, float font, float panelZ, float anim) {
        renderActionButton(context, clearButton(panel), "Clear", font, panelZ, false, clearDisabled(), ButtonId.CLEAR, anim);
        renderActionButton(context, playButton(panel), "Play", font, panelZ, false, draftSound == null, ButtonId.PLAY, anim);
        renderActionButton(context, selectButton(panel), "Select", font, panelZ, true, selectDisabled(), ButtonId.SELECT, anim);
    }

    private void renderActionButton(RenderContext context, Rect rect, String text, float font, float panelZ, boolean accent, boolean disabled, ButtonId id, float anim) {
        boolean hover = !disabled && rect.contains(mouseX, mouseY);
        boolean down = hover && pressedButton == id;
        ColorRGBA fill;
        ColorRGBA border;
        ColorRGBA textColor;
        if (disabled) {
            fill = context.theme().palette().surfaceAlt().withAlpha(Math.round(80 * anim));
            border = context.theme().palette().border().withAlpha(Math.round(100 * anim));
            textColor = context.theme().palette().mutedText().withAlpha(Math.round(130 * anim));
        } else if (accent) {
            fill = (down ? context.theme().palette().accent().darken(0.18f) : hover ? context.theme().palette().accent().lighten(0.10f) : context.theme().palette().accent()).withAlpha(Math.round(255 * anim));
            border = context.theme().palette().accent().lighten(0.18f).withAlpha(Math.round(255 * anim));
            textColor = context.theme().palette().accentText().withAlpha(Math.round(255 * anim));
        } else {
            fill = (down ? context.theme().palette().surfaceActive() : hover ? context.theme().palette().surfaceHover() : context.theme().palette().surfaceAlt()).withAlpha(Math.round(255 * anim));
            border = (hover ? context.theme().palette().accent() : context.theme().palette().border()).withAlpha(Math.round(255 * anim));
            textColor = context.theme().palette().text().withAlpha(Math.round(255 * anim));
        }
        context.rect(rect, SdfRectStyle.create().fill(fill).border(1, border).radius(context.theme().radii().sm()), panelZ);
        float w = context.measureText(text, font).width();
        context.text(text, rect.centerX() - w * 0.5f, rect.centerY() - context.lineHeight(font) * 0.5f + (down ? 1 : 0), font, textColor, panelZ + 1);
    }

    @Override
    public Component hitTest(float x, float y) {
        if (!visible() || !enabled()) return null;
        if (headerRect().contains(x, y)) return this;
        if ((expanded || overlayAnim > 0.0f) && panelRect(1.0f).contains(x, y)) return this;
        hoveredIndex = -1;
        return null;
    }

    @Override
    public Component hitTestOverlay(float x, float y) {
        if (!visible() || !enabled()) return null;
        return (expanded || overlayAnim > 0.0f) && panelRect(1.0f).contains(x, y) ? this : super.hitTestOverlay(x, y);
    }

    @Override
    public boolean onMouseMove(MouseMoveEvent event) {
        if (!enabled()) return false;
        mouseX = event.x;
        mouseY = event.y;
        if (draggingScrollbar) {
            setScrollFromMouseY(event.y);
            hoveredIndex = itemIndexAt(event.x, event.y);
            return true;
        }
        hoveredIndex = expanded ? itemIndexAt(event.x, event.y) : -1;
        return expanded && panelRect(1.0f).contains(event.x, event.y);
    }

    @Override
    public boolean onMouseScrolled(MouseScrollEvent event) {
        if (!enabled()) return false;
        mouseX = event.x;
        mouseY = event.y;
        if (!expanded || !needsScrollbar()) return false;
        Rect panel = panelRect(1.0f);
        if (!listAndScrollbarRect(panel).contains(event.x, event.y)) return false;
        scroll -= event.amountY * ITEM_HEIGHT * 3.0f;
        clampScroll();
        hoveredIndex = itemIndexAt(event.x, event.y);
        return true;
    }

    @Override
    public boolean onMouseDragged(MouseDragEvent event) {
        if (!enabled()) return false;
        mouseX = event.x;
        mouseY = event.y;
        if (draggingScrollbar) {
            setScrollFromMouseY(event.y);
            return true;
        }
        if (expanded && searchInput.focused() && searchInput.onMouseDragged(event)) return true;
        if (expanded && volumeInput.focused() && volumeInput.onMouseDragged(event)) return true;
        return expanded && pitchInput.focused() && pitchInput.onMouseDragged(event);
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (!enabled()) return false;
        mouseX = event.x;
        mouseY = event.y;
        if (event.button != MouseButton.LEFT) return false;
        if (headerRect().contains(event.x, event.y)) {
            if (expanded) close(); else open();
            return true;
        }
        if (!expanded) return false;
        Rect panel = panelRect(1.0f);
        if (!panel.contains(event.x, event.y)) {
            close();
            return false;
        }
        if (searchInput.bounds().contains(event.x, event.y)) {
            focusOnly(searchInput);
            return searchInput.onMousePressed(event);
        }
        if (volumeInput.bounds().contains(event.x, event.y)) {
            focusOnly(volumeInput);
            return volumeInput.onMousePressed(event);
        }
        if (pitchInput.bounds().contains(event.x, event.y)) {
            focusOnly(pitchInput);
            return pitchInput.onMousePressed(event);
        }
        focusOnly(null);
        if (needsScrollbar() && scrollbarTrack(panel).contains(event.x, event.y)) {
            Rect thumb = scrollbarThumb(panel);
            scrollbarGrabY = thumb.contains(event.x, event.y) ? event.y - thumb.y() : thumb.h() * 0.5f;
            draggingScrollbar = true;
            setScrollFromMouseY(event.y);
            return true;
        }
        int item = itemIndexAt(event.x, event.y);
        if (item >= 0 && item < filtered.size()) {
            draftSound = filtered.get(item);
            ensureVisible(item);
            return true;
        }
        pressedButton = buttonAt(panel, event.x, event.y);
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        if (!enabled()) return false;
        mouseX = event.x;
        mouseY = event.y;
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        if (!expanded) {
            pressedButton = null;
            return false;
        }
        ButtonId released = buttonAt(panelRect(1.0f), event.x, event.y);
        ButtonId pressed = pressedButton;
        pressedButton = null;
        if (pressed != null && pressed == released) {
            handleButton(pressed);
            return true;
        }
        if (searchInput.onMouseReleased(event)) return true;
        if (volumeInput.onMouseReleased(event)) return true;
        return pitchInput.onMouseReleased(event);
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (!enabled()) return false;
        if (!expanded && (event.keyCode == KeyCodes.ENTER || event.keyCode == KeyCodes.SPACE)) {
            open();
            return true;
        }
        if (!expanded) return false;
        if (event.keyCode == KeyCodes.ESCAPE) {
            close();
            return true;
        }
        if (searchInput.focused()) {
            if (event.keyCode == KeyCodes.DOWN || event.keyCode == KeyCodes.UP) searchInput.setFocused(false);
            else if (searchInput.onKeyPressed(event)) { refreshFilter(); return true; }
        }
        if (volumeInput.focused() && volumeInput.onKeyPressed(event)) { syncDraftFromNumberInputs(); return true; }
        if (pitchInput.focused() && pitchInput.onKeyPressed(event)) { syncDraftFromNumberInputs(); return true; }
        int idx = Math.max(0, filtered.indexOf(draftSound));
        if (event.keyCode == KeyCodes.DOWN && !filtered.isEmpty()) {
            int next = Math.min(filtered.size() - 1, idx + 1);
            draftSound = filtered.get(next);
            ensureVisible(next);
            return true;
        }
        if (event.keyCode == KeyCodes.UP && !filtered.isEmpty()) {
            int next = Math.max(0, idx - 1);
            draftSound = filtered.get(next);
            ensureVisible(next);
            return true;
        }
        if (event.keyCode == KeyCodes.ENTER && !selectDisabled()) {
            commitAndClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCharTyped(CharTypedEvent event) {
        if (!enabled()) return false;
        if (!expanded) return false;
        if (searchInput.focused()) {
            boolean consumed = searchInput.onCharTyped(event);
            refreshFilter();
            return consumed;
        }
        if (volumeInput.focused()) return volumeInput.onCharTyped(event);
        if (pitchInput.focused()) return pitchInput.onCharTyped(event);
        return false;
    }

    @Override
    protected void onFocusChanged(boolean focused) {
        if (!focused) close();
    }

    private void open() {
        SoundSetting current = value.get() == null ? SoundSetting.none() : value.get();
        draftSound = current.sound();
        volumeState.set((double) current.volume());
        pitchState.set((double) current.pitch());
        search.set("");
        lastSearch = null;
        ensureSoundsLoaded(true);
        refreshFilter();
        if (!allowNone && draftSound == null && !filtered.isEmpty()) {
            draftSound = filtered.get(0);
        }
        scrollToDraftSound();
        expanded = true;
        searchInput.visible(true);
        volumeInput.visible(true);
        pitchInput.visible(true);
        focusOnly(searchInput);
        hoveredIndex = -1;
        pressedButton = null;
        draggingScrollbar = false;
        markLayoutDirty();
    }

    private void close() {
        expanded = false;
        hoveredIndex = -1;
        pressedButton = null;
        draggingScrollbar = false;
        focusOnly(null);
        markLayoutDirty();
    }

    private void commitAndClose() {
        focusOnly(null);
        syncDraftFromNumberInputs();
        if (selectDisabled()) return;
        value.set(new SoundSetting(draftSound, volumeState.get().floatValue(), pitchState.get().floatValue()));
        close();
    }

    private void handleButton(ButtonId button) {
        if (button == null) return;
        switch (button) {
            case CLEAR -> {
                if (!clearDisabled()) draftSound = null;
            }
            case PLAY -> {
                if (draftSound != null) MinecraftSounds.playPreview(draftSound, volumeState.get().floatValue(), pitchState.get().floatValue());
            }
            case SELECT -> commitAndClose();
        }
    }

    private boolean clearDisabled() { return !allowNone || draftSound == null; }
    private boolean selectDisabled() { return !allowNone && draftSound == null; }

    private void syncDraftFromNumberInputs() {
        volumeState.set((double) MathUtil.clamp(volumeState.get().floatValue(), SoundSetting.MIN_VOLUME, SoundSetting.MAX_VOLUME));
        pitchState.set((double) MathUtil.clamp(pitchState.get().floatValue(), SoundSetting.MIN_PITCH, SoundSetting.MAX_PITCH));
    }

    private String currentLabel() {
        SoundSetting current = value.get();
        return current == null ? "None" : current.displayName();
    }

    private void ensureSoundsLoaded(boolean force) {
        long now = System.nanoTime();
        if (!force && !allSounds.isEmpty() && now - lastRefreshNanos < 5_000_000_000L) return;
        List<Identifier> loaded = MinecraftSounds.availableSounds();
        if (!loaded.isEmpty() && !loaded.equals(allSounds)) {
            allSounds.clear();
            allSounds.addAll(loaded);
            lastSearch = null;
        }
        lastRefreshNanos = now;
    }

    private void refreshFilter() {
        String query = search.get() == null ? "" : search.get().trim().toLowerCase(Locale.ROOT);
        if (Objects.equals(query, lastSearch)) return;
        boolean searchChanged = lastSearch != null && !Objects.equals(query, lastSearch);
        lastSearch = query;
        filtered.clear();
        for (Identifier id : allSounds) {
            if (query.isEmpty() || id.toString().toLowerCase(Locale.ROOT).contains(query)) filtered.add(id);
        }
        if (searchChanged) scroll = 0;
        hoveredIndex = -1;
    }

    private void focusOnly(Component child) {
        searchInput.setFocused(child == searchInput);
        volumeInput.setFocused(child == volumeInput);
        pitchInput.setFocused(child == pitchInput);
    }

    private Rect headerRect() { return new Rect(bounds.x(), bounds.y(), bounds.w(), HEADER_HEIGHT); }

    private Rect panelRect(float opened) {
        float w = PANEL_WIDTH;
        Rect available = availableBounds();
        float fullH = fullPanelHeight();
        float h = Math.max(0.0f, fullH * MathUtil.clamp(opened, 0.0f, 1.0f));
        float x = Math.max(available.x(), Math.min(bounds.right() - w, available.right() - w));
        float belowY = bounds.bottom() + PANEL_GAP;
        float belowSpace = available.bottom() - belowY;
        float aboveSpace = bounds.y() - available.y();
        boolean openAbove = belowSpace < fullH && aboveSpace > belowSpace;
        float y = openAbove ? bounds.y() - PANEL_GAP - h : belowY;
        y = Math.max(available.y(), Math.min(y, available.bottom() - h));
        return new Rect(x, y, w, h);
    }

    private float fullPanelHeight() {
        return PANEL_PADDING + SEARCH_HEIGHT + 8 + visibleRows() * ITEM_HEIGHT + 9 + SEPARATOR_HEIGHT + 10 + 24 + 14 + NUMBER_HEIGHT + 12 + 30 + PANEL_PADDING;
    }

    private void layoutOverlayInputs(Rect panel) {
        boolean show = expanded;
        searchInput.visible(show);
        volumeInput.visible(show);
        pitchInput.visible(show);
        searchInput.layout(null, searchRect(panel));
        volumeInput.layout(null, volumeRect(panel));
        pitchInput.layout(null, pitchRect(panel));
    }

    private Rect searchRect(Rect panel) { return new Rect(panel.x() + PANEL_PADDING, panel.y() + PANEL_PADDING, panel.w() - PANEL_PADDING * 2, SEARCH_HEIGHT); }
    private Rect listRect(Rect panel) { return new Rect(panel.x() + PANEL_PADDING, panel.y() + PANEL_PADDING + SEARCH_HEIGHT + 8, panel.w() - PANEL_PADDING * 2 - (needsScrollbar() ? SCROLLBAR_W + 7 : 0), visibleRows() * ITEM_HEIGHT); }
    private Rect listAndScrollbarRect(Rect panel) { Rect list = listRect(panel); return new Rect(list.x(), list.y(), panel.w() - PANEL_PADDING * 2, list.h()); }
    private Rect separatorRect(Rect panel) { return new Rect(panel.x() + PANEL_PADDING, listRect(panel).bottom() + 8, panel.w() - PANEL_PADDING * 2, SEPARATOR_HEIGHT); }
    private float selectedLabelY(Rect panel) { return separatorRect(panel).bottom() + 10; }
    private float controlsY(Rect panel) { return selectedLabelY(panel) + 24 + 14; }
    private Rect volumeRect(Rect panel) { return new Rect(panel.x() + PANEL_PADDING, controlsY(panel), 92, NUMBER_HEIGHT); }
    private Rect pitchRect(Rect panel) { return new Rect(volumeRect(panel).right() + 12, controlsY(panel), 92, NUMBER_HEIGHT); }

    private Rect clearButton(Rect panel) { return actionButton(panel, 0); }
    private Rect playButton(Rect panel) { return actionButton(panel, 1); }
    private Rect selectButton(Rect panel) { return actionButton(panel, 2); }

    private Rect actionButton(Rect panel, int index) {
        float gap = 8;
        float y = panel.bottom() - PANEL_PADDING - 30;
        float w = (panel.w() - PANEL_PADDING * 2 - gap * 2) / 3.0f;
        return new Rect(panel.x() + PANEL_PADDING + index * (w + gap), y, w, 30);
    }

    private ButtonId buttonAt(Rect panel, float x, float y) {
        if (clearButton(panel).contains(x, y)) return clearDisabled() ? null : ButtonId.CLEAR;
        if (playButton(panel).contains(x, y)) return draftSound == null ? null : ButtonId.PLAY;
        if (selectButton(panel).contains(x, y)) return selectDisabled() ? null : ButtonId.SELECT;
        return null;
    }

    private int visibleRows() { return Math.max(1, Math.min(MAX_VISIBLE_ROWS, Math.max(filtered.size(), 1))); }
    private boolean needsScrollbar() { return filtered.size() > visibleRows(); }
    private float maxScroll() { return Math.max(0, filtered.size() * ITEM_HEIGHT - visibleRows() * ITEM_HEIGHT); }
    private void clampScroll() { scroll = MathUtil.clamp(scroll, 0, maxScroll()); }

    private Rect scrollbarTrack(Rect panel) { Rect list = listRect(panel); return new Rect(panel.right() - PANEL_PADDING - SCROLLBAR_W, list.y(), SCROLLBAR_W, list.h()); }
    private Rect scrollbarThumb(Rect panel) {
        Rect track = scrollbarTrack(panel);
        float max = maxScroll();
        if (max <= 0) return new Rect(track.x(), track.y(), track.w(), track.h());
        float ratio = MathUtil.clamp((visibleRows() * ITEM_HEIGHT) / Math.max(1, filtered.size() * ITEM_HEIGHT), 0.12f, 1.0f);
        float h = Math.max(14, track.h() * ratio);
        float y = track.y() + (scroll / max) * Math.max(0, track.h() - h);
        return new Rect(track.x(), y, track.w(), h);
    }

    private void setScrollFromMouseY(float y) {
        Rect panel = panelRect(1.0f);
        Rect track = scrollbarTrack(panel);
        Rect thumb = scrollbarThumb(panel);
        float movable = Math.max(1, track.h() - thumb.h());
        float pct = MathUtil.clamp((y - scrollbarGrabY - track.y()) / movable, 0, 1);
        scroll = pct * maxScroll();
        clampScroll();
    }

    private int itemIndexAt(float x, float y) {
        Rect list = listRect(panelRect(1.0f));
        if (!list.contains(x, y) || filtered.isEmpty()) return -1;
        int index = (int) ((y - list.y() + scroll) / ITEM_HEIGHT);
        return index >= 0 && index < filtered.size() ? index : -1;
    }

    private void ensureVisible(int index) {
        float top = index * ITEM_HEIGHT;
        float bottom = top + ITEM_HEIGHT;
        float visible = visibleRows() * ITEM_HEIGHT;
        if (top < scroll) scroll = top;
        else if (bottom > scroll + visible) scroll = bottom - visible;
        clampScroll();
    }

    private void scrollToDraftSound() {
        if (draftSound == null || filtered.isEmpty()) {
            scroll = 0.0f;
            return;
        }
        int idx = filtered.indexOf(draftSound);
        if (idx >= 0) {
            float visible = visibleRows() * ITEM_HEIGHT;
            scroll = idx * ITEM_HEIGHT - Math.max(0.0f, (visible - ITEM_HEIGHT) * 0.5f);
            clampScroll();
        } else {
            scroll = 0.0f;
        }
    }

    private Rect availableBounds() {
        Component root = this;
        while (root.parent() != null) root = root.parent();
        return root.bounds();
    }

    private static float easeOutCubic(float t) {
        float clamped = MathUtil.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - clamped;
        return 1.0f - inv * inv * inv;
    }

    private static String ellipsize(RenderContext context, String value, float font, float maxWidth) {
        String s = value == null ? "" : value;
        if (context.measureText(s, font).width() <= maxWidth) return s;
        String ellipsis = "...";
        if (context.measureText(ellipsis, font).width() > maxWidth) return "";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (context.measureText(s.substring(0, mid) + ellipsis, font).width() <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }
}
