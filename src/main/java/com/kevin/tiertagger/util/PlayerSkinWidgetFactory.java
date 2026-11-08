package com.kevin.tiertagger.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.PlayerSkinWidget;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class PlayerSkinWidgetFactory {
    private PlayerSkinWidgetFactory() {
    }

    public static PlayerSkinWidget create(int width, int height, Supplier<?> skinSupplier) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (Constructor<?> constructor : PlayerSkinWidget.class.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 4 || parameters[0] != int.class || parameters[1] != int.class
                    || !Supplier.class.isAssignableFrom(parameters[3])) {
                continue;
            }

            Object models = findModels(client, parameters[2]);
            if (models == null) continue;

            try {
                return (PlayerSkinWidget) constructor.newInstance(width, height, models, skinSupplier);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not create the player skin preview", exception);
            }
        }
        throw new IllegalStateException("No compatible player skin widget constructor was found");
    }

    public static Supplier<?> getSkinTexturesSupplier(GameProfile profile) {
        Object provider = MinecraftClient.getInstance().getSkinProvider();
        for (Method method : provider.getClass().getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if ((parameters.length != 1 && parameters.length != 2) || parameters[0] != GameProfile.class
                    || !Supplier.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            try {
                Object supplier = parameters.length == 1
                        ? method.invoke(provider, profile)
                        : parameters.length == 2 && parameters[1] == boolean.class
                        ? method.invoke(provider, profile, false)
                        : null;
                if (supplier instanceof Supplier<?> skinSupplier) return skinSupplier;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not load player skin textures", exception);
            }
        }
        throw new IllegalStateException("No compatible skin texture supplier was found");
    }

    private static Object findModels(MinecraftClient client, Class<?> expectedType) {
        for (Method method : client.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || !expectedType.isAssignableFrom(method.getReturnType())) continue;
            try {
                Object models = method.invoke(client);
                if (expectedType.isInstance(models)) return models;
            } catch (ReflectiveOperationException ignored) {
                // Try another public accessor with the expected model type.
            }
        }
        return null;
    }
}
