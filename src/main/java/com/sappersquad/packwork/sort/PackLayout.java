package com.sappersquad.packwork.sort;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

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
 * @param voidList   the DISCARD LIST - items the player has marked as "I don't want to haul
 *                   this". Opt-in, and the only path by which a pack ever throws anything
 *                   away. Membership alone means "bin it outright" (the Compass Rose's
 *                   contract, unchanged); an entry that also carries a {@link Spill} keep
 *                   level instead means "carry this much and bleed the rest" (the Overflow
 *                   Valve). One list, one concept, two fittings reading it.
 * @param spill      per-item keep levels for the list above, in vanilla stacks. An item with
 *                   no entry keeps 0 - which is exactly what every pack written before this
 *                   field existed decodes to, so old packs behave identically.
 * @param manual     compartments in keep-my-layout mode, with their remembered cell placements
 * @param packFirst  whether a fitted Lodestone routes pickups the pack can file straight in
 *                   (default ON; the toggle lives in the pack GUI's title strip)
 */
public record PackLayout(List<String> tabOrder, List<TabDef> customTabs, List<Pin> pins,
                         List<Identifier> voidList, List<ManualTab> manual,
                         boolean packFirst, List<Spill> spill) {

    public static final PackLayout EMPTY =
            new PackLayout(List.of(), List.of(), List.of(), List.of(), List.of(), true, List.of());

    /** Pre-spill shape (no keep levels); kept so older call sites and tests read naturally. */
    public PackLayout(List<String> tabOrder, List<TabDef> customTabs, List<Pin> pins,
                      List<Identifier> voidList, List<ManualTab> manual,
                      boolean packFirst) {
        this(tabOrder, customTabs, pins, voidList, manual, packFirst, List.of());
    }

    /** Pre-pickup-toggle shape (pack-first defaults ON); kept for older call sites and tests. */
    public PackLayout(List<String> tabOrder, List<TabDef> customTabs, List<Pin> pins,
                      List<Identifier> voidList, List<ManualTab> manual) {
        this(tabOrder, customTabs, pins, voidList, manual, true, List.of());
    }

    /** Pre-manual-mode shape; kept so old call sites and tests read naturally. */
    public PackLayout(List<String> tabOrder, List<TabDef> customTabs, List<Pin> pins,
                      List<Identifier> voidList) {
        this(tabOrder, customTabs, pins, voidList, List.of(), true, List.of());
    }

    /**
     * How much of a listed item the pack keeps before the Overflow Valve bleeds the surplus,
     * measured in vanilla stacks of that item. Zero is not stored here - an absent entry IS
     * zero, and zero means the Compass Rose bins the item outright.
     */
    public record Spill(Identifier item, int keepStacks) {
        /** The ladder the GUI cycles through. 0 = bin outright, and it comes round again. */
        public static final int[] LADDER = {1, 2, 4, 8, 16};
        /** A lid against a hostile component; nothing above this is honoured. */
        public static final int MAX_KEEP = 64;

        public static final Codec<Spill> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Identifier.CODEC.fieldOf("item").forGetter(Spill::item),
                Codec.INT.fieldOf("keep_stacks").forGetter(Spill::keepStacks)
        ).apply(inst, Spill::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Spill> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Spill::item,
                ByteBufCodecs.VAR_INT, Spill::keepStacks,
                Spill::new);
    }

    /** A manual override: this exact item always lands in this tab. */
    public record Pin(Identifier item, String tabId) {
        public static final Codec<Pin> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Identifier.CODEC.fieldOf("item").forGetter(Pin::item),
                Codec.STRING.fieldOf("tab").forGetter(Pin::tabId)
        ).apply(inst, Pin::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Pin> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Pin::item,
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
            Identifier.CODEC.listOf().optionalFieldOf("void_list", List.of()).forGetter(PackLayout::voidList),
            ManualTab.CODEC.listOf().optionalFieldOf("manual", List.of()).forGetter(PackLayout::manual),
            Codec.BOOL.optionalFieldOf("pack_first", true).forGetter(PackLayout::packFirst),
            Spill.CODEC.listOf().optionalFieldOf("spill", List.of()).forGetter(PackLayout::spill)
    ).apply(inst, PackLayout::new));

    /**
     * Hand-rolled, and it has to be: vanilla's {@code StreamCodec.composite} tops out at
     * <b>six</b> components (checked in the 1.21.1 sources, not remembered) and this record
     * has seven. Written out longhand rather than split into a nested sub-record, because the
     * wire order is then plainly readable next to the CODEC above - and the next component
     * costs one more line here instead of a refactor.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PackLayout> STREAM_CODEC =
            new StreamCodec<>() {
                // NB the two plain-ByteBuf ones: STRING_UTF8 and Identifier.STREAM_CODEC are
                // registry-free, so their list forms are StreamCodec<ByteBuf, ...>. Inline inside
                // `composite` that was hidden by its `? super B` parameter; named as fields it has
                // to be said out loud.
                private static final StreamCodec<io.netty.buffer.ByteBuf, List<String>> ORDER =
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());
                private static final StreamCodec<RegistryFriendlyByteBuf, List<TabDef>> TABS =
                        TabDef.STREAM_CODEC.apply(ByteBufCodecs.list());
                private static final StreamCodec<RegistryFriendlyByteBuf, List<Pin>> PINS =
                        Pin.STREAM_CODEC.apply(ByteBufCodecs.list());
                private static final StreamCodec<io.netty.buffer.ByteBuf, List<Identifier>> VOIDS =
                        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list());
                private static final StreamCodec<RegistryFriendlyByteBuf, List<ManualTab>> MANUAL =
                        ManualTab.STREAM_CODEC.apply(ByteBufCodecs.list());
                private static final StreamCodec<RegistryFriendlyByteBuf, List<Spill>> SPILL =
                        Spill.STREAM_CODEC.apply(ByteBufCodecs.list());

                @Override
                public PackLayout decode(RegistryFriendlyByteBuf buf) {
                    List<String> order = ORDER.decode(buf);
                    List<TabDef> tabs = TABS.decode(buf);
                    List<Pin> pins = PINS.decode(buf);
                    List<Identifier> voids = VOIDS.decode(buf);
                    List<ManualTab> manual = MANUAL.decode(buf);
                    boolean packFirst = ByteBufCodecs.BOOL.decode(buf);
                    List<Spill> spill = SPILL.decode(buf);
                    return new PackLayout(order, tabs, pins, voids, manual, packFirst, spill);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, PackLayout value) {
                    ORDER.encode(buf, value.tabOrder());
                    TABS.encode(buf, value.customTabs());
                    PINS.encode(buf, value.pins());
                    VOIDS.encode(buf, value.voidList());
                    MANUAL.encode(buf, value.manual());
                    ByteBufCodecs.BOOL.encode(buf, value.packFirst());
                    SPILL.encode(buf, value.spill());
                }
            };

    /** The tab id a manual pin sends this item to, or null if unpinned. */
    public String pinnedTab(Identifier itemId) {
        for (Pin p : pins) {
            if (p.item().equals(itemId)) return p.tabId();
        }
        return null;
    }

    public PackLayout withPins(List<Pin> newPins) {
        return new PackLayout(tabOrder, customTabs, newPins, voidList, manual, packFirst, spill);
    }

    public PackLayout withCustomTabs(List<TabDef> newTabs) {
        return new PackLayout(tabOrder, newTabs, pins, voidList, manual, packFirst, spill);
    }

    public PackLayout withTabOrder(List<String> newOrder) {
        return new PackLayout(newOrder, customTabs, pins, voidList, manual, packFirst, spill);
    }

    /**
     * Replace the discard list. Any keep level for an item that just left the list goes with
     * it - a level with no listing behind it would be a rule nobody can see.
     */
    public PackLayout withVoidList(List<Identifier> newVoid) {
        List<Spill> kept = new ArrayList<>();
        for (Spill s : spill) {
            if (newVoid.contains(s.item())) kept.add(s);
        }
        return new PackLayout(tabOrder, customTabs, pins, newVoid, manual, packFirst, List.copyOf(kept));
    }

    public PackLayout withManual(List<ManualTab> newManual) {
        return new PackLayout(tabOrder, customTabs, pins, voidList, newManual, packFirst, spill);
    }

    public PackLayout withPackFirst(boolean newPackFirst) {
        return new PackLayout(tabOrder, customTabs, pins, voidList, manual, newPackFirst, spill);
    }

    /**
     * Set one listed item's keep level, in vanilla stacks. Zero removes the entry (absent IS
     * zero), which puts the item back on the Compass Rose's bin-outright contract.
     */
    public PackLayout withKeepStacks(Identifier item, int keepStacks) {
        int clamped = Math.max(0, Math.min(Spill.MAX_KEEP, keepStacks));
        List<Spill> out = new ArrayList<>();
        for (Spill s : spill) {
            if (!s.item().equals(item)) out.add(s);
        }
        if (clamped > 0) out.add(new Spill(item, clamped));
        return new PackLayout(tabOrder, customTabs, pins, voidList, manual, packFirst, List.copyOf(out));
    }

    /** This tab's kept arrangement, or null when the pack tidies it (the default). */
    public ManualTab manualFor(String tabId) {
        for (ManualTab m : manual) {
            if (m.tabId().equals(tabId)) return m;
        }
        return null;
    }

    /**
     * Is this item marked for the Compass Rose to bin outright - thrown away at the door,
     * never entering the pack? True only for a listed item whose keep level is zero. An item
     * the player has given the Overflow Valve a keep level for is on the same list but is NOT
     * binned on the way in: it comes in, files normally, and only the surplus bleeds off.
     */
    public boolean voids(Identifier itemId) {
        return voidList.contains(itemId) && keepStacks(itemId) == 0;
    }

    /** Is this item on the discard list at all, at any keep level? */
    public boolean listed(Identifier itemId) {
        return voidList.contains(itemId);
    }

    /**
     * How many vanilla stacks of this item the pack keeps before the Overflow Valve bleeds
     * the surplus. Zero for anything unlisted and for anything the Rose bins outright.
     */
    public int keepStacks(Identifier itemId) {
        for (Spill s : spill) {
            if (s.item().equals(itemId)) return Math.max(0, Math.min(Spill.MAX_KEEP, s.keepStacks()));
        }
        return 0;
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
