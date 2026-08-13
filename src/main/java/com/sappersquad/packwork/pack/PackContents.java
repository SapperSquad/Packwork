package com.sappersquad.packwork.pack;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The pack's flat item store, as its own component value. Packwork rode vanilla's
 * {@code ItemContainerContents} up to 1.21.11, but 26.1 rebuilt that class on
 * {@code ItemStackTemplate} and routed every stack-shaped read through
 * {@code ItemStack.validateStrict} - which nulls any count past the item's own max
 * stack size to EMPTY. Per-slot DEPTH (a 384-cobble slot on a Sculkhide pack) is the
 * pack's second axis, so the store now owns its holder: same shape, same slot
 * semantics, <b>no validation on read</b>. Counts are only ever bounded by the tier's
 * depth rule in {@link PackInventory}.
 *
 * <p>Immutable, like every component value. The persistent codec lives in
 * {@link DeepContentsCodec} (unchanged serialized shape, so 1.21.x-era saves read
 * intact); the stream codec below mirrors vanilla's template wire shape - counts ride
 * as raw VarInts and never cap.
 */
public final class PackContents {

    public static final int MAX_SIZE = 256;
    public static final PackContents EMPTY = new PackContents(List.of());

    public static final StreamCodec<RegistryFriendlyByteBuf, PackContents> STREAM_CODEC =
            ItemStackTemplate.STREAM_CODEC
                    .apply(ByteBufCodecs::optional)
                    .<List<Optional<ItemStackTemplate>>>apply(ByteBufCodecs.list(MAX_SIZE))
                    .map(PackContents::new, c -> c.items);

    private final List<Optional<ItemStackTemplate>> items;
    private final int hashCode;

    private PackContents(List<Optional<ItemStackTemplate>> items) {
        if (items.size() > MAX_SIZE) {
            throw new IllegalArgumentException("Got " + items.size() + " items, but maximum is " + MAX_SIZE);
        }
        this.items = items;
        this.hashCode = items.hashCode();
    }

    /** Build from a slot list; trailing empties are trimmed, counts are kept verbatim. */
    public static PackContents fromItems(List<ItemStack> stacks) {
        int last = -1;
        for (int i = stacks.size() - 1; i >= 0; i--) {
            if (!stacks.get(i).isEmpty()) {
                last = i;
                break;
            }
        }
        if (last == -1) return EMPTY;
        List<Optional<ItemStackTemplate>> items = new ArrayList<>(Collections.nCopies(last + 1, Optional.empty()));
        for (int i = 0; i <= last; i++) {
            ItemStack s = stacks.get(i);
            if (!s.isEmpty()) items.set(i, Optional.of(ItemStackTemplate.fromNonEmptyStack(s)));
        }
        return new PackContents(items);
    }

    public int getSlots() {
        return items.size();
    }

    /**
     * A fresh copy of the stack in a slot - built directly from the template
     * <b>without</b> {@code validateStrict}, so deep counts come back whole.
     */
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= items.size()) return ItemStack.EMPTY;
        return items.get(slot).map(PackContents::toStackUnvalidated).orElse(ItemStack.EMPTY);
    }

    private static ItemStack toStackUnvalidated(ItemStackTemplate t) {
        ItemStack stack = new ItemStack(t.item(), t.count(), DataComponentPatch.EMPTY);
        stack.applyComponents(t.components());
        return stack;
    }

    /** Copy every slot into the destination list (destination sizing is the caller's). */
    public void copyInto(NonNullList<ItemStack> destination) {
        for (int i = 0; i < destination.size(); i++) {
            destination.set(i, getStackInSlot(i));
        }
    }

    /** An updated store with one slot replaced (EMPTY clears it). */
    public PackContents withSlot(int slot, ItemStack stack, int minSize) {
        NonNullList<ItemStack> list = NonNullList.withSize(Math.max(getSlots(), Math.max(minSize, slot + 1)), ItemStack.EMPTY);
        copyInto(list);
        list.set(slot, stack);
        return fromItems(list);
    }

    /** Copies of every non-empty stack, in slot order - same name vanilla's holder uses. */
    public Stream<ItemStack> nonEmptyItemCopyStream() {
        return items.stream().filter(Optional::isPresent).map(o -> toStackUnvalidated(o.get()));
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof PackContents other && items.equals(other.items));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
