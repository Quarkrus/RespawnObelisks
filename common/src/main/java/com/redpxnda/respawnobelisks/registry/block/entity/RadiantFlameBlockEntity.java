package com.redpxnda.respawnobelisks.registry.block.entity;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.redpxnda.respawnobelisks.config.RespawnObelisksConfig;
import com.redpxnda.respawnobelisks.registry.ModRegistries;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RadiantFlameBlockEntity extends BlockEntity {
    public static double getDefaultCharge() {
        return 60;
    }
    public static int getDefaultTime() {
        return RespawnObelisksConfig.INSTANCE.radiantFlame.lifetime;
    }

    public double charge;
    public double initialCharge;
    public int timeRemaining;
    public @Nullable UUID owner;
    public final Multimap<GlobalPos, ServerPlayerEntity> respawningPlayers = Multimaps.newMultimap(new ConcurrentHashMap<>(), HashSet::new);

    public RadiantFlameBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.radiantFlameBlockEntity.get(), pos, state);
        charge = getDefaultCharge();
        timeRemaining = getDefaultTime();
    }

    @Override
    public void readNbt(NbtCompound tag) {
        charge = tag.getDouble("Charge");
        initialCharge = tag.getDouble("InitialCharge");
        timeRemaining = tag.getInt("TimeRemaining");
        if (tag.contains("Owner")) owner = tag.getUuid("Owner");
    }

    @Override
    protected void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        saveData(tag);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound tag = new NbtCompound();
        saveData(tag);
        return tag;
    }

    private void saveData(NbtCompound tag) {
        tag.putDouble("Charge", charge);
        tag.putDouble("InitialCharge", initialCharge);
        tag.putInt("TimeRemaining", timeRemaining);
        if (owner != null) tag.putUuid("Owner", owner);
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public double getInitialCharge() {
        return initialCharge;
    }

    public void setInitialCharge(double initialCharge) {
        this.initialCharge = initialCharge;
    }

    public double getCharge() {
        return charge;
    }

    public void setCharge(double charge) {
        this.charge = charge;
    }

    public void decreaseCharge(double amnt) {
        setCharge(Math.max(getCharge() - amnt, 0));
        removeIfNoCharge();
        syncWithClient();
    }

    public void increaseCharge(double amnt) {
        setCharge(Math.max(getCharge() + amnt, 0));
        removeIfNoCharge();
        syncWithClient();
    }

    public void syncWithClient() {
        if (world == null || world.isClient) return;
        markDirty(this.world, this.getPos(), this.getCachedState());
    }

    public void removeIfNoCharge() {
        if (charge <= 0)
            remove();
    }

    public void remove() {
        if (getWorld() != null)
            getWorld().removeBlock(getPos(), false);
    }

    public void remove(World level, BlockPos pos) {
        level.removeBlock(pos, false);
    }

    public void tick(World level, BlockPos blockPos, BlockState state) {
        reduceTime(level, blockPos, state);

        if (level.getTime() % 20 == 0 && RespawnObelisksConfig.INSTANCE.radiantFlame.radianceReduction != 0)
            decreaseCharge(RespawnObelisksConfig.INSTANCE.radiantFlame.radianceReduction);
    }

    public void reduceTime(World level, BlockPos blockPos, BlockState state) {
        if (timeRemaining > 0)
            timeRemaining -= 1;
        else if (timeRemaining == 0) {
            remove(level, blockPos);
        }
    }
}
