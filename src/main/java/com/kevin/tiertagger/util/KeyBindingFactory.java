package com.kevin.tiertagger.util;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class KeyBindingFactory {
    private KeyBindingFactory() {
    }

    public static KeyBinding create(String translationKey, int keyCode, String categoryTranslationKey) {
        for (Constructor<?> constructor : KeyBinding.class.getConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 3 || parameters[0] != String.class || parameters[1] != int.class) continue;

            try {
                Object category = parameters[2] == String.class
                        ? categoryTranslationKey
                        : createCategory(parameters[2]);
                return (KeyBinding) constructor.newInstance(translationKey, keyCode, category);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not create the gamemode key binding", exception);
            }
        }
        throw new IllegalStateException("No compatible Minecraft key binding constructor was found");
    }

    private static Object createCategory(Class<?> categoryType) throws ReflectiveOperationException {
        for (Method method : categoryType.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != Identifier.class
                    || !categoryType.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            return method.invoke(null, Identifier.of("tiertagger", "general"));
        }
        throw new IllegalStateException("Could not create the R7XTiertagger key binding category");
    }
}
