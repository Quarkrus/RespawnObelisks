package com.redpxnda.respawnobelisks.facet.kept;

import com.redpxnda.respawnobelisks.RespawnObelisks;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public interface KeptItemsModule {
    Logger LOGGER = RespawnObelisks.getLogger();
    Map<String, Function<ServerPlayerEntity, @Nullable KeptItemsModule>> MODULES = new HashMap<>();

    static void init() {
        KeptItemsModule.registerModule("armor", player -> new KeptArmorModule());
        KeptItemsModule.registerModule("offhand", player -> new KeptOffhandModule());
        KeptItemsModule.registerModule("inventory", player -> new KeptInventoryModule());
        KeptItemsModule.registerModule("xp", player -> new KeptXpModule());
    }

    static void registerModule(String key, Function<ServerPlayerEntity, @Nullable KeptItemsModule> creator) {
        if (MODULES.containsKey(key)) {
            LOGGER.warn("Cannot register KeptItemsModule as another already exists under the same name!");
            return;
        }
        MODULES.put(key, creator);
    }

    NbtElement toNbt();
    void fromNbt(NbtElement element);

    /**
     * @param oldPlayer the dead player object from before death (same as newPlayer if in rune circle teleport or obelisk interaction)
     * @param newPlayer the alive, replacement player object after death
     */
    void restore(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer);
    void gather(ServerPlayerEntity player);
    void scatter(double x, double y, double z, ServerPlayerEntity player);
    boolean isEmpty();
}
