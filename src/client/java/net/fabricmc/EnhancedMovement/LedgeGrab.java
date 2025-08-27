package net.fabricmc.EnhancedMovement;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.fabricmc.EnhancedMovement.config.EnhancedMovementConfig;
import me.shedaniel.autoconfig.AutoConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LedgeGrab {

    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        // Get config
        EnhancedMovementConfig config = AutoConfig.getConfigHolder(EnhancedMovementConfig.class).getConfig();
        
        // Check if ledge grab is enabled
        if (!config.movement.doubleJump.enableLedgeGrab) return;

        boolean jumpKeyIsPressed = client.options.jumpKey.isPressed();
        if (!jumpKeyIsPressed) return;
        if (!isNearLedge(player.getBlockPos())) return;

        if (player.getHungerManager().getFoodLevel() < 6) {
            return;
        }

        float ledgeGrabBoost = 0.5f; // 50% boost

        player.setVelocity(player.getVelocity().x, ledgeGrabBoost, player.getVelocity().z);
        player.velocityModified = true;
        
        player.addExhaustion(0.15f);
    }

    public boolean isNearLedge(@NotNull BlockPos blockPos) {
        // List of blocks which surrounds the player;
        List<BlockPos> playerBlockPos= List.of(
                blockPos.add(-1, 0, 0),
                blockPos.add(+1, 0, 0),
                blockPos.add(0, 0, 0),
                blockPos.add(0, 0, -1),
                blockPos.add(0, 0, 1)
        );

        // Check if any of the surroundings is a valid ledge.
        boolean hasValidLedge = playerBlockPos
                .stream()
                .anyMatch(blockPosItem -> isValidLedge(blockPosItem));

        return hasValidLedge && isEmpty(blockPos.add(0, -1, 0));
    }

    public boolean isValidLedge(BlockPos blockPos) {
        boolean isNotLedgeEmpty = !isEmpty(blockPos);
        boolean isLedgeTopEmpty = isEmpty(blockPos.add(0, 1, 0));

        // Check if 'Ledge' not empty so as not to be grabbing on empty blocks.
        // Check if Ledge's  top block is empty.
        return isNotLedgeEmpty && isLedgeTopEmpty;
    }

    public boolean isEmpty(BlockPos blockPos) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        World world = player.getWorld();
        BlockState blockState = world.getBlockState(blockPos);
        VoxelShape voxelShape = blockState.getCollisionShape(world, blockPos, ShapeContext.of(player));
        return voxelShape.isEmpty();
    }
}
