package com.sappersquad.packwork.guide;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Outfitter's Handbook: right-click to open the in-mod guide screen. The screen-open
 * call lives in a client-only hooks class and is reached only inside the isClientSide
 * branch, so this item stays safe to load on a dedicated server (the client class is
 * never classloaded there).
 */
public class HandbookItem extends Item {

    public HandbookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            com.sappersquad.packwork.client.HandbookClientHooks.open();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
