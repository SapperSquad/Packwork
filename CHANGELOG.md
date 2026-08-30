# Packwork changelog

All notable changes, newest first. Dates are the suite's working dates.

## 1.1.0 "Field Kit" — 2026-08-30

The adoption wave: the things that make a pack easy to run, easy to tune, and easy to
talk about.

- **Your pack shows up on your back** (NeoForge 1.21.1 and 1.21.8 for now). Wear one in a
  back slot and it renders there —
  the same per-tier leather, buckle and trim a set-down pack shows, right down to the
  Sculkhide's echo veins. It rides the shoulders, tips with you when you crouch, steps
  aside for an elytra, and vanishes when you do. Don't want it? `show_worn_pack = false`
  in `packwork-client.toml` — that setting is yours alone and never leaves your machine.
  Minecraft 1.21.10 rebuilt the entity renderer, so the newer NeoForge builds are waiting
  on a second implementation of the layer; everything else in 1.1.0 is on all six.
- **The Outfitter's Handbook has a last page: Field Reports.** Where to send a bug, what
  to put in it, and two brass-ruled links — GitHub issues and the Discord — that open in
  your browser after your game asks you first. Nothing pops up, nothing nags; the page is
  just there when you want it.
- **Ten more languages.** Simplified Chinese, Russian, Brazilian Portuguese, German,
  French, Spanish, Japanese, Korean, Polish and Ukrainian — every item, tooltip,
  compartment name, button and keybind. These are **machine-drafted first passes**, not
  yet read by native speakers; if one reads wrong to you, a one-line pull request is very
  welcome (see the README). The Handbook's long prose stays English for now.
- **A manual you can read in a browser** — `docs/`: sorting, tiers and fittings, stores and
  automation, plus two pages the in-game book can't hold. **Every config key** with its
  default and its range, and a **packmaker's page**: what to turn off, the item tags the
  auto-sorter reads, and how to fold your own mod's items into a compartment from a
  datapack.

## 1.0.0+mc26.1-fabric / +mc26.2-fabric — 2026-08-13 (version ports, wave 3: Fabric)

Packwork crosses the loader line: the same 1.0.0 "First Haul" on **Fabric**, for the
26.1.x and 26.2 lines. Nothing gameplay-visible changed; each build passes the same
58-test suite (plain, with Trinkets, with JEI + Trinkets) plus a pixel-inspected GUI
pass — sorting grid, deep counts, gauges, rule editor, Recipe Ledger, six-tier
placed trim, handbook, and the JEI upgrade ring.

- **New:** Fabric **26.1.x** and **26.2** builds (loader 0.19.3+, Fabric API), from
  `fabric/26.1` and `fabric/26.2`, jars named `packwork-1.0.0+mc<ver>-fabric.jar`.
- **Wear on Fabric = Trinkets** (the maintained Trinkets Updated fork): the pack rides
  the chest/back slot, B / Shift-B behave exactly as with Curios on NeoForge, worn
  trinkets keep ticking, and everything works without it (both wear mods stay optional).
- **Energy on Fabric = Team Reborn Energy**, the ecosystem's standard, bundled inside
  the jar (never a mod to install). Same reservoir, same numbers: 1 E = 1 FE.
- **JEI renders the pack ladder on Fabric too** — the full nine-cell ring, pack
  centered. Under the hood the upgrade recipes now opt into Fabric's client recipe
  sync, so any viewer reading synced recipes can see them.
- Hoppers, pipes, and cables feed a placed pack through Fabric's own transfer API —
  same standard-first story as NeoForge, no bridge blocks.
- **Mekanism and Forgework are NeoForge-only mods**, so the gas store and the Flux
  bridge stay dark on Fabric (no dead craftables; the Flask Harness recipe requires
  Mekanism present and stays off the creative shelf there).
