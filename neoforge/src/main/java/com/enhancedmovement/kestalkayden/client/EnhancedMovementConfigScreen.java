package com.enhancedmovement.kestalkayden.client;

import java.util.Collections;
import java.util.List;

import com.enhancedmovement.kestalkayden.config.EnhancedMovementConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Hand-built vanilla config screen for Enhanced Movement — single-column, full-width layout.
 *
 * <p>Replaces the former Cloth AutoConfig screen, which throws on Minecraft 26.2 (Cloth has no 26.2
 * build). Mirrors the proven Veinminer++ screen: a custom {@link ContainerObjectSelectionList} that
 * places each option on its own full-width row, grouped under centred section headers.
 *
 * <h3>Layout (top to bottom)</h3>
 * <pre>
 *                Enhanced Movement Config        (title)
 *   ─────────────── Double Jump ───────────────  (header)
 *     Enable Double Jump               [on/off]
 *     Jump Power (%)                   [slider]
 *     Double Jump Delay (ms)           [slider]
 *     Enable Ledge Grab                [on/off]
 *   ─────────────────── Dash ───────────────────  (header)
 *     Enable Dash                      [on/off]
 *     Dash Cooldown (ms)               [slider]
 *     Use Single Keybind               [on/off]
 *     Allow Air Dash                   [on/off]
 *   ───────────────── Afterimage ───────────────  (header)
 *     Enable Afterimages               [on/off]
 *     Afterimage Count                 [slider]
 *     Fade Duration (tens of ms)       [slider]
 *     Prism Mode                       [on/off]
 *   ────────────────── General ─────────────────  (header)
 *     Disable While Sneaking           [on/off]
 *                     [ Done ]
 * </pre>
 *
 * <p>This class is identical on both loaders (same package + same config FQN). On Fabric it lives in
 * the {@code client} source set and is referenced from {@code ModMenuIntegration}; on NeoForge it
 * lives in {@code main} (Dist-guarded) and is referenced from the {@code IConfigScreenFactory}
 * registered in {@code EnhancedMovement}.
 */
public class EnhancedMovementConfigScreen extends Screen {

    private static final Component TITLE =
            Component.translatable("enhancedmovement.config.title");

