package com.enhancedmovement.kestalkayden.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gson-backed config for Enhanced Movement (NeoForge).
 *
 * <p>Replaces the former Cloth AutoConfig POJO. Cloth Config has no Minecraft 26.2 build, so its
 * in-game screen threw on open (from the mods-list Config button on NeoForge, from ModMenu on
 * Fabric). Storage is now plain Gson at {@code config/enhancedmovement.json} — the SAME path and
 * nested field shape Cloth's {@code GsonConfigSerializer} already used, so existing user configs
 * carry over unchanged. The in-game screen is the hand-built
 * {@link com.enhancedmovement.kestalkayden.client.EnhancedMovementConfigScreen}.
 *
 * <p>This is the NeoForge copy; Fabric keeps a byte-identical copy that differs only in the
 * config-directory lookup (the one loader-specific line below).
 */
public class EnhancedMovementConfig {

    public MovementConfig movement = new MovementConfig();

    public static class MovementConfig {
        public DoubleJumpConfig doubleJump = new DoubleJumpConfig();
        public DashConfig dash = new DashConfig();
        public GeneralConfig general = new GeneralConfig();
    }

    public static class DoubleJumpConfig {
        public boolean enabled = true;
        /** Strength of the double-jump boost, percent. Range: 20-60. */
        public int jumpBoostPercent = 40;
        /** Delay after the initial jump before a double jump is allowed, ms. Range: 100-500. */
        public int delayBeforeDoubleJumpMs = 250;
        public boolean enableLedgeGrab = true;
    }

    public static class DashConfig {
        public boolean enabled = true;
        /** Time between dashes, ms. Range: 100-5000. */
        public int cooldownMs = 1000;
        public boolean useKeybinds = false;
        public boolean enableAirDash = true;
        public AfterimageConfig afterimage = new AfterimageConfig();
    }

    public static class AfterimageConfig {
        public boolean enabled = true;
        /** Number of afterimages along the dash path. Range: 6-30. */
        public int imageCount = 20;
        /** Base fade time, in tens of milliseconds. Range: 30-200. */
        public int baseLifetimeMs = 120;
        public boolean prismMode = false;
    }

    public static class GeneralConfig {
        public boolean sneakDisablesFeatures = false;
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Loader-specific config-directory seam — the only line that differs from the Fabric copy. */
    private static final Path PATH =
            FMLPaths.CONFIGDIR.get().resolve("enhancedmovement.json");

    /** The live singleton, populated by {@link #load()}. */
    private static EnhancedMovementConfig instance;

    /** The live config instance, lazily loaded. Mutate fields directly, then call {@link #save()}. */
    public static EnhancedMovementConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    /**
     * Load (or create) {@code config/enhancedmovement.json}. A missing or unreadable file falls
     * back to defaults, which are then written so the file is self-documenting on first run.
     * Always leaves {@link #instance} non-null with all nested objects present.
     */
    public static EnhancedMovementConfig load() {
        EnhancedMovementConfig cfg = new EnhancedMovementConfig();
        if (Files.exists(PATH)) {
            try (Reader r = Files.newBufferedReader(PATH)) {
                EnhancedMovementConfig loaded = GSON.fromJson(r, EnhancedMovementConfig.class);
                if (loaded != null) {
                    cfg = loaded;
                }
            } catch (IOException | RuntimeException e) {
                // Keep defaults; the broken file is overwritten by the save() below.
            }
        }
        cfg.fillMissing();
        instance = cfg;
        clamp();
        save();
        return instance;
    }

    /** Clamp bounded fields to their documented ranges, then persist to disk. */
    public static void save() {
        if (instance == null) {
            return;
        }
        clamp();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) {
                GSON.toJson(instance, w);
            }
        } catch (IOException e) {
            // Non-fatal: the in-memory state is already correct.
        }
    }

    /** Replace any null nested object (e.g. a partial/old JSON file) with its default. */
    private void fillMissing() {
        if (movement == null) movement = new MovementConfig();
        if (movement.doubleJump == null) movement.doubleJump = new DoubleJumpConfig();
        if (movement.dash == null) movement.dash = new DashConfig();
        if (movement.dash.afterimage == null) movement.dash.afterimage = new AfterimageConfig();
        if (movement.general == null) movement.general = new GeneralConfig();
    }

    private static void clamp() {
        DoubleJumpConfig dj = instance.movement.doubleJump;
        dj.jumpBoostPercent = clampInt(dj.jumpBoostPercent, 20, 60);
        dj.delayBeforeDoubleJumpMs = clampInt(dj.delayBeforeDoubleJumpMs, 100, 500);

        DashConfig d = instance.movement.dash;
        d.cooldownMs = clampInt(d.cooldownMs, 100, 5000);

        AfterimageConfig a = d.afterimage;
        a.imageCount = clampInt(a.imageCount, 6, 30);
        a.baseLifetimeMs = clampInt(a.baseLifetimeMs, 30, 200);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
