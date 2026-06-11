package dev.someoneok.crystalconfig.containers;

import dev.someoneok.crystalconfig.animation.AnimatedFloat;
import dev.someoneok.crystalconfig.input.MouseButton;
import dev.someoneok.crystalconfig.input.MouseButtonEvent;
import dev.someoneok.crystalconfig.input.MouseDragEvent;
import dev.someoneok.crystalconfig.input.MouseScrollEvent;
import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.render.ColorRGBA;
import dev.someoneok.crystalconfig.render.Rect;
import dev.someoneok.crystalconfig.render.RenderContext;
import dev.someoneok.crystalconfig.render.SdfRectStyle;
import dev.someoneok.crystalconfig.ui.Component;
import dev.someoneok.crystalconfig.utils.MathUtil;

public class ScrollContainer extends Component {
    private static final float SCROLLBAR_WIDTH = 6;

    private Component content;
    private final AnimatedFloat scroll = new AnimatedFloat(0).speed(22);
    private float targetScroll;
    private float contentHeight;
    private boolean draggingScrollbar;
    private float scrollbarGrab;

    public ScrollContainer(Component content) {
        setContent(content);
    }

    public ScrollContainer setContent(Component content) {
        clearChildren();
        this.content = content;
        targetScroll = 0.0f;
        scroll.snap(0.0f);
        if (content != null) add(content);
        markLayoutDirty();
        return this;
    }

    public Component content() {
        return content;
    }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        float height = preferredHeight >= 0 ? preferredHeight : Math.min(420, constraints.maxHeight());
        return new Size(constraints.maxWidth(), height);
    }

    @Override
    public void layout(LayoutContext context, Rect area) {
        this.bounds = area;
        if (content == null) return;
        Rect viewport = viewport();
        Size measured = content.measure(context, new Constraints(viewport.w(), 100000));
        contentHeight = measured.height();
        clampTarget();
        content.layout(context, new Rect(viewport.x(), viewport.y() - scroll.value(), viewport.w(), contentHeight));
    }

    @Override
    public void tick(float deltaSeconds) {
        float before = scroll.value();
        scroll.target(targetScroll);
        scroll.update(deltaSeconds);
        if (Math.abs(before - scroll.value()) > 0.001f) {
            markLayoutDirty();
        }
        if (content != null) content.tick(deltaSeconds);
    }

    @Override
    public void render(RenderContext context) {
        render(context, null);
    }

    @Override
    public void render(RenderContext context, Rect parentClip) {
        if (!visible) return;
        if (parentClip != null && !bounds.intersects(parentClip)) return;

        Rect viewport = viewport();
        Rect clip = parentClip == null ? viewport : viewport.intersect(parentClip);
        if (!clip.isEmpty()) {
            context.pushClip(clip);
            if (content != null) content.render(context, clip);
            context.popClip();
        }
        renderScrollbar(context, viewport);
        if (context.debugBounds()) {
            context.rect(bounds, SdfRectStyle.create().fill(ColorRGBA.TRANSPARENT).border(1, ColorRGBA.rgba(0, 220, 255, 120)), z + 5000);
        }
    }

    @Override
    public boolean needsFreshRender() {
        return super.needsFreshRender() || draggingScrollbar || Math.abs(scroll.target() - scroll.value()) > 0.0005f;
    }

    @Override
    public Component hitTest(float x, float y) {
        if (!visible || !enabled || !bounds.contains(x, y)) return null;
        Rect viewport = viewport();
        if (viewport.contains(x, y) && content != null) {
            Component hit = content.hitTest(x, y);
            if (hit != null) return hit;
        }
        return this;
    }

    @Override
    public Component hitTestOverlay(float x, float y) {
        if (!visible || !enabled || content == null) return null;
        return content.hitTestOverlay(x, y);
    }

    @Override
    public boolean onMouseScrolled(MouseScrollEvent event) {
        targetScroll -= event.amountY * 34.0f;
        clampTarget();
        markLayoutDirty();
        return true;
    }

    @Override
    public boolean onMousePressed(MouseButtonEvent event) {
        if (event.button != MouseButton.LEFT) return false;
        Rect thumb = scrollbarThumb(viewport());
        if (!thumb.isEmpty() && thumb.contains(event.x, event.y)) {
            draggingScrollbar = true;
            scrollbarGrab = event.y - thumb.y();
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseDragged(MouseDragEvent event) {
        if (!draggingScrollbar) return false;
        Rect viewport = viewport();
        float thumbH = scrollbarThumb(viewport).h();
        float track = Math.max(1, viewport.h() - thumbH);
        float t = MathUtil.clamp((event.y - viewport.y() - scrollbarGrab) / track, 0, 1);
        targetScroll = t * maxScroll();
        markLayoutDirty();
        return true;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return false;
    }

    private void renderScrollbar(RenderContext context, Rect viewport) {
        if (maxScroll() <= 0) return;
        Rect track = scrollbarTrack(viewport);
        Rect thumb = scrollbarThumb(viewport);
        context.rect(track, SdfRectStyle.create()
                .fill(context.theme().palette().surfaceAlt().withAlpha(55))
                .radius(context.theme().radii().sm()), z + 20);
        context.rect(thumb, SdfRectStyle.create()
                .fill(context.theme().palette().surfaceActive().withAlpha(draggingScrollbar ? 220 : 185))
                .radius(context.theme().radii().sm()), z + 21);
    }

    private Rect scrollbarTrack(Rect viewport) {
        return new Rect(viewport.right() - SCROLLBAR_WIDTH, viewport.y(), SCROLLBAR_WIDTH, viewport.h());
    }

    private Rect scrollbarThumb(Rect viewport) {
        float max = maxScroll();
        if (max <= 0 || contentHeight <= 0) return Rect.ZERO;
        float ratio = MathUtil.clamp(viewport.h() / contentHeight, 0.08f, 1.0f);
        float thumbH = viewport.h() * ratio;
        float y = viewport.y() + (scroll.value() / max) * Math.max(0, viewport.h() - thumbH);
        return new Rect(viewport.right() - SCROLLBAR_WIDTH, y, SCROLLBAR_WIDTH, thumbH);
    }

    private Rect viewport() {
        return bounds.inset(padding.left(), padding.top(), padding.right(), padding.bottom());
    }

    private float maxScroll() {
        return Math.max(0, contentHeight - viewport().h());
    }

    private void clampTarget() {
        targetScroll = MathUtil.clamp(targetScroll, 0, maxScroll());
    }
}
