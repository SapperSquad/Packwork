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

    /** Custom tabs, manual pins, and per-pack view settings. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PackLayout>> PACK_LAYOUT =
            COMPONENTS.registerComponentType("pack_layout", builder -> builder
                    .persistent(PackLayout.CODEC)
                    .networkSynchronized(PackLayout.STREAM_CODEC));
}
