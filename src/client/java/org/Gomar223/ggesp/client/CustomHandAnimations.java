package org.Gomar223.ggesp.client;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class CustomHandAnimations {
    public static final String[] MODES = {
        "Обычная",
        "Наклон",
        "Взмах",
        "Вращение",
        "Увеличение",
        "Уменьшение",
        "Динамичная",
        "Растяжение"
    };

    private CustomHandAnimations() {
    }

    public static void cycleMode() {
        int index = 0;
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equals(EspSettings.customHandAnimationMode)) {
                index = i;
                break;
            }
        }
        EspSettings.customHandAnimationMode = MODES[(index + 1) % MODES.length];
        EspSettings.saveConfig();
    }

    public static void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress, boolean offhand) {
        float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
        if (!EspSettings.customHand) {
            matrices.translate(side * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
            return;
        }

        float scale = (float) (offhand ? EspSettings.customHandOffScale : EspSettings.customHandMainScale);
        float offsetX = (float) (offhand ? EspSettings.customHandOffX : EspSettings.customHandMainX);
        float offsetY = (float) (offhand ? EspSettings.customHandOffY : EspSettings.customHandMainY);
        float offsetZ = (float) (offhand ? EspSettings.customHandOffZ : EspSettings.customHandMainZ);

        matrices.translate(side * 0.56F, -0.52F, -0.72F);
        matrices.scale(scale, scale, scale);
        matrices.translate(offsetX, offsetY, offsetZ);
    }

    public static void applyVanillaSwing(MatrixStack matrices, Arm arm, float swingProgress) {
        float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
        float sin1 = MathHelper.sin(swingProgress * swingProgress * MathHelper.PI);
        float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * MathHelper.PI);

        rotateY(matrices, side * (45.0F + sin1 * -20.0F));
        rotateZ(matrices, side * sin2 * -20.0F);
        rotateX(matrices, sin2 * -80.0F);
        rotateY(matrices, side * -45.0F);
    }

    public static void cancelVanillaSwingTranslation(MatrixStack matrices, Arm arm, float swingProgress) {
        float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
        float swingSin = MathHelper.sin(MathHelper.sqrt(swingProgress) * MathHelper.PI);
        float swingWave = MathHelper.sin(swingProgress * MathHelper.PI);
        float swingBounce = MathHelper.sin(MathHelper.sqrt(swingProgress) * MathHelper.PI * 2.0F);
        matrices.translate(-(side * (-0.4F * swingSin)), -(0.2F * swingBounce), -(-0.2F * swingWave));
    }

    public static void applyPulseAnimation(MatrixStack matrices, float swingProgress, Runnable vanillaTransform, boolean offhand, boolean rightHand) {
        if (offhand) {
            vanillaTransform.run();
            return;
        }

        float side = rightHand ? 1.0F : -1.0F;
        float progress = swingProgress;

        float swingSin = MathHelper.sin(MathHelper.sqrt(progress) * MathHelper.PI);
        float swingSin2 = MathHelper.sin(progress * progress * MathHelper.PI);
        float swingWave = MathHelper.sin(progress * MathHelper.PI);

        float power = (float) EspSettings.customHandSwingPower / 5.0F;
        float strength = (float) EspSettings.customHandSwingPower * 10.0F;

        switch (EspSettings.customHandAnimationMode) {
            case "Обычная":
                rotateY(matrices, side * (45.0F + swingSin2 * (-strength / 4.0F)));
                rotateZ(matrices, side * swingSin * -(strength / 4.0F));
                rotateX(matrices, swingSin * -strength);
                rotateY(matrices, side * -45.0F);
                break;

            case "Наклон":
                matrices.translate(side * -0.15F * swingSin, 0.05F * swingWave, -0.10F * swingSin);
                rotateY(matrices, side * (35.0F + swingSin2 * -10.0F * power));
                rotateZ(matrices, side * -45.0F * swingSin * power);
                rotateX(matrices, -25.0F * swingSin * power);
                break;

            case "Взмах":
                matrices.translate(
                    side * (-0.4F * swingSin),
                    0.2F * MathHelper.sin(MathHelper.sqrt(progress) * MathHelper.PI * 2.0F),
                    -0.2F * swingWave
                );
                rotateY(matrices, side * (45.0F + swingSin2 * (-strength / 4.0F)));
                rotateZ(matrices, side * swingSin * -(strength / 4.0F));
                rotateX(matrices, swingSin * -strength);
                rotateY(matrices, side * -45.0F);
                break;

            case "Вращение":
                matrices.translate(side * -0.20F * swingSin, 0.10F * swingWave, -0.15F * swingSin);
                rotateY(matrices, side * 45.0F);
                rotateX(matrices, -strength * 0.5F * swingSin);
                rotateZ(matrices, side * progress * 360.0F * power);
                rotateY(matrices, side * -45.0F);
                break;

            case "Увеличение": {
                float scale = 1.0F + swingSin * 0.35F * power;
                matrices.scale(scale, scale, scale);
                rotateY(matrices, side * (45.0F + swingSin2 * (-strength / 5.0F)));
                rotateZ(matrices, side * swingSin * -15.0F * power);
                rotateX(matrices, swingSin * -35.0F * power);
                rotateY(matrices, side * -45.0F);
                break;
            }

            case "Уменьшение": {
                float scale = 1.0F - swingSin * 0.30F * power;
                scale = Math.max(scale, 0.35F);
                matrices.scale(scale, scale, scale);
                rotateY(matrices, side * (45.0F + swingSin2 * (-strength / 5.0F)));
                rotateZ(matrices, side * swingSin * -15.0F * power);
                rotateX(matrices, swingSin * -35.0F * power);
                rotateY(matrices, side * -45.0F);
                break;
            }

            case "Динамичная": {
                matrices.translate(side * (-0.25F * swingSin), 0.15F * swingWave, -0.15F * swingSin);
                rotateX(matrices, -strength * 0.75F * swingSin);
                rotateY(matrices, side * (35.0F + 25.0F * swingSin));
                rotateZ(matrices, side * -30.0F * swingSin);
                float scale = 1.0F + swingSin * 0.12F * power;
                matrices.scale(scale, scale, scale);
                break;
            }

            case "Растяжение": {
                float scaleX = 1.0F;
                float scaleY = 1.0F + swingSin * 0.25F * power;
                float scaleZ = 1.0F + swingSin * 0.45F * power;
                matrices.scale(scaleX, scaleY, scaleZ);
                rotateX(matrices, -strength * 0.5F * swingSin);
                rotateZ(matrices, side * -20.0F * swingSin);
                break;
            }

            default:
                vanillaTransform.run();
                break;
        }
    }

    private static void rotateX(MatrixStack matrices, float angle) {
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
    }

    private static void rotateY(MatrixStack matrices, float angle) {
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
    }

    private static void rotateZ(MatrixStack matrices, float angle) {
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
    }
}
