package org.Gomar223.ggesp.client.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.Gomar223.ggesp.client.KxVbNq;
import org.Gomar223.ggesp.client.HmDqSv;
import org.Gomar223.ggesp.client.JcNpYw;

@Mixin(net.minecraft.client.render.entity.LivingEntityRenderer.class)
public abstract class UlPcVd {
    @Shadow
    public abstract Identifier getTexture(LivingEntityRenderState state);

    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
        at = @At("TAIL")
    )
    private void ggesp$allowInvisibleFeatures(
        LivingEntity entity,
        LivingEntityRenderState state,
        float tickDelta,
        CallbackInfo ci
    ) {
        if (HmDqSv.isActive()
            || (KxVbNq.espEnabled && KxVbNq.wallModels && KxVbNq.renderPlayers && entity instanceof PlayerEntity)) {
            state.invisibleToPlayer = false;
        }
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
    private void ggesp$wallEntityLayer(
        LivingEntityRenderState state,
        boolean showBody,
        boolean translucent,
        boolean showOutline,
        CallbackInfoReturnable<RenderLayer> cir
    ) {
        if (HmDqSv.isActive() && HmDqSv.areCustomLayersEnabled()) {
            cir.setReturnValue(JcNpYw.getEntityNoDepth(this.getTexture(state)));
        }
    }
}
