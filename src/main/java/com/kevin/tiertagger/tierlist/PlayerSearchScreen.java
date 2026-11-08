package com.kevin.tiertagger.tierlist;

import com.kevin.tiertagger.TierCache;
import com.kevin.tiertagger.TierTagger;
import com.kevin.tiertagger.util.ApiServicesProvider;
import com.kevin.tiertagger.util.PlayerSkinWidgetFactory;
import com.kevin.tiertagger.util.ProfileLookup;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PlayerSkinWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.ApiServices;
import net.uku3lig.ukulib.config.option.widget.TextInputWidget;
import net.uku3lig.ukulib.config.screen.CloseableScreen;
import net.uku3lig.ukulib.utils.Ukutils;

import java.util.UUID;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PlayerSearchScreen extends CloseableScreen {
    private TextInputWidget textField;
    private ButtonWidget searchButton;

    private boolean searching = false;
    private CompletableFuture<?> future = null;

    public PlayerSearchScreen(Screen parent) {
        super(Text.translatable("tiertagger.search.title"), parent);
    }

    @Override
    protected void init() {
        String username = I18n.translate("tiertagger.search.user");
        this.textField = this.addSelectableChild(new TextInputWidget(this.width / 2 - 100, 116, 200, 20,
                "", s -> {
        }, username, s -> s.matches("[a-zA-Z0-9_-]+"), 32));

        this.searchButton = this.addDrawableChild(
                ButtonWidget.builder(Text.translatable("tiertagger.search"), button -> this.loadAndShowProfile())
                        .dimensions(this.width / 2 - 100, this.height / 4 + 96 + 12, 200, 20)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(ScreenTexts.CANCEL, button -> {
                            if (this.future != null) {
                                this.future.cancel(true);
                            }
                            this.close();
                        })
                        .dimensions(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20)
                        .build()
        );

        this.setInitialFocus(this.textField);
    }

    @Override
    public void tick() {
        super.tick();
        this.searchButton.active = this.textField.isValid() && !searching;
    }

    private void loadAndShowProfile() {
        String username = this.textField.getText();
        this.searching = true;
        this.searchButton.setMessage(Text.translatable("tiertagger.search.loading"));

        MinecraftClient client = MinecraftClient.getInstance();
        ApiServices services = null;
        try {
            services = ApiServicesProvider.get(client);
        } catch (RuntimeException exception) {
            TierTagger.getLogger().warn("Could not initialize Minecraft profile lookup; opening the tier profile without a skin", exception);
        }

        ApiServices profileServices = services;
        CompletableFuture<PlayerSkinWidget> skinFuture = CompletableFuture
                .supplyAsync(() -> findProfile(profileServices, username))
                .thenCompose(profile -> onClientThread(client, () -> {
                    Supplier<?> supplier = PlayerSkinWidgetFactory.getSkinTexturesSupplier(profile);
                    PlayerSkinWidget skin = PlayerSkinWidgetFactory.create(60, 144, supplier);
                    skin.setPosition(this.width / 2 - 65, (this.height - 144) / 2);
                    return skin;
                }))
                .completeOnTimeout(null, 4, TimeUnit.SECONDS)
                .exceptionally(error -> {
                    TierTagger.getLogger().warn("Could not load the Minecraft skin preview; opening the tier profile without a skin", unwrap(error));
                    return null;
                });

        this.future = TierCache.searchPlayer(username)
                .thenCompose(info -> skinFuture.thenCompose(skin ->
                        onClientThread(client, () -> new PlayerInfoScreen(this, info, skin))))
                .whenComplete((screen, error) -> client.execute(() -> {
                    Throwable cause = error == null ? null : unwrap(error);
                    if (cause == null) {
                        if (client.currentScreen == this) client.setScreen(screen);
                    } else if (!(cause instanceof CancellationException) && client.currentScreen == this) {
                        Text message = cause instanceof NoSuchElementException
                                ? Text.translatable("tiertagger.search.unknown")
                                : Text.translatable("tiertagger.search.failed");
                        Ukutils.sendToast(message, null);
                    }

                    this.searching = false;
                    if (client.currentScreen == this && this.searchButton != null) {
                        this.searchButton.setMessage(Text.translatable("tiertagger.search"));
                    }
                }));
    }

    private static GameProfile findProfile(ApiServices services, String username) {
        if (services != null) {
            try {
                return ProfileLookup.find(services, username).orElseGet(() -> offlineProfile(username));
            } catch (RuntimeException exception) {
                TierTagger.getLogger().debug("Minecraft profile lookup failed for {}; using an offline profile for the skin preview", username, exception);
            }
        }
        return offlineProfile(username);
    }

    private static GameProfile offlineProfile(String username) {
        UUID id = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
        return new GameProfile(id, username);
    }

    private static <T> CompletableFuture<T> onClientThread(MinecraftClient client, Supplier<T> task) {
        CompletableFuture<T> result = new CompletableFuture<>();
        client.execute(() -> {
            try {
                result.complete(task.get());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result;
    }

    private static Throwable unwrap(Throwable error) {
        while ((error instanceof CompletionException || error instanceof java.util.concurrent.ExecutionException)
                && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    public void resize(MinecraftClient client, int width, int height) {
        resizeScreen(client, width, height);
    }

    public void resize(int width, int height) {
        resizeScreen(MinecraftClient.getInstance(), width, height);
    }

    private void resizeScreen(MinecraftClient client, int width, int height) {
        String string = this.textField.getText();
        Method init = findInitMethod();
        try {
            init.setAccessible(true);
            if (init.getParameterCount() == 3) init.invoke(this, client, width, height);
            else init.invoke(this, width, height);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not resize the player search screen", exception);
        }
        this.textField.setText(string);
    }

    private static Method findInitMethod() {
        for (Method method : Screen.class.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getReturnType() != void.class) continue;
            if (parameters.length == 3 && parameters[0] == MinecraftClient.class
                    && parameters[1] == int.class && parameters[2] == int.class) return method;
            if (parameters.length == 2 && parameters[0] == int.class && parameters[1] == int.class) return method;
        }
        throw new IllegalStateException("Could not find a compatible screen initialization method");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 16777215);
        this.textField.render(context, mouseX, mouseY, delta);
    }
}
