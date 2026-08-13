package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.guide.HandbookItem;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.trinket.TrinketItem;
import com.sappersquad.packwork.trinket.TrinketType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The pack items (one per material tier) and the trinket fittings - both registered
 * straight off their SSOT enums ({@link PackTier}, {@link TrinketType}) so the ladder
 * and the fitting set each stay a single source of truth.
 *
 * <p>Fabric: plain eager {@code Registry.register}; 26.1 items REQUIRE their registry
 * id on the Properties ({@code setId}) before construction.
 */
public class ModItems {

    public static final Map<PackTier, RegHandle<PackItem>> PACKS = new EnumMap<>(PackTier.class);
    public static final Map<TrinketType, RegHandle<TrinketItem>> TRINKETS = new EnumMap<>(TrinketType.class);

    /** Every item this mod registers, in registration order (creative tab, audits). */
    public static final List<RegHandle<? extends Item>> ALL = new ArrayList<>();

    private static <T extends Item> RegHandle<T> register(String name, Function<Item.Properties, T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Packwork.id(name));
        T item = factory.apply(new Item.Properties().setId(key));
        RegHandle<T> handle = new RegHandle<>(Registry.register(BuiltInRegistries.ITEM, key, item));
        ALL.add(handle);
        return handle;
    }

    /** The in-house guide book. Opens a client screen on right-click; auto-listed in the creative tab. */
    public static final RegHandle<HandbookItem> HANDBOOK =
            register("outfitters_handbook", HandbookItem::new);

    static {
        for (PackTier tier : PackTier.values()) {
            PACKS.put(tier, register(tier.getSerializedName() + "_pack",
                    props -> new PackItem(props, tier)));
        }
        for (TrinketType type : TrinketType.values()) {
            TRINKETS.put(type, register(type.id(),
                    props -> new TrinketItem(props, type)));
        }
    }

    public static RegHandle<TrinketItem> trinket(TrinketType type) {
        return TRINKETS.get(type);
    }

    public static RegHandle<PackItem> pack(PackTier tier) {
        return PACKS.get(tier);
    }

    /** Convenience for the Phase 0 headline item. */
    public static RegHandle<PackItem> leatherPack() {
        return PACKS.get(PackTier.LEATHER);
    }

    /** Touch to classload + register everything above (called once from mod init). */
    public static void init() {}
}
