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
import java.util.List;
import java.util.Map;

public record MsdfTextRunRenderState(
        Matrix3x2f pose,
        Identifier texture,
        List<GlyphQuad> glyphs,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    private static final Map<Identifier, TextureSetup> TEXTURE_SETUPS = new HashMap<>(4);

    public MsdfTextRunRenderState(
            Matrix3x2f pose,
            Identifier texture,
            List<GlyphQuad> glyphs,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(pose, texture, glyphs, scissorArea, createBounds(glyphs, pose, scissorArea));
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
        for (GlyphQuad q : glyphs) {
            float x0 = q.x();
            float y0 = q.y();
            float x1 = x0 + q.w();
            float y1 = y0 + q.h();

            vertex(vertices, x0, y0, q.u0(), q.v0(), q.color());
            vertex(vertices, x1, y0, q.u1(), q.v0(), q.color());
            vertex(vertices, x1, y1, q.u1(), q.v1(), q.color());
            vertex(vertices, x0, y1, q.u0(), q.v1(), q.color());
        }
    }

    private void vertex(VertexConsumer vertices, float px, float py, float u, float v, int color) {
        vertices.addVertexWith2DPose(pose, px, py)
                .setUv(u, v)
                .setColor(color);
    }

    private static @Nullable ScreenRectangle createBounds(List<GlyphQuad> glyphs, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
        if (glyphs.isEmpty()) return null;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (GlyphQuad q : glyphs) {
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

    public record GlyphQuad(
            float x,
            float y,
            float w,
            float h,
            float u0,
            float v0,
            float u1,
            float v1,
            int color
    ) {}
}
