package com.enhancedmovement.kestalkayden.client;

import com.enhancedmovement.kestalkayden.config.EnhancedMovementConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Native vanilla config screen for Enhanced Movement (1.21.8).
 *
 * <p>Replaces the former Cloth AutoConfig screen — Cloth Config's {@code @OnlyIn} usage trips
 * NeoForge 1.21.8's mod-loading warning gate, so we no longer depend on it. This is a stock
 * {@link OptionsSubScreen} hosting the same options the 26.x branch uses, each backed by the Gson
 * {@link EnhancedMovementConfig}. The option listeners write values into the POJO live;
 * {@link EnhancedMovementConfig#save()} clamps and flushes to disk on close.
 *
 * <p>Identical on both loaders. On Fabric it lives in the {@code client} source set and is opened
 * from {@code ModMenuIntegration}; on NeoForge it lives in {@code main} (Dist-guarded) and is opened
 * from the {@code IConfigScreenFactory} registered in {@code EnhancedMovementClient}.
 */
public class EnhancedMovementConfigScreen extends OptionsSubScreen {

    private static final Component TITLE =
            Component.translatable("enhancedmovement.config.title");

    public EnhancedMovementConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, TITLE);
    }

    @Override
    protected void addOptions() {
        EnhancedMovementConfig cfg = EnhancedMovementConfig.get();
        this.list.addSmall(
                // Double Jump
                bool("enhancedmovement.config.doubleJump.enabled", cfg.movement.doubleJump.enabled,
                        v -> EnhancedMovementConfig.get().movement.doubleJump.enabled = v),
                slider("enhancedmovement.config.doubleJump.jumpBoostPercent", 20, 60, cfg.movement.doubleJump.jumpBoostPercent,
                        v -> EnhancedMovementConfig.get().movement.doubleJump.jumpBoostPercent = v),
                slider("enhancedmovement.config.doubleJump.delay", 100, 500, cfg.movement.doubleJump.delayBeforeDoubleJumpMs,
                        v -> EnhancedMovementConfig.get().movement.doubleJump.delayBeforeDoubleJumpMs = v),
                bool("enhancedmovement.config.doubleJump.ledgeGrab", cfg.movement.doubleJump.enableLedgeGrab,
                        v -> EnhancedMovementConfig.get().movement.doubleJump.enableLedgeGrab = v),
                // Dash
                bool("enhancedmovement.config.dash.enabled", cfg.movement.dash.enabled,
                        v -> EnhancedMovementConfig.get().movement.dash.enabled = v),
                slider("enhancedmovement.config.dash.cooldown", 100, 5000, cfg.movement.dash.cooldownMs,
                        v -> EnhancedMovementConfig.get().movement.dash.cooldownMs = v),
                bool("enhancedmovement.config.dash.useKeybinds", cfg.movement.dash.useKeybinds,
                        v -> EnhancedMovementConfig.get().movement.dash.useKeybinds = v),
                bool("enhancedmovement.config.dash.airDash", cfg.movement.dash.enableAirDash,
                        v -> EnhancedMovementConfig.get().movement.dash.enableAirDash = v),
                // Afterimage
                bool("enhancedmovement.config.afterimage.enabled", cfg.movement.dash.afterimage.enabled,
                        v -> EnhancedMovementConfig.get().movement.dash.afterimage.enabled = v),
                slider("enhancedmovement.config.afterimage.imageCount", 6, 30, cfg.movement.dash.afterimage.imageCount,
                        v -> EnhancedMovementConfig.get().movement.dash.afterimage.imageCount = v),
                slider("enhancedmovement.config.afterimage.lifetime", 30, 200, cfg.movement.dash.afterimage.baseLifetimeMs,
                        v -> EnhancedMovementConfig.get().movement.dash.afterimage.baseLifetimeMs = v),
                bool("enhancedmovement.config.afterimage.prismMode", cfg.movement.dash.afterimage.prismMode,
                        v -> EnhancedMovementConfig.get().movement.dash.afterimage.prismMode = v),
                // General
                bool("enhancedmovement.config.general.sneakDisables", cfg.movement.general.sneakDisablesFeatures,
                        v -> EnhancedMovementConfig.get().movement.general.sneakDisablesFeatures = v));
    }

    /** Persist on close — via Done, Escape, or any other navigation. */
    @Override
    public void removed() {
        super.removed();
        EnhancedMovementConfig.save();
    }

    // -------------------------------------------------------------------------

    private static OptionInstance<Boolean> bool(String key, boolean initial, Consumer<Boolean> setter) {
        return OptionInstance.createBoolean(
                key,
                OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip")),
                initial,
                setter);
    }

    private static OptionInstance<Integer> slider(String key, int min, int max, int initial, IntConsumer setter) {
        return new OptionInstance<>(
                key,
                OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip")),
                (caption, val) -> Component.translatable("options.generic_value", caption, Component.literal(String.valueOf(val))),
                new OptionInstance.IntRange(min, max),
                initial,
                val -> setter.accept(val));
    }
}
