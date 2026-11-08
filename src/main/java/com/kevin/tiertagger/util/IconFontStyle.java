package com.kevin.tiertagger.util;

import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class IconFontStyle {
    private IconFontStyle() {
    }

    public static Style apply(Style style, Identifier font) {
        for (Method method : Style.class.getMethods()) {
            if (method.getReturnType() != Style.class || method.getParameterCount() != 1) continue;

            Class<?> parameterType = method.getParameterTypes()[0];
            Object fontValue;
            if (parameterType == Identifier.class) {
                fontValue = font;
            } else {
                fontValue = createFontSource(parameterType, font);
                if (fontValue == null) continue;
            }

            try {
                return (Style) method.invoke(style, fontValue);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not apply the custom gamemode icon font", exception);
            }
        }
        throw new IllegalStateException("Could not find a compatible Minecraft text font method");
    }

    private static Object createFontSource(Class<?> sourceType, Identifier font) {
        if (!sourceType.isInterface()) return null;

        for (Class<?> nestedType : sourceType.getDeclaredClasses()) {
            if (!sourceType.isAssignableFrom(nestedType)) continue;
            for (Constructor<?> constructor : nestedType.getConstructors()) {
                if (constructor.getParameterCount() != 1 || constructor.getParameterTypes()[0] != Identifier.class) continue;
                try {
                    return constructor.newInstance(font);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Could not create the custom font source", exception);
                }
            }
        }
        return null;
    }
}
