package ru.arthur.ggesp.client;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class ClickGuiScreen extends Screen {
    private final Screen parent;
    private KeyCaptureMode captureMode = KeyCaptureMode.NONE;

    public ClickGuiScreen(Screen parent) {
        super(Text.literal("GGESP ClickGUI"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();

        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int width = 150;
        int y = 40;
        int step = 24;

        this.addDrawableChild(ButtonWidget.builder(toggleEspText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.ESP))
        ).dimensions(left, y, width, 20).build());

        this.addDrawableChild(ButtonWidget.builder(togglePlayersText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.PLAYERS))
        ).dimensions(right, y, width, 20).build());
        y += step;

        this.addDrawableChild(ButtonWidget.builder(toggleMobsText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.MOBS))
        ).dimensions(left, y, width, 20).build());

        this.addDrawableChild(ButtonWidget.builder(toggleBoxesText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.BOXES))
        ).dimensions(right, y, width, 20).build());
        y += step;

        this.addDrawableChild(ButtonWidget.builder(toggleFilledText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.FILLED))
        ).dimensions(left, y, width, 20).build());

        this.addDrawableChild(ButtonWidget.builder(toggleTracersText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.TRACERS))
        ).dimensions(right, y, width, 20).build());
        y += step;

        this.addDrawableChild(ButtonWidget.builder(toggleNametagsText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.NAMETAGS))
        ).dimensions(left, y, width, 20).build());

        this.addDrawableChild(ButtonWidget.builder(toggleStorageText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.STORAGE))
        ).dimensions(right, y, width, 20).build());
        y += step;

        this.addDrawableChild(ButtonWidget.builder(toggleAncientDebrisText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.ANCIENT_DEBRIS))
        ).dimensions(left, y, width, 20).build());

        this.addDrawableChild(ButtonWidget.builder(toggleWallModelsText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.WALL_MODELS))
        ).dimensions(right, y, width, 20).build());
        y += step;

        this.addDrawableChild(ButtonWidget.builder(toggleGhostText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.GHOST))
        ).dimensions(left, y, width, 20).build());

        this.addDrawableChild(ButtonWidget.builder(toggleFreecamText(), button ->
            button.setMessage(toggleAndGet(EspSettingsToggle.FREECAM))
        ).dimensions(right, y, width, 20).build());
        y += step;

        this.addDrawableChild(ButtonWidget.builder(guiKeyText(), button -> {
            captureMode = KeyCaptureMode.GUI;
            button.setMessage(Text.literal("Press a key for GUI..."));
        }).dimensions(left, y, 310, 20).build());
        y += step;

        this.addDrawableChild(ButtonWidget.builder(toggleKeyText(), button -> {
            captureMode = KeyCaptureMode.ESP_TOGGLE;
            button.setMessage(Text.literal("Press a key for ESP toggle..."));
        }).dimensions(left, y, 310, 20).build());
        y += step + 4;

        this.addDrawableChild(new ConfigSlider(
            left, y, 310, 20,
            "Red", EspSettings.red, 0.0D, 1.0D,
            value -> EspSettings.red = value.floatValue(),
            value -> String.format("%.2f", value)
        ));
        y += step;

        this.addDrawableChild(new ConfigSlider(
            left, y, 310, 20,
            "Green", EspSettings.green, 0.0D, 1.0D,
            value -> EspSettings.green = value.floatValue(),
            value -> String.format("%.2f", value)
        ));
        y += step;

        this.addDrawableChild(new ConfigSlider(
            left, y, 310, 20,
            "Blue", EspSettings.blue, 0.0D, 1.0D,
            value -> EspSettings.blue = value.floatValue(),
            value -> String.format("%.2f", value)
        ));
        y += step;

        this.addDrawableChild(new ConfigSlider(
            left, y, 310, 20,
            "Alpha", EspSettings.alpha, 0.05D, 1.0D,
            value -> EspSettings.alpha = value.floatValue(),
            value -> String.format("%.2f", value)
        ));
        y += step;

        this.addDrawableChild(new ConfigSlider(
            left, y, 310, 20,
            "Line Thickness", EspSettings.lineThickness, 1.0D, 5.0D,
            value -> EspSettings.lineThickness = value,
            value -> String.format("%.1f", value)
        ));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
            .dimensions(this.width / 2 - 60, this.height - 34, 120, 20)
            .build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (captureMode != KeyCaptureMode.NONE) {
            if (keyCode == 256) {
                captureMode = KeyCaptureMode.NONE;
                this.init();
                return true;
            }

            InputUtil.Key newKey = keyCode == 261 || keyCode == 259
                ? InputUtil.UNKNOWN_KEY
                : InputUtil.fromKeyCode(keyCode, scanCode);

            if (captureMode == KeyCaptureMode.GUI) {
                EspSettings.setGuiKey(newKey);
            } else {
                EspSettings.setToggleEspKey(newKey);
            }

            captureMode = KeyCaptureMode.NONE;
            this.init();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        TextRenderer textRenderer = this.textRenderer;
        context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("GUI key: " + keyName(EspSettings.getBoundGuiKey()) + " | ESP key: " + keyName(EspSettings.getBoundToggleEspKey())),
            this.width / 2,
            this.height - 50,
            0xB0B0B0
        );

        if (captureMode != KeyCaptureMode.NONE) {
            String waitingText = captureMode == KeyCaptureMode.GUI
                ? "Press a key for the ClickGUI"
                : "Press a key for ESP on/off";
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(waitingText), this.width / 2, this.height - 66, 0xFFD966);
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Esc - cancel | Backspace/Delete - unbind"),
                this.width / 2,
                this.height - 78,
                0xA0A0A0
            );
        }
    }

    private Text toggleAndGet(EspSettingsToggle toggle) {
        return switch (toggle) {
            case ESP -> {
                EspSettings.espEnabled = !EspSettings.espEnabled;
                yield toggleEspText();
            }
            case PLAYERS -> {
                EspSettings.renderPlayers = !EspSettings.renderPlayers;
                yield togglePlayersText();
            }
            case BOXES -> {
                EspSettings.boxes = !EspSettings.boxes;
                yield toggleBoxesText();
            }
            case MOBS -> {
                EspSettings.renderMobs = !EspSettings.renderMobs;
                yield toggleMobsText();
            }
            case FILLED -> {
                EspSettings.filledBoxes = !EspSettings.filledBoxes;
                yield toggleFilledText();
            }
            case TRACERS -> {
                EspSettings.tracers = !EspSettings.tracers;
                yield toggleTracersText();
            }
            case NAMETAGS -> {
                EspSettings.nametags = !EspSettings.nametags;
                yield toggleNametagsText();
            }
            case ANCIENT_DEBRIS -> {
                EspSettings.ancientDebrisEsp = !EspSettings.ancientDebrisEsp;
                yield toggleAncientDebrisText();
            }
            case STORAGE -> {
                EspSettings.storageEsp = !EspSettings.storageEsp;
                yield toggleStorageText();
            }
            case WALL_MODELS -> {
                EspSettings.wallModels = !EspSettings.wallModels;
                yield toggleWallModelsText();
            }
            case GHOST -> {
                EspSettings.ghostEsp = !EspSettings.ghostEsp;
                yield toggleGhostText();
            }
            case FREECAM -> {
                FreecamController.toggle();
                yield toggleFreecamText();
            }
        };
    }

    private Text toggleEspText() {
        return Text.literal("ESP: " + onOff(EspSettings.espEnabled));
    }

    private Text togglePlayersText() {
        return Text.literal("Players: " + onOff(EspSettings.renderPlayers));
    }

    private Text toggleMobsText() {
        return Text.literal("Mobs: " + onOff(EspSettings.renderMobs));
    }

    private Text toggleBoxesText() {
        return Text.literal("Boxes: " + onOff(EspSettings.boxes));
    }

    private Text toggleFilledText() {
        return Text.literal("Filled Boxes: " + onOff(EspSettings.filledBoxes));
    }

    private Text toggleTracersText() {
        return Text.literal("Tracers: " + onOff(EspSettings.tracers));
    }

    private Text toggleNametagsText() {
        return Text.literal("Nametags: " + onOff(EspSettings.nametags));
    }

    private Text toggleAncientDebrisText() {
        return Text.literal("Ancient Debris: " + onOff(EspSettings.ancientDebrisEsp));
    }

    private Text toggleStorageText() {
        return Text.literal("Storage ESP: " + onOff(EspSettings.storageEsp));
    }

    private Text toggleGhostText() {
        return Text.literal("Ghost ESP: " + onOff(EspSettings.ghostEsp));
    }

    private Text toggleWallModelsText() {
        return Text.literal("Wall Models: " + onOff(EspSettings.wallModels));
    }

    private Text toggleFreecamText() {
        return Text.literal("Freecam: " + onOff(FreecamController.isActive()));
    }

    private Text guiKeyText() {
        return Text.literal("GUI Key: " + keyName(EspSettings.getBoundGuiKey()));
    }

    private Text toggleKeyText() {
        return Text.literal("ESP Toggle Key: " + keyName(EspSettings.getBoundToggleEspKey()));
    }

    private String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    private String keyName(InputUtil.Key key) {
        if (key == null || key.equals(InputUtil.UNKNOWN_KEY)) {
            return "UNBOUND";
        }

        return key.getLocalizedText().getString();
    }

    private enum EspSettingsToggle {
        ESP,
        PLAYERS,
        BOXES,
        MOBS,
        FILLED,
        TRACERS,
        NAMETAGS,
        ANCIENT_DEBRIS,
        STORAGE,
        WALL_MODELS,
        GHOST,
        FREECAM
    }

    private enum KeyCaptureMode {
        NONE,
        GUI,
        ESP_TOGGLE
    }

    private static final class ConfigSlider extends SliderWidget {
        private final String label;
        private final double min;
        private final double max;
        private final Consumer<Double> setter;
        private final Function<Double, String> formatter;

        private ConfigSlider(
            int x,
            int y,
            int width,
            int height,
            String label,
            double initialValue,
            double min,
            double max,
            Consumer<Double> setter,
            Function<Double, String> formatter
        ) {
            super(x, y, width, height, Text.empty(), toSliderValue(initialValue, min, max));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.formatter = formatter;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            double currentValue = this.fromSliderValue(this.value);
            this.setMessage(Text.literal(this.label + ": " + this.formatter.apply(currentValue)));
        }

        @Override
        protected void applyValue() {
            this.setter.accept(this.fromSliderValue(this.value));
        }

        private double fromSliderValue(double sliderValue) {
            return this.min + sliderValue * (this.max - this.min);
        }

        private static double toSliderValue(double value, double min, double max) {
            if (max <= min) {
                return 0.0D;
            }

            return (value - min) / (max - min);
        }
    }
}
