package net.fabricmc.EnhancedMovement.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.EnhancedMovement.config.EnhancedMovementConfig;
import net.fabricmc.EnhancedMovement.LedgeGrab;
import net.fabricmc.EnhancedMovement.client.ClientNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerEntity;
import me.shedaniel.autoconfig.AutoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnhancedMovementClient implements ClientModInitializer {
    
    public static final Logger LOGGER = LoggerFactory.getLogger("enhancedmovement-client");
    private final MinecraftClient client = MinecraftClient.getInstance();
    
    // Dash keybind
    private static KeyBinding dashKey;
    
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

    private final AtomicBoolean forwardPressHandled = new AtomicBoolean(false);
    private final AtomicBoolean backPressHandled = new AtomicBoolean(false);
    private final AtomicBoolean leftPressHandled = new AtomicBoolean(false);
    private final AtomicBoolean rightPressHandled = new AtomicBoolean(false);
    
    public static LedgeGrab ledgeGrab = new LedgeGrab();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Enhanced Movement client initialized.");
        
        dashKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.enhancedmovement.dash",
            GLFW.GLFW_KEY_UNKNOWN, // Unbound by default - perfect for mouse side buttons!
            "key.categories.enhancedmovement"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                EnhancedMovementConfig config = AutoConfig.getConfigHolder(EnhancedMovementConfig.class).getConfig();

                boolean isSneaking = client.options.sneakKey.isPressed();
                if (isSneaking && config.movement.general.sneakDisablesFeatures) {
                    return;
                }

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
            }
        });
    }

    private void handleDashInput(EnhancedMovementConfig config) {
        KeyBinding forwardKey = client.options.forwardKey;
        KeyBinding backKey = client.options.backKey;
        KeyBinding leftKey = client.options.leftKey;
        KeyBinding rightKey = client.options.rightKey;

        forwardPressed = forwardKey.isPressed();
        leftPressed = leftKey.isPressed();
        rightPressed = rightKey.isPressed();
        backPressed = backKey.isPressed();

        handleDash(forwardKey, forwardPressed, forwardPressTime, forwardCooldownTime, forwardKeyReleased, globalCooldownTime, forwardPressHandled, config);
        handleDash(backKey, backPressed, backPressTime, backCooldownTime, backKeyReleased, globalCooldownTime, backPressHandled, config);
        handleDash(leftKey, leftPressed, leftPressTime, leftCooldownTime, leftKeyReleased, globalCooldownTime, leftPressHandled, config);
        handleDash(rightKey, rightPressed, rightPressTime, rightCooldownTime, rightKeyReleased, globalCooldownTime, rightPressHandled, config);
    }

    private void handleKeybindDash(EnhancedMovementConfig config) {
        if (client.player == null) return;
        
        // Skip if player has a GUI open
        if (client.currentScreen != null) {
            return;
        }
        
        if (dashKey.wasPressed()) {
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - globalCooldownTime.get() <= config.movement.dash.cooldownSeconds * 1000L && globalCooldownTime.get() != 0) {
                return;
            }
            
            // Detect current movement direction
            String direction = detectMovementDirection();
            if (direction != null) {
                // Perform dash in the direction player is moving
                performDashInDirection(direction, config);
                globalCooldownTime.set(currentTime);
            }
        }
    }
    
    private String detectMovementDirection() {
        boolean forward = client.options.forwardKey.isPressed();
        boolean back = client.options.backKey.isPressed();
        boolean left = client.options.leftKey.isPressed();
        boolean right = client.options.rightKey.isPressed();
        
        // Prioritize single directions first, then diagonals
        if (forward && !back && !left && !right) return "forward";
        if (back && !forward && !left && !right) return "back";
        if (left && !forward && !back && !right) return "left";
        if (right && !forward && !back && !left) return "right";
        
        // Handle diagonal movement (forward takes priority for diagonals)
        if (forward && left) return "forward";
        if (forward && right) return "forward";
        if (back && left) return "back";
        if (back && right) return "back";
        
        // No clear direction or multiple conflicting directions - default to forward
        return "forward";
    }
    
    private void performDashInDirection(String direction, EnhancedMovementConfig config) {
        if (isInAir && !config.movement.dash.enableAirDash) {
            return;
        }
        
        if (client.player != null) {
            // Check hunger level requirement (same as sprinting)
            if (client.player.getHungerManager().getFoodLevel() < 6) {
                return;
            }
            
            // Use same dash logic as double-tap mode
            KeyBinding key = null;
            switch (direction) {
                case "forward": key = client.options.forwardKey; break;
                case "back": key = client.options.backKey; break;
                case "left": key = client.options.leftKey; break;
                case "right": key = client.options.rightKey; break;
                default: return;
            }
            
            performDash(key, config);
        }
    }

    private void handleDoubleJumpInput(EnhancedMovementConfig config) {
        boolean onGround = client.player.isOnGround();
        jumpKeyPressed = client.options.jumpKey.isPressed();

        // Handle the initial jump
        if (jumpKeyPressed && !isInAir) {
            jumpStartTime = System.currentTimeMillis();
            isInAir = true;
            isJumping = true;
        }

        // Handle mid-air jump
        if (isJumping && isInAir) {
            long timeSinceJumpStart = System.currentTimeMillis() - jumpStartTime;
            
            if (timeSinceJumpStart >= config.movement.doubleJump.delayBeforeDoubleJumpMs && 
                jumpKeyPressed && jumpKeyReleased && !midAirJumpPerformed && !onGround) {
                if (client.player.getHungerManager().getFoodLevel() >= 6) {
                    double firstJumpY = client.player.getY() - (timeSinceJumpStart / 1000.0 * 0.08); // Estimate first jump Y
                    double doubleJumpY = client.player.getY();
                    performMidAirJump(client.player, config);
                    ClientNetworkHandler.sendDoubleJumpData(firstJumpY, doubleJumpY);
                    
                    client.player.addExhaustion(0.2f);
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
            resetJumpState();
        }
    }

    private void handleDash(KeyBinding key, boolean isPressed, AtomicLong pressTime, AtomicLong cooldownTime, 
                          AtomicBoolean keyReleased, AtomicLong globalCooldownTime, AtomicBoolean pressHandled, 
                          EnhancedMovementConfig config) {

        if (client.currentScreen != null) {
            return;
        }
    
        long currentTime = System.currentTimeMillis();
    
        boolean resetState = false;
        if (key == client.options.forwardKey) {
            resetState = !forwardPressed || leftPressed || rightPressed || backPressed;
        } else if (key == client.options.backKey) {
            resetState = !backPressed || leftPressed || rightPressed || forwardPressed;
        } else if (key == client.options.leftKey) {
            resetState = !leftPressed || forwardPressed || backPressed || rightPressed;
        } else if (key == client.options.rightKey) {
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
                        if (currentTime - pressTime.get() < 400) { // 400ms double-tap window
                            performDash(key, config);
                            cooldownTime.set(currentTime);
                            globalCooldownTime.set(currentTime);
                            keyReleased.set(false);
                        } else if (currentTime - pressTime.get() >= 400) {
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

    private void performDash(KeyBinding key, EnhancedMovementConfig config) {
        if (isInAir && !config.movement.dash.enableAirDash) {
            return;
        }
        
        if (client.player != null) {
            // Check hunger level requirement (same as sprinting)
            if (client.player.getHungerManager().getFoodLevel() < 6) {
                return;
            }
            
            // Use fixed dash speeds like Burst Boots
            float dashSpeed = 1.5f; // Ground dash speed (matches Burst Boots)
            float inAirDashSpeed = 1.2f; // Air dash speed
            
            float _dashSpeed;
            float _upwardLift = 0f;
            if (isInAir) {
                _dashSpeed = inAirDashSpeed;
            } else {
                _dashSpeed = dashSpeed;
                _upwardLift = 0.2f; // Small upward boost like Burst Boots
            }

            double playerYaw = Math.toRadians(client.player.getYaw());
            double offsetX = -Math.sin(playerYaw) * _dashSpeed;
            double offsetZ = Math.cos(playerYaw) * _dashSpeed;

            if (key == client.options.forwardKey) {
                client.player.setVelocity(client.player.getVelocity().add(offsetX, _upwardLift, offsetZ));
                client.player.velocityModified = true;
            } else if (key == client.options.backKey) {
                client.player.setVelocity(client.player.getVelocity().subtract(offsetX, _upwardLift, offsetZ));
                client.player.velocityModified = true;
            } else if (key == client.options.leftKey) {
                double leftOffsetX = Math.cos(playerYaw) * _dashSpeed;
                double leftOffsetZ = Math.sin(playerYaw) * _dashSpeed;
                client.player.setVelocity(client.player.getVelocity().add(leftOffsetX, _upwardLift, leftOffsetZ));
                client.player.velocityModified = true;
            } else if (key == client.options.rightKey) {
                double rightOffsetX = -Math.cos(playerYaw) * _dashSpeed;
                double rightOffsetZ = -Math.sin(playerYaw) * _dashSpeed;
                client.player.setVelocity(client.player.getVelocity().add(rightOffsetX, _upwardLift, rightOffsetZ));
                client.player.velocityModified = true;
            }
            
            // Add hunger exhaustion (like sprinting)
            client.player.addExhaustion(0.1f);
        }
    }

    public void performMidAirJump(PlayerEntity player, EnhancedMovementConfig config) {
        double currentVerticalVelocity = player.getVelocity().y;
        double minimumVerticalVelocity = 0.4; // Fixed value like Burst Boots/Ultra Jump Belt
        double fixedJumpBoost = config.movement.doubleJump.jumpBoostPercent / 100.0;

        currentVerticalVelocity = Math.max(currentVerticalVelocity, minimumVerticalVelocity);
        double newVerticalVelocity = currentVerticalVelocity + fixedJumpBoost;

        player.setVelocity(player.getVelocity().add(0, newVerticalVelocity, 0));
        player.fallDistance = 0;
    }

    private void resetJumpState() {
        isInAir = false;
        midAirJumpPerformed = false;
        isJumping = false;
        jumpKeyReleased = false;
        jumpKeyPressed = false;
        jumpStartTime = 0;
    }

}