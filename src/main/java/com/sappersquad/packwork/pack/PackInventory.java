package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ComponentItemHandler;

import java.util.function.Supplier;

/**
 * The pack's item store: a {@link ComponentItemHandler} whose backing is the
 * {@code pack_contents} data component on the pack stack. One flat inventory -
 * tabs are virtual views computed over it, never physical partitions, so sorting
 * never moves an item and can never lose one.
 *
 * <p>The backing stack is resolved <em>live</em> through a supplier rather than
 * captured once. That matters inside a menu: the client can build the menu a tick
 * before its inventory slot syncs, and a captured empty stack would silently
 * swallow every write (components on an empty stack are no-ops). Resolving live
 * also means the store follows the pack if it changes slot while open.
 */
public class PackInventory extends ComponentItemHandler {

    private final Supplier<ItemStack> live;

    /** Fixed-stack form for capability providers on a standalone/placed pack. */
    public PackInventory(ItemStack packStack, PackTier tier) {
        this(() -> packStack, tier);
    }

    /** Live form for the menu: always reads whichever stack currently sits in the bound slot. */
    public PackInventory(Supplier<ItemStack> live, PackTier tier) {
        super(ItemStack.EMPTY, ModComponents.PACK_CONTENTS.get(), tier.capacity());
        this.live = live;
    }

    @Override
    protected ItemContainerContents getContents() {
        return live.get().getOrDefault(this.component, ItemContainerContents.EMPTY);
    }

    @Override
    protected void updateContents(ItemContainerContents contents, ItemStack stack, int slot) {
        ItemStack target = live.get();
        if (target.isEmpty()) return; // nothing to write into yet; avoid a silent no-op write
        NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), getSlots()), ItemStack.EMPTY);
        contents.copyInto(list);
        ItemStack old = list.get(slot);
        list.set(slot, stack);
        target.set(this.component, ItemContainerContents.fromItems(list));
        onContentsChanged(slot, old, stack);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // No pack-in-pack in v1 (see DECISIONS.md): blocks the dupe/lag surface.
        if (stack.getItem() instanceof PackItem) return false;
        return super.isItemValid(slot, stack);
    }
}
