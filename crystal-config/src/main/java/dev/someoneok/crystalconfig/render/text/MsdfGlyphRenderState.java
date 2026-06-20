package dev.someoneok.crystalconfig.render.text;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.someoneok.crystalconfig.render.SdfUiRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public record MsdfGlyphRenderState(
        Matrix3x2f pose,
        Identifier texture,
        float x,
        float y,
        float w,
        float h,
        float u0,
        float v0,
        float u1,
        float v1,
        int color,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    private static final Map<Identifier, TextureSetup> TEXTURE_SETUPS = new HashMap<>(4);

    public MsdfGlyphRenderState(
            Matrix3x2f pose,
            Identifier texture,
            float x,
            float y,
            float w,
            float h,
            float u0,
            float v0,
            float u1,
            float v1,
            int color,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(pose, texture, x, y, w, h, u0, v0, u1, v1, color, scissorArea, createBounds(x, y, w, h, pose, scissorArea));
    }

    @Override
    public RenderPipeline pipeline() {
        return SdfUiRender.MSDF_TEXT_PIPELINE;
    }

    @Override
    public TextureSetup textureSetup() {
        return TEXTURE_SETUPS.computeIfAbsent(texture, id -> {
            var tex = Minecraft.getInstance().getTextureManager().getTexture(id);
            return TextureSetup.singleTexture(
                    tex.getTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            );
        });
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;

        vertex(vertices, x0, y0, u0, v0);
        vertex(vertices, x1, y0, u1, v0);
        vertex(vertices, x1, y1, u1, v1);
        vertex(vertices, x0, y1, u0, v1);
    }

    private void vertex(VertexConsumer vertices, float px, float py, float u, float v) {
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
