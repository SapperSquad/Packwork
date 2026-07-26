package com.sappersquad.packwork.sort;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/*
 * Quill &amp; Ledger note: custom tabs only claim items by RULE when a Quill &amp; Ledger
 * is fitted (the {@code ledger} flag). Without it a custom tab is pin-only - its stored
 * rules are ignored, so a fresh custom compartment holds exactly what you pin to it.
 * With the ledger fitted, a custom tab evaluates its stored rules AND a rule derived from
 * the item it's stamped with, so "stamp a tab with a pickaxe and it gathers your tools".
 * Manual pins always win either way.
 */

/**
 * Turns a pack's {@link PackLayout} into the ordered list of tabs, and routes any
 * stack to the tab that claims it.
 *
 * <p>Routing order: a manual pin always wins; otherwise the first tab (in tab-order
 * priority) whose rules match claims the item; if nothing claims it, the Loose tab
 * catches it - so an item never simply vanishes.
 */
public final class SortEngine {

    /**
     * Build the visible, ordered tab list for a pack. Always ends with Loose.
     * Unknown ids in a saved order are skipped; autos/customs missing from the
     * saved order are appended so nothing a player made disappears.
     */
    public static List<TabView> tabsFor(PackLayout layout) {
        return tabsFor(layout, java.util.Set.of());
    }

    /** Convenience for tests/callers that only care about the Quill &amp; Ledger gate. */
    public static List<TabView> tabsFor(PackLayout layout, boolean ledger) {
        return tabsFor(layout, ledger
                ? java.util.Set.of(com.sappersquad.packwork.trinket.TrinketType.QUILL_LEDGER)
                : java.util.Set.of());
    }

    /**
     * @param fitted the trinkets currently installed in this pack. Two things depend on it:
     *               a Quill &amp; Ledger turns custom tabs from pin-only into rule-matching, and
     *               a fitting-gated compartment (Charts, Catch) only exists while its fitting
     *               does. Must be computed the same on client + server.
     */
    public static List<TabView> tabsFor(PackLayout layout,
                                        java.util.Set<com.sappersquad.packwork.trinket.TrinketType> fitted) {
        boolean ledger = fitted.contains(com.sappersquad.packwork.trinket.TrinketType.QUILL_LEDGER);
        List<TabView> out = new ArrayList<>();
        List<String> order = layout.tabOrder().isEmpty() ? defaultOrderWithCustoms(layout) : layout.tabOrder();

        List<String> seen = new ArrayList<>();
        for (String id : order) {
            if (seen.contains(id)) continue;
            TabView v = viewFor(id, layout, ledger, fitted);
            if (v != null) {
                out.add(v);
                seen.add(id);
            }
        }
        // Safety net: a shipped compartment missing from a saved order is INSERTED at its
        // default priority, not appended. That matters for the gated ones - fit a creel to an
        // old pack and its Catch compartment has to land ahead of Food to actually claim fish.
        for (AutoTabs.Auto a : AutoTabs.DEFAULTS) {
            if (seen.contains(a.id()) || (a.gate() != null && !fitted.contains(a.gate()))) continue;
            out.add(insertionIndexFor(a.id(), out), AutoTabs.toView(a));
            seen.add(a.id());
        }
        for (TabDef t : layout.customTabs()) {
            if (!seen.contains(t.id())) {
                out.add(toView(t, ledger));
                seen.add(t.id());
            }
        }
        out.add(AutoTabs.looseView());
        return out;
    }

    /** The first position in {@code out} held by a shipped tab of lower priority (else the end). */
    private static int insertionIndexFor(String id, List<TabView> out) {
        int mine = AutoTabs.priorityOf(id);
        for (int i = 0; i < out.size(); i++) {
            int theirs = AutoTabs.priorityOf(out.get(i).id());
            if (theirs >= 0 && theirs > mine) return i;
        }
        return out.size();
    }

    private static List<String> defaultOrderWithCustoms(PackLayout layout) {
        List<String> order = new ArrayList<>(AutoTabs.defaultOrder());
        for (TabDef t : layout.customTabs()) order.add(t.id());
        return order;
    }

    private static TabView viewFor(String id, PackLayout layout, boolean ledger,
                                   java.util.Set<com.sappersquad.packwork.trinket.TrinketType> fitted) {
        if (id.equals(AutoTabs.LOOSE_ID)) return null; // Loose is appended once, always last
        AutoTabs.Auto a = AutoTabs.byId(id);
        if (a != null) {
            // a fitting-gated compartment simply isn't there without its fitting; pull the
            // fitting and its items just re-route (nothing is stored per-tab, so nothing is lost)
            if (a.gate() != null && !fitted.contains(a.gate())) return null;
            return AutoTabs.toView(a);
        }
        TabDef t = layout.customTab(id);
        if (t != null) return toView(t, ledger);
        return null;
    }

    public static TabView toView(TabDef t) {
        return toView(t, false);
    }

    /**
     * Build the runtime view of a custom tab. Without a Quill &amp; Ledger the tab is
     * pin-only (no rules evaluated); with one, it claims by its stored rules plus a rule
     * derived from the item it's stamped with (see {@link #iconRule}).
     */
    public static TabView toView(TabDef t, boolean ledger) {
        Component name = t.name().isBlank() ? Component.translatable("packwork.tab.unnamed")
                : Component.literal(t.name());
        List<SortRule> rules;
        if (!ledger) {
            rules = List.of();
        } else {
            SortRule fromIcon = iconRule(t.icon());
            if (fromIcon == null || t.rules().contains(fromIcon)) {
                rules = t.rules();
            } else {
                rules = new ArrayList<>(t.rules());
                rules.add(fromIcon);
            }
        }
        return new TabView(t.id(), name, t.icon(), t.color(), rules, true, false);
    }

    /**
     * The category rule a Quill &amp; Ledger reads off a tab's stamped icon: the icon's
     * own kind (food, potion, weapon, armour, tool, block), or null if the icon is a
     * plain item with no obvious category (so the tab just stays pin-only). Order matters
     * - the most specific kinds are tested first so a stamped item claims one category.
     */
    public static SortRule iconRule(ResourceLocation icon) {
        var item = BuiltInRegistries.ITEM.getOptional(icon).orElse(null);
        if (item == null) return null;
        ItemStack probe = new ItemStack(item);
        for (PredicateKind kind : new PredicateKind[]{
                PredicateKind.IS_FOOD, PredicateKind.IS_POTION, PredicateKind.IS_WEAPON,
                PredicateKind.IS_ARMOR, PredicateKind.IS_TOOL, PredicateKind.IS_BLOCK}) {
            if (kind.test(probe)) return SortRule.predicate(kind);
        }
        return null;
    }

    /**
     * Which tab id claims this stack under the given tab list + layout (pins).
     * Returns {@link AutoTabs#LOOSE_ID} if nothing else does.
     */
    public static String route(ItemStack stack, List<TabView> tabs, PackLayout layout) {
        if (stack.isEmpty()) return AutoTabs.LOOSE_ID;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String pinned = layout.pinnedTab(itemId);
        if (pinned != null && tabExists(pinned, tabs)) return pinned;

        for (TabView t : tabs) {
            if (t.loose()) continue;
            if (t.claims(stack)) return t.id();
        }
        return AutoTabs.LOOSE_ID;
    }

    private static boolean tabExists(String id, List<TabView> tabs) {
        for (TabView t : tabs) if (t.id().equals(id)) return true;
        return false;
    }

    private SortEngine() {}
}
