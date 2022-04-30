package com.p4.cycleswitcher.mixin;

import com.p4.cycleswitcher.screen.SelectionScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
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
        this.client.inGameHud.getChatHud().addMessage(new LiteralText("").append(new TranslatableText("debug.prefix").formatted(formatting, Formatting.BOLD)).append(" ").append(text));
    }

    @Inject(method = "processF3(I)Z", at = @At("RETURN"), cancellable = true)
    private void onProcessF3(int key, CallbackInfoReturnable<Boolean> cir) {
        switch (key) {
            case 81: {
                ChatHud chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();
                chatHud.addMessage(new TranslatableText("debug.weathers.help"));
                chatHud.addMessage(new TranslatableText("debug.times.help"));
                chatHud.addMessage(new TranslatableText("debug.difficulties.help"));
                cir.setReturnValue(true);
                return;
            }
            case 294: {
                if (this.client.player.hasPermissionLevel(2)) {
                    this.client.setScreen(new SelectionScreen("weather", key));
                } else addDebugMessage(Formatting.YELLOW, new TranslatableText("debug.weathers.error"));
                cir.setReturnValue(true);
                return;
            }
            case 295: {
                if (this.client.player.hasPermissionLevel(2)) {
                    this.client.setScreen(new SelectionScreen("time", key));
                } else addDebugMessage(Formatting.YELLOW, new TranslatableText("debug.times.error"));
                cir.setReturnValue(true);
                return;
            }
            case 296: {
                if (this.client.world.getLevelProperties().isDifficultyLocked()) {
                    addDebugMessage(Formatting.YELLOW, new TranslatableText("debug.difficulties.error2"));
                    cir.setReturnValue(true);
                    return;
                }
                if (this.client.player.hasPermissionLevel(2)) {
                    this.client.setScreen(new SelectionScreen("difficulty", key));
                } else addDebugMessage(Formatting.YELLOW, new TranslatableText("debug.difficulties.error"));
                cir.setReturnValue(true);
                return;
            }
            case 297: {
                this.client.setScreen(new SelectionScreen("weather", key));
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
