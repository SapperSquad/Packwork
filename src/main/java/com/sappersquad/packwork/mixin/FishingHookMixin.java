package com.sappersquad.packwork.mixin;

import com.sappersquad.packwork.trinket.TrinketEffects;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The Angler's Creel: Fabric has no item-fished event, so this is one of Packwork's
 * three (minimal, documented) mixins. Redirects the loot roll inside
 * {@code FishingHook.retrieve} - the same drops-list the NeoForge branches'
 * ItemFishedEvent hands over - so the creel stows the catch and only what the pack
 * can't take is left in the list for vanilla to spawn toward the player. Nothing is
 * ever swallowed: a full pack costs you nothing.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Redirect(method = "retrieve",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> packwork$creelStowsCatch(LootTable table, LootParams params) {
        ObjectArrayList<ItemStack> drops = table.getRandomItems(params);
        FishingHook self = (FishingHook) (Object) this;
        if (self.getPlayerOwner() instanceof ServerPlayer sp) {
            TrinketEffects.onItemFished(sp, drops);
        }
        return drops;
    }
}
