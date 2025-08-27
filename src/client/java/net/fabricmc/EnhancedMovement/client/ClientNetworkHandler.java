package net.fabricmc.EnhancedMovement.client;

import net.fabricmc.EnhancedMovement.NetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ClientNetworkHandler {
    
    public static void sendDoubleJumpData(double firstJumpY, double doubleJumpY) {
        ClientPlayNetworking.send(new NetworkHandler.DoubleJumpPayload(firstJumpY, doubleJumpY));
    }
}