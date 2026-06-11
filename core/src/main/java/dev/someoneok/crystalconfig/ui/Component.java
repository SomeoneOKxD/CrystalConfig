package dev.someoneok.crystalconfig.ui;

import dev.someoneok.crystalconfig.input.*;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.Insets;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Component {
    protected Rect bounds = Rect.ZERO;
    protected Insets padding = Insets.ZERO;
    protected Insets margin = Insets.ZERO;
    protected float preferredWidth = -1;
    protected float preferredHeight = -1;
    protected float minWidth = 0;
    protected float minHeight = 0;
    protected boolean fillX;
    protected boolean fillY;
    protected float flexGrow = 0;
    protected boolean visible = true;
    protected boolean enabled = true;
    protected boolean hovered;
    protected boolean focused;
    protected boolean focusable;
    protected boolean pressed;
    protected float z = 0;
    protected String id = "";
    protected String tooltip = "";
    private boolean layoutDirty = true;
    protected Component parent;
    protected final List<Component> children = new ArrayList<>();
    private final List<Component> readOnlyChildren = Collections.unmodifiableList(children);

    protected boolean absolute;
    protected float absoluteX;
    protected float absoluteY;
    protected float absoluteWidth = -1;
    protected float absoluteHeight = -1;

    @SuppressWarnings("unchecked")
    protected <T extends Component> T self() {
        return (T) this;
    }

    public <T extends Component> T id(String id) { this.id = id == null ? "" : id; return self(); }
    public <T extends Component> T tooltip(String tooltip) { this.tooltip = tooltip == null ? "" : tooltip; return self(); }
    public <T extends Component> T padding(float value) { this.padding = Insets.all(value); markLayoutDirty(); return self(); }
    public <T extends Component> T padding(float x, float y) { this.padding = Insets.xy(x, y); markLayoutDirty(); return self(); }
    public <T extends Component> T padding(Insets padding) { this.padding = padding == null ? Insets.ZERO : padding; markLayoutDirty(); return self(); }
    public <T extends Component> T margin(float value) { this.margin = Insets.all(value); markLayoutDirty(); return self(); }
    public <T extends Component> T margin(float x, float y) { this.margin = Insets.xy(x, y); markLayoutDirty(); return self(); }
    public <T extends Component> T margin(Insets margin) { this.margin = margin == null ? Insets.ZERO : margin; markLayoutDirty(); return self(); }
    public <T extends Component> T width(float width) { this.preferredWidth = width; markLayoutDirty(); return self(); }
    public <T extends Component> T height(float height) { this.preferredHeight = height; markLayoutDirty(); return self(); }
    public <T extends Component> T size(float width, float height) { this.preferredWidth = width; this.preferredHeight = height; markLayoutDirty(); return self(); }
    public <T extends Component> T minSize(float width, float height) { this.minWidth = width; this.minHeight = height; markLayoutDirty(); return self(); }
    public <T extends Component> T fillX() { this.fillX = true; markLayoutDirty(); return self(); }
    public <T extends Component> T fillY() { this.fillY = true; markLayoutDirty(); return self(); }
    public <T extends Component> T fill() { this.fillX = true; this.fillY = true; markLayoutDirty(); return self(); }
    public <T extends Component> T flex(float flexGrow) { this.flexGrow = Math.max(0, flexGrow); markLayoutDirty(); return self(); }
    public <T extends Component> T visible(boolean visible) { if (this.visible != visible) { this.visible = visible; markLayoutDirty(); } return self(); }
    public <T extends Component> T enabled(boolean enabled) { this.enabled = enabled; return self(); }
    public <T extends Component> T z(float z) { this.z = z; return self(); }
    public <T extends Component> T focusable(boolean focusable) { this.focusable = focusable; return self(); }
    public <T extends Component> T absolute(float x, float y, float width, float height) { this.absolute = true; this.absoluteX = x; this.absoluteY = y; this.absoluteWidth = width; this.absoluteHeight = height; markLayoutDirty(); return self(); }

    public Component add(Component child) {
        if (child == null) return this;
        if (child.parent != null) child.parent.children.remove(child);
        child.parent = this;
        children.add(child);
        markLayoutDirty();
        return this;
    }

    public Component remove(Component child) {
        if (child != null && children.remove(child)) { child.parent = null; markLayoutDirty(); }
        return this;
    }

    public Rect bounds() { return bounds; }
    public Insets padding() { return padding; }
    public Insets margin() { return margin; }
    public boolean visible() { return visible; }
    public boolean enabled() { return enabled; }
    public boolean hovered() { return hovered; }
    public boolean focused() { return focused; }
    public boolean focusable() { return focusable; }
    public float flexGrow() { return flexGrow; }
    public boolean fillXValue() { return fillX; }
    public boolean fillYValue() { return fillY; }
    public float preferredWidth() { return preferredWidth; }
    public float preferredHeight() { return preferredHeight; }
    public float minWidth() { return minWidth; }
    public float minHeight() { return minHeight; }
    public float z() { return z; }
    public boolean absoluteLayout() { return absolute; }
    public float absoluteX() { return absoluteX; }
    public float absoluteY() { return absoluteY; }
    public float absoluteWidth() { return absoluteWidth; }
    public float absoluteHeight() { return absoluteHeight; }
    public String tooltip() { return tooltip; }
    public Component parent() { return parent; }
    public List<Component> children() { return readOnlyChildren; }

    public boolean layoutDirty() {
        if (layoutDirty) return true;
        for (Component child : children) {
            if (child.layoutDirty()) return true;
        }
        return false;
    }

    public void markLayoutDirty() {
        layoutDirty = true;
        if (parent != null) parent.markLayoutDirty();
    }

    protected void clearLayoutDirtyDeep() {
        layoutDirty = false;
        for (Component child : children) child.clearLayoutDirtyDeep();
    }

    public Component clearChildren() {
        for (Component child : children) child.parent = null;
        children.clear();
        markLayoutDirty();
        return this;
    }

    public Size measure(LayoutContext context, Constraints constraints) {
        if (!visible()) return Size.ZERO;
        Size content = measureSelf(context, new Constraints(
                Math.max(0, constraints.maxWidth() - padding.horizontal()),
                Math.max(0, constraints.maxHeight() - padding.vertical())
        ));
        float w = preferredWidth >= 0 ? preferredWidth : content.width() + padding.horizontal();
        float h = preferredHeight >= 0 ? preferredHeight : content.height() + padding.vertical();
        return constraints.clamp(new Size(Math.max(minWidth, w), Math.max(minHeight, h)));
    }

    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float w = preferredWidth >= 0 ? preferredWidth : minWidth;
        float h = preferredHeight >= 0 ? preferredHeight : minHeight;
        return constraints.clamp(new Size(w, h));
    }

    public void layout(LayoutContext context, Rect area) {
        this.bounds = area;
        layoutChildren(context);
        layoutDirty = false;
    }

    protected void layoutChildren(LayoutContext context) {
        Rect content = bounds.inset(padding.left(), padding.top(), padding.right(), padding.bottom());
        for (Component child : children) {
            if (!child.visible()) continue;
            Size measured = child.measure(context, new Constraints(content.w(), content.h()));
            float cw = child.fillXValue() ? content.w() - child.margin().horizontal() : measured.width();
            float ch = child.fillYValue() ? content.h() - child.margin().vertical() : measured.height();
            child.layout(context, new Rect(content.x() + child.margin().left(), content.y() + child.margin().top(), Math.max(0, cw), Math.max(0, ch)));
        }
    }

    public void render(RenderContext context) {
        render(context, null);
    }

    public void render(RenderContext context, Rect clip) {
        if (!visible()) return;
        if (clip != null && !bounds.intersects(clip)) return;
        renderSelf(context);
        for (Component child : children) {
            child.render(context, clip);
        }
        if (context.debugBounds()) {
            context.rect(bounds, SdfRectStyle.create().fill(ColorRGBA.TRANSPARENT).border(1, ColorRGBA.rgba(255, 0, 180, 120)).radius(0), z + 5000);
        }
    }

    protected void renderSelf(RenderContext context) { }

    /** Render popups after the main component tree. This pass is not clipped by scroll containers. */
    public void renderOverlay(RenderContext context) {
        if (!visible()) return;
        for (Component child : children) child.renderOverlay(context);
    }

    public void tick(float deltaSeconds) {
        if (!visible()) return;
        for (Component child : children) child.tick(deltaSeconds);
    }

    /**
     * True while this component needs fresh render states even when the screen render cap
     * would otherwise replay the cached frame. Use this for smooth scroll/drag animations.
     */
    public boolean needsFreshRender() {
        if (!visible()) return false;
        for (Component child : children) {
            if (child.needsFreshRender()) return true;
        }
        return false;
    }

    public Component hitTest(float x, float y) {
        if (!visible() || !enabled()) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            Component overlayHit = children.get(i).hitTestOverlay(x, y);
            if (overlayHit != null) return overlayHit;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            Component hit = children.get(i).hitTest(x, y);
            if (hit != null) return hit;
        }
        return bounds.contains(x, y) ? this : null;
    }

    public Component hitTestOverlay(float x, float y) {
        if (!visible() || !enabled()) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            Component overlayHit = children.get(i).hitTestOverlay(x, y);
            if (overlayHit != null) return overlayHit;
        }
        return null;
    }

    public void setHovered(boolean hovered) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        onHoverChanged(hovered);
    }

    public void setFocused(boolean focused) {
        if (this.focused == focused) return;
        this.focused = focused;
        onFocusChanged(focused);
    }

    protected void onHoverChanged(boolean hovered) { }
    protected void onFocusChanged(boolean focused) { }

    public boolean onMouseMove(MouseMoveEvent event) { return false; }
    public boolean onMousePressedCapture(MouseButtonEvent event) { return false; }
    public boolean onMousePressed(MouseButtonEvent event) { return false; }
    public boolean onMouseReleased(MouseButtonEvent event) { return false; }
    public boolean onMouseDragged(MouseDragEvent event) { return false; }
    public boolean onMouseScrolled(MouseScrollEvent event) { return false; }
    public boolean onKeyPressed(KeyEvent event) { return false; }
    public boolean onCharTyped(CharTypedEvent event) { return false; }
}