- Internal, for the curious: the storage internals mapped straight onto Fabric's
  transactional transfer API (the design NeoForge's 21.9 rework shares), with the
  same three rules — per-slot depth, nesting refusal, one-stack pulls — enforced at
  the same native choke points, pinned by the same conservation gametests. Exactly
  three mixins where Fabric has no event: pack-first pickup, the Angler's Creel
  catch, and Quick-Draw's broken-tool refill (break-only on Fabric, documented).

## 1.0.0+mc26.1.2 / +mc26.2 — 2026-08-13 (version ports, wave 2)

The same 1.0.0 "First Haul" on Minecraft's 26.x lines. Nothing gameplay-visible
changed; each port passes the same 58-test suite (plain, with Curios, with JEI +
Curios) plus a pixel-inspected GUI pass against the master gallery.

- **New:** NeoForge **26.1.2** (26.1.2.95) and **26.2** (26.2.0.59) builds, from
  `port/26.1` and `port/26.2`, jars named `packwork-1.0.0+mc<ver>.jar`.
- Per-version soft deps: JEI (29 / 30) and Curios (15 / 16) light up on both lines.
  **Mekanism and Forgework stay 1.21.1-only** - the gas store and the Flux bridge
  simply stay dark on 26.x (no dead craftables).
- Internal, for the curious: 26.1 rebuilt vanilla's item-container component on
  templates that reject oversized stacks on read, so the pack's DEEP store now rides
  its own holder (`PackContents`) - same save format, same 384-a-slot depth - and
  the storage internals went fully native on NeoForge's transactional transfer API:
  one handler now backs the standard capability AND the menu/trinkets/sorting, so
  the depth clamp, nesting refusal, and one-stack pulls are provably the same rules
  inside and out. Conservation is pinned by the same gametests as every other line.
- Fixed in passing (all versions): a handbook caption that clipped at the panel edge
  ("one ladder, every pack") now sits whole on its own line above the pack row.

## 1.0.0+mc1.21.8 / +mc1.21.10 / +mc1.21.11 — 2026-08-13 (version ports, wave 1)

The same 1.0.0 "First Haul", ported to three more NeoForge lines so more players can
carry one. Nothing gameplay-visible changed - the sorting, trinkets, stores, ledger,
and worn-slot opening all behave exactly as on 1.21.1, and each port passes the same
57-test suite (plain, with Curios, with JEI + Curios) plus a pixel-inspected GUI pass.

- **New:** NeoForge **1.21.8** (21.8.54), **1.21.10** (21.10.64), and **1.21.11**
  (21.11.45) builds, each from its own `port/` branch with the jar named
  `packwork-1.0.0+mc<ver>.jar`.
- Per-version soft deps: JEI and Curios light up on every line (JEI 24/26/27,
  Curios 12/13/14). **Mekanism and Forgework stay 1.21.1-only** - neither ships for
  the newer lines yet, so the gas store and the Flux bridge simply stay dark there
  (no dead craftables; the Flask Harness recipe still requires Mekanism present).
- Internal, for the curious: the newer lines rebuilt several foundations underneath
  the same behavior - recipes are placement/display-based, the Recipe Ledger's
  craftable scan runs server-side (clients stopped receiving recipes in 1.21.2),
  gametests are registry entries, and on 1.21.10+ the standard capabilities are the
  transactional transfer API. Conservation guarantees (depth clamps, no nesting,
  one-stack pulls, pause-never-punish) are pinned by the same gametests everywhere.

## 1.0.0 "First Haul" — 2026-07-26

The first public release. Everything below this header up to the internal 0.1.0
milestone shipped together as **1.0.0**: the playtest-polish waves (worn-slot opening,
pack-first pickup, the upgrade ring, the rule editor, keep-my-layout, JEI real recipes)
on top of the internal first build.

### Added — open the pack straight off your back (Curios)
- **A worn pack opens now.** With Curios installed and a pack in the back slot, **B**
  opens it whenever your pockets hold no pack, and **Shift-B** (rebindable in Controls)
  opens the worn one outright — same tabbed organizer, fully live: sort, pin, trinkets,
  stores, rule editor, and the Tinker's Kit bench all read and write the pack on your
  shoulders. This closes the old honest-note ("open it from your inventory") for good.
- Unequipping the pack while its GUI is open closes it cleanly: anything on the tool
  roll comes back to you, and nothing is ever written onto — or duped out of — a pack
  that already left the slot. The scan order stays consistent everywhere: pockets first,
  then the back slot, same as pack-first pickup.
- The back slot is now assigned to players by an explicit `curios/entities` datapack
  file instead of relying on defaults, so the slot exists in every environment
  (dedicated servers and test harnesses included).

### Internal — release dressing (nothing published)
- `PUBLISHING.md` restructured into the paste-ready store kit: summary, full project
  description, gallery plan with captions, requirements, and the first-release changelog
  block. (The version placeholder it carried was stamped **1.0.0** at release.)
- New `promo/` kit: icon + wide banner composed from the real in-game sprites by
  `tools/GenPromo.java` (Java-only tooling), plus eight staged gallery shots from the new
  `-Pgallery` dev-harness chain — six-tier lineup, sorting GUI, Recipe Ledger ghost, rule
  editor, keep-my-layout, drop-to-pin + pickup toggle, the glowing tiers at night, and the
  JEI upgrade ring. Every shot inspected as pixels before use.

### Added — the Lodestone files what you mine (pack-first pickup)
- **Mined cobble goes straight in the pack now.** With a Lodestone Charm fitted (carried
  or worn), anything you pick up that the pack knows where to put — it sorts to a
  compartment, it's pinned somewhere, or the pack already holds some — routes **pack-first**
  and files itself under the right tab, with the usual pickup flourish. New, unknown finds
  still land in your pockets, so nothing ever vanishes into the bag unseen.
- **A small switch in the title strip** (it appears with the Lodestone) turns pack-first
  pickup on or off per pack; it remembers per pack. If the pack is full for an item, it
  takes what fits and the rest reaches your pockets — never the void. Items on a Compass
  Rose's void list are binned on pickup, exactly as the magnet already does. Packs are
  never swallowed into packs.

### Changed — every upgrade is a full ring around the pack
- **The recipe is a picture now.** Set your pack in the MIDDLE of the bench and build the
  next tier around it — all nine cells filled, the tier's bulk material on the four
  edges, its fittings on the four corners. Turn the ring any way you like; swap edges and
  corners and it won't craft, because the picture is the recipe.
- **The ladder, cell by cell:** CANVAS is a chest wrapped in wool, corners tied with
  string. LEATHER rings the canvas pack in leather with copper buckles at the corners.
  STUDDED rings it in **copper set with iron studs**. REINFORCED wears **gold cornered
  with diamonds**. RUNED binds **diamond in netherite**. SCULKHIDE closes the ladder with
  **amethyst cornered in echo shards** from the Deep Dark. Everything inside still rides
  up untouched.
- The sixth tier's marginal ring is deliberately cheaper than Runed's diamond-and-
  netherite: the real gate is that it demands the Runed pack itself, so the total climb
  still holds.

### Changed — the sixth tier is the SCULKHIDE Pack
- **Dragonhide is renamed to Sculkhide**, and the tier's whole identity moves from the
  End to the Deep Dark: the near-black hide keeps its brick-laid plates but goes cold
  sculk-teal, the pale dragon-bone claws are now **echo-cyan sculk veins** creeping over
  the flap's shoulders, and the ember-pink breath gem is an **echo gem** in the buckle —
  same glow when you set it down, colder light. The Studded pack's studs also went from
  brass to iron-grey to match its new iron-cornered ring.
- **Dev-world note:** this is a registry rename (`dragonhide_pack` → `sculkhide_pack`)
  with no migration — Dragonhide packs in existing dev worlds will not survive. Nothing
  was ever released with the old id.
- The sixth-tier pack can now actually be worn: it was missing from the Curios back-slot
  tag (found during the rename).

### Fixed — JEI shows the real recipes
- **"How do I make each pack" is a recipe again, not lore.** The tier-upgrade craft is a
  custom recipe, and JEI was never taught to draw it — every pack above Canvas showed only
  its info page. Two pieces were needed, and the first field test caught that the second
  alone wasn't enough: JEI's recipe scan **silently drops** any crafting recipe whose
  ingredient list is empty, before drawing extensions are ever consulted — so the upgrade
  now carries its honest cell-by-cell ingredient list (the previous pack, then one cell
  per material), and Packwork's JEI extension draws the layout in the standard crafting
  category: previous pack + materials in, next pack out, marked shapeless, with the
  carries-everything-up behaviour spelled out on the result's tooltip. Canvas, every
  trinket, and the handbook were plain recipes all along and render as ever; the info
  pages stay on as supplements. The JEI plugin also logs one "Packwork JEI:" line with the
  upgrade-recipe count, so this working (or not) is visible in any log.
- **The upgrade can't be underpaid anymore.** Crafting consumes exactly one item per grid
  cell when you take the result, but the upgrade recipe was counting item *totals* — so a
  stack of four shulker shells in ONE cell matched, and the craft quietly charged you one.
  Materials now count per CELL: spread them out, one per cell, like every vanilla recipe —
  which is exactly the layout JEI draws.

### Changed — pinning you can read
- **Drop an item into a tab and it stays there.** Putting an item into a compartment its
  rules wouldn't send it to now pins it on the spot — no more dropping bread into
  Valuables and watching the sort snatch it back. A stitched parchment note says what
  happened ("Pinned Bread to Ores & Valuables — auto-sort won't move it") and the red
  ribbon appears immediately. Dropping an item where it already belongs pins nothing.
- **P still works, and now it talks back.** The hover-and-press-P toggle shows the same
  note both ways, and the tooltip says what pinning *means* in plain words: "Keep in this
  tab — auto-sort won't move it."

### Added — every compartment chooses: tidy, or keep my layout
- **A per-compartment arrangement switch**, under the grid by the page count. TIDY is the
  pack as you know it — the compartment arranges itself. Flip it to **KEEP MY LAYOUT** and
  items stay in the exact cells you drop them: lay your tools out your way and they're
  still there after a relog, after a trip, after a hopper tops the pack up — new arrivals
  quietly fill the gaps instead of reshuffling your work. **Tidy Up still works as a
  one-shot re-sort**; the sorted order just becomes your new starting layout. Flip back
  and the pack takes over again.
- Under the hood the arrangement is pure view — every item still lives exactly once in the
  pack's one store, so a kept layout can never dupe or lose anything, and it rides the
  pack through drops, placements, and upgrades like everything else.

### Changed — the Quill & Ledger got a real job
- **Stamps just work now.** Stamp a custom compartment with a pickaxe and it gathers your
  tools; stamp it with bread and it gathers food — no fitting required. The stamp was
  always meant to be a live filter; now it's the baseline every pack gets.
- **The Quill & Ledger is the rule editor.** Fit it, open a custom compartment, and click
  the quill under the grid: a parchment sheet where you write your own filters — type a
  word and file it **by name** or **by mod**, or press a **category chip** (Food, Tools,
  Weapons, Armor, Blocks, Potions) — and strike any of them off again. Written rules sort
  while the ledger is fitted; pull it and the compartment falls back to its stamp and
  pins, with your writings kept safe in the leather for its return. Pins still beat
  everything, so exceptions stay yours.

### Added — deep slots (the headline)
- **Every tier now deepens every slot.** A slot holds one vanilla stack per tier step: Canvas
  64 of a common item, Leather 128, Studded 192, Reinforced 256, Runed 320, Sculkhide 384 —
  six whole stacks in one slot, filed under one tab. Sixteen-stackables scale the same way
  (pearls: 16 up to 96); unstackables never stack. Tidy Up merges loose stacks down into
  depth, and deep counts render as exact numbers, sized to stay inside their own cell.
- **Depth stays inside the pack.** Anything that leaves — your cursor, a hopper, a fitting
  drawing from stock — always comes out one vanilla stack at a time, so the world outside
  never sees an impossible stack. And the pack's save format carries deep counts safely
  (vanilla's own item format caps at 99 and would have corrupted them); packs saved before
  this update load exactly as they were.
- **Depth is the tier's job; slots are the Lining's.** The Bottomless Lining keeps adding
  BREADTH (more slots); the material ladder now owns DEPTH. One axis each, no double-dipping.

### Added — the Sculkhide Pack (a sixth tier)
- **Above Runed: the Sculkhide Pack**, cut from a near-black hide cured in the Deep Dark —
  brick-laid plates gone cold sculk-teal, echo-cyan veins creeping over the flap, and an
  echo gem set in the buckle. Crafted from a Runed pack ringed in amethyst and cornered
  with echo shards. Five trinket sockets, 384-deep slots, and every store at six times
  Canvas (a 48-bucket waterskin, 30,000 XP, 600,000 FE of arcane charge, 96,000 mB of
  bottled vapors). Set one down and its gem lights the camp brighter than a Runed pack's
  glyphs. (Shipped briefly in dev as "Dragonhide" — see the rename note above.)

### Changed — the ladder is a chain now
- **Every tier above Canvas is crafted FROM the pack before it.** The old raw-material
  recipes for Leather and up are gone; the pack-plus-materials upgrade craft IS the recipe,
  and it has always been the preserving one — contents, compartments, pins, trinkets, name,
  all carried up. There is no recipe left anywhere that could eat a filled pack.
- **Upgrades now carry the stores too.** Found while reworking the chain: an upgrade used to
  quietly drop stored water, XP, charge, embers and vapors. Everything rides up now.
- **The Runed upgrade keeps the Deep Dark gate** the old raw recipe had — echo shards sit
  at its corners (see the full-ring recipes above).

### Added — the Recipe Ledger (the Tinker's Kit browser)
- **A parchment sheet of everything the pack can make right now.** With the tool roll out,
  a ledger button opens a searchable sheet computed from YOUR PACK's stock — not your
  pockets. Click a recipe to **chalk it onto the roll** (a translucent ghost of the pattern;
  nothing moves), then click the result well to **lay it out from stock** — the pack covers
  the whole pattern or moves nothing at all. Craft and the bench refills as usual. Scroll to
  browse, type to search, click the chalked recipe again to wipe it off.

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
- **Nothing sits low or bleeds off the slot anymore.** Every Packwork sprite (every pack,
  every trinket, the handbook, the block-item) was re-authored to a shared centred box with a 1px
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
- **The packs are hero art now (32×32).** Each pack is a boxy, form-shaded leather
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

## 0.1.0 — 2026-07-23 (internal milestone, never published)

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
