# For packmakers

[← back to the manual](README.md)

Everything here is either a config line or a datapack file. You never need to touch code,
and you never need to ask.

## Modpacks: yes

Include Packwork in any modpack, on any launcher, no permission needed. Attribution is
appreciated but not required.

## The short answers

| You want to | Do this |
|---|---|
| Retire a fitting | `[trinkets] <id> = false` in `packwork-server.toml` |
| Change how much a tier holds | `[tiers.<name>]` in `packwork-server.toml` |
| Change what counts as an ore / food / tool | Add to the `packwork:sorting/*` item tags |
| Add your own compartment category | Add to a `packwork:sorting/*` tag, or ship a custom tab your players make |
| Stop the pouch eating something | Add it to the `packwork:never_auto_eat` item tag |
| Change what the Field Furnace burns | Add to the `packwork:furnace_fuel` item tag |
| Make packs survive death | `[death] handling = "keep"` |
| Make the pack quieter on a big server | Raise `[lodestone] magnet_every_ticks` |

Full key reference: [Configuration](config.md).

## The tags the sorter reads

Every shipped compartment matches on two things: a **predicate** (what an item *is*, which
covers modded items for free) and one or more **item tags**. The tags are the part you can
retune, and they all live under `data/packwork/tags/item/sorting/`.

| Compartment | Predicate it uses | Tags it reads, in order |
|---|---|---|
| The Catch | — | `#packwork:sorting/catch`, `#minecraft:fishes` |
| Food | is food | `#packwork:sorting/food` |
| Combat | is weapon, is armor | `#minecraft:arrows`, `#packwork:sorting/combat` |
| Charts & Bearings | — | `#packwork:sorting/charts` |
| Tools & Utility | is tool | `#minecraft:shears`, `#packwork:sorting/tools` |
| Ores & Valuables | — | `#c:ores`, `#c:ingots`, `#c:raw_materials`, `#c:gems`, `#c:nuggets`, `#c:storage_blocks`, `#packwork:sorting/ores` |
| Brewing & Alchemy | is potion | `#c:crops/nether_wart`, name contains "potion", `#packwork:sorting/brewing` |
| Nature & Farming | — | `#minecraft:saplings`, `#minecraft:flowers`, `#minecraft:leaves`, `#c:seeds`, `#c:crops`, `#packwork:sorting/nature` |
| Blocks & Building | is block | `#packwork:sorting/blocks` |

The `packwork:sorting/*` tags are **ours and yours** — Packwork seeds each of them with a few
entries and expects packs to fill them in. The community `c:` tags are read directly, so if
your pack already tags its ores properly, Packwork already sorts them.

### Adding items to a category

Drop a file into your datapack at
`data/packwork/tags/item/sorting/<category>.json`:

```json
{
  "replace": false,
  "values": [
    { "id": "yourmod:mysterious_ore", "required": false },
    { "id": "#yourmod:gemstones", "required": false }
  ]
}
```

Two habits worth keeping:

- **`"replace": false`** so your file merges with Packwork's and with every other pack's,
  instead of blanking them.
- **`"required": false`** on every entry, so an item from a mod the player didn't install is
  skipped instead of breaking tag loading for everyone.

The categories, verbatim, are: `catch`, `food`, `combat`, `charts`, `tools`, `ores`,
`brewing`, `nature`, `blocks`.

### Which compartment wins

Compartments are checked **top to bottom** and the first one that wants an item claims it.
The shipped order is the table above, and it is deliberate: The Catch above Food (or every
cod files as rations), Combat and Charts above Tools (the `c:tools` tag includes swords and
compasses), and Blocks last of the real compartments because it's the broad catch.

If you add a tool to `sorting/ores`, it will land in Ores — Ores is checked first. Put an
item in the category you actually want it in rather than the one that describes it best.

**Loose sits last and takes everything nobody claimed.** There is no way to configure an
item into oblivion; the worst a bad tag can do is file something oddly.

### Two more tags, outside sorting

- **`packwork:never_auto_eat`** — the Provisioner's Pouch will not eat these. Ships with
  chorus fruit, golden apples, golden carrots, suspicious stew, honey, cake, and the things
  that hurt you. Add your pack's exotic foods here.
- **`packwork:furnace_fuel`** — what the Field Furnace will burn from pack stock. Ships with
  coals, blaze rods, lava buckets, dried kelp blocks, `#minecraft:coals` and `#c:coal_blocks`.

## Custom compartments are the player's, not the pack's

Player-made compartments (name, stamp, dye, rail position, written rules) live **on the pack
item**, not in your datapack — they travel with the pack through upgrades and across worlds.
There is deliberately no way for a datapack to force a compartment onto someone's pack: the
rail is the player's desk.

What you *can* do is make the shipped compartments cover your pack's items properly, which
is what the tags above are for. In practice that is enough — a player who wants "my base
build kit" makes a tab and stamps it in about four seconds.

## Turning things off

Every fitting can be retired with one line, and retiring one is safe at any point in a
pack's life: the recipe is pulled, JEI and the creative tab follow, and an already-installed
fitting goes inert in its socket without losing what it held. Turn it back on later and it
wakes up intact.

A few that packs commonly reconsider:

- **`compass_rose = false`** — the only fitting that destroys items. Turn it off if your pack
  wants a strictly no-void ruleset.
- **`tinkers_kit = false`** — removes crafting on the go, if your pack's progression is built
  around getting back to a bench.
- **`field_furnace = false`** — removes smelting on the go, same reasoning.
- **`lodestone_charm = false`** — removes both the magnet and pack-first pickup, if you want
  looting to stay manual.

## Server performance

Two knobs matter, both under `[lodestone]`:

- **`magnet_every_ticks`** (default `4`) is the big one on a busy server. Raising it to `10`
  or `20` cuts the magnet's work by 2.5×–5× and players barely notice.
- **`magnet_range`** (default `5.0`) shrinks the search volume. `0.0` turns the pull off
  completely while leaving pack-first pickup working on touch.

Everything else is event-driven — sorting runs when an item lands, not on a tick.

## What Packwork will never do to your pack

- Void an item it wasn't explicitly told to void.
- Require another mod. Every bridge is gated and simply absent otherwise.
- Require power to *use* a pack. The Charge Crystal stores energy; it never demands it.
- Add a `@Mod`-level hard dependency you have to resolve.
