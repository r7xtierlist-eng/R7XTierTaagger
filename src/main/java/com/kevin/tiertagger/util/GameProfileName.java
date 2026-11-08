package com.kevin.tiertagger.util;

import com.mojang.authlib.GameProfile;

import java.lang.reflect.Method;

public final class GameProfileName {
    private GameProfileName() {
    }

    public static String get(GameProfile profile) {
        for (String accessorName : new String[]{"getName", "name"}) {
            try {
                return (String) profile.getClass().getMethod(accessorName).invoke(profile);
            } catch (ReflectiveOperationException ignored) {
                // Authlib changed GameProfile#getName() to GameProfile#name() in newer releases.
            }
        }
        throw new IllegalStateException("Could not read the player profile name");
    }
}
