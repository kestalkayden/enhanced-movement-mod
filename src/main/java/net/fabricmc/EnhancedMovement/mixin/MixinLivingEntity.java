package net.fabricmc.EnhancedMovement.mixin;

import net.fabricmc.EnhancedMovement.EnhancedMovement;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.text.Text;

@Pseudo
@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    protected MixinLivingEntity() {
        super(null, null);
    }

    @Inject(method = "fall", at = @At("HEAD"), cancellable = true)
    private void onFall(double heightDifference, boolean onGround, BlockState block, BlockPos pos, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            EnhancedMovement instance = EnhancedMovement.getInstance();
            if (player.equals(instance.getClient().player) && instance.hasPerformedMidAirJump()) {
                ci.cancel();
                this.fallDistance = 0.0f;
            }
            
        }
        
    }
}
