package com.redpxnda.respawnobelisks.forge;

import com.redpxnda.respawnobelisks.RespawnObelisks;
import com.redpxnda.respawnobelisks.forge.compat.CuriosCompat;
import com.redpxnda.respawnobelisks.registry.ModRegistries;
import com.redpxnda.respawnobelisks.registry.item.BoundCompassItem;
import com.redpxnda.respawnobelisks.registry.particle.RuneCircleParticle;
import com.redpxnda.respawnobelisks.util.CoreUtils;
import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static com.redpxnda.respawnobelisks.RespawnObelisks.MOD_ID;

@Mod(MOD_ID)
public class RespawnObelisksForge {
    public RespawnObelisksForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        RespawnObelisks.init();

        if (Platform.isModLoaded("curios")) {
            //MinecraftForge.EVENT_BUS.addListener(CuriosCompat::onDropRules);
            CuriosCompat.init();
        }
    }

    public static class ClientEvents {
        @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
        public static class ModBus {
            @SubscribeEvent
            public static void onClientSetup(FMLClientSetupEvent event) {
                event.enqueueWork(() -> {
                    ModelPredicateProviderRegistry.register(ModRegistries.boundCompass.get(), new Identifier("angle"), new CompassAnglePredicateProvider((level, stack, player) -> BoundCompassItem.hasLodestone(stack) ? BoundCompassItem.createLodestonePos(stack.getOrCreateNbt()) : null));
                    ModelPredicateProviderRegistry.register(ModRegistries.dormantObelisk.get(), new Identifier(MOD_ID, "dimension"), (stack, level, player, i) -> !stack.hasNbt() || !stack.getNbt().contains("Dimension") ? 0f : stack.getNbt().getFloat("Dimension"));
                    ModelPredicateProviderRegistry.register(ModRegistries.dormantObelisk.get(), new Identifier(MOD_ID, "uncharged"), (stack, level, player, i) -> CoreUtils.getCharge(stack.getOrCreateNbt()) == 0 ? 1 : 0);
                });
            }

            @SubscribeEvent
            public static void onParticleProvidersRegistry(RegisterParticleProvidersEvent event) {
                event.registerSpriteSet(ModRegistries.runeCircleParticle.get(), RuneCircleParticle.Provider::new);
            }
        }
    }
}
