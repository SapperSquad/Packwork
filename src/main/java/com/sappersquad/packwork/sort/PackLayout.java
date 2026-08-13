package com.sappersquad.packwork.sort;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * The durable sorting configuration carried on a pack stack: the tab order the
 * player has set, any custom tabs they built, manual pins that always win, and
 * which compartments the player arranges by hand ({@link ManualTab}).
 *
 * <p>Pure view state (which tab is showing, the search text, the flatten toggle,
 * the page) deliberately lives only in the open menu, not here - so flipping tabs
 * never rewrites the item component.
 *
 * <p>An {@link #EMPTY} layout means "use the built-in auto-tab order, no custom
 * tabs, no pins" - a brand-new pack.
 *
 * @param tabOrder   ordered tab ids (mix of {@code auto:*} and {@code custom:*}); empty = default
 * @param customTabs player-made tab definitions
 * @param pins       item-to-tab overrides that beat every rule
 * @param voidList   items the Compass Rose trinket discards on insert (opt-in; the ONLY void path)
 * @param manual     compartments in keep-my-layout mode, with their remembered cell placements
 * @param packFirst  whether a fitted Lodestone routes pickups the pack can file straight in
 *                   (default ON; the toggle lives in the pack GUI's title strip)
 */
public record PackLayout(List<String> tabOrder, List<TabDef> customTabs, List<Pin> pins,
                         List<ResourceLocation> voidList, List<ManualTab> manual,
                         boolean packFirst) {

    public static final PackLayout EMPTY =
            new PackLayout(List.of(), List.of(), List.of(), List.of(), List.of(), true);

    /** Pre-pickup-toggle shape (pack-first defaults ON); kept for older call sites and tests. */
    public PackLayout(List<String> tabOrder, List<TabDef> customTabs, List<Pin> pins,
                      List<ResourceLocation> voidList, List<ManualTab> manual) {
        this(tabOrder, customTabs, pins, voidList, manual, true);
    }

    /** Pre-manual-mode shape; kept so old call sites and tests read naturally. */
    public PackLayout(List<String> tabOrder, List<TabDef> customTabs, List<Pin> pins,
                      List<ResourceLocation> voidList) {
        this(tabOrder, customTabs, pins, voidList, List.of(), true);
    }

    /** A manual override: this exact item always lands in this tab. */
    public record Pin(ResourceLocation item, String tabId) {
        public static final Codec<Pin> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Pin::item),
                Codec.STRING.fieldOf("tab").forGetter(Pin::tabId)
        ).apply(inst, Pin::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Pin> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Pin::item,
                ByteBufCodecs.STRING_UTF8, Pin::tabId,
                Pin::new);
    }

    /**
     * A compartment in keep-my-layout mode: the player's own arrangement, as "grid cell
     * {@code cell} shows backing slot {@code slot}" pairs. Strictly VIEW-ONLY - items
     * always live exactly once in the flat store, so a stale or duplicated entry can
     * mis-draw at worst, never dupe or lose. Entries whose backing slot has emptied or
     * re-routed are skipped at display time and pruned as the player works.
     */
    public record ManualTab(String tabId, List<Cell> cells) {
        /** Cells above this are ignored everywhere - a lid against a hostile component. */
        public static final int MAX_CELL = 512;

        public record Cell(int cell, int slot) {
            public static final Codec<Cell> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                    Codec.INT.fieldOf("cell").forGetter(Cell::cell),
                    Codec.INT.fieldOf("slot").forGetter(Cell::slot)
            ).apply(inst, Cell::new));

            public static final StreamCodec<RegistryFriendlyByteBuf, Cell> STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, Cell::cell,
                    ByteBufCodecs.VAR_INT, Cell::slot,
                    Cell::new);
        }

        public static final Codec<ManualTab> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("tab").forGetter(ManualTab::tabId),
                Cell.CODEC.listOf().optionalFieldOf("cells", List.of()).forGetter(ManualTab::cells)
        ).apply(inst, ManualTab::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ManualTab> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ManualTab::tabId,
                Cell.STREAM_CODEC.apply(ByteBufCodecs.list()), ManualTab::cells,
                ManualTab::new);
    }

    public static final Codec<PackLayout> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().optionalFieldOf("tab_order", List.of()).forGetter(PackLayout::tabOrder),
            TabDef.CODEC.listOf().optionalFieldOf("custom_tabs", List.of()).forGetter(PackLayout::customTabs),
            Pin.CODEC.listOf().optionalFieldOf("pins", List.of()).forGetter(PackLayout::pins),
            ResourceLocation.CODEC.listOf().optionalFieldOf("void_list", List.of()).forGetter(PackLayout::voidList),
            ManualTab.CODEC.listOf().optionalFieldOf("manual", List.of()).forGetter(PackLayout::manual),
            Codec.BOOL.optionalFieldOf("pack_first", true).forGetter(PackLayout::packFirst)
    ).apply(inst, PackLayout::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PackLayout> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PackLayout::tabOrder,
            TabDef.STREAM_CODEC.apply(ByteBufCodecs.list()), PackLayout::customTabs,
            Pin.STREAM_CODEC.apply(ByteBufCodecs.list()), PackLayout::pins,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), PackLayout::voidList,
            ManualTab.STREAM_CODEC.apply(ByteBufCodecs.list()), PackLayout::manual,
            ByteBufCodecs.BOOL, PackLayout::packFirst,
            PackLayout::new);

    /** The tab id a manual pin sends this item to, or null if unpinned. */
    public String pinnedTab(ResourceLocation itemId) {
        for (Pin p : pins) {
            if (p.item().equals(itemId)) return p.tabId();
        }
        return null;
    }

    public PackLayout withPins(List<Pin> newPins) {
        return new PackLayout(tabOrder, customTabs, newPins, voidList, manual, packFirst);
    }

    public PackLayout withCustomTabs(List<TabDef> newTabs) {
        return new PackLayout(tabOrder, newTabs, pins, voidList, manual, packFirst);
    }

    public PackLayout withTabOrder(List<String> newOrder) {
        return new PackLayout(newOrder, customTabs, pins, voidList, manual, packFirst);
    }

    public PackLayout withVoidList(List<ResourceLocation> newVoid) {
        return new PackLayout(tabOrder, customTabs, pins, newVoid, manual, packFirst);
    }

    public PackLayout withManual(List<ManualTab> newManual) {
        return new PackLayout(tabOrder, customTabs, pins, voidList, newManual, packFirst);
    }

    public PackLayout withPackFirst(boolean newPackFirst) {
        return new PackLayout(tabOrder, customTabs, pins, voidList, manual, newPackFirst);
    }

    /** This tab's kept arrangement, or null when the pack tidies it (the default). */
    public ManualTab manualFor(String tabId) {
        for (ManualTab m : manual) {
            if (m.tabId().equals(tabId)) return m;
        }
        return null;
    }

    /** Is this item marked for the Compass Rose to discard? */
    public boolean voids(ResourceLocation itemId) {
        return voidList.contains(itemId);
    }

    /** Custom tab matching an id, or null. */
    public TabDef customTab(String id) {
        for (TabDef t : customTabs) {
            if (t.id().equals(id)) return t;
        }
        return null;
    }

    /** Next free {@code custom:<n>} id given the current custom tabs. */
    public String nextCustomId() {
        int max = -1;
        for (TabDef t : customTabs) {
            if (t.id().startsWith("custom:")) {
                try {
                    max = Math.max(max, Integer.parseInt(t.id().substring("custom:".length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return "custom:" + (max + 1);
    }

    public List<String> mutableTabOrder() {
        return new ArrayList<>(tabOrder);
    }
}
