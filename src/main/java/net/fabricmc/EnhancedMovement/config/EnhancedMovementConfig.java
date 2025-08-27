package net.fabricmc.EnhancedMovement.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "enhancedmovement")
public class EnhancedMovementConfig implements ConfigData {
    
    @ConfigEntry.Category("movement")
    @ConfigEntry.Gui.TransitiveObject
    public MovementConfig movement = new MovementConfig();

    public static class MovementConfig {
        
        @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
        public DoubleJumpConfig doubleJump = new DoubleJumpConfig();
        
        @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
        public DashConfig dash = new DashConfig();
        
        @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
        public GeneralConfig general = new GeneralConfig();
    }
    
    public static class DoubleJumpConfig {
        
        @ConfigEntry.Gui.Tooltip(count = 1)
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public boolean enabled = true;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 20, max = 60)
        public int jumpBoostPercent = 40;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 100, max = 500)
        public int delayBeforeDoubleJumpMs = 250;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean enableLedgeGrab = true;
    }
    
    public static class DashConfig {
        
        @ConfigEntry.Gui.Tooltip(count = 1)
        public boolean enabled = true;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
        public int cooldownSeconds = 1;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean useKeybinds = false;
        
        @ConfigEntry.Gui.Tooltip(count = 1)
        public boolean enableAirDash = true;
        
        @ConfigEntry.Gui.CollapsibleObject
        public AfterimageConfig afterimage = new AfterimageConfig();
    }
    
    public static class AfterimageConfig {
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean enabled = true;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 6, max = 25)
        public int imageCount = 16;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 30, max = 150)
        public int baseLifetimeMs = 80;
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean prismMode = false;
    }
    
    public static class GeneralConfig {
        
        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean sneakDisablesFeatures = false;
    }
}