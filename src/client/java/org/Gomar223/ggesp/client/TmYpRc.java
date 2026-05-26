package org.Gomar223.ggesp.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.Gomar223.ggesp.AeKtQm;

public final class TmYpRc {
    private static boolean active = false;
    private static double posX, posY, posZ;
    private static double prevPosX, prevPosY, prevPosZ;
    private static double anchorX, anchorY, anchorZ;
    private static float anchorYaw, anchorPitch;
    private static float yaw, pitch;

    private static final double BASE_SPEED = 0.5;
    private static final double SPRINT_MULTIPLIER = 2.5;

    private TmYpRc() {
    }

    public static boolean isActive() {
        return active;
    }

    public static double getX() {
        return posX;
    }

    public static double getY() {
        return posY;
    }

    public static double getZ() {
        return posZ;
    }

    public static double getRenderX(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPosX, posX);
    }

    public static double getRenderY(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPosY, posY);
    }

    public static double getRenderZ(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevPosZ, posZ);
    }

    public static float getYaw() {
        return yaw;
    }

    public static float getPitch() {
        return pitch;
    }

    public static void toggle() {
        if (active) {
            disable();
        } else {
            enable();
        }
    }

    public static void enable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        posX = client.player.getX();
        posY = client.player.getEyeY();
        posZ = client.player.getZ();
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        anchorX = client.player.getX();
        anchorY = client.player.getY();
        anchorZ = client.player.getZ();
        anchorYaw = client.player.getYaw();
        anchorPitch = client.player.getPitch();
        yaw = client.player.getYaw();
        pitch = client.player.getPitch();

        active = true;
        syncClientPlayerToFreecam();
        AeKtQm.LOGGER.info("Freecam enabled at [{}, {}, {}]", (int) posX, (int) posY, (int) posZ);
    }

    public static void disable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.setPosition(anchorX, anchorY, anchorZ);
            client.player.setYaw(anchorYaw);
            client.player.setPitch(anchorPitch);
        }

        active = false;
        AeKtQm.LOGGER.info("Freecam disabled");
    }

    public static void handleMouseInput(double deltaX, double deltaY) {
        if (!active) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        double sensitivity = client.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double factor = sensitivity * sensitivity * sensitivity * 8.0;

        yaw = yaw + (float) (deltaX * factor);
        pitch = MathHelper.clamp(pitch + (float) (deltaY * factor), -90.0F, 90.0F);
    }

    public static void tickMovement() {
        if (!active) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;

        if (client.currentScreen != null) {
            syncClientPlayerToFreecam();
            return;
        }

        long window = client.getWindow().getHandle();
        GameOptions options = client.options;

        double forward = 0;
        double strafe = 0;
        double vertical = 0;

        if (isKeyDown(window, options.forwardKey)) forward += 1;
        if (isKeyDown(window, options.backKey)) forward -= 1;
        if (isKeyDown(window, options.leftKey)) strafe += 1;
        if (isKeyDown(window, options.rightKey)) strafe -= 1;
        if (isKeyDown(window, options.jumpKey)) vertical += 1;
        if (isKeyDown(window, options.sneakKey)) vertical -= 1;

        if (forward != 0 || strafe != 0 || vertical != 0) {
            double speed = BASE_SPEED;
            if (isKeyDown(window, options.sprintKey)) {
                speed *= SPRINT_MULTIPLIER;
            }

            double yawRad = Math.toRadians(yaw);
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            double strafeX = Math.cos(yawRad);
            double strafeZ = Math.sin(yawRad);

            posX += (forward * forwardX + strafe * strafeX) * speed;
            posY += vertical * speed;
            posZ += (forward * forwardZ + strafe * strafeZ) * speed;
        }

        syncClientPlayerToFreecam();
    }

    public static void syncClientPlayerToFreecam() {
        if (!active) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.setPosition(posX, posY - client.player.getEyeHeight(client.player.getPose()), posZ);
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
    }

    private static boolean isKeyDown(long window, KeyBinding binding) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(binding);
        if (key.equals(InputUtil.UNKNOWN_KEY)) {
            return false;
        }

        return InputUtil.isKeyPressed(window, key.getCode());
    }
}
