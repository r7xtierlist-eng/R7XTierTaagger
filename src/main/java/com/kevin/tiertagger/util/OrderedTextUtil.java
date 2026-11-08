package com.kevin.tiertagger.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

public final class OrderedTextUtil {
    private OrderedTextUtil() {
    }

    public static Text toText(OrderedText orderedText) {
        MutableText text = Text.empty();
        orderedText.accept((index, style, codePoint) -> {
            text.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(style));
            return true;
        });
        return text;
    }
}
