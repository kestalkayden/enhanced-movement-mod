package com.enhancedmovement.kestalkayden;

import com.enhancedmovement.kestalkayden.client.EnhancedMovementClient;
import com.enhancedmovement.kestalkayden.config.EnhancedMovementConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(EnhancedMovement.MOD_ID)
public class EnhancedMovement {

    public static final String MOD_ID = "enhancedmovement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static EnhancedMovementConfig CONFIG;

    public EnhancedMovement(ModContainer container, IEventBus modBus) {
        CONFIG = EnhancedMovementConfig.load();

        modBus.addListener(NetworkHandler::onRegisterPayloadHandlers);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            // All client wiring (keybinds, renderers, config screen) lives in
            // EnhancedMovementClient. Reaching it only via this guarded invokestatic keeps the
            // dedicated server from loading or verifying any client class — see that class's note.
            EnhancedMovementClient.register(modBus, container);
        }

        LOGGER.info("Enhanced Movement NeoForge mod loaded!");
    }
}
