package com.kevin.tiertagger.util;

import net.fabricmc.loader.api.FabricLoader;

public final class MinecraftVersionName {
    private MinecraftVersionName() {
    }

    public static String get() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .orElseThrow(() -> new IllegalStateException("Could not read the Minecraft mod metadata"))
                .getMetadata().getVersion().getFriendlyString();
    }
}
