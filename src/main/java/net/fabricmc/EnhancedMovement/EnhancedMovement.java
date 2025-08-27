package net.fabricmc.EnhancedMovement;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.EnhancedMovement.config.EnhancedMovementConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class EnhancedMovement implements ModInitializer {

    public static final String MOD_ID = "enhancedmovement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static EnhancedMovement instance;
    public static EnhancedMovementConfig CONFIG;

    @Override
    public void onInitialize() {
        instance = this;
        
        // Initialize config
        AutoConfig.register(EnhancedMovementConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(EnhancedMovementConfig.class).getConfig();

        // Initialize networking
        NetworkHandler.initialize();

        // Handle player joining server - sync config if needed
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Server-side player join logic can be added here if needed
            LOGGER.info("Player {} joined with Enhanced Movement mod", handler.player.getName().getString());
        });

        LOGGER.info("Enhanced Movement mod initialized!");
    }



    public static EnhancedMovement getInstance() {
        return instance;
    }
}
