package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.block.PackContainerBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The placeable pack block. Deliberately registered with NO BlockItem - the existing pack
 * item IS its item form ({@code PackItem.useOn} places it), so a pack in the world and a
 * pack in the hand are the same object. Drops come from {@code PackContainerBlock.getDrops}
 * (the pack stack itself), so the block carries no loot table.
 */
public class ModBlocks {

    public static final RegHandle<PackContainerBlock> PACK = registerPack();

    private static RegHandle<PackContainerBlock> registerPack() {
        ResourceKey<net.minecraft.world.level.block.Block> key =
                ResourceKey.create(Registries.BLOCK, Packwork.id("pack"));
        PackContainerBlock block = new PackContainerBlock(BlockBehaviour.Properties.of()
                .setId(key)
                .mapColor(MapColor.TERRACOTTA_BROWN)
                .strength(0.8f)
                .sound(SoundType.WOOL)
                .noOcclusion()
                // per-tier glow (Runed glyphs, the Sculkhide echo gem) lives on the tier SSOT
                .lightLevel(state -> state.getValue(PackContainerBlock.TIER).lightLevel())
                .noLootTable());
        return new RegHandle<>(Registry.register(BuiltInRegistries.BLOCK, key, block));
    }

    /** Touch to classload + register everything above (called once from mod init). */
    public static void init() {}
}
