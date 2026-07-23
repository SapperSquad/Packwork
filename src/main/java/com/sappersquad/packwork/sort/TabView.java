package com.sappersquad.packwork.sort;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A tab as the engine and GUI see it at runtime - auto and custom tabs unified.
 * Never serialised; rebuilt each open from {@link AutoTabs} (for {@code auto:*} ids)
 * and the pack's {@link PackLayout} (for {@code custom:*} ids and the tab order).
 *
 * @param editable whether the player may rename/re-rule/delete it (custom tabs only)
 * @param loose    the always-last catch-all that claims anything no rule wanted
 */
public record TabView(String id, Component name, ResourceLocation icon, int color,
                      List<SortRule> rules, boolean editable, boolean loose) {

    /** Does any of this tab's rules claim the stack? (Loose claims nothing here; it is the fallback.) */
    public boolean claims(ItemStack stack) {
        for (SortRule r : rules) {
            if (r.matches(stack)) return true;
        }
        return false;
    }

    public ItemStack iconStack() {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(icon).orElse(net.minecraft.world.item.Items.LEATHER);
        return new ItemStack(item);
    }
}
