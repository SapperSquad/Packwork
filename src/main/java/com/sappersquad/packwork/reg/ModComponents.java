package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.sort.PackLayout;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components carried on a pack {@link net.minecraft.world.item.ItemStack}.
 *
 * <p>The heavy item data lives in a vanilla {@link ItemContainerContents} (efficient,
 * battle-tested slot codec); the light sorting metadata - custom tabs, pins, view
 * settings - rides in a separate {@link PackLayout} so the two never churn each other.
 */
public class ModComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Packwork.MODID);

    /** The pack's item store: one flat backing inventory; tabs are virtual views over it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> PACK_CONTENTS =
            COMPONENTS.registerComponentType("pack_contents", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /** Custom tabs, manual pins, the void-filter list, and per-pack view settings. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PackLayout>> PACK_LAYOUT =
            COMPONENTS.registerComponentType("pack_layout", builder -> builder
                    .persistent(PackLayout.CODEC)
                    .networkSynchronized(PackLayout.STREAM_CODEC));

    /** The trinket fittings slotted into the pack (a tiny item container). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> PACK_TRINKETS =
            COMPONENTS.registerComponentType("pack_trinkets", builder -> builder
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /** The Waterskin Rack's fluid tank (one fluid at a time). Gated by the trinket. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.neoforged.neoforge.fluids.SimpleFluidContent>> PACK_FLUID =
            COMPONENTS.registerComponentType("pack_fluid", builder -> builder
                    .persistent(net.neoforged.neoforge.fluids.SimpleFluidContent.CODEC)
                    .networkSynchronized(net.neoforged.neoforge.fluids.SimpleFluidContent.STREAM_CODEC));

    /** The Soul Vial's stored experience, in points. Gated by the trinket. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PACK_XP =
            COMPONENTS.registerComponentType("pack_xp", builder -> builder
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
}
