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
 * The persistent codec for the pack's item store, replacing vanilla's
 * {@code ItemContainerContents.CODEC} which routes every slot through
 * {@code ItemStack.CODEC} - and that codec hard-fails on any count over 99
 * ({@code ExtraCodecs.intRange(1, 99)}, verified against the 1.21.1 sources).
 * With per-slot depth a Dragonhide slot holds up to 384, so saving through the
 * vanilla codec would corrupt the pack at world save - the single worst thing
 * this mod could do.
 *
 * <p>Shape: a list of {@code {slot, item: {id, components}, count}} - the item
 * WITHOUT its count ({@code ItemStack.SINGLE_ITEM_CODEC}) and the count as its own
 * unbounded positive field. The network path needs no change: the stream codec
 * writes counts as raw VarInts and never caps them.
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

    private static final Codec<DeepSlot> SLOT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.intRange(0, 255).fieldOf("slot").forGetter(DeepSlot::slot),
            ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(DeepSlot::item),
            ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(DeepSlot::count)
    ).apply(inst, DeepSlot::new));

    private static final Codec<ItemContainerContents> DEEP = SLOT_CODEC.sizeLimitedListOf(256)
            .xmap(DeepContentsCodec::fromSlots, DeepContentsCodec::toSlots);

    /** What the component registers: writes deep, reads deep OR the vanilla legacy shape. */
    public static final Codec<ItemContainerContents> CODEC =
            Codec.withAlternative(DEEP, ItemContainerContents.CODEC);

    private static ItemContainerContents fromSlots(List<DeepSlot> slots) {
        int maxIndex = -1;
        for (DeepSlot s : slots) maxIndex = Math.max(maxIndex, s.slot());
        if (maxIndex < 0) return ItemContainerContents.EMPTY;
        NonNullList<ItemStack> list = NonNullList.withSize(maxIndex + 1, ItemStack.EMPTY);
        for (DeepSlot s : slots) list.set(s.slot(), s.item().copyWithCount(s.count()));
        return ItemContainerContents.fromItems(list);
    }

    private static List<DeepSlot> toSlots(ItemContainerContents contents) {
        List<DeepSlot> out = new ArrayList<>();
        for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack s = contents.getStackInSlot(i);
            if (!s.isEmpty()) out.add(new DeepSlot(i, s.copyWithCount(1), s.getCount()));
        }
        return out;
    }

    private DeepContentsCodec() {}
}
