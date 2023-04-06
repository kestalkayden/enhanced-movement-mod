package net.fabricmc.EnhancedMovement;

import net.minecraft.network.PacketByteBuf;

public class Config {
    public boolean isEnableDoubleJump;
    public boolean isEnableDash;
    public boolean isEnableLedgeGrab;
    public boolean isEnableAirDash;
    public boolean isSneakDisableFeatures;
    public int timeDelayDash;
    public int timeCooldownDash;
    public double minimumVerticalVelocity;
    public double fixedJumpBoost;
    public float dashSpeed;
    public float inAirDashSpeed;

    public Config() {
        // Initialize the fields with their default values
        isSneakDisableFeatures = false;
        isEnableDoubleJump = true;
        isEnableAirDash = true;
        isEnableDash = true;
        isEnableLedgeGrab = true;
        timeDelayDash = 400;
        timeCooldownDash = 1300;
        minimumVerticalVelocity = 0.4;
        fixedJumpBoost = 0.4;
        dashSpeed = 1.7f;
        inAirDashSpeed = 1.3f;
    }

    public void readFromPacket(PacketByteBuf buf) {
        isSneakDisableFeatures = buf.readBoolean();
        isEnableDoubleJump = buf.readBoolean();
        isEnableDash = buf.readBoolean();
        isEnableAirDash = buf.readBoolean();
        isEnableLedgeGrab = buf.readBoolean();
        timeDelayDash = buf.readInt();
        timeCooldownDash = buf.readInt();
        minimumVerticalVelocity = buf.readDouble();
        fixedJumpBoost = buf.readDouble();
        dashSpeed = buf.readFloat();
        inAirDashSpeed = buf.readFloat();
    }

    public void writeToPacket(PacketByteBuf buf) {
        buf.writeBoolean(isSneakDisableFeatures);
        buf.writeBoolean(isEnableDoubleJump);
        buf.writeBoolean(isEnableDash);
        buf.writeBoolean(isEnableAirDash);
        buf.writeBoolean(isEnableLedgeGrab);
        buf.writeInt(timeDelayDash);
        buf.writeInt(timeCooldownDash);
        buf.writeDouble(minimumVerticalVelocity);
        buf.writeDouble(fixedJumpBoost);
        buf.writeFloat(dashSpeed);
        buf.writeFloat(inAirDashSpeed);
    }
}
