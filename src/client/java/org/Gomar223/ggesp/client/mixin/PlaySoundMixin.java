package org.Gomar223.ggesp.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.Gomar223.ggesp.client.GhostTracker;

@Mixin(ClientPlayNetworkHandler.class)
public class PlaySoundMixin {
    @Inject(method = "onPlaySound", at = @At("RETURN"))
    private void ggesp$onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (!GhostTracker.isEnabled()) return;

        SoundCategory cat = packet.getCategory();
        if (cat != SoundCategory.PLAYERS && cat != SoundCategory.HOSTILE && cat != SoundCategory.NEUTRAL) {
            return;
        }

        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        double distSq = client.player.squaredDistanceTo(x, y, z);
        if (distSq < 9.0) return;

        GhostTracker.addGhost(x, y, z, "Sound: " + cat.getName());
    }
}
