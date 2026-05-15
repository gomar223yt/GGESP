package org.Gomar223.ggesp.client.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.Gomar223.ggesp.client.EspSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void ggesp$customHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        StatusEffectInstance fatigue = entity.getStatusEffect(StatusEffects.MINING_FATIGUE);
        if (fatigue != null) {
            cir.setReturnValue(6 + (1 + fatigue.getAmplifier()) * 2);
            return;
        }

        if (EspSettings.customHand) {
            cir.setReturnValue(Math.max(1, Math.min(20, (int) Math.round(EspSettings.customHandAnimationSpeed))));
        } else {
            cir.setReturnValue(6);
        }
    }
}
