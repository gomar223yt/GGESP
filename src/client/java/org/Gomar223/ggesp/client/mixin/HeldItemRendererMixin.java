package org.Gomar223.ggesp.client.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.Gomar223.ggesp.client.CustomHandAnimations;
import org.Gomar223.ggesp.client.EspSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Unique
    private Hand ggesp$currentHand;
    @Unique
    private ItemStack ggesp$currentStack = ItemStack.EMPTY;
    @Unique
    private boolean ggesp$currentPlayerUsingItem;

    @Inject(
        method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD")
    )
    private void ggesp$trackHand(
        AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        Hand hand,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        ggesp$currentHand = hand;
        ggesp$currentStack = item;
        ggesp$currentPlayerUsingItem = player.isUsingItem();
    }

    @Inject(
        method = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("RETURN")
    )
    private void ggesp$clearHand(
        AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        Hand hand,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        ggesp$currentHand = null;
        ggesp$currentStack = ItemStack.EMPTY;
        ggesp$currentPlayerUsingItem = false;
    }

    @Inject(method = "applyEquipOffset", at = @At("HEAD"), cancellable = true)
    private void ggesp$applyCustomEquipOffset(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        boolean offhand = ggesp$currentHand == Hand.OFF_HAND;
        CustomHandAnimations.applyEquipOffset(matrices, arm, equipProgress, offhand);
        ci.cancel();
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void ggesp$applyCustomSwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        boolean offhand = ggesp$currentHand == Hand.OFF_HAND;
        if (EspSettings.customHand && !offhand && ggesp$canUseCustomSwing()) {
            if (!ggesp$currentStack.isEmpty()) {
                CustomHandAnimations.cancelVanillaSwingTranslation(matrices, arm, swingProgress);
            }
            CustomHandAnimations.applyPulseAnimation(
                matrices,
                swingProgress,
                () -> CustomHandAnimations.applyVanillaSwing(matrices, arm, swingProgress),
                false,
                arm == Arm.RIGHT
            );
        } else {
            CustomHandAnimations.applyVanillaSwing(matrices, arm, swingProgress);
        }
        ci.cancel();
    }

    @Unique
    private boolean ggesp$canUseCustomSwing() {
        return !ggesp$currentPlayerUsingItem
            && !ggesp$currentStack.isEmpty()
            && !ggesp$currentStack.isOf(Items.CROSSBOW)
            && !ggesp$currentStack.contains(DataComponentTypes.MAP_ID);
    }
}
