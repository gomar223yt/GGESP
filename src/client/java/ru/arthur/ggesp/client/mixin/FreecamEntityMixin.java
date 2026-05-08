package ru.arthur.ggesp.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.arthur.ggesp.client.FreecamController;

@Mixin(Entity.class)
public abstract class FreecamEntityMixin {
    /**
     * Redirect mouse look input to the freecam controller instead of
     * rotating the player entity. Targets Entity because changeLookDirection
     * is not overridden in ClientPlayerEntity in 1.21.4.
     */
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void ggesp$redirectLookToFreecam(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this == MinecraftClient.getInstance().player && FreecamController.isActive()) {
            FreecamController.handleMouseInput(cursorDeltaX, cursorDeltaY);
            ci.cancel();
        }
    }
}
