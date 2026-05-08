package ru.arthur.ggesp.client.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.arthur.ggesp.client.WallModelRenderState;
import ru.arthur.ggesp.client.WallRenderLayers;

@Mixin(RenderLayer.class)
public class RenderLayerMixin {
    @Inject(method = "getArmorCutoutNoCull", at = @At("HEAD"), cancellable = true)
    private static void ggesp$wallArmorLayer(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        if (WallModelRenderState.isActive() && WallModelRenderState.areCustomLayersEnabled()) {
            cir.setReturnValue(WallRenderLayers.getArmorNoDepth(texture));
        }
    }

    @Inject(method = "createArmorDecalCutoutNoCull", at = @At("HEAD"), cancellable = true)
    private static void ggesp$wallArmorDecalLayer(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        if (WallModelRenderState.isActive() && WallModelRenderState.areCustomLayersEnabled()) {
            cir.setReturnValue(WallRenderLayers.getArmorDecalNoDepth(texture));
        }
    }
}
