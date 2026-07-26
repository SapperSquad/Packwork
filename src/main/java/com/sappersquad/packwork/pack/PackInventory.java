package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.trinket.TrinketAccess;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * The pack's item store: a component-backed handler over the {@code pack_contents}
 * component. One flat inventory - tabs are virtual views computed over it, never
 * physical partitions, so sorting never moves an item and can never lose one.
 *
 * <p>Its slot count is live: a Bottomless Lining trinket grows it (BREADTH). Slots
 * beyond the current capacity are never truncated on write, so removing Bottomless
 * hides the extra items rather than voiding them.
 *
 * <p><b>Depth:</b> each slot holds {@link PackTier#depthMultiplier()} vanilla stacks
 * of its item - the material ladder's second axis. Two hard rules keep depth safe:
 * inserts may fill a slot to {@link #depthFor}, but every {@link #extractItem} pull
 * is clamped to the item's own vanilla max stack size, so an oversized stack can
 * exist INSIDE the pack and never anywhere else - not on a cursor, not in a hopper,
 * not on the ground. (The parent's insert clamps to the item's max stack size, and
 * its extract doesn't clamp at all - both verified in the NeoForge sources - which
 * is why both are overridden here.)
 */
public class PackInventory extends LiveComponentHandler {

    private final PackTier tier;

    /** Fixed-stack form for capability providers on a standalone/placed pack. */
    public PackInventory(ItemStack packStack, PackTier tier) {
        this(() -> packStack, tier);
    }

    /** Live form for the menu: always reads whichever stack currently sits in the bound slot. */
    public PackInventory(Supplier<ItemStack> live, PackTier tier) {
        super(live, ModComponents.PACK_CONTENTS.get(), 256);
        this.tier = tier;
    }

    @Override
    public int getSlots() {
        return TrinketAccess.capacity(live.get());
    }

    /**
     * How deep a slot goes for this item at this tier. The rule itself lives on the tier
     * SSOT ({@link PackTier#slotDepth}); this just feeds it the item's own max stack.
     */
    public int depthFor(ItemStack stack) {
        return tier.slotDepth(stack.getMaxStackSize());
    }

    /** Empty-slot ceiling for generic consumers: the depth of an ordinary 64-stackable. */
    @Override
    public int getSlotLimit(int slot) {
        return tier.slotDepth(net.minecraft.world.item.Item.DEFAULT_MAX_STACK_SIZE);
    }

    /** Parent clamps to the item's own max stack; this clamps to the tier's DEPTH instead. */
    @Override
    public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate) {
        validateSlotIndex(slot);
        if (toInsert.isEmpty()) return ItemStack.EMPTY;
        if (!isItemValid(slot, toInsert)) return toInsert;

        net.minecraft.world.item.component.ItemContainerContents contents = getContents();
        ItemStack existing = getStackFromContents(contents, slot);
        int insertLimit = depthFor(toInsert);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(toInsert, existing)) return toInsert;
            insertLimit -= existing.getCount();
        }
        if (insertLimit <= 0) return toInsert;

        int inserted = Math.min(insertLimit, toInsert.getCount());
        if (!simulate) {
            updateContents(contents, toInsert.copyWithCount(existing.getCount() + inserted), slot);
        }
        return toInsert.copyWithCount(toInsert.getCount() - inserted);
    }

    /**
     * Every pull out of the pack is clamped to ONE vanilla stack of the item, however
     * deep the slot is. This is the guarantee that automation (hoppers, pipes), the
     * cursor, and every trinket that draws from stock only ever see legal stacks.
     * (Inlined against a single component read - this is the hottest automation path.)
     */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        if (amount <= 0) return ItemStack.EMPTY;
        net.minecraft.world.item.component.ItemContainerContents contents = getContents();
        ItemStack existing = getStackFromContents(contents, slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        int toExtract = Math.min(Math.min(amount, existing.getMaxStackSize()), existing.getCount());
        if (!simulate) {
            updateContents(contents, existing.copyWithCount(existing.getCount() - toExtract), slot);
        }
        return existing.copyWithCount(toExtract);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // No pack-in-pack in v1 (see DECISIONS.md): blocks the dupe/lag surface.
        if (stack.getItem() instanceof PackItem) return false;
        return super.isItemValid(slot, stack);
    }
}
