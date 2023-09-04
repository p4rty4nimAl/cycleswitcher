package com.xtraea.cycleswitcher.screen;

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
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SelectionScreen extends Screen {
        static final Identifier TEXTURE = new Identifier("textures/gui/container/gamemode_switcher.png");
        private final int UI_WIDTH;
        private final Text SELECT_NEXT_TEXT;
        private Optional<Selection> selection;
        private int lastMouseX;
        private int lastMouseY;
        private boolean mouseUsedForSelection;
        private final List<ButtonWidget> selectionButtons = Lists.newArrayList();
        private static Selection[] lastSelection = new Selection[3];
        //Weather, Time, Difficulty
        private int lastSelectionTypeIndex;
        private final String selectionCategory;
        private final int cycleKey;

    public SelectionScreen(String selectionCategory, int cycleKey) {
            super(NarratorManager.EMPTY);
            this.lastSelectionTypeIndex = selectionCategory.equals("weather") ? 0 : selectionCategory.equals("time") ? 1 : 2;
            this.SELECT_NEXT_TEXT = Text.translatable("debug."+ selectionCategory + ".select_next", Text.translatable("debug." + selectionCategory + ".press_key").formatted(Formatting.AQUA));
            this.selection = Selection.of(lastSelection[lastSelectionTypeIndex]);
            this.selectionCategory = selectionCategory;
            this.cycleKey = cycleKey;
            ArrayList<Integer> templist = new ArrayList<>();
            for (int i = 0; i < Selection.VALUES.length; ++i) {
                if (Objects.equals(Selection.VALUES[i].selectionType, selectionCategory)) {
                    templist.add(i);
                }
            }
            this.UI_WIDTH = templist.size() * 31 - 5;
        }

        @Override
        protected void init() {
            super.init();
            int itemCount = 0;
            for (int i = 0; i < Selection.VALUES.length; ++i) {
                if (Objects.equals(Selection.VALUES[i].selectionType, this.selectionCategory)) {
                    this.selectionButtons.add(new ButtonWidget(
                            Selection.VALUES[i],
                            this.width / 2 - UI_WIDTH / 2 + itemCount * 31,
                            this.height / 2 - 31
                    ));
                    itemCount++;
                }
            }
            for (int i = 0; i < Selection.VALUES.length; ++i) {
                if (Objects.equals(Selection.VALUES[i].selectionType, this.selectionCategory)) {
                    this.selection = Selection.VALUES[i].next();
                }
            }
            if (lastSelection[lastSelectionTypeIndex] != null) {
                this.selection = Optional.of(lastSelection[lastSelectionTypeIndex]);
            }
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (this.checkForClose()) {
                return;
            }
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            matrices.push();
            RenderSystem.enableBlend();
            RenderSystem.setShaderTexture(0, TEXTURE);
            int i = this.width / 2 - 62;
            int j = this.height / 2 - 31 - 27;
            drawTexture(matrices, i, j, 0.0f, 0.0f, 125, 75, 128, 128);
            matrices.pop();
            super.render(matrices, mouseX, mouseY, delta);
            this.selection.ifPresent(selection -> drawCenteredTextWithShadow(matrices, this.textRenderer, selection.getText(), this.width / 2, this.height / 2 - 31 - 20, -1));
            drawCenteredTextWithShadow(matrices, this.textRenderer, SELECT_NEXT_TEXT, this.width / 2, this.height / 2 + 5, 0xFFFFFF);
            if (!this.mouseUsedForSelection) {
                this.lastMouseX = mouseX;
                this.lastMouseY = mouseY;
                this.mouseUsedForSelection = true;
            }
            boolean bl = this.lastMouseX == mouseX && this.lastMouseY == mouseY;
            for (ButtonWidget buttonWidget : this.selectionButtons) {
                buttonWidget.render(matrices, mouseX, mouseY, delta);
                this.selection.ifPresent(selection -> buttonWidget.setSelected(selection == buttonWidget.selection));
                if (bl || !buttonWidget.isHovered()) continue;
                this.selection = Optional.of(buttonWidget.selection);
            }
        }

        private void apply(MinecraftClient client, Optional<Selection> selection) {
            Selection newSelection = selection.get();
            client.player.networkHandler.sendCommand(newSelection.getCommand());
            switch (lastSelectionTypeIndex) {
                case 0 -> {
                    if (this.client.world.getThunderGradient(1) > 0) {
                        lastSelection[lastSelectionTypeIndex] = Selection.THUNDER;
                    } else if (this.client.world.getRainGradient(1) > 0) {
                        lastSelection[lastSelectionTypeIndex] = Selection.RAIN;
                    } else lastSelection[lastSelectionTypeIndex] = Selection.CLEAR;
                }
                case 1 -> {
                    long time = this.client.world.getTimeOfDay();
                    if (time > 18000) {
                        lastSelection[lastSelectionTypeIndex] = Selection.MIDNIGHT;
                    } else if (time > 13000) {
                        lastSelection[lastSelectionTypeIndex] = Selection.NIGHT;
                    } else if (time > 6000) {
                        lastSelection[lastSelectionTypeIndex] = Selection.NOON;
                    } else lastSelection[lastSelectionTypeIndex] = Selection.DAY;
                }
                case 2 -> {
                    int difficulty = this.client.world.getDifficulty().getId();
                    switch (difficulty) {
                        case 0 -> {
                            lastSelection[lastSelectionTypeIndex] = Selection.PEACEFUL;
                        }
                        case 1 -> {
                            lastSelection[lastSelectionTypeIndex] = Selection.EASY;
                        }
                        case 2 -> {
                            lastSelection[lastSelectionTypeIndex] = Selection.NORMAL;
                        }
                        case 3 -> {
                            lastSelection[lastSelectionTypeIndex] = Selection.HARD;
                        }
                    }
                }
            }
        }

        private boolean checkForClose() {
            if (!InputUtil.isKeyPressed(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_F3)) {
                this.apply(this.client, this.selection);
                this.client.setScreen(null);
                return true;
            }
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == cycleKey) {
                this.mouseUsedForSelection = false;
                this.selection = this.selection.get().next();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean shouldPause() {
            return false;
        }

        @Environment(value=EnvType.CLIENT)
        enum Selection {
            //WEATHERS
            CLEAR(Text.translatable("weather.clear"), "weather clear", new ItemStack(Items.SUNFLOWER), "weather"),
            RAIN(Text.translatable("weather.rain"), "weather rain", new ItemStack(Items.WATER_BUCKET), "weather"),
            THUNDER(Text.translatable("weather.thunder"), "weather thunder", new ItemStack(Blocks.LIGHTNING_ROD), "weather"),
            //TIMES
            DAY(Text.translatable("time.day"), "time set day", new ItemStack(Blocks.ORANGE_CONCRETE), "time"),
            NOON(Text.translatable("time.noon"), "time set noon", new ItemStack(Items.SUNFLOWER), "time"),
            NIGHT(Text.translatable("time.night"), "time set night", new ItemStack(Items.INK_SAC), "time"),
            MIDNIGHT(Text.translatable("time.midnight"), "time set midnight", new ItemStack(Items.BLACK_DYE), "time"),
            //DIFFICULTIES
            PEACEFUL(Text.translatable("difficulty.peaceful"), "difficulty peaceful", new ItemStack(Items.IRON_HOE), "difficulty"),
            EASY(Text.translatable("difficulty.easy"), "difficulty easy", new ItemStack(Items.WOODEN_SWORD), "difficulty"),
            NORMAL(Text.translatable("difficulty.normal"), "difficulty normal", new ItemStack(Items.COOKED_BEEF), "difficulty"),
            HARD(Text.translatable("difficulty.hard"), "difficulty hard", new ItemStack(Blocks.SKELETON_SKULL), "difficulty");

            private static final Selection[] VALUES;
            final Text text;
            final String command;
            final ItemStack icon;
            final String selectionType;

            Selection(Text text, String command, ItemStack icon, String selectionType) {
                this.text = text;
                this.command = command;
                this.icon = icon;
                this.selectionType = selectionType;
            }

            void renderIcon(MatrixStack matrices, ItemRenderer itemRenderer, int x, int y) {
                itemRenderer.renderInGuiWithOverrides(matrices, this.icon, x, y);
            }

            Text getText() {
                return this.text;
            }

            String getCommand() {
                return this.command;
            }

            Optional<Selection> next() {
                switch (this.selectionType) {
                    case "weather" -> {
                        return nextWeather();
                    }
                    case "time" -> {
                        return nextTime();
                    }
                    case "difficulty" -> {
                        return nextDifficulty();
                    }
                }
                return Optional.empty();
            }
            Optional<Selection> nextWeather() {
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
            Optional<Selection> nextTime() {
                switch (this) {
                    case DAY -> {
                        return Optional.of(NOON);
                    }
                    case NOON -> {
                        return Optional.of(NIGHT);
                    }
                    case NIGHT -> {
                        return Optional.of(MIDNIGHT);
                    }
                    case MIDNIGHT -> {
                        return Optional.of(DAY);
                    }
                }
                return Optional.of(DAY);
            }
            Optional<Selection> nextDifficulty() {
                switch (this) {
                    case PEACEFUL -> {
                        return Optional.of(EASY);
                    }
                    case EASY -> {
                        return Optional.of(NORMAL);
                    }
                    case NORMAL -> {
                        return Optional.of(HARD);
                    }
                    case HARD -> {
                        return Optional.of(PEACEFUL);
                    }
                }
                return Optional.of(PEACEFUL);
            }
            static Optional<Selection> of(Selection selection) {
                if (selection == null) return Optional.empty();
                switch (selection) {
                    case CLEAR -> {
                        return Optional.of(CLEAR);
                    }
                    case RAIN -> {
                        return Optional.of(RAIN);
                    }
                    case THUNDER -> {
                        return Optional.of(THUNDER);
                    }
                    case DAY -> {
                        return Optional.of(DAY);
                    }
                    case NOON -> {
                        return Optional.of(NOON);
                    }
                    case NIGHT -> {
                        return Optional.of(NIGHT);
                    }
                    case MIDNIGHT -> {
                        return Optional.of(MIDNIGHT);
                    }
                    case PEACEFUL -> {
                        return Optional.of(PEACEFUL);
                    }
                    case EASY -> {
                        return Optional.of(EASY);
                    }
                    case NORMAL -> {
                        return Optional.of(NORMAL);
                    }
                    case HARD -> {
                        return Optional.of(HARD);
                    }
                }
                return Optional.empty();
            }

            static {
                VALUES = Selection.values();
            }
        }

        @Environment(value=EnvType.CLIENT)
        public class ButtonWidget
                extends ClickableWidget {
            final Selection selection;
            private boolean selected;

            public ButtonWidget(Selection selection, int x, int y) {
                super(x, y, 26, 26, selection.getText());
                this.selection = selection;
            }

            @Override
            public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                this.drawBackground(matrices, minecraftClient.getTextureManager());
                this.selection.renderIcon(matrices, SelectionScreen.this.itemRenderer, this.getX() + 5, this.getY() + 5);
                if (this.selected) {
                    this.drawSelectionBox(matrices, minecraftClient.getTextureManager());
                }
            }
            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {}

            @Override
            public boolean isHovered() {
                return super.isHovered() || this.selected;
            }

            public void setSelected(boolean selected) {
                this.selected = selected;
            }

            private void drawBackground(MatrixStack matrices, TextureManager textureManager) {
                RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                RenderSystem.setShaderTexture(0, TEXTURE);
                matrices.push();
                matrices.translate(this.getX(), this.getY(), 0.0);
                drawTexture(matrices, 0, 0, 0.0f, 75.0f, 26, 26, 128, 128);
                matrices.pop();
            }

            private void drawSelectionBox(MatrixStack matrices, TextureManager textureManager) {
                RenderSystem.setShader(GameRenderer::getPositionTexProgram);
                RenderSystem.setShaderTexture(0, TEXTURE);
                matrices.push();
                matrices.translate(this.getX(), this.getY(), 0.0);
                drawTexture(matrices, 0, 0, 26.0f, 75.0f, 26, 26, 128, 128);
                matrices.pop();
            }
        }
    }
