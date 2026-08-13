package com.sappersquad.packwork.mixin;

import com.sappersquad.packwork.trinket.TrinketEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quick-Draw Straps: Fabric has no item-broken event, so this is one of Packwork's
 * three (minimal, documented) mixins. {@code LivingEntity.onEquippedItemBroken} is the
 * one choke point every equipped item's break routes through (verified in the 26.1
 * sources); at TAIL the hand is already empty, which is exactly the state the refill
 * checks before pulling a spare from the pack.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "onEquippedItemBroken", at = @At("TAIL"))
    private void packwork$quickDrawRefill(Item item, EquipmentSlot slot, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer sp)) return;
        InteractionHand hand = switch (slot) {
            case MAINHAND -> InteractionHand.MAIN_HAND;
            case OFFHAND -> InteractionHand.OFF_HAND;
            default -> null;
        };
        if (hand != null) {
            TrinketEffects.onHeldItemBroken(sp, hand, item);
        }
    }
}
