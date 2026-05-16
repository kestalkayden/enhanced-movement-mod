package com.enhancedmovement.kestalkayden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

public class AfterimageRenderer {

    private static final Minecraft client = Minecraft.getInstance();

    public static void renderAfterimage(
        PoseStack matrices,
        MultiBufferSource vertexConsumers,
        AfterimageManager.AfterimageData afterimage,
        int light
    ) {
        matrices.pushPose();

        Vec3 cameraPos = client.gameRenderer.getMainCamera().position();
        Vec3 offsetPos = afterimage.position;

        Vec3 cameraToAfterimage = offsetPos.subtract(cameraPos);
        if (cameraToAfterimage.length() < 2.0) {
            Vec3 offsetDirection = cameraToAfterimage.normalize();
            offsetPos = cameraPos.add(offsetDirection.scale(2.0));
        }

        matrices.translate(
            offsetPos.x - cameraPos.x,
            offsetPos.y - cameraPos.y,
            offsetPos.z - cameraPos.z
        );

        float opacity = afterimage.getOpacity();
        renderPlayerSilhouette(matrices, vertexConsumers, opacity, light, afterimage);

        matrices.popPose();
    }

    private static void renderPlayerSilhouette(PoseStack matrices, MultiBufferSource vertexConsumers, float opacity, int light, AfterimageManager.AfterimageData afterimage) {
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderTypes.debugQuads());

        float[] colors = getPlayerColors(afterimage);
        float red = colors[0];
        float green = colors[1];
        float blue = colors[2];

        renderPlayerSilhouetteParts(matrix, vertexConsumer, red, green, blue, opacity);
    }

    private static float[] getPlayerColors(AfterimageManager.AfterimageData afterimage) {
        String playerName = getPlayerName(afterimage.playerId);
        float[] specialColors = getSpecialPlayerColors(playerName);
        if (specialColors != null) return specialColors;
        return afterimage.prismMode ? getPrismColors(afterimage) : getMatrixColors();
    }

    private static String getPlayerName(UUID playerId) {
        if (client.level != null) {
            var player = client.level.getPlayerByUUID(playerId);
            if (player != null) return player.getName().getString();
        }
        return "";
    }

    private static float[] getSpecialPlayerColors(String playerName) {
        if (playerName == null) return null;
        if ("kestalkayden".equals(playerName.toLowerCase())) {
            return new float[]{1.0f, 0.0f, 0.0f};
        }
        return null;
    }

    private static float[] getMatrixColors() {
        float red = (float) (Math.random() * 0.1);
        float green = 0.8f + (float) (Math.random() * 0.2 - 0.1);
        float blue = 0.9f + (float) (Math.random() * 0.1);
        return new float[]{red, green, blue};
    }

    private static float[] getPrismColors(AfterimageManager.AfterimageData afterimage) {
        long timeSinceCreation = System.currentTimeMillis() - afterimage.creationTime;
        float cycleDuration = 1250.0f;
        float cycleProgress = (timeSinceCreation % (long) cycleDuration) / cycleDuration;
        float startHue = 240.0f / 360.0f;
        float hue = (startHue + cycleProgress) % 1.0f;
        return hsvToRgb(hue, 0.9f, 1.0f);
    }

    private static float[] hsvToRgb(float hue, float saturation, float value) {
        int i = (int) (hue * 6);
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

    private static void renderPlayerSilhouetteParts(Matrix4f matrix, VertexConsumer vc,
                                                    float red, float green, float blue, float opacity) {
        float playerWidth = 0.6f;
        float playerHeight = 1.8f;
        float playerDepth = 0.3f;

        float glitchVariation = 0.15f;
        float armSwing = (float) (Math.random() * 0.6 - 0.3);
        float legSwing = (float) (Math.random() * 0.4 - 0.2);
        float armHeight = (float) (Math.random() * 0.2 - 0.1);

        float headSize = 0.25f;
        renderBox(matrix, vc,
            -headSize / 2, playerHeight - headSize, -headSize / 2,
            headSize / 2, playerHeight, headSize / 2,
            red, green, blue, opacity);

        float torsoWidth = playerWidth * 0.65f;
        float torsoHeight = 0.75f;
        float torsoDepth = playerDepth;
        renderBox(matrix, vc,
            -torsoWidth / 2, playerHeight - headSize - torsoHeight, -torsoDepth / 2,
            torsoWidth / 2, playerHeight - headSize, torsoDepth / 2,
            red, green, blue, opacity);

        float armWidth = 0.125f;
        float armLength = 0.75f;
        float armDepth = 0.125f;
        float armOffset = torsoWidth / 2 + armWidth / 2;

        float leftArmY = playerHeight - headSize + armHeight;
        renderBox(matrix, vc,
            -armOffset - glitchVariation, leftArmY - armLength, -armDepth / 2 + armSwing,
            -armOffset + armWidth - glitchVariation, leftArmY, armDepth / 2 + armSwing,
            red, green, blue, opacity * 0.9f);

        float rightArmY = playerHeight - headSize + armHeight * 0.5f;
        renderBox(matrix, vc,
            armOffset - armWidth + glitchVariation, rightArmY - armLength, -armDepth / 2 - armSwing,
            armOffset + glitchVariation, rightArmY, armDepth / 2 - armSwing,
            red, green, blue, opacity * 0.9f);

        float legWidth = 0.125f;
        float legHeight = 0.75f;
        float legDepth = 0.125f;
        float legOffset = legWidth / 2;
        float legStartY = playerHeight - headSize - torsoHeight;

        float leftLegX = -legOffset - legWidth + glitchVariation;
        renderBox(matrix, vc,
            leftLegX, legStartY - legHeight, -legDepth / 2 + legSwing,
            leftLegX + legWidth, legStartY, legDepth / 2 + legSwing,
            red, green, blue, opacity * 0.8f);

        float rightLegX = legOffset - glitchVariation;
        renderBox(matrix, vc,
            rightLegX, legStartY - legHeight, -legDepth / 2 - legSwing,
            rightLegX + legWidth, legStartY, legDepth / 2 - legSwing,
            red, green, blue, opacity * 0.8f);
    }

    private static void renderBox(Matrix4f matrix, VertexConsumer vc,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  float red, float green, float blue, float alpha) {
        // 6 faces, 4 vertices each, position + color only (debugQuads vertex format)
        // Back face (-Z)
        vc.addVertex(matrix, x2, y1, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x1, y2, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y2, z1).setColor(red, green, blue, alpha);
        // Left face (-X)
        vc.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x1, y1, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x1, y2, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x1, y2, z1).setColor(red, green, blue, alpha);
        // Right face (+X)
        vc.addVertex(matrix, x2, y1, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y1, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y2, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
        // Top face (+Y)
        vc.addVertex(matrix, x1, y2, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y2, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x1, y2, z1).setColor(red, green, blue, alpha);
        // Bottom face (-Y)
        vc.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y1, z1).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y1, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x1, y1, z2).setColor(red, green, blue, alpha);
        // Front face (+Z)
        vc.addVertex(matrix, x1, y1, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y1, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
        vc.addVertex(matrix, x1, y2, z2).setColor(red, green, blue, alpha);
    }
}
