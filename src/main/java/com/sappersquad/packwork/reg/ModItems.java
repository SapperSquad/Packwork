package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.guide.HandbookItem;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.trinket.TrinketItem;
import com.sappersquad.packwork.trinket.TrinketType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/**
 * The pack items (one per material tier) and the trinket fittings - both registered
 * straight off their SSOT enums ({@link PackTier}, {@link TrinketType}) so the ladder
 * and the fitting set each stay a single source of truth.
 */
public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Packwork.MODID);

    public static final Map<PackTier, DeferredItem<PackItem>> PACKS = new EnumMap<>(PackTier.class);
    public static final Map<TrinketType, DeferredItem<TrinketItem>> TRINKETS = new EnumMap<>(TrinketType.class);

    /** The in-house guide book. Opens a client screen on right-click; auto-listed in the creative tab. */
    public static final DeferredItem<HandbookItem> HANDBOOK =
            ITEMS.registerItem("outfitters_handbook", HandbookItem::new, Item.Properties::new);

    static {
        for (PackTier tier : PackTier.values()) {
            PACKS.put(tier, ITEMS.registerItem(tier.getSerializedName() + "_pack",
                    props -> new PackItem(props, tier), Item.Properties::new));
        }
        for (TrinketType type : TrinketType.values()) {
            TRINKETS.put(type, ITEMS.registerItem(type.id(),
                    props -> new TrinketItem(props, type), Item.Properties::new));
        }
    }

    public static DeferredItem<TrinketItem> trinket(TrinketType type) {
        return TRINKETS.get(type);
    }

    public static DeferredItem<PackItem> pack(PackTier tier) {
        return PACKS.get(tier);
    }

    /** Convenience for the Phase 0 headline item. */
    public static DeferredItem<PackItem> leatherPack() {
        return PACKS.get(PackTier.LEATHER);
    }
}
