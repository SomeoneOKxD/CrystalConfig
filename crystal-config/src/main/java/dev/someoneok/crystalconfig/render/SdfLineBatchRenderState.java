package dev.someoneok.crystalconfig.render;

//? if <26.3 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?} else {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline;
*///?}
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Batched arbitrary line segments rendered through the same anti-aliased SDF pipeline as rects. */
public record SdfLineBatchRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        List<LineQuad> lines,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public SdfLineBatchRenderState(
            Matrix3x2f pose,
            List<LineQuad> lines,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                SdfUiRender.SDF_RECT_PIPELINE,
                TextureSetup.noTexture(),
                pose,
                lines,
                scissorArea,
                createBounds(lines, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        for (LineQuad line : lines) buildLine(vertices, line);
    }

    private void buildLine(VertexConsumer vertices, LineQuad line) {
        float thickness = Math.max(0.0f, line.thickness());
        if (thickness <= 0.0f) return;

        float dx = line.x2() - line.x1();
        float dy = line.y2() - line.y1();
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float half = thickness * 0.5f;

        float ux;
        float uy;
        if (length <= 0.0001f) {
            ux = 1.0f;
            uy = 0.0f;
            length = 0.0f;
        } else {
            ux = dx / length;
            uy = dy / length;
        }

        float cap = line.roundCaps() ? half : 0.0f;
        float sx = line.x1() - ux * cap;
        float sy = line.y1() - uy * cap;
        float ex = line.x2() + ux * cap;
        float ey = line.y2() + uy * cap;

        // A zero-length line still gets a visible thickness-sized primitive.
        if (length <= 0.0001f) {
            sx = line.x1() - half;
            sy = line.y1();
            ex = line.x1() + half;
            ey = line.y1();
        }

        float px = -uy * half;
        float py = ux * half;
        float radiusNorm = line.roundCaps() ? 0.5f : 0.0f;

        vertex(vertices, sx + px, sy + py, 0.0f, 0.0f, radiusNorm, line.color());
        vertex(vertices, sx - px, sy - py, 0.0f, 1.0f, radiusNorm, line.color());
        vertex(vertices, ex - px, ey - py, 1.0f, 1.0f, radiusNorm, line.color());
        vertex(vertices, ex + px, ey + py, 1.0f, 0.0f, radiusNorm, line.color());
    }

    private void vertex(VertexConsumer vertices, float x, float y, float u, float v, float radiusNorm, int color) {
        vertices.addVertexWith2DPose(pose, x, y)
                .setUv(u + radiusNorm, v)
                .setColor(color);
    }

    private static @Nullable ScreenRectangle createBounds(
            List<LineQuad> lines,
            Matrix3x2f pose,
            @Nullable ScreenRectangle scissorArea
    ) {
        if (lines.isEmpty()) return null;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (LineQuad line : lines) {
            float half = Math.max(0.0f, line.thickness()) * 0.5f;
            minX = Math.min(minX, Math.min(line.x1(), line.x2()) - half);
            minY = Math.min(minY, Math.min(line.y1(), line.y2()) - half);
            maxX = Math.max(maxX, Math.max(line.x1(), line.x2()) + half);
            maxY = Math.max(maxY, Math.max(line.y1(), line.y2()) + half);
        }

        ScreenRectangle rect = new ScreenRectangle(
                (int) Math.floor(minX),
                (int) Math.floor(minY),
                (int) Math.ceil(maxX - minX),
                (int) Math.ceil(maxY - minY)
        ).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(rect) : rect;
    }

    public record LineQuad(
            float x1,
            float y1,
            float x2,
            float y2,
            float thickness,
            int color,
            boolean roundCaps
    ) {}
}
