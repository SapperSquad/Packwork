# Packwork changelog

All notable changes, newest first. Dates are the suite's working dates.

## Unreleased — playtest polish

### Added — craft on the go
- **Tinker's Kit: a bench inside the pack.** Fit the kit and a latch appears in the title strip;
  click it and a leather tool roll unrolls across the pack's lower rows — a 3×3 workspace and a
  brass-ringed well for what comes out, with the top rows of your pack still in reach above it.
  **Shift-click from the pack to lay one item on the bench**, and after every craft each emptied
  cell tops itself straight back up from your stores. Set the pattern once and one shift-click on
  the result runs the whole batch until the pack is out of makings. Roll it back up — or just
  close the pack — and everything laid out goes home. No craft can ever cost you an ingredient it
  didn't turn into something.

### Added — six more fittings
- **Field Furnace** — banked campfire embers cook as you walk. Raw ore and raw food only, so it
  never turns your cobblestone to stone behind your back, and it burns proper fuel (coal,
  charcoal, blaze rods, lava) at exactly furnace rates: a lump of coal is still eight things
  cooked. If the finished piece won't fit, the raw one never leaves its slot.
- **Provisioner's Pouch** — feeds you before hunger bites, and eats the plainest thing in the
  pack first. Anything carrying an effect — your golden apples, your suspicious stew — stays
  yours, and it won't touch rotten flesh. The bowl comes back in the pack.
- **Cartographer's Sleeve** — opens a **Charts & Bearings** compartment: maps, compasses, clocks
  and the spyglass file themselves instead of scattering through Tools.
- **Angler's Creel** — opens **The Catch**, and your catch drops straight into the pack instead
  of bouncing off your chest. A full pack simply hands it back, as always.
- **Torchbearer's Loop** — sets a torch down from pack stock whenever you're standing somewhere
  genuinely dark. It stops the moment the light comes up, and a torch that can't stand there goes
  straight back in.
- **Herbalist's Bundle** — replants a grown crop the instant you pull it, spending one seed out
  of your own stock. If the ground is taken by the time it gets there, the seed comes home.
- Pull either gated fitting and its compartment simply closes; the items re-route, because a tab
  was only ever a filter over one flat store.

### Changed — the art reads cleaner
- **The packs were busy, not detailed.** The shading carried per-pixel random noise on top of the
  leather grain, the canvas weave ran at a one-pixel pitch, the hem stitch was every other pixel
  at near-white, and the studs, plates and runes were single-pixel sprinkles. At hotbar size all
  of that dissolves into static. Now: broad, smooth value transitions with one large soft mottle;
  a coarse low-contrast canvas weave; a proper dashed thread on an unbroken seam; **studs that are
  studs** (2×2, lit, with a contact shadow), **bevelled steel plates with a brass rivet**, and
  **runes drawn as strokes with a soft glow**. The detail is still all there — it just stopped
  fighting the shape.
- **The closure strap stopped reading as a hole.** Its shading was bottoming out at near-black and
  punching a dark slot down the middle of every pack; it's darker leather now, with a tapered tip,
  and the buckle got a crisp outer edge to sit against it. Re-checked at 16px and 12px.
- Same de-noising on the placed-block faces, so a pack you set down still matches the one in hand.

### Fixed
- **Filling the waterskin no longer throws your bucket on the floor.** The gauges and the tab
  rail hang outside the panel's edge, and that strip is exactly where the game assumes you meant
  to toss whatever you're holding — so a click that filled the tank *also* pitched the bucket at
  your feet. The pack now counts its own rails as part of the pack, for the press and the
  release both. Same fix covers clicking a leather tab with something on the cursor.
- **A stack of buckets stays a stack.** Filling or emptying at the gauge handles exactly one
  container per click: one goes in, one comes back — to your cursor if it fits there, else your
  pockets, else the pack. Holding three water buckets used to leave you holding one empty one,
  with the other two gone.
- **The gauge waits for the server before it moves anything.** Anything that touches real items
  or your XP — the waterskin, the soul vial's siphon and pour — is settled server-side now, so a
  laggy click can't half-apply twice. Tabs, search and pins still respond instantly.
