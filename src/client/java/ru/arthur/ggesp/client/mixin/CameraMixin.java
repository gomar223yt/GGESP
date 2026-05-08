package ru.arthur.ggesp.client.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.world.BlockView;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.arthur.ggesp.client.FreecamController;

@Mixin(Camera.class)
public abstract class CameraMixin {
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
        if (FreecamController.isActive()) {
            setPos(FreecamController.getX(), FreecamController.getY(), FreecamController.getZ());
            setRotation(FreecamController.getYaw(), FreecamController.getPitch());
        }
    }
}
