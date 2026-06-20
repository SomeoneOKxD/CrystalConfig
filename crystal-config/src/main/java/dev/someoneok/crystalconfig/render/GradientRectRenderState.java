package dev.someoneok.crystalconfig.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public record GradientRectRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2f pose,
        float x,
        float y,
        float w,
        float h,
        int topLeftColor,
        int topRightColor,
        int bottomRightColor,
        int bottomLeftColor,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public GradientRectRenderState(
            Matrix3x2f pose,
            float x,
            float y,
            float w,
            float h,
            int topLeftColor,
            int topRightColor,
            int bottomRightColor,
            int bottomLeftColor,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(
                SdfUiRender.SDF_RECT_PIPELINE,
                TextureSetup.noTexture(),
                pose,
                x,
                y,
                w,
                h,
                topLeftColor,
                topRightColor,
                bottomRightColor,
                bottomLeftColor,
                scissorArea,
                createBounds(x, y, w, h, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;

        vertex(vertices, x0, y0, 0.0f, 0.0f, topLeftColor);
        vertex(vertices, x0, y1, 0.0f, 1.0f, bottomLeftColor);
        vertex(vertices, x1, y1, 1.0f, 1.0f, bottomRightColor);
        vertex(vertices, x1, y0, 1.0f, 0.0f, topRightColor);
    }

    private void vertex(VertexConsumer vertices, float px, float py, float u, float v, int color) {
        vertices.addVertexWith2DPose(pose, px, py)
                .setUv(u, v)
                .setColor(color);
    }

    private static @Nullable ScreenRectangle createBounds(float x, float y, float w, float h, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle rect = new ScreenRectangle(
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.ceil(w),
                (int) Math.ceil(h)
        ).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(rect) : rect;
    }
}
