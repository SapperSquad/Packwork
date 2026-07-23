package com.sappersquad.packwork.pack;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ComponentItemHandler;

import java.util.function.Supplier;

/**
 * A {@link ComponentItemHandler} that resolves its backing stack <em>live</em> through
 * a supplier rather than capturing a reference. Inside a menu the client can build the
 * handler a tick before its inventory slot syncs, and a captured empty stack would
 * silently swallow every write (components on an empty stack are no-ops). Resolving
 * live also lets the store follow the stack if it changes slot while open.
 */
public abstract class LiveComponentHandler extends ComponentItemHandler {

    protected final Supplier<ItemStack> live;

    protected LiveComponentHandler(Supplier<ItemStack> live,
                                   DataComponentType<ItemContainerContents> component, int size) {
        super(ItemStack.EMPTY, component, size);
        this.live = live;
    }

    @Override
    protected ItemContainerContents getContents() {
        return live.get().getOrDefault(this.component, ItemContainerContents.EMPTY);
    }

    @Override
    protected void updateContents(ItemContainerContents contents, ItemStack stack, int slot) {
        ItemStack target = live.get();
        if (target.isEmpty()) return; // nothing to write into yet; avoid a silent no-op
        NonNullList<ItemStack> list = NonNullList.withSize(Math.max(contents.getSlots(), getSlots()), ItemStack.EMPTY);
        contents.copyInto(list);
        ItemStack old = list.get(slot);
        list.set(slot, stack);
        target.set(this.component, ItemContainerContents.fromItems(list));
        onContentsChanged(slot, old, stack);
    }
}
