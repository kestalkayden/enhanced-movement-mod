package net.fabricmc.EnhancedMovement;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class NetworkHandler {
    public static final Identifier DOUBLE_JUMP_PACKET_ID = new Identifier("enhancedmovement", "double_jump");
    public static final Identifier DASH_PACKET_ID = new Identifier("enhancedmovement", "dash");
    public static final Identifier CONFIG_SYNC_PACKET_ID = new Identifier("enhancedmovement", "config_sync");

    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(DOUBLE_JUMP_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                EnhancedMovement.getInstance().performMidAirJump(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(DASH_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            double offsetX = buf.readDouble();
            double offsetY = buf.readDouble();
            double offsetZ = buf.readDouble();

            server.execute(() -> {
                player.setVelocity(player.getVelocity().add(offsetX, offsetY, offsetZ));
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CONFIG_SYNC_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            Config config = new Config();
            config.readFromPacket(buf);
            EnhancedMovement.getInstance().setConfig(config);
        });
    }

    public static void sendDoubleJumpPacket(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            ServerPlayNetworking.send((ServerPlayerEntity) player, DOUBLE_JUMP_PACKET_ID, new PacketByteBuf(Unpooled.buffer()));
        }
    }

    public static void sendDashPacket(PlayerEntity player, double offsetX, double offsetY, double offsetZ) {
        if (player instanceof ServerPlayerEntity) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeDouble(offsetX);
            buf.writeDouble(offsetY);
            buf.writeDouble(offsetZ);
            ServerPlayNetworking.send((ServerPlayerEntity) player, DASH_PACKET_ID, buf);
        }
    }

    public static void sendConfigSyncPacket(ServerPlayerEntity player, PacketByteBuf configData) {
        ServerPlayNetworking.send(player, CONFIG_SYNC_PACKET_ID, configData);
    }
}
