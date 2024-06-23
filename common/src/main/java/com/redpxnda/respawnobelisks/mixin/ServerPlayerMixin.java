package com.redpxnda.respawnobelisks.mixin;

import com.redpxnda.respawnobelisks.config.RespawnObelisksConfig;
import com.redpxnda.respawnobelisks.config.SecondarySpawnPointConfig;
import com.redpxnda.respawnobelisks.facet.HardcoreRespawningTracker;
import com.redpxnda.respawnobelisks.facet.SecondarySpawnPoints;
import com.redpxnda.respawnobelisks.network.AllowHardcoreRespawnPacket;
import com.redpxnda.respawnobelisks.network.ModPackets;
import com.redpxnda.respawnobelisks.registry.block.RespawnObeliskBlock;
import com.redpxnda.respawnobelisks.registry.block.entity.RespawnObeliskBlockEntity;
import com.redpxnda.respawnobelisks.util.SpawnPoint;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin {

    @Shadow public abstract ServerWorld getServerWorld();

    @Shadow public abstract void sendMessage(Text message);

    @Shadow private float spawnAngle;

    @Shadow private boolean spawnForced;

    @Shadow private RegistryKey<World> spawnPointDimension;

    @Shadow private @Nullable BlockPos spawnPointPosition;

    @Inject(method = "getSpawnPointPosition", at = @At("RETURN"), cancellable = true)
    private void RESPAWNOBELISKS_getAndCacheSpawnPosition(CallbackInfoReturnable<BlockPos> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        boolean override = false;

        BlockPos bp = cir.getReturnValue();
        GlobalPos pos;

        if (RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.enableSecondarySpawnPoints) {
            SecondarySpawnPoints facet = SecondarySpawnPoints.KEY.get(player);
            if (facet != null) {
                SpawnPoint point = facet.getValidSpawnPoint(player);
                if (point != null) {
                    spawnPointDimension = point.dimension();
                    spawnPointPosition = point.pos();
                    spawnAngle = point.angle();
                    spawnForced = point.forced();
                    pos = point.asGlobalPos();
                } else pos = null;
                override = true;
            } else pos = bp == null ? null : GlobalPos.create(player.getSpawnPointDimension(), bp);
        } else pos = bp == null ? null : GlobalPos.create(player.getSpawnPointDimension(), bp);

        if (pos != null && player.getServer().getWorld(pos.getDimension()).getBlockEntity(pos.getPos()) instanceof RespawnObeliskBlockEntity robe) {
            robe.respawningPlayers.remove(pos, player);
            robe.respawningPlayers.put(pos, player);
        }

        if (override) cir.setReturnValue(pos == null ? null : pos.getPos());
    }

    @Inject(method = "setSpawnPoint", at = @At("HEAD"), cancellable = true)
    private void RESPAWNOBELISKS_overrideSpawnSetting(RegistryKey<World> dimension, @Nullable BlockPos pos, float angle, boolean forced, boolean sendMessage, CallbackInfo ci) {
        if (!forced && pos != null) {
            World world = getServerWorld().getServer().getWorld(dimension);
            if (world != null && RespawnObelisksConfig.INSTANCE.behaviorOverrides.isBlockBanned(world.getBlockState(pos))) {
                sendMessage(Text.translatable("text.respawnobelisks.cannot_set_spawn").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("text.respawnobelisks.cannot_set_spawn.hover")))));
                ci.cancel();
                return;
            }
        }

        if (RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.enableSecondarySpawnPoints) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            SecondarySpawnPoints facet = SecondarySpawnPoints.KEY.get(player);
            if (facet == null) return;
            SpawnPoint point = new SpawnPoint(dimension, pos, angle, forced);

            if (pos == null) {
                facet.removeLatestPoint();
                return;
            }

            if (facet.points.contains(point)) {
                if (RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.enableBlockPriorities)
                    ci.cancel();
                else
                    facet.addPoint(point);
            } else if (facet.blockAdditionAllowed(player, getServerWorld().getBlockState(pos).getBlock(), player.getServer())) {
                facet.addPoint(point);
                if (RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.enableBlockPriorities) facet.sortByPrio(player.getServer());
            } else if (!facet.points.contains(point)) {
                player.sendMessage(Text.translatable("block.respawnobelisks.cannot_set_spawn"));
                ci.cancel();
            } else
                ci.cancel();
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void RESPAWNOBELISKS_allowHardcoreRespawning(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        BlockPos pos = BlockPos.ORIGIN;
        if (RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.worldSpawnMode != SecondarySpawnPointConfig.PointSpawnMode.NEVER || RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.secondarySpawnMode != SecondarySpawnPointConfig.PointSpawnMode.NEVER) {
            pos = player.getSpawnPointPosition(); // updating player spawn points
            SecondarySpawnPoints facet = SecondarySpawnPoints.KEY.get(player);
            if (facet != null) {
                SpawnPoint point = facet.getLatestPoint();

                facet.willRespawnAtWorldSpawn = false;
                facet.canChooseRespawn = RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.secondarySpawnMode.evaluate(point, player);
                facet.canChooseWorldSpawn = RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.worldSpawnMode.evaluate(point, player);

                facet.sendToClient(player);
            }
        }
        if (RespawnObelisksConfig.INSTANCE.allowHardcoreRespawning) {
            if (pos == BlockPos.ORIGIN) pos = player.getSpawnPointPosition();
            if (pos == null) return;
            RegistryKey<World> dim = player.getSpawnPointDimension();
            ServerWorld world = player.getServer().getWorld(dim);
            if (world == null) return;
            BlockState state = world.getBlockState(pos);
            boolean canRespawn = state.getBlock() instanceof RespawnObeliskBlock rob && rob.getRespawnLocation(false, false, false, state, pos, world, player).isPresent();
            HardcoreRespawningTracker tracker = HardcoreRespawningTracker.KEY.get(player);
            if (tracker != null) tracker.canRespawn = canRespawn;
            ModPackets.CHANNEL.sendToPlayer(player, new AllowHardcoreRespawnPacket(canRespawn));
        }
    }
}
