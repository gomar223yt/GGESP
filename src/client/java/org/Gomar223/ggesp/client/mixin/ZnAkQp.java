package org.Gomar223.ggesp.client.mixin;

import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import org.Gomar223.ggesp.client.TmYpRc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public abstract class ZnAkQp {
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void ggesp$clearLavaFogDuringFreecam(
        Camera camera,
        BackgroundRenderer.FogType fogType,
        Vector4f color,
        float viewDistance,
        boolean thickenFog,
        float tickDelta,
        CallbackInfoReturnable<Fog> cir
    ) {
        if (TmYpRc.isActive() && camera.getSubmersionType() == CameraSubmersionType.LAVA) {
            cir.setReturnValue(new Fog(0.0F, viewDistance, FogShape.CYLINDER, 0.9F, 0.25F, 0.02F, color.w));
        }
    }
}
