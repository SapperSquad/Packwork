# Packwork changelog

All notable changes, newest first. Dates are the suite's working dates.

## 0.1.0 "First Haul" — 2026-07-23

The first shippable build: the self-sorting pack, the material ladder, the trinket
framework, and the first resource store.

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
  eight craftable fittings. Working: Lodestone Charm (magnet), Restock Strap, Repair Kit,
  Bottomless Lining (extra slots, never voided), Compass Rose (opt-in void — the only void
  path).
- **Fluids store** — the Waterskin Rack fits a tier-scaled fluid tank shown as a glass
  gauge; fill or drain it with a bucket or flask. Exposes NeoForge's fluid capability only
  when the Rack is fitted.
- **Standard capabilities** — an item handler always, a fluid handler when a Rack is
  fitted, so any mod's automation works against a placed pack. Zero hard dependencies.
- **Open Pack keybind** (default B) plus native right-click use. Pantrywork food tags fold
  into the Food tab when present.

### Known / on the bench
- **Feather Charm, Quick-Draw Straps, and Quill & Ledger** are craftable and slot in, but
  their effects aren't wired yet. Feather is waiting on a pack-weight decision.
- **Resource stores:** only fluids so far — XP, energy, and gas are next.
- **No Curios wear-slot** compat yet (native use + keybind cover carrying it); no in-game
  guide book or JEI integration yet.

### Notes for returning players
Nothing to migrate — this is the first release.
