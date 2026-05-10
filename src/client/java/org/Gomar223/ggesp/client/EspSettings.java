package org.Gomar223.ggesp.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.Gomar223.ggesp.GGESP;

public final class EspSettings {
    public static final String KEY_CATEGORY = "category." + GGESP.MOD_ID;
    public static final String GUI_KEY_ID = "key." + GGESP.MOD_ID + ".click_gui";
    public static final String TOGGLE_KEY_ID = "key." + GGESP.MOD_ID + ".toggle_esp";
    public static final String FREECAM_KEY_ID = "key." + GGESP.MOD_ID + ".freecam";
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(GGESP.MOD_ID + ".properties");

    public static boolean espEnabled = true;
    public static boolean boxes = true;
    public static boolean renderPlayers = true;
    public static boolean renderMobs = true;
    public static boolean filledBoxes = false;
    public static boolean tracers = false;
    public static boolean friendTracers = false;
    public static boolean friends = true;
    public static boolean nametags = true;
    public static boolean storageEsp = false;
    public static boolean ancientDebrisEsp = false;
    public static boolean itemEsp = false;
    public static boolean ghostEsp = true;
    public static boolean wallModels = false;
    public static boolean freecam = false;

    public static float red = 1.0F;
    public static float green = 0.1F;
    public static float blue = 0.1F;
    public static float alpha = 0.9F;
    public static float friendTracerRed = 0.1F;
    public static float friendTracerGreen = 0.8F;
    public static float friendTracerBlue = 1.0F;
    public static double lineThickness = 1.0D;

    private static final Set<String> friendsList = new LinkedHashSet<>();
    private static final Map<String, FriendSettings> friendSettings = new LinkedHashMap<>();
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

