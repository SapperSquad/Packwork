package com.sappersquad.packwork.config;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.block.PackContainerBlock;
import com.sappersquad.packwork.block.PackContainerBlockEntity;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.reg.ModAttachments;
import com.sappersquad.packwork.reg.ModBlocks;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The config's {@code death.handling} switch, honoured at the moment a pack would become
 * an item on the ground, so it composes with everything else that touches death loot:
 *
 * <ul>
 *   <li><b>drop</b> (default) - this class does nothing; vanilla and every grave/corpse
 *       mod see the pack exactly as before.</li>
 *   <li><b>keep</b> - the pack never becomes an item entity; it goes into a copy-on-death
 *       attachment and is handed back on respawn (into the inventory, or set at the
 *       player's feet if it is somehow full).</li>
 *   <li><b>place</b> - each pack sets itself down as the placed pack block where the
 *       player fell, contents intact (the same lossless stack-adoption as sneak-placing
 *       it). No honest spot - a void death, solid rock, a border - falls back to
 *       <b>keep</b>, never to the void. Pause, never punish.</li>
 * </ul>
 *
 * <p>(Fabric) There is no LivingDropsEvent here, so the hook is the one choke point every
 * death drop actually funnels through: {@code LivingEntity.drop(stack, randomly,
 * thrownFromHand)}. The vanilla inventory reaches it via {@code Inventory.dropAll}, the
 * armour row via {@code EntityEquipment.dropAll}, and - checked in the Trinkets Updated
 * jar's own bytecode rather than assumed - a worn trinket reaches it via
 * {@code Player.drop} too. So one narrow injection covers carried AND worn packs, which is
 * exactly the coverage the NeoForge branches get from sweeping the drop list. A grave mod
 * that takes custody earlier means the call never happens and this never runs, so graves
 * keep full custody either way; with keepInventory on, nothing is dropped and this
 * naturally no-ops.
 */
public final class PackworkDeathHandling {

    /** Hand stashed packs back on respawn. */
    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> restoreKeptPacks(newPlayer));
    }

    /**
     * One stack on its way to becoming a death drop. Returns true when Packwork has taken
     * custody and the drop must not happen.
     *
     * <p>Package-visible and side-effect-transparent so the gametests can drive it directly
     * with mock players - the mixin is a one-line call into here and nothing else.
     */
    public static boolean claimDeathDrop(Player player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PackItem)) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
        PackworkConfig.DeathHandling mode = PackworkConfig.get().deathHandling();
        if (mode == PackworkConfig.DeathHandling.DROP) return false;

        ItemStack pack = stack.copy();
        if (mode == PackworkConfig.DeathHandling.PLACE
                && placePackAt(level, player.blockPosition(), pack)) {
            return true;
        }
        stash(player, pack); // keep, or place's never-void fallback
        return true;
    }

    /** Put a pack in the player's copy-on-death stash. */
    public static void stash(Player player, ItemStack pack) {
        List<ItemStack> kept = new ArrayList<>(player.getAttachedOrCreate(ModAttachments.KEPT_PACKS));
        kept.add(pack);
        player.setAttached(ModAttachments.KEPT_PACKS, kept);
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

    /** Hand stashed packs back - into the pockets, or at the feet if full. */
    public static void restoreKeptPacks(Player player) {
        List<ItemStack> stash = player.getAttachedOrCreate(ModAttachments.KEPT_PACKS);
        if (stash.isEmpty()) return;
        for (ItemStack s : stash) {
            if (!player.getInventory().add(s)) {
                player.drop(s, false);
            }
        }
        player.setAttached(ModAttachments.KEPT_PACKS, new ArrayList<>());
    }

    private PackworkDeathHandling() {}
}
