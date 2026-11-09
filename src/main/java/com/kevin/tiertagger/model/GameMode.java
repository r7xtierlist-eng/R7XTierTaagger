package com.kevin.tiertagger.model;

import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.MutableText;
import com.kevin.tiertagger.util.IconFontStyle;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;

import java.util.Locale;
import java.util.Optional;

public record GameMode(String id, String title) {
    private static final Identifier ICON_FONT = Identifier.of("tier-tagger", "gamemodes");
    public static final GameMode NONE = new GameMode("annoying_long_id_that_no_one_will_ever_use_just_to_make_sure", "§cNone§r");

    public static GameMode fromId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        String title = switch (normalized) {
            case "nethpot" -> "Nethpot";
            case "dia_crystal", "crystal" -> "Crystal";
            case "dia_smp" -> "Diamond SMP";
            case "og_vanilla" -> "OG Vanilla";
            default -> normalized.isEmpty() ? id : Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
        };
        return new GameMode(normalized, title);
    }

    public boolean isNone() {
        return this.id.equals(NONE.id);
    }

    private Pair<Character, TextColor> iconAndColor() {
        return switch (this.id.toLowerCase(Locale.ROOT)) {
            case "axe" -> new Pair<>('\uE701', TextColor.fromFormatting(Formatting.GREEN));
            case "mace" -> new Pair<>('\uE702', TextColor.fromFormatting(Formatting.GRAY));
            case "nethop", "nethpot", "neth_pot" -> new Pair<>('\uE703', TextColor.fromRgb(0x7d4a40));
            case "pot" -> new Pair<>('\uE704', TextColor.fromRgb(0xff0000));
            case "smp" -> new Pair<>('\uE705', TextColor.fromRgb(0xeccb45));
            case "sword" -> new Pair<>('\uE706', TextColor.fromRgb(0xa4fdf0));
            case "uhc" -> new Pair<>('\uE707', TextColor.fromFormatting(Formatting.RED));
            case "vanilla" -> new Pair<>('\uE708', TextColor.fromFormatting(Formatting.LIGHT_PURPLE));
            case "bed" -> new Pair<>('\uE801', TextColor.fromRgb(0xff0000));
            case "bow" -> new Pair<>('\uE802', TextColor.fromRgb(0x663d10));
            case "creeper" -> new Pair<>('\uE803', TextColor.fromFormatting(Formatting.GREEN));
            case "debuff" -> new Pair<>('\uE804', TextColor.fromFormatting(Formatting.DARK_GRAY));
            case "crystal", "dia_crystal" -> new Pair<>('\uE708', TextColor.fromFormatting(Formatting.AQUA));
            case "dia_smp" -> new Pair<>('\uE806', TextColor.fromRgb(0x8c668b));
            case "elytra" -> new Pair<>('\uE807', TextColor.fromRgb(0x8d8db1));
            case "manhunt" -> new Pair<>('\uE808', TextColor.fromFormatting(Formatting.RED));
            case "minecart" -> new Pair<>('\uE809', TextColor.fromFormatting(Formatting.GRAY));
            case "og_vanilla" -> new Pair<>('\uE810', TextColor.fromFormatting(Formatting.GOLD));
            case "speed" -> new Pair<>('\uE811', TextColor.fromRgb(0x43a9d1));
            case "trident" -> new Pair<>('\uE812', TextColor.fromRgb(0x579b8c));
            default -> new Pair<>('•', TextColor.fromFormatting(Formatting.WHITE));
        };
    }

    public Optional<Character> icon() {
        Pair<Character, TextColor> pair = this.iconAndColor();

        return pair.getRight().getRgb() == 0xFFFFFF ? Optional.empty() : Optional.of(pair.getLeft());
    }

    public MutableText iconText() {
        Pair<Character, TextColor> pair = this.iconAndColor();
        return Text.literal(pair.getLeft().toString())
                .styled(style -> IconFontStyle.apply(style, ICON_FONT).withColor(pair.getRight()));
    }

    public Text asStyled(boolean withDefaultDot) {
        if (this.isNone()) return Text.translatable("tiertagger.gamemode.none.title");

        Pair<Character, TextColor> pair = this.iconAndColor();

        if (pair.getRight().getRgb() == 0xFFFFFF && !withDefaultDot) {
            return Text.of(this.title);
        } else {
            Text name = Text.literal(this.title).styled(s -> s.withColor(pair.getRight()));
            return Text.empty().append(this.iconText()).append(Text.literal(" ")).append(name);
        }
    }
}
