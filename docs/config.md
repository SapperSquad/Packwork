# Configuration

[← back to the manual](README.md)

Two files, written with their own documentation as comments the first time the game runs.

| File | Where | Who owns it |
|---|---|---|
| `config/packwork-server.toml` | Beside every world (single-player) or in the server folder | **The server.** Its values are sent to every client on login. |
| `config/packwork-client.toml` | The player's own game folder | **The player.** Never synced, never seen by the server. |

Both files use the same keys and the same parser on **NeoForge and Fabric** — a modpack
ships one config for both loaders.

**Everything below is the shipped default.** Delete a line, or the whole file, and that
default quietly returns. Changes apply on the next game or server start; the recipe gating
under `[trinkets]` also re-applies on `/reload`.

**Nothing in this file can void what a player already stored.** Shrink a capacity and stores
just stop accepting and pay out normally; over-deep slots draw down. Disable a fitting and
an already-installed one goes quietly to sleep in its socket — nothing it held is lost, and
it wakes up intact when you turn it back on.

---

## `packwork-server.toml`

### `[death]`

| Key | Default | Values | What it does |
|---|---|---|---|
| `handling` | `"drop"` | `"drop"`, `"keep"`, `"place"` | What happens to carried and worn packs when a player dies. |

- **`"drop"`** — vanilla: packs drop with the rest of the inventory.
- **`"keep"`** — packs stay with the player and are handed back on respawn.
- **`"place"`** — the pack sets itself down as a block where the player fell, contents
  intact. If there is no honest spot to set it — a void death, solid rock — it falls back to
  `"keep"` rather than ever risking the contents.

With the `keepInventory` gamerule on, everything is kept already and this setting changes
nothing.

### `[lodestone]`

| Key | Default | Range | What it does |
|---|---|---|---|
| `magnet_range` | `5.0` | `0.0`–`16.0` | How far, in blocks, the Lodestone Charm's magnet reaches. `0.0` turns the pull off entirely; pack-first pickup on touch still works. |
| `magnet_every_ticks` | `4` | `1`–`200` | Server ticks between magnet pulls. `4` is five times a second, `20` is once a second. Higher is cheaper and lazier. |
| `pack_first_default` | `true` | `true` / `false` | Whether a fresh pack starts with pack-first pickup on. Players can still flip it per pack in the GUI. |

### `[provisioner]`

| Key | Default | What it does |
|---|---|---|
| `never_auto_eat` | `[]` | Extra item ids the Provisioner's Pouch must never auto-eat, **on top of** the `packwork:never_auto_eat` item tag. Example: `["minecraft:golden_carrot"]`. |

For a modpack, prefer the tag — it merges with other packs and doesn't need a config edit.
See [For packmakers](for-packmakers.md).

### `[overflow_valve]`

| Key | Default | Range | What it does |
|---|---|---|---|
| `default_keep_stacks` | `4` | 1–64 | The keep level a marked item lands on the first time a player gives it one, in vanilla stacks. They dial it per item in the pack GUI with **Shift+O** (1, 2, 4, 8, 16, then back round to "bin it outright"); this is only the starting number. |

The Valve never touches an item the player has not marked, and never takes the count below
the keep level. See [the one discard list](tiers-and-fittings.md) for how it and the Compass
Rose share one list.

### `[compacting_press]`

| Key | Default | Range | What it does |
|---|---|---|---|
| `keep_loose` | `64` | 0–4096 | How many of an item the press leaves uncompacted, so there's always some to hand. `0` squeezes everything it can. |
| `include_2x2` | `true` | — | Whether the press also does 2×2 families (nuggets → ingots and the like). `false` leaves it to 3×3 only. |

The press only ever squeezes a family whose result **uncrafts back into exactly what went
in** — that check is vanilla recipe data, so every modded ingot ladder that plays by the
normal rules works with no config, and anything one-way is refused with no blocklist to
maintain.

### `[trinkets]`

One `true`/`false` line per fitting. Setting one to `false` pulls its recipe (JEI and the
creative shelf follow), and an already-fitted one goes quietly inert.

```toml
[trinkets]
lodestone_charm = true
compass_rose = true
restock_strap = true
bottomless_lining = true
repair_kit = true
quick_draw_straps = true
quill_and_ledger = true
tinkers_kit = true
field_furnace = true
provisioners_pouch = true
cartographers_sleeve = true
anglers_creel = true
torchbearers_loop = true
herbalists_bundle = true
overflow_valve = true
compacting_press = true
waterskin_rack = true
soul_vial = true
charge_crystal = true
flask_harness = true
```

Two fittings also own a compartment — turn off `cartographers_sleeve` and the **Charts &
Bearings** tab never appears; turn off `anglers_creel` and **The Catch** doesn't either.
Their contents don't vanish, they just re-sort into the compartments that remain.

### `[tiers.<name>]`

One block per tier: `canvas`, `leather`, `studded`, `reinforced`, `runed`, `sculkhide`.

| Key | Range | What it does |
|---|---|---|
| `slots` | `1`–`256` | Backing compartment slots. 256 is a hard ceiling — the data component the contents live in cannot hold more. |
| `stacks_per_slot` | `1`–`99` | How many vanilla stacks one slot holds. Unstackable items never stack regardless. |
| `fluid_mb` | `1`–`1,000,000` | Waterskin Rack capacity, in millibuckets. |
| `xp_points` | `1`–`10,000,000` | Soul Vial capacity, in experience points. |
| `energy_fe` | `1`–`100,000,000` | Charge Crystal capacity, in FE. Its transfer rate scales with this. |
| `vapor_mb` | `1`–`100,000,000` | Alchemist's Flask Harness capacity, in millibuckets. |

Shipped defaults:

| Tier | `slots` | `stacks_per_slot` | `fluid_mb` | `xp_points` | `energy_fe` | `vapor_mb` |
|---|---|---|---|---|---|---|
| `canvas` | 54 | 1 | 8000 | 5000 | 100000 | 16000 |
| `leather` | 108 | 2 | 16000 | 10000 | 200000 | 32000 |
| `studded` | 162 | 3 | 24000 | 15000 | 300000 | 48000 |
| `reinforced` | 216 | 4 | 32000 | 20000 | 400000 | 64000 |
| `runed` | 256 | 5 | 40000 | 25000 | 500000 | 80000 |
| `sculkhide` | 256 | 6 | 48000 | 30000 | 600000 | 96000 |

A value outside its range is clamped, and a line that doesn't parse is skipped with a
warning in the log naming the key — the rest of the file still loads, and the bad key keeps
its default.

---

## `packwork-client.toml`

Yours alone. Never synced, never sent anywhere.

| Key | Default | What it does |
|---|---|---|
| `show_worn_pack` | `true` | Draw the pack on your character's back while it's worn in a back slot. It hides itself under an elytra either way. |

---

## Authority and syncing

The **server's** `packwork-server.toml` is the truth. On login the server sends its values
down, so gauges, slot counts and depth on the client draw exactly what the server enforces.
The overlay clears when you disconnect and your own single-player file takes over again.

That means: in a modpack, ship your edited file at `config/packwork-server.toml` and every
install agrees, whether or not each player's own copy was edited.
