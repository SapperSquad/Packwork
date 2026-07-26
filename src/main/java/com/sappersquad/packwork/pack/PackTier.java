package com.sappersquad.packwork.pack;

import net.minecraft.util.StringRepresentable;

/**
 * The material ladder. Single source of truth for how much a pack of each tier
 * holds, how DEEP each slot goes, and how many upgrade-trinket fittings it carries.
 *
 * <p>Capacity is capped at 256 because the underlying {@code ItemContainerContents}
 * data component cannot hold more than 256 slots. The visible grid is always
 * {@link #VIEW_COLS} x {@link #VIEW_ROWS}; anything beyond that paginates.
 *
 * <p><b>Depth vs. breadth:</b> the tier ladder grows slots wide (capacity) AND deep
 * ({@link #depthMultiplier()}: every slot holds that many vanilla stacks - Canvas 64
 * of a 64-item, Leather 128, up to Dragonhide 384). The Bottomless Lining trinket
 * adds BREADTH only (extra slots); tiers own depth. One axis each, so they never
 * double-dip.
 */
public enum PackTier implements StringRepresentable {
    CANVAS("canvas", 54, 0, 0),
    LEATHER("leather", 108, 1, 0),
    STUDDED("studded", 162, 2, 0),
    REINFORCED("reinforced", 216, 3, 0),
    RUNED("runed", 256, 4, 8),
    /** The endgame hide: slain-dragon flavored, gated behind End materials. */
    DRAGONHIDE("dragonhide", 256, 5, 11);

    /** Columns of item slots shown in the pack grid. */
    public static final int VIEW_COLS = 9;
    /** Rows of item slots shown at once before paging. */
    public static final int VIEW_ROWS = 6;
    /** Slots shown on one page of the grid. */
    public static final int VIEW_SLOTS = VIEW_COLS * VIEW_ROWS;

    private final String name;
    private final int capacity;
    private final int trinketSlots;
    private final int lightLevel;

    PackTier(String name, int capacity, int trinketSlots, int lightLevel) {
        this.name = name;
        this.capacity = capacity;
        this.trinketSlots = trinketSlots;
        this.lightLevel = lightLevel;
    }

    /** Total backing slots this tier can hold. */
    public int capacity() {
        return capacity;
    }

    /** How many upgrade-trinket fittings this tier exposes (Phase 2). */
    public int trinketSlots() {
        return trinketSlots;
    }

    /**
     * The tier's step on the ladder (1-based). Everything that scales linearly - slot
     * depth, store capacities, transfer rates - multiplies by this ONE number, so the
     * "how much better is each tier" rule lives in exactly one place.
     */
    public int step() {
        return ordinal() + 1;
    }

    /**
     * How many vanilla stacks each slot holds at this tier: Canvas 1 (64 of a
     * 64-stackable), Leather 2 (128), ... Dragonhide 6 (384). Sixteen-stackables
     * scale by the same multiplier (pearls: 16, 32, ...); unstackables never stack.
     */
    public int depthMultiplier() {
        return step();
    }

    /** The one depth rule: a slot holds {@code itemMaxStack x step}, and unstackables never stack. */
    public int slotDepth(int itemMaxStack) {
        return itemMaxStack <= 1 ? 1 : itemMaxStack * step();
    }

    /** Block light a placed pack of this tier gives off (the Runed glyphs, the Dragonhide gem). */
    public int lightLevel() {
        return lightLevel;
    }

    /** The top of the ladder - for player-facing "up to ..." sentences that must not go stale. */
    public static PackTier top() {
        return values()[values().length - 1];
    }

    /** Number of full/partial pages the grid needs to show every backing slot. */
    public int pages() {
        return Math.max(1, (capacity + VIEW_SLOTS - 1) / VIEW_SLOTS);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
