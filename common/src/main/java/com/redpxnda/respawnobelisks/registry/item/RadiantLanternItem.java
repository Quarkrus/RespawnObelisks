package com.redpxnda.respawnobelisks.registry.item;

import com.redpxnda.respawnobelisks.config.RespawnObelisksConfig;
import com.redpxnda.respawnobelisks.util.CoreUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RadiantLanternItem extends BlockItem {
    public RadiantLanternItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> lines, TooltipContext tooltipFlag) {
        lines.add(1,
                Text.translatable("text.respawnobelisks.tooltip.charge").formatted(Formatting.GRAY)
                        .append(Text.literal(" " + CoreUtils.getCharge(stack.getOrCreateNbt())).formatted(Formatting.WHITE))
        );
    }

    @Override
    protected boolean canPlace(ItemPlacementContext context, BlockState state) {
        return CoreUtils.getCharge(context.getStack().getOrCreateNbt()) > 0 && super.canPlace(context, state);
    }

    @Override
    protected boolean postPlacement(BlockPos pos, World world, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
        if (player != null) player.getItemCooldownManager().set(stack.getItem(), RespawnObelisksConfig.INSTANCE.radiantFlame.placementCooldown);
        return super.postPlacement(pos, world, player, stack, state);
    }
}
