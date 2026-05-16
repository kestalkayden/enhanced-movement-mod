package com.enhancedmovement.kestalkayden.client;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class AfterimageManager {

    private static final List<AfterimageData> activeAfterimages = new ArrayList<>();
    private static final int MAX_AFTERIMAGES = 50;

    public static class AfterimageData {
        public Vec3 position;
        public float yaw, pitch;
        public long creationTime;
        public long spawnDelay;
        public float maxLifetime;
        public UUID playerId;
        public boolean prismMode = false;

        public AfterimageData(Vec3 position, float yaw, float pitch, float maxLifetime, UUID playerId, long spawnDelay) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
            this.creationTime = System.currentTimeMillis();
            this.spawnDelay = spawnDelay;
            this.maxLifetime = maxLifetime;
            this.playerId = playerId;
        }

        public boolean hasSpawned() {
            return System.currentTimeMillis() >= (creationTime + spawnDelay);
        }

        public long getSpawnTime() {
            return creationTime + spawnDelay;
        }

        public float getOpacity() {
            long currentTime = System.currentTimeMillis();
            if (!hasSpawned()) return 0.0f;
            float age = (currentTime - getSpawnTime()) / 1000.0f;
            if (age >= maxLifetime) return 0.0f;
            float progress = age / maxLifetime;
            return 0.8f * (1.0f - progress);
        }

        public boolean isExpired() {
            return hasSpawned() && getOpacity() <= 0.0f;
        }
    }

    public static void addAfterimages(UUID playerId, Vec3 startPos, Vec3 endPos, float yaw, float pitch, int imageCount) {
        addAfterimages(playerId, startPos, endPos, yaw, pitch, imageCount, 40);
    }

    public static void addSingleAfterimage(UUID playerId, Vec3 position, float yaw, float pitch, int baseLifetimeTensOfMs, long spawnDelay) {
        addSingleAfterimage(playerId, position, yaw, pitch, baseLifetimeTensOfMs, spawnDelay, false);
    }

    public static void addSingleAfterimage(UUID playerId, Vec3 position, float yaw, float pitch, int baseLifetimeTensOfMs, long spawnDelay, boolean prismMode) {
        float baseLifetime = baseLifetimeTensOfMs / 100.0f;
        AfterimageData afterimage = new AfterimageData(position, yaw, pitch, baseLifetime, playerId, spawnDelay);
        afterimage.prismMode = prismMode;
        if (activeAfterimages.size() >= MAX_AFTERIMAGES) {
            activeAfterimages.remove(0);
        }
        activeAfterimages.add(afterimage);
    }

    public static void addAfterimages(UUID playerId, Vec3 startPos, Vec3 endPos, float yaw, float pitch, int imageCount, int baseLifetimeTensOfMs) {
        for (int i = 0; i < imageCount; i++) {
            float linearT = (float) i / (imageCount - 1);
            float smoothT = (float) (1.0 - Math.pow(1.0 - linearT, 1.8));
            float finalT = linearT * 0.3f + smoothT * 0.7f;
            Vec3 position = startPos.lerp(endPos, finalT);

            float baseLifetime = baseLifetimeTensOfMs / 100.0f;
            float progressionFactor = (float) i / (imageCount - 1);
            float additionalTime = progressionFactor * progressionFactor * 0.8f;
            float maxLifetime = baseLifetime + additionalTime;

            long spawnDelay = i * 50L;
            AfterimageData afterimage = new AfterimageData(position, yaw, pitch, maxLifetime, playerId, spawnDelay);
            if (activeAfterimages.size() >= MAX_AFTERIMAGES) {
                activeAfterimages.remove(0);
            }
            activeAfterimages.add(afterimage);
        }
    }

    public static void tick() {
        Iterator<AfterimageData> iterator = activeAfterimages.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExpired()) iterator.remove();
        }
    }

    public static List<AfterimageData> getActiveAfterimages() {
        return activeAfterimages;
    }

    public static void clear() {
        activeAfterimages.clear();
    }
}
