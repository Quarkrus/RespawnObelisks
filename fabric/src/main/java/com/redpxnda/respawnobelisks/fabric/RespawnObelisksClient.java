package com.redpxnda.respawnobelisks.fabric;

import com.redpxnda.respawnobelisks.registry.ModRegistries;
import com.redpxnda.respawnobelisks.registry.item.BoundCompassItem;
import com.redpxnda.respawnobelisks.registry.particle.RuneCircleParticle;
import com.redpxnda.respawnobelisks.util.CoreUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.item.CompassAnglePredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;

import static com.redpxnda.respawnobelisks.RespawnObelisks.MOD_ID;

public class RespawnObelisksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelPredicateProviderRegistry.register(ModRegistries.boundCompass.get(), new Identifier("angle"), new CompassAnglePredicateProvider((level, stack, player) -> BoundCompassItem.hasLodestone(stack) ? BoundCompassItem.createLodestonePos(stack.getOrCreateNbt()) : null));
        ModelPredicateProviderRegistry.register(ModRegistries.dormantObelisk.get(), new Identifier(MOD_ID, "dimension"), (stack, level, player, i) -> !stack.hasNbt() || !stack.getNbt().contains("Dimension") ? 0f : stack.getNbt().getFloat("Dimension"));
        ModelPredicateProviderRegistry.register(ModRegistries.radiantLantern.get(), new Identifier(MOD_ID, "uncharged"), (stack, level, player, i) -> CoreUtils.getCharge(stack.getOrCreateNbt()) == 0 ? 1 : 0);
        ParticleFactoryRegistry.getInstance().register(ModRegistries.runeCircleParticle.get(), RuneCircleParticle.Provider::new);
    }
}
