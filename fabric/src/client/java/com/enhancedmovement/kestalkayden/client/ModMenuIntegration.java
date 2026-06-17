package com.enhancedmovement.kestalkayden.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu integration — supplies the "Config" button on the Fabric mod list screen.
 *
 * <p>Client-only (references {@link EnhancedMovementConfigScreen}, which imports {@code Screen}),
 * registered under the {@code "modmenu"} entrypoint in {@code fabric.mod.json}.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new EnhancedMovementConfigScreen(parent);
    }
}
