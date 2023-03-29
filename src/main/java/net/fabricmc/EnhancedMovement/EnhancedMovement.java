package net.fabricmc.EnhancedMovement;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

import net.fabricmc.EnhancedMovement.NetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.LivingEntity;


public class EnhancedMovement implements ModInitializer {

    // Defines
    private static EnhancedMovement instance;
    private final MinecraftClient client = MinecraftClient.getInstance();
    public static final Logger LOGGER = LoggerFactory.getLogger("enhancedmovement");
    private int timeDelayDash = 600;
    private int timeCooldownDash = 2200;

    // Jump
    private boolean jumpKeyPressed = false;
    private boolean jumpKeyReleased = false;
    private boolean midAirJumpPerformed = false;
    private boolean isInAir = false;
    private boolean isJumping = false;
    private long jumpStartTime = 0;
    private long gracePeriod = 0;

    // Movement
    private boolean forwardPressed = false;
    private boolean backPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    private AtomicLong forwardPressTime = new AtomicLong(0);
    private AtomicLong backPressTime = new AtomicLong(0);
    private AtomicLong leftPressTime = new AtomicLong(0);
    private AtomicLong rightPressTime = new AtomicLong(0);
    
    private AtomicLong globalCooldownTime = new AtomicLong(0);
    private AtomicLong forwardCooldownTime = new AtomicLong(0);
    private AtomicLong backCooldownTime = new AtomicLong(0);
    private AtomicLong leftCooldownTime = new AtomicLong(0);
    private AtomicLong rightCooldownTime = new AtomicLong(0);
    
    private AtomicBoolean forwardKeyReleased = new AtomicBoolean(false);
    private AtomicBoolean backKeyReleased = new AtomicBoolean(false);
    private AtomicBoolean leftKeyReleased = new AtomicBoolean(false);
    private AtomicBoolean rightKeyReleased = new AtomicBoolean(false);

    private KeyBinding lastKeyPressed;

    @Override
    public void onInitialize() {
        LOGGER.info("Enhanced Movement mod initialized.");

        instance = this;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {

                KeyBinding forwardKey = client.options.forwardKey;
                KeyBinding backKey = client.options.backKey;
                KeyBinding leftKey = client.options.leftKey;
                KeyBinding rightKey = client.options.rightKey;

                // Movement Bools
                if (InputUtil.isKeyPressed(client.getWindow().getHandle(), forwardKey.getDefaultKey().getCode())) { forwardPressed = true; } else { forwardPressed = false; }
                if (InputUtil.isKeyPressed(client.getWindow().getHandle(), leftKey.getDefaultKey().getCode())) { leftPressed = true; } else { leftPressed = false; }
                if (InputUtil.isKeyPressed(client.getWindow().getHandle(), rightKey.getDefaultKey().getCode())) { rightPressed = true; } else { rightPressed = false; }
                if (InputUtil.isKeyPressed(client.getWindow().getHandle(), backKey.getDefaultKey().getCode())) { backPressed = true; } else { backPressed = false; }

                handleDash(forwardKey, forwardPressed, forwardPressTime, forwardCooldownTime, forwardKeyReleased, globalCooldownTime);
                handleDash(backKey, backPressed, backPressTime, backCooldownTime, backKeyReleased, globalCooldownTime);
                handleDash(leftKey, leftPressed, leftPressTime, leftCooldownTime, leftKeyReleased, globalCooldownTime);
                handleDash(rightKey, rightPressed, rightPressTime, rightCooldownTime, rightKeyReleased, globalCooldownTime);
                boolean onGround = client.player.isOnGround();
                jumpKeyPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), client.options.jumpKey.getDefaultKey().getCode());
                
                // Handle the initial jump
                if (jumpKeyPressed && !isInAir) {
                    jumpStartTime = System.currentTimeMillis();
                    isInAir = true;
                    isJumping = true;
                }
                
                // Handle mid-air jump
                if (isJumping && isInAir) {
                    long timeSinceJumpStart = System.currentTimeMillis() - jumpStartTime;
                    
                    if (timeSinceJumpStart >= 250 && jumpKeyPressed && jumpKeyReleased && !midAirJumpPerformed && !onGround) {
                        performMidAirJump(client.player);
                        NetworkHandler.sendDoubleJumpPacket(client.player);
                        midAirJumpPerformed = true;
                    }
                    
                    if (!jumpKeyPressed) {
                        jumpKeyReleased = true;
                    }
                }
                
