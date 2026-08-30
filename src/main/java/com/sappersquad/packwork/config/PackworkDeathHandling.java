package com.sappersquad.packwork.config;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.block.PackContainerBlock;
import com.sappersquad.packwork.block.PackContainerBlockEntity;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.reg.ModAttachments;
import com.sappersquad.packwork.reg.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The config's {@code death.handling} switch, honoured at the drops stage so it composes
 * with everything else that touches death loot:
 *
 * <ul>
 *   <li><b>drop</b> (default) - this class does nothing; vanilla and every grave/corpse
 *       mod see the pack exactly as before.</li>
 *   <li><b>keep</b> - pack drops are swept out of the drop list into a copy-on-death
 *       attachment and handed back on respawn (into the inventory, or set at the
 *       player's feet if it is somehow full).</li>
 *   <li><b>place</b> - each pack sets itself down as the placed pack block where the
 *       player fell, contents intact (the same lossless stack-adoption as sneak-placing
 *       it). No honest spot - a void death, solid rock, a border - falls back to
 *       <b>keep</b>, never to the void. Pause, never punish.</li>
 * </ul>
 *
 * <p>Runs at LOW priority so mods that add their own drops (Curios drops the worn back
 * slot here too) and mods that CANCEL drops (graves that collect everything) act first:
 * a cancelled event never reaches us, so a grave mod keeps full custody. With
 * keepInventory on, packs never enter the drop list and this class naturally no-ops.
 */
@EventBusSubscriber(modid = Packwork.MODID)
public final class PackworkDeathHandling {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerDrops(LivingDropsEvent event) {
        // Deliberately Player, not ServerPlayer: the event is server-side by contract and
        // gametest mock players are bare Players - the narrow gate silently disabled
        // pack-first pickup in tests once already; don't repeat it here.
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (event.isCanceled()) return; // a grave/corpse mod took custody; respect it
        PackworkConfig.DeathHandling mode = PackworkConfig.get().deathHandling();
        if (mode == PackworkConfig.DeathHandling.DROP) return;
        sweepPackDrops(player, level, player.blockPosition(), event.getDrops(), mode);
    }

    /**
     * Pull every pack out of {@code drops} per the config mode. Package-visible and
     * side-effect-transparent so the gametests can drive it directly with mock players.
     */
    public static void sweepPackDrops(Player player, ServerLevel level, BlockPos deathPos,
                                      Collection<ItemEntity> drops, PackworkConfig.DeathHandling mode) {
        List<ItemStack> kept = new ArrayList<>();
        Iterator<ItemEntity> it = drops.iterator();
        while (it.hasNext()) {
            ItemEntity ie = it.next();
            ItemStack stack = ie.getItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof PackItem)) continue;
            it.remove();
            ie.discard();
            if (mode == PackworkConfig.DeathHandling.PLACE && placePackAt(level, deathPos, stack.copy())) {
                continue;
            }
            kept.add(stack.copy()); // keep, or place's never-void fallback
        }
        if (!kept.isEmpty()) {
            List<ItemStack> stash = new ArrayList<>(player.getData(ModAttachments.KEPT_PACKS.get()));
            stash.addAll(kept);
            player.setData(ModAttachments.KEPT_PACKS.get(), stash);
        }
    }

    /**
     * Set the pack down as its block near {@code deathPos}: the same lossless
     * stack-adoption as sneak-placing it, so contents/trinkets/stores ride untouched.
     * Scans a small column-first neighbourhood for a replaceable, in-world spot
     * (up to 4 out, 8 up, 2 down - death in a cave, at a wall, or mid-fall all land
     * somewhere sensible). Returns false when nothing honest exists (void, solid rock).
     */
    public static boolean placePackAt(ServerLevel level, BlockPos deathPos, ItemStack pack) {
        int minY = level.getMinY();
        int maxY = level.getMaxY() - 1;
        BlockPos start = new BlockPos(deathPos.getX(),
                Math.max(minY, Math.min(maxY, deathPos.getY())), deathPos.getZ());
        int[] yOffsets = {0, 1, -1, 2, -2, 3, 4, 5, 6, 7, 8};
        for (int r = 0; r <= 4; r++) {
            for (int dy : yOffsets) {
                int y = start.getY() + dy;
                if (y < minY || y > maxY) continue;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // ring only
                        BlockPos pos = new BlockPos(start.getX() + dx, y, start.getZ() + dz);
                        if (tryPlace(level, pos, pack)) return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean tryPlace(ServerLevel level, BlockPos pos, ItemStack pack) {
        if (!level.isLoaded(pos) || !level.getBlockState(pos).canBeReplaced()) return false;
        BlockState state = ModBlocks.PACK.get().defaultBlockState()
                .setValue(PackContainerBlock.TIER, PackItem.tierOf(pack));
        if (!level.setBlockAndUpdate(pos, state)) return false;
        if (!(level.getBlockEntity(pos) instanceof PackContainerBlockEntity be)) {
            return false; // should not happen; the pack falls back to KEEP, never the void
        }
        be.setPackStack(pack);
        Packwork.LOGGER.info("A fallen adventurer's pack set itself down at {}", pos.toShortString());
        return true;
    }

    /** Hand stashed packs back on respawn - into the pockets, or at the feet if full. */
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        restoreKeptPacks(event.getEntity());
    }

    /** Split out for the gametests (mock players never fire the respawn event). */
    public static void restoreKeptPacks(Player player) {
        List<ItemStack> stash = player.getData(ModAttachments.KEPT_PACKS.get());
        if (stash.isEmpty()) return;
        for (ItemStack s : stash) {
            if (!player.getInventory().add(s)) {
                player.drop(s, false);
            }
        }
        player.setData(ModAttachments.KEPT_PACKS.get(), new ArrayList<>());
    }

    private PackworkDeathHandling() {}
}
