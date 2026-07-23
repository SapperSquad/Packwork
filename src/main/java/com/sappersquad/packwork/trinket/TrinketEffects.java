package com.sappersquad.packwork.trinket;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackInventory;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.EnumSet;
import java.util.List;

/**
 * Runs the "active" trinket effects each server tick for every pack a player carries.
 * Passive trinkets (Bottomless capacity, Compass Rose void, Feather, Quill &amp; Ledger)
 * are read where they matter instead of here.
 *
 * <p>Everything is throttled and bounded; nothing here can void or dupe (magnet respects
 * the void list only when a Compass Rose is present, and only inserts what fits).
 */
@EventBusSubscriber(modid = Packwork.MODID)
public final class TrinketEffects {

    private static final double MAGNET_RANGE = 5.0;
    private static final int REPAIR_PER_TICK = 1;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) return;
        long time = sp.level().getGameTime();
        Inventory inv = sp.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack packStack = inv.getItem(i);
            if (!(packStack.getItem() instanceof PackItem)) continue;
            EnumSet<TrinketType> installed = TrinketAccess.installed(packStack);
            if (installed.isEmpty()) continue;
            PackTier tier = PackItem.tierOf(packStack);
            PackInventory pack = new PackInventory(packStack, tier);

            if (installed.contains(TrinketType.LODESTONE) && time % 4 == 0) magnet(sp, packStack, pack);
            if (installed.contains(TrinketType.RESTOCK) && time % 10 == 0) restock(sp, pack);
            if (installed.contains(TrinketType.REPAIR) && time % 20 == 0) repair(sp, pack);
            if (installed.contains(TrinketType.SOUL_VIAL) && time % 10 == 0) autoMend(sp, packStack);
            if (installed.contains(TrinketType.CHARGE_CRYSTAL) && time % 10 == 0) charge(sp, packStack);
        }
    }

    /** Charge Crystal: pour stored charge into the tools you're holding that accept it. */
    private static void charge(ServerPlayer sp, ItemStack packStack) {
        var crystal = packStack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        if (crystal == null || crystal.getEnergyStored() <= 0) return;
        for (ItemStack held : List.of(sp.getMainHandItem(), sp.getOffhandItem())) {
            if (held.isEmpty() || held.getItem() instanceof PackItem) continue;
            var sink = held.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
            if (sink == null || !sink.canReceive()) continue;
            int room = sink.receiveEnergy(Integer.MAX_VALUE, true);
            if (room <= 0) continue;
            int pulled = crystal.extractEnergy(room, false);
            if (pulled > 0) sink.receiveEnergy(pulled, false);
        }
    }

    /** Soul Vial: spend stored XP to mend Mending-enchanted gear you're wearing/holding. */
    private static void autoMend(ServerPlayer sp, ItemStack packStack) {
        if (com.sappersquad.packwork.pack.PackXpStore.stored(packStack) <= 0) return;
        var mending = sp.level().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.MENDING);
        List<ItemStack> gear = new java.util.ArrayList<>(List.of(sp.getMainHandItem(), sp.getOffhandItem()));
        sp.getArmorSlots().forEach(gear::add);
        for (ItemStack g : gear) {
            if (g.isEmpty() || !g.isDamageableItem() || !g.isDamaged()) continue;
            if (net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(mending, g) <= 0) continue;
            int wantPoints = Math.min(10, (g.getDamageValue() + 1) / 2); // up to 20 durability/tick
            int spent = com.sappersquad.packwork.pack.PackXpStore.spend(packStack, wantPoints);
            if (spent > 0) {
                g.setDamageValue(Math.max(0, g.getDamageValue() - spent * 2));
                return;
            }
        }
    }

    /** Pull loose items nearby into the pack (and quietly bin voided ones if a Compass Rose is fitted). */
    private static void magnet(ServerPlayer sp, ItemStack packStack, PackInventory pack) {
        AABB box = sp.getBoundingBox().inflate(MAGNET_RANGE);
        List<ItemEntity> items = sp.level().getEntitiesOfClass(ItemEntity.class, box,
                e -> e.isAlive() && !e.hasPickUpDelay());
        boolean hasRose = TrinketAccess.has(packStack, TrinketType.COMPASS_ROSE);
        var layout = packStack.getOrDefault(com.sappersquad.packwork.reg.ModComponents.PACK_LAYOUT.get(),
                com.sappersquad.packwork.sort.PackLayout.EMPTY);

        for (ItemEntity ie : items) {
            ItemStack stack = ie.getItem();
            if (stack.isEmpty() || stack.getItem() instanceof PackItem) continue;
            if (hasRose && layout.voids(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                ie.discard(); // magnet + void = a tidy trash collector
                continue;
            }
            ItemStack leftover = insertAll(pack, stack.copy());
            if (leftover.isEmpty()) ie.discard();
            else ie.setItem(leftover);
        }
    }

    /** Top up partial stacks already on the hotbar from pack stock. */
    private static void restock(ServerPlayer sp, PackInventory pack) {
        Inventory inv = sp.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack held = inv.getItem(slot);
            if (held.isEmpty() || held.getItem() instanceof PackItem) continue;
            int need = held.getMaxStackSize() - held.getCount();
            if (need <= 0 || held.getMaxStackSize() == 1) continue;
            for (int i = 0; i < pack.getSlots() && need > 0; i++) {
                ItemStack inPack = pack.getStackInSlot(i);
                if (ItemStack.isSameItemSameComponents(inPack, held)) {
                    ItemStack pulled = pack.extractItem(i, need, false);
                    held.grow(pulled.getCount());
                    need -= pulled.getCount();
                }
            }
        }
    }

    /** Slowly mend one damaged equipped item. Free but slow QoL; no materials consumed (v1). */
    private static void repair(ServerPlayer sp, PackInventory pack) {
        for (ItemStack gear : List.of(sp.getMainHandItem(), sp.getOffhandItem())) {
            if (mendOne(gear)) return;
        }
        for (ItemStack armor : sp.getArmorSlots()) {
            if (mendOne(armor)) return;
        }
    }

    private static boolean mendOne(ItemStack stack) {
        if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - REPAIR_PER_TICK));
            return true;
        }
        return false;
    }

    private static ItemStack insertAll(PackInventory pack, ItemStack stack) {
        ItemStack remaining = stack;
        for (int i = 0; i < pack.getSlots() && !remaining.isEmpty(); i++) {
            if (!pack.getStackInSlot(i).isEmpty()) remaining = pack.insertItem(i, remaining, false);
        }
        for (int i = 0; i < pack.getSlots() && !remaining.isEmpty(); i++) {
            if (pack.getStackInSlot(i).isEmpty()) remaining = pack.insertItem(i, remaining, false);
        }
        return remaining;
    }

    private TrinketEffects() {}
}
