package net.fabricmc.EnhancedMovement;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.util.InputUtil;
import net.minecraft.client.option.KeyBinding;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

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
                    
                    if (timeSinceJumpStart >= 250 && jumpKeyPressed && jumpKeyReleased && !midAirJumpPerformed) {
                        if (!onGround) {
                            performMidAirJump(client);
                            midAirJumpPerformed = true;
                        }
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
                    isInAir = false;
                    midAirJumpPerformed = false;
                    isJumping = false;
                    jumpKeyReleased = false;
                    jumpKeyPressed = false;
                    jumpStartTime = 0;
                    gracePeriod = 0;
                }
            }
        });
    }

    private void performMidAirJump(MinecraftClient client) {
        double currentVerticalVelocity = client.player.getVelocity().y;
        double minimumVerticalVelocity = 0.5;
        double fixedJumpBoost = 0.4; 
    
        currentVerticalVelocity = Math.max(currentVerticalVelocity, minimumVerticalVelocity);
        double newVerticalVelocity = currentVerticalVelocity + fixedJumpBoost;
    
        // Set the new velocity for the player
        client.player.setVelocity(client.player.getVelocity().add(0, newVerticalVelocity, 0));
        client.player.fallDistance = 0;
    }
    
    public MinecraftClient getClient() {
        return client;
    }
    
    public static EnhancedMovement getInstance() {
        if (instance == null) {
            instance = new EnhancedMovement();
        }
        return instance;
    }
    
    public boolean hasPerformedMidAirJump() {
        return midAirJumpPerformed;
    }

    private void handleDash(KeyBinding key, boolean isPressed, AtomicLong pressTime, AtomicLong cooldownTime, AtomicBoolean keyReleased, AtomicLong globalCooldownTime) {
        long currentTime = System.currentTimeMillis();
    
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
        double speedMultiplier = 1.5;
        double heightBoost = 0.4;
        double xVelocity = 0;
        double zVelocity = 0;
        float yaw = client.player.getYaw();
    
        if (key == client.options.forwardKey) {
            xVelocity = -Math.sin(Math.toRadians(yaw));
            zVelocity = Math.cos(Math.toRadians(yaw));
        } else if (key == client.options.backKey) {
            xVelocity = Math.sin(Math.toRadians(yaw));
            zVelocity = -Math.cos(Math.toRadians(yaw));
        } else if (key == client.options.leftKey) {
            xVelocity = Math.cos(Math.toRadians(yaw));
            zVelocity = Math.sin(Math.toRadians(yaw));
        } else if (key == client.options.rightKey) {
            xVelocity = -Math.cos(Math.toRadians(yaw));
            zVelocity = -Math.sin(Math.toRadians(yaw));
        }
    
        xVelocity *= speedMultiplier;
        zVelocity *= speedMultiplier;
    
        // Set the new velocity for the player
        client.player.setVelocity(client.player.getVelocity().add(xVelocity, heightBoost, zVelocity));
    }
}
