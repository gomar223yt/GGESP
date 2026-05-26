package org.Gomar223.ggesp.client.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.Gomar223.ggesp.client.HmDqSv;
import org.Gomar223.ggesp.client.JcNpYw;

@Mixin(RenderLayer.class)
public class CqIwUz {
    @Inject(method = "getArmorCutoutNoCull", at = @At("HEAD"), cancellable = true)
    private static void ggesp$wallArmorLayer(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        if (HmDqSv.isActive() && HmDqSv.areCustomLayersEnabled()) {
            cir.setReturnValue(JcNpYw.getArmorNoDepth(texture));
        }
    }

    @Inject(method = "createArmorDecalCutoutNoCull", at = @At("HEAD"), cancellable = true)
    private static void ggesp$wallArmorDecalLayer(Identifier texture, CallbackInfoReturnable<RenderLayer> cir) {
        if (HmDqSv.isActive() && HmDqSv.areCustomLayersEnabled()) {
            cir.setReturnValue(JcNpYw.getArmorDecalNoDepth(texture));
        }
    }
}
