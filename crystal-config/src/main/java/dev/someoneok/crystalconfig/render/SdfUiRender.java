package dev.someoneok.crystalconfig.render;

//? if <26.3 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?} else {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline;
*///?}
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//? if <26.2 {
import com.mojang.blaze3d.vertex.VertexFormat;
//?}
//? if >=26.2 && <26.3 {
/*import com.mojang.blaze3d.PrimitiveTopology;
*///?}
//? if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
*///?}
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class SdfUiRender {
    public static final RenderPipeline SDF_RECT_PIPELINE =
            RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                            .withLocation(Identifier.fromNamespaceAndPath("crystalconfig", "sdf_rect"))
                            .withVertexShader(Identifier.fromNamespaceAndPath("crystalconfig", "core/sdf_rect"))
                            .withFragmentShader(Identifier.fromNamespaceAndPath("crystalconfig", "core/sdf_rect"))
                            //? if <26.2 {
                            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                            //?} else {
                            /*.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            *///?}
                            .withCull(false)
                            .build()
            );

    public static final RenderPipeline MSDF_TEXT_PIPELINE =
            RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                            .withLocation(Identifier.fromNamespaceAndPath("crystalconfig", "msdf_text"))
                            .withVertexShader(Identifier.fromNamespaceAndPath("crystalconfig", "core/msdf_text"))
                            .withFragmentShader(Identifier.fromNamespaceAndPath("crystalconfig", "core/msdf_text"))
                            //? if <26.2 {
                            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                            //?} else {
                            /*.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            *///?}
                            .withCull(false)
                            .build()
            );

    private SdfUiRender() {}
}