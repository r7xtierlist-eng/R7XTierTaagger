package com.kevin.tiertagger.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void tiertagger$hideUnsupportedMinecraftWarning(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof ConfirmScreen)) return;

        String title = screen.getTitle().getString().toLowerCase(Locale.ROOT);
        if (title.contains("unsupported minecraft version")
                || title.contains("desteklenmeyen minecraft sürümü")) {
            ci.cancel();
        }
    }
}
