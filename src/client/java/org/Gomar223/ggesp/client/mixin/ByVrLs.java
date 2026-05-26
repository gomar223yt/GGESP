package org.Gomar223.ggesp.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.world.BlockView;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.Gomar223.ggesp.client.TmYpRc;

@Mixin(Camera.class)
public abstract class ByVrLs {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void ggesp$overrideFreecamCamera(
        BlockView area,
        Entity focusedEntity,
        boolean thirdPerson,
        boolean inverseView,
        float tickDelta,
        CallbackInfo ci
    ) {
        if (TmYpRc.isActive()) {
            setPos(
                TmYpRc.getRenderX(tickDelta),
                TmYpRc.getRenderY(tickDelta),
                TmYpRc.getRenderZ(tickDelta)
            );
            setRotation(TmYpRc.getYaw(), TmYpRc.getPitch());
        }
    }
}
