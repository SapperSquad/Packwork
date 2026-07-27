package com.sappersquad.packwork.trinket;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackInventory;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.EnumSet;
import java.util.List;

/**
 * Runs the "active" trinket effects each server tick for every pack a player carries.
 * Passive trinkets (Bottomless capacity, Compass Rose void, Quill &amp; Ledger) are read
 * where they matter instead of here; Quick-Draw Straps react to an item breaking (below).
 *
 * <p>Everything is throttled and bounded; nothing here can void or dupe (magnet respects
 * the void list only when a Compass Rose is present, and only inserts what fits).
 */
@EventBusSubscriber(modid = Packwork.MODID)
public final class TrinketEffects {

    private static final double MAGNET_RANGE = 5.0;
    private static final int REPAIR_PER_TICK = 1;
    /** Cap the Charge Crystal's Flux hand-off to a Forgework terminal per tick (with FE tools it's uncapped). */
    private static final int FLUX_PER_TICK = 5_000;

    /** Gate every touch of the Forgework bridge so its com.forgework.* imports never classload without the mod. */
    private static final boolean FORGEWORK_LOADED = ModList.get().isLoaded("forgework");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) return;
        long time = sp.level().getGameTime();
        Inventory inv = sp.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            applyPack(sp, inv.getItem(i), time);
        }
    }

    /**
     * Run one pack's active trinket effects for this tick. Shared by the inventory scan above
     * and the Curios back-slot ({@code compat/curios}), so a worn pack's magnet / restock /
     * repair / soul-vial / charge all keep working exactly as a pocketed one's do.
     */
    public static void applyWornPack(ServerPlayer sp, ItemStack packStack) {
        applyPack(sp, packStack, sp.level().getGameTime());
    }

    private static void applyPack(ServerPlayer sp, ItemStack packStack, long time) {
        if (!(packStack.getItem() instanceof PackItem)) return;
        EnumSet<TrinketType> installed = TrinketAccess.installed(packStack);
        if (installed.isEmpty()) return;
        PackInventory pack = new PackInventory(packStack, PackItem.tierOf(packStack));

        if (installed.contains(TrinketType.LODESTONE) && time % 4 == 0) magnet(sp, packStack, pack);
        if (installed.contains(TrinketType.RESTOCK) && time % 10 == 0) restock(sp, pack);
        if (installed.contains(TrinketType.REPAIR) && time % 20 == 0) repair(sp, pack);
        if (installed.contains(TrinketType.SOUL_VIAL) && time % 10 == 0) autoMend(sp, packStack);
        if (installed.contains(TrinketType.CHARGE_CRYSTAL) && time % 10 == 0) charge(sp, packStack);
        if (installed.contains(TrinketType.FIELD_FURNACE) && time % SMELT_EVERY == 0) fieldFurnace(sp, packStack, pack);
        if (installed.contains(TrinketType.PROVISIONER) && time % 20 == 0) provision(sp, pack);
        if (installed.contains(TrinketType.TORCHBEARER) && time % 20 == 0) torchbearer(sp, pack);
    }

    // ---- Field Furnace: banked campfire embers that cook raw ore and raw food ----

    /** How often the embers turn out a finished piece. */
    private static final int SMELT_EVERY = 60;
    /** Ticks of burn one item costs - the same as a furnace, so a lump of coal is still 8 things. */
    private static final int BURN_PER_ITEM = 200;

    /**
     * Cook one thing from the pack, on embers fed by the pack's own fuel.
     *
     * <p>Deliberately narrow: only <em>raw ore</em> and <em>raw food</em> are cooked. A furnace
     * would happily turn your cobblestone into stone and your logs into charcoal, which is not a
     * favour when it happens behind your back. If the finished piece won't fit, the raw one goes
     * straight back and no embers are spent - the pack pauses, it never punishes.
     */
    private static void fieldFurnace(ServerPlayer sp, ItemStack packStack, PackInventory pack) {
        smeltOnce(sp.serverLevel(), packStack, pack);
    }

    /**
     * Cook one item, and only if the finished piece definitely fits: the room is checked BEFORE
     * the raw one comes out, so the swap is atomic and nothing can be stranded or lost. Returns
     * true if something was cooked.
     */
    public static boolean smeltOnce(net.minecraft.server.level.ServerLevel level,
                             ItemStack packStack, PackInventory pack) {
        var embersKey = com.sappersquad.packwork.reg.ModComponents.PACK_EMBERS.get();
        int embers = packStack.getOrDefault(embersKey, 0);
        if (embers < BURN_PER_ITEM) {
            embers += stokeEmbers(pack);
            if (embers < BURN_PER_ITEM) {
                packStack.set(embersKey, embers);
                return false;
            }
        }
        for (int i = 0; i < pack.getSlots(); i++) {
            ItemStack raw = pack.getStackInSlot(i);
            if (raw.isEmpty() || !worthCooking(raw)) continue;
            var input = new net.minecraft.world.item.crafting.SingleRecipeInput(raw);
            var recipe = level.getServer().getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) continue;
            ItemStack out = recipe.get().value().assemble(input, level.registryAccess());
            if (out.isEmpty()) continue;
            // no room for what it would make? leave the raw one exactly where it is
            if (!insertAll(pack, out.copy(), true).isEmpty()) continue;

            ItemStack taken = pack.extractItem(i, 1, false);
            if (taken.isEmpty()) continue;
            insertAll(pack, out.copy(), false);
            packStack.set(embersKey, embers - BURN_PER_ITEM);
            return true;
        }
        packStack.set(embersKey, embers);
        return false;
    }

    /**
     * What the Field Furnace is allowed to burn. Deliberately NOT "anything with a burn time" -
     * a live playtest had it quietly eating oak planks off the top of the pack, which is exactly
     * the behind-your-back behaviour the cooking list already avoids. Datapack-tunable.
     */
    public static final net.minecraft.tags.TagKey<Item> FURNACE_FUEL =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("packwork", "furnace_fuel"));

    /** Burn one piece of proper fuel out of the pack; its container (a bucket, say) goes back in. */
    private static int stokeEmbers(PackInventory pack) {
        for (int i = 0; i < pack.getSlots(); i++) {
            ItemStack s = pack.getStackInSlot(i);
            if (s.isEmpty() || !s.is(FURNACE_FUEL)) continue;
            int burn = s.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
            if (burn <= 0) continue;
            ItemStack fuel = pack.extractItem(i, 1, false);
            if (fuel.isEmpty()) continue;
            ItemStack remainder = fuel.getCraftingRemainingItem();
            if (!remainder.isEmpty()) insertAll(pack, remainder);
            return burn;
        }
        return 0;
    }

    /** Raw ore and raw food only - see {@link #fieldFurnace}. */
    private static boolean worthCooking(ItemStack stack) {
        if (stack.getFoodProperties(null) != null) return true;
        return stack.is(net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS)
                || stack.is(net.neoforged.neoforge.common.Tags.Items.ORES);
    }

    // ---- Provisioner's Pouch: eats from pack stock before you start starving ----

    /**
     * When you're down to three haunches, the pouch feeds you the CHEAPEST safe thing in the
     * pack - your golden apples stay yours. Anything with a harmful effect (rotten flesh,
     * pufferfish, spider eyes) is left well alone, and the bowl or bottle goes back in the pack.
     */
    private static void provision(ServerPlayer sp, PackInventory pack) {
        if (!feedFrom(sp, pack)) return;
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_BURP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1f);
    }

    /**
     * Eat the plainest thing in the pack. Returns true if the player actually ate.
     *
     * <p>"Plainest" means: no effects attached at all, and not on the never-auto-eat list.
     * That rules out rotten flesh and pufferfish for the obvious reason, and golden apples and
     * suspicious stew for a better one - a food you went to trouble for is not rations, and the
     * pouch has no business spending it. Among what's left it takes the least filling first.
     */
    public static boolean feedFrom(Player player, PackInventory pack) {
        if (player.getFoodData().getFoodLevel() > 6) return false;
        int best = -1, bestNutrition = Integer.MAX_VALUE;
        for (int i = 0; i < pack.getSlots(); i++) {
            ItemStack s = pack.getStackInSlot(i);
            if (s.isEmpty()) continue;
            var food = s.getFoodProperties(player);
            if (food == null || !isRations(s, food)) continue;
            if (food.nutrition() < bestNutrition) {
                bestNutrition = food.nutrition();
                best = i;
            }
        }
        if (best < 0) return false;
        ItemStack meal = pack.extractItem(best, 1, false);
        if (meal.isEmpty()) return false;
        ItemStack remainder = meal.finishUsingItem(player.level(), player); // vanilla eating, effects and all
        if (!remainder.isEmpty()) {
            ItemStack left = insertAll(pack, remainder);          // the bowl goes back in the pack
            if (!left.isEmpty() && !player.getInventory().add(left)) player.drop(left, false);
        }
        return true;
    }

    /**
     * Items a Provisioner's Pouch will never eat on your behalf, whatever their nutrition.
     * Datapack-tunable, so a pack can add its own delicacies (or hand back the chorus fruit).
     */
    public static final net.minecraft.tags.TagKey<Item> NEVER_AUTO_EAT =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("packwork", "never_auto_eat"));

    private static boolean isRations(ItemStack stack, net.minecraft.world.food.FoodProperties food) {
        if (!food.effects().isEmpty()) return false;   // anything with an effect on it stays yours
        return !stack.is(NEVER_AUTO_EAT);
    }

    // ---- Torchbearer's Loop: lights the dark from pack stock ----

    /**
     * Standing somewhere genuinely dark, the loop sets one torch down from pack stock. It is
     * self-limiting - the moment the light comes up it stops - and if the torch can't stand
     * there, it goes straight back in the pack.
     */
    private static void torchbearer(ServerPlayer sp, PackInventory pack) {
        var level = sp.serverLevel();
        net.minecraft.core.BlockPos pos = sp.blockPosition();
        if (level.getMaxLocalRawBrightness(pos) > 3) return;
        if (!level.getBlockState(pos).canBeReplaced()) return;
        var torchState = net.minecraft.world.level.block.Blocks.TORCH.defaultBlockState();
        if (!torchState.canSurvive(level, pos)) return;

        for (int i = 0; i < pack.getSlots(); i++) {
            ItemStack s = pack.getStackInSlot(i);
            if (!s.is(net.minecraft.world.item.Items.TORCH)) continue;
            ItemStack torch = pack.extractItem(i, 1, false);
            if (torch.isEmpty()) continue;
            if (level.setBlockAndUpdate(pos, torchState)) {
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOOD_PLACE,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1f);
            } else {
                ItemStack back = insertAll(pack, torch);   // couldn't set it down - keep it
                if (!back.isEmpty() && !sp.getInventory().add(back)) sp.drop(back, false);
            }
            return;
        }
    }

    // ---- Angler's Creel: the catch goes in the pack ----

    /**
     * Reel something in with a creel fitted and it lands in the pack instead of bouncing off your
     * chest - and the Catch compartment already has a place for it. Anything the pack can't take
     * is left in the drop list so vanilla still hands it over; nothing is ever swallowed.
     */
    @SubscribeEvent
    public static void onItemFished(net.neoforged.neoforge.event.entity.player.ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack packStack = inv.getItem(i);
            if (!(packStack.getItem() instanceof PackItem)) continue;
            if (!TrinketAccess.has(packStack, TrinketType.ANGLERS_CREEL)) continue;

            stowCatch(new PackInventory(packStack, PackItem.tierOf(packStack)), event.getDrops());
            return;
        }
    }

    /**
     * Move the day's catch into the pack, leaving behind only what wouldn't fit - vanilla then
     * spawns whatever is left, so a full pack costs you nothing.
     */
    public static void stowCatch(PackInventory pack, java.util.List<ItemStack> drops) {
        for (int d = 0; d < drops.size(); d++) {
            ItemStack caught = drops.get(d);
            if (caught.isEmpty() || caught.getItem() instanceof PackItem) continue;
            drops.set(d, insertAll(pack, caught));
        }
        drops.removeIf(ItemStack::isEmpty);
    }

    // ---- Herbalist's Bundle: replants what you harvest, from your own seed stock ----

    /**
     * Break a grown crop with the bundle fitted and it goes straight back in the ground, using a
     * seed out of the pack. It only spends a seed the pack actually holds, and if the ground is
     * taken by the time it gets there the seed goes back - so it can neither dupe nor lose one.
     */
    @SubscribeEvent
    public static void onCropHarvested(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;
        var state = event.getState();
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop)) return;
        if (!crop.isMaxAge(state)) return;

        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack packStack = inv.getItem(i);
            if (!(packStack.getItem() instanceof PackItem)) continue;
            if (!TrinketAccess.has(packStack, TrinketType.HERBALIST)) continue;

            PackInventory pack = new PackInventory(packStack, PackItem.tierOf(packStack));
            ItemStack seed = takeSeedFor(pack, crop);
            if (seed.isEmpty()) return;

            net.minecraft.core.BlockPos pos = event.getPos().immutable();
            var young = crop.defaultBlockState();
            level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 1, () -> {
                if (level.getBlockState(pos).canBeReplaced() && young.canSurvive(level, pos)) {
                    level.setBlockAndUpdate(pos, young);
                } else {
                    ItemStack back = insertAll(pack, seed);   // ground taken - the seed comes home
                    if (!back.isEmpty() && !sp.getInventory().add(back)) sp.drop(back, false);
                }
            }));
            return;
        }
    }

    /** One seed out of the pack that plants this crop, or EMPTY if the pack has none. */
    public static ItemStack takeSeedFor(PackInventory pack, net.minecraft.world.level.block.CropBlock crop) {
        for (int i = 0; i < pack.getSlots(); i++) {
            ItemStack s = pack.getStackInSlot(i);
            if (s.isEmpty() || !(s.getItem() instanceof net.minecraft.world.item.BlockItem bi)) continue;
            if (bi.getBlock() != crop) continue;
            return pack.extractItem(i, 1, false);
        }
        return ItemStack.EMPTY;
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
        // Forgework interop (gated): the crystal also tops up any Forgework portable
        // terminal you're carrying, 1 Flux = 1 FE. Reached only when forgework is loaded,
        // so ForgeworkFluxBridge (and com.forgework.*) never classloads without it.
        if (FORGEWORK_LOADED && crystal.getEnergyStored() > 0) {
            com.sappersquad.packwork.compat.forgework.ForgeworkFluxBridge.topUpCarried(sp, crystal, FLUX_PER_TICK);
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

    // ---- pack-first pickup: the Lodestone routes what the pack can FILE straight in ----

    /** Gate every touch of the Curios compat class so its imports never classload without the mod. */
    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    /**
     * The mining case the magnet always lost: an item at the player's feet is vanilla's the
     * instant they touch it, so mined cobble went to the pockets, never the pack. This hook
     * fires FIRST - {@code EventHooks.fireItemPickupPre} runs "before any other processing
     * occurs" in {@code ItemEntity.playerTouch} (verified in the 21.1.235 sources), and the
     * event contract explicitly permits mutating the entity's stored stack (never
     * {@code setItem}).
     *
     * <p>The routing rule, per pack (inventory order, then the Curios-worn pack - the same
     * order the tick effects scan): with a Lodestone fitted and the pack's PACK-FIRST toggle
     * on, a pickup the pack can FILE goes straight in - "file" meaning it routes to a
     * non-Loose compartment, or is pinned anywhere, or the pack already holds that very item.
     * Anything that would land in Loose falls through to vanilla untouched: new, unknown loot
     * must never vanish into the bag. A Compass Rose discards void-listed pickups exactly as
     * the magnet does (the trash-collector contract).
     *
     * <p>Conservation: insert what fits (depth-aware), shrink the ground stack by exactly the
     * amount inserted, and deny vanilla only when NOTHING remains - a partial fit leaves the
     * remainder to vanilla pickup. Packs are never intercepted (nesting stays blocked). The
     * routed portion still plays the fly-to-player pickup cue.
     */
    @SubscribeEvent
    public static void onItemPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        // the event is server-only by contract; the level guard is belt-and-braces, and the
        // player is deliberately NOT narrowed to ServerPlayer (gametest mock players aren't one)
        if (player.level().isClientSide()) return;
        // never fight an explicit decision by vanilla-to-be or another mod, and never jump
        // the pickup delay (a just-dropped item must not be re-swallowed instantly)
        if (event.canPickup() != net.neoforged.neoforge.common.util.TriState.DEFAULT) return;
        ItemEntity ie = event.getItemEntity();
        if (ie.hasPickUpDelay()) return;
        ItemStack ground = ie.getItem();
        if (ground.isEmpty() || ground.getItem() instanceof PackItem) return;

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (routePickupInto(player, inv.getItem(i), event, ie)) return;
        }
        if (CURIOS_LOADED && player instanceof ServerPlayer sp) {
            ItemStack worn = com.sappersquad.packwork.compat.curios.CuriosCompat.wornPack(sp);
            routePickupInto(player, worn, event, ie);
        }
    }

    /** Try one pack against a ground item. True if this pack settled the pickup (filed it
     *  fully, filed a part of it, or binned it by the Rose contract). */
    private static boolean routePickupInto(Player player, ItemStack packStack,
                                           net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event,
                                           ItemEntity ie) {
        if (!(packStack.getItem() instanceof PackItem)) return false;
        if (!TrinketAccess.has(packStack, TrinketType.LODESTONE)) return false;
        var layout = packStack.getOrDefault(com.sappersquad.packwork.reg.ModComponents.PACK_LAYOUT.get(),
                com.sappersquad.packwork.sort.PackLayout.EMPTY);
        if (!layout.packFirst()) return false; // the GUI toggle: off = pure vanilla

        ItemStack ground = ie.getItem();
        // Compass Rose void contract, exactly as the magnet path
        if (TrinketAccess.has(packStack, TrinketType.COMPASS_ROSE)
                && layout.voids(BuiltInRegistries.ITEM.getKey(ground.getItem()))) {
            ground.setCount(0);
            ie.discard();
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            return true;
        }

        PackInventory pack = new PackInventory(packStack, PackItem.tierOf(packStack));
        if (!packWouldFile(packStack, layout, ground, pack)) return false;

        int before = ground.getCount();
        ItemStack leftover = insertAll(pack, ground.copy());
        int moved = before - leftover.getCount();
        if (moved <= 0) return false;               // this pack is full for it - try the next

        ground.shrink(moved);                        // the documented-legal mutation
        player.take(ie, moved);                      // the vanilla fly-to-player pickup cue
        if (ground.isEmpty()) {
            event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
            ie.discard();
        }
        // a remainder stays on the ground stack with canPickup DEFAULT, so vanilla picks it
        // up into the pockets this same touch - conservation to the item
        return true;
    }

    /**
     * Would this pack FILE the item? Yes when a pin claims it anywhere, when the rules route
     * it to any non-Loose compartment, or when the pack already holds that exact item (the
     * top-up case). No for everything that would land in Loose - the pack only swallows what
     * it genuinely knows where to put.
     */
    private static boolean packWouldFile(ItemStack packStack, com.sappersquad.packwork.sort.PackLayout layout,
                                         ItemStack stack, PackInventory pack) {
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (layout.pinnedTab(itemId) != null) return true;
        var tabs = com.sappersquad.packwork.sort.SortEngine.tabsFor(layout, TrinketAccess.installed(packStack));
        if (!com.sappersquad.packwork.sort.AutoTabs.LOOSE_ID.equals(
                com.sappersquad.packwork.sort.SortEngine.route(stack, tabs, layout))) {
            return true;
        }
        for (int i = 0; i < pack.getSlots(); i++) {
            if (ItemStack.isSameItemSameComponents(pack.getStackInSlot(i), stack)) return true;
        }
        return false;
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

    /**
     * Quick-Draw Straps: when a held tool breaks (or a held stack is used to nothing),
     * pull an identical one from a pack you carry straight into that hand - no fumbling
     * for a spare. Fires only for gear that emptied a hand in use (getHand() != null),
     * so setting an item aside never triggers a refill, and it can only hand back what
     * the pack actually holds, so it never dupes.
     */
    @SubscribeEvent
    public static void onHeldItemBroken(PlayerDestroyItemEvent event) {
        InteractionHand hand = event.getHand();
        if (hand == null || !(event.getEntity() instanceof ServerPlayer sp)) return;
        ItemStack broken = event.getOriginal();
        if (broken.isEmpty() || broken.getItem() instanceof PackItem) return;
        if (!sp.getItemInHand(hand).isEmpty()) return; // only step in when that hand went empty

        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack packStack = inv.getItem(i);
            if (!(packStack.getItem() instanceof PackItem)) continue;
            if (!TrinketAccess.has(packStack, TrinketType.QUICK_DRAW)) continue;
            ItemStack replacement = pullReplacement(
                    new PackInventory(packStack, PackItem.tierOf(packStack)), broken.getItem());
            if (!replacement.isEmpty()) {
                sp.setItemInHand(hand, replacement);
                return;
            }
        }
    }

    /**
     * Pull one stack of {@code wanted} out of the pack - a fresh copy of that tool, or a
     * refill of that stackable - removing it from the store. Returns EMPTY if the pack
     * holds none. Conserves exactly: it only hands back what it takes out, so a
     * Quick-Draw refill can never mint a duplicate.
     */
    public static ItemStack pullReplacement(PackInventory pack, Item wanted) {
        for (int i = 0; i < pack.getSlots(); i++) {
            ItemStack inPack = pack.getStackInSlot(i);
            if (inPack.isEmpty() || inPack.getItem() != wanted) continue;
            return pack.extractItem(i, inPack.getMaxStackSize(), false);
        }
        return ItemStack.EMPTY;
    }

    static ItemStack insertAll(PackInventory pack, ItemStack stack) {
        return insertAll(pack, stack, false);
    }

    /** Merge into part-filled stacks first, then fill empties. {@code simulate} touches nothing. */
    static ItemStack insertAll(PackInventory pack, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int i = 0; i < pack.getSlots() && !remaining.isEmpty(); i++) {
            if (!pack.getStackInSlot(i).isEmpty()) remaining = pack.insertItem(i, remaining, simulate);
        }
        for (int i = 0; i < pack.getSlots() && !remaining.isEmpty(); i++) {
            if (pack.getStackInSlot(i).isEmpty()) remaining = pack.insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    private TrinketEffects() {}
}
