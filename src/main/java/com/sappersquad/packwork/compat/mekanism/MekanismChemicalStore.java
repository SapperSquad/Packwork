package com.sappersquad.packwork.compat.mekanism;

import com.sappersquad.packwork.block.PackContainerBlockEntity;
import com.sappersquad.packwork.pack.PackChemical;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.trinket.TrinketAccess;
import com.sappersquad.packwork.trinket.TrinketType;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * The ONLY class in Packwork allowed to import {@code mekanism.*}. Every entry point is
 * reached strictly behind a {@code ModList.isLoaded("mekanism")} gate (in
 * {@code PackworkCapabilities}), so neither this class nor Mekanism's ever classloads without
 * the mod. The Flask Harness's tank - a single chemical, tier-scaled, "bottled vapors" - is
 * exposed through Mekanism's own chemical capability so its pipes fill a pack.
 *
 * <p>Storage is the dist-neutral {@link PackChemical} component (a chemical id + an amount);
 * this class is the only place a {@code ChemicalStack} is built, by resolving the id against
 * {@link MekanismAPI#CHEMICAL_REGISTRY}. The capability tokens are recreated with Mekanism's
 * own {@code mekanism:chemical_handler} name + the standard MultiTypeCapability shape (item =
 * void, block = sided), so they are the SAME tokens Mekanism registers - the way to interop
 * with an api-only artifact that doesn't ship the token itself.
 */
public final class MekanismChemicalStore {

    private MekanismChemicalStore() {}

    private static final Identifier CHEMICAL_HANDLER =
            Identifier.fromNamespaceAndPath("mekanism", "chemical_handler");

    public static final ItemCapability<IChemicalHandler, Void> ITEM =
            ItemCapability.createVoid(CHEMICAL_HANDLER, IChemicalHandler.class);
    public static final BlockCapability<IChemicalHandler, Direction> BLOCK =
            BlockCapability.createSided(CHEMICAL_HANDLER, IChemicalHandler.class);

    /** Tank size for a pack, in mB (shared, dist-neutral formula in {@link PackChemical}). */
    public static long capacityFor(ItemStack pack) {
        return PackChemical.capacityFor(pack);
    }

    public static void registerItem(RegisterCapabilitiesEvent event, DeferredItem<?> holder) {
        event.registerItem(ITEM,
                (stack, ctx) -> TrinketAccess.has(stack, TrinketType.FLASK_HARNESS)
                        ? new Tank(() -> stack, capacityFor(stack), () -> {})
                        : null,
                holder.get());
    }

    public static void registerBlock(RegisterCapabilitiesEvent event,
                                     BlockEntityType<PackContainerBlockEntity> type) {
        event.registerBlockEntity(BLOCK, type,
                (be, side) -> TrinketAccess.has(be.getPackStack(), TrinketType.FLASK_HARNESS)
                        ? new Tank(be::getPackStack, capacityFor(be.getPackStack()), be::setChanged)
                        : null);
    }

    /** One-tank {@link IChemicalHandler} backed by the {@link PackChemical} component. */
    private record Tank(Supplier<ItemStack> live, long capacity, Runnable onChange) implements IChemicalHandler {

        @Override
        public int getChemicalTanks() {
            return 1;
        }

        @Override
        public ChemicalStack getChemicalInTank(int tank) {
            PackChemical pc = live.get().getOrDefault(ModComponents.PACK_CHEMICAL.get(), PackChemical.EMPTY);
            if (pc.isEmpty()) return ChemicalStack.EMPTY;
            Identifier id = Identifier.tryParse(pc.chemical());
            if (id == null) return ChemicalStack.EMPTY;
            Chemical chem = MekanismAPI.CHEMICAL_REGISTRY.getOptional(id).orElse(null);
            return chem == null ? ChemicalStack.EMPTY : new ChemicalStack(chem, pc.amount());
        }

        @Override
        public void setChemicalInTank(int tank, ChemicalStack stack) {
            if (stack.isEmpty()) write(null, 0);
            else write(stack.getChemical(), stack.getAmount());
        }

        @Override
        public long getChemicalTankCapacity(int tank) {
            return capacity;
        }

        @Override
        public boolean isValid(int tank, ChemicalStack stack) {
            return true;
        }

        @Override
        public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
            if (stack.isEmpty()) return ChemicalStack.EMPTY;
            ChemicalStack current = getChemicalInTank(0);
            if (!current.isEmpty() && current.getChemical() != stack.getChemical()) {
                return stack; // one chemical at a time - a different one is refused whole
            }
            long stored = current.getAmount();
            long room = capacity - stored;
            if (room <= 0) return stack;
            long accepted = Math.min(room, stack.getAmount());
            if (action.execute()) write(stack.getChemical(), stored + accepted);
            return accepted >= stack.getAmount()
                    ? ChemicalStack.EMPTY
                    : new ChemicalStack(stack.getChemical(), stack.getAmount() - accepted);
        }

        @Override
        public ChemicalStack extractChemical(int tank, long amount, Action action) {
            ChemicalStack current = getChemicalInTank(0);
            if (current.isEmpty() || amount <= 0) return ChemicalStack.EMPTY;
            long extracted = Math.min(current.getAmount(), amount);
            if (action.execute()) {
                long left = current.getAmount() - extracted;
                if (left <= 0) write(null, 0);
                else write(current.getChemical(), left);
            }
            return new ChemicalStack(current.getChemical(), extracted);
        }

        private void write(Chemical chem, long amount) {
            ItemStack s = live.get();
            if (s.isEmpty()) return;
            if (chem == null || amount <= 0) {
                s.set(ModComponents.PACK_CHEMICAL.get(), PackChemical.EMPTY);
            } else {
                Identifier id = MekanismAPI.CHEMICAL_REGISTRY.getKey(chem);
                s.set(ModComponents.PACK_CHEMICAL.get(),
                        new PackChemical(id == null ? "" : id.toString(), Math.min(amount, capacity)));
            }
            onChange.run();
        }
    }
}
