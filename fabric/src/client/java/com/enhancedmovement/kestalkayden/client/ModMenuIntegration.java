package com.enhancedmovement.kestalkayden.client;

import com.enhancedmovement.kestalkayden.config.EnhancedMovementConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(EnhancedMovementConfig.class, parent).get();
    }
}
