package dev.someoneok.crystalconfig.render;

import java.util.*;

public final class DrawList {
    private static final Comparator<DrawCommand> DRAW_ORDER =
            Comparator.comparingDouble((DrawCommand c) -> c.z).thenComparingInt(DrawCommand::order);

    private final List<DrawCommand> commands = new ArrayList<>(512);
    private final List<QuadCommand> quadBatch = new ArrayList<>(128);
    private final ArrayDeque<Rect> clipStack = new ArrayDeque<>();
    private Rect activeClip;
    private int order;
    private boolean zMonotonic = true;
    private float lastZ = Float.NEGATIVE_INFINITY;

    public void add(DrawCommand command) {
        if (command == null) return;
        command.order = order++;
        command.clip = clipStack.peek();
        if (command.z < lastZ) zMonotonic = false;
        lastZ = command.z;
        commands.add(command);
    }

    public void pushClip(Rect rect) {
        Rect current = clipStack.peek();
        clipStack.push(current == null ? rect : current.intersect(rect));
    }

    public void popClip() { if (!clipStack.isEmpty()) clipStack.pop(); }
    public Rect currentClip() { return clipStack.peek(); }

    public void flush(RenderBackend backend) {
        if (commands.isEmpty()) {
            clearActiveClip(backend);
            clear();
            return;
        }
        if (!zMonotonic) commands.sort(DRAW_ORDER);

        Material batchMaterial = null;
        Rect batchClip = null;
        for (DrawCommand command : commands) {
            if (command instanceof QuadCommand quad) {
                if (batchMaterial != quad.material || !Objects.equals(batchClip, quad.clip)) {
                    flushBatch(backend, batchMaterial, batchClip);
                    batchMaterial = quad.material;
                    batchClip = quad.clip;
                }
                quadBatch.add(quad);
            } else if (command instanceof TextCommand text) {
                flushBatch(backend, batchMaterial, batchClip);
                batchMaterial = null;
                batchClip = null;
                applyClip(backend, text.clip);
                backend.drawText(text);
            }
        }
        flushBatch(backend, batchMaterial, batchClip);
        clearActiveClip(backend);
        clear();
    }

    private void flushBatch(RenderBackend backend, Material material, Rect clip) {
        if (quadBatch.isEmpty() || material == null) {
            quadBatch.clear();
            return;
        }
        applyClip(backend, clip);
        backend.drawQuads(material, quadBatch);
        quadBatch.clear();
    }

    private void applyClip(RenderBackend backend, Rect clip) {
        if (Objects.equals(activeClip, clip)) return;
        if (clip == null) backend.clearClip(); else backend.setClip(clip);
        activeClip = clip;
    }

    private void clearActiveClip(RenderBackend backend) {
        if (activeClip != null) {
            backend.clearClip();
            activeClip = null;
        }
    }

    public void clear() {
        commands.clear();
        quadBatch.clear();
        clipStack.clear();
        order = 0;
        zMonotonic = true;
        lastZ = Float.NEGATIVE_INFINITY;
    }
}
