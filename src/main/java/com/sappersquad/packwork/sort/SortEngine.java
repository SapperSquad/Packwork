package com.sappersquad.packwork.sort;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
        List<TabView> out = new ArrayList<>();
        List<String> order = layout.tabOrder().isEmpty() ? defaultOrderWithCustoms(layout) : layout.tabOrder();

        List<String> seen = new ArrayList<>();
        for (String id : order) {
            if (seen.contains(id)) continue;
            TabView v = viewFor(id, layout);
            if (v != null) {
                out.add(v);
                seen.add(id);
            }
        }
        // Safety net: append any auto/custom tab not named in the order.
        for (AutoTabs.Auto a : AutoTabs.DEFAULTS) {
            if (!seen.contains(a.id())) {
                out.add(AutoTabs.toView(a));
                seen.add(a.id());
            }
        }
        for (TabDef t : layout.customTabs()) {
            if (!seen.contains(t.id())) {
                out.add(toView(t));
                seen.add(t.id());
            }
        }
        out.add(AutoTabs.looseView());
        return out;
    }

    private static List<String> defaultOrderWithCustoms(PackLayout layout) {
        List<String> order = new ArrayList<>(AutoTabs.defaultOrder());
        for (TabDef t : layout.customTabs()) order.add(t.id());
        return order;
    }

    private static TabView viewFor(String id, PackLayout layout) {
        if (id.equals(AutoTabs.LOOSE_ID)) return null; // Loose is appended once, always last
        AutoTabs.Auto a = AutoTabs.byId(id);
        if (a != null) return AutoTabs.toView(a);
        TabDef t = layout.customTab(id);
        if (t != null) return toView(t);
        return null;
    }

    public static TabView toView(TabDef t) {
        Component name = t.name().isBlank() ? Component.translatable("packwork.tab.unnamed")
                : Component.literal(t.name());
        return new TabView(t.id(), name, t.icon(), t.color(), t.rules(), true, false);
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
