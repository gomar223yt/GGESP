package org.Gomar223.ggesp.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.Gomar223.ggesp.GGESP;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GGESPClient implements ClientModInitializer {
    private static final double MAX_WALL_MODEL_DISTANCE = 64.0D;
    private static final double MAX_ENTITY_ESP_DISTANCE = 128.0D;
    private static final int STORAGE_SCAN_INTERVAL = 60;
    private static final int STORAGE_SCAN_CHUNK_RADIUS = 4;
    private static final int DEBRIS_SCAN_INTERVAL = 200;
    private static final int DEBRIS_SCAN_CHUNK_RADIUS = 2;
    private static final int DEBRIS_SCAN_MAX_Y = 32;
    private static final Set<BlockEntityType<?>> STORAGE_TYPES = Set.of(
        BlockEntityType.CHEST,
        BlockEntityType.TRAPPED_CHEST,
        BlockEntityType.ENDER_CHEST,
        BlockEntityType.SHULKER_BOX,
        BlockEntityType.BARREL,
        BlockEntityType.HOPPER,
        BlockEntityType.DISPENSER,
        BlockEntityType.DROPPER,
        BlockEntityType.MOB_SPAWNER
    );

    private final List<StorageEspEntry> cachedStorageEntries = new ArrayList<>();
    private final List<BlockPos> cachedDebrisPositions = new ArrayList<>();
    private int storageScanTimer = STORAGE_SCAN_INTERVAL;
    private int debrisScanTimer = DEBRIS_SCAN_INTERVAL;

    @Override
    public void onInitializeClient() {
        EspSettings.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(this::render);
        WorldRenderEvents.LAST.register(this::renderWallModelsPass);
        GGESP.LOGGER.info("GGESP client renderer initialized.");
    }

    private void onEndTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        boolean ghostEnabled = EspSettings.espEnabled && EspSettings.ghostEsp;
        if (!ghostEnabled && GhostTracker.isEnabled()) {
            GhostTracker.clear();
        }
        GhostTracker.setEnabled(ghostEnabled);
        GhostTracker.tick();

        while (EspSettings.getGuiKeyBinding().wasPressed()) {
            client.setScreen(new ClickGuiScreen(client.currentScreen));
        }

        while (EspSettings.getToggleEspKeyBinding().wasPressed()) {
            EspSettings.espEnabled = !EspSettings.espEnabled;
            EspSettings.saveConfig();
            GGESP.LOGGER.info("ESP toggled: {}", EspSettings.espEnabled);
        }

        while (EspSettings.getFreecamKeyBinding().wasPressed()) {
            FreecamController.toggle();
        }

        if (FreecamController.isActive() && client.world == null) {
            FreecamController.disable();
        }

        FreecamController.tickMovement();

        if (EspSettings.espEnabled && EspSettings.wallModels && EspSettings.renderPlayers && client.world != null) {
            revealInvisiblePlayers(client);
        }

        if (EspSettings.storageEsp && client.world != null) {
            if (++storageScanTimer >= STORAGE_SCAN_INTERVAL) {
                storageScanTimer = 0;
                rescanStorageEsp(client);
            }
        } else {
            storageScanTimer = STORAGE_SCAN_INTERVAL;
            cachedStorageEntries.clear();
        }

        if (EspSettings.ancientDebrisEsp && client.world != null) {
            if (++debrisScanTimer >= DEBRIS_SCAN_INTERVAL) {
                debrisScanTimer = 0;
                rescanAncientDebris(client);
            }
        } else {
            debrisScanTimer = DEBRIS_SCAN_INTERVAL;
            cachedDebrisPositions.clear();
        }
    }

    private void revealInvisiblePlayers(MinecraftClient client) {
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator() || !player.isInvisible()) {
                continue;
            }

            player.setInvisible(false);
        }
    }

    private void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !EspSettings.espEnabled) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        if (matrices == null || camera == null) {
            return;
        }

        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;
        float tickDelta = client.getRenderTickCounter().getTickDelta(false);
        boolean friendTracersEnabled = EspSettings.hasEnabledFriendTracers();
        List<Entity> renderableEntities = (EspSettings.boxes || EspSettings.tracers || friendTracersEnabled)
            ? collectRenderableEntities(client, camera.getPos())
            : List.of();
        List<ItemEntity> renderableItems = EspSettings.itemEsp
            ? collectRenderableItems(client, camera.getPos())
            : List.of();

        if (EspSettings.boxes && !renderableEntities.isEmpty()) {
            if (EspSettings.filledBoxes) {
                renderFilledBoxes(renderableEntities, matrices, cameraX, cameraY, cameraZ, tickDelta);
            }

            renderOutlineBoxes(renderableEntities, matrices, cameraX, cameraY, cameraZ, tickDelta);
        }

        if ((EspSettings.tracers || friendTracersEnabled) && !renderableEntities.isEmpty()) {
            renderTracers(renderableEntities, matrices, camera, tickDelta);
        }

        if (EspSettings.nametags && EspSettings.renderPlayers) {
            renderNametags(client, matrices, context, cameraX, cameraY, cameraZ);
        }

        if (EspSettings.storageEsp) {
            renderStorageEsp(client, matrices, cameraX, cameraY, cameraZ);
        }

        if (EspSettings.itemEsp && !renderableItems.isEmpty()) {
            renderItemEsp(renderableItems, matrices, cameraX, cameraY, cameraZ, tickDelta);
        }

        if (EspSettings.ancientDebrisEsp) {
            renderAncientDebrisEsp(client, matrices, cameraX, cameraY, cameraZ);
        }

        if (EspSettings.ghostEsp) {
            renderGhosts(client, matrices, context, cameraX, cameraY, cameraZ);
        }
    }

    private void renderWallModelsPass(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || !EspSettings.espEnabled || !EspSettings.wallModels) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        Camera camera = context.camera();
        if (matrices == null || camera == null) {
            return;
        }

        float tickDelta = client.getRenderTickCounter().getTickDelta(false);
        renderWallModels(client, matrices, camera, tickDelta);
    }

    private void renderFilledBoxes(
        List<Entity> renderableEntities,
        MatrixStack matrices,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta
    ) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
            VertexFormat.DrawMode.QUADS,
            VertexFormats.POSITION_COLOR
        );

        for (Entity entity : renderableEntities) {
            Box box = toCameraRelativeBox(entity, cameraX, cameraY, cameraZ, tickDelta);
            drawFilledBoxFaces(matrices, buffer, box, EspSettings.red, EspSettings.green, EspSettings.blue, EspSettings.alpha * 0.2F);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        try (BuiltBuffer builtBuffer = buffer.endNullable()) {
            if (builtBuffer != null) {
                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private void drawFilledBoxFaces(
        MatrixStack matrices,
        BufferBuilder buffer,
        Box box,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        drawQuad(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        drawQuad(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        drawQuad(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, red, green, blue, alpha);
        drawQuad(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
        drawQuad(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        drawQuad(buffer, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, red, green, blue, alpha);
    }

    private void drawQuad(
        BufferBuilder buffer,
        Matrix4f matrix,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
        buffer.vertex(matrix, x3, y3, z3).color(red, green, blue, alpha);
        buffer.vertex(matrix, x4, y4, z4).color(red, green, blue, alpha);
    }

    private void renderOutlineBoxes(
        List<Entity> renderableEntities,
        MatrixStack matrices,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta
    ) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
            RenderLayer.getLines().getDrawMode(),
            RenderLayer.getLines().getVertexFormat()
        );

        for (Entity entity : renderableEntities) {
            Box box = toCameraRelativeBox(entity, cameraX, cameraY, cameraZ, tickDelta);
            VertexRendering.drawBox(
                matrices, buffer, box,
                EspSettings.red, EspSettings.green, EspSettings.blue,
                EspSettings.alpha
            );
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth((float) EspSettings.lineThickness);
        try (BuiltBuffer builtBuffer = buffer.endNullable()) {
            if (builtBuffer != null) {
                RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }
        } finally {
            RenderSystem.lineWidth(1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void renderTracers(
        List<Entity> renderableEntities,
        MatrixStack matrices,
        Camera camera,
        float tickDelta
    ) {
        Vec3d cameraPos = camera.getPos();
        Vec3d lookVec = new Vec3d(camera.getHorizontalPlane());
        Vec3d tracerStart = cameraPos.add(lookVec.multiply(0.5));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
            RenderLayer.getLines().getDrawMode(),
            RenderLayer.getLines().getVertexFormat()
        );

        Matrix4f posMatrix = matrices.peek().getPositionMatrix();
        float tnx = (float) lookVec.x;
        float tny = (float) lookVec.y;
        float tnz = (float) lookVec.z;
        float len = (float) Math.sqrt(tnx * tnx + tny * tny + tnz * tnz);
        if (len > 0) { tnx /= len; tny /= len; tnz /= len; }
        final float fnx = tnx, fny = tny, fnz = tnz;

        for (Entity entity : renderableEntities) {
            Vec3d entityPos = entity.getLerpedPos(tickDelta).add(0.0, entity.getHeight() / 2.0, 0.0);
            float red = EspSettings.red;
            float green = EspSettings.green;
            float blue = EspSettings.blue;
            boolean friend = entity instanceof AbstractClientPlayerEntity player && EspSettings.isFriend(player.getName().getString());
            if (friend) {
                String friendName = entity.getName().getString();
                if (!EspSettings.areFriendTracersEnabled(friendName)) {
                    continue;
                }

                red = EspSettings.getFriendTracerRed(friendName);
                green = EspSettings.getFriendTracerGreen(friendName);
                blue = EspSettings.getFriendTracerBlue(friendName);
            } else if (!EspSettings.tracers) {
                continue;
            }

            float x1 = (float) (tracerStart.x - cameraPos.x);
            float y1 = (float) (tracerStart.y - cameraPos.y);
            float z1 = (float) (tracerStart.z - cameraPos.z);
            float x2 = (float) (entityPos.x - cameraPos.x);
            float y2 = (float) (entityPos.y - cameraPos.y);
            float z2 = (float) (entityPos.z - cameraPos.z);

            buffer.vertex(posMatrix, x1, y1, z1)
                .color(red, green, blue, EspSettings.alpha)
                .normal(matrices.peek(), fnx, fny, fnz);
            buffer.vertex(posMatrix, x2, y2, z2)
                .color(red, green, blue, EspSettings.alpha)
                .normal(matrices.peek(), fnx, fny, fnz);
        }

        RenderSystem.lineWidth((float) EspSettings.lineThickness);
        try (BuiltBuffer builtBuffer = buffer.endNullable()) {
            if (builtBuffer != null) {
                RenderLayer.getLines().draw(builtBuffer);
            }
        } finally {
            RenderSystem.lineWidth(1.0F);
        }
    }

    private void renderWallModels(
        MinecraftClient client,
        MatrixStack matrices,
        Camera camera,
        float tickDelta
    ) {
        Vec3d cameraPos = camera.getPos();
        double maxWallModelDistanceSq = Math.min(
            client.options.getViewDistance().getValue() * 16.0D,
            MAX_WALL_MODEL_DISTANCE
        );
        maxWallModelDistanceSq *= maxWallModelDistanceSq;

        if (!EspSettings.renderPlayers) {
            return;
        }

        List<AbstractClientPlayerEntity> players = new ArrayList<>();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator()) {
                continue;
            }

            Vec3d pos = player.getLerpedPos(tickDelta);
            if (cameraPos.squaredDistanceTo(pos) <= maxWallModelDistanceSq
                && isOccluded(client, cameraPos, player, tickDelta)) {
                players.add(player);
            }
        }

        if (players.isEmpty()) {
            return;
        }

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        dispatcher.setRenderShadows(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        WallModelRenderState.setCustomLayersEnabled(true);
        WallModelRenderState.begin();

        try {
            for (AbstractClientPlayerEntity player : players) {
                Vec3d pos = player.getLerpedPos(tickDelta);
                double x = pos.x - cameraPos.x;
                double y = pos.y - cameraPos.y;
                double z = pos.z - cameraPos.z;
                boolean wasInvisible = player.isInvisible();

                matrices.push();
                if (wasInvisible) {
                    player.setInvisible(false);
                }
                try {
                    dispatcher.render(
                        player,
                        x,
                        y,
                        z,
                        tickDelta,
                        matrices,
                        consumers,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE
                    );
                } finally {
                    if (wasInvisible) {
                        player.setInvisible(true);
                    }
                    matrices.pop();
                }
            }

            consumers.draw();
        } finally {
            WallModelRenderState.end();
            RenderSystem.depthMask(true);
            WallModelRenderState.setCustomLayersEnabled(false);
            dispatcher.setRenderShadows(true);
        }
    }

    private boolean isOccluded(MinecraftClient client, Vec3d cameraPos, AbstractClientPlayerEntity player, float tickDelta) {
        Vec3d pos = player.getLerpedPos(tickDelta);
        double halfWidth = player.getWidth() * 0.5D;
        double height = player.getHeight();

        return isRayBlocked(client, cameraPos, pos.add(0.0D, height * 0.5D, 0.0D))
            || isRayBlocked(client, cameraPos, pos.add(0.0D, height * 0.9D, 0.0D))
            || isRayBlocked(client, cameraPos, pos.add(halfWidth, height * 0.5D, 0.0D))
            || isRayBlocked(client, cameraPos, pos.add(-halfWidth, height * 0.5D, 0.0D))
            || isRayBlocked(client, cameraPos, pos.add(0.0D, height * 0.5D, halfWidth))
            || isRayBlocked(client, cameraPos, pos.add(0.0D, height * 0.5D, -halfWidth));
    }

    private boolean isRayBlocked(MinecraftClient client, Vec3d from, Vec3d to) {
        HitResult hit = client.world.raycast(new RaycastContext(
            from,
            to,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            client.player
        ));

        return hit.getType() != HitResult.Type.MISS
            && from.squaredDistanceTo(hit.getPos()) < from.squaredDistanceTo(to);
    }

    private void renderNametags(
        MinecraftClient client,
        MatrixStack matrices,
        WorldRenderContext context,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        TextRenderer textRenderer = client.textRenderer;
        if (textRenderer == null) return;
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        boolean drewAny = false;

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isSpectator()) continue;
            if (!EspSettings.renderPlayers) continue;

            Vec3d pos = player.getPos();
            double dx = pos.x - cameraX;
            double baseDy = pos.y - cameraY;
            double dz = pos.z - cameraZ;
            double distSq = dx * dx + baseDy * baseDy + dz * dz;
            if (distSq > MAX_ENTITY_ESP_DISTANCE * MAX_ENTITY_ESP_DISTANCE) {
                continue;
            }

            double dist = Math.sqrt(distSq);
            double dy = baseDy + player.getHeight() + 0.5;
            String label = player.getName().getString() + " [" + (int) dist + "m]";

            matrices.push();
            matrices.translate(dx, dy, dz);
            matrices.multiply(context.camera().getRotation());
            float scale = Math.max(0.025F, (float) dist * 0.01F);
            matrices.scale(-scale, -scale, scale);

            int textWidth = textRenderer.getWidth(label);
            float x = -textWidth / 2.0F;

            Matrix4f mat = matrices.peek().getPositionMatrix();
            int backgroundColor = ((int) (0.45F * 255) << 24);
            textRenderer.draw(
                label,
                x,
                0,
                0xFFFFFFFF,
                false,
                mat,
                consumers,
                TextRenderer.TextLayerType.SEE_THROUGH,
                backgroundColor,
                0xF000F0
            );
            drewAny = true;

            matrices.pop();
        }

        if (drewAny) {
            consumers.draw();
        }
    }

    private void renderStorageEsp(
        MinecraftClient client,
        MatrixStack matrices,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (cachedStorageEntries.isEmpty()) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
            RenderLayer.getLines().getDrawMode(),
            RenderLayer.getLines().getVertexFormat()
        );

        double maxDistanceSq = getStorageRenderDistance(client);
        maxDistanceSq *= maxDistanceSq;
        for (StorageEspEntry entry : cachedStorageEntries) {
            BlockPos pos = entry.pos();
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 0.5D;
            double centerZ = pos.getZ() + 0.5D;
            double dx = centerX - cameraX;
            double dy = centerY - cameraY;
            double dz = centerZ - cameraZ;
            if (dx * dx + dy * dy + dz * dz > maxDistanceSq) {
                continue;
            }

            Box box = new Box(
                pos.getX() - cameraX,
                pos.getY() - cameraY,
                pos.getZ() - cameraZ,
                pos.getX() + 1 - cameraX,
                pos.getY() + 1 - cameraY,
                pos.getZ() + 1 - cameraZ
            );

            VertexRendering.drawBox(matrices, buffer, box, entry.red(), entry.green(), entry.blue(), 0.8F);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(2.0F);
        try (BuiltBuffer builtBuffer = buffer.endNullable()) {
            if (builtBuffer != null) {
                RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }
        } finally {
            RenderSystem.lineWidth(1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void renderItemEsp(
        List<ItemEntity> items,
        MatrixStack matrices,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta
    ) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
            RenderLayer.getLines().getDrawMode(),
            RenderLayer.getLines().getVertexFormat()
        );

        for (ItemEntity item : items) {
            Box box = toCameraRelativeBox(item, cameraX, cameraY, cameraZ, tickDelta).expand(0.03D);
            VertexRendering.drawBox(matrices, buffer, box, 0.2F, 1.0F, 0.2F, 0.9F);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth((float) EspSettings.lineThickness);
        try (BuiltBuffer builtBuffer = buffer.endNullable()) {
            if (builtBuffer != null) {
                RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }
        } finally {
            RenderSystem.lineWidth(1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void rescanStorageEsp(MinecraftClient client) {
        cachedStorageEntries.clear();
        if (client.world == null || client.player == null) return;

        Vec3d playerPos = client.player.getPos();
        int radius = Math.min(client.options.getViewDistance().getValue(), STORAGE_SCAN_CHUNK_RADIUS);
        int chunkX = (int) Math.floor(playerPos.x) >> 4;
        int chunkZ = (int) Math.floor(playerPos.z) >> 4;

        for (int cx = chunkX - radius; cx <= chunkX + radius; cx++) {
            for (int cz = chunkZ - radius; cz <= chunkZ + radius; cz++) {
                WorldChunk chunk = (WorldChunk) client.world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;

                Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
                for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
                    BlockEntityType<?> type = entry.getValue().getType();
                    if (!STORAGE_TYPES.contains(type)) continue;

                    cachedStorageEntries.add(createStorageEntry(entry.getKey(), type));
                }
            }
        }
    }

    private StorageEspEntry createStorageEntry(BlockPos pos, BlockEntityType<?> type) {
        if (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST || type == BlockEntityType.BARREL) {
            return new StorageEspEntry(pos, 1.0F, 0.8F, 0.0F);
        } else if (type == BlockEntityType.ENDER_CHEST) {
            return new StorageEspEntry(pos, 0.5F, 0.0F, 1.0F);
        } else if (type == BlockEntityType.SHULKER_BOX) {
            return new StorageEspEntry(pos, 1.0F, 0.4F, 0.7F);
        } else if (type == BlockEntityType.MOB_SPAWNER) {
            return new StorageEspEntry(pos, 0.2F, 0.8F, 1.0F);
        }

        return new StorageEspEntry(pos, 0.6F, 0.6F, 0.6F);
    }

    private void renderAncientDebrisEsp(
        MinecraftClient client,
        MatrixStack matrices,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (cachedDebrisPositions.isEmpty()) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
            RenderLayer.getLines().getDrawMode(),
            RenderLayer.getLines().getVertexFormat()
        );

        for (BlockPos pos : cachedDebrisPositions) {
            Box box = new Box(
                pos.getX() - cameraX,
                pos.getY() - cameraY,
                pos.getZ() - cameraZ,
                pos.getX() + 1 - cameraX,
                pos.getY() + 1 - cameraY,
                pos.getZ() + 1 - cameraZ
            );

            VertexRendering.drawBox(matrices, buffer, box, 0.6F, 0.3F, 0.2F, 0.9F);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(2.0F);
        try (BuiltBuffer builtBuffer = buffer.endNullable()) {
            if (builtBuffer != null) {
                RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
                BufferRenderer.drawWithGlobalProgram(builtBuffer);
            }
        } finally {
            RenderSystem.lineWidth(1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void rescanAncientDebris(MinecraftClient client) {
        cachedDebrisPositions.clear();
        if (client.world == null) return;

        Vec3d cam = client.player != null ? client.player.getPos() : Vec3d.ZERO;
        int viewDist = Math.min(client.options.getViewDistance().getValue(), DEBRIS_SCAN_CHUNK_RADIUS);
        int chunkX = (int) Math.floor(cam.x) >> 4;
        int chunkZ = (int) Math.floor(cam.z) >> 4;
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int cx = chunkX - viewDist; cx <= chunkX + viewDist; cx++) {
            for (int cz = chunkZ - viewDist; cz <= chunkZ + viewDist; cz++) {
                WorldChunk chunk = (WorldChunk) client.world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;

                int startX = cx << 4;
                int startZ = cz << 4;
                int bottomY = chunk.getBottomY();
                int topY = Math.min(bottomY + chunk.getHeight(), DEBRIS_SCAN_MAX_Y + 1);
                if (topY <= bottomY) continue;

                for (int x = startX; x < startX + 16; x++) {
                    for (int z = startZ; z < startZ + 16; z++) {
                        for (int y = bottomY; y < topY; y++) {
                            mutablePos.set(x, y, z);
                            if (chunk.getBlockState(mutablePos).isOf(Blocks.ANCIENT_DEBRIS)) {
                                cachedDebrisPositions.add(mutablePos.toImmutable());
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderGhosts(
        MinecraftClient client,
        MatrixStack matrices,
        WorldRenderContext context,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        Collection<GhostTracker.GhostPosition> ghosts = GhostTracker.getGhosts();
        if (ghosts.isEmpty()) return;

        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder lineBuffer = tessellator.begin(
            RenderLayer.getLines().getDrawMode(),
            RenderLayer.getLines().getVertexFormat()
        );

        for (GhostTracker.GhostPosition ghost : ghosts) {
            float alpha = ghost.getAlpha();
            if (alpha <= 0) continue;

            double gx = ghost.pos.x - cameraX;
            double gy = ghost.pos.y - cameraY;
            double gz = ghost.pos.z - cameraZ;

            Box box = new Box(gx - 0.4, gy, gz - 0.4, gx + 0.4, gy + 1.8, gz + 0.4);

            VertexRendering.drawBox(
                matrices, lineBuffer, box,
                1.0F, 0.6F, 0.0F, alpha * 0.8F
            );
        }

        RenderSystem.lineWidth(2.0F);
        try (BuiltBuffer built = lineBuffer.endNullable()) {
            if (built != null) {
                RenderLayer.getLines().draw(built);
            }
        } finally {
            RenderSystem.lineWidth(1.0F);
        }

        TextRenderer textRenderer = client.textRenderer;
        if (textRenderer == null) return;
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        boolean drewAny = false;

        for (GhostTracker.GhostPosition ghost : ghosts) {
            float alpha = ghost.getAlpha();
            if (alpha <= 0) continue;

            double gx = ghost.pos.x - cameraX;
            double gy = ghost.pos.y + 2.1 - cameraY;
            double gz = ghost.pos.z - cameraZ;

            double dist = Math.sqrt(
                (ghost.pos.x - client.player.getX()) * (ghost.pos.x - client.player.getX())
                + (ghost.pos.y - client.player.getY()) * (ghost.pos.y - client.player.getY())
                + (ghost.pos.z - client.player.getZ()) * (ghost.pos.z - client.player.getZ())
            );

            String label = ghost.source + " [" + (int) dist + "m]";

            matrices.push();
            matrices.translate(gx, gy, gz);
            matrices.multiply(context.camera().getRotation());
            float scale = Math.max(0.025F, (float) dist * 0.01F);
            matrices.scale(-scale, -scale, scale);

            int textWidth = textRenderer.getWidth(label);
            float tx = -textWidth / 2.0F;

            Matrix4f mat = matrices.peek().getPositionMatrix();
            int color = (((int) (alpha * 255)) << 24) | 0x00FFFFFF;
            int backgroundColor = (((int) (alpha * 0.45F * 255)) << 24);
            textRenderer.draw(label, tx, 0, color, false, mat,
                consumers,
                TextRenderer.TextLayerType.SEE_THROUGH, backgroundColor, 0xF000F0);
            drewAny = true;

            matrices.pop();
        }

        if (drewAny) {
            consumers.draw();
        }
    }

    private List<Entity> collectRenderableEntities(MinecraftClient client, Vec3d cameraPos) {
        List<Entity> entities = new ArrayList<>();
        double maxDistanceSq = MAX_ENTITY_ESP_DISTANCE * MAX_ENTITY_ESP_DISTANCE;

        if (EspSettings.renderPlayers) {
            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                if (player == client.player || player.isSpectator()) {
                    continue;
                }
                if (cameraPos.squaredDistanceTo(player.getPos()) > maxDistanceSq) {
                    continue;
                }
                entities.add(player);
            }
        }

        if (EspSettings.renderMobs) {
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof MobEntity mob)) {
                    continue;
                }
                if (cameraPos.squaredDistanceTo(mob.getPos()) > maxDistanceSq) {
                    continue;
                }
                entities.add(mob);
            }
        }

        return entities;
    }

    private List<ItemEntity> collectRenderableItems(MinecraftClient client, Vec3d cameraPos) {
        List<ItemEntity> items = new ArrayList<>();
        double maxDistanceSq = MAX_ENTITY_ESP_DISTANCE * MAX_ENTITY_ESP_DISTANCE;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ItemEntity item)) {
                continue;
            }
            if (cameraPos.squaredDistanceTo(item.getPos()) > maxDistanceSq) {
                continue;
            }
            items.add(item);
        }

        return items;
    }

    private double getStorageRenderDistance(MinecraftClient client) {
        return Math.min(client.options.getViewDistance().getValue(), STORAGE_SCAN_CHUNK_RADIUS) * 16.0D;
    }

    private record StorageEspEntry(BlockPos pos, float red, float green, float blue) {
    }

    private Box toCameraRelativeBox(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta) {
        Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
        Box baseBox = entity.getBoundingBox();
        Vec3d entityPos = entity.getPos();
        Box worldBox = baseBox.offset(
            lerpedPos.x - entityPos.x,
            lerpedPos.y - entityPos.y,
            lerpedPos.z - entityPos.z
        );

        return worldBox
            .offset(-cameraX, -cameraY, -cameraZ)
            .expand(0.05D);
    }
}
