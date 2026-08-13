package com.sappersquad.packwork.pack;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One trinket socket on the menu's right rail, bound to a {@link PackTrinketInventory}
 * index. The Fabric stand-in for the NeoForge branches' {@code ResourceHandlerSlot}:
 * component-backed copy-slot plumbing (vanilla mutates {@code getItem()}'s result then
 * calls {@code setChanged}; programmatic writes - including sync setting air - go through
 * the handler's direct {@code setSlot}, bypassing validity exactly as the old direct-set
 * path did).
 */
public class TrinketSocketSlot extends Slot {

    private static final Container EMPTY_INV = new SimpleContainer(0);

    private final PackTrinketInventory handler;
    private final int index;

    @Nullable
    private ItemStack cachedReturnedStack = null;

    public TrinketSocketSlot(PackTrinketInventory handler, int index, int x, int y) {
        super(EMPTY_INV, 0, x, y);
        this.handler = handler;
        this.index = index;
    }

    @Override
    public ItemStack getItem() {
        return cachedReturnedStack = handler.getStackInSlot(index);
    }

    @Override
    public void set(ItemStack stack) {
        handler.setSlot(index, stack);
        cachedReturnedStack = stack;
    }

    @Override
    public void setChanged() {
        if (cachedReturnedStack != null) {
            set(cachedReturnedStack);
        }
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack stack = handler.getStackInSlot(index);
        ItemStack ret = stack.split(amount);
        set(stack);
        cachedReturnedStack = null;
        return ret;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return handler.isValid(index, stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return !handler.getStackInSlot(index).isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return 1; // trinkets are one-per-socket gear
    }
}
