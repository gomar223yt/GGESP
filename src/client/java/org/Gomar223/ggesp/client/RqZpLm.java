package org.Gomar223.ggesp.client;

import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import xyz.breadloaf.imguimc.Imguimc;
import xyz.breadloaf.imguimc.interfaces.Renderable;
import xyz.breadloaf.imguimc.interfaces.Theme;

public class RqZpLm extends Screen {
    private static final String INTER_FONT_RESOURCE = "/Inter_18pt-Regular.ttf";
    private static final String ICONS_RESOURCE = "/clickGUI-icons.png";
    private static final String FRIENDS_ICONS_RESOURCE = "/friends-icons.png";
    private static final float DESIGN_WIDTH = 1044.0F;
    private static final float DESIGN_HEIGHT = 1506.0F;
    private static final int ICONS_WIDTH = 1536;
    private static final int ICONS_HEIGHT = 1024;
    private static ImFont interFont;
    private static Path interFontPath;
    private static boolean interFontAttempted;
    private static final boolean USE_INTER_FONT = false;
    private static NativeImageBackedTexture iconsTexture;
    private static int iconsTextureId;
    private static boolean iconsAttempted;
    private static NativeImageBackedTexture friendsIconsTexture;
    private static int friendsIconsTextureId;
    private static boolean friendsIconsAttempted;
    private static InterTextRenderer interTextRenderer;
    private static boolean interTextAttempted;

    private final Screen parent;
    private final PulseTheme theme = new PulseTheme();
    private final PulseRenderable renderable = new PulseRenderable();
    private final Map<String, Float> toggleAnimations = new HashMap<>();
    private final ImString friendName = new ImString(32);
    private final ImString friendSearch = new ImString(64);
    private GuiPage currentPage = GuiPage.MAIN;
    private TextFieldMode activeTextField = TextFieldMode.NONE;
    private KeyCaptureMode captureMode = KeyCaptureMode.NONE;
    private String selectedFriend = "";
    private long openedAtNanos;
    private float scrollOffset;
    private boolean pushedRenderable;

