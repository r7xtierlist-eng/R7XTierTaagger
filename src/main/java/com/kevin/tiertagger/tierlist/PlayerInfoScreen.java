package com.kevin.tiertagger.tierlist;

import com.kevin.tiertagger.TierTagger;
import com.kevin.tiertagger.model.GameMode;
import com.kevin.tiertagger.model.PlayerInfo;
import com.kevin.tiertagger.util.DrawContextCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.uku3lig.ukulib.config.screen.CloseableScreen;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class PlayerInfoScreen extends CloseableScreen {
    private final PlayerInfo info;
    private final PlayerSkinWidget skin;

    public PlayerInfoScreen(Screen parent, PlayerInfo info, PlayerSkinWidget skin) {
        super(Text.translatable("tiertagger.profile.title", info.name()), parent);
        this.info = info;
        this.skin = skin;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> MinecraftClient.getInstance().setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());

        if (this.skin != null) this.addDrawableChild(this.skin);

        int rankingHeight = this.info.rankings().size() * 11;
        int infoHeight = 56; // 4 lines of text (10 px tall) + 6 px padding
        int startY = (this.height - infoHeight - rankingHeight) / 2;
        int rankingY = startY + infoHeight;

        for (PlayerInfo.NamedRanking namedRanking : this.info.getSortedTiers()) {
            // ugly "fix" to avoid crashes if upstream doesn't have the right names
            if (namedRanking.mode() == null) continue;

            TextWidget text = new TextWidget(formatTier(namedRanking.mode(), namedRanking.ranking()), this.textRenderer);
            text.setX(this.width / 2 + 5);
            text.setY(rankingY);

            MutableText tooltip = Text.translatable("tiertagger.profile.points_value", points(namedRanking.ranking()));
            if (namedRanking.ranking().attained() > 0) {
                String date = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(Instant.ofEpochSecond(namedRanking.ranking().attained()));
                tooltip = Text.translatable("tiertagger.profile.attained", date, tooltip);
            }
            Text tooltipText = tooltip.copy().formatted(Formatting.GRAY);
            text.setTooltip(Tooltip.of(tooltipText));
            this.addDrawableChild(text);
            rankingY += 11;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("tiertagger.profile.title", this.info.name()), this.width / 2, 20, 0xFFFFFFFF);

        int rankingHeight = this.info.rankings().size() * 11;
        int infoHeight = 56; // 4 lines of text (10 px tall) + 6 px padding
        int startY = (this.height - infoHeight - rankingHeight) / 2;

        DrawContextCompat.drawTextWithShadow(context, this.textRenderer, getRegionText(this.info), this.width / 2 + 5, startY, 0xFFFFFFFF);
        DrawContextCompat.drawTextWithShadow(context, this.textRenderer, getPointsText(this.info), this.width / 2 + 5, startY + 15, 0xFFFFFFFF);
        DrawContextCompat.drawTextWithShadow(context, this.textRenderer, getRankText(this.info), this.width / 2 + 5, startY + 30, 0xFFFFFFFF);
        DrawContextCompat.drawTextWithShadow(context, this.textRenderer, Text.translatable("tiertagger.profile.rankings"), this.width / 2 + 5, startY + 45, 0xFFFFFFFF);
    }

    private Text formatTier(@NotNull GameMode gamemode, PlayerInfo.Ranking ranking) {
        Text tierText = TierTagger.getRankingText(ranking, true);

        return Text.empty()
                .append(gamemode.asStyled(true))
                .append(Text.literal(": ").formatted(Formatting.GRAY))
                .append(tierText);
    }

    private Text getRegionText(PlayerInfo info) {
        return Text.empty()
                .append(Text.translatable("tiertagger.profile.region"))
                .append(Text.literal(": "))
                .append(Text.literal(info.region()).styled(s -> s.withColor(info.getRegionColor())));
    }

    private Text getPointsText(PlayerInfo info) {
        PlayerInfo.PointInfo pointInfo = info.getPointInfo();

        return Text.empty()
                .append(Text.translatable("tiertagger.profile.points"))
                .append(Text.literal(": "))
                .append(Text.literal(info.points() + " ").styled(s -> s.withColor(pointInfo.getColor())))
                .append(Text.literal("("))
                .append(info.getTitleText().copy().styled(s -> s.withColor(pointInfo.getAccentColor())))
                .append(Text.literal(")"));
    }

    private Text getRankText(PlayerInfo info) {
        int color = switch (info.overall()) {
            case 1 -> 0xe5ba43;
            case 2 -> 0x808c9c;
            case 3 -> 0xb56326;
            default -> 0x1e2634;
        };

        return Text.empty()
                .append(Text.translatable("tiertagger.profile.global_rank"))
                .append(Text.literal(": "))
                .append(Text.literal("#" + info.overall()).styled(s -> s.withColor(color)));
    }

    private int points(PlayerInfo.Ranking ranking) {
        return switch (ranking.tier()) {
            case 1 -> ranking.pos() == 0 ? 60 : 45;
            case 2 -> ranking.pos() == 0 ? 30 : 20;
            case 3 -> ranking.pos() == 0 ? 10 : 6;
            case 4 -> ranking.pos() == 0 ? 4 : 3;
            case 5 -> ranking.pos() == 0 ? 2 : 1;
            default -> 0;
        };
    }
}
