package dev.someoneok.crystalconfig.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record SdfRectBatchRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        List<RectQuad> rects,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public SdfRectBatchRenderState(
            Matrix3x2f pose,
            List<RectQuad> rects,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                SdfUiRender.SDF_RECT_PIPELINE,
                TextureSetup.noTexture(),
                pose,
                rects,
                scissorArea,
                createBounds(rects, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        for (RectQuad r : rects) {
            float x0 = r.x();
            float y0 = r.y();
            float x1 = x0 + r.w();
            float y1 = y0 + r.h();

            float radiusNorm = r.w() <= 0.0f || r.h() <= 0.0f
                    ? 0.0f
                    : Math.min(0.5f, r.radius() / Math.min(r.w(), r.h()));

            vertex(vertices, x0, y0, 0.0f, 0.0f, radiusNorm, r.color());
            vertex(vertices, x0, y1, 0.0f, 1.0f, radiusNorm, r.color());
            vertex(vertices, x1, y1, 1.0f, 1.0f, radiusNorm, r.color());
            vertex(vertices, x1, y0, 1.0f, 0.0f, radiusNorm, r.color());
        }
    }

    private void vertex(VertexConsumer vertices, float px, float py, float u, float v, float radiusNorm, int color) {
        vertices.addVertexWith2DPose(pose, px, py)
                .setUv(u + radiusNorm, v)
                .setColor(color);
    }

    private static @Nullable ScreenRectangle createBounds(
            List<RectQuad> rects,
            Matrix3x2f pose,
            @Nullable ScreenRectangle scissorArea
    ) {
        if (rects.isEmpty()) return null;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (RectQuad r : rects) {
            minX = Math.min(minX, r.x());
            minY = Math.min(minY, r.y());
            maxX = Math.max(maxX, r.x() + r.w());
            maxY = Math.max(maxY, r.y() + r.h());
        }

        ScreenRectangle rect = new ScreenRectangle(
                (int) Math.floor(minX),
                (int) Math.floor(minY),
                (int) Math.ceil(maxX - minX),
                (int) Math.ceil(maxY - minY)
        ).transformMaxBounds(pose);

        return scissorArea != null ? scissorArea.intersection(rect) : rect;
    }

    public record RectQuad(
            float x,
            float y,
            float w,
            float h,
            int color,
            float radius
    ) {}
}
