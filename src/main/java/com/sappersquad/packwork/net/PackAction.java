package com.sappersquad.packwork.net;

/**
 * Everything the pack GUI can ask the server to do. Carried as an ordinal in
 * {@link PackActionPayload}; {@code arg}/{@code s1}/{@code s2} mean whatever each
 * action documents.
 */
public enum PackAction {
    /** s1 = tab id to show. */
    SELECT_TAB,
    /** s1 = search text. */
    SET_SEARCH,
    /** toggles the flatten-to-one-grid view. */
    TOGGLE_FLATTEN,
    /** arg = page delta (+1 / -1). */
    PAGE,
    /** re-run the whole sort: merge partial stacks, compact, order by tab. */
    TIDY_UP,
    /** make a new custom tab and select it. */
    CREATE_TAB,
    /** s1 = custom tab id to delete. */
    DELETE_TAB,
    /** s1 = custom tab id, s2 = new name. */
    RENAME_TAB,
    /** s1 = tab id, arg = move delta in the rail order. */
    MOVE_TAB,
    /** s1 = custom tab id, arg = ARGB colour. */
    SET_TAB_COLOR,
    /** s1 = custom tab id, s2 = item id for the stamp icon. */
    SET_TAB_ICON,
    /** s1 = tab id, s2 = item id to pin there. */
    PIN_ITEM,
    /** s2 = item id to unpin. */
    UNPIN_ITEM,
    /** s2 = item id to toggle on the Compass Rose void list. */
    VOID_TOGGLE,
    /** fill/drain the Waterskin tank with the item on the cursor. */
    FLUID_INTERACT,
    /** siphon the player's XP into the Soul Vial. */
    XP_SIPHON,
    /** pour the Soul Vial back into the player. */
    XP_POUR,
    /** unroll / roll up the Tinker's Kit tool roll across the pack's lower rows. */
    TOGGLE_ROLL,
    /** s1 = recipe id: lay one set of that recipe's makings from PACK stock onto the tool roll. */
    LAY_OUT_GHOST,
    /** s1 = custom tab id, arg = {@code SortRule.Type} ordinal, s2 = the rule's value.
     *  Requires a Quill &amp; Ledger fitted (the rule editor is its whole job). */
    ADD_TAB_RULE,
    /** s1 = custom tab id, arg = index into the tab's stored rule list. Ledger-gated too. */
    REMOVE_TAB_RULE,
    /** s1 = tab id: flip that compartment between Tidy (auto-arranged) and Keep-my-layout. */
    TOGGLE_TAB_MODE,
    /** flip whether a fitted Lodestone routes fileable pickups straight into this pack. */
    TOGGLE_PACK_FIRST,
    /** (1.21.11+) ask the server for the Recipe Ledger's craftable list; answered by
     *  {@link LedgerSyncPayload}. Recipes stopped syncing to clients in 1.21.2, so the
     *  scan the screen used to run locally lives server-side on these versions. */
    LEDGER_REFRESH,
    /** (1.21.11+) s1 = recipe id: ask for its 3x3 chalk arrangement; answered by
     *  {@link GhostSyncPayload}. Empty s1 just clears (client-side wipe needs no ask). */
    REQUEST_GHOST;

    private static final PackAction[] VALUES = values();

    public static PackAction byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : null;
    }

    /**
     * True for actions that move real items or XP rather than just re-arranging the view.
     * The client never applies these locally - it asks and waits for the sync - because a
     * client-side guess at the cursor or the player's XP either double-applies or desyncs.
     * Layout verbs (tabs, search, pins) stay optimistic so the rail still feels instant.
     */
    public boolean serverAuthoritative() {
        return this == FLUID_INTERACT || this == XP_SIPHON || this == XP_POUR
                || this == LAY_OUT_GHOST || this == LEDGER_REFRESH || this == REQUEST_GHOST;
    }
}
