package com.kevin.tiertagger.mixin;

import com.kevin.tiertagger.TierTagger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public class MixinTextDisplayEntityRenderer {
    @Inject(method = "getData", at = @At("RETURN"), cancellable = true)
    private void addTier(DisplayEntity.TextDisplayEntity entity,
                         CallbackInfoReturnable<DisplayEntity.TextDisplayEntity.Data> cir) {
        if (!TierTagger.getManager().getConfig().isEnabled()) return;
        if (!(entity.getVehicle() instanceof PlayerEntity player)) return;

        DisplayEntity.TextDisplayEntity.Data data = cir.getReturnValue();
        if (data == null) return;

        Text modified = TierTagger.appendTier(player.getNameForScoreboard(), data.text());
        if (modified == data.text()) return;

        int lineWidth = Math.max(data.lineWidth(), MinecraftClient.getInstance().textRenderer.getWidth(modified));
        cir.setReturnValue(new DisplayEntity.TextDisplayEntity.Data(
                modified, lineWidth, data.textOpacity(), data.backgroundColor(), data.flags()));
    }
}
