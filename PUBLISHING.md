# Packwork — publishing kit (current as of v0.1.0 "First Haul")

Everything needed to update the Modrinth / CurseForge **profile page** for a release.
**The rule:** `CHANGELOG.md`, `PUBLISHING.md`, and `README.md` get bumped in the same
pass — never one alone. The store page is the only thing most players read.

> **Not yet published.** Uploading to Modrinth / CurseForge / GitHub is SapperSquad's call, per
> release. This file is the ready-to-paste copy; nothing here goes external automatically.

Art will live in `assets/` (icon, banner, gallery cards) — not drawn yet. Real in-game
shots already exist from the dev harness (`run/client/screenshots/packwork_*.png`); use
those as the gallery until promo cards are made.

**Files to upload — ADD as new versions; do not delete older ones.**

| Upload as version | File | Game version tag | Loader |
|---|---|---|---|
| `0.1.0+mc1.21.1` | `build/libs/packwork-0.1.0.jar` | 1.21.1 | neoforge |

**Migration notes for this release:** none — first release.

---

## Summary (the short-description field)

> An adventurer's pack that sorts itself: tabbed compartments, a rules engine, and
> trinkets for magnet, repair, and even a fluid tank. Leather and brass, never tech.

---

## Project description (paste into the body)

# The pack that packs itself

You know the drill: twenty stacks of loot, one backpack, and five minutes of dragging
things into rows. Packwork does that part for you. Drop an item in and it *goes
somewhere* — stamped leather tabs down the side are compartments, and a rules engine
claims every item for the right one the instant it lands. A Loose tab catches anything
no rule wanted, so nothing ever disappears.

## Sorting is the soul

- **Auto-tabs out of the box** — Food, Combat, Tools, Ores & Valuables, Brewing, Nature,
  Blocks, and Loose. They sort by what an item *is*, so a modded pickaxe is a Tool for
  free — no config, no per-mod support. Two more (Charts & Bearings, The Catch) open up
  when you fit the trinket that earns them.
- **Make your own** — name a compartment, stamp it with any item icon, dye it, drag to
  reorder. **Pin** an item by dropping it where you want it — it stays put, wears a red
  ribbon, and beats every rule.
- **Tidy Up, search, flatten** — merge and re-sort with one button, find anything across
  every tab, or collapse it all into one grid when you just want to rummage.

## One pack, many fittings

Craft it up the ladder — Canvas, Leather, Studded, Reinforced, Runed, **Dragonhide** —
each tier crafted *from the pack before it*, carrying everything inside straight up. And
every step cuts the slots **deeper**: from one stack a slot on Canvas to **six stacks a
slot** on Dragonhide, with the world outside only ever seeing legal stacks. Then slot
brass **trinkets** into it:

| Fitting | |
|---|---|
| Lodestone Charm | pulls loose items nearby into the pack |
| Restock Strap | refills your hotbar from pack stock |
| Repair Kit | slowly mends worn and held gear |
| Bottomless Lining | more slots, never voided |
| Compass Rose | the only way a pack throws anything out — and it's opt-in |
| Quick-Draw Straps | when a tool breaks in your hand, the pack hands you another |
| Quill & Ledger | custom compartments start filing by rule, not just pins |
| Tinker's Kit | a leather tool roll unrolls inside the pack — a 3×3 bench fed from your stores, with a parchment Recipe Ledger of everything craftable from pack stock |
| Field Furnace | banked embers cook raw ore and raw food as you walk |
| Provisioner's Pouch | feeds you before hunger bites, cheapest rations first |
| Cartographer's Sleeve | opens a Charts & Bearings compartment for maps and compasses |
| Angler's Creel | opens The Catch, and your catch goes straight in the pack |
| Torchbearer's Loop | sets a torch down when you're standing in the dark |
| Herbalist's Bundle | replants a grown crop from your own seed stock |
| Waterskin Rack | a real fluid tank, shown as a glass gauge |
| Soul Vial | stores your XP and auto-mends your gear from it |
| Charge Crystal | an arcane charge in copper-wound glass, topping up your powered tools |
| Alchemist's Flask Harness | a chemical tank for bottled vapors (with Mekanism) |

New to the pack? Craft the **Outfitter's Handbook** and right-click it — five chapters walk
you through sorting, trinkets, tiers, and the stores, in-game.

## Set it down, pipe it up

Sneak-right-click to stand a pack in the world — leather and brass, wearing its tier's own
trim (twine, studs, steel plates, or glowing runes), facing you. Break it and you get the
pack back with everything still inside. A placed pack
speaks NeoForge's own item, fluid, and energy capabilities, so hoppers, pipes, and cables
feed it with no bridge block — and dropped-in items auto-file into the right compartment,
because the sorting is virtual over one store.

## Needs nothing, plays with everything

Zero hard dependencies — every cross-mod touch lights up only when its mod is present:
**Mekanism** turns the Flask Harness into a real chemical tank its pipes can fill;
**Curios** lets you wear the pack in the back slot (trinkets keep working worn);
**Forgework** Flux cables charge a placed pack's Charge Crystal, 1 Flux = 1 FE, and the
crystal tops up carried Forgework terminals; **JEI** renders every craft as a real recipe —
tier upgrades included — plus info pages for every pack, trinket, and the handbook; and
**Pantrywork** food tags fold into the Food tab. Remove any of them and Packwork just
carries on without it.

---

## Gallery upload plan

| File | Caption |
|---|---|
| `banner.png` (featured, TODO) | "The pack that packs itself." |
| `packwork_tabs.png` (real shot) | The tab rail, mid-sort, with a fluid gauge on the side. |
| `packwork_combat.png` (real shot) | One compartment: weapons and armor, claimed automatically. |
| `packwork_search.png` (real shot) | Search "iron" — flattened to just what matches. |
| `packwork_handbook.png` (real shot) | The Outfitter's Handbook, open to "The Pack". |
| `packwork_placed_world.png` (real shot) | All five per-tier packs set down in the world, each wearing its trim. |
| `card_trinkets.png` (TODO) | The working fittings. |

**Art carrying version-specific claims** (trinket counts, resource-store list, MC
version): keep it in step with the changelog. All **four** resource stores now ship (fluids,
XP, energy always; **gas** with Mekanism). Placeable packs, the Forgework Flux bridge (item-
AND block-level), the Outfitter's Handbook, **Curios** back-slot wear, **Mekanism** gas, and
**JEI** lookup are all in — but note the mod-gated ones (gas, Curios, JEI, Forgework) light up
only with their mod, so don't imply they work standalone.

---

## Store voice

Playful, forge-y, second person. Concrete over adjective, player consequence over
implementation — "the Loose tab catches anything no rule claims, so nothing vanishes"
beats "a fallback bucket handles unmatched predicates." Never call the trinkets modules
or the energy store a battery; hold the adventurer line.

See `CHANGELOG.md` for the per-version description block.
