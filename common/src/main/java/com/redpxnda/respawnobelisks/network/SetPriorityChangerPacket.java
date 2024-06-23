package com.redpxnda.respawnobelisks.network;

import com.redpxnda.nucleus.facet.network.clientbound.FacetSyncPacket;
import com.redpxnda.respawnobelisks.config.RespawnObelisksConfig;
import com.redpxnda.respawnobelisks.facet.SecondarySpawnPoints;
import com.redpxnda.respawnobelisks.util.ClientUtils;
import com.redpxnda.respawnobelisks.util.SpawnPoint;
import dev.architectury.networking.NetworkManager;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SetPriorityChangerPacket extends FacetSyncPacket<NbtCompound, SecondarySpawnPoints> {
    private final Map<SpawnPoint, Item> cachedItems;
    private final Map<SpawnPoint, Block> cachedBlocks;

    @Override
    public void send(ServerPlayerEntity player) {
        ModPackets.CHANNEL.sendToPlayer(player, this);
    }

    @Override
    public void send(Iterable<ServerPlayerEntity> players) {
        ModPackets.CHANNEL.sendToPlayers(players, this);
    }

    public SetPriorityChangerPacket(Entity target, SecondarySpawnPoints facet) {
        super(target, SecondarySpawnPoints.KEY, facet);
        cachedItems = new HashMap<>();
        cachedBlocks = new HashMap<>();
        for (SpawnPoint point : facet.points) {
            Block block = target.getServer().getWorld(point.dimension()).getBlockState(point.pos()).getBlock();
            cachedItems.put(point, block.asItem());
            if (RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.secondarySpawnBlocksAsWhitelist == RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.secondarySpawnBlockBlacklist.contains(block))
                cachedBlocks.put(point, block);
        }
    }

    public SetPriorityChangerPacket(PacketByteBuf buffer) {
        super(buffer);
        cachedItems = new HashMap<>();
        cachedBlocks = new HashMap<>();

        int mapSize = buffer.readInt();
        for (int i = 0; i < mapSize; i++) {
            int tempX = buffer.readInt();
            int tempY = buffer.readInt();
            int tempZ = buffer.readInt();
            RegistryKey<World> tempWorld = RegistryKey.of(RegistryKeys.WORLD, new Identifier(buffer.readString()));

            SpawnPoint spawnPoint = new SpawnPoint(tempWorld, new BlockPos(tempX, tempY, tempZ), 0, false);
            Item item = Registries.ITEM.get(new Identifier(buffer.readString()));

            cachedItems.put(spawnPoint, item);
        }

        mapSize = buffer.readInt();
        for (int i = 0; i < mapSize; i++) {
            int tempX = buffer.readInt();
            int tempY = buffer.readInt();
            int tempZ = buffer.readInt();
            RegistryKey<World> tempWorld = RegistryKey.of(RegistryKeys.WORLD, new Identifier(buffer.readString()));

            SpawnPoint spawnPoint = new SpawnPoint(tempWorld, new BlockPos(tempX, tempY, tempZ), 0, false);
            Block block = Registries.BLOCK.get(new Identifier(buffer.readString()));

            cachedBlocks.put(spawnPoint, block);
        }
    }

    public void toBytes(PacketByteBuf buffer) {
        super.toBuffer(buffer);
        buffer.writeInt(cachedItems.size());
        cachedItems.forEach((point, item) -> {
            buffer.writeInt(point.pos().getX());
            buffer.writeInt(point.pos().getY());
            buffer.writeInt(point.pos().getZ());
            buffer.writeString(point.dimension().getValue().toString());
            buffer.writeString(Registries.ITEM.getId(item).toString());
        });

        buffer.writeInt(cachedBlocks.size());
        cachedBlocks.forEach((point, block) -> {
            buffer.writeInt(point.pos().getX());
            buffer.writeInt(point.pos().getY());
            buffer.writeInt(point.pos().getZ());
            buffer.writeString(point.dimension().getValue().toString());
            buffer.writeString(Registries.BLOCK.getId(block).toString());
        });
    }

    public void handle(Supplier<NetworkManager.PacketContext> supplier) {
        NetworkManager.PacketContext context = supplier.get();
        supplier.get().queue(() -> {
            super.handle(context);
            ClientUtils.cachedSpawnPointBlocks = cachedBlocks;
            ClientUtils.cachedSpawnPointItems = cachedItems;
            ClientUtils.hasLookedAwayFromPriorityChanger = false;
        });
    }
}
