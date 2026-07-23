package com.sappersquad.packwork.pack;

import net.minecraft.util.StringRepresentable;

/**
 * The material ladder. Single source of truth for how much a pack of each tier
 * holds and how many upgrade-trinket fittings it carries.
 *
 * <p>Capacity is capped at 256 because the underlying {@code ItemContainerContents}
 * data component cannot hold more than 256 slots. The visible grid is always
 * {@link #VIEW_COLS} x {@link #VIEW_ROWS}; anything beyond that paginates.
 */
public enum PackTier implements StringRepresentable {
    CANVAS("canvas", 54, 0),
    LEATHER("leather", 108, 1),
    STUDDED("studded", 162, 2),
    REINFORCED("reinforced", 216, 3),
    RUNED("runed", 256, 4);

    /** Columns of item slots shown in the pack grid. */
    public static final int VIEW_COLS = 9;
    /** Rows of item slots shown at once before paging. */
    public static final int VIEW_ROWS = 6;
    /** Slots shown on one page of the grid. */
    public static final int VIEW_SLOTS = VIEW_COLS * VIEW_ROWS;

    private final String name;
    private final int capacity;
    private final int trinketSlots;

    PackTier(String name, int capacity, int trinketSlots) {
        this.name = name;
        this.capacity = capacity;
        this.trinketSlots = trinketSlots;
    }

    /** Total backing slots this tier can hold. */
    public int capacity() {
        return capacity;
    }

    /** How many upgrade-trinket fittings this tier exposes (Phase 2). */
    public int trinketSlots() {
        return trinketSlots;
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
