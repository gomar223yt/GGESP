package org.Gomar223.ggesp.client.mixin;

import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.Gomar223.ggesp.client.FreecamController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void ggesp$hideInWallOverlayDuringFreecam(
        Sprite sprite,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        CallbackInfo ci
    ) {
        if (FreecamController.isActive()) {
            ci.cancel();
        }
    }
}
