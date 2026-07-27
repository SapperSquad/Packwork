package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.block.PackContainerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The placeable pack block. Deliberately registered with NO BlockItem - the existing pack
 * item IS its item form ({@code PackItem.useOn} places it), so a pack in the world and a
 * pack in the hand are the same object. Drops come from {@code PackContainerBlock.getDrops}
 * (the pack stack itself), so the block carries no loot table.
 */
public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Packwork.MODID);

    public static final DeferredBlock<PackContainerBlock> PACK = BLOCKS.registerBlock("pack",
            PackContainerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(0.8f)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    // per-tier glow (Runed glyphs, the Sculkhide breath-gem) lives on the tier SSOT
                    .lightLevel(state -> state.getValue(PackContainerBlock.TIER).lightLevel())
                    .noLootTable());
}
