package com.sappersquad.packwork.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.IContainerFactory;

import java.util.List;

/**
 * The pack. Right-click (or the keybind) opens the tabbed organizer bound to
 * whichever inventory slot the pack sits in, so the same menu code serves a pack
 * in the hand and a pack pulled up from the inventory.
 */
public class PackItem extends Item {

    private final PackTier tier;

    public PackItem(Properties properties, PackTier tier) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public PackTier tier() {
        return tier;
    }

    public static PackTier tierOf(ItemStack stack) {
        return stack.getItem() instanceof PackItem p ? p.tier() : PackTier.LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            int slot = findSlot(player, held, hand);
            if (slot >= 0) {
                openPack(player, slot);
            }
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    /** The inventory slot index of the held stack, so the menu can rebind to it. */
    private static int findSlot(Player player, ItemStack held, InteractionHand hand) {
        Inventory inv = player.getInventory();
        if (hand == InteractionHand.OFF_HAND) {
            return Inventory.SLOT_OFFHAND; // 40
        }
        // main hand -> currently selected hotbar slot
        if (inv.getItem(inv.selected) == held) return inv.selected;
        // fall back to a scan
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == held) return i;
        }
        return -1;
    }

    /** Open the organizer for the pack at the given inventory slot. */
    public static void openPack(Player player, int slot) {
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return player.getInventory().getItem(slot).getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player p) {
                return com.sappersquad.packwork.pack.PackMenu.server(id, playerInv, slot);
            }
        };
        player.openMenu(provider, buf -> buf.writeVarInt(slot));
    }

    /** Client menu factory: reads the bound slot from the open packet. */
    public static final IContainerFactory<PackMenu> CLIENT_FACTORY =
            (id, playerInv, buf) -> PackMenu.client(id, playerInv, buf.readVarInt());

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents contents = stack.get(com.sappersquad.packwork.reg.ModComponents.PACK_CONTENTS.get());
        int used = 0;
        if (contents != null) {
            used = (int) contents.nonEmptyStream().count();
        }
        tooltip.add(Component.translatable("packwork.pack.slots_used", used, tier.capacity())
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.add(Component.translatable("packwork.pack.hint")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
