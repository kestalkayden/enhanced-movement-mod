package com.enhancedmovement.kestalkayden;

import com.enhancedmovement.kestalkayden.config.EnhancedMovementConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnhancedMovement implements ModInitializer {

    public static final String MOD_ID = "enhancedmovement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static EnhancedMovement instance;
    public static EnhancedMovementConfig CONFIG;

    @Override
    public void onInitialize() {
        instance = this;

        CONFIG = EnhancedMovementConfig.load();

        NetworkHandler.initialize();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            LOGGER.info("Player {} joined with Enhanced Movement mod", handler.player.getName().getString())
        );

        LOGGER.info("Enhanced Movement mod initialized!");
    }

    public static EnhancedMovement getInstance() {
        return instance;
    }
}
