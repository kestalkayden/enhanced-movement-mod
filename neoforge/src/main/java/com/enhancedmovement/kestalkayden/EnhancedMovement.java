package com.enhancedmovement.kestalkayden;

import com.enhancedmovement.kestalkayden.client.EnhancedMovementClient;
import com.enhancedmovement.kestalkayden.config.EnhancedMovementConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(EnhancedMovement.MOD_ID)
public class EnhancedMovement {

    public static final String MOD_ID = "enhancedmovement";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static EnhancedMovementConfig CONFIG;

    public EnhancedMovement(ModContainer container, IEventBus modBus) {
        AutoConfig.register(EnhancedMovementConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(EnhancedMovementConfig.class).getConfig();

        modBus.addListener(NetworkHandler::onRegisterPayloadHandlers);

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> AutoConfigClient.getConfigScreen(EnhancedMovementConfig.class, parent).get());
            EnhancedMovementClient.register(modBus);
        }

        LOGGER.info("Enhanced Movement NeoForge mod loaded!");
    }
}
