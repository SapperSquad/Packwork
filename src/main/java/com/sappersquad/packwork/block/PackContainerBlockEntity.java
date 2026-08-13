package com.sappersquad.packwork.block;

import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.reg.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed pack. The whole pack lives on ONE {@link ItemStack} exactly as it does in the
 * hand - the same data components carry the layout, the flat item store, the trinkets, and
 * the fluid/XP/energy stores - so placing and breaking is a lossless move of that one
 * stack, never a re-serialisation that could drop or dupe a field.
 *
 * <p>The client render comes from the {@code tier} blockstate property (per-tier models +
 * textures), not from this entity, so the full stack stays server-side and reaches an open GUI
 * through the menu's own synced host slot - a 256-slot pack never floods block updates. The
 * tier is still tracked here for the server-side GUI-open packet and drops.
 */
public class PackContainerBlockEntity extends BlockEntity {

    private ItemStack packStack = ItemStack.EMPTY;
    private PackTier tier = PackTier.LEATHER;

    public PackContainerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PACK.get(), pos, state);
    }

    /** The live pack stack: every store reads/writes it directly, same as a carried pack. */
    public ItemStack getPackStack() {
        return packStack;
    }

    /** Adopt a pack stack (on placement) or replace it. Keeps the per-tier render + caps in step. */
    public void setPackStack(ItemStack stack) {
        this.packStack = stack;
        this.tier = PackItem.tierOf(stack);
        setChanged();
        if (level != null) {
            // (Fabric: no capability-cache invalidation needed - the API lookup provider
            // re-derives off the live held stack on every query.)
            if (!level.isClientSide()) {
                BlockState st = getBlockState();
                // Keep the rendered tier (a blockstate property) in step with the stored pack.
                // On normal placement getStateForPlacement already set it, so this is a no-op; it
                // covers a stack set AFTER a bare placement (test harness) or a whole-pack swap.
                if (st.hasProperty(PackContainerBlock.TIER) && st.getValue(PackContainerBlock.TIER) != tier) {
                    level.setBlock(worldPosition, st.setValue(PackContainerBlock.TIER, tier), 3);
                } else {
                    level.sendBlockUpdated(worldPosition, st, st, 3);
                }
            }
        }
    }

    public PackTier getTier() {
        return tier;
    }

    public boolean isEmpty() {
        return packStack.isEmpty();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.store("Pack", ItemStack.OPTIONAL_CODEC, packStack);
    }

    /**
     * Reads both shapes of tag: the SERVER's saved form (a "Pack" stack) and the CLIENT's
     * light update form (a "Tier" int only - see {@link #getUpdateTag}). Vanilla applies
     * update packets through {@code loadWithComponents -> loadAdditional} (verified in the
     * 26.1 ClientPacketListener sources; the NeoForge branches hook onDataPacket instead),
     * so a tier-only tag must never blank the client tier back to the default - the tinted
     * render's tier comes from whichever field the tag actually carries.
     */
    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        var read = input.read("Pack", ItemStack.OPTIONAL_CODEC);
        if (read.isPresent()) {
            this.packStack = read.get();
            this.tier = PackItem.tierOf(packStack);
        } else {
            // no Pack field: either a fresh/empty BE or the client's tier-only update tag
            this.packStack = ItemStack.EMPTY;
            this.tier = PackTier.values()[input.getIntOr("Tier", tier.ordinal())];
        }
    }

    // ---- client render sync: TIER ONLY (never the contents) ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Tier", tier.ordinal());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
