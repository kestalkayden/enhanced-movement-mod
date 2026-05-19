package com.enhancedmovement.kestalkayden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.UUID;

public class AfterimageRenderer {

    private static final Minecraft client = Minecraft.getInstance();

    /** 1x1 white pixel texture so we can route through entityTranslucentEmissive without
     *  altering the on-screen color. The previous renderer used RenderTypes.debugQuads(),
     *  which uses the `pipeline/debug_quads` shader program. Iris/shader packs do NOT
     *  include debug pipelines in their override lists (they're meant for F3 overlays),
     *  so under shaders the trail draws silently fail. entity_translucent_emissive
     *  uses the entity rendering pipeline which every shader pack handles. Blend, depth,
     *  and cull semantics happen to match exactly what debug_quads provided. */
    private static final Identifier WHITE_TEXTURE =
        Identifier.fromNamespaceAndPath("enhancedmovement", "textures/effect/white.png");

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
        RenderType renderType = RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderType);

        float[] colors = getPlayerColors(afterimage);
        float red = colors[0];
        float green = colors[1];
        float blue = colors[2];

        renderPlayerSilhouetteParts(matrix, vertexConsumer, red, green, blue, opacity, light);
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
                                                    float red, float green, float blue, float opacity, int light) {
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
            red, green, blue, opacity, light);

        float torsoWidth = playerWidth * 0.65f;
        float torsoHeight = 0.75f;
        float torsoDepth = playerDepth;
        renderBox(matrix, vc,
            -torsoWidth / 2, playerHeight - headSize - torsoHeight, -torsoDepth / 2,
            torsoWidth / 2, playerHeight - headSize, torsoDepth / 2,
            red, green, blue, opacity, light);

        float armWidth = 0.125f;
        float armLength = 0.75f;
        float armDepth = 0.125f;
        float armOffset = torsoWidth / 2 + armWidth / 2;

        float leftArmY = playerHeight - headSize + armHeight;
        renderBox(matrix, vc,
            -armOffset - glitchVariation, leftArmY - armLength, -armDepth / 2 + armSwing,
            -armOffset + armWidth - glitchVariation, leftArmY, armDepth / 2 + armSwing,
            red, green, blue, opacity * 0.9f, light);

        float rightArmY = playerHeight - headSize + armHeight * 0.5f;
        renderBox(matrix, vc,
            armOffset - armWidth + glitchVariation, rightArmY - armLength, -armDepth / 2 - armSwing,
            armOffset + glitchVariation, rightArmY, armDepth / 2 - armSwing,
            red, green, blue, opacity * 0.9f, light);

        float legWidth = 0.125f;
        float legHeight = 0.75f;
        float legDepth = 0.125f;
        float legOffset = legWidth / 2;
        float legStartY = playerHeight - headSize - torsoHeight;

        float leftLegX = -legOffset - legWidth + glitchVariation;
        renderBox(matrix, vc,
            leftLegX, legStartY - legHeight, -legDepth / 2 + legSwing,
            leftLegX + legWidth, legStartY, legDepth / 2 + legSwing,
            red, green, blue, opacity * 0.8f, light);

        float rightLegX = legOffset - glitchVariation;
        renderBox(matrix, vc,
            rightLegX, legStartY - legHeight, -legDepth / 2 - legSwing,
            rightLegX + legWidth, legStartY, legDepth / 2 - legSwing,
            red, green, blue, opacity * 0.8f, light);
    }

    private static void renderBox(Matrix4f matrix, VertexConsumer vc,
                                  float x1, float y1, float z1, float x2, float y2, float z2,
                                  float red, float green, float blue, float alpha, int light) {
        // 6 faces, 4 vertices each. Vertex format now includes UV (constant 0.5,0.5 since
        // the texture is uniform white), overlay (none), light (passed through), and a
        // per-face normal vector — required by the entity_translucent_emissive pipeline.
        int overlay = OverlayTexture.NO_OVERLAY;
        // Back face (-Z)
        addVertex(vc, matrix, x2, y1, z1, red, green, blue, alpha, overlay, light, 0, 0, -1);
        addVertex(vc, matrix, x1, y1, z1, red, green, blue, alpha, overlay, light, 0, 0, -1);
        addVertex(vc, matrix, x1, y2, z1, red, green, blue, alpha, overlay, light, 0, 0, -1);
        addVertex(vc, matrix, x2, y2, z1, red, green, blue, alpha, overlay, light, 0, 0, -1);
        // Left face (-X)
        addVertex(vc, matrix, x1, y1, z1, red, green, blue, alpha, overlay, light, -1, 0, 0);
        addVertex(vc, matrix, x1, y1, z2, red, green, blue, alpha, overlay, light, -1, 0, 0);
        addVertex(vc, matrix, x1, y2, z2, red, green, blue, alpha, overlay, light, -1, 0, 0);
        addVertex(vc, matrix, x1, y2, z1, red, green, blue, alpha, overlay, light, -1, 0, 0);
        // Right face (+X)
        addVertex(vc, matrix, x2, y1, z2, red, green, blue, alpha, overlay, light, 1, 0, 0);
        addVertex(vc, matrix, x2, y1, z1, red, green, blue, alpha, overlay, light, 1, 0, 0);
        addVertex(vc, matrix, x2, y2, z1, red, green, blue, alpha, overlay, light, 1, 0, 0);
        addVertex(vc, matrix, x2, y2, z2, red, green, blue, alpha, overlay, light, 1, 0, 0);
        // Top face (+Y)
        addVertex(vc, matrix, x1, y2, z2, red, green, blue, alpha, overlay, light, 0, 1, 0);
        addVertex(vc, matrix, x2, y2, z2, red, green, blue, alpha, overlay, light, 0, 1, 0);
        addVertex(vc, matrix, x2, y2, z1, red, green, blue, alpha, overlay, light, 0, 1, 0);
        addVertex(vc, matrix, x1, y2, z1, red, green, blue, alpha, overlay, light, 0, 1, 0);
        // Bottom face (-Y)
        addVertex(vc, matrix, x1, y1, z1, red, green, blue, alpha, overlay, light, 0, -1, 0);
        addVertex(vc, matrix, x2, y1, z1, red, green, blue, alpha, overlay, light, 0, -1, 0);
        addVertex(vc, matrix, x2, y1, z2, red, green, blue, alpha, overlay, light, 0, -1, 0);
        addVertex(vc, matrix, x1, y1, z2, red, green, blue, alpha, overlay, light, 0, -1, 0);
        // Front face (+Z)
        addVertex(vc, matrix, x1, y1, z2, red, green, blue, alpha, overlay, light, 0, 0, 1);
        addVertex(vc, matrix, x2, y1, z2, red, green, blue, alpha, overlay, light, 0, 0, 1);
        addVertex(vc, matrix, x2, y2, z2, red, green, blue, alpha, overlay, light, 0, 0, 1);
        addVertex(vc, matrix, x1, y2, z2, red, green, blue, alpha, overlay, light, 0, 0, 1);
    }

    private static void addVertex(VertexConsumer vc, Matrix4f matrix,
                                  float x, float y, float z,
                                  float r, float g, float b, float a,
                                  int overlay, int light,
                                  float nx, float ny, float nz) {
        vc.addVertex(matrix, x, y, z)
            .setColor(r, g, b, a)
            .setUv(0.5f, 0.5f)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(nx, ny, nz);
    }
}
