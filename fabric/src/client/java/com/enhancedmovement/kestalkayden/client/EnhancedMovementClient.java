package com.enhancedmovement.kestalkayden.client;

import com.enhancedmovement.kestalkayden.EnhancedMovement;
import com.enhancedmovement.kestalkayden.NetworkHandler;
import com.enhancedmovement.kestalkayden.config.EnhancedMovementConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class EnhancedMovementClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("enhancedmovement-client");
    private final Minecraft client = Minecraft.getInstance();

    private static KeyMapping dashKey;
    private static final KeyMapping.Category EM_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("enhancedmovement", "main")
    );

    // Jump tracking
    private boolean jumpKeyPressed = false;
    private boolean jumpKeyReleased = false;
    private boolean midAirJumpPerformed = false;
    private boolean isInAir = false;
    private boolean isJumping = false;
    private long jumpStartTime = 0;

    // Movement tracking
    private boolean forwardPressed = false;
    private boolean backPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    // Dash tracking for afterimages
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

    public static LedgeGrab ledgeGrab = new LedgeGrab();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Enhanced Movement client initialized.");

        dashKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.enhancedmovement.dash",
            GLFW.GLFW_KEY_UNKNOWN,
            EM_CATEGORY
        ));

        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.AfterimagePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                EnhancedMovementConfig config = EnhancedMovement.CONFIG;
                if (config.movement.dash.afterimage.enabled) {
                    Vec3 position = new Vec3(payload.startX(), payload.startY(), payload.startZ());
                    AfterimageManager.addSingleAfterimage(
                        payload.playerId(),
                        position,
                        payload.yaw(),
                        payload.pitch(),
                        config.movement.dash.afterimage.baseLifetimeMs,
                        0L
                    );
                }
            });
        });

        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register((context) -> {
            EnhancedMovementConfig config = EnhancedMovement.CONFIG;
            if (!config.movement.dash.afterimage.enabled) return;

            var matrices = context.poseStack();
            var vertexConsumers = context.bufferSource();
            int light = 15728880;

            for (AfterimageManager.AfterimageData afterimage : AfterimageManager.getActiveAfterimages()) {
                if (afterimage.getOpacity() > 0.0f) {
                    Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
                    double distanceToCamera = afterimage.position.distanceTo(cameraPos);
                    boolean isFirstPerson = client.options.getCameraType().isFirstPerson();
                    if (!isFirstPerson || distanceToCamera > 1.5) {
                        AfterimageRenderer.renderAfterimage(matrices, vertexConsumers, afterimage, light);
                    }
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (c.player == null) return;
            EnhancedMovementConfig config = EnhancedMovement.CONFIG;

            boolean isSneaking = c.options.keyShift.isDown();
            if (isSneaking && config.movement.general.sneakDisablesFeatures) return;

            if (config.movement.dash.enabled) {
                if (config.movement.dash.useKeybinds) {
                    handleKeybindDash(config);
                } else {
                    handleDashInput(config);
                }
            }

            if (config.movement.doubleJump.enabled) {
                handleDoubleJumpInput(config);
            }

            if (config.movement.doubleJump.enableLedgeGrab) {
                ledgeGrab.tick();
            }

            AfterimageManager.tick();
            handleDashTracking(config);
        });
    }

    private void handleDashInput(EnhancedMovementConfig config) {
        KeyMapping forwardKey = client.options.keyUp;
        KeyMapping backKey = client.options.keyDown;
        KeyMapping leftKey = client.options.keyLeft;
        KeyMapping rightKey = client.options.keyRight;

        forwardPressed = forwardKey.isDown();
        leftPressed = leftKey.isDown();
        rightPressed = rightKey.isDown();
        backPressed = backKey.isDown();

        handleDash(forwardKey, forwardPressed, forwardPressTime, forwardCooldownTime, forwardKeyReleased, forwardPressHandled, config);
        handleDash(backKey, backPressed, backPressTime, backCooldownTime, backKeyReleased, backPressHandled, config);
        handleDash(leftKey, leftPressed, leftPressTime, leftCooldownTime, leftKeyReleased, leftPressHandled, config);
        handleDash(rightKey, rightPressed, rightPressTime, rightCooldownTime, rightKeyReleased, rightPressHandled, config);
    }

    private void handleKeybindDash(EnhancedMovementConfig config) {
        if (client.player == null) return;
        if (client.screen != null) return;

        if (dashKey.consumeClick()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - globalCooldownTime.get() <= config.movement.dash.cooldownSeconds * 1000L && globalCooldownTime.get() != 0) {
                return;
            }
            String direction = detectMovementDirection();
            if (direction != null) {
                performDashInDirection(direction, config);
                globalCooldownTime.set(currentTime);
            }
        }
    }

    private String detectMovementDirection() {
        boolean forward = client.options.keyUp.isDown();
        boolean back = client.options.keyDown.isDown();
        boolean left = client.options.keyLeft.isDown();
        boolean right = client.options.keyRight.isDown();

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

    private void performDashInDirection(String direction, EnhancedMovementConfig config) {
        if (isInAir && !config.movement.dash.enableAirDash) return;
        if (client.player == null) return;
        if (client.player.getFoodData().getFoodLevel() < 6) return;

        KeyMapping key;
        switch (direction) {
            case "forward": key = client.options.keyUp; break;
            case "back": key = client.options.keyDown; break;
            case "left": key = client.options.keyLeft; break;
            case "right": key = client.options.keyRight; break;
            default: return;
        }
        performDash(key, config);
    }

    private void handleDoubleJumpInput(EnhancedMovementConfig config) {
        if (client.player == null) return;
        boolean onGround = client.player.onGround();
        jumpKeyPressed = client.options.keyJump.isDown();

        if (jumpKeyPressed && !isInAir) {
            jumpStartTime = System.currentTimeMillis();
            isInAir = true;
            isJumping = true;
        }

        if (isJumping && isInAir) {
            long timeSinceJumpStart = System.currentTimeMillis() - jumpStartTime;
            if (timeSinceJumpStart >= config.movement.doubleJump.delayBeforeDoubleJumpMs
                && jumpKeyPressed && jumpKeyReleased && !midAirJumpPerformed && !onGround) {
                if (client.player.getFoodData().getFoodLevel() >= 6) {
                    double firstJumpY = client.player.getY() - (timeSinceJumpStart / 1000.0 * 0.08);
                    double doubleJumpY = client.player.getY();
                    performMidAirJump(client.player, config);
                    ClientNetworkHandler.sendDoubleJumpData(firstJumpY, doubleJumpY);
                    client.player.causeFoodExhaustion(0.2f);
                    midAirJumpPerformed = true;
                }
            }

            if (!jumpKeyPressed) {
                jumpKeyReleased = true;
            }
        }

        if (isInAir && jumpKeyReleased && midAirJumpPerformed) {
            client.player.fallDistance = 0.0;
        }

        if (onGround) {
            resetJumpState();
        }
    }

    private void handleDash(KeyMapping key, boolean isPressed, AtomicLong pressTime, AtomicLong cooldownTime,
                            AtomicBoolean keyReleased, AtomicBoolean pressHandled,
                            EnhancedMovementConfig config) {
        if (client.screen != null) return;

        long currentTime = System.currentTimeMillis();

        boolean resetState = false;
        if (key == client.options.keyUp) {
            resetState = !forwardPressed || leftPressed || rightPressed || backPressed;
        } else if (key == client.options.keyDown) {
            resetState = !backPressed || leftPressed || rightPressed || forwardPressed;
        } else if (key == client.options.keyLeft) {
            resetState = !leftPressed || forwardPressed || backPressed || rightPressed;
        } else if (key == client.options.keyRight) {
            resetState = !rightPressed || forwardPressed || backPressed || leftPressed;
        }

        if (resetState) {
            forwardPressHandled.set(false);
            backPressHandled.set(false);
            leftPressHandled.set(false);
            rightPressHandled.set(false);
            keyReleased.set(false);
        }

        if (currentTime - globalCooldownTime.get() > config.movement.dash.cooldownSeconds * 1000L || globalCooldownTime.get() == 0) {
            if (isPressed) {
                if (!pressHandled.get()) {
                    if (keyReleased.get()) {
                        if (currentTime - pressTime.get() < 400) {
                            performDash(key, config);
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

    private void performDash(KeyMapping key, EnhancedMovementConfig config) {
        if (isInAir && !config.movement.dash.enableAirDash) return;
        if (client.player == null) return;
        if (client.player.getFoodData().getFoodLevel() < 6) return;

        Vec3 startPos = client.player.position();
        float yaw = client.player.getYRot();
        float pitch = client.player.getXRot();

        float dashSpeed = 1.5f;
        float inAirDashSpeed = 1.2f;
        float _dashSpeed;
        float _upwardLift = 0f;
        if (isInAir) {
            _dashSpeed = inAirDashSpeed;
        } else {
            _dashSpeed = dashSpeed;
            _upwardLift = 0.2f;
        }

        double playerYaw = Math.toRadians(yaw);
        double offsetX = -Math.sin(playerYaw) * _dashSpeed;
        double offsetZ = Math.cos(playerYaw) * _dashSpeed;

        LocalPlayer p = client.player;
        if (key == client.options.keyUp) {
            p.setDeltaMovement(p.getDeltaMovement().add(offsetX, _upwardLift, offsetZ));
            p.hurtMarked = true;
        } else if (key == client.options.keyDown) {
            p.setDeltaMovement(p.getDeltaMovement().subtract(offsetX, _upwardLift, offsetZ));
            p.hurtMarked = true;
        } else if (key == client.options.keyLeft) {
            double leftOffsetX = Math.cos(playerYaw) * _dashSpeed;
            double leftOffsetZ = Math.sin(playerYaw) * _dashSpeed;
            p.setDeltaMovement(p.getDeltaMovement().add(leftOffsetX, _upwardLift, leftOffsetZ));
            p.hurtMarked = true;
        } else if (key == client.options.keyRight) {
            double rightOffsetX = -Math.cos(playerYaw) * _dashSpeed;
            double rightOffsetZ = -Math.sin(playerYaw) * _dashSpeed;
            p.setDeltaMovement(p.getDeltaMovement().add(rightOffsetX, _upwardLift, rightOffsetZ));
            p.hurtMarked = true;
        }

        if (config.movement.dash.afterimage.enabled) {
            startDashTracking(p.getUUID(), startPos, yaw, pitch);
        }

        p.causeFoodExhaustion(0.1f);
    }

    public void performMidAirJump(Player player, EnhancedMovementConfig config) {
        double currentVerticalVelocity = player.getDeltaMovement().y;
        double minimumVerticalVelocity = 0.4;
        double fixedJumpBoost = config.movement.doubleJump.jumpBoostPercent / 100.0;

        currentVerticalVelocity = Math.max(currentVerticalVelocity, minimumVerticalVelocity);
        double newVerticalVelocity = currentVerticalVelocity + fixedJumpBoost;

        player.setDeltaMovement(player.getDeltaMovement().add(0, newVerticalVelocity, 0));
        player.fallDistance = 0.0;
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

    private void handleDashTracking(EnhancedMovementConfig config) {
        if (!isDashTracking || client.player == null) return;

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
            Vec3 currentPos = client.player.position();

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
                    dashingPlayerId,
                    currentPos,
                    fakeEndPos,
                    dashYaw,
                    dashPitch,
                    1
                );

                long spawnDelay = Math.max(0, elapsedTime - 35);
                AfterimageManager.addSingleAfterimage(
                    dashingPlayerId,
                    currentPos,
                    dashYaw,
                    dashPitch,
                    config.movement.dash.afterimage.baseLifetimeMs,
                    spawnDelay,
                    config.movement.dash.afterimage.prismMode
                );

                afterimagesSpawned++;
            }
        }
    }
}
