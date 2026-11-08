package com.kevin.tiertagger.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Bridges a DrawContext return-type change between supported Minecraft versions.
 * The 1.21.5 method returns an int, while newer versions return void, so a direct
 * invocation from a jar compiled against the newer version fails to link on 1.21.5.
 */
public final class DrawContextCompat {
    private static final Method DRAW_TEXT_WITH_SHADOW = findTextShadowMethod();

    private DrawContextCompat() {
    }

    public static void drawTextWithShadow(DrawContext context, TextRenderer textRenderer, Text text,
                                          int x, int y, int color) {
        try {
            DRAW_TEXT_WITH_SHADOW.invoke(context, textRenderer, text, x, y, color);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Could not access DrawContext text renderer", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("DrawContext text rendering failed", cause);
        }
    }

    private static Method findTextShadowMethod() {
        for (Method method : DrawContext.class.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            boolean textMethod = method.getName().equals("method_27535")
                    || method.getName().equals("drawTextWithShadow");
            if (textMethod
                    && parameters.length == 5
                    && parameters[0] == TextRenderer.class
                    && parameters[1] == Text.class
                    && parameters[2] == int.class
                    && parameters[3] == int.class
                    && parameters[4] == int.class) {
                return method;
            }
        }
        throw new ExceptionInInitializerError("Could not find DrawContext text rendering method");
    }
}
