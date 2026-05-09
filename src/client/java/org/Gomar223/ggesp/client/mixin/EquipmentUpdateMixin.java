package org.Gomar223.ggesp.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.Gomar223.ggesp.client.GhostTracker;

@Mixin(ClientPlayNetworkHandler.class)
public class EquipmentUpdateMixin {
    @Inject(method = "onEntityEquipmentUpdate", at = @At("HEAD"))
    private void ggesp$onEquipmentUpdate(EntityEquipmentUpdateS2CPacket packet, CallbackInfo ci) {
        if (!GhostTracker.isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        int entityId = packet.getEntityId();
        Entity entity = client.world.getEntityById(entityId);

        if (entity == null || entity == client.player) {
            return;
        }

        double distSq = client.player.squaredDistanceTo(entity);
        if (distSq > 9.0) {
            GhostTracker.addGhost(entity.getX(), entity.getY(), entity.getZ(), "Equip #" + entityId);
        }
    }
}
