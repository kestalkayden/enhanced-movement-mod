package com.enhancedmovement.kestalkayden;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = EnhancedMovement.MOD_ID)
public class NetworkHandler {

    public static final ResourceLocation DOUBLE_JUMP_PACKET_ID =
        ResourceLocation.fromNamespaceAndPath("enhancedmovement", "double_jump");
    public static final ResourceLocation AFTERIMAGE_PACKET_ID =
        ResourceLocation.fromNamespaceAndPath("enhancedmovement", "afterimage");

    private static final Map<UUID, DoubleJumpData> playerJumpData = new HashMap<>();
    private static int tickCounter = 0;

    // Set from client side at init time; default no-op keeps the dedicated server safe.
    public static Consumer<AfterimagePayload> clientAfterimageReceiver = payload -> {};

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

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("enhancedmovement").versioned("1");

        registrar.playToServer(
            DoubleJumpPayload.TYPE,
            DoubleJumpPayload.STREAM_CODEC,
            NetworkHandler::handleDoubleJumpPayload
        );

        registrar.playBidirectional(
            AfterimagePayload.TYPE,
            AfterimagePayload.STREAM_CODEC,
            new DirectionalPayloadHandler<>(
                NetworkHandler::handleAfterimagePayloadClient,
                NetworkHandler::handleAfterimagePayloadServer
            )
        );
    }

    private static void handleDoubleJumpPayload(DoubleJumpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                recordDoubleJump(serverPlayer, payload.firstJumpY(), payload.doubleJumpY());
            }
        });
    }

    private static void handleAfterimagePayloadServer(AfterimagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                broadcastAfterimageToNearbyPlayers(serverPlayer, payload);
            }
        });
    }

    private static void handleAfterimagePayloadClient(AfterimagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> clientAfterimageReceiver.accept(payload));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        MinecraftServer server = event.getServer();
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
                player.fallDistance = 0.0f;
            } else {
                player.fallDistance = (float) Math.max(2.0, smartFallDistance * 0.7);
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
            if (nearbyPlayer.position().distanceTo(dashingPlayerPos) <= maxDistance) {
                PacketDistributor.sendToPlayer(nearbyPlayer, payload);
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
