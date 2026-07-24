# Packwork changelog

All notable changes, newest first. Dates are the suite's working dates.

## 0.1.0 "First Haul" — 2026-07-23

The first shippable build: the self-sorting pack, the material ladder, the trinket
framework, three resource stores, a placeable + automatable pack, a gated Forgework bridge,
and an in-game guide.

### Added
- **Placeable packs.** Sneak-right-click a block face to stand a pack in the world - it
  renders as the pack, tinted for its tier and facing you; break it (or middle-click) to get
  the item back with every field intact (contents, layout, trinkets, fluid/XP/energy). Right-
  click a placed pack for the same organizer. It speaks standard NeoForge block capabilities -
  an item handler (dropped-in items auto-file), a fluid tank with a Waterskin Rack, an energy
  store with a Charge Crystal - so hoppers, pipes, and cables interact with no bridge block.
  With Forgework installed, a Forgework cable charges a placed pack's Charge Crystal directly,
  1 Flux = 1 FE.
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
- **Gas store — the Alchemist's Flask Harness (needs Mekanism).** A chemical tank for
  bottled vapors, filled by any Mekanism chemical pipe, shown as a violet gauge on the rail.
  It's only craftable and only appears when Mekanism is installed.
- **Optional integrations, all soft deps** — each lights up when its mod is present and is
  simply absent otherwise (zero hard dependencies): the **Forgework** Flux bridge (the Charge
  Crystal feeds carried terminals, and a *placed* pack charges off Forgework cables, 1 Flux =
  1 FE); **Curios** back-slot wear (the pack's trinkets keep working while worn); and **JEI**
  info pages for every pack tier, trinket, and the handbook.
- **Outfitter's Handbook** — an in-house guide item (craft a book + leather, right-click to
  open). Five leather-and-brass chapters: the pack, sorting, trinkets, tiers, and the
  stores, with numbers pulled straight from the code.
- **Standard capabilities** — an item handler always, a fluid handler when a Rack is
  fitted, an energy handler when a Charge Crystal is fitted, so any mod's automation works
  against a placed pack. Zero hard dependencies.
- **Open Pack keybind** (default B) plus native right-click use. Pantrywork food tags fold
  into the Food tab when present.

### Known / on the bench
- **All four resource stores ship** (fluids, XP, energy, and — with Mekanism — gas).
- **Opening the pack GUI from the Curios back slot** isn't wired yet; open it from your
  inventory (the trinkets still work while it's worn).
- A dedicated **Outfitter's Bench** upgrade station and a **quest chapter** are still to come.

### Cut
- **Feather Charm** — Packwork has no pack-weight / encumbrance penalty, so a "weightless"
  fitting had no job. Removed rather than shipped as a dead craftable.

### Notes for returning players
Nothing to migrate — this is the first release.
