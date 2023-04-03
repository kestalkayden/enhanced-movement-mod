package net.fabricmc.EnhancedMovement;

import net.minecraft.network.PacketByteBuf;

public class Config {
    public boolean isEnableDoubleJump;
    public boolean isEnableDash;
    public boolean isEnableLedgeGrab;
    public int timeDelayDash;
    public int timeCooldownDash;
    public double minimumVerticalVelocity;
    public double fixedJumpBoost;
    public float dashSpeed;

    public Config() {
        // Initialize the fields with their default values
        isEnableDoubleJump = true;
        isEnableDash = true;
        isEnableLedgeGrab = true;
        timeDelayDash = 450;
        timeCooldownDash = 1000;
        minimumVerticalVelocity = 0.4;
        fixedJumpBoost = 0.4;
        dashSpeed = 1.8f;
    }

    public void readFromPacket(PacketByteBuf buf) {
        isEnableDoubleJump = buf.readBoolean();
        isEnableDash = buf.readBoolean();
        isEnableLedgeGrab = buf.readBoolean();
        timeDelayDash = buf.readInt();
        timeCooldownDash = buf.readInt();
        minimumVerticalVelocity = buf.readDouble();
        fixedJumpBoost = buf.readDouble();
        dashSpeed = buf.readFloat();
    }

    public void writeToPacket(PacketByteBuf buf) {
        buf.writeBoolean(isEnableDoubleJump);
        buf.writeBoolean(isEnableDash);
        buf.writeBoolean(isEnableLedgeGrab);
        buf.writeInt(timeDelayDash);
        buf.writeInt(timeCooldownDash);
        buf.writeDouble(minimumVerticalVelocity);
        buf.writeDouble(fixedJumpBoost);
        buf.writeFloat(dashSpeed);
    }
}
