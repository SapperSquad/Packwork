package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.trinket.TrinketItem;
import com.sappersquad.packwork.trinket.TrinketType;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * The pack's trinket sockets: a tiny component-backed handler over
 * {@code pack_trinkets}. Slot count is the tier's fitting count. Only trinket items
 * fit, and never two of the same kind (a second identical fitting does nothing).
 */
public class PackTrinketInventory extends LiveComponentHandler {

    private final int slots;
    private final Supplier<ItemStack> live;

    public PackTrinketInventory(Supplier<ItemStack> live, PackTier tier) {
        super(live, ModComponents.PACK_TRINKETS.get(), Math.max(1, tier.trinketSlots()));
        this.live = live;
        this.slots = tier.trinketSlots();
    }

    @Override
    public int getSlots() {
        return slots;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true; // clearing a socket must be allowed (sync sets air)
        if (!(stack.getItem() instanceof TrinketItem)) return false;
        TrinketType type = TrinketType.of(stack);
        // one of each kind; a duplicate fitting grants nothing, so refuse it
        for (int i = 0; i < getSlots(); i++) {
            if (i == slot) continue;
            if (TrinketType.of(getStackInSlot(i)) == type) return false;
        }
        return true;
    }
}
