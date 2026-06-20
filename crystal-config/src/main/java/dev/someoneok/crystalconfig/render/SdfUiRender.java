package dev.someoneok.crystalconfig.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class SdfUiRender {
    public static final RenderPipeline SDF_RECT_PIPELINE =
            RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                            .withLocation(Identifier.fromNamespaceAndPath("crystalconfig", "sdf_rect"))
                            .withVertexShader(Identifier.fromNamespaceAndPath("crystalconfig", "core/sdf_rect"))
                            .withFragmentShader(Identifier.fromNamespaceAndPath("crystalconfig", "core/sdf_rect"))
                            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                            .withCull(false)
                            .build()
            );

    public static final RenderPipeline MSDF_TEXT_PIPELINE =
            RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                            .withLocation(Identifier.fromNamespaceAndPath("crystalconfig", "msdf_text"))
                            .withVertexShader(Identifier.fromNamespaceAndPath("crystalconfig", "core/msdf_text"))
                            .withFragmentShader(Identifier.fromNamespaceAndPath("crystalconfig", "core/msdf_text"))
                            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                            .withCull(false)
                            .build()
            );

    private SdfUiRender() {}
}