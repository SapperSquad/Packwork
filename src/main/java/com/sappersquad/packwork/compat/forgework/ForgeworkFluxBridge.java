package com.sappersquad.packwork.compat.forgework;

import com.forgework.energy.IFlowEnergyStorage;
import com.forgework.item.PortableEnderTerminalItem;
import com.forgework.registry.ModCapabilities;
import com.sappersquad.packwork.block.PackContainerBlockEntity;
import com.sappersquad.packwork.pack.PackEnergyStorage;
import com.sappersquad.packwork.trinket.TrinketAccess;
import com.sappersquad.packwork.trinket.TrinketType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * The ONLY class in Packwork allowed to import {@code com.forgework.*}. Every entry
 * point is reached strictly behind a {@code ModList.isLoaded("forgework")} gate (see
 * {@link com.sappersquad.packwork.trinket.TrinketEffects}), so neither this class nor
 * Forgework's ever classloads without the mod installed - the same lazy-classloading
 * pattern PhytoForge's {@code ForgeworkEnergyBridge} uses.
 *
 * <p><b>What it does.</b> Forgework's "Flux" ({@code IFlowEnergyStorage}) is a
 * <em>block</em> capability, and Forgework stores Flux on its portable gear in a bespoke
 * {@code custom_data} tag reachable only through {@link PortableEnderTerminalItem}'s
 * public {@code getFlux}/{@code charge}. Packwork's pack is item-only (no placed
 * block-entity form yet), so the honest bridge is item-level: a fitted Charge Crystal
 * tops up any Forgework portable terminal the player is carrying from its own arcane
 * charge, <b>1 Flux = 1 FE</b> - exactly the way the crystal already tops up FE tools in
 * hand. One direction only (pack -&gt; terminal), because Forgework exposes no way to pull
 * Flux back out of an item; that mirrors how the crystal feeds consumers, not sources.
 *
 * <p>Block-level interop (a Forgework cable charging a <em>placed</em> pack) would need a
 * pack block-entity exposing {@code FLOW_ENERGY}; that is still-open scope, flagged in
 * {@code PROJECT_HANDOFF.md}.
 */
public final class ForgeworkFluxBridge {

    private ForgeworkFluxBridge() {}

    /**
     * Top up every Forgework portable terminal the player carries from the pack's charge.
     * Bounded by {@code perTick} so a full pack can't dump instantly. Returns FE moved.
     */
    public static int topUpCarried(ServerPlayer sp, PackEnergyStorage crystal, int perTick) {
        Inventory inv = sp.getInventory();
        int budget = perTick;
        int moved = 0;
        for (int i = 0; i < inv.getContainerSize() && budget > 0 && crystal.getEnergyStored() > 0; i++) {
            int m = chargeItem(inv.getItem(i), crystal, budget);
            moved += m;
            budget -= m;
        }
        return moved;
    }

    /**
     * Transfer up to {@code cap} FE from the crystal into a single Forgework Flux item,
     * 1:1. A non-Flux item (anything that isn't a Portable Ender Terminal) is a no-op.
     * Never dupes: it only adds what the crystal actually gives up.
     */
    public static int chargeItem(ItemStack fluxItem, PackEnergyStorage crystal, int cap) {
        if (fluxItem.isEmpty() || !(fluxItem.getItem() instanceof PortableEnderTerminalItem)) return 0;
        int room = PortableEnderTerminalItem.FLUX_CAPACITY - PortableEnderTerminalItem.getFlux(fluxItem);
        if (room <= 0) return 0;
        int want = Math.min(cap, room);
        int pulled = crystal.extractEnergy(want, false);
        if (pulled <= 0) return 0;
        int accepted = PortableEnderTerminalItem.charge(fluxItem, pulled); // 1 Flux == 1 FE
        // Refund any Flux the terminal couldn't take back into the crystal (never lose FE).
        if (accepted < pulled) crystal.receiveEnergy(pulled - accepted, false);
        return accepted;
    }

    // ---- block-level bridge: the placed pack speaks Forgework Flux directly ----

    /**
     * Expose a placed pack's Charge Crystal reservoir as Forgework's own {@code FLOW_ENERGY}
     * block capability, so a Forgework cable/battery charges it directly - the block-level
     * interop the item-only pack couldn't do (Forgework Flux is a block cap, not standard FE).
     * The adapter is a pure 1:1 delegate onto the pack's FE store, exactly as PhytoForge's
     * bridge does. Only present when a Charge Crystal is fitted.
     */
    public static void register(RegisterCapabilitiesEvent event,
                                BlockEntityType<PackContainerBlockEntity> type) {
        event.registerBlockEntity(ModCapabilities.FLOW_ENERGY, type, (be, side) -> {
            ItemStack pack = be.getPackStack();
            if (!TrinketAccess.has(pack, TrinketType.CHARGE_CRYSTAL)) return null;
            return new FlowAdapter(new PackEnergyStorage(be::getPackStack,
                    PackEnergyStorage.capacityFor(pack), PackEnergyStorage.transferFor(pack), be::setChanged));
        });
    }

    /** 1 Flux = 1 FE, straight delegation onto the pack's energy store - no unit conversion. */
    private record FlowAdapter(PackEnergyStorage fe) implements IFlowEnergyStorage {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return fe.receiveEnergy(maxReceive, simulate); }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return fe.extractEnergy(maxExtract, simulate); }
        @Override public int getEnergyStored() { return fe.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return fe.getMaxEnergyStored(); }
        @Override public boolean canReceive() { return fe.canReceive(); }
        @Override public boolean canExtract() { return fe.canExtract(); }
    }
}
