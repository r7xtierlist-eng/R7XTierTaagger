package com.kevin.tiertagger.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.ApiServices;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public final class ProfileLookup {
    private ProfileLookup() {
    }

    public static Optional<GameProfile> find(ApiServices services, String username) {
        Object repository = services.profileRepository();
        try {
            Method directLookup = repository.getClass().getMethod("findProfileByName", String.class);
            return unwrap(directLookup.invoke(repository, username));
        } catch (NoSuchMethodException ignored) {
            // Older Authlib versions use the callback based lookup below.
        } catch (ReflectiveOperationException exception) {
            throw new CompletionException(exception);
        }

        Method callbackLookup = Arrays.stream(repository.getClass().getMethods())
                .filter(method -> method.getName().equals("findProfilesByNames"))
                .filter(method -> method.getParameterCount() == 2 || method.getParameterCount() == 3)
                .filter(method -> method.getParameterTypes()[0] == String[].class)
                .min(Comparator.comparingInt(Method::getParameterCount))
                .orElseThrow(() -> new IllegalStateException("No supported profile lookup method"));

        Class<?>[] parameterTypes = callbackLookup.getParameterTypes();
        Class<?> callbackType = parameterTypes[parameterTypes.length - 1];
        CompletableFuture<GameProfile> result = new CompletableFuture<>();
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("onProfileLookupSucceeded")) {
                result.complete((GameProfile) args[0]);
            } else if (method.getName().equals("onProfileLookupFailed")) {
                Throwable cause = args != null && args.length > 0 && args[args.length - 1] instanceof Throwable throwable
                        ? throwable
                        : new IllegalArgumentException("Player profile was not found");
                result.completeExceptionally(cause);
            } else if (method.getName().equals("toString")) {
                return "R7XTiertagger profile lookup callback";
            } else if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            } else if (method.getName().equals("equals")) {
                return proxy == args[0];
            }
            return null;
        };
        Object callback = Proxy.newProxyInstance(callbackType.getClassLoader(), new Class<?>[]{callbackType}, handler);

        try {
            Object[] arguments;
            if (parameterTypes.length == 2) {
                arguments = new Object[]{new String[]{username}, callback};
            } else {
                arguments = new Object[]{new String[]{username}, minecraftAgent(parameterTypes[1]), callback};
            }

            callbackLookup.invoke(repository, arguments);
            return Optional.ofNullable(result.get(15, TimeUnit.SECONDS));
        } catch (ReflectiveOperationException exception) {
            throw new CompletionException(exception);
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }

    private static Object minecraftAgent(Class<?> agentType) throws ReflectiveOperationException {
        try {
            Field minecraft = agentType.getField("MINECRAFT");
            return minecraft.get(null);
        } catch (NoSuchFieldException ignored) {
            if (agentType.isEnum()) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object minecraft = Enum.valueOf((Class<? extends Enum>) agentType.asSubclass(Enum.class), "MINECRAFT");
                return minecraft;
            }
            throw ignored;
        }
    }

    private static Optional<GameProfile> unwrap(Object result) throws ReflectiveOperationException {
        if (result instanceof Optional<?> optional) {
            if (optional.isEmpty()) return Optional.empty();
            result = optional.get();
        }
        if (result instanceof GameProfile profile) return Optional.of(profile);
        if (result == null) return Optional.empty();

        Object profile = result.getClass().getMethod("profile").invoke(result);
        return profile instanceof GameProfile gameProfile ? Optional.of(gameProfile) : Optional.empty();
    }
}
