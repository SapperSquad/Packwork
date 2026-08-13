package com.sappersquad.packwork.mixin;

import com.sappersquad.packwork.trinket.TrinketEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pack-first pickup (the Lodestone): Fabric has no pre-pickup event, so this is one of
 * Packwork's three (minimal, documented) mixins. Injects at the HEAD of
 * {@code ItemEntity.playerTouch} - before any vanilla processing, the exact point the
 * NeoForge branches' ItemEntityPickupEvent.Pre fires - and lets
 * {@link TrinketEffects#onItemPickup} route what a fitted pack can FILE straight in.
 * A fully-settled touch (filed whole, or binned by the Compass Rose contract) cancels
 * vanilla; a partial fit shrinks the ground stack and lets vanilla pocket the rest.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void packwork$packFirstPickup(Player player, CallbackInfo ci) {
        if (TrinketEffects.onItemPickup(player, (ItemEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
