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
    XP_POUR;

    private static final PackAction[] VALUES = values();

    public static PackAction byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : null;
    }
}
