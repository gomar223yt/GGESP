package org.Gomar223.ggesp.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JcNpYw extends RenderLayer {
    private static final Map<Identifier, RenderLayer> ENTITY_LAYERS = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderLayer> ARMOR_LAYERS = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderLayer> ARMOR_DECAL_LAYERS = new ConcurrentHashMap<>();

    private JcNpYw(
        String name,
        VertexFormat vertexFormat,
        VertexFormat.DrawMode drawMode,
        int expectedBufferSize,
        boolean hasCrumbling,
        boolean translucent,
        Runnable startAction,
        Runnable endAction
    ) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    public static RenderLayer getEntityNoDepth(Identifier texture) {
        return ENTITY_LAYERS.computeIfAbsent(texture, id -> createLayer(
            "ggesp_wall_entity",
            id,
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            ENTITY_CUTOUT_NONULL_PROGRAM,
            NO_TRANSPARENCY
        ));
    }

    public static RenderLayer getArmorNoDepth(Identifier texture) {
        return ARMOR_LAYERS.computeIfAbsent(texture, id -> createLayer(
            "ggesp_wall_armor",
            id,
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            ARMOR_CUTOUT_NO_CULL_PROGRAM,
            NO_TRANSPARENCY
        ));
    }

    public static RenderLayer getArmorDecalNoDepth(Identifier texture) {
        return ARMOR_DECAL_LAYERS.computeIfAbsent(texture, id -> createLayer(
            "ggesp_wall_armor_decal",
            id,
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            ARMOR_CUTOUT_NO_CULL_PROGRAM,
            NO_TRANSPARENCY
        ));
    }

    private static RenderLayer createLayer(
        String name,
        Identifier texture,
        VertexFormat format,
        RenderPhase.ShaderProgram program,
        RenderPhase.Transparency transparency
    ) {
        MultiPhaseParameters parameters = MultiPhaseParameters.builder()
            .texture(new RenderPhase.Texture(texture, TriState.FALSE, false))
            .program(program)
            .transparency(transparency)
            .depthTest(BIGGER_DEPTH_TEST)
            .cull(DISABLE_CULLING)
            .lightmap(ENABLE_LIGHTMAP)
            .overlay(ENABLE_OVERLAY_COLOR)
            .target(MAIN_TARGET)
            .writeMaskState(ALL_MASK)
            .build(false);

        return of(name + ":" + texture, format, VertexFormat.DrawMode.QUADS, DEFAULT_BUFFER_SIZE, parameters);
    }
}
