package com.redpxnda.respawnobelisks;

import com.google.gson.Gson;
import com.redpxnda.nucleus.codec.tag.TaggableBlock;
import com.redpxnda.nucleus.config.ConfigBuilder;
import com.redpxnda.nucleus.config.ConfigManager;
import com.redpxnda.nucleus.config.ConfigType;
import com.redpxnda.nucleus.facet.FacetRegistry;
import com.redpxnda.respawnobelisks.config.RespawnObelisksConfig;
import com.redpxnda.respawnobelisks.data.listener.RevivedNbtEditing;
import com.redpxnda.respawnobelisks.event.ClientEvents;
import com.redpxnda.respawnobelisks.event.CommonEvents;
import com.redpxnda.respawnobelisks.facet.FailedSpawnBlocks;
import com.redpxnda.respawnobelisks.facet.HardcoreRespawningTracker;
import com.redpxnda.respawnobelisks.facet.SecondarySpawnPoints;
import com.redpxnda.respawnobelisks.facet.kept.KeptItemsModule;
import com.redpxnda.respawnobelisks.facet.kept.KeptRespawnItems;
import com.redpxnda.respawnobelisks.network.ModPackets;
import com.redpxnda.respawnobelisks.registry.ModRegistries;
import com.redpxnda.respawnobelisks.registry.ModTags;
import com.redpxnda.respawnobelisks.registry.block.RadiantFlameBlock;
import com.redpxnda.respawnobelisks.registry.block.RespawnObeliskBlock;
import com.redpxnda.respawnobelisks.registry.block.entity.theme.RenderTheme;
import com.redpxnda.respawnobelisks.util.RespawnAvailability;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RespawnObelisks {
    public static final String MOD_ID = "respawnobelisks";
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    public static final Gson GSON = new Gson();
    
    public static void init() {
        ConfigManager.register(ConfigBuilder.automatic(RespawnObelisksConfig.class)
                .id("respawnobelisks:common")
                .fileLocation("respawnobelisks")
                .creator(RespawnObelisksConfig::new)
                .type(ConfigType.COMMON)
                .updateListener(instance -> RespawnObelisksConfig.INSTANCE = instance)
                .presetGetter(conf -> conf.preset)
                .automaticScreen());

        ModRegistries.init();
        ModPackets.init();

        CommonEvents.init();

        KeptItemsModule.init();

        SecondarySpawnPoints.KEY = FacetRegistry.register(new Identifier(MOD_ID, "spawn_points"), SecondarySpawnPoints.class);
        HardcoreRespawningTracker.KEY = FacetRegistry.register(new Identifier(MOD_ID, "hardcore_respawning"), HardcoreRespawningTracker.class);
        KeptRespawnItems.KEY = FacetRegistry.register(new Identifier(MOD_ID, "kept_items"), KeptRespawnItems.class);
        FailedSpawnBlocks.KEY = FacetRegistry.register(new Identifier(MOD_ID, "failed_spawn_blocks"), FailedSpawnBlocks.class);
        FacetRegistry.ENTITY_FACET_ATTACHMENT.register((entity, attacher) -> {
            if (entity instanceof ServerPlayerEntity sp) {
                attacher.add(FailedSpawnBlocks.KEY, new FailedSpawnBlocks());
                if (RespawnObelisksConfig.INSTANCE.allowHardcoreRespawning)
                    attacher.add(HardcoreRespawningTracker.KEY, new HardcoreRespawningTracker());
                attacher.add(KeptRespawnItems.KEY, new KeptRespawnItems(sp));
            }

            if (entity instanceof PlayerEntity) {
                if (RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.enableSecondarySpawnPoints)
                    attacher.add(SecondarySpawnPoints.KEY, new SecondarySpawnPoints());
            }
        });

        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> {
            //ConfigManager.CONFIG_SCREENS_REGISTRY.register(screenRegisterer -> screenRegisterer.add(MOD_ID, "respawnobelisks"));
            com.redpxnda.nucleus.event.ClientEvents.TRANSLATIONS_RELOADED.register(map -> {
                if (RespawnObelisksConfig.INSTANCE.behaviorOverrides.destructionCatalysts)
                    map.replace("block.minecraft.respawn_anchor", map.getOrDefault("block.respawnobelisks.destruction_catalyst", "block.respawnobelisks.destruction_catalyst"));
                if (RespawnObelisksConfig.INSTANCE.secondarySpawnPoints.enableSecondarySpawnPoints)
                    map.replace("block.minecraft.set_spawn", map.getOrDefault("block.respawnobelisks.set_spawn", "block.respawnobelisks.set_spawn"));
            });

            ClientEvents.init();
            ClientEvents.registerParticleProviders();
            RenderTheme.init();
        });

        LifecycleEvent.SETUP.register(() -> {
            RespawnAvailability.availabilityProviders.put(new TaggableBlock(ModTags.Blocks.RESPAWN_OBELISKS), (point, pos, state, world, player) -> {
                return state.getBlock() instanceof RespawnObeliskBlock rob && rob.getRespawnLocation(false, false, false, state, pos, world, player).isPresent();
            });
            RespawnAvailability.availabilityProviders.put(new TaggableBlock(ModRegistries.radiantFlame.get()), (point, pos, state, world, player) -> {
                return state.getBlock() instanceof RadiantFlameBlock rob && rob.getRespawnLocation(false, state, pos, world, player).isPresent();
            });
            RespawnAvailability.availabilityProviders.put(new TaggableBlock(BlockTags.BEDS), (point, pos, state, world, player) -> {
                return state.getBlock() instanceof BedBlock && BedBlock.findWakeUpPosition(EntityType.PLAYER, world, pos, state.get(BedBlock.FACING), point.angle()).isPresent();
            });
            RespawnAvailability.availabilityProviders.put(new TaggableBlock(Blocks.RESPAWN_ANCHOR), (point, pos, state, world, player) -> {
                return RespawnAnchorBlock.findRespawnPosition(EntityType.PLAYER, world, pos).isPresent();
            });
        });

        ReloadListenerRegistry.register(ResourceType.SERVER_DATA, new RevivedNbtEditing());
    }

    public static Logger getLogger() {
        return LoggerFactory.getLogger("Respawn Obelisks: " + STACK_WALKER.getCallerClass().getSimpleName());
    }
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger("Respawn Obelisks: " + name);
    }
}