        loadConfig();
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
        saveConfig();
    }

    public static void setToggleEspKey(InputUtil.Key key) {
        getToggleEspKeyBinding().setBoundKey(key);
        KeyBinding.updateKeysByCode();
        saveConfig();
    }

    public static InputUtil.Key getBoundGuiKey() {
        KeyBinding binding = getGuiKeyBinding();
        return binding.isUnbound() ? InputUtil.UNKNOWN_KEY : KeyBindingHelper.getBoundKeyOf(binding);
    }

    public static InputUtil.Key getBoundToggleEspKey() {
        KeyBinding binding = getToggleEspKeyBinding();
        return binding.isUnbound() ? InputUtil.UNKNOWN_KEY : KeyBindingHelper.getBoundKeyOf(binding);
    }

    public static void addFriend(String name) {
        String normalized = normalizeFriendName(name);
        if (!normalized.isEmpty()) {
            friendsList.add(normalized);
            friendSettings.computeIfAbsent(normalized, unused -> new FriendSettings(
                friendTracerRed,
                friendTracerGreen,
                friendTracerBlue,
                friendTracers
            ));
            saveConfig();
        }
    }

    public static void removeFriend(String name) {
        String normalized = normalizeFriendName(name);
        if (friendsList.remove(normalized)) {
            friendSettings.remove(normalized);
            saveConfig();
        }
    }

    public static boolean isFriend(String name) {
        return friends && friendsList.contains(normalizeFriendName(name));
    }

    public static Set<String> getFriends() {
        return Collections.unmodifiableSet(friendsList);
    }

    public static FriendSettings getFriendSettings(String name) {
        String normalized = normalizeFriendName(name);
        if (normalized.isEmpty()) {
            return new FriendSettings(friendTracerRed, friendTracerGreen, friendTracerBlue, friendTracers);
        }

        FriendSettings settings = friendSettings.computeIfAbsent(normalized, unused -> new FriendSettings(
            friendTracerRed,
            friendTracerGreen,
            friendTracerBlue,
            friendTracers
        ));
        return settings;
    }

    public static boolean areFriendTracersEnabled(String name) {
        return isFriend(name) && getFriendSettings(name).tracers();
    }

    public static boolean hasEnabledFriendTracers() {
        return friends && friendsList.stream().anyMatch(friend -> getFriendSettings(friend).tracers());
    }

    public static float getFriendTracerRed(String name) {
        return getFriendSettings(name).red();
    }

    public static float getFriendTracerGreen(String name) {
        return getFriendSettings(name).green();
    }

    public static float getFriendTracerBlue(String name) {
        return getFriendSettings(name).blue();
    }

    public static void setFriendTracerRed(String name, float value) {
        getFriendSettings(name).setRed(value);
        saveConfig();
    }

    public static void setFriendTracerGreen(String name, float value) {
        getFriendSettings(name).setGreen(value);
        saveConfig();
    }

    public static void setFriendTracerBlue(String name, float value) {
        getFriendSettings(name).setBlue(value);
        saveConfig();
    }

    public static void setFriendTracers(String name, boolean value) {
        getFriendSettings(name).setTracers(value);
        saveConfig();
    }

    private static String normalizeFriendName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public static void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("espEnabled", Boolean.toString(espEnabled));
        properties.setProperty("boxes", Boolean.toString(boxes));
        properties.setProperty("renderPlayers", Boolean.toString(renderPlayers));
        properties.setProperty("renderMobs", Boolean.toString(renderMobs));
        properties.setProperty("filledBoxes", Boolean.toString(filledBoxes));
        properties.setProperty("tracers", Boolean.toString(tracers));
        properties.setProperty("friendTracers", Boolean.toString(friendTracers));
        properties.setProperty("friends", Boolean.toString(friends));
        properties.setProperty("nametags", Boolean.toString(nametags));
        properties.setProperty("storageEsp", Boolean.toString(storageEsp));
        properties.setProperty("ancientDebrisEsp", Boolean.toString(ancientDebrisEsp));
        properties.setProperty("itemEsp", Boolean.toString(itemEsp));
        properties.setProperty("ghostEsp", Boolean.toString(ghostEsp));
        properties.setProperty("wallModels", Boolean.toString(wallModels));
        properties.setProperty("red", Float.toString(red));
        properties.setProperty("green", Float.toString(green));
        properties.setProperty("blue", Float.toString(blue));
        properties.setProperty("alpha", Float.toString(alpha));
        properties.setProperty("friendTracerRed", Float.toString(friendTracerRed));
        properties.setProperty("friendTracerGreen", Float.toString(friendTracerGreen));
        properties.setProperty("friendTracerBlue", Float.toString(friendTracerBlue));
        properties.setProperty("lineThickness", Double.toString(lineThickness));
        properties.setProperty("guiKey", getBoundGuiKey().getTranslationKey());
        properties.setProperty("toggleEspKey", getBoundToggleEspKey().getTranslationKey());
        properties.setProperty("friendsList", String.join(",", friendsList));
        for (String friend : friendsList) {
            FriendSettings settings = getFriendSettings(friend);
            properties.setProperty("friend." + friend + ".red", Float.toString(settings.red()));
            properties.setProperty("friend." + friend + ".green", Float.toString(settings.green()));
            properties.setProperty("friend." + friend + ".blue", Float.toString(settings.blue()));
            properties.setProperty("friend." + friend + ".tracers", Boolean.toString(settings.tracers()));
        }

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                properties.store(writer, "GGESP settings");
            }
        } catch (IOException e) {
            GGESP.LOGGER.warn("Failed to save GGESP config.", e);
        }
    }

    private static void loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            properties.load(reader);
        } catch (IOException e) {
            GGESP.LOGGER.warn("Failed to load GGESP config.", e);
            return;
        }

        espEnabled = getBoolean(properties, "espEnabled", espEnabled);
        boxes = getBoolean(properties, "boxes", boxes);
        renderPlayers = getBoolean(properties, "renderPlayers", renderPlayers);
        renderMobs = getBoolean(properties, "renderMobs", renderMobs);
        filledBoxes = getBoolean(properties, "filledBoxes", filledBoxes);
        tracers = getBoolean(properties, "tracers", tracers);
        friendTracers = getBoolean(properties, "friendTracers", friendTracers);
        friends = getBoolean(properties, "friends", friends);
        nametags = getBoolean(properties, "nametags", nametags);
        storageEsp = getBoolean(properties, "storageEsp", storageEsp);
        ancientDebrisEsp = getBoolean(properties, "ancientDebrisEsp", ancientDebrisEsp);
        itemEsp = getBoolean(properties, "itemEsp", itemEsp);
        ghostEsp = getBoolean(properties, "ghostEsp", ghostEsp);
        wallModels = getBoolean(properties, "wallModels", wallModels);
        red = getFloat(properties, "red", red);
        green = getFloat(properties, "green", green);
        blue = getFloat(properties, "blue", blue);
        alpha = getFloat(properties, "alpha", alpha);
        friendTracerRed = getFloat(properties, "friendTracerRed", friendTracerRed);
        friendTracerGreen = getFloat(properties, "friendTracerGreen", friendTracerGreen);
        friendTracerBlue = getFloat(properties, "friendTracerBlue", friendTracerBlue);
        lineThickness = getDouble(properties, "lineThickness", lineThickness);
        loadKey(guiKeyBinding, properties.getProperty("guiKey"));
        loadKey(toggleEspKeyBinding, properties.getProperty("toggleEspKey"));
        KeyBinding.updateKeysByCode();

        friendsList.clear();
        friendSettings.clear();
        String savedFriends = properties.getProperty("friendsList", "");
        friendsList.addAll(Arrays.stream(savedFriends.split(","))
            .map(EspSettings::normalizeFriendName)
            .filter(name -> !name.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new)));
        for (String friend : friendsList) {
            friendSettings.put(friend, new FriendSettings(
                getFloat(properties, "friend." + friend + ".red", friendTracerRed),
                getFloat(properties, "friend." + friend + ".green", friendTracerGreen),
                getFloat(properties, "friend." + friend + ".blue", friendTracerBlue),
                getBoolean(properties, "friend." + friend + ".tracers", friendTracers)
            ));
        }
    }

    private static void loadKey(KeyBinding binding, String translationKey) {
        if (translationKey == null || translationKey.isBlank()) {
            return;
        }

        try {
            binding.setBoundKey(InputUtil.fromTranslationKey(translationKey));
        } catch (IllegalArgumentException e) {
            GGESP.LOGGER.warn("Ignoring invalid GGESP key binding: {}", translationKey);
        }
    }

    private static boolean getBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static float getFloat(Properties properties, String key, float fallback) {
        try {
            return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double getDouble(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static final class FriendSettings {
        private float red;
        private float green;
        private float blue;
        private boolean tracers;

        private FriendSettings(float red, float green, float blue, boolean tracers) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.tracers = tracers;
        }

        public float red() {
            return red;
        }

        public float green() {
            return green;
        }

        public float blue() {
            return blue;
        }

        public boolean tracers() {
            return tracers;
        }

        private void setRed(float red) {
            this.red = clampColor(red);
        }

        private void setGreen(float green) {
            this.green = clampColor(green);
        }

        private void setBlue(float blue) {
            this.blue = clampColor(blue);
        }

        private void setTracers(boolean tracers) {
            this.tracers = tracers;
        }

        private static float clampColor(float value) {
            return Math.max(0.0F, Math.min(1.0F, value));
        }
    }
}
