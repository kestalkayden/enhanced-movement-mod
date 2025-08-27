package net.fabricmc.EnhancedMovement.client;

import net.fabricmc.EnhancedMovement.NetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class ClientNetworkHandler {
    
    public static void sendDoubleJumpData(double firstJumpY, double doubleJumpY) {
        ClientPlayNetworking.send(new NetworkHandler.DoubleJumpPayload(firstJumpY, doubleJumpY));
    }
    
    public static void sendAfterimageData(UUID playerId, Vec3d startPos, Vec3d endPos, float yaw, float pitch, int imageCount) {
        NetworkHandler.AfterimagePayload payload = new NetworkHandler.AfterimagePayload(
            playerId,
            startPos.x, startPos.y, startPos.z,
            endPos.x, endPos.y, endPos.z,
            yaw, pitch, imageCount
        );
        ClientPlayNetworking.send(payload);
    }
}