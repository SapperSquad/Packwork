package com.sappersquad.packwork.sort;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Tidy Up operation, factored out of the menu so it is testable headlessly:
 * merge partial stacks of the same item, then order by tab priority and item id.
 * Pure over its inputs; no side effects.
 */
public final class PackSorting {

    /** Vanilla-depth form (one stack per slot); kept for callers without a tiered pack. */
    public static List<ItemStack> tidy(List<ItemStack> source, List<TabView> tabs, PackLayout layout) {
        return tidy(source, tabs, layout, ItemStack::getMaxStackSize);
    }

    /**
     * @param source non-empty backing stacks (copies; not mutated)
     * @param tabs   the resolved tab list (for priority ordering)
     * @param layout the pack layout (for pin-aware routing)
     * @param depth  how many items one slot may hold of a given stack - the pack tier's
     *               per-slot DEPTH, so Tidy Up merges loose stacks down into deep ones
     * @return a compacted, merged, ordered list ready to write back to the front slots
     */
    public static List<ItemStack> tidy(List<ItemStack> source, List<TabView> tabs, PackLayout layout,
                                       java.util.function.ToIntFunction<ItemStack> depth) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack in : source) {
            if (in.isEmpty()) continue;
            ItemStack s = in.copy();
            for (ItemStack m : merged) {
                if (s.isEmpty()) break;
                if (ItemStack.isSameItemSameComponents(m, s)) {
                    int space = depth.applyAsInt(m) - m.getCount();
                    if (space > 0) {
                        int move = Math.min(space, s.getCount());
                        m.grow(move);
                        s.shrink(move);
                    }
                }
            }
            if (!s.isEmpty()) merged.add(s);
        }

        Map<String, Integer> tabIndex = new HashMap<>();
        for (int i = 0; i < tabs.size(); i++) tabIndex.put(tabs.get(i).id(), i);

        merged.sort(Comparator
                .comparingInt((ItemStack s) -> tabIndex.getOrDefault(SortEngine.route(s, tabs, layout), 999))
                .thenComparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
        return merged;
    }

    private PackSorting() {}
}
