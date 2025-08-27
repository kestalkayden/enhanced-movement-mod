package net.fabricmc.EnhancedMovement;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkHandler {
    
    public static final Identifier DOUBLE_JUMP_PACKET_ID = Identifier.of("enhancedmovement", "double_jump");
    public static final Identifier AFTERIMAGE_PACKET_ID = Identifier.of("enhancedmovement", "afterimage");
    
    // Track double jump data for smart fall damage calculation
    private static final Map<UUID, DoubleJumpData> playerJumpData = new HashMap<>();
    private static int tickCounter = 0;
    
    public static class DoubleJumpData {
        public double firstJumpY;
        public double doubleJumpY;
        public boolean hasProtection;
        public long timestamp;
        
        public DoubleJumpData(double firstJumpY, double doubleJumpY) {
            this.firstJumpY = firstJumpY;
            this.doubleJumpY = doubleJumpY;
            this.hasProtection = true;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static void initialize() {
        // Register payload types
        PayloadTypeRegistry.playC2S().register(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AfterimagePayload.ID, AfterimagePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AfterimagePayload.ID, AfterimagePayload.CODEC);
        
        // Register server-side packet handler
        ServerPlayNetworking.registerGlobalReceiver(DoubleJumpPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            
            // Execute on server thread
            context.server().execute(() -> {
                recordDoubleJump(player, payload.firstJumpY(), payload.doubleJumpY());
            });
        });
        
        // Register afterimage packet handler (server receives from client, broadcasts to others)
        ServerPlayNetworking.registerGlobalReceiver(AfterimagePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            
            context.server().execute(() -> {
                broadcastAfterimageToNearbyPlayers(player, payload);
            });
        });
        
        ServerTickEvents.END_SERVER_TICK.register(NetworkHandler::onServerTick);
    }
    
    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter >= 1) {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                handleSmartFallDamage(player);
            }
            // Cleanup old data every 100 ticks (5 seconds)
            if (tickCounter % 100 == 0) {
                cleanupOldData();
            }
            tickCounter = 0;
        }
    }
    
    private static void recordDoubleJump(ServerPlayerEntity player, double firstJumpY, double doubleJumpY) {
        playerJumpData.put(player.getUuid(), new DoubleJumpData(firstJumpY, doubleJumpY));
    }
    
    private static void handleSmartFallDamage(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        double currentFallDistance = player.fallDistance;
        DoubleJumpData jumpData = playerJumpData.get(playerId);
        
        // If player is falling and has significant fall distance, apply smart dampening
        if (currentFallDistance > 0 && player.isOnGround() && jumpData != null && jumpData.hasProtection) {
            // Calculate smart fall distance based on jump heights
            double currentY = player.getY();
            double referenceHeight = Math.min(jumpData.firstJumpY, jumpData.doubleJumpY);
            double smartFallDistance = Math.max(0, referenceHeight - currentY);
            
            // For small falls (≤8.0 blocks), provide complete protection
            if (smartFallDistance <= 8.0) {
                player.fallDistance = 0.0f;
            } else {
                // For larger falls, apply proportional damage reduction
                double normalFallDistance = currentFallDistance;
                if (normalFallDistance > 0) {
                    double reductionRatio = smartFallDistance / normalFallDistance;
                    float reducedFallDistance = (float) (currentFallDistance * reductionRatio);
                    player.fallDistance = reducedFallDistance;
                }
            }
            
            // Clear the protection data since we've used it
            jumpData.hasProtection = false;
        }
    }
    
    private static void cleanupOldData() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 30000; // 30 seconds
        
        playerJumpData.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().timestamp > maxAge
        );
    }
    
    private static void broadcastAfterimageToNearbyPlayers(ServerPlayerEntity dashingPlayer, AfterimagePayload payload) {
        // Broadcast to all players within 64 blocks (including the dashing player for self-visibility)
        double maxDistance = 64.0;
        Vec3d dashingPlayerPos = dashingPlayer.getPos();
        
        for (ServerPlayerEntity nearbyPlayer : dashingPlayer.getServer().getPlayerManager().getPlayerList()) {
            double distance = nearbyPlayer.getPos().distanceTo(dashingPlayerPos);
            if (distance <= maxDistance) {
                ServerPlayNetworking.send(nearbyPlayer, payload);
            }
        }
    }
    
    public record DoubleJumpPayload(double firstJumpY, double doubleJumpY) implements CustomPayload {
        public static final CustomPayload.Id<DoubleJumpPayload> ID = new CustomPayload.Id<>(DOUBLE_JUMP_PACKET_ID);
        
        public static final PacketCodec<PacketByteBuf, DoubleJumpPayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeDouble(value.firstJumpY);
                buf.writeDouble(value.doubleJumpY);
            },
            buf -> new DoubleJumpPayload(buf.readDouble(), buf.readDouble())
        );
        
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    
    public record AfterimagePayload(
        UUID playerId,
        double startX, double startY, double startZ,
        double endX, double endY, double endZ,
        float yaw, float pitch,
        int imageCount
    ) implements CustomPayload {
        public static final CustomPayload.Id<AfterimagePayload> ID = new CustomPayload.Id<>(AFTERIMAGE_PACKET_ID);
        
        public static final PacketCodec<PacketByteBuf, AfterimagePayload> CODEC = PacketCodec.of(
            (value, buf) -> {
                buf.writeUuid(value.playerId);
                buf.writeDouble(value.startX);
                buf.writeDouble(value.startY);
                buf.writeDouble(value.startZ);
                buf.writeDouble(value.endX);
                buf.writeDouble(value.endY);
                buf.writeDouble(value.endZ);
                buf.writeFloat(value.yaw);
                buf.writeFloat(value.pitch);
                buf.writeInt(value.imageCount);
            },
            buf -> new AfterimagePayload(
                buf.readUuid(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(),
                buf.readInt()
            )
        );
        
        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
