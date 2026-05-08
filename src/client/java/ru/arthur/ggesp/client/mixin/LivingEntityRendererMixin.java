package ru.arthur.ggesp.client.mixin;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.arthur.ggesp.client.WallModelRenderState;
import ru.arthur.ggesp.client.WallRenderLayers;

@Mixin(net.minecraft.client.render.entity.LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
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
        if (WallModelRenderState.isActive()) {
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
        if (WallModelRenderState.isActive() && WallModelRenderState.areCustomLayersEnabled()) {
            cir.setReturnValue(WallRenderLayers.getEntityNoDepth(this.getTexture(state)));
        }
    }
}
