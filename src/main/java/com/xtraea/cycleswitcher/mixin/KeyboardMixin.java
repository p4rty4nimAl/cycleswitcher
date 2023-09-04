package com.xtraea.cycleswitcher.mixin;

import com.xtraea.cycleswitcher.screen.SelectionScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    private void addDebugMessage(Formatting formatting, Text text) {
        this.client.inGameHud.getChatHud().addMessage(Text.translatable("debug.prefix").formatted(formatting, Formatting.BOLD).append(" ").append(text));
    }

    @Inject(method = "processF3(I)Z", at = @At("RETURN"), cancellable = true)
    private void onProcessF3(int key, CallbackInfoReturnable<Boolean> cir) {
        switch (key) {
            case 81 -> { // Q
                ChatHud chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
                chatHud.addMessage(Text.translatable("debug.weather.help"));
                chatHud.addMessage(Text.translatable("debug.time.help"));
                chatHud.addMessage(Text.translatable("debug.difficulty.help"));
                cir.setReturnValue(true);
            }
            case 294 -> { // F5
                if (this.client.player.hasPermissionLevel(2)) {
                    this.client.setScreen(new SelectionScreen("weather", key));
                } else addDebugMessage(Formatting.YELLOW, Text.translatable("debug.weather.error"));
                cir.setReturnValue(true);
            }
            case 295 -> { // F6
                if (this.client.player.hasPermissionLevel(2)) {
                    this.client.setScreen(new SelectionScreen("time", key));
                } else addDebugMessage(Formatting.YELLOW, Text.translatable("debug.time.error"));
                cir.setReturnValue(true);
            }
            case 296 -> { // F7
                if (this.client.world.getLevelProperties().isDifficultyLocked()) {
                    addDebugMessage(Formatting.YELLOW, Text.translatable("debug.difficulty.error2"));
                    cir.setReturnValue(true);
                    return;
                }
                if (this.client.player.hasPermissionLevel(2)) {
                    this.client.setScreen(new SelectionScreen("difficulty", key));
                } else addDebugMessage(Formatting.YELLOW, Text.translatable("debug.difficulty.error"));
                cir.setReturnValue(true);
            }
        }
    }
}
