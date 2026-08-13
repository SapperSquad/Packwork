package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.DeepContentsCodec;
import com.sappersquad.packwork.pack.PackChemical;
import com.sappersquad.packwork.pack.PackContents;
import com.sappersquad.packwork.pack.PackFluidContent;
import com.sappersquad.packwork.sort.PackLayout;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Data components carried on a pack {@link net.minecraft.world.item.ItemStack}.
 *
 * <p>The heavy item data lives in Packwork's own {@link PackContents} (26.1: vanilla's
 * ItemContainerContents strict-validates every stack-shaped read and nulls deep counts
 * to EMPTY, so per-slot DEPTH needs a holder that trusts its counts - see the wave-2
 * notes); the light sorting metadata - custom tabs, pins, view settings - rides in a
 * separate {@link PackLayout} so the two never churn each other.
 *
 * <p>Fabric: components register straight into the registry (no DeferredRegister);
 * the {@link RegHandle} wrapper keeps every call site on its {@code .get()}.
 */
public class ModComponents {

    private static <T> RegHandle<DataComponentType<T>> register(String name, DataComponentType<T> type) {
        return new RegHandle<>(Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE, Packwork.id(name), type));
    }

    /**
     * The pack's item store: one flat backing inventory; tabs are virtual views over it.
     * Persisted through {@link DeepContentsCodec} (same serialized shape as every other
     * branch, legacy fallback intact); the stream codec writes counts as raw VarInts and
     * never caps.
     */
    public static final RegHandle<DataComponentType<PackContents>> PACK_CONTENTS =
            register("pack_contents", DataComponentType.<PackContents>builder()
                    .persistent(DeepContentsCodec.CODEC)
                    .networkSynchronized(PackContents.STREAM_CODEC)
                    .build());

    /** Custom tabs, manual pins, the void-filter list, and per-pack view settings. */
    public static final RegHandle<DataComponentType<PackLayout>> PACK_LAYOUT =
            register("pack_layout", DataComponentType.<PackLayout>builder()
                    .persistent(PackLayout.CODEC)
                    .networkSynchronized(PackLayout.STREAM_CODEC)
                    .build());

    /** The trinket fittings slotted into the pack (a tiny item container). */
    public static final RegHandle<DataComponentType<ItemContainerContents>> PACK_TRINKETS =
            register("pack_trinkets", DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    /** The Waterskin Rack's fluid tank (one fluid at a time, mB). Gated by the trinket. */
    public static final RegHandle<DataComponentType<PackFluidContent>> PACK_FLUID =
            register("pack_fluid", DataComponentType.<PackFluidContent>builder()
                    .persistent(PackFluidContent.CODEC)
                    .networkSynchronized(PackFluidContent.STREAM_CODEC)
                    .build());

    /** The Soul Vial's stored experience, in points. Gated by the trinket. */
    public static final RegHandle<DataComponentType<Integer>> PACK_XP =
            register("pack_xp", DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** The Charge Crystal's stored arcane charge, in FE-equivalent units. Gated by the trinket. */
    public static final RegHandle<DataComponentType<Integer>> PACK_ENERGY =
            register("pack_energy", DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * The Field Furnace's banked embers, in ticks of burn left. One fuel item feeds it exactly
     * as long as it would feed a furnace, so a lump of charcoal is still eight things cooked.
     */
    public static final RegHandle<DataComponentType<Integer>> PACK_EMBERS =
            register("pack_embers", DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * The Flask Harness's chemical tank (dist-neutral primitives). On Fabric no gas mod
     * exists to light the gate (Mekanism is NeoForge-only), but the component stays
     * registered so a pack that travelled from a NeoForge world keeps its vapors intact -
     * pause, never punish.
     */
    public static final RegHandle<DataComponentType<PackChemical>> PACK_CHEMICAL =
            register("pack_chemical", DataComponentType.<PackChemical>builder()
                    .persistent(PackChemical.CODEC)
                    .networkSynchronized(PackChemical.STREAM_CODEC)
                    .build());

    /** Touch to classload + register everything above (called once from mod init). */
    public static void init() {}
}
