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

/** Batches simple horizontal/vertical color gradients into one GUI render state. */
public record GradientRectBatchRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        List<GradientQuad> gradients,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public GradientRectBatchRenderState(
            Matrix3x2f pose,
            List<GradientQuad> gradients,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                SdfUiRender.SDF_RECT_PIPELINE,
                TextureSetup.noTexture(),
                pose,
                gradients,
                scissorArea,
                createBounds(gradients, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        for (GradientQuad q : gradients) {
            float x0 = q.x();
            float y0 = q.y();
            float x1 = x0 + q.w();
            float y1 = y0 + q.h();

            int topLeft = q.startColor();
            int topRight = q.horizontal() ? q.endColor() : q.startColor();
            int bottomRight = q.endColor();
            int bottomLeft = q.horizontal() ? q.startColor() : q.endColor();

            vertex(vertices, x0, y0, 0.0f, 0.0f, topLeft);
            vertex(vertices, x0, y1, 0.0f, 1.0f, bottomLeft);
            vertex(vertices, x1, y1, 1.0f, 1.0f, bottomRight);
            vertex(vertices, x1, y0, 1.0f, 0.0f, topRight);
        }
    }

    private void vertex(VertexConsumer vertices, float x, float y, float u, float v, int color) {
        vertices.addVertexWith2DPose(pose, x, y)
                .setUv(u, v)
                .setColor(color);
    }

    private static @Nullable ScreenRectangle createBounds(
            List<GradientQuad> gradients,
            Matrix3x2f pose,
            @Nullable ScreenRectangle scissorArea
    ) {
        if (gradients.isEmpty()) return null;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (GradientQuad q : gradients) {
            minX = Math.min(minX, q.x());
            minY = Math.min(minY, q.y());
            maxX = Math.max(maxX, q.x() + q.w());
            maxY = Math.max(maxY, q.y() + q.h());
        }

        ScreenRectangle rect = new ScreenRectangle(
                (int) Math.floor(minX),
                (int) Math.floor(minY),
                (int) Math.ceil(maxX - minX),
                (int) Math.ceil(maxY - minY)
        ).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(rect) : rect;
    }

    public record GradientQuad(
            float x,
            float y,
            float w,
            float h,
            int startColor,
            int endColor,
            boolean horizontal
    ) {}
}
