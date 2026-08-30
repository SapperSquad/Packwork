package com.sappersquad.packwork.config;

import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.sort.PackLayout;
import com.sappersquad.packwork.trinket.TrinketType;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The packmaker's lever: every tunable Packwork exposes, read from
 * {@code config/packwork-server.toml} (written with its documentation as comments the
 * first time the game runs). The same file, the same keys, the same parser on NeoForge
 * and Fabric - a pack ships one config for both loaders.
 *
 * <p><b>Authority:</b> the server's file is the truth. On login the server sends its
 * values to the client (the {@code ConfigSyncPayload}), so gauges, slot counts and
 * depth draw exactly what the server enforces; the overlay clears on disconnect.
 * Changes apply on the next game/server start (recipe gating also re-applies on
 * {@code /reload}).
 *
 * <p><b>Pause, never punish:</b> nothing here can void what a player already stored.
 * Shrinking a capacity strands nothing - stores stop accepting and pay out normally;
 * over-deep slots simply draw down. Disabling a trinket pulls its recipe and shelf
 * entry and puts an already-fitted one quietly to sleep in its socket - re-enable it
 * and it wakes with everything intact.
 */
public final class PackworkConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("packwork/config");
    public static final String FILE_NAME = "packwork-server.toml";
    public static final String CLIENT_FILE_NAME = "packwork-client.toml";

    /** What happens to carried (and worn) packs when their owner dies. */
    public enum DeathHandling {
        /** Vanilla: packs drop with the rest of the inventory. The shipped default. */
        DROP,
        /** Packs stay with the player through death and are handed back on respawn. */
        KEEP,
        /** The pack sets itself down as a placed block where the player fell (contents
         *  intact); if no honest spot exists - a void death, solid rock - it falls back
         *  to KEEP rather than ever risking the contents. */
        PLACE
    }

    /** One immutable snapshot of every tunable. Arrays index by enum ordinal. */
    public record Values(
            int[] slots, int[] stacksPerSlot,
            int[] fluidMb, int[] xpPoints, int[] energyFe, long[] vaporMb,
            boolean[] trinketEnabled,
            DeathHandling deathHandling,
            double magnetRange, int magnetEveryTicks, boolean packFirstDefault,
            Set<Identifier> neverAutoEat,
            int valveDefaultKeepStacks, int pressKeepLoose, boolean pressIncludes2x2) {

        public int slotsFor(PackTier t) { return slots[t.ordinal()]; }
        public int stacksPerSlotFor(PackTier t) { return stacksPerSlot[t.ordinal()]; }
        public int fluidMbFor(PackTier t) { return fluidMb[t.ordinal()]; }
        public int xpPointsFor(PackTier t) { return xpPoints[t.ordinal()]; }
        public int energyFeFor(PackTier t) { return energyFe[t.ordinal()]; }
        public long vaporMbFor(PackTier t) { return vaporMb[t.ordinal()]; }
        public boolean enabled(TrinketType t) { return trinketEnabled[t.ordinal()]; }
    }

    /** Convenience readers for the two 1.2.0 fittings, so call sites stay short. */
    public static int valveDefaultKeepStacks() { return get().valveDefaultKeepStacks(); }

    /**
     * The shipped defaults - byte-identical to pre-config behaviour, and the single
     * place the "how much does each tier hold" formulas live now. {@link PackTier}'s
     * getters read the active values, which default to these.
     */
    public static Values defaults() {
        int n = PackTier.values().length;
        int[] slots = new int[n];
        int[] depth = new int[n];
        int[] fluid = new int[n];
        int[] xp = new int[n];
        int[] fe = new int[n];
        long[] vapor = new long[n];
        for (PackTier t : PackTier.values()) {
            int i = t.ordinal();
            int step = i + 1;
            slots[i] = t.baseCapacity();
            depth[i] = step;               // Canvas x1 .. Sculkhide x6
            fluid[i] = 8_000 * step;       // Canvas 8 buckets .. Sculkhide 48
            xp[i] = 5_000 * step;          // Canvas 5k points .. Sculkhide 30k
            fe[i] = 100_000 * step;        // Canvas 100k FE .. Sculkhide 600k
            vapor[i] = 16_000L * step;     // Canvas 16k mB .. Sculkhide 96k
        }
        boolean[] trinkets = new boolean[TrinketType.values().length];
        java.util.Arrays.fill(trinkets, true);
        return new Values(slots, depth, fluid, xp, fe, vapor, trinkets,
                DeathHandling.DROP, 5.0, 4, true, Set.of(), 4, 64, true);
    }

    private static volatile Values local = defaults();
    /** A remote server's values, overlaid while connected; null when we are the authority. */
    private static volatile Values remote = null;

    /** Client-only cosmetic: whether the worn pack renders on your back. Never synced. */
    private static volatile boolean showWornPack = true;

    /** The values in force right now - the remote server's while connected, ours otherwise. */
    public static Values get() {
        Values r = remote;
        return r != null ? r : local;
    }

    public static Values localValues() { return local; }

    public static void setRemote(Values values) { remote = values; }

    public static boolean showWornPack() { return showWornPack; }

    public static void setShowWornPack(boolean v) { showWornPack = v; }

    /** Test seam: swap the local values in and get the previous ones back to restore. */
    public static Values setLocalForTesting(Values values) {
        Values old = local;
        local = values;
        return old;
    }

    /** A fresh pack's layout - {@code PackLayout.EMPTY} with the configured pack-first default. */
    public static PackLayout defaultLayout() {
        return get().packFirstDefault() ? PackLayout.EMPTY : PackLayout.EMPTY.withPackFirst(false);
    }

    // ------------------------------------------------------------------
    // loading
    // ------------------------------------------------------------------

    /** Read (or first write) {@code config/packwork-server.toml}. Call once at mod construction. */
    public static void loadServer(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(configDir);
                Files.write(file, defaultFileText().getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Packwork wrote its default server config to {}", file);
                local = defaults();
                return;
            }
            List<String> problems = new ArrayList<>();
            Map<String, Object> map = SimpleToml.parse(Files.readAllLines(file, StandardCharsets.UTF_8), problems);
            local = fromMap(map, problems);
            for (String p : problems) LOGGER.warn("Packwork config ({}): {}", FILE_NAME, p);
            LOGGER.info("Packwork server config read from {} ({} keys{})", file, map.size(),
                    problems.isEmpty() ? "" : ", " + problems.size() + " fell back to defaults");
        } catch (IOException e) {
            LOGGER.warn("Packwork could not read {} - carrying on with the shipped defaults", file, e);
            local = defaults();
        }
    }

    /** Read (or first write) the little client cosmetics file. Client dist only. */
    public static void loadClient(Path configDir) {
        Path file = configDir.resolve(CLIENT_FILE_NAME);
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(configDir);
                Files.write(file, clientFileText().getBytes(StandardCharsets.UTF_8));
                return;
            }
            List<String> problems = new ArrayList<>();
            Map<String, Object> map = SimpleToml.parse(Files.readAllLines(file, StandardCharsets.UTF_8), problems);
            showWornPack = SimpleToml.readBool(map, "show_worn_pack", true, problems);
            for (String p : problems) LOGGER.warn("Packwork config ({}): {}", CLIENT_FILE_NAME, p);
        } catch (IOException e) {
            LOGGER.warn("Packwork could not read {} - keeping client defaults", file, e);
        }
    }

    /** Build a Values from parsed keys, clamped, with every miss falling back to the default. */
    public static Values fromMap(Map<String, Object> map, List<String> problems) {
        Values d = defaults();
        int n = PackTier.values().length;
        int[] slots = new int[n];
        int[] depth = new int[n];
        int[] fluid = new int[n];
        int[] xp = new int[n];
        int[] fe = new int[n];
        long[] vapor = new long[n];
        for (PackTier t : PackTier.values()) {
            int i = t.ordinal();
            String k = "tiers." + t.getSerializedName() + ".";
            slots[i] = SimpleToml.readInt(map, k + "slots", d.slots()[i], 1, 256, problems);
            depth[i] = SimpleToml.readInt(map, k + "stacks_per_slot", d.stacksPerSlot()[i], 1, 99, problems);
            fluid[i] = SimpleToml.readInt(map, k + "fluid_mb", d.fluidMb()[i], 1, 1_000_000, problems);
            xp[i] = SimpleToml.readInt(map, k + "xp_points", d.xpPoints()[i], 1, 10_000_000, problems);
            fe[i] = SimpleToml.readInt(map, k + "energy_fe", d.energyFe()[i], 1, 100_000_000, problems);
            vapor[i] = SimpleToml.readLong(map, k + "vapor_mb", d.vaporMb()[i], 1, 100_000_000L, problems);
        }
        boolean[] trinkets = new boolean[TrinketType.values().length];
        for (TrinketType t : TrinketType.values()) {
            trinkets[t.ordinal()] = SimpleToml.readBool(map, "trinkets." + t.id(), true, problems);
        }
        String rawDeath = SimpleToml.readString(map, "death.handling", "drop", problems)
                .trim().toLowerCase(Locale.ROOT);
        DeathHandling death = switch (rawDeath) {
            case "drop" -> DeathHandling.DROP;
            case "keep" -> DeathHandling.KEEP;
            case "place" -> DeathHandling.PLACE;
            default -> {
                problems.add("'death.handling' = \"" + rawDeath + "\" is not drop/keep/place; keeping \"drop\"");
                yield DeathHandling.DROP;
            }
        };
        double magnetRange = SimpleToml.readDouble(map, "lodestone.magnet_range", 5.0, 0.0, 16.0, problems);
        int magnetTicks = SimpleToml.readInt(map, "lodestone.magnet_every_ticks", 4, 1, 200, problems);
        boolean packFirst = SimpleToml.readBool(map, "lodestone.pack_first_default", true, problems);
        Set<Identifier> noEat = new HashSet<>();
        for (String s : SimpleToml.readStringList(map, "provisioner.never_auto_eat", List.of(), problems)) {
            Identifier id = Identifier.tryParse(s);
            if (id != null) noEat.add(id);
            else problems.add("'provisioner.never_auto_eat' entry \"" + s + "\" is not an item id; skipped");
        }
        int valveKeep = SimpleToml.readInt(map, "overflow_valve.default_keep_stacks", 4, 1,
                com.sappersquad.packwork.sort.PackLayout.Spill.MAX_KEEP, problems);
        int pressKeep = SimpleToml.readInt(map, "compacting_press.keep_loose", 64, 0, 4096, problems);
        boolean press2x2 = SimpleToml.readBool(map, "compacting_press.include_2x2", true, problems);
        return new Values(slots, depth, fluid, xp, fe, vapor, trinkets,
                death, magnetRange, magnetTicks, packFirst, Set.copyOf(noEat),
                valveKeep, pressKeep, press2x2);
    }

    // ------------------------------------------------------------------
    // the generated files (the documentation lives here, as comments)
    // ------------------------------------------------------------------

    /** The default server file, comments and all. Every value in it IS the shipped behaviour. */
    public static String defaultFileText() {
        Values d = defaults();
        StringBuilder sb = new StringBuilder();
        sb.append("""
                # Packwork - server tuning. Every value below is the shipped default; delete a line
                # (or the whole file) and that default quietly returns. The server's copy of this
                # file is the authority - clients receive its values on login - so in a modpack,
                # ship your edited file at config/packwork-server.toml and every install agrees.
                # Changes apply on the next game/server start.
                #
                # Nothing in this file can void what players already stored: shrinking a capacity
                # strands nothing (stores just stop accepting and pay out normally, over-deep
                # slots draw down), and a disabled trinket goes to sleep in its socket instead of
                # breaking - pause, never punish.

                [death]
                # What happens to carried and worn packs when a player dies:
                #   "drop"  - vanilla: they drop with the rest of the inventory
                #   "keep"  - packs stay with the player and come back on respawn
                #   "place" - the pack sets itself down as a block where the player fell,
                #             contents intact (a void death or solid rock falls back to "keep")
                # keepInventory ON already keeps everything; this setting then changes nothing.
                handling = "drop"

                [lodestone]
                # How far (in blocks) the Lodestone Charm's magnet reaches. 0.0 turns the pull off
                # (pack-first pickup on touch still works). Range 0.0..16.0.
                magnet_range = 5.0
                # Server ticks between magnet pulls: 4 = five times a second, 20 = once a second.
                # Higher is cheaper and lazier. Range 1..200.
                magnet_every_ticks = 4
                # Whether a fresh pack starts with pack-first pickup ON (the Lodestone filing what
                # you mine straight into the pack). Players can still flip it per pack in the GUI.
                pack_first_default = true

                [provisioner]
                # Extra items the Provisioner's Pouch must never auto-eat, on top of the
                # packwork:never_auto_eat item tag. Item ids, e.g. ["minecraft:golden_carrot"].
                never_auto_eat = []

                [overflow_valve]
                # The keep level a marked item starts at when you give it one, in vanilla
                # stacks. The player sets it per item in the pack GUI (Shift+O over the item
                # cycles 1, 2, 4, 8, 16 stacks and back round to "bin it outright"); this is
                # only the number the first press lands on. Range 1..64.
                # The Valve NEVER touches an item the player has not marked, and never takes
                # the count below the keep level.
                default_keep_stacks = 4

                [compacting_press]
                # How many of an item the press leaves loose before it starts squeezing, so
                # you always have some to hand. 0 = squeeze everything it can. Range 0..4096.
                keep_loose = 64
                # Whether the press also does 2x2 families (nuggets -> ingots, and the like).
                # false leaves it to 3x3 only.
                include_2x2 = true

                [trinkets]
                # Set any fitting to false to retire it: its recipe is pulled (JEI and the
                # creative shelf follow), and one already fitted goes quietly inert - nothing it
                # stored is lost, and it wakes up again if you turn it back on.
                """);
        for (TrinketType t : TrinketType.values()) {
            sb.append(t.id()).append(" = true\n");
        }
        sb.append("""

                # Per-tier sizing. slots = backing compartment slots (1..256);
                # stacks_per_slot = how many vanilla stacks one slot holds (1..99, unstackables
                # never stack); then the four store capacities, unlocked by their fittings:
                # fluid_mb (Waterskin Rack), xp_points (Soul Vial), energy_fe (Charge Crystal;
                # its transfer rate scales with it), vapor_mb (Alchemist's Flask Harness).
                """);
        for (PackTier t : PackTier.values()) {
            int i = t.ordinal();
            sb.append("[tiers.").append(t.getSerializedName()).append("]\n");
            sb.append("slots = ").append(d.slots()[i]).append('\n');
            sb.append("stacks_per_slot = ").append(d.stacksPerSlot()[i]).append('\n');
            sb.append("fluid_mb = ").append(d.fluidMb()[i]).append('\n');
            sb.append("xp_points = ").append(d.xpPoints()[i]).append('\n');
            sb.append("energy_fe = ").append(d.energyFe()[i]).append('\n');
            sb.append("vapor_mb = ").append(d.vaporMb()[i]).append('\n');
            if (i < PackTier.values().length - 1) sb.append('\n');
        }
        return sb.toString();
    }

    public static String clientFileText() {
        return """
                # Packwork - client cosmetics. Yours alone; never synced.

                # Draw the pack on your character's back while it is worn in the back slot.
                # (It hides itself automatically under an elytra.)
                show_worn_pack = true
                """;
    }

    private PackworkConfig() {}
}
