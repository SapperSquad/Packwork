package com.sappersquad.packwork.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * The persistent codec for the pack's item store ({@link PackContents}), replacing
 * vanilla's slot codec - which routes every slot through a count codec hard-capped at
 * 99 ({@code ExtraCodecs.intRange(1, 99)}, verified against the sources). With per-slot
 * depth a Sculkhide slot holds up to 384, so saving through the vanilla codec would
 * corrupt the pack at world save - the single worst thing this mod could do.
 *
 * <p>Shape: a list of {@code {slot, item: {id, components}, count}} - the item WITHOUT
 * its count (26.1: {@code ItemStackTemplate}'s map codec, which omits a count of 1, so
 * the serialized form is byte-identical to the pre-26 {@code SINGLE_ITEM_CODEC} shape
 * and 1.21.x-era saves read back intact) and the count as its own unbounded positive
 * field. The network path lives on {@link PackContents#STREAM_CODEC}: raw VarInts,
 * never capped.
 *
 * <p><b>Migration:</b> {@code count} here is deliberately REQUIRED (no default).
 * A pack saved before this codec ({@code {slot, item: {id, count, components}}})
 * is missing the top-level count, so decoding with the deep shape FAILS - which is
 * exactly what {@link Codec#withAlternative} needs to fall back to the vanilla
 * codec and read the old data intact. If count were optional, old data would
 * "succeed" as count 1 and silently shrink every stack on world upgrade.
 */
public final class DeepContentsCodec {

    private record DeepSlot(int slot, ItemStack item, int count) {}

    /** The count-less item shape ({@code {id, components}}), validation-free on read. */
    private static final Codec<ItemStack> SINGLE_ITEM = net.minecraft.world.item.ItemStackTemplate.MAP_CODEC.codec()
            .xmap(t -> {
                ItemStack s = new ItemStack(t.item(), 1, net.minecraft.core.component.DataComponentPatch.EMPTY);
                s.applyComponents(t.components());
                return s;
            }, net.minecraft.world.item.ItemStackTemplate::fromNonEmptyStack);

    private static final Codec<DeepSlot> SLOT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.intRange(0, 255).fieldOf("slot").forGetter(DeepSlot::slot),
            SINGLE_ITEM.fieldOf("item").forGetter(DeepSlot::item),
            ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(DeepSlot::count)
    ).apply(inst, DeepSlot::new));

    private static final Codec<PackContents> DEEP = SLOT_CODEC.sizeLimitedListOf(256)
            .xmap(DeepContentsCodec::fromSlots, DeepContentsCodec::toSlots);

    /** Legacy fallback: pre-depth saves in vanilla's own shape (counts are <=99 there,
     *  so the validating vanilla read is safe) converted into the pack's holder. */
    private static final Codec<PackContents> LEGACY = ItemContainerContents.CODEC
            .xmap(DeepContentsCodec::fromVanilla, DeepContentsCodec::toVanilla);

    /** What the component registers: writes deep, reads deep OR the vanilla legacy shape. */
    public static final Codec<PackContents> CODEC = Codec.withAlternative(DEEP, LEGACY);

    private static PackContents fromSlots(List<DeepSlot> slots) {
        int maxIndex = -1;
        for (DeepSlot s : slots) maxIndex = Math.max(maxIndex, s.slot());
        if (maxIndex < 0) return PackContents.EMPTY;
        NonNullList<ItemStack> list = NonNullList.withSize(maxIndex + 1, ItemStack.EMPTY);
        for (DeepSlot s : slots) list.set(s.slot(), s.item().copyWithCount(s.count()));
        return PackContents.fromItems(list);
    }

    private static List<DeepSlot> toSlots(PackContents contents) {
        List<DeepSlot> out = new ArrayList<>();
        for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack s = contents.getStackInSlot(i);
            if (!s.isEmpty()) out.add(new DeepSlot(i, s.copyWithCount(1), s.getCount()));
        }
        return out;
    }

    private static PackContents fromVanilla(ItemContainerContents contents) {
        // pure vanilla 26.1 has no getSlots(); the copy-stream count is the slot count
        NonNullList<ItemStack> list = NonNullList.withSize(
                (int) contents.allItemsCopyStream().count(), ItemStack.EMPTY);
        contents.copyInto(list);
        return PackContents.fromItems(list);
    }

    private static ItemContainerContents toVanilla(PackContents contents) {
        NonNullList<ItemStack> list = NonNullList.withSize(contents.getSlots(), ItemStack.EMPTY);
        contents.copyInto(list);
        return ItemContainerContents.fromItems(list);
    }

    private DeepContentsCodec() {}
}
