package org.Gomar223.ggesp.client.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.Gomar223.ggesp.client.FreecamController;

@Mixin(ClientPlayerEntity.class)
public abstract class FreecamPlayerMixin {
    @Unique
    private double ggesp$savedX;
    @Unique
    private double ggesp$savedY;
    @Unique
    private double ggesp$savedZ;
    @Unique
    private float ggesp$savedYaw;
    @Unique
    private float ggesp$savedPitch;
    @Unique
    private float ggesp$savedHeadYaw;

    /**
     * Save the player's position and rotation before tick processing,
     * so we can restore it afterwards to keep the player frozen in place.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void ggesp$preTickSaveState(CallbackInfo ci) {
        if (!FreecamController.isActive()) {
            return;
        }

        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        ggesp$savedX = self.getX();
        ggesp$savedY = self.getY();
        ggesp$savedZ = self.getZ();
        ggesp$savedYaw = self.getYaw();
        ggesp$savedPitch = self.getPitch();
        ggesp$savedHeadYaw = self.headYaw;
    }

    /**
     * After tick, restore the player position/rotation so the player entity
     * stays frozen at the original spot. The server never sees any movement.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void ggesp$postTickRestoreState(CallbackInfo ci) {
        if (!FreecamController.isActive()) {
            return;
        }

        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        self.setPosition(ggesp$savedX, ggesp$savedY, ggesp$savedZ);
        self.setYaw(ggesp$savedYaw);
        self.setPitch(ggesp$savedPitch);
        self.headYaw = ggesp$savedHeadYaw;
        self.setVelocity(Vec3d.ZERO);
    }

    /**
     * Block all movement packets from being sent to the server while
     * freecam is active. The server thinks the player is standing still.
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void ggesp$blockMovementPackets(CallbackInfo ci) {
        if (FreecamController.isActive()) {
            ci.cancel();
        }
    }
}