    public RqZpLm(Screen parent) {
        super(Text.literal("GGESP ClickGUI"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        openedAtNanos = System.nanoTime();
        ensureInterTextLoaded();
        ensureInterFontLoaded();
        ensureIconsLoaded();
        ensureFriendsIconsLoaded();
        if (!pushedRenderable) {
            Imguimc.pushRenderable(renderable);
            pushedRenderable = true;
        }
    }

    @Override
    public void close() {
        dismissGui();
    }

    private void dismissGui() {
        KxVbNq.saveConfig();
        if (pushedRenderable) {
            Imguimc.pullRenderable(renderable);
            pushedRenderable = false;
        }
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeTextField != TextFieldMode.NONE) {
            if (keyCode == 256) {
                activeTextField = TextFieldMode.NONE;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                if (activeTextField == TextFieldMode.FRIEND_NAME) {
                    addFriendFromInput();
                }
                activeTextField = TextFieldMode.NONE;
                return true;
            }
            if (keyCode == 259) {
                deleteLastTextFieldCharacter();
                return true;
            }
        }

        if (captureMode != KeyCaptureMode.NONE) {
            if (keyCode == 256) {
                captureMode = KeyCaptureMode.NONE;
                return true;
            }

            InputUtil.Key newKey = keyCode == 261 || keyCode == 259
                ? InputUtil.UNKNOWN_KEY
                : InputUtil.fromKeyCode(keyCode, scanCode);

            if (captureMode == KeyCaptureMode.GUI) {
                KxVbNq.setGuiKey(newKey);
            } else if (captureMode == KeyCaptureMode.ESP_TOGGLE) {
                KxVbNq.setToggleEspKey(newKey);
            } else if (captureMode == KeyCaptureMode.FREECAM) {
                KxVbNq.setFreecamKey(newKey);
            }

            captureMode = KeyCaptureMode.NONE;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (activeTextField != TextFieldMode.NONE && chr >= 32 && chr != 127) {
            ImString value = activeTextField == TextFieldMode.FRIEND_NAME ? friendName : friendSearch;
            if (value.get().length() < 32) {
                value.set(value.get() + chr);
            }
            return true;
        }

        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    private final class PulseRenderable implements Renderable {
        @Override
        public String getName() {
            return "GGESP ClickGUI";
        }

        @Override
        public Theme getTheme() {
            return theme;
        }

        @Override
        public void render() {
            boolean useInter = interFont != null;
            if (useInter) {
                ImGui.pushFont(interFont);
            }

            int flags = ImGuiWindowFlags.NoDecoration
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoSavedSettings
                | ImGuiWindowFlags.NoScrollbar
                | ImGuiWindowFlags.NoScrollWithMouse
                | ImGuiWindowFlags.NoBackground;

            float displayW = Math.max(1.0F, ImGui.getIO().getDisplaySizeX());
            float displayH = Math.max(1.0F, ImGui.getIO().getDisplaySizeY());
            float openProgress = easeOutCubic(Math.min(1.0F, (System.nanoTime() - openedAtNanos) / 220_000_000.0F));
            float panelW = Math.min(Math.max(420.0F, displayW * 0.44F), Math.min(620.0F, displayW - 28.0F));
            float panelH = Math.min(860.0F, displayH - 28.0F);
            float panelOpenScale = 0.94F + 0.06F * openProgress;
            panelW *= panelOpenScale;
            panelH *= panelOpenScale;
            float scale = Math.min(1.0F, panelW / 560.0F);
            float panelX = (displayW - panelW) * 0.5F;
            float panelY = (displayH - panelH) * 0.5F;
            float overlayPadding = 22.0F * panelOpenScale;

            ImGui.setNextWindowPos(panelX - overlayPadding, panelY - overlayPadding);
            ImGui.setNextWindowSize(panelW + overlayPadding * 2.0F, panelH + overlayPadding * 2.0F);
            if (!ImGui.begin("##ggesp_root", flags)) {
                ImGui.end();
                if (useInter) {
                    ImGui.popFont();
                }
                return;
            }

            ImDrawList draw = ImGui.getWindowDrawList();
            draw.addRectFilled(
                panelX - overlayPadding,
                panelY - overlayPadding,
                panelX + panelW + overlayPadding,
                panelY + panelH + overlayPadding,
                rgba(4, 5, 16, (int) (245.0F * openProgress)),
                24.0F
            );

            if (currentPage == GuiPage.FRIENDS) {
                drawFriendsPanel(draw, panelX, panelY, panelW, panelH, scale);
            } else {
                drawPanel(draw, panelX, panelY, panelW, panelH, scale);
            }

            ImGui.end();
            if (useInter) {
                ImGui.popFont();
            }
        }

        private void drawPanel(ImDrawList draw, float x, float y, float w, float h, float scale) {
            draw.addRectFilled(x, y, x + w, y + h, rgba(12, 13, 27, 236), 20.0F);
            draw.addRect(x, y, x + w, y + h, rgba(88, 82, 132, 82), 20.0F, 0, 1.0F);

            float padding = 26.0F * scale;
            float headerH = 82.0F * scale;
            drawLogo(draw, x + w * 0.5F - 86.0F * scale, y + 18.0F * scale, 172.0F * scale);

            float contentX = x + padding;
            float contentW = w - padding * 2.0F;
            float cardH = 43.0F * scale;
            float rowGap = 9.0F * scale;
            float contentStartY = y + headerH + 18.0F * scale;
            float viewportBottom = y + h - 82.0F * scale;
            float viewportH = viewportBottom - contentStartY;

            List<ToggleItem> items = List.of(
                new ToggleItem("ESP", Icon.ESP, () -> KxVbNq.espEnabled, value -> KxVbNq.espEnabled = value),
                new ToggleItem("Players", Icon.PLAYERS, () -> KxVbNq.renderPlayers, value -> KxVbNq.renderPlayers = value),
                new ToggleItem("Mobs", Icon.MOBS, () -> KxVbNq.renderMobs, value -> KxVbNq.renderMobs = value),
                new ToggleItem("Boxes", Icon.BOXES, () -> KxVbNq.boxes, value -> KxVbNq.boxes = value),
                new ToggleItem("Filled Boxes", Icon.FILLED_BOXES, () -> KxVbNq.filledBoxes, value -> KxVbNq.filledBoxes = value),
                new ToggleItem("Tracers", Icon.TRACERS, () -> KxVbNq.tracers, value -> KxVbNq.tracers = value),
                new ToggleItem("Nametags", Icon.NAMETAGS, () -> KxVbNq.nametags, value -> KxVbNq.nametags = value),
                new ToggleItem("Storage ESP", Icon.STORAGE, () -> KxVbNq.storageEsp, value -> KxVbNq.storageEsp = value),
                new ToggleItem("Storage Type Colors", Icon.STORAGE, () -> KxVbNq.storageUseTypeColors, value -> KxVbNq.storageUseTypeColors = value),
                new ToggleItem("Ancient Debris", Icon.ANCIENT_DEBRIS, () -> KxVbNq.ancientDebrisEsp, value -> KxVbNq.ancientDebrisEsp = value),
                new ToggleItem("Item ESP", Icon.ITEM, () -> KxVbNq.itemEsp, value -> KxVbNq.itemEsp = value),
                new ToggleItem("Wall Models", Icon.WALL_MODELS, () -> KxVbNq.wallModels, value -> KxVbNq.wallModels = value),
                new ToggleItem("Ghost ESP", Icon.GHOST, () -> KxVbNq.ghostEsp, value -> KxVbNq.ghostEsp = value),
                new ToggleItem("Freecam", Icon.FREECAM, TmYpRc::isActive, value -> TmYpRc.toggle())
            );

            float contentHeight = cardH + rowGap + items.size() * (cardH + rowGap) + (cardH + rowGap) * 3.0F + 10.0F * 50.0F * scale + 148.0F * scale;
            float maxScroll = Math.max(0.0F, contentHeight - viewportH);
            float mouseX = ImGui.getIO().getMousePosX();
            float mouseY = ImGui.getIO().getMousePosY();
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                scrollOffset -= ImGui.getIO().getMouseWheel() * 48.0F * scale;
            }
            scrollOffset = Math.max(0.0F, Math.min(scrollOffset, maxScroll));

            draw.pushClipRect(x + 2.0F, contentStartY, x + w - 2.0F, viewportBottom, true);
            float contentY = contentStartY - scrollOffset;

            drawSectionCard(draw, contentX, contentY, contentW, cardH, Icon.MAIN, "Main", GuiPage.MAIN);
            contentY += cardH + rowGap;
            drawSectionCard(draw, contentX, contentY, contentW, cardH, Icon.FRIENDS, "Friends GUI", GuiPage.FRIENDS);
            contentY += cardH + rowGap;
            for (int i = 0; i < items.size(); i++) {
                float rowY = contentY + i * (cardH + rowGap);
                drawToggleCard(draw, items.get(i), contentX, rowY, contentW, cardH, scale);
            }

            contentY += items.size() * (cardH + rowGap) + 2.0F * scale;
            drawKeyCard(draw, contentX, contentY, contentW, cardH, Icon.GUI_KEY, "GUI Key", keyName(KxVbNq.getBoundGuiKey()), KeyCaptureMode.GUI, scale);
            contentY += cardH + rowGap;
            drawKeyCard(draw, contentX, contentY, contentW, cardH, Icon.ESP_KEY, "ESP Toggle Key", keyName(KxVbNq.getBoundToggleEspKey()), KeyCaptureMode.ESP_TOGGLE, scale);
            contentY += cardH + rowGap;
            drawKeyCard(draw, contentX, contentY, contentW, cardH, Icon.FREECAM, "Freecam Key", keyName(KxVbNq.getBoundFreecamKey()), KeyCaptureMode.FREECAM, scale);
            contentY += cardH + 14.0F * scale;

            drawSlider(draw, "Red", KxVbNq.red, value -> KxVbNq.red = value, contentX, contentY, contentW, cardH * 0.78F, 0.0F, 1.0F, scale);
            contentY += 50.0F * scale;
            drawSlider(draw, "Green", KxVbNq.green, value -> KxVbNq.green = value, contentX, contentY, contentW, cardH * 0.78F, 0.0F, 1.0F, scale);
            contentY += 50.0F * scale;
            drawSlider(draw, "Blue", KxVbNq.blue, value -> KxVbNq.blue = value, contentX, contentY, contentW, cardH * 0.78F, 0.0F, 1.0F, scale);
            contentY += 50.0F * scale;
            drawSlider(draw, "Alpha", KxVbNq.alpha, value -> KxVbNq.alpha = value, contentX, contentY, contentW, cardH * 0.78F, 0.0F, 1.0F, scale);
            contentY += 50.0F * scale;
            drawSlider(draw, "Line Thickness", (float) KxVbNq.lineThickness, value -> KxVbNq.lineThickness = value, contentX, contentY, contentW, cardH * 0.78F, 1.0F, 5.0F, scale);
            contentY += 62.0F * scale;
            drawSlider(draw, "Storage Red", KxVbNq.storageRed, value -> KxVbNq.storageRed = value, contentX, contentY, contentW, cardH * 0.78F, 0.0F, 1.0F, scale);
            contentY += 50.0F * scale;
            drawSlider(draw, "Storage Green", KxVbNq.storageGreen, value -> KxVbNq.storageGreen = value, contentX, contentY, contentW, cardH * 0.78F, 0.0F, 1.0F, scale);
            contentY += 50.0F * scale;
            drawSlider(draw, "Storage Blue", KxVbNq.storageBlue, value -> KxVbNq.storageBlue = value, contentX, contentY, contentW, cardH * 0.78F, 0.0F, 1.0F, scale);
            contentY += 50.0F * scale;
            drawSlider(draw, "Storage Alpha", KxVbNq.storageAlpha, value -> KxVbNq.storageAlpha = value, contentX, contentY, contentW, cardH * 0.78F, 0.0F, 1.0F, scale);
            contentY += 50.0F * scale;
            drawSlider(draw, "Storage Thickness", (float) KxVbNq.storageLineThickness, value -> KxVbNq.storageLineThickness = value, contentX, contentY, contentW, cardH * 0.78F, 1.0F, 8.0F, scale);

            float footerY = contentY + 82.0F * scale;
            String footer = "GUI: " + keyName(KxVbNq.getBoundGuiKey()) + " | ESP: " + keyName(KxVbNq.getBoundToggleEspKey()) + " | Freecam: " + keyName(KxVbNq.getBoundFreecamKey());
            drawText(draw, x + w * 0.5F - textWidth(footer) * 0.5F, footerY, rgba(132, 128, 180, 255), footer);
            draw.popClipRect();

            float closeW = 260.0F * scale;
            float closeH = 38.0F * scale;
            float closeX = x + w * 0.5F - closeW * 0.5F;
            float closeY = y + h - 58.0F * scale;
            ImGui.setCursorScreenPos(closeX, closeY);
            boolean closeHovered = ImGui.invisibleButton("##ggesp_close", closeW, closeH);
            closeHovered = ImGui.isItemHovered();
            if (ImGui.isItemClicked()) {
                dismissGui();
            }
            draw.addRectFilled(closeX, closeY, closeX + closeW, closeY + closeH, closeHovered ? rgba(42, 29, 82, 226) : rgba(28, 20, 55, 218), 17.0F);
            draw.addRect(closeX, closeY, closeX + closeW, closeY + closeH, rgba(135, 99, 255, closeHovered ? 190 : 135), 17.0F, 0, 1.3F);
            drawText(draw, closeX + closeW * 0.5F - textWidth("Close") * 0.5F, closeY + closeH * 0.5F - 9.0F, rgba(166, 135, 255, 255), "Close");
        }

        private void drawFriendsPanel(ImDrawList draw, float x, float y, float w, float h, float scale) {
            draw.addRectFilled(x, y, x + w, y + h, rgba(12, 13, 27, 238), 20.0F);
            draw.addRect(x, y, x + w, y + h, rgba(88, 82, 132, 86), 20.0F, 0, 1.0F);
            drawLogo(draw, x + w * 0.5F - 86.0F * scale, y + 17.0F * scale, 172.0F * scale);
            drawText(draw, x + w * 0.5F - textWidth("Friends") * 0.5F, y + 68.0F * scale, rgba(124, 86, 255, 255), "Friends");

            float padding = 26.0F * scale;
            float contentX = x + padding;
            float contentW = w - padding * 2.0F;
            float cardH = 43.0F * scale;
            float gap = 12.0F * scale;
            float rowGap = 9.0F * scale;
            float contentY = y + 112.0F * scale;
            float halfW = (contentW - gap) * 0.5F;

            drawSectionCard(draw, contentX, contentY, halfW, cardH, Icon.MAIN, "Main", GuiPage.MAIN);
            drawSectionCard(draw, contentX + halfW + gap, contentY, halfW, cardH, Icon.FRIENDS, "Friends", GuiPage.FRIENDS);
            contentY += cardH + 16.0F * scale;

            drawInputCard(draw, contentX, contentY, halfW, cardH, "Search friends...", friendSearch, FriendsIcon.SEARCH, TextFieldMode.FRIEND_SEARCH);
            drawDropdownCard(draw, contentX + halfW + gap, contentY, halfW, cardH, "Sort: Name");
            contentY += cardH + 16.0F * scale;
            drawFriendAction(draw, contentX, contentY, halfW, cardH, "Add Friend", FriendsIcon.ADD_FRIEND, true);
            drawFriendAction(draw, contentX + halfW + gap, contentY, halfW, cardH, "Remove Friend", FriendsIcon.REMOVE_FRIEND, false);
            contentY += cardH + 16.0F * scale;
            drawInputCard(draw, contentX, contentY, contentW, cardH, "Friend nickname", friendName, FriendsIcon.FRIEND_AVATAR, TextFieldMode.FRIEND_NAME);
            contentY += cardH + 16.0F * scale;

            float tableX = contentX;
            float tableY = contentY;
            float tableW = contentW;
            float tableH = y + h - tableY - 96.0F * scale;
            draw.addRectFilled(tableX, tableY, tableX + tableW, tableY + tableH, rgba(17, 18, 35, 214), 15.0F);
            draw.addRect(tableX, tableY, tableX + tableW, tableY + tableH, rgba(72, 68, 112, 75), 15.0F, 0, 1.0F);
            drawText(draw, tableX + 22.0F * scale, tableY + 20.0F * scale, rgba(164, 151, 222, 255), "Friend");
            drawText(draw, tableX + tableW * 0.38F, tableY + 20.0F * scale, rgba(164, 151, 222, 255), "Red");
            drawText(draw, tableX + tableW * 0.56F, tableY + 20.0F * scale, rgba(164, 151, 222, 255), "Green");
            drawText(draw, tableX + tableW * 0.74F, tableY + 20.0F * scale, rgba(164, 151, 222, 255), "Blue");
            drawText(draw, tableX + tableW - 88.0F * scale, tableY + 20.0F * scale, rgba(164, 151, 222, 255), "Tracers");
            draw.addLine(tableX, tableY + 54.0F * scale, tableX + tableW, tableY + 54.0F * scale, rgba(56, 54, 84, 170), 1.0F);

            draw.pushClipRect(tableX, tableY + 56.0F * scale, tableX + tableW, tableY + tableH, true);
            List<String> friends = visibleFriends();
            float rowY = tableY + 58.0F * scale;
            float rowH = 58.0F * scale;
            if (friends.isEmpty()) {
                String empty = "No friends yet";
                drawText(draw, tableX + tableW * 0.5F - textWidth(empty) * 0.5F, rowY + 26.0F * scale, rgba(105, 101, 145, 255), empty);
            }
            for (String friend : friends) {
                drawFriendRow(draw, friend, tableX, rowY, tableW, rowH, scale);
                rowY += rowH;
            }
            draw.popClipRect();

            drawCloseButton(draw, x, y, w, h, scale);
        }

        private void drawLogo(ImDrawList draw, float x, float y, float w) {
            if (iconsTextureId != 0) {
                Icon.LOGO.draw(draw, x, y, w, w * 0.30F, rgba(255, 255, 255, 255));
            } else {
                drawText(draw, x + 72.0F, y, rgba(255, 255, 255, 255), "GGESP");
            }
        }

        private void drawSectionCard(ImDrawList draw, float x, float y, float w, float h, Icon icon, String label, GuiPage page) {
            ImGui.setCursorScreenPos(x, y);
            if (ImGui.invisibleButton("##section_" + page.name(), w, h)) {
                currentPage = page;
                scrollOffset = 0.0F;
                openedAtNanos = System.nanoTime();
            }
            drawCardBase(draw, x, y, w, h, ImGui.isItemHovered());
            icon.draw(draw, x + 28.0F, y + h * 0.5F - 15.0F, 30.0F, 30.0F, rgba(169, 141, 255, 255));
            drawText(draw, x + 76.0F, y + h * 0.5F - 9.0F, rgba(183, 164, 255, 255), label);
        }

        private void drawInputCard(ImDrawList draw, float x, float y, float w, float h, String hint, ImString value, FriendsIcon icon, TextFieldMode mode) {
            ImGui.setCursorScreenPos(x, y);
            if (ImGui.invisibleButton("##input_card_" + mode.name(), w, h)) {
                activeTextField = mode;
            }
            boolean active = activeTextField == mode;
            drawCardBase(draw, x, y, w, h, active || ImGui.isItemHovered());
            icon.draw(draw, x + 18.0F, y + h * 0.5F - 12.0F, 24.0F, 24.0F, rgba(164, 139, 255, 255));
            String rendered = value.get().isBlank() ? hint : value.get();
            int textColor = value.get().isBlank() ? rgba(141, 134, 190, 255) : rgba(230, 226, 255, 255);
            drawText(draw, x + 54.0F, y + h * 0.5F - 9.0F, textColor, rendered);
            if (active && ((System.nanoTime() / 450_000_000L) & 1L) == 0L) {
                float caretX = x + 54.0F + textWidth(value.get());
                draw.addLine(caretX, y + h * 0.5F - 10.0F, caretX, y + h * 0.5F + 10.0F, rgba(174, 150, 255, 255), 1.0F);
            }
        }

        private void drawDropdownCard(ImDrawList draw, float x, float y, float w, float h, String label) {
            drawCardBase(draw, x, y, w, h, false);
            drawText(draw, x + 24.0F, y + h * 0.5F - 9.0F, rgba(226, 224, 245, 255), label);
            FriendsIcon.DROPDOWN.draw(draw, x + w - 38.0F, y + h * 0.5F - 8.0F, 16.0F, 16.0F, rgba(154, 141, 218, 255));
        }

        private void drawFriendAction(ImDrawList draw, float x, float y, float w, float h, String label, FriendsIcon icon, boolean add) {
            ImGui.setCursorScreenPos(x, y);
            if (ImGui.invisibleButton("##friend_action_" + label, w, h)) {
                if (add) {
                    addFriendFromInput();
                } else {
                    removeSelectedFriend();
                }
            }
            boolean hovered = ImGui.isItemHovered();
            draw.addRectFilled(x, y, x + w, y + h, hovered ? rgba(33, 24, 68, 230) : rgba(24, 21, 48, 218), 15.0F);
            draw.addRect(x, y, x + w, y + h, rgba(122, 82, 255, add || hovered ? 180 : 82), 15.0F, 0, 1.0F);
            icon.draw(draw, x + w * 0.5F - 70.0F, y + h * 0.5F - 14.0F, 28.0F, 28.0F, rgba(140, 94, 255, 255));
            drawText(draw, x + w * 0.5F - 28.0F, y + h * 0.5F - 9.0F, add ? rgba(145, 100, 255, 255) : rgba(144, 132, 190, 255), label);
        }

        private void drawFriendRow(ImDrawList draw, String friend, float x, float y, float w, float h, float scale) {
            ImGui.setCursorScreenPos(x, y);
            if (ImGui.invisibleButton("##friend_row_" + friend, w * 0.31F, h)) {
                selectedFriend = friend;
                friendName.set(friend);
                activeTextField = TextFieldMode.NONE;
            }
            boolean selected = friend.equals(selectedFriend);
            boolean hovered = ImGui.isItemHovered();
            if (selected || hovered) {
                draw.addRectFilled(x, y, x + w, y + h, selected ? rgba(35, 27, 74, 190) : rgba(25, 24, 48, 145), 7.0F);
            }
            draw.addLine(x, y + h, x + w, y + h, rgba(48, 47, 74, 150), 1.0F);
            FriendsIcon.FRIEND_AVATAR.draw(draw, x + 18.0F * scale, y + 12.0F * scale, 34.0F * scale, 34.0F * scale, rgba(150, 118, 255, 255));
            drawText(draw, x + 66.0F * scale, y + h * 0.5F - 9.0F, rgba(245, 244, 255, 255), friend);
            drawFriendSlider("friend_red_" + friend, x + w * 0.34F, y + 14.0F * scale, w * 0.14F, KxVbNq.getFriendTracerRed(friend), value -> KxVbNq.setFriendTracerRed(friend, value));
            drawFriendSlider("friend_green_" + friend, x + w * 0.53F, y + 14.0F * scale, w * 0.14F, KxVbNq.getFriendTracerGreen(friend), value -> KxVbNq.setFriendTracerGreen(friend, value));
            drawFriendSlider("friend_blue_" + friend, x + w * 0.72F, y + 14.0F * scale, w * 0.14F, KxVbNq.getFriendTracerBlue(friend), value -> KxVbNq.setFriendTracerBlue(friend, value));
            drawSwitch(draw, x + w - 62.0F * scale, y + h * 0.5F - 14.0F * scale, "Friend Tracers " + friend, KxVbNq.areFriendTracersEnabled(friend), scale);
            ImGui.setCursorScreenPos(x + w - 72.0F * scale, y + 8.0F * scale);
            if (ImGui.invisibleButton("##friend_tracers_" + friend, 64.0F * scale, 42.0F * scale)) {
                KxVbNq.setFriendTracers(friend, !KxVbNq.areFriendTracersEnabled(friend));
            }
        }

        private void drawFriendSlider(String id, float x, float y, float w, float value, Consumer<Float> setter) {
            ImGui.setCursorScreenPos(x, y);
            ImGui.pushStyleColor(ImGuiCol.FrameBg, rgba(41, 40, 68, 255));
            ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, rgba(52, 47, 88, 255));
            ImGui.pushStyleColor(ImGuiCol.SliderGrab, rgba(112, 70, 255, 255));
            ImGui.pushStyleColor(ImGuiCol.SliderGrabActive, rgba(147, 104, 255, 255));
            float[] holder = new float[] { value };
            ImGui.setNextItemWidth(w);
            if (ImGui.sliderFloat("##" + id, holder, 0.0F, 1.0F, "")) {
                setter.accept(holder[0]);
                KxVbNq.saveConfig();
            }
            ImGui.popStyleColor(4);
        }

        private List<String> visibleFriends() {
            String filter = friendSearch.get().trim().toLowerCase(Locale.ROOT);
            List<String> friends = new ArrayList<>(KxVbNq.getFriends());
            friends.sort(Comparator.naturalOrder());
            if (filter.isEmpty()) {
                return friends;
            }

            return friends.stream()
                .filter(friend -> friend.toLowerCase(Locale.ROOT).contains(filter))
                .toList();
        }

        private void addFriendFromInput() {
            String name = friendName.get().trim();
            if (name.isEmpty()) {
                name = friendSearch.get().trim();
            }
            KxVbNq.addFriend(name);
            selectedFriend = name.toLowerCase(Locale.ROOT);
            friendName.set("");
            friendSearch.set("");
        }

        private void removeSelectedFriend() {
            String name = friendName.get().trim();
            if (name.isEmpty()) {
                name = selectedFriend;
            }
            if (name.isEmpty()) {
                name = friendSearch.get().trim();
            }
            if (!name.isEmpty()) {
                KxVbNq.removeFriend(name);
                if (name.equalsIgnoreCase(selectedFriend)) {
                    selectedFriend = "";
                }
                friendName.set("");
            }
        }

        private void drawCloseButton(ImDrawList draw, float x, float y, float w, float h, float scale) {
            float closeW = 260.0F * scale;
            float closeH = 38.0F * scale;
            float closeX = x + w * 0.5F - closeW * 0.5F;
            float closeY = y + h - 58.0F * scale;
            ImGui.setCursorScreenPos(closeX, closeY);
            ImGui.invisibleButton("##ggesp_close_friends", closeW, closeH);
            boolean closeHovered = ImGui.isItemHovered();
            if (ImGui.isItemClicked()) {
                currentPage = GuiPage.MAIN;
                scrollOffset = 0.0F;
                openedAtNanos = System.nanoTime();
            }
            draw.addRectFilled(closeX, closeY, closeX + closeW, closeY + closeH, closeHovered ? rgba(42, 29, 82, 226) : rgba(28, 20, 55, 218), 17.0F);
            draw.addRect(closeX, closeY, closeX + closeW, closeY + closeH, rgba(135, 99, 255, closeHovered ? 190 : 135), 17.0F, 0, 1.3F);
            drawText(draw, closeX + closeW * 0.5F - textWidth("Main") * 0.5F, closeY + closeH * 0.5F - 9.0F, rgba(166, 135, 255, 255), "Main");
        }

        private void drawToggleCard(ImDrawList draw, ToggleItem item, float x, float y, float w, float h, float scale) {
            boolean enabled = item.getter().get();
            ImGui.setCursorScreenPos(x, y);
            if (ImGui.invisibleButton("##toggle_" + item.label(), w, h)) {
                item.setter().accept(!enabled);
                KxVbNq.saveConfig();
            }
            enabled = item.getter().get();
            boolean hovered = ImGui.isItemHovered();
            drawCardBase(draw, x, y, w, h, hovered);
            item.icon().draw(draw, x + 27.0F * scale, y + h * 0.5F - 14.0F * scale, 28.0F * scale, 28.0F * scale, rgba(160, 132, 255, 255));
            String state = item.label() + ": " + (enabled ? "ON" : "OFF");
            drawText(draw, x + 76.0F * scale, y + h * 0.5F - 9.0F, enabled ? rgba(245, 244, 255, 255) : rgba(214, 211, 235, 235), state);
            drawSwitch(draw, x + w - 78.0F * scale, y + h * 0.5F - 14.0F * scale, item.label(), enabled, scale);
        }

        private void drawKeyCard(ImDrawList draw, float x, float y, float w, float h, Icon icon, String label, String value, KeyCaptureMode mode, float scale) {
            ImGui.setCursorScreenPos(x, y);
            if (ImGui.invisibleButton("##key_" + mode.name(), w, h)) {
                captureMode = mode;
            }
            boolean hovered = ImGui.isItemHovered();
            drawCardBase(draw, x, y, w, h, hovered);
            icon.draw(draw, x + 28.0F * scale, y + h * 0.5F - 13.0F * scale, 28.0F * scale, 28.0F * scale, rgba(158, 130, 255, 255));
            String text = captureMode == mode ? label + ": press key..." : label + ": " + value;
            drawText(draw, x + 76.0F * scale, y + h * 0.5F - 9.0F, rgba(166, 145, 255, 255), text);
        }

        private void drawSlider(ImDrawList draw, String label, float value, Consumer<Float> setter, float x, float y, float w, float h, float min, float max, float scale) {
            drawCardBase(draw, x, y, w, h, false);
            String text = label + ": " + String.format(Locale.ROOT, "%.2f", value);
            if ("Line Thickness".equals(label)) {
                text = label + ": " + String.format(Locale.ROOT, "%.1f", value);
            }
            drawText(draw, x + 28.0F * scale, y + h * 0.5F - 9.0F, rgba(164, 143, 255, 255), text);

            float sliderX = x + 230.0F * scale;
            float sliderW = w - 270.0F * scale;
            ImGui.setCursorScreenPos(sliderX, y + h * 0.5F - 10.0F);
            ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 8.0F);
            ImGui.pushStyleVar(ImGuiStyleVar.GrabRounding, 10.0F);
            ImGui.pushStyleColor(ImGuiCol.FrameBg, rgba(12, 12, 24, 255));
            ImGui.pushStyleColor(ImGuiCol.FrameBgHovered, rgba(18, 16, 35, 255));
            ImGui.pushStyleColor(ImGuiCol.SliderGrab, rgba(112, 70, 255, 255));
            ImGui.pushStyleColor(ImGuiCol.SliderGrabActive, rgba(147, 104, 255, 255));
            float[] holder = new float[] { value };
            ImGui.setNextItemWidth(sliderW);
            if (ImGui.sliderFloat("##slider_" + label, holder, min, max, "")) {
                setter.accept(holder[0]);
                KxVbNq.saveConfig();
            }
            ImGui.popStyleColor(4);
            ImGui.popStyleVar(2);
        }

        private void drawCardBase(ImDrawList draw, float x, float y, float w, float h, boolean hovered) {
            draw.addRectFilled(x, y, x + w, y + h, hovered ? rgba(25, 25, 48, 232) : rgba(21, 21, 42, 220), 16.0F);
            draw.addRect(x, y, x + w, y + h, rgba(83, 78, 128, hovered ? 102 : 66), 16.0F, 0, 1.0F);
        }

        private void drawSwitch(ImDrawList draw, float x, float y, String id, boolean enabled, float scale) {
            float w = 50.0F * scale;
            float h = 28.0F * scale;
            float target = enabled ? 1.0F : 0.0F;
            float progress = toggleAnimations.getOrDefault(id, target);
            progress += (target - progress) * 0.24F;
            if (Math.abs(target - progress) < 0.002F) {
                progress = target;
            }
            toggleAnimations.put(id, progress);

            int trackRed = lerpInt(14, 106, progress);
            int trackGreen = lerpInt(15, 64, progress);
            int trackBlue = lerpInt(30, 242, progress);
            int borderRed = lerpInt(58, 139, progress);
            int borderGreen = lerpInt(54, 99, progress);
            int borderBlue = lerpInt(90, 255, progress);
            int knob = lerpInt(78, 238, progress);

            draw.addRectFilled(x, y, x + w, y + h, rgba(trackRed, trackGreen, trackBlue, 255), h * 0.5F);
            draw.addRect(x, y, x + w, y + h, rgba(borderRed, borderGreen, borderBlue, 150), h * 0.5F, 0, 1.0F);
            float knobX = x + h * 0.5F + (w - h) * progress;
            draw.addCircleFilled(knobX, y + h * 0.5F, 10.0F * scale, rgba(knob, knob, Math.min(255, knob + 17), 255), 20);
        }
    }