- **A set-down canvas pack's buckle is twine now, matching the one in your hand.** The placed
  canvas block wore a brass buckle like the higher tiers, but the canvas *item* closes with a
  twine buckle — so the block and the item disagreed. The canvas block's buckle now reads as pale
  woven twine cord. The other four tiers keep their brass buckles, and the canvas straps are
  unchanged.
- **Search text is crisp now.** The search field was drawn with a drop shadow, which muddied
  dark text on the pale canvas strip into a fuzzy look; it now draws sharp at every GUI scale.
- **Nothing sits low or bleeds off the slot anymore.** Every Packwork sprite (5 packs, 11
  trinkets, the handbook, the block-item) was re-authored to a shared centred box with a 1px
  margin, so no icon runs to the edge or hangs low in its cell. A tiny audit tool
  (`tools/AnalyzeSprites.java`) proves each sprite's bounding box and margins.
- **Hover + P to pin actually shows — and now you can't miss it.** Pinning is a rebindable
  keybind (shows in Controls) and a pinned slot wears a bold red ribbon with a brass tack in
  its corner (was a faint pin-head); a hovered item's tooltip still reminds you "[P] Pin to
  this tab".

### Changed — art pass (hand-authored)
- **Sprites are hand-authored pixel art now, not procedural.** Every icon is placed pixel by
  pixel with a top-left light source, a per-material value ramp (shadow → mid → light →
  highlight) and a clean dark outline, so the set reads as crafted leather-and-brass gear
  instead of generated noise.
- **The murky icons got fixed.** Quill & Ledger is a legible open book with a quill + ink nib;
  Quick-Draw Straps are two clearly buckled belts (no longer a red ✗); the Repair Kit is a
  hammer on an anvil; and the Soul Vial (one green vial) reads apart from the Flask Harness (a
  rack of two vapour flasks) and the Waterskin (corked, with a water sheen).

### Changed — hero packs + trinket polish
- **The five packs are hero art now (32×32).** Each pack is a boxy, form-shaded leather
  **backpack** — a draped flap with a stitched hem, a brass buckle with a metal glint, a strap
  with real thickness, side pockets, a top grab-handle — lit against a rounded, cushioned form
  with ambient occlusion under the flap and a rim light. The material story climbs the ladder:
  **canvas** weave + twine → **leather** grain + brass buckle → **studded** leather + a ring of
  brass studs → **reinforced** steel corner plates + a steel band → **runed** deep-dyed leather
  with glowing glyphs + a gem in the buckle. The placed-block leather + brass faces got the same
  lift so a set-down pack matches the one in your hand.
- **The packs actually read as backpacks now.** The first hero pass came out too round — the
  bags read like pouches, and the runed one like a magic orb. The silhouette was rebuilt boxy
  and gently tapered with a flat bottom and a prominent top flap, so a pack reads as a pack even
  shrunk to hotbar size, without losing any of the hero-pass shading. The **Charge Crystal**'s
  energy gauge on the right rail went from amber to the fitting's cool crystal-blue, so the icon
  and the gauge finally agree.
- **A set-down pack now shows its tier in the world.** The placed pack used to be one tinted
  leather box for every tier; it now carries the full material ladder onto the block — canvas
  weave + twine, leather grain, **brass studs**, **riveted steel corner plates + a band**, and
  **glowing runed glyphs + a gem** (the Runed pack even gives off a faint arcane glow). Break it
  and you still get exactly the pack you set down, right down to the tier.
- **Three trinkets sharpened.** The **Restock Strap** is now a bold bandolier — a big central
  brass buckle with a studded pouch above and below. The **Charge Crystal** is unmistakably a
  faceted crystal (cool blue, wound in dark copper on a brass mount) instead of reading like a
  flame. The **Lodestone Charm** is a dark iron-grey magnetite stone with a faint violet sheen
  on a cord (no more antennae).

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
  tools in your hands. A cool crystal-blue gauge shows the charge, matching the fitting.
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
