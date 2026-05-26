package org.Gomar223.ggesp.client.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.Gomar223.ggesp.client.TmYpRc;

@Mixin(ClientPlayerEntity.class)
public abstract class OxNeHb {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ggesp$syncClientPlayerToFreecam(CallbackInfo ci) {
        if (!TmYpRc.isActive()) {
            return;
        }

        TmYpRc.syncClientPlayerToFreecam();
    }

    /**
     * Block all movement packets from being sent to the server while
     * freecam is active. The server thinks the player is standing still.
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void ggesp$blockMovementPackets(CallbackInfo ci) {
        if (TmYpRc.isActive()) {
            ci.cancel();
        }
    }
}
