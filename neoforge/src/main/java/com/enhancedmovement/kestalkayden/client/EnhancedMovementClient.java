package com.enhancedmovement.kestalkayden.client;

import com.enhancedmovement.kestalkayden.EnhancedMovement;
import com.enhancedmovement.kestalkayden.NetworkHandler;
import com.enhancedmovement.kestalkayden.config.EnhancedMovementConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class EnhancedMovementClient {

    public static final Logger LOGGER = LoggerFactory.getLogger("enhancedmovement-client");

    public static final KeyMapping DASH_KEY = new KeyMapping(
        "key.enhancedmovement.dash",
        GLFW.GLFW_KEY_UNKNOWN,
        "key.category.enhancedmovement.main"
    );

    private static final EnhancedMovementClient INSTANCE = new EnhancedMovementClient();
    public static LedgeGrab ledgeGrab = new LedgeGrab();

    private boolean jumpKeyPressed = false;
    private boolean jumpKeyReleased = false;
    private boolean midAirJumpPerformed = false;
    private boolean isInAir = false;
    private boolean isJumping = false;
    private long jumpStartTime = 0;

    private boolean forwardPressed = false;
    private boolean backPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private boolean isDashTracking = false;
    private long dashTrackingStartTime = 0;
    private Vec3 dashStartPosition = null;
    private UUID dashingPlayerId = null;
    private float dashYaw = 0;
    private float dashPitch = 0;
    private int afterimagesSpawned = 0;
    private Vec3 lastPlayerPosition = null;
    private long lastMovementTime = 0;

    private final AtomicLong forwardPressTime = new AtomicLong(0);
    private final AtomicLong backPressTime = new AtomicLong(0);
    private final AtomicLong leftPressTime = new AtomicLong(0);
    private final AtomicLong rightPressTime = new AtomicLong(0);

    private final AtomicLong globalCooldownTime = new AtomicLong(0);
    private final AtomicLong forwardCooldownTime = new AtomicLong(0);
    private final AtomicLong backCooldownTime = new AtomicLong(0);
    private final AtomicLong leftCooldownTime = new AtomicLong(0);
    private final AtomicLong rightCooldownTime = new AtomicLong(0);

    private final AtomicBoolean forwardKeyReleased = new AtomicBoolean(false);
    private final AtomicBoolean backKeyReleased = new AtomicBoolean(false);
    private final AtomicBoolean leftKeyReleased = new AtomicBoolean(false);
    private final AtomicBoolean rightKeyReleased = new AtomicBoolean(false);

    private final AtomicBoolean forwardPressHandled = new AtomicBoolean(false);
    private final AtomicBoolean backPressHandled = new AtomicBoolean(false);
    private final AtomicBoolean leftPressHandled = new AtomicBoolean(false);
    private final AtomicBoolean rightPressHandled = new AtomicBoolean(false);

    public static void register(IEventBus modBus, ModContainer container) {
        LOGGER.info("Enhanced Movement client registering.");
        modBus.addListener(EnhancedMovementClient::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.register(EnhancedMovementClient.class);

        // Config button on the Mods screen. Registered here rather than on the @Mod class so the
        // dedicated server never verifies the Screen-returning lambda: the JVM verifies every
        // method of a class at link time, and @Mod is linked during constructMods, which would
        // force-load vanilla client classes the server strips. This class is only reached via a
        // guarded invokestatic, so the server never loads it.
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mod, parent) -> AutoConfig.getConfigScreen(EnhancedMovementConfig.class, parent).get());

        NetworkHandler.clientAfterimageReceiver = payload -> {
            EnhancedMovementConfig config = EnhancedMovement.CONFIG;
            if (config.movement.dash.afterimage.enabled) {
                Vec3 position = new Vec3(payload.startX(), payload.startY(), payload.startZ());
                AfterimageManager.addSingleAfterimage(
                    payload.playerId(), position,
                    payload.yaw(), payload.pitch(),
                    config.movement.dash.afterimage.baseLifetimeMs,
                    0L
                );
            }
        };
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DASH_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft c = Minecraft.getInstance();
        if (c.player == null) return;
        EnhancedMovementConfig config = EnhancedMovement.CONFIG;

        boolean isSneaking = c.options.keyShift.isDown();
        if (isSneaking && config.movement.general.sneakDisablesFeatures) return;

        if (config.movement.dash.enabled) {
            if (config.movement.dash.useKeybinds) {
                INSTANCE.handleKeybindDash(c, config);
            } else {
                INSTANCE.handleDashInput(c, config);
            }
        }

        if (config.movement.doubleJump.enabled) {
            INSTANCE.handleDoubleJumpInput(c, config);
        }

        if (config.movement.doubleJump.enableLedgeGrab) {
            ledgeGrab.tick();
        }

        AfterimageManager.tick();
        INSTANCE.handleDashTracking(c, config);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        EnhancedMovementConfig config = EnhancedMovement.CONFIG;
        if (!config.movement.dash.afterimage.enabled) return;

        Minecraft c = Minecraft.getInstance();
        var matrices = event.getPoseStack();
        var bufferSource = c.renderBuffers().bufferSource();
        int light = 15728880;

        for (AfterimageManager.AfterimageData afterimage : AfterimageManager.getActiveAfterimages()) {
            if (afterimage.getOpacity() > 0.0f) {
                Vec3 cameraPos = c.gameRenderer.getMainCamera().getPosition();
                double distanceToCamera = afterimage.position.distanceTo(cameraPos);
                boolean isFirstPerson = c.options.getCameraType().isFirstPerson();
                if (!isFirstPerson || distanceToCamera > 1.5) {
                    AfterimageRenderer.renderAfterimage(matrices, bufferSource, afterimage, light);
                }
            }
        }
    }

    private void handleDashInput(Minecraft c, EnhancedMovementConfig config) {
        KeyMapping forwardKey = c.options.keyUp;
        KeyMapping backKey = c.options.keyDown;
        KeyMapping leftKey = c.options.keyLeft;
        KeyMapping rightKey = c.options.keyRight;

        forwardPressed = forwardKey.isDown();
        leftPressed = leftKey.isDown();
        rightPressed = rightKey.isDown();
        backPressed = backKey.isDown();

        handleDash(c, forwardKey, forwardPressed, forwardPressTime, forwardCooldownTime, forwardKeyReleased, forwardPressHandled, config);
        handleDash(c, backKey, backPressed, backPressTime, backCooldownTime, backKeyReleased, backPressHandled, config);
        handleDash(c, leftKey, leftPressed, leftPressTime, leftCooldownTime, leftKeyReleased, leftPressHandled, config);
        handleDash(c, rightKey, rightPressed, rightPressTime, rightCooldownTime, rightKeyReleased, rightPressHandled, config);
    }

    private void handleKeybindDash(Minecraft c, EnhancedMovementConfig config) {
        if (c.player == null) return;
        if (c.screen != null) return;

        if (DASH_KEY.consumeClick()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - globalCooldownTime.get() <= config.movement.dash.cooldownMs && globalCooldownTime.get() != 0) {
                return;
            }
            String direction = detectMovementDirection(c);
            if (direction != null) {
                performDashInDirection(c, direction, config);
                globalCooldownTime.set(currentTime);
            }
        }
    }

    private String detectMovementDirection(Minecraft c) {
        boolean forward = c.options.keyUp.isDown();
        boolean back = c.options.keyDown.isDown();
        boolean left = c.options.keyLeft.isDown();
        boolean right = c.options.keyRight.isDown();

        if (forward && !back && !left && !right) return "forward";
        if (back && !forward && !left && !right) return "back";
        if (left && !forward && !back && !right) return "left";
        if (right && !forward && !back && !left) return "right";

        if (forward && left) return "forward";
        if (forward && right) return "forward";
        if (back && left) return "back";
        if (back && right) return "back";

        return "forward";
    }

    private void performDashInDirection(Minecraft c, String direction, EnhancedMovementConfig config) {
        if (isInAir && !config.movement.dash.enableAirDash) return;
        if (c.player == null) return;
        if (c.player.getFoodData().getFoodLevel() < 6) return;

        KeyMapping key;
        switch (direction) {
            case "forward": key = c.options.keyUp; break;
            case "back": key = c.options.keyDown; break;
            case "left": key = c.options.keyLeft; break;
            case "right": key = c.options.keyRight; break;
            default: return;
        }
        performDash(c, key, config);
    }

    private void handleDoubleJumpInput(Minecraft c, EnhancedMovementConfig config) {
        if (c.player == null) return;
        boolean onGround = c.player.onGround();
        jumpKeyPressed = c.options.keyJump.isDown();

        if (jumpKeyPressed && !isInAir) {
            jumpStartTime = System.currentTimeMillis();
            isInAir = true;
            isJumping = true;
        }

        if (isJumping && isInAir) {
            long timeSinceJumpStart = System.currentTimeMillis() - jumpStartTime;
            if (timeSinceJumpStart >= config.movement.doubleJump.delayBeforeDoubleJumpMs
                && jumpKeyPressed && jumpKeyReleased && !midAirJumpPerformed && !onGround) {
                if (c.player.getFoodData().getFoodLevel() >= 6) {
                    double firstJumpY = c.player.getY() - (timeSinceJumpStart / 1000.0 * 0.08);
                    double doubleJumpY = c.player.getY();
                    performMidAirJump(c.player, config);
                    ClientNetworkHandler.sendDoubleJumpData(firstJumpY, doubleJumpY);
                    c.player.causeFoodExhaustion(0.2f);
                    midAirJumpPerformed = true;
                }
            }

            if (!jumpKeyPressed) {
                jumpKeyReleased = true;
            }
        }

        if (isInAir && jumpKeyReleased && midAirJumpPerformed) {
            c.player.fallDistance = 0.0f;
        }

        if (onGround) {
            resetJumpState();
        }
    }

    private void handleDash(Minecraft c, KeyMapping key, boolean isPressed, AtomicLong pressTime, AtomicLong cooldownTime,
                            AtomicBoolean keyReleased, AtomicBoolean pressHandled,
                            EnhancedMovementConfig config) {
        if (c.screen != null) return;

        long currentTime = System.currentTimeMillis();

        boolean resetState = false;
        if (key == c.options.keyUp) {
            resetState = !forwardPressed || leftPressed || rightPressed || backPressed;
        } else if (key == c.options.keyDown) {
            resetState = !backPressed || leftPressed || rightPressed || forwardPressed;
        } else if (key == c.options.keyLeft) {
            resetState = !leftPressed || forwardPressed || backPressed || rightPressed;
        } else if (key == c.options.keyRight) {
            resetState = !rightPressed || forwardPressed || backPressed || leftPressed;
        }

        if (resetState) {
            forwardPressHandled.set(false);
            backPressHandled.set(false);
            leftPressHandled.set(false);
            rightPressHandled.set(false);
            keyReleased.set(false);
        }

        if (currentTime - globalCooldownTime.get() > config.movement.dash.cooldownMs || globalCooldownTime.get() == 0) {
            if (isPressed) {
                if (!pressHandled.get()) {
                    if (keyReleased.get()) {
                        if (currentTime - pressTime.get() < 400) {
                            performDash(c, key, config);
                            cooldownTime.set(currentTime);
                            globalCooldownTime.set(currentTime);
                            keyReleased.set(false);
                        } else {
                            pressTime.set(currentTime);
                            keyReleased.set(false);
                        }
                    } else {
                        pressTime.set(currentTime);
                    }
                    pressHandled.set(true);
                }
            } else {
                if (currentTime - pressTime.get() < 400) {
                    keyReleased.set(true);
                }
                pressHandled.set(false);
            }
        }
    }

    private void performDash(Minecraft c, KeyMapping key, EnhancedMovementConfig config) {
        if (isInAir && !config.movement.dash.enableAirDash) return;
        if (c.player == null) return;
        if (c.player.getFoodData().getFoodLevel() < 6) return;

        Vec3 startPos = c.player.position();
        float yaw = c.player.getYRot();
        float pitch = c.player.getXRot();

        float dashSpeed = 1.7f;
        float inAirDashSpeed = 1.5f;
        float _dashSpeed;
        float _upwardLift = 0f;
        if (isInAir) {
            _dashSpeed = inAirDashSpeed;
        } else {
            _dashSpeed = dashSpeed;
            _upwardLift = 0.4f;
        }

        double playerYaw = Math.toRadians(yaw);
        double offsetX = -Math.sin(playerYaw) * _dashSpeed;
        double offsetZ = Math.cos(playerYaw) * _dashSpeed;

        LocalPlayer p = c.player;
        if (key == c.options.keyUp) {
            p.setDeltaMovement(p.getDeltaMovement().add(offsetX, _upwardLift, offsetZ));
            p.hurtMarked = true;
        } else if (key == c.options.keyDown) {
            p.setDeltaMovement(p.getDeltaMovement().subtract(offsetX, _upwardLift, offsetZ));
            p.hurtMarked = true;
        } else if (key == c.options.keyLeft) {
            double leftOffsetX = Math.cos(playerYaw) * _dashSpeed;
            double leftOffsetZ = Math.sin(playerYaw) * _dashSpeed;
            p.setDeltaMovement(p.getDeltaMovement().add(leftOffsetX, _upwardLift, leftOffsetZ));
            p.hurtMarked = true;
        } else if (key == c.options.keyRight) {
            double rightOffsetX = -Math.cos(playerYaw) * _dashSpeed;
            double rightOffsetZ = -Math.sin(playerYaw) * _dashSpeed;
            p.setDeltaMovement(p.getDeltaMovement().add(rightOffsetX, _upwardLift, rightOffsetZ));
            p.hurtMarked = true;
        }

        if (config.movement.dash.afterimage.enabled) {
            startDashTracking(p.getUUID(), startPos, yaw, pitch);
        }

        // Piggyback on the double-jump fall-damage protection: dash-start Y as both
        // reference heights gives the player a ~3.5-block "free fall" budget below
        // their dash position, with scaled damage beyond that.
        ClientNetworkHandler.sendDoubleJumpData(startPos.y, startPos.y);
        p.fallDistance = 0.0f;

        p.causeFoodExhaustion(0.1f);
    }

    public void performMidAirJump(Player player, EnhancedMovementConfig config) {
        double currentVerticalVelocity = player.getDeltaMovement().y;
        double minimumVerticalVelocity = 0.4;
        double fixedJumpBoost = config.movement.doubleJump.jumpBoostPercent / 100.0;

        currentVerticalVelocity = Math.max(currentVerticalVelocity, minimumVerticalVelocity);
        double newVerticalVelocity = currentVerticalVelocity + fixedJumpBoost;

        player.setDeltaMovement(player.getDeltaMovement().add(0, newVerticalVelocity, 0));
        player.fallDistance = 0.0f;
    }

    private void resetJumpState() {
        isInAir = false;
        midAirJumpPerformed = false;
        isJumping = false;
        jumpKeyReleased = false;
        jumpKeyPressed = false;
        jumpStartTime = 0;
    }

    private void startDashTracking(UUID playerId, Vec3 startPos, float yaw, float pitch) {
        isDashTracking = true;
        dashTrackingStartTime = System.currentTimeMillis();
        dashStartPosition = startPos;
        dashingPlayerId = playerId;
        dashYaw = yaw;
        dashPitch = pitch;
        afterimagesSpawned = 0;
        lastPlayerPosition = startPos;
        lastMovementTime = System.currentTimeMillis();

        EnhancedMovementConfig config = EnhancedMovement.CONFIG;
        AfterimageManager.addSingleAfterimage(playerId, startPos, yaw, pitch,
            config.movement.dash.afterimage.baseLifetimeMs, 50L, config.movement.dash.afterimage.prismMode);
        afterimagesSpawned++;
    }

    private void handleDashTracking(Minecraft c, EnhancedMovementConfig config) {
        if (!isDashTracking || c.player == null) return;

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - dashTrackingStartTime;

        long trackingDuration = 750L;
        if (elapsedTime > trackingDuration) {
            isDashTracking = false;
            return;
        }

        int maxAfterimages = config.movement.dash.afterimage.imageCount;
        long spawnInterval = trackingDuration / maxAfterimages;
        long nextSpawnTime = afterimagesSpawned * spawnInterval;

        if (elapsedTime >= nextSpawnTime && afterimagesSpawned < maxAfterimages) {
            Vec3 currentPos = c.player.position();

            double movementDistance = lastPlayerPosition != null ? currentPos.distanceTo(lastPlayerPosition) : 1.0;
            if (movementDistance > 0.1) {
                lastMovementTime = currentTime;
                lastPlayerPosition = currentPos;
            } else if (currentTime - lastMovementTime > 150) {
                isDashTracking = false;
                return;
            }

            double distanceFromStart = currentPos.distanceTo(dashStartPosition);
            if (distanceFromStart > 0.2) {
                Vec3 fakeEndPos = currentPos.add(0, 0, 0);
                ClientNetworkHandler.sendAfterimageData(
                    dashingPlayerId, currentPos, fakeEndPos,
                    dashYaw, dashPitch, 1
                );

                long spawnDelay = Math.max(0, elapsedTime - 35);
                AfterimageManager.addSingleAfterimage(
                    dashingPlayerId, currentPos,
                    dashYaw, dashPitch,
                    config.movement.dash.afterimage.baseLifetimeMs,
                    spawnDelay,
                    config.movement.dash.afterimage.prismMode
                );

                afterimagesSpawned++;
            }
        }
    }
}