                // Reset fall distance
                if (isInAir && jumpKeyReleased && midAirJumpPerformed) {
                    client.player.fallDistance = 0;
                }
                
                // Reset all variables when the player is on the ground
                if (onGround) {
                    resetJumpState();
                }

            }
        });
    }
    
    public void performMidAirJump(PlayerEntity player) {
        double currentVerticalVelocity = player.getVelocity().y;
        double minimumVerticalVelocity = 0.3;
        double fixedJumpBoost = 0.3;

        currentVerticalVelocity = Math.max(currentVerticalVelocity, minimumVerticalVelocity);
        double newVerticalVelocity = currentVerticalVelocity + fixedJumpBoost;

        // Set the new velocity for the player
        player.setVelocity(player.getVelocity().add(0, newVerticalVelocity, 0));
        player.fallDistance = 0;
    }

    private void handleDash(KeyBinding key, boolean isPressed, AtomicLong pressTime, AtomicLong cooldownTime, AtomicBoolean keyReleased, AtomicLong globalCooldownTime) {
        long currentTime = System.currentTimeMillis();
        int timeDelayDash = 350;
        int timeCooldownDash = 1300;

        if (isPressed) {
            if (keyReleased.get()) {
                if (currentTime - pressTime.get() < timeDelayDash && currentTime - globalCooldownTime.get() > timeCooldownDash) {
                    performDash(key);
                    cooldownTime.set(currentTime);
                    globalCooldownTime.set(currentTime);
                    keyReleased.set(false);
                } else if (currentTime - pressTime.get() >= timeDelayDash) {
                    pressTime.set(currentTime);
                    keyReleased.set(false);
                }
            } else {
                pressTime.set(currentTime);
            }
        } else {
            if (currentTime - pressTime.get() < timeDelayDash) {
                keyReleased.set(true);
            } else {
                keyReleased.set(false);
                cooldownTime.set(0);
            }
        }
    }

    private void performDash(KeyBinding key) {
        if (client.player != null) {
            float dashSpeed = 1.6f;
            double playerYaw = Math.toRadians(client.player.getYaw());
            double offsetX = -Math.sin(playerYaw) * dashSpeed;
            double offsetZ = Math.cos(playerYaw) * dashSpeed;

            if (key == client.options.forwardKey) {
                client.player.setVelocity(client.player.getVelocity().add(offsetX, 0, offsetZ));
                NetworkHandler.sendDashPacket(client.player, offsetX, 0, offsetZ);
            } else if (key == client.options.backKey) {
                client.player.setVelocity(client.player.getVelocity().subtract(offsetX, 0, offsetZ));
                NetworkHandler.sendDashPacket(client.player, -offsetX, 0, -offsetZ);
            } else if (key == client.options.leftKey) {
                double leftOffsetX = Math.cos(playerYaw) * dashSpeed;
                double leftOffsetZ = Math.sin(playerYaw) * dashSpeed;
                client.player.setVelocity(client.player.getVelocity().add(leftOffsetX, 0, leftOffsetZ));
                NetworkHandler.sendDashPacket(client.player, leftOffsetX, 0, leftOffsetZ);
            } else if (key == client.options.rightKey) {
                double rightOffsetX = -Math.cos(playerYaw) * dashSpeed;
                double rightOffsetZ = -Math.sin(playerYaw) * dashSpeed;
                client.player.setVelocity(client.player.getVelocity().add(rightOffsetX, 0, rightOffsetZ));
                NetworkHandler.sendDashPacket(client.player, rightOffsetX, 0, rightOffsetZ);
            }
        }
    }

    public static EnhancedMovement getInstance() {
        return instance;
    }

    public MinecraftClient getClient() {
        return client;
    }
    
    public boolean hasPerformedMidAirJump() {
        return midAirJumpPerformed;
    }

    private void resetJumpState() {
        isInAir = false;
        midAirJumpPerformed = false;
        isJumping = false;
        jumpKeyReleased = false;
        jumpKeyPressed = false;
        jumpStartTime = 0;
        gracePeriod = 0;
    }

}
