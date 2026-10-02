package dev.someoneok.crystalconfig.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A blit-compatible GUI texture batch. Consecutive images sharing texture/filter/clip are submitted
 * as a single {@link GuiElementRenderState} instead of one state per {@code blit(...)} call.
 */
public record TextureBatchRenderState(
        Matrix3x2f pose,
        Identifier texture,
        TextureFilter filter,
        List<TextureQuad> images,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    private static final Map<TextureKey, CachedTextureSetup> TEXTURE_SETUPS = new HashMap<>(16);

    public TextureBatchRenderState(
            Matrix3x2f pose,
            Identifier texture,
            TextureFilter filter,
            List<TextureQuad> images,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(pose, texture, filter, images, scissorArea, createBounds(images, pose, scissorArea));
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI_TEXTURED;
    }

    @Override
    public TextureSetup textureSetup() {
        var tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        var textureView = tex.getTextureView();
        TextureKey key = new TextureKey(texture, filter);
        CachedTextureSetup cached = TEXTURE_SETUPS.get(key);
        if (cached != null && cached.textureView() == textureView) return cached.textureSetup();

        FilterMode mode = filter == TextureFilter.LINEAR ? FilterMode.LINEAR : FilterMode.NEAREST;
        TextureSetup setup = TextureSetup.singleTexture(
                textureView,
                RenderSystem.getSamplerCache().getClampToEdge(mode)
        );
        TEXTURE_SETUPS.put(key, new CachedTextureSetup(textureView, setup));
        return setup;
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        for (TextureQuad q : images) {
            float x0 = q.x();
            float y0 = q.y();
            float x1 = x0 + q.w();
            float y1 = y0 + q.h();

            vertex(vertices, x0, y0, q.u0(), q.v0(), q.color());
            vertex(vertices, x0, y1, q.u0(), q.v1(), q.color());
            vertex(vertices, x1, y1, q.u1(), q.v1(), q.color());
            vertex(vertices, x1, y0, q.u1(), q.v0(), q.color());
        }
    }

    private void vertex(VertexConsumer vertices, float x, float y, float u, float v, int color) {
        vertices.addVertexWith2DPose(pose, x, y)
                .setUv(u, v)
                .setColor(color);
    }

    private static @Nullable ScreenRectangle createBounds(
            List<TextureQuad> images,
            Matrix3x2f pose,
            @Nullable ScreenRectangle scissorArea
    ) {
        if (images.isEmpty()) return null;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (TextureQuad q : images) {
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

    private record TextureKey(Identifier texture, TextureFilter filter) {}
    private record CachedTextureSetup(Object textureView, TextureSetup textureSetup) {}

    public record TextureQuad(
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