    private final Screen parent;
    private @Nullable ConfigList list;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public EnhancedMovementConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(TITLE, font);
        list = layout.addToContents(new ConfigList(minecraft, width, this));
        layout.addToFooter(
                Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                      .width(200)
                      .build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (list != null) {
            list.updateSize(width, layout);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    /**
     * Persist on close — whether via Done, Escape, or any other navigation. The option listeners in
     * {@link ConfigList} have already written their values into the {@link EnhancedMovementConfig}
     * POJO; {@link EnhancedMovementConfig#save()} clamps and flushes to disk.
     */
    @Override
    public void removed() {
        EnhancedMovementConfig.save();
    }

    // =========================================================================
    // Custom scrolling list
    // =========================================================================

    static final class ConfigList
            extends ContainerObjectSelectionList<ConfigList.AbstractEntry> {

        private static final int ROW_WIDTH = 310;
        private static final int OPTION_ROW_HEIGHT = 25;
        private static final int HEADER_ROW_HEIGHT = 18;

        private final Screen screen;

        ConfigList(Minecraft minecraft, int screenWidth, Screen screen) {
            super(minecraft, screenWidth, 0, 0, OPTION_ROW_HEIGHT);
            this.centerListVertically = false;
            this.screen = screen;
            populateEntries();
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        private void populateEntries() {
            EnhancedMovementConfig cfg = EnhancedMovementConfig.get();

            // ---- Double Jump ----
            addHeader("enhancedmovement.config.section.doubleJump");
            addOption(OptionInstance.createBoolean(
                    "enhancedmovement.config.doubleJump.enabled",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.doubleJump.enabled.tooltip")),
                    cfg.movement.doubleJump.enabled,
                    val -> EnhancedMovementConfig.get().movement.doubleJump.enabled = val));
            addOption(new OptionInstance<>(
                    "enhancedmovement.config.doubleJump.jumpBoostPercent",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.doubleJump.jumpBoostPercent.tooltip")),
                    (caption, val) -> Component.translatable("options.generic_value", caption, Component.literal(String.valueOf(val))),
                    new OptionInstance.IntRange(20, 60),
                    cfg.movement.doubleJump.jumpBoostPercent,
                    val -> EnhancedMovementConfig.get().movement.doubleJump.jumpBoostPercent = val));
            addOption(new OptionInstance<>(
                    "enhancedmovement.config.doubleJump.delay",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.doubleJump.delay.tooltip")),
                    (caption, val) -> Component.translatable("options.generic_value", caption, Component.literal(String.valueOf(val))),
                    new OptionInstance.IntRange(100, 500),
                    cfg.movement.doubleJump.delayBeforeDoubleJumpMs,
                    val -> EnhancedMovementConfig.get().movement.doubleJump.delayBeforeDoubleJumpMs = val));
            addOption(OptionInstance.createBoolean(
                    "enhancedmovement.config.doubleJump.ledgeGrab",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.doubleJump.ledgeGrab.tooltip")),
                    cfg.movement.doubleJump.enableLedgeGrab,
                    val -> EnhancedMovementConfig.get().movement.doubleJump.enableLedgeGrab = val));

            // ---- Dash ----
            addHeader("enhancedmovement.config.section.dash");
            addOption(OptionInstance.createBoolean(
                    "enhancedmovement.config.dash.enabled",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.dash.enabled.tooltip")),
                    cfg.movement.dash.enabled,
                    val -> EnhancedMovementConfig.get().movement.dash.enabled = val));
            addOption(new OptionInstance<>(
                    "enhancedmovement.config.dash.cooldown",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.dash.cooldown.tooltip")),
                    (caption, val) -> Component.translatable("options.generic_value", caption, Component.literal(String.valueOf(val))),
                    new OptionInstance.IntRange(100, 5000),
                    cfg.movement.dash.cooldownMs,
                    val -> EnhancedMovementConfig.get().movement.dash.cooldownMs = val));
            addOption(OptionInstance.createBoolean(
                    "enhancedmovement.config.dash.useKeybinds",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.dash.useKeybinds.tooltip")),
                    cfg.movement.dash.useKeybinds,
                    val -> EnhancedMovementConfig.get().movement.dash.useKeybinds = val));
            addOption(OptionInstance.createBoolean(
                    "enhancedmovement.config.dash.airDash",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.dash.airDash.tooltip")),
                    cfg.movement.dash.enableAirDash,
                    val -> EnhancedMovementConfig.get().movement.dash.enableAirDash = val));

            // ---- Afterimage ----
            addHeader("enhancedmovement.config.section.afterimage");
            addOption(OptionInstance.createBoolean(
                    "enhancedmovement.config.afterimage.enabled",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.afterimage.enabled.tooltip")),
                    cfg.movement.dash.afterimage.enabled,
                    val -> EnhancedMovementConfig.get().movement.dash.afterimage.enabled = val));
            addOption(new OptionInstance<>(
                    "enhancedmovement.config.afterimage.imageCount",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.afterimage.imageCount.tooltip")),
                    (caption, val) -> Component.translatable("options.generic_value", caption, Component.literal(String.valueOf(val))),
                    new OptionInstance.IntRange(6, 30),
                    cfg.movement.dash.afterimage.imageCount,
                    val -> EnhancedMovementConfig.get().movement.dash.afterimage.imageCount = val));
            addOption(new OptionInstance<>(
                    "enhancedmovement.config.afterimage.lifetime",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.afterimage.lifetime.tooltip")),
                    (caption, val) -> Component.translatable("options.generic_value", caption, Component.literal(String.valueOf(val))),
                    new OptionInstance.IntRange(30, 200),
                    cfg.movement.dash.afterimage.baseLifetimeMs,
                    val -> EnhancedMovementConfig.get().movement.dash.afterimage.baseLifetimeMs = val));
            addOption(OptionInstance.createBoolean(
                    "enhancedmovement.config.afterimage.prismMode",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.afterimage.prismMode.tooltip")),
                    cfg.movement.dash.afterimage.prismMode,
                    val -> EnhancedMovementConfig.get().movement.dash.afterimage.prismMode = val));

            // ---- General ----
            addHeader("enhancedmovement.config.section.general");
            addOption(OptionInstance.createBoolean(
                    "enhancedmovement.config.general.sneakDisables",
                    OptionInstance.cachedConstantTooltip(
                            Component.translatable("enhancedmovement.config.general.sneakDisables.tooltip")),
                    cfg.movement.general.sneakDisablesFeatures,
                    val -> EnhancedMovementConfig.get().movement.general.sneakDisablesFeatures = val));
        }

        private void addHeader(String langKey) {
            addEntry(new HeaderEntry(Component.translatable(langKey), minecraft), HEADER_ROW_HEIGHT);
        }

        private void addOption(OptionInstance<?> option) {
            addEntry(new OptionEntry(option, minecraft, screen), OPTION_ROW_HEIGHT);
        }

        // =====================================================================
        // Entry types
        // =====================================================================

        abstract static class AbstractEntry
                extends ContainerObjectSelectionList.Entry<AbstractEntry> {
        }

        static final class HeaderEntry extends AbstractEntry {

            private final StringWidget widget;

            HeaderEntry(Component title, Minecraft minecraft) {
                this.widget = new StringWidget(ROW_WIDTH, 9, title, minecraft.font);
            }

            @Override
            public void extractContent(
                    GuiGraphicsExtractor graphics,
                    int mouseX, int mouseY,
                    boolean hovered,
                    float a) {
                int textWidth = this.widget.getWidth();
                int centreX = this.getContentXMiddle() - textWidth / 2;
                int centreY = this.getContentY() + (this.getContentHeight() - 9) / 2;
                this.widget.setPosition(centreX, centreY);
                this.widget.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(widget);
            }
        }

        static final class OptionEntry extends AbstractEntry {

            private final AbstractWidget widget;
            private final Screen screen;

            OptionEntry(OptionInstance<?> option, Minecraft minecraft, Screen screen) {
                this.screen = screen;
                this.widget = option.createButton(minecraft.options, 0, 0, ROW_WIDTH);
            }

            @Override
            public void extractContent(
                    GuiGraphicsExtractor graphics,
                    int mouseX, int mouseY,
                    boolean hovered,
                    float a) {
                int widgetX = this.getContentXMiddle() - this.widget.getWidth() / 2;
                this.widget.setPosition(widgetX, this.getContentY());
                this.widget.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(widget);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(widget);
            }
        }
    }
}
