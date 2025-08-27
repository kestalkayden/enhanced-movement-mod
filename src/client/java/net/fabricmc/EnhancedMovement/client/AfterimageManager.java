package net.fabricmc.EnhancedMovement.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class AfterimageManager {
    
    private static final List<AfterimageData> activeAfterimages = new ArrayList<>();
    private static final int MAX_AFTERIMAGES = 50; // Total limit across all players
    
    public static class AfterimageData {
        public Vec3d position;
        public float yaw, pitch;
        public long creationTime; // When this data was created
        public long spawnDelay; // How long to wait before showing
        public float maxLifetime; // How long this afterimage should live
        public UUID playerId;
        public boolean prismMode = false;
        
        public AfterimageData(Vec3d position, float yaw, float pitch, float maxLifetime, UUID playerId, long spawnDelay) {
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
            
            // Don't show until spawn delay has passed
            if (!hasSpawned()) {
                return 0.0f;
            }
            
            float age = (currentTime - getSpawnTime()) / 1000.0f;
            
            if (age >= maxLifetime) {
                return 0.0f;
            }
            
            // Fade from 0.8 to 0.0 over the lifetime
            float progress = age / maxLifetime;
            return 0.8f * (1.0f - progress);
        }
        
        public boolean isExpired() {
            // Don't remove until both spawned and faded out
            return hasSpawned() && getOpacity() <= 0.0f;
        }
    }
    
    public static void addAfterimages(UUID playerId, Vec3d startPos, Vec3d endPos, float yaw, float pitch, int imageCount) {
        addAfterimages(playerId, startPos, endPos, yaw, pitch, imageCount, 40); // Default 400ms base
    }
    
    public static void addSingleAfterimage(UUID playerId, Vec3d position, float yaw, float pitch, int baseLifetimeTensOfMs, long spawnDelay) {
        addSingleAfterimage(playerId, position, yaw, pitch, baseLifetimeTensOfMs, spawnDelay, false);
    }
    
    public static void addSingleAfterimage(UUID playerId, Vec3d position, float yaw, float pitch, int baseLifetimeTensOfMs, long spawnDelay, boolean prismMode) {
        float baseLifetime = baseLifetimeTensOfMs / 100.0f; // Convert tens of ms to seconds
        
        AfterimageData afterimage = new AfterimageData(position, yaw, pitch, baseLifetime, playerId, spawnDelay);
        afterimage.prismMode = prismMode;
        
        // Add to list, but respect max limit
        if (activeAfterimages.size() >= MAX_AFTERIMAGES) {
            // Remove oldest afterimage
            activeAfterimages.remove(0);
        }
        
        activeAfterimages.add(afterimage);
    }
    
    public static void addAfterimages(UUID playerId, Vec3d startPos, Vec3d endPos, float yaw, float pitch, int imageCount, int baseLifetimeTensOfMs) {
        // Calculate positions along the dash path with non-linear spacing
        for (int i = 0; i < imageCount; i++) {
            float linearT = (float) i / (imageCount - 1); // 0.0 to 1.0
            
            // Create exponential spacing: very spread out near origin, compressed near target
            // Early afterimages get 2-3x normal spacing, later ones compress toward linear
            float spacingMultiplier = 1.0f + (1.0f - linearT) * 2.0f; // 3.0x at start, 1.0x at end
            float adjustedT = linearT * spacingMultiplier;
            
            // Apply a smoothing function to prevent hard transitions
            float smoothT = (float)(1.0 - Math.pow(1.0 - linearT, 1.8)); // Smooth curve
            
            // Blend the two approaches
            float finalT = linearT * 0.3f + smoothT * 0.7f; // Favor the smooth curve
            
            Vec3d position = startPos.lerp(endPos, finalT);
            
            // Each afterimage further along the path lasts progressively longer
            float baseLifetime = baseLifetimeTensOfMs / 100.0f; // Convert tens of ms to seconds
            // More dramatic progression - later afterimages last much longer
            float progressionFactor = (float) i / (imageCount - 1); // 0.0 to 1.0
            float additionalTime = progressionFactor * progressionFactor * 0.8f; // Quadratic progression, up to +800ms
            float maxLifetime = baseLifetime + additionalTime;
            
            // Add staggered spawn delays - 50ms per afterimage for natural progression
            long spawnDelay = i * 50L; // 0ms, 50ms, 100ms, 150ms, etc.
            
            AfterimageData afterimage = new AfterimageData(position, yaw, pitch, maxLifetime, playerId, spawnDelay);
            
            // Add to list, but respect max limit
            if (activeAfterimages.size() >= MAX_AFTERIMAGES) {
                // Remove oldest afterimage
                activeAfterimages.remove(0);
            }
            
            activeAfterimages.add(afterimage);
        }
    }
    
    public static void tick() {
        // Remove expired afterimages
        Iterator<AfterimageData> iterator = activeAfterimages.iterator();
        while (iterator.hasNext()) {
            AfterimageData afterimage = iterator.next();
            if (afterimage.isExpired()) {
                iterator.remove();
            }
        }
    }
    
    public static List<AfterimageData> getActiveAfterimages() {
        return activeAfterimages;
    }
    
    public static void clear() {
        activeAfterimages.clear();
    }
}