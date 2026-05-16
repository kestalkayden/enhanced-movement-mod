package com.enhancedmovement.kestalkayden.client;

import com.enhancedmovement.kestalkayden.NetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ClientNetworkHandler {

    public static void sendDoubleJumpData(double firstJumpY, double doubleJumpY) {
        ClientPlayNetworking.send(new NetworkHandler.DoubleJumpPayload(firstJumpY, doubleJumpY));
    }

    public static void sendAfterimageData(UUID playerId, Vec3 start, Vec3 end, float yaw, float pitch, int imageCount) {
        ClientPlayNetworking.send(new NetworkHandler.AfterimagePayload(
            playerId,
            start.x, start.y, start.z,
            end.x, end.y, end.z,
            yaw, pitch,
            imageCount
        ));
    }
}
