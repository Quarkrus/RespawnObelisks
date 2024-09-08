package com.redpxnda.respawnobelisks.facet;

import com.redpxnda.nucleus.facet.FacetKey;
import com.redpxnda.nucleus.facet.entity.EntityFacet;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class FailedSpawnBlocks implements EntityFacet<NbtList> {
    public static FacetKey<FailedSpawnBlocks> KEY;

    public final Set<Block> blocks = new HashSet<>();

    @Override
    public NbtList toNbt() {
        NbtList list = new NbtList();
        for (Block block : blocks)
            list.add(NbtString.of(Registries.BLOCK.getId(block).toString()));
        return list;
    }

    @Override
    public void loadNbt(NbtList nbt) {
        for (NbtElement element : nbt) {
            if (element instanceof NbtString nbtStr) {
                String strId = nbtStr.asString();
                Identifier id = Identifier.tryParse(strId);
                if (id != null) {
                    Block block = Registries.BLOCK.get(id);
                    blocks.add(block);
                }
            }
        }
    }
}
