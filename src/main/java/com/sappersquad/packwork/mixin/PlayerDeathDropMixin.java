package com.sappersquad.packwork.mixin;

import com.sappersquad.packwork.config.PackworkDeathHandling;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Death handling for {@code death.handling = "keep"} / {@code "place"}: Fabric has no
 * LivingDropsEvent, so this is the fourth (minimal, documented) mixin. Injects at the HEAD
 * of {@code LivingEntity.drop(stack, randomly, thrownFromHand)} - the one choke point every
 * death drop funnels through: the pockets via {@code Inventory.dropAll}, the armour row via
 * {@code EntityEquipment.dropAll}, and a worn trinket via {@code Player.drop} as well
 * (verified in the Trinkets Updated jar's bytecode, not assumed). That gives the same
 * coverage the NeoForge branches get from sweeping the drop list, in one narrow place.
 *
 * <p>Only a dying player is touched, and only when the config asks for something other than
 * "drop", so an ordinary Q-toss and every other mod's drop go straight through. Returning
 * null is a shape vanilla already produces - {@code drop} is {@code @Nullable} and every
 * caller on this path ignores it - and the pack it swallowed is in the player's copy-on-death
 * stash or standing as a block by then, never gone.
 */
@Mixin(LivingEntity.class)
public abstract class PlayerDeathDropMixin {

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("HEAD"), cancellable = true)
    private void packwork$claimDyingPacks(ItemStack stack, boolean randomly, boolean thrownFromHand,
                                          CallbackInfoReturnable<ItemEntity> cir) {
        if (!((Object) this instanceof Player player)) return;
        if (!player.isDeadOrDying()) return;
        if (PackworkDeathHandling.claimDeathDrop(player, stack)) {
            stack.setCount(0); // the stash owns the copy now; leave nothing behind to re-drop
            cir.setReturnValue(null);
        }
    }
}
