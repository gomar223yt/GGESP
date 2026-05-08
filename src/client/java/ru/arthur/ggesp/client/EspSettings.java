package ru.arthur.ggesp.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.arthur.ggesp.GGESP;

public final class EspSettings {
    public static final String KEY_CATEGORY = "category." + GGESP.MOD_ID;
    public static final String GUI_KEY_ID = "key." + GGESP.MOD_ID + ".click_gui";
    public static final String TOGGLE_KEY_ID = "key." + GGESP.MOD_ID + ".toggle_esp";
    public static final String FREECAM_KEY_ID = "key." + GGESP.MOD_ID + ".freecam";

    public static boolean espEnabled = true;
    public static boolean boxes = true;
    public static boolean renderPlayers = true;
    public static boolean renderMobs = true;
    public static boolean filledBoxes = false;
    public static boolean tracers = false;
    public static boolean nametags = true;
    public static boolean storageEsp = false;
    public static boolean ancientDebrisEsp = false;
    public static boolean ghostEsp = true;
    public static boolean wallModels = false;
    public static boolean freecam = false;

    public static float red = 1.0F;
    public static float green = 0.1F;
    public static float blue = 0.1F;
    public static float alpha = 0.9F;
    public static double lineThickness = 1.0D;

    private static boolean initialized;
    private static KeyBinding guiKeyBinding;
    private static KeyBinding toggleEspKeyBinding;
    private static KeyBinding freecamKeyBinding;

    private EspSettings() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            GUI_KEY_ID,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            KEY_CATEGORY
        ));

        toggleEspKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            TOGGLE_KEY_ID,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
        ));

        freecamKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            FREECAM_KEY_ID,
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F4,
            KEY_CATEGORY
        ));

        initialized = true;
    }

    public static KeyBinding getGuiKeyBinding() {
        if (guiKeyBinding == null) {
            throw new IllegalStateException("GGESP keybindings are not initialized yet.");
        }

        return guiKeyBinding;
    }

    public static KeyBinding getToggleEspKeyBinding() {
        if (toggleEspKeyBinding == null) {
            throw new IllegalStateException("GGESP keybindings are not initialized yet.");
        }

        return toggleEspKeyBinding;
    }

    public static KeyBinding getFreecamKeyBinding() {
        if (freecamKeyBinding == null) {
            throw new IllegalStateException("GGESP keybindings are not initialized yet.");
        }

        return freecamKeyBinding;
    }

    public static void setGuiKey(InputUtil.Key key) {
        getGuiKeyBinding().setBoundKey(key);
        KeyBinding.updateKeysByCode();
    }

    public static void setToggleEspKey(InputUtil.Key key) {
        getToggleEspKeyBinding().setBoundKey(key);
        KeyBinding.updateKeysByCode();
    }

    public static InputUtil.Key getBoundGuiKey() {
        KeyBinding binding = getGuiKeyBinding();
        return binding.isUnbound() ? InputUtil.UNKNOWN_KEY : KeyBindingHelper.getBoundKeyOf(binding);
    }

    public static InputUtil.Key getBoundToggleEspKey() {
        KeyBinding binding = getToggleEspKeyBinding();
        return binding.isUnbound() ? InputUtil.UNKNOWN_KEY : KeyBindingHelper.getBoundKeyOf(binding);
    }
}
