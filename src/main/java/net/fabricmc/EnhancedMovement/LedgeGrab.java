package net.fabricmc.EnhancedMovement;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LedgeGrab {

    public void tick() {

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        boolean jumpKeyIsPressed = client.options.jumpKey.isPressed();
        if (!jumpKeyIsPressed) return;
        if (!isNearLedge(player.getBlockPos())) return;


        player.setVelocity(player.getVelocity().x, 0.4f, player.getVelocity().z);
        // Stamina Player's consumption
        player.addExhaustion(3000000.0F);
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
        BlockState blockState = player.world.getBlockState(blockPos);
        VoxelShape voxelShape = blockState.getCollisionShape(world, blockPos, ShapeContext.of(player));
        return voxelShape.isEmpty();
    }
}
