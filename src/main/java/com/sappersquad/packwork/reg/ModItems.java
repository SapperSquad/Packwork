package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * The pack items, one per material tier - registered straight off the
 * {@link PackTier} enum so the ladder stays a single source of truth.
 */
public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Packwork.MODID);

    public static final Map<PackTier, DeferredItem<PackItem>> PACKS = new EnumMap<>(PackTier.class);

    static {
        for (PackTier tier : PackTier.values()) {
            PACKS.put(tier, ITEMS.registerItem(tier.getSerializedName() + "_pack",
                    props -> new PackItem(props, tier), new Item.Properties()));
        }
    }

    public static DeferredItem<PackItem> pack(PackTier tier) {
        return PACKS.get(tier);
    }

    /** Convenience for the Phase 0 headline item. */
    public static DeferredItem<PackItem> leatherPack() {
        return PACKS.get(PackTier.LEATHER);
    }
}