    private static void ensureInterFontLoaded() {
        if (interFontAttempted) {
            return;
        }

        interFontAttempted = true;
        if (!USE_INTER_FONT) {
            interFont = null;
            return;
        }

        try (InputStream input = RqZpLm.class.getResourceAsStream(INTER_FONT_RESOURCE)) {
            if (input == null) {
                return;
            }

            interFontPath = Files.createTempFile("ggesp-inter-", ".ttf");
            Files.copy(input, interFontPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            interFontPath.toFile().deleteOnExit();

            ImFontAtlas fonts = ImGui.getIO().getFonts();
            interFont = fonts.addFontFromFileTTF(interFontPath.toAbsolutePath().toString(), 18.0F);
            if (interFont != null) {
                fonts.build();
                ImGui.getIO().setFontDefault(interFont);
                updateImguiFontTexture();
            }
        } catch (IOException | ReflectiveOperationException | RuntimeException ignored) {
            interFont = null;
        }
    }

    private static void ensureInterTextLoaded() {
        if (interTextAttempted) {
            return;
        }

        interTextAttempted = true;
        try (InputStream input = RqZpLm.class.getResourceAsStream(INTER_FONT_RESOURCE)) {
            if (input != null) {
                interTextRenderer = InterTextRenderer.create(input);
            }
        } catch (IOException | RuntimeException ignored) {
            interTextRenderer = null;
        }
    }

    private static void drawText(ImDrawList draw, float x, float y, int color, String text) {
        if (interTextRenderer != null) {
            interTextRenderer.draw(draw, x, y, color, text);
        } else {
            draw.addText(x, y, color, text);
        }
    }

    private static float textWidth(String text) {
        return interTextRenderer != null ? interTextRenderer.width(text) : ImGui.calcTextSize(text).x;
    }

    private static void ensureIconsLoaded() {
        if (iconsAttempted) {
            return;
        }

        iconsAttempted = true;
        try (InputStream input = RqZpLm.class.getResourceAsStream(ICONS_RESOURCE)) {
            if (input == null) {
                return;
            }

            iconsTexture = new NativeImageBackedTexture(NativeImage.read(input));
            iconsTexture.setFilter(false, false);
            iconsTexture.upload();
            iconsTextureId = iconsTexture.getGlId();
        } catch (IOException | RuntimeException ignored) {
            iconsTextureId = 0;
        }
    }

    private static void ensureFriendsIconsLoaded() {
        if (friendsIconsAttempted) {
            return;
        }

        friendsIconsAttempted = true;
        try (InputStream input = RqZpLm.class.getResourceAsStream(FRIENDS_ICONS_RESOURCE)) {
            if (input == null) {
                return;
            }

            friendsIconsTexture = new NativeImageBackedTexture(NativeImage.read(input));
            friendsIconsTexture.setFilter(false, false);
            friendsIconsTexture.upload();
            friendsIconsTextureId = friendsIconsTexture.getGlId();
        } catch (IOException | RuntimeException ignored) {
            friendsIconsTextureId = 0;
        }
    }

    private static void updateImguiFontTexture() throws ReflectiveOperationException {
        Class<?> loaderClass = Class.forName("xyz.breadloaf.imguimc.imgui.ImguiLoader");
        Field rendererField = loaderClass.getDeclaredField("imGuiGl3");
        rendererField.setAccessible(true);
        Object renderer = rendererField.get(null);
        Method updateFontsTexture = renderer.getClass().getMethod("updateFontsTexture");
        updateFontsTexture.invoke(renderer);
    }

    private String keyName(InputUtil.Key key) {
        if (key == null || key.equals(InputUtil.UNKNOWN_KEY)) {
            return "UNBOUND";
        }

        String translationKey = key.getTranslationKey();
        if (translationKey.startsWith("key.keyboard.")) {
            return formatKeyToken(translationKey.substring("key.keyboard.".length()));
        }
        if (translationKey.startsWith("key.mouse.")) {
            return "Mouse " + formatKeyToken(translationKey.substring("key.mouse.".length()));
        }

        String localized = key.getLocalizedText().getString();
        return localized == null || localized.isBlank() ? translationKey : localized;
    }

    private static String formatKeyToken(String token) {
        if (token.length() == 1) {
            return token.toUpperCase(Locale.ROOT);
        }

        String[] parts = token.split("\\.");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? token : builder.toString();
    }

    private static int rgba(int red, int green, int blue, int alpha) {
        return (alpha << 24) | (blue << 16) | (green << 8) | red;
    }

    private static float easeOutCubic(float value) {
        float inverted = 1.0F - value;
        return 1.0F - inverted * inverted * inverted;
    }

    private static int lerpInt(int from, int to, float delta) {
        return Math.round(from + (to - from) * delta);
    }

    private void deleteLastTextFieldCharacter() {
        ImString value = activeTextField == TextFieldMode.FRIEND_NAME ? friendName : friendSearch;
        String text = value.get();
        if (!text.isEmpty()) {
            value.set(text.substring(0, text.length() - 1));
        }
    }

    private void addFriendFromInput() {
        String name = friendName.get().trim();
        if (name.isEmpty()) {
            name = friendSearch.get().trim();
        }
        KxVbNq.addFriend(name);
        selectedFriend = name.toLowerCase(Locale.ROOT);
        friendName.set("");
        friendSearch.set("");
    }

    private record ToggleItem(String label, Icon icon, Supplier<Boolean> getter, Consumer<Boolean> setter) {
    }

    private enum KeyCaptureMode {
        NONE,
        GUI,
        ESP_TOGGLE,
        FREECAM
    }

    private enum GuiPage {
        MAIN,
        FRIENDS
    }

    private enum TextFieldMode {
        NONE,
        FRIEND_SEARCH,
        FRIEND_NAME
    }

    private enum Icon {
        LOGO(80, 58, 430, 150),
        MAIN(84, 260, 125, 120),
        FRIENDS(295, 260, 125, 120),
        ESP(500, 260, 125, 120),
        PLAYERS(705, 260, 125, 120),
        MOBS(905, 260, 125, 120),
        BOXES(1110, 260, 125, 120),
        FILLED_BOXES(1310, 260, 125, 120),
        TRACERS(82, 486, 125, 120),
        NAMETAGS(295, 486, 125, 120),
        STORAGE(505, 486, 125, 120),
        ITEM(705, 486, 125, 120),
        GHOST(905, 486, 125, 120),
        WALL_MODELS(1105, 486, 125, 120),
        ANCIENT_DEBRIS(1305, 486, 125, 120),
        FREECAM(80, 720, 125, 120),
        GUI_KEY(292, 720, 125, 120),
        ESP_KEY(505, 720, 125, 120);

        private final float x;
        private final float y;
        private final float w;
        private final float h;

        Icon(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private void draw(ImDrawList draw, float drawX, float drawY, float drawW, float drawH, int tint) {
            if (iconsTextureId == 0) {
                return;
            }

            draw.addImage(
                iconsTextureId,
                drawX,
                drawY,
                drawX + drawW,
                drawY + drawH,
                x / ICONS_WIDTH,
                y / ICONS_HEIGHT,
                (x + w) / ICONS_WIDTH,
                (y + h) / ICONS_HEIGHT,
                tint
            );
        }
    }

    private enum FriendsIcon {
        ADD_FRIEND(312, 818, 125, 120),
        REMOVE_FRIEND(505, 818, 125, 120),
        SEARCH(705, 818, 125, 120),
        DROPDOWN(900, 818, 125, 120),
        FRIEND_AVATAR(1088, 798, 125, 150);

        private final float x;
        private final float y;
        private final float w;
        private final float h;

        FriendsIcon(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private void draw(ImDrawList draw, float drawX, float drawY, float drawW, float drawH, int tint) {
            if (friendsIconsTextureId == 0) {
                return;
            }

            draw.addImage(
                friendsIconsTextureId,
                drawX,
                drawY,
                drawX + drawW,
                drawY + drawH,
                x / ICONS_WIDTH,
                y / ICONS_HEIGHT,
                (x + w) / ICONS_WIDTH,
                (y + h) / ICONS_HEIGHT,
                tint
            );
        }
    }

    private static final class InterTextRenderer {
        private static final int FIRST_CHAR = 32;
        private static final int LAST_CHAR = 126;
        private static final int FONT_SIZE = 18;
        private static final int ATLAS_SIZE = 1024;
        private final NativeImageBackedTexture texture;
        private final int textureId;
        private final Glyph[] glyphs;
        private final int lineHeight;

        private InterTextRenderer(NativeImageBackedTexture texture, Glyph[] glyphs, int lineHeight) {
            this.texture = texture;
            this.textureId = texture.getGlId();
            this.glyphs = glyphs;
            this.lineHeight = lineHeight;
        }

        private static InterTextRenderer create(InputStream input) throws IOException {
            Font font;
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, input).deriveFont(Font.PLAIN, FONT_SIZE);
            } catch (java.awt.FontFormatException e) {
                throw new IOException("Invalid Inter font.", e);
            }

            BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = atlas.createGraphics();
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE);
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);

            FontMetrics metrics = graphics.getFontMetrics();
            int lineHeight = metrics.getHeight();
            Glyph[] glyphs = new Glyph[LAST_CHAR - FIRST_CHAR + 1];
            int cursorX = 2;
            int cursorY = 2;

            for (int code = FIRST_CHAR; code <= LAST_CHAR; code++) {
                char character = (char) code;
                int width = Math.max(1, metrics.charWidth(character));
                int glyphWidth = width + 4;
                int glyphHeight = lineHeight + 4;
                if (cursorX + glyphWidth >= ATLAS_SIZE) {
                    cursorX = 2;
                    cursorY += glyphHeight + 2;
                }

                graphics.drawString(String.valueOf(character), cursorX + 2, cursorY + 2 + metrics.getAscent());
                glyphs[code - FIRST_CHAR] = new Glyph(
                    cursorX,
                    cursorY,
                    glyphWidth,
                    glyphHeight,
                    width
                );
                cursorX += glyphWidth + 2;
            }
            graphics.dispose();

            NativeImage image = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, false);
            for (int y = 0; y < ATLAS_SIZE; y++) {
                for (int x = 0; x < ATLAS_SIZE; x++) {
                    image.setColorArgb(x, y, atlas.getRGB(x, y));
                }
            }

            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            texture.setFilter(true, false);
            texture.upload();
            return new InterTextRenderer(texture, glyphs, lineHeight);
        }

        private void draw(ImDrawList draw, float x, float y, int color, String text) {
            float cursor = x;
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                if (character < FIRST_CHAR || character > LAST_CHAR) {
                    cursor += FONT_SIZE * 0.55F;
                    continue;
                }

                Glyph glyph = glyphs[character - FIRST_CHAR];
                if (glyph == null) {
                    continue;
                }

                if (character != ' ') {
                    draw.addImage(
                        textureId,
                        cursor,
                        y,
                        cursor + glyph.width,
                        y + glyph.height,
                        glyph.x / (float) ATLAS_SIZE,
                        glyph.y / (float) ATLAS_SIZE,
                        (glyph.x + glyph.width) / (float) ATLAS_SIZE,
                        (glyph.y + glyph.height) / (float) ATLAS_SIZE,
                        color
                    );
                }
                cursor += glyph.advance;
            }
        }

        private float width(String text) {
            float width = 0.0F;
            for (int i = 0; i < text.length(); i++) {
                char character = text.charAt(i);
                if (character < FIRST_CHAR || character > LAST_CHAR) {
                    width += FONT_SIZE * 0.55F;
                    continue;
                }

                Glyph glyph = glyphs[character - FIRST_CHAR];
                if (glyph != null) {
                    width += glyph.advance;
                }
            }
            return width;
        }

        private record Glyph(int x, int y, int width, int height, int advance) {
        }
    }

    private static final class PulseTheme implements Theme {
        @Override
        public void preRender() {
            ImGui.styleColorsDark();
            ImGui.getStyle().setWindowBorderSize(0.0F);
            ImGui.getStyle().setWindowRounding(0.0F);
            ImGui.getStyle().setFrameBorderSize(0.0F);
            ImGui.getStyle().setFrameRounding(10.0F);
            ImGui.getStyle().setGrabRounding(10.0F);
            ImGui.getStyle().setScrollbarSize(4.0F);
            ImGui.getStyle().setWindowPadding(0.0F, 0.0F);
        }

        @Override
        public void postRender() {
        }
    }
}
