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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkHandler {
    
    public static final Identifier DOUBLE_JUMP_PACKET_ID = Identifier.of("enhancedmovement", "double_jump");
    
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
        // Register the payload type
        PayloadTypeRegistry.playC2S().register(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC);
        
        // Register server-side packet handler
        ServerPlayNetworking.registerGlobalReceiver(DoubleJumpPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            
            // Execute on server thread
            context.server().execute(() -> {
                recordDoubleJump(player, payload.firstJumpY(), payload.doubleJumpY());
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
}
