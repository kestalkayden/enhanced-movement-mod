package com.enhancedmovement.kestalkayden;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkHandler {

    public static final Identifier DOUBLE_JUMP_PACKET_ID =
        Identifier.fromNamespaceAndPath("enhancedmovement", "double_jump");
    public static final Identifier AFTERIMAGE_PACKET_ID =
        Identifier.fromNamespaceAndPath("enhancedmovement", "afterimage");

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
        PayloadTypeRegistry.serverboundPlay().register(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AfterimagePayload.TYPE, AfterimagePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AfterimagePayload.TYPE, AfterimagePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DoubleJumpPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() ->
                recordDoubleJump(player, payload.firstJumpY(), payload.doubleJumpY())
            );
        });

        ServerPlayNetworking.registerGlobalReceiver(AfterimagePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> broadcastAfterimageToNearbyPlayers(player, payload));
        });

        ServerTickEvents.END_SERVER_TICK.register(NetworkHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        if (!playerJumpData.isEmpty()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                handleContinuousProtection(player);
            }
        }
        tickCounter++;
        if (tickCounter % 100 == 0) {
            cleanupOldData();
        }
    }

    private static void recordDoubleJump(ServerPlayer player, double firstJumpY, double doubleJumpY) {
        playerJumpData.put(player.getUUID(), new DoubleJumpData(firstJumpY, doubleJumpY));
    }

    private static void handleContinuousProtection(ServerPlayer player) {
        UUID playerId = player.getUUID();
        DoubleJumpData jumpData = playerJumpData.get(playerId);
        if (jumpData == null || !jumpData.hasProtection) return;

        double currentFallDistance = player.fallDistance;

        if (currentFallDistance > 0.3 && !player.onGround()) {
            double currentY = player.getY();
            double referenceHeight = Math.max(jumpData.firstJumpY, jumpData.doubleJumpY);
            double smartFallDistance = Math.max(0, referenceHeight - currentY);

            if (smartFallDistance <= 3.5) {
                player.fallDistance = 0.0;
            } else {
                player.fallDistance = Math.max(2.0, smartFallDistance * 0.7);
            }
        }

        if (player.onGround() && jumpData.hasProtection) {
            jumpData.hasProtection = false;
        }
    }

    private static void cleanupOldData() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 30000;
        playerJumpData.entrySet().removeIf(entry -> currentTime - entry.getValue().timestamp > maxAge);
    }

    private static void broadcastAfterimageToNearbyPlayers(ServerPlayer dashingPlayer, AfterimagePayload payload) {
        double maxDistance = 64.0;
        Vec3 dashingPlayerPos = dashingPlayer.position();
        MinecraftServer server = dashingPlayer.level().getServer();
        if (server == null) return;

        for (ServerPlayer nearbyPlayer : server.getPlayerList().getPlayers()) {
            double distance = nearbyPlayer.position().distanceTo(dashingPlayerPos);
            if (distance <= maxDistance) {
                ServerPlayNetworking.send(nearbyPlayer, payload);
            }
        }
    }

    public record DoubleJumpPayload(double firstJumpY, double doubleJumpY) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DoubleJumpPayload> TYPE =
            new CustomPacketPayload.Type<>(DOUBLE_JUMP_PACKET_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeDouble(payload.firstJumpY);
                buf.writeDouble(payload.doubleJumpY);
            },
            buf -> new DoubleJumpPayload(buf.readDouble(), buf.readDouble())
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AfterimagePayload(
        UUID playerId,
        double startX, double startY, double startZ,
        double endX, double endY, double endZ,
        float yaw, float pitch,
        int imageCount
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<AfterimagePayload> TYPE =
            new CustomPacketPayload.Type<>(AFTERIMAGE_PACKET_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, AfterimagePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.playerId);
                buf.writeDouble(payload.startX);
                buf.writeDouble(payload.startY);
                buf.writeDouble(payload.startZ);
                buf.writeDouble(payload.endX);
                buf.writeDouble(payload.endY);
                buf.writeDouble(payload.endZ);
                buf.writeFloat(payload.yaw);
                buf.writeFloat(payload.pitch);
                buf.writeInt(payload.imageCount);
            },
            buf -> new AfterimagePayload(
                buf.readUUID(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(),
                buf.readInt()
            )
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
