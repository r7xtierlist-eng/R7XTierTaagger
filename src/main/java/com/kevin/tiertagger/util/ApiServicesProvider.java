package com.kevin.tiertagger.util;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ApiServices;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ApiServicesProvider {
    private ApiServicesProvider() {
    }

    public static ApiServices get(MinecraftClient client) {
        for (Method method : client.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || !ApiServices.class.isAssignableFrom(method.getReturnType())) continue;
            try {
                Object services = method.invoke(client);
                if (services instanceof ApiServices apiServices) return apiServices;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not get Minecraft API services", exception);
            }
        }

        for (Class<?> current = client.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!YggdrasilAuthenticationService.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object service = field.get(client);
                    if (service instanceof YggdrasilAuthenticationService authenticationService) {
                        return ApiServices.create(authenticationService, client.runDirectory);
                    }
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Could not access Minecraft authentication services", exception);
                }
            }
        }
        throw new IllegalStateException("Could not find Minecraft authentication services");
    }
}
