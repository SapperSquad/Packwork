package com.sappersquad.packwork.trinket;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A craftable fitting that grants a pack a capability once slotted into one of its
 * trinket sockets. Leather-and-brass gear, never a "module" or "chip".
 */
public class TrinketItem extends Item {

    private final TrinketType type;

    public TrinketItem(Properties properties, TrinketType type) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    public TrinketType type() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay tooltipDisplay,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(type.description().withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("packwork.trinket.install_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
