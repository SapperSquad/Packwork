package com.sappersquad.packwork.guide;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

    /**
     * Two lines on the tooltip: what it is, and where a field report goes. The second one
     * is the door out - a player who never opens chapter six still sees it, and it is a
     * translated key so it can point speakers of other languages somewhere they can be read.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay tooltipDisplay,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.accept(net.minecraft.network.chat.Component.translatable("packwork.handbook.hint")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.accept(net.minecraft.network.chat.Component.translatable("packwork.handbook.report")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            com.sappersquad.packwork.client.HandbookClientHooks.open();
        }
        return InteractionResult.SUCCESS;
    }
}
