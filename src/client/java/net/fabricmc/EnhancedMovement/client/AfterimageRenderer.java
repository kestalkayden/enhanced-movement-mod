package net.fabricmc.EnhancedMovement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class AfterimageRenderer {
    
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static void renderAfterimage(
        MatrixStack matrices, 
        VertexConsumerProvider vertexConsumers,
        AfterimageManager.AfterimageData afterimage,
        int light
    ) {
        matrices.push();
        
        // Get camera position
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        
        // Position the afterimage relative to camera with slight offset to avoid artifacts
        Vec3d offsetPos = afterimage.position;
        
        // Add small offset away from camera to prevent first-person artifacts
        Vec3d cameraToAfterimage = offsetPos.subtract(cameraPos);
        if (cameraToAfterimage.length() < 2.0) {
            Vec3d offsetDirection = cameraToAfterimage.normalize();
            offsetPos = cameraPos.add(offsetDirection.multiply(2.0));
        }
        
        matrices.translate(
            offsetPos.x - cameraPos.x,
            offsetPos.y - cameraPos.y,
            offsetPos.z - cameraPos.z
        );
        
        // Create a simple glowing cube/outline effect
        float opacity = afterimage.getOpacity();
        
        // Render a glowing player-like shape (simplified humanoid outline)
        renderPlayerSilhouette(matrices, vertexConsumers, opacity, light, afterimage);
        
        matrices.pop();
    }
    
    private static void renderPlayerSilhouette(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float opacity, int light, AfterimageManager.AfterimageData afterimage) {
        // Use cutout render layer for visibility
        RenderLayer renderLayer = RenderLayer.getCutout();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Get colors based on prism mode or special player easter eggs
        float[] colors = getPlayerColors(afterimage);
        float red = colors[0];
        float green = colors[1];
        float blue = colors[2];
        
        // Render a multi-part player silhouette with head, torso, arms, and legs
        renderPlayerSilhouetteParts(matrix, vertexConsumer, red, green, blue, opacity, light);
    }
    
    private static float[] getPlayerColors(AfterimageManager.AfterimageData afterimage) {
        // Check for special player easter eggs first
        String playerName = getPlayerName(afterimage.playerId);
        float[] specialColors = getSpecialPlayerColors(playerName);
        if (specialColors != null) {
            return specialColors;
        }
        
        // Normal color logic
        return afterimage.prismMode ? getPrismColors(afterimage) : getMatrixColors();
    }
    
    private static String getPlayerName(java.util.UUID playerId) {
        // Get player name from UUID
        if (client.world != null) {
            var player = client.world.getPlayerByUuid(playerId);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return "";
    }
    
    private static float[] getSpecialPlayerColors(String playerName) {
        if (playerName == null) return null;
        
        switch (playerName.toLowerCase()) {
            case "kestalkayden":
                return new float[]{1.0f, 0.0f, 0.0f}; // Vibrant red
            default:
                return null; // No special color
        }
    }
    
    private static float[] getMatrixColors() {
        // Matrix-like cyan/green color with slight variation
        float red = 0.0f + (float)(Math.random() * 0.1);
        float green = 0.8f + (float)(Math.random() * 0.2 - 0.1);
        float blue = 0.9f + (float)(Math.random() * 0.1);
        return new float[]{red, green, blue};
    }
    
    private static float[] getPrismColors(AfterimageManager.AfterimageData afterimage) {
        // Calculate time-based color cycling starting with blue
        long currentTime = System.currentTimeMillis();
        long timeSinceCreation = currentTime - afterimage.creationTime;
        
        // Fast color cycling - complete rainbow cycle every 1.25 seconds
        float cycleDuration = 1250.0f; // 1.25 seconds per full cycle
        float cycleProgress = (timeSinceCreation % (long)cycleDuration) / cycleDuration;
        
        // Start with blue (hue = 240°/360° = 0.667) and cycle through spectrum
        float startHue = 240.0f / 360.0f; // Blue starting point
        float hue = (startHue + cycleProgress) % 1.0f;
        
        // Convert HSV to RGB for rainbow spectrum
        return hsvToRgb(hue, 0.9f, 1.0f); // High saturation and value for vibrant colors
    }
    
    private static float[] hsvToRgb(float hue, float saturation, float value) {
        int i = (int)(hue * 6);
        float f = hue * 6 - i;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);
        
        switch (i % 6) {
            case 0: return new float[]{value, t, p};
            case 1: return new float[]{q, value, p};
            case 2: return new float[]{p, value, t};
            case 3: return new float[]{p, q, value};
            case 4: return new float[]{t, p, value};
            case 5: return new float[]{value, p, q};
            default: return new float[]{1.0f, 1.0f, 1.0f};
        }
    }
    
    private static void renderPlayerSilhouetteParts(Matrix4f matrix, VertexConsumer vertexConsumer, 
                                                   float red, float green, float blue, float opacity, int light) {
        // Player dimensions (roughly based on Minecraft player model)
        float playerWidth = 0.6f;
        float playerHeight = 1.8f;
        float playerDepth = 0.3f;
        
        // Add random variations for dynamic pose and glitch effect
        float glitchVariation = 0.15f; // Much more noticeable variation
        float armSwing = (float)(Math.random() * 0.6 - 0.3); // -0.3 to +0.3 arm swing
        float legSwing = (float)(Math.random() * 0.4 - 0.2); // -0.2 to +0.2 leg swing
        float armHeight = (float)(Math.random() * 0.2 - 0.1); // Arm height variation
        
        // HEAD (8x8x8 pixels scaled)
        float headSize = 0.25f;
        renderBox(matrix, vertexConsumer, 
            -headSize/2, playerHeight - headSize, -headSize/2, 
            headSize/2, playerHeight, headSize/2,
            red, green, blue, opacity, light);
        
        // TORSO (8x12x4 pixels scaled) 
        float torsoWidth = playerWidth * 0.65f;
        float torsoHeight = 0.75f;
        float torsoDepth = playerDepth;
        renderBox(matrix, vertexConsumer,
            -torsoWidth/2, playerHeight - headSize - torsoHeight, -torsoDepth/2,
            torsoWidth/2, playerHeight - headSize, torsoDepth/2,
            red, green, blue, opacity, light);
        
        // ARMS (4x12x4 pixels scaled each)
        float armWidth = 0.125f;
        float armLength = 0.75f; 
        float armDepth = 0.125f;
        float armOffset = torsoWidth/2 + armWidth/2;
        
        // Left arm - with swing and height variation
        float leftArmSwing = armSwing;
        float leftArmY = playerHeight - headSize + armHeight;
        renderBox(matrix, vertexConsumer,
            -armOffset - glitchVariation, leftArmY - armLength, -armDepth/2 + leftArmSwing,
            -armOffset + armWidth - glitchVariation, leftArmY, armDepth/2 + leftArmSwing,
            red, green, blue, opacity * 0.9f, light);
        
        // Right arm - opposite swing for natural movement
        float rightArmSwing = -armSwing;
        float rightArmY = playerHeight - headSize + armHeight * 0.5f; // Different height
        renderBox(matrix, vertexConsumer,
            armOffset - armWidth + glitchVariation, rightArmY - armLength, -armDepth/2 + rightArmSwing,
            armOffset + glitchVariation, rightArmY, armDepth/2 + rightArmSwing,
            red, green, blue, opacity * 0.9f, light);
        
        // LEGS (4x12x4 pixels scaled each)
        float legWidth = 0.125f;
        float legHeight = 0.75f;
        float legDepth = 0.125f;
        float legOffset = legWidth/2;
        float legStartY = playerHeight - headSize - torsoHeight;
        
        // Left leg - with stride and position variation
        float leftLegSwing = legSwing;
        float leftLegX = -legOffset - legWidth + glitchVariation;
        renderBox(matrix, vertexConsumer,
            leftLegX, legStartY - legHeight, -legDepth/2 + leftLegSwing,
            leftLegX + legWidth, legStartY, legDepth/2 + leftLegSwing,
            red, green, blue, opacity * 0.8f, light);
        
        // Right leg - opposite stride for running pose
        float rightLegSwing = -legSwing;
        float rightLegX = legOffset - glitchVariation;
        renderBox(matrix, vertexConsumer,
            rightLegX, legStartY - legHeight, -legDepth/2 + rightLegSwing,
            rightLegX + legWidth, legStartY, legDepth/2 + rightLegSwing,
            red, green, blue, opacity * 0.8f, light);
    }
    
    private static void renderBox(Matrix4f matrix, VertexConsumer vertexConsumer, 
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float red, float green, float blue, float alpha, int light) {
        
        // Render all 6 faces of the box with proper quad ordering
        
        // Front face (positive Z)
        vertexConsumer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).texture(0, 0).light(light).normal(0, 0, 1);
        vertexConsumer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).texture(1, 0).light(light).normal(0, 0, 1);
        vertexConsumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).texture(1, 1).light(light).normal(0, 0, 1);
        vertexConsumer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).texture(0, 1).light(light).normal(0, 0, 1);
        
        // Back face (negative Z)
        vertexConsumer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).texture(0, 0).light(light).normal(0, 0, -1);
        vertexConsumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).texture(1, 0).light(light).normal(0, 0, -1);
        vertexConsumer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).texture(1, 1).light(light).normal(0, 0, -1);
        vertexConsumer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).texture(0, 1).light(light).normal(0, 0, -1);
        
        // Left face (negative X)
        vertexConsumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).texture(0, 0).light(light).normal(-1, 0, 0);
        vertexConsumer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).texture(1, 0).light(light).normal(-1, 0, 0);
        vertexConsumer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).texture(1, 1).light(light).normal(-1, 0, 0);
        vertexConsumer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).texture(0, 1).light(light).normal(-1, 0, 0);
        
        // Right face (positive X)
        vertexConsumer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).texture(0, 0).light(light).normal(1, 0, 0);
        vertexConsumer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).texture(1, 0).light(light).normal(1, 0, 0);
        vertexConsumer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).texture(1, 1).light(light).normal(1, 0, 0);
        vertexConsumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).texture(0, 1).light(light).normal(1, 0, 0);
        
        // Top face (positive Y)
        vertexConsumer.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).texture(0, 0).light(light).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).texture(1, 0).light(light).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).texture(1, 1).light(light).normal(0, 1, 0);
        vertexConsumer.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).texture(0, 1).light(light).normal(0, 1, 0);
        
        // Bottom face (negative Y)
        vertexConsumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).texture(0, 0).light(light).normal(0, -1, 0);
        vertexConsumer.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).texture(1, 0).light(light).normal(0, -1, 0);
        vertexConsumer.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).texture(1, 1).light(light).normal(0, -1, 0);
        vertexConsumer.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).texture(0, 1).light(light).normal(0, -1, 0);
    }
}