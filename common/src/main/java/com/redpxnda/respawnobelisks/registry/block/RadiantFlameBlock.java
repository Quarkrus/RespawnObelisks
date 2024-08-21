package com.redpxnda.respawnobelisks.registry.block;

import com.redpxnda.respawnobelisks.config.RespawnObelisksConfig;
import com.redpxnda.respawnobelisks.registry.ModRegistries;
import com.redpxnda.respawnobelisks.registry.block.entity.RadiantFlameBlockEntity;
import com.redpxnda.respawnobelisks.util.CoreUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class RadiantFlameBlock extends Block implements BlockEntityProvider { // todo prevent sprint particles - Entity#spawnSprintParticle
    private static final VoxelShape HITBOX = Block.createCuboidShape(1.5D, 1.0D, 1.5D, 14.5D, 16.0D, 14.5D);

    public RadiantFlameBlock(Settings settings) {
        super(settings);
    }

    public void onPlaced(World level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof RadiantFlameBlockEntity blockEntity) {
            blockEntity.setCharge(CoreUtils.getCharge(stack.getOrCreateNbt()));
            blockEntity.setInitialCharge(blockEntity.getCharge());
            blockEntity.setOwner(placer.getUuid());
        }

        if (placer instanceof ServerPlayerEntity player) {
            Vec3d centerPos = pos.toCenterPos();
            Vec3d playerPos = player.getPos();
            float angle = (float) Math.atan2(playerPos.z - centerPos.z, playerPos.x - centerPos.x); // finding spawn angle based on where player is standing
            player.setSpawnPoint(level.getRegistryKey(), pos, angle, false, true);
        }
    }

    public Optional<Vec3d> getRespawnLocation(boolean shouldCost, BlockState state, BlockPos pos, ServerWorld level, ServerPlayerEntity player) {
        if ( // condition stuff
                level.getBlockEntity(pos) instanceof RadiantFlameBlockEntity blockEntity &&
                (!RespawnObelisksConfig.INSTANCE.radiantFlame.playerBound || blockEntity.getOwner() == null || blockEntity.getOwner().equals(player.getUuid()))
        ) {
            double charge = blockEntity.getCharge();
            double cost = RespawnObelisksConfig.INSTANCE.radiance.respawnCost; // preparing cost value

            //if (charge-cost >= 0 && shouldCost) player.removeStatusEffect(immortalityCurse.get()); // remove curse if charge

            if (charge - (RespawnObelisksConfig.INSTANCE.radiance.forgivingRespawn ? 0 : cost) <= 0) {
                if (shouldCost) blockEntity.remove();
                return Optional.empty();
            }

            if (shouldCost)
                blockEntity.decreaseCharge(cost);

            Vec3d centered = pos.toCenterPos();
            return Optional.of(centered);
        }
        return Optional.empty();
    }

    public ActionResult onUse(BlockState state, World level, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Vec3d centerPos = pos.toCenterPos();
            Vec3d playerPos = player.getPos();
            float angle = (float) Math.atan2(playerPos.z - centerPos.z, playerPos.x - centerPos.x); // finding spawn angle based on where player is standing
            serverPlayer.setSpawnPoint(level.getRegistryKey(), pos, angle, false, true);
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, level, pos, player, hand, hitResult);
    }

    public boolean isTransparent(BlockState pState, BlockView pLevel, BlockPos pPos) {
        return true;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return HITBOX;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return ModRegistries.radiantFlameBlockEntity.get().instantiate(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World level, BlockState state, BlockEntityType<T> type) {
        return type == ModRegistries.radiantFlameBlockEntity.get() ? (pLevel, pos, blockState, be) -> {
            if (be instanceof RadiantFlameBlockEntity blockEntity)
                blockEntity.tick(pLevel, pos, blockState);
        } : null;
    }
}
