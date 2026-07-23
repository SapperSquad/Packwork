# Packwork changelog

All notable changes, newest first. Dates are the suite's working dates.

## 0.1.0 "First Haul" — 2026-07-23

The first shippable build: the self-sorting pack, the material ladder, the trinket
framework, three resource stores, a gated Forgework bridge, and an in-game guide.

### Added
- **The self-sorting pack.** A tabbed organizer with a stamped-leather tab rail: seven
  auto-tabs (Food, Combat, Tools, Ores, Brewing, Nature, Blocks) plus a Loose catch-all
  that guarantees nothing dropped in ever vanishes.
- **Rules engine** — auto-tabs match by class/predicate (modded items sort for free) and
  by item tag; the auto-tab tag lists ship as datapack JSON so packs can retune without
  code.
- **Custom compartments** — create, name, stamp with any item icon, dye, and reorder;
  **manual pins** (press P) that always win; **Tidy Up**, **search**, and **flatten**.
- **Five material tiers** (Canvas → Runed), craftable, with a preserving upgrade recipe
  that carries a full pack's contents, layout, and trinkets up a tier.
- **Trinket framework** — brass sockets on the right rail (count scales with tier) and
  craftable fittings. Working: Lodestone Charm (magnet), Restock Strap, Repair Kit,
  Bottomless Lining (extra slots, never voided), Compass Rose (opt-in void — the only void
  path), **Quick-Draw Straps** (a broken held tool is replaced from pack stock, never
  duped), and **Quill & Ledger** (custom compartments file by rule, not just pins — each
  gathers items that share the kind of the item it's stamped with).
- **Fluids store** — the Waterskin Rack fits a tier-scaled fluid tank shown as a glass
  gauge; fill or drain it with a bucket or flask. Exposes NeoForge's fluid capability only
  when the Rack is fitted.
- **XP store** — the Soul Vial stores experience: click the green gauge to siphon your XP
  in, Shift-click to pour it back, and it auto-mends your Mending-enchanted gear from the
  reservoir.
- **Energy store** — the Charge Crystal holds an arcane charge (standard FE) in a
  copper-wound crystal; any mod's charger fills a placed pack, and it tops up the powered
  tools in your hands. The amber gauge shows the charge.
- **Forgework bridge (optional, gated).** With Forgework installed, the Charge Crystal
  also feeds any Forgework portable terminal you carry, 1 Flux = 1 FE. Does nothing when
  Forgework is absent — one gated class, zero hard dependency.
- **Outfitter's Handbook** — an in-house guide item (craft a book + leather, right-click to
  open). Five leather-and-brass chapters: the pack, sorting, trinkets, tiers, and the
  stores, with numbers pulled straight from the code.
- **Standard capabilities** — an item handler always, a fluid handler when a Rack is
  fitted, an energy handler when a Charge Crystal is fitted, so any mod's automation works
  against a placed pack. Zero hard dependencies.
- **Open Pack keybind** (default B) plus native right-click use. Pantrywork food tags fold
  into the Food tab when present.

### Known / on the bench
- **Resource stores:** fluids, XP, and energy ship; gas (an Alchemist's Flask Harness for
  Mekanism chemicals) is the last one, deferred until the Mekanism dependency is added.
- **No Curios wear-slot** compat yet (native use + keybind cover carrying it), and **no
  JEI** integration yet. Both are written to light up when their mod is present.
- **The Forgework bridge is item-level** (the pack has no placed block-entity form yet), so
  it charges terminals you carry rather than a Forgework cable charging a placed pack.

### Cut
- **Feather Charm** — Packwork has no pack-weight / encumbrance penalty, so a "weightless"
  fitting had no job. Removed rather than shipped as a dead craftable.

### Notes for returning players
Nothing to migrate — this is the first release.
