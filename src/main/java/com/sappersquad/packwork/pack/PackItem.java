package com.sappersquad.packwork.pack;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.IContainerFactory;

import java.util.List;

/**
 * The pack. Right-click (or the keybind) opens the tabbed organizer bound to
 * whichever inventory slot the pack sits in, so the same menu code serves a pack
 * in the hand and a pack pulled up from the inventory.
 */
public class PackItem extends Item {

    private final PackTier tier;

    public PackItem(Properties properties, PackTier tier) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public PackTier tier() {
        return tier;
    }

    public static PackTier tierOf(ItemStack stack) {
        return stack.getItem() instanceof PackItem p ? p.tier() : PackTier.LEATHER;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            int slot = findSlot(player, held, hand);
            if (slot >= 0) {
                openPack(player, slot);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Sneak-right-click on a block face sets the pack down as a placed block, moving its
     * ENTIRE state (layout, item store, trinkets, fluid/XP/energy) onto the block entity
     * and consuming the item - a lossless move, never a copy. A normal (non-sneak) click
     * falls through to {@link #use}, which opens the organizer instead.
     */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        Level level = ctx.getLevel();
        BlockPlaceContext place = new BlockPlaceContext(ctx);
        if (!place.canPlace()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = place.getClickedPos();
        BlockState state = com.sappersquad.packwork.reg.ModBlocks.PACK.get().getStateForPlacement(place);
        if (state == null || !state.canSurvive(level, pos)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = ctx.getItemInHand();
        if (!level.setBlock(pos, state, 3)) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(pos) instanceof com.sappersquad.packwork.block.PackContainerBlockEntity be) {
            ItemStack one = held.copy();
            one.setCount(1);
            be.setPackStack(one); // adopt the full pack state; nothing re-serialised
        }
        level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
        held.shrink(1);
        return InteractionResult.CONSUME;
    }

    /** The inventory slot index of the held stack, so the menu can rebind to it. */
    private static int findSlot(Player player, ItemStack held, InteractionHand hand) {
        Inventory inv = player.getInventory();
        if (hand == InteractionHand.OFF_HAND) {
            return Inventory.SLOT_OFFHAND; // 40
        }
        // main hand -> currently selected hotbar slot
        if (inv.getItem(inv.getSelectedSlot()) == held) return inv.getSelectedSlot();
        // fall back to a scan
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i) == held) return i;
        }
        return -1;
    }

    /** Open the organizer for the pack at the given inventory slot. */
    public static void openPack(Player player, int slot) {
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return player.getInventory().getItem(slot).getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player p) {
                return com.sappersquad.packwork.pack.PackMenu.server(id, playerInv, slot);
            }
        };
        // The tier rides in the open packet so the client builds the SAME number of
        // trinket sockets as the server, even before its inventory slot syncs - a
        // mismatch there overruns the container-content packet and drops the player.
        PackTier tier = tierOf(player.getInventory().getItem(slot));
        player.openMenu(provider, buf -> {
            buf.writeByte(HOST_CARRIED);
            buf.writeVarInt(slot);
            buf.writeVarInt(tier.ordinal());
        });
    }

    /**
     * Open the organizer for the pack worn in the Curios back slot. The host container
     * comes ready-built from the gated compat class (its getter live-resolves the worn
     * stack every access), so this class - and the menu - never import curios.
     */
    public static void openWornPack(net.minecraft.server.level.ServerPlayer player,
                                    PackStackSlotContainer host) {
        ItemStack worn = host.getPack();
        if (!(worn.getItem() instanceof PackItem)) return;
        PackTier tier = tierOf(worn);
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return host.getPack().getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player p) {
                return PackMenu.serverForWorn(id, playerInv, host, tier);
            }
        };
        // Same contract as the other hosts: the tier rides so the client builds the same
        // trinket-socket count as the server before the hidden host slot syncs.
        player.openMenu(provider, buf -> {
            buf.writeByte(HOST_WORN);
            buf.writeVarInt(tier.ordinal());
        });
    }

    // The open packet's host kind: which of the menu's three bindings the client builds.
    private static final int HOST_CARRIED = 0;
    private static final int HOST_BLOCK = 1;
    private static final int HOST_WORN = 2;

    /** The block's open path writes its kind through here so the three writers and the one
     *  reader can never drift. */
    public static void writeBlockHost(net.minecraft.network.RegistryFriendlyByteBuf buf,
                                      net.minecraft.core.BlockPos pos, PackTier tier) {
        buf.writeByte(HOST_BLOCK);
        buf.writeBlockPos(pos);
        buf.writeVarInt(tier.ordinal());
    }

    /**
     * Client menu factory: a host-kind byte says whether this is a placed pack (bind to the
     * block at the given pos), a worn one (bind to the synced host slot), or a carried one
     * (bind to the inventory slot); the tier rides along so the client builds the same slot
     * count as the server before anything syncs.
     */
    public static final IContainerFactory<PackMenu> CLIENT_FACTORY =
            (id, playerInv, buf) -> {
                int kind = buf.readByte();
                if (kind == HOST_BLOCK) {
                    net.minecraft.core.BlockPos pos = buf.readBlockPos();
                    PackTier tier = PackTier.values()[buf.readVarInt()];
                    return PackMenu.clientForBlock(id, playerInv, pos, tier);
                }
                if (kind == HOST_WORN) {
                    PackTier tier = PackTier.values()[buf.readVarInt()];
                    return PackMenu.clientForWorn(id, playerInv, tier);
                }
                int slot = buf.readVarInt();
                PackTier tier = PackTier.values()[buf.readVarInt()];
                return PackMenu.client(id, playerInv, slot, tier);
            };

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay tooltipDisplay,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        com.sappersquad.packwork.pack.PackContents contents = stack.get(com.sappersquad.packwork.reg.ModComponents.PACK_CONTENTS.get());
        int used = 0;
        if (contents != null) {
            used = (int) contents.nonEmptyItemCopyStream().count();
        }
        tooltip.accept(Component.translatable("packwork.pack.slots_used", used, tier.capacity())
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("packwork.pack.hint")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
