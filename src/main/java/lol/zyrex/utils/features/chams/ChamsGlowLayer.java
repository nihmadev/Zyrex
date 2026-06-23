package lol.zyrex.utils.features.chams;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;

public class ChamsGlowLayer {
    private static final ShaderProgramKey CHAMS_GLOW_KEY = new ShaderProgramKey(
            Identifier.of("minecraft", "core/chams_glow"),
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            Defines.EMPTY
    );

    private static final RenderPhase.ShaderProgram CHAMS_GLOW_PROGRAM = new RenderPhase.ShaderProgram(CHAMS_GLOW_KEY);

    public static RenderLayer getLayer(Identifier texture) {
        return RenderLayer.of(
                "chams_glow",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                256,
                true,
                false,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(CHAMS_GLOW_PROGRAM)
                        .texture(new RenderPhase.Texture(texture, TriState.FALSE, false))
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .build(true)
        );
    }
}
