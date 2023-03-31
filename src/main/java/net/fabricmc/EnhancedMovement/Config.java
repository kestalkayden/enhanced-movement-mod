package net.fabricmc.EnhancedMovement;

import net.minecraft.network.PacketByteBuf;

public class Config {
    public boolean isEnableDoubleJump;
    public boolean isEnableDash;
    public int timeDelayDash;
    public int timeCooldownDash;
    public double minimumVerticalVelocity;
    public double fixedJumpBoost;
    public float dashSpeed;

    public Config() {
        // Initialize the fields with their default values
        isEnableDoubleJump = true;
        isEnableDash = true;
        timeDelayDash = 400;
        timeCooldownDash = 1000;
        minimumVerticalVelocity = 0.3;
        fixedJumpBoost = 0.3;
        dashSpeed = 1.75f;
    }

    public void readFromPacket(PacketByteBuf buf) {
        isEnableDoubleJump = buf.readBoolean();
        isEnableDash = buf.readBoolean();
        timeDelayDash = buf.readInt();
        timeCooldownDash = buf.readInt();
        minimumVerticalVelocity = buf.readDouble();
        fixedJumpBoost = buf.readDouble();
        dashSpeed = buf.readFloat();
    }

    public void writeToPacket(PacketByteBuf buf) {
        buf.writeBoolean(isEnableDoubleJump);
        buf.writeBoolean(isEnableDash);
        buf.writeInt(timeDelayDash);
        buf.writeInt(timeCooldownDash);
        buf.writeDouble(minimumVerticalVelocity);
        buf.writeDouble(fixedJumpBoost);
        buf.writeFloat(dashSpeed);
    }
}
