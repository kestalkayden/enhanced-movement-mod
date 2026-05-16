package com.enhancedmovement.kestalkayden.client;

import com.enhancedmovement.kestalkayden.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ClientNetworkHandler {

    public static void sendDoubleJumpData(double firstJumpY, double doubleJumpY) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        connection.send(new ServerboundCustomPayloadPacket(
            new NetworkHandler.DoubleJumpPayload(firstJumpY, doubleJumpY)
        ));
    }

    public static void sendAfterimageData(UUID playerId, Vec3 start, Vec3 end, float yaw, float pitch, int imageCount) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;
        connection.send(new ServerboundCustomPayloadPacket(
            new NetworkHandler.AfterimagePayload(
                playerId,
                start.x, start.y, start.z,
                end.x, end.y, end.z,
                yaw, pitch,
                imageCount
            )
        ));
    }
}
