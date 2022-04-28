package com.p4.cycleswitcher.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

@Environment(value= EnvType.CLIENT)
public class WeatherSelectionScreen
        extends Screen {
    static final Identifier TEXTURE = new Identifier("textures/gui/container/gamemode_switcher.png");
    private static final int TEXTURE_WIDTH = 128;
    private static final int TEXTURE_HEIGHT = 128;
    private static final int BUTTON_SIZE = 26;
    private static final int ICON_OFFSET = 5;
    private static final int UI_WIDTH = WeatherSelection.values().length * 31 - 5;
    private static final Text SELECT_NEXT_TEXT = new TranslatableText("debug.weathers.select_next", new TranslatableText("debug.weathers.press_f5").formatted(Formatting.AQUA));
    private final Optional<WeatherSelection> currentWeather;
    private Optional<WeatherSelection> weather = Optional.empty();
    private int lastMouseX;
    private int lastMouseY;
    private boolean mouseUsedForSelection;
    private final List<ButtonWidget> weatherButtons = Lists.newArrayList();
    private static WeatherSelection lastWeather;

    public WeatherSelectionScreen() {
        super(NarratorManager.EMPTY);
        this.currentWeather = WeatherSelection.of(this.getPreviousWeather());
    }

    private WeatherSelection getPreviousWeather() {
        return lastWeather;
    }

    @Override
    protected void init() {
        super.init();
        this.weather = this.currentWeather.isPresent() ? this.currentWeather : WeatherSelection.of(client.world.isRaining() ? WeatherSelection.RAIN : client.world.isThundering() ? WeatherSelection.THUNDER : WeatherSelection.CLEAR);
        this.weather = this.weather.get().next();
        for (int i = 0; i < WeatherSelection.VALUES.length; ++i) {
            WeatherSelection weatherSelection = WeatherSelection.VALUES[i];
            this.weatherButtons.add(new ButtonWidget(weatherSelection, this.width / 2 - UI_WIDTH / 2 + i * 31, this.height / 2 - 31));
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.checkForClose()) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = this.width / 2 - 62;
        int j = this.height / 2 - 31 - 27;
        drawTexture(matrices, i, j, 0.0f, 0.0f, 125, 75, 128, 128);
        matrices.pop();
        super.render(matrices, mouseX, mouseY, delta);
        this.weather.ifPresent(weather -> drawCenteredText(matrices, this.textRenderer, weather.getText(), this.width / 2, this.height / 2 - 31 - 20, -1));
        drawCenteredText(matrices, this.textRenderer, SELECT_NEXT_TEXT, this.width / 2, this.height / 2 + 5, 0xFFFFFF);
        if (!this.mouseUsedForSelection) {
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            this.mouseUsedForSelection = true;
        }
        boolean bl = this.lastMouseX == mouseX && this.lastMouseY == mouseY;
        for (ButtonWidget buttonWidget : this.weatherButtons) {
            buttonWidget.render(matrices, mouseX, mouseY, delta);
            this.weather.ifPresent(weather -> buttonWidget.setSelected(weather == buttonWidget.weather));
            if (bl || !buttonWidget.isHovered()) continue;
            this.weather = Optional.of(buttonWidget.weather);
        }
    }

    private void apply() {
        apply(this.client, this.weather);
    }

    private static void apply(MinecraftClient client, Optional<WeatherSelection> weather) {
        if (client.interactionManager == null || client.player == null || !weather.isPresent()) {
            return;
        }
        Optional<WeatherSelection> optional = WeatherSelection.of(client.world.isRaining() ? WeatherSelection.RAIN : client.world.isThundering() ? WeatherSelection.THUNDER : WeatherSelection.CLEAR);
        WeatherSelection weatherSelection = weather.get();
        if (optional.isPresent() && client.player.hasPermissionLevel(2) ) { // && weatherSelection != optional.get() ) {//&& weatherSelection != lastWeather) {
            client.player.sendChatMessage(weatherSelection.getCommand());
        }
        lastWeather = weatherSelection;
    }

    private boolean checkForClose() {
        if (!InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_F3)) {
            this.apply();
            this.client.setScreen(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F5 && this.weather.isPresent()) {
            this.mouseUsedForSelection = false;
            this.weather = this.weather.get().next();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Environment(value=EnvType.CLIENT)
    enum WeatherSelection {
        CLEAR(new TranslatableText("weather.clear"), "/weather clear", new ItemStack(Items.SUNFLOWER)),
        RAIN(new TranslatableText("weather.rain"), "/weather rain", new ItemStack(Items.WATER_BUCKET)),
        THUNDER(new TranslatableText("weather.thunder"), "/weather thunder", new ItemStack(Blocks.LIGHTNING_ROD));

        protected static final WeatherSelection[] VALUES;
        final Text text;
        final String command;
        final ItemStack icon;

        WeatherSelection(Text text, String command, ItemStack icon) {
            this.text = text;
            this.command = command;
            this.icon = icon;
        }

        void renderIcon(ItemRenderer itemRenderer, int x, int y) {
            itemRenderer.renderInGuiWithOverrides(this.icon, x, y);
        }

        Text getText() {
            return this.text;
        }

        String getCommand() {
            return this.command;
        }

        Optional<WeatherSelection> next() {
            switch (this) {
                case CLEAR -> {
                    return Optional.of(RAIN);
                }
                case RAIN -> {
                    return Optional.of(THUNDER);
                }
                case THUNDER -> {
                    return Optional.of(CLEAR);
                }
            }
            return Optional.of(CLEAR);
        }

        static Optional<WeatherSelection> of(WeatherSelection weatherSelection) {
            if (weatherSelection == null) return Optional.empty();
            switch (weatherSelection) {
                case CLEAR -> {
                    return Optional.of(CLEAR);
                }
                case RAIN -> {
                    return Optional.of(RAIN);
                }
                case THUNDER -> {
                    return Optional.of(THUNDER);
                }
            }
            return Optional.empty();
        }

        static {
            VALUES = WeatherSelection.values();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public class ButtonWidget
            extends ClickableWidget {
        final WeatherSelection weather;
        private boolean selected;

        public ButtonWidget(WeatherSelection weather, int x, int y) {
            super(x, y, 26, 26, weather.getText());
            this.weather = weather;
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            this.drawBackground(matrices, minecraftClient.getTextureManager());
            this.weather.renderIcon(WeatherSelectionScreen.this.itemRenderer, this.x + 5, this.y + 5);
            if (this.selected) {
                this.drawSelectionBox(matrices, minecraftClient.getTextureManager());
            }
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
            this.appendDefaultNarrations(builder);
        }

        @Override
        public boolean isHovered() {
            return super.isHovered() || this.selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        private void drawBackground(MatrixStack matrices, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            matrices.push();
            matrices.translate(this.x, this.y, 0.0);
            drawTexture(matrices, 0, 0, 0.0f, 75.0f, 26, 26, 128, 128);
            matrices.pop();
        }

        private void drawSelectionBox(MatrixStack matrices, TextureManager textureManager) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            matrices.push();
            matrices.translate(this.x, this.y, 0.0);
            drawTexture(matrices, 0, 0, 26.0f, 75.0f, 26, 26, 128, 128);
            matrices.pop();
        }
    }
}