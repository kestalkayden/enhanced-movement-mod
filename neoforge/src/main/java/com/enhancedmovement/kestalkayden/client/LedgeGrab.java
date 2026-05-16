package com.enhancedmovement.kestalkayden.client;

import com.enhancedmovement.kestalkayden.EnhancedMovement;
import com.enhancedmovement.kestalkayden.config.EnhancedMovementConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class LedgeGrab {

    public void tick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        EnhancedMovementConfig config = EnhancedMovement.CONFIG;
        if (!config.movement.doubleJump.enableLedgeGrab) return;

        boolean jumpKeyIsPressed = client.options.keyJump.isDown();
        if (!jumpKeyIsPressed) return;
        if (!isNearLedge(player.blockPosition())) return;

        if (player.getFoodData().getFoodLevel() < 6) return;

        float ledgeGrabBoost = 0.5f;
        player.setDeltaMovement(player.getDeltaMovement().x, ledgeGrabBoost, player.getDeltaMovement().z);
        player.causeFoodExhaustion(0.15f);
    }

    private boolean isNearLedge(BlockPos blockPos) {
        List<BlockPos> playerBlockPos = List.of(
            blockPos.offset(-1, 0, 0),
            blockPos.offset(1, 0, 0),
            blockPos.offset(0, 0, 0),
            blockPos.offset(0, 0, -1),
            blockPos.offset(0, 0, 1)
        );
        boolean hasValidLedge = playerBlockPos.stream().anyMatch(this::isValidLedge);
        return hasValidLedge && isEmpty(blockPos.offset(0, -1, 0));
    }

    private boolean isValidLedge(BlockPos blockPos) {
        return !isEmpty(blockPos) && isEmpty(blockPos.offset(0, 1, 0));
    }

    private boolean isEmpty(BlockPos blockPos) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return true;
        Level world = player.level();
        BlockState blockState = world.getBlockState(blockPos);
        VoxelShape voxelShape = blockState.getCollisionShape(world, blockPos, CollisionContext.of(player));
        return voxelShape.isEmpty();
    }
}
