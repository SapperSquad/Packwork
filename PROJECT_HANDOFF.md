# Packwork — Project Handoff & Design Bible

> The canonical state of the project. Read this and `DECISIONS.md` before doing anything.
> Keep this file updated as work lands — it is the resume mechanism between sessions.

## What it is

**Packwork** is a NeoForge 1.21.1 portable-storage mod: a humble adventurer's pack that
holds far more than it should and quietly organizes itself. It's the "much better
Sophisticated Backpacks" — the headline is a **tabbed, self-sorting GUI** where items
flow into premade or player-made compartments, and the pack can also carry **fluids,
gases, energy, and XP** — all re-skinned as leather-and-brass gear, never tech.

Published under **SapperSquad**, playful forge-y voice. Sits beside Coinkeep, Highroller,
Forgework, PhytoForge, Gunsmith, Pantrywork, and Reel Rivals.

## Status — shippable for its available-dep scope; gas/Curios/JEI deferred, ready to enable

> Newest first. Full source map and roadmap below. Version is still **0.1.0** (unreleased
> first build); bump/label at publish. 23 GameTests green; jar builds clean.

**2026-07-23 art pass 4 (backpack silhouette) — DONE & verified in-game.** Alex reviewed the
hero packs: shading was good but the silhouette had come out too ROUND — the packs read like
pouches/orbs (the runed one like a magic orb), not rugged backpacks. Reshaped in-pipeline,
keeping every fidelity gain from pass 3.
- **The 5 pack sprites are now unmistakable backpacks.** `heroPack` in `tools/GenTextures.java`
  swapped its elliptical `dome()` form for a **superellipse cushion** (`superForm`): a boxy,
  gently tapered body with a flat bottom, plus a **prominent wide flap** draped over the top
  third with a hard stitched hem + AO shadow beneath it, a central closure strap through a brass
  buckle straddling the hem, a top grab-handle loop, and side pockets. Kept from pass 3: the
  32×32 form-shading, AO, rim light, specular buckle glint, and the per-tier material ladder
  (canvas weave+twine → leather grain+buckle → brass studs ringing the flap → riveted steel
  plates+band → deep leather+glowing runes+gem). **Verified as pixels at hotbar size**: a new
  offline preview (`tools/pack_small_preview.png`, box-downscaled to 16px & 12px) plus the live
  hotbar/inventory-row/in-hand/GUI-host-slot shots — it reads as a pack even shrunk.
- **Energy gauge unified with the Charge Crystal (Alex confirmed: match them).** The right-rail
  energy gauge in `PackScreen.drawEnergyGauge` went from amber to the crystal's cool blue
  (`0xFF3EA9C4` fill on `0xFF15323B` glass), so the icon and the gauge agree; still distinct
  from the deeper water-blue fluid gauge.
- **Placed block:** the block MODEL is boxy (body + draped flap + handle + buckle + straps) and
  its leather/brass faces + per-tier tint matched the item's material language. **Per-tier block
  trim was the noted follow-up at the time** — now done, see the next entry.
- 23 GameTests green; `compileJava` clean; version stays **0.1.0**.

**2026-07-26 playtest wave 4 (in progress) — JEI real recipes + legible pinning.** Alex's
live-client playtest called four things; the first two are landed. 42 GameTests green.

1. **JEI renders the pack ladder as REAL recipes (bug).** The compat plugin only registered
   info pages, so "how do I make each pack" showed lore. `PackworkJeiPlugin` now registers an
   `ICraftingCategoryExtension<PackUpgradeRecipe>` (via `registerVanillaCategoryExtensions`,
   verified against the pinned JEI 19.21.1.312 API jar): previous-tier pack + material cells
   in, next pack out, shapeless-marked, and the result's tooltip notes that contents/layout/
   trinkets/name/stores all carry up. Trinket + handbook + Canvas recipes are plain JSONs and
   always rendered. Info pages stay as supplements. **Found under this: the upgrade could be
   UNDERPAID** — `matches()` summed item counts but vanilla crafting consumes one item per
   grid cell (`ResultSlot.onTake`), so 4 shells stacked in one cell bought the craft for 1.
   Materials now count per CELL, exact (`found[m] == count`), `canCraftInDimensions` demands
   1 + total cells, and the JEI layout is literally the gesture. Gametests updated (spread
   inputs; a stacked input is pinned as NOT matching).
2. **Pinning is legible (Alex: "I don't understand what it means").** Three layers:
   (a) tooltip copy in plain words ("[P] Keep in this tab — auto-sort won't move it");
   (b) feedback — a stitched parchment note over the panel names the tab on every pin/unpin;
   (c) the natural gesture — placing an item into a tab its rules would NOT route it to
   auto-pins it there. Mechanism: `PackViewSlot.setByPlayer` (the one hook vanilla fires only
   for the player's own hand — place/merge/swap, verified in the decompiled sources) records
   the placement; `PackMenu.clicked` flushes it AFTER the click resolves (rebinding mid-click
   would fight vanilla's bookkeeping) and applies the pin identically on both sides — no new
   packet. Handbook + README/PUBLISHING copy updated. Gametest `droppingIntoForeignTabAutoPins`.

**2026-07-25 playtest wave 3 — DEPTH, the recipe chain, the Dragonhide tier, the Recipe
Ledger.** Alex's four asks, all landed. 41 GameTests green; version stays **0.1.0**.

1. **Per-slot DEPTH by tier (the headline).** Every slot holds `maxStack × (ordinal+1)`:
   Canvas 64 → Dragonhide 384 of a 64-stackable (pearls 16→96; unstackables never stack).
   The hard part was persistence: `ItemStack.CODEC` caps counts at 99 (verified in sources),
   so `pack/DeepContentsCodec` persists `{slot, item:{id,components}, count}` with an
   unbounded count and a `Codec.withAlternative` fallback that loads pre-depth saves intact
   (count REQUIRED on the deep shape so legacy data falls through instead of shrinking to 1).
   Network sync needed nothing (stream codecs are uncapped VarInts). Escape hatches closed:
   `PackInventory.insertItem` (parent clamps to item max — verified), `extractItem` (parent
   doesn't clamp at all), `PackViewSlot.remove` (vanilla pickup passes Integer.MAX_VALUE) +
   a swap-guard in `mayPlace` (swap hands the slot stack to the cursor without `remove`).
   Rule: inserts fill to depth, every pull out is ≤ one vanilla stack. Tidy merges INTO
   depth. Deep counts render exact at 3/4 scale inside their own cell (vanilla's 3-digit
   spill smeared into the neighbour — seen and fixed as pixels). Gametests: tier depth
   scaling, relog + block-entity NBT + legacy round-trips, never-escapes drain, tidy depth.

2. **The recipe chain (supersedes raw-materials-only — Alex's call).** Canvas stays a raw
   craft; every higher tier is `packwork:pack_upgrade` FROM the previous pack (raw recipes
   for tiers 2+ deleted). Found + fixed: the upgrade was silently dropping the five STORE
   components (fluid/XP/energy/embers/chemical) — all carried now, gametested with deep
   contents + name + trinkets across Runed→Dragonhide. The recipe gained an optional
   `material2`/`count2` (hand-rolled stream codec; 7 fields beats composite's arity).

3. **DRAGONHIDE, the 6th tier (name flagged for Alex — alternatives: Wyrmhide, Drakeskin).**
   Runed pack + 4 shulker shells + 4 dragon's breath. 256 slots (component cap — the top
   tiers grow DEEP, not wide), 5 sockets, depth ×6, stores ×6, placed light 11. Art: near
   black charcoal-plum hide, brick-laid scale scallops, pale bone claws, ember-pink breath
   gem — clearly a step past Runed at hotbar size (checked in `pack_small_preview.png` and
   in-game). Blockstate went 20→24 variants; the gauge rail shrinks (`gaugeHeight()`) so
   5 sockets + gauges stay inside the panel. Runed upgrade also gained 2 echo shards
   (keeping the deleted raw recipe's Deep Dark gate).

4. **The Recipe Ledger (Alex said the word on the recipe book).** Path taken: IN-HOUSE
   parchment browser, not `RecipeBookMenu` — vanilla's craftability + auto-place plumbing is
   hardwired to the player inventory and its layout claims the tab rail's flank; fighting
   both is more code than the sheet. Client-side: searchable, scrollable list of every
   3×3-able recipe craftable FROM PACK STOCK (StackedContents at full depth + the roll),
   recomputed on open/search/40 ticks. Click chalks a GHOST onto the roll (vanilla's own
   ghost-overlay render pattern; zero movement). Clicking the result well sends the ONE
   server verb `LAY_OUT_GHOST`: simulate-first, all-or-nothing pull of one item per cell
   from pack stock. Gametested (uncoverable recipe moves nothing). The GUI recentres while
   the ledger is open so it never clips the screen edge (found as pixels, fixed).

**2026-07-25 playtest wave 2 — bug fix, art de-noise, 7 new fittings, craft-on-the-go.** Alex
playtested and called four things. All four landed.

1. **BUG: the waterskin gauge threw your bucket on the floor. FIXED, verified in a live client.**
   Three real bugs on one path. (a) The gauges and the tab rail are drawn OUTSIDE the panel rect,
   which is exactly the region vanilla treats as "clicked outside the GUI"; consuming the press
   was never enough because the drop fires on **release**
   (`AbstractContainerScreen.mouseReleased` → `slotClicked(null, -999, PICKUP)` →
   `AbstractContainerMenu.doClick` → `player.drop`), and `PackScreen` doesn't override
   `mouseReleased`. `PackScreen.hasClickedOutside` now returns false over both rails, killing it
   for every rail widget at once. (b) `FluidUtil.tryEmptyContainer/tryFillContainer` act on ONE
   container and return one item, so `setCarried(result)` was replacing a whole carried stack with
   a single bucket — the menu now spends exactly one and hands the result back (cursor → pockets →
   pack → floor). (c) FLUID_INTERACT / XP_SIPHON / XP_POUR are **server-authoritative** now
   (`PackAction.serverAuthoritative()` + an `isClient()` guard in `PackMenu`); layout verbs keep
   their optimistic apply. **Live proof** (`runClient -Pautoshot` dispatches a real press+release
   at the gauge): 1 bucket → empty bucket ON THE CURSOR, nothing on the ground; 3 water buckets →
   2 stay on the cursor + 1 empty bucket in the pockets; an empty bucket fills back. 3 new
   conservation gametests.

2. **ART: the packs read busy, not detailed. DE-NOISED.** Diagnosis confirmed at the pixel level:
   `leatherGrain` added **per-pixel random ±3** on top of a 5-row crease, the canvas tier ran a
   1px-pitch crosshatch at ±8 over the whole sprite, the hem stitch was every-other-pixel at
   near-white, and the trim was 1px sprinkles. All of it is high-frequency detail that turns to
   mush the moment a 32px sprite lands in a 16px slot. Fixes, all in `tools/GenTextures.java`:
   a new `smoothNoise` (coarse lattice + smoothstep) replaces every per-pixel `valueNoise` call;
   grain is now a soft 7-row crease plus one large mottle; the canvas weave is a 2-on/2-off rib at
   ±5; the hem is a 3-on/1-off dashed thread on an unbroken groove; studs became **2×2 shapes with
   a contact shadow** (6, not 9), plates became **bevelled 4×4 with a brass rivet**, runes became
   drawn strokes with a 1px bloom. Also: the closure strap was bottoming out on the ramp's darkest
   stop and reading as a black slot punched through the pack — it's darker leather with a tapered
   tip now, and the buckle got a hard outer edge. Same treatment applied to the placed-block faces
   so a set-down pack still matches. Re-checked at 16px and 12px via `tools/pack_small_preview.png`.

3. **SEVEN new fittings** (SSOT entries in `TrinketType`, effects in `TrinketEffects`, recipes,
   sprites, Handbook entries, lang, gametests): **Tinker's Kit** (below), **Field Furnace**
   (cooks raw ore + raw food on pack fuel, at furnace rates, via a `pack_embers` component),
   **Provisioner's Pouch** (eats the plainest thing in the pack when you're down to 3 haunches —
   effects-bearing foods and the datapack `packwork:never_auto_eat` tag are left alone),
   **Cartographer's Sleeve** and **Angler's Creel** (each opens a **fitting-gated compartment** —
   `AutoTabs.Auto` gained a `gate` field, so a trinket adds a compartment with ONE table entry),
   **Torchbearer's Loop** (sets a torch from pack stock when you're in the dark), **Herbalist's
   Bundle** (replants a grown crop from your own seed stock).

4. **CRAFT ON THE GO — the Tinker's Kit.** A leather **tool roll** unrolls across the pack's
   bottom three grid rows (a latch appears in the title strip only when the kit is fitted): a 3×3
   bench plus a brass-ringed result well, drawn as leather and canvas, never a workbench UI. The
   pack keeps its top three rows so you can still see and reach your stock. **Shift-click from the
   pack lays ONE item on the bench** (you're setting a pattern, not tipping a stack in), and after
   every craft each emptied cell **tops itself back up from pack stock** — so one shift-click on
   the result runs the batch until the pack is out of makings. Shift-clicking the result puts the
   output in the pack first, your pockets second. Rolling up — or closing the pack — returns
   everything laid out. **Conservation is gametested end to end**: 12 planks' worth in play, 12 at
   every step, and no free craft once the pack is dry.

**34 GameTests green**; `compileJava` + `runData` clean; version stays **0.1.0**.

**2026-07-24 art pass 5 (per-tier placed-block trim) — DONE & verified in-game.** The deferred
follow-up: a set-down pack now shows its tier's detailing in the world, not just a tinted base.
- **A `tier` `EnumProperty<PackTier>` blockstate** (5 values) drives per-tier models + textures
  statically. It's set from the placed pack item in `PackContainerBlock.getStateForPlacement`
  (and re-synced in `PackContainerBlockEntity.setPackStack` for the test/stack-swap path).
  **Contents still live on the block entity**; the blockstate is render-only and never feeds the
  drop, so the place↔break round-trip is byte-for-byte lossless and the break returns the
  right-tier item exactly as before.
- **Per-tier faces, from `tools/GenTextures.java` (shared `TIER_RAMP`):** each tier gets a
  colour-baked `pack_<tier>_leather` (body/sides/top) + a trimmed `pack_<tier>_front` (the flap
  face) carrying the item ladder — canvas weave+twine → leather grain → **brass stud ring** →
  **riveted steel corner plates + band** → **glowing runed glyphs + gem**. Trim is kept clear of
  the 3D brass buckle/straps. One shared `pack_shape` model holds the geometry; five tiny child
  models (`pack_<tier>`) swap textures; a 20-variant blockstate (facing × tier) picks them. The
  old block colour handler + neutral `pack_block.png` were removed (colour is baked now).
- **Runed glow:** the Runed tier emits block light (`lightLevel` on the `tier` property) and its
  glyphs are bright/high-contrast, so a set-down Runed pack visibly glows.
- **Verified in-game** (`runClient -Pautoshot`): all five tiers placed side by side show distinct
  trim; the placed-pack GUI still opens/binds (title "Studded Pack", contents + gauges); a
  break/replace check breaks the Runed pack (drop logged as `packwork:runed_pack`) and re-places a
  Leather pack, whose render retracks with no stale trim. **24 GameTests green** (added
  `placedTierDrivesBlockstateAndDrop`: the render tier tracks the pack, the drop stays right-tier,
  a swap retracks). `compileJava` clean; version stays **0.1.0**.

**2026-07-23 art pass 3 (hero packs) — DONE & verified in-game.** Alex asked for hero art on
the 5 packs plus cleanup on 3 weak trinkets.
- **The 5 packs are now 32×32 hero sprites**, rendered by a form-shading model in
  `tools/GenTextures.java` (`heroPack`): a rounded body + draped flap dome lit top-left, AO in
  the flap seam, a rim light, a stitched hem, a brass buckle with a specular glint, a strap
  with thickness, side pockets and a handle. Per-tier material story: canvas weave+twine →
  leather grain+buckle → brass studs → riveted steel plates+band → runed glyphs+gem. Item
  model is `item/generated`, which renders a 32×32 sprite crisply in-slot/in-hand.
- **The placed-block faces (`pack_block.png`, `pack_block_brass.png`) went to 32×32** with real
  leather grain + a stitched seam + a bevel, and brushed brass + rivets, so a set-down pack
  matches the held item. (Per-tier trim on the *block* is still just the tint — studs/plates/
  runes on the block would need per-tier block textures + models; noted as a follow-up.)
- **3 trinkets cleaned:** Restock Strap → bold central brass buckle + two studded pouches;
  Charge Crystal → cool-blue faceted crystal wound in dark copper on a brass mount (kills the
  candle-flame read); Lodestone Charm → dark magnetite stone on a cord, no antennae. (The
  energy gauge was still amber here; art pass 4 unified it to the crystal's cool blue.)
- Verified in-game (`runClient -Pautoshot`, single clean run): hero packs in-hand, in the
  hotbar/inventory row, the GUI host slot, and three placed blocks in-world; all 26×29-centred,
  no edge touch; 23 GameTests green.

**2026-07-23 art pass 2 — DONE & verified in-game.** Alex playtested art pass 1 and called
two shots: sprites sitting low / bleeding off the slot, and art that was "okay, not blown
away." Both actioned.
- **Centering sweep (all sprites, not a spot-check).** The item generator was rebuilt so every
  16x16 sprite is authored inside a shared centred box with a >=1px margin. Before: nearly
  every sprite touched the bottom edge (B-margin 0, sitting low) and `restock_strap` filled the
  whole 16x16; `charge_crystal` was 6x10. After: every item sprite is centred, no edge touch
  (`java tools/AnalyzeSprites.java` prints the bounding box + per-edge margin + centre offset
  for each — use it to re-audit any future sprite). Verified in-game: varied items (tools,
  full blocks, potions, tall/tiny items) all sit centred in the grid, and the host-slot pack
  no longer hangs low.
- **Art lifted from procedural to hand-authored.** `tools/GenTextures.java` now authors each
  item sprite as a pixel-art char grid (a per-material value ramp + top-left light + dark
  outline) instead of noise-fill. The 5 packs are a real material ladder (twine → buckle →
  studs → steel plates → runes+gem). The four murky icons Alex named are redesigned: Quill &
  Ledger (legible book + quill), Charge Crystal (copper-wound faceted crystal, not a flame),
  Quick-Draw Straps (two buckled belts, not a red ✗), Soul Vial vs Flask Harness vs Waterskin
  now clearly distinct. Run `java tools/GenTextures.java` to regen; it also writes
  `tools/sprite_montage.png` (a flat-background lineup of every sprite for pixel inspection).
- **Bolder pin marker.** A pinned slot now wears a red ribbon fold + brass tack in the corner
  (`PackScreen.drawPinRibbon`), replacing the faint pin-head; verified in-game.
- **The GUI panel, tab and placed-block faces stayed procedural** (large tiled surfaces where
  grain reads well) — only the 16x16 item icons became hand-authored.

**2026-07-23 placeable pack — DONE & verified.** Packs are now placeable in the world and
automatable through block capabilities.
- **Block + block entity (`block/`).** Sneak-right-click a face to set a pack down; it renders
  as the pack (tier-tinted, facing the player) and breaking it returns the pack item with
  every field intact. The block entity holds the pack as ONE `ItemStack`, so place→break is a
  lossless move of that stack, never a re-serialisation — a gametest proves items + trinkets +
  layout + each store round-trip byte-for-byte, dupe-safe. No BlockItem (the pack item places
  it), no loot table (`getDrops` returns the stack).
- **Block capabilities.** The placed pack exposes standard item / fluid / energy block caps
  (each trinket-gated), so hoppers/pipes/cables interact with it; inserted items auto-route
  into the right compartment because sorting is virtual over the flat store. Every external
  write marks the block entity dirty.
- **Same GUI, generalised.** `PackMenu` binds to a carried slot OR the block entity (via a
  hidden synced host slot); the carried path is unchanged. Both verified in-game.
- **Forgework block-level charging — WORKS (gated).** Forgework Flux is its own block cap, not
  standard FE, so it isn't free — but the block entity let me register a gated 1:1 `FLOW_ENERGY`
  adapter (`ForgeworkFluxBridge.register`), so a Forgework cable charges a *placed* pack. Live
  proof: `runGameTestServer -Pforgework` lands 5,000 Flux = 5,000 FE.

**2026-07-23 finishing run — DONE & verified.** Forgework Flux bridge, the two remaining
trinkets, the guide, and the Feather cut all landed. Details:
- **Forgework Flux bridge (`compat/forgework/ForgeworkFluxBridge`).** A fitted Charge
  Crystal tops up any Forgework portable terminal you carry, 1 Flux = 1 FE (item-level,
  because Forgework Flux is a block cap + hardcoded item-Flux and the pack has no block
  form — see DECISIONS). Gated `ModList.isLoaded("forgework")`, one class imports
  `com.forgework.*`, never classloads without it. **Verified live:** 17/17 gametests green
  with the local Forgework jar in the runtime (`runGameTestServer -Pforgework`), the
  transfer test asserting exact 1:1 conservation.
- **Quick-Draw Straps — LIVE.** On `PlayerDestroyItemEvent`, a broken held tool is replaced
  from pack stock (dupe-safe; only replaces what the pack holds). `TrinketEffects`.
- **Quill & Ledger — LIVE.** Custom tabs go from pin-only to rule-matching when it's
  fitted: they evaluate stored rules plus a category rule derived from the tab's stamped
  icon. Gated in `SortEngine.toView(TabDef, ledger)`, threaded from `PackMenu` (both sides).
- **Outfitter's Handbook — LIVE & verified in-game.** `guide/HandbookItem` opens
  `client/OutfitterHandbookScreen` (leather/brass, five chapters); content in
  `guide/HandbookContent` interpolates real SSOT numbers. Autoshot screenshots inspected.
- **Feather Charm — CUT.** No encumbrance system, so no job. Removed from the SSOT + assets.

**Phase 2 — trinket framework, DONE & verified in-game.** Right-rail brass sockets
(count = tier), craftable fittings off a `TrinketType` SSOT table. Working: Lodestone
(magnet), Restock (hotbar top-up), Repair (slow mend), Bottomless (grows capacity, never
truncates), Compass Rose (opt-in void, the only void path — press O on a hovered item),
Quick-Draw (break-replace), Quill & Ledger (custom-tab rules). Effects run server-side per
`PlayerTickEvent` (or the break event), throttled/bounded. Preserving tier-upgrade recipe
(`packwork:pack_upgrade`) carries contents+trinkets+name up a tier so no craft eats a pack.

**Phase 3 — fluids + XP + energy stores, DONE & verified in-game.** Three of four stores,
each trinket-gated and stacked as a gauge on the right rail: Waterskin Rack (fluid tank,
`FluidHandler.ITEM`, glass gauge, click-with-cursor fill/drain via `FluidUtil`); Soul Vial
(XP via `PackXpStore`, green gauge, click siphon / shift pour, auto-mends Mending gear);
Charge Crystal (arcane charge via `PackEnergyStorage` implementing `IEnergyStorage`, cool
crystal-blue gauge, any FE source fills it, tops up powered tools in hand, + the gated Forgework bridge
above). All three capabilities are exposed **only when the fitting is present**. The 4th
store — Gas (Flask Harness + Mekanism chemical cap) — is **deferred** (needs the Mekanism
dep); leave the shipped stores as the template: component + (gated) capability + STORE
trinket + gauge + gametest.

**All four resource stores now ship** (fluids/XP/energy always; gas via the gated Mekanism
integration), and **Curios wear + JEI are in** (all soft deps — see the integrations section).
The placed-pack block-entity carries the block-level Forgework + Mekanism caps. Still open: a
dedicated Outfitter's Bench upgrade station (the preserving recipe covers the need for now), a
quest chapter, `CLAUDE.md`, and opening the pack GUI from the Curios slot. **README.md /
PUBLISHING.md / CHANGELOG.md exist** — keep their copy in step with code.

## The three optional integrations — DONE (gas / Curios / JEI), soft-dep verified

All three are strict SOFT deps: `compileOnly` the API in `build.gradle`, `ModList.isLoaded`-
gated, one class per mod under `compat/`. The mod builds, loads, and passes all 22 GameTests
with **none** present (verified). Runtime inclusion is opt-in per flag so the default stays
dependency-free: `./gradlew runClient -Pjei -Pcurios -Pmekanism` (any subset). Pinned
versions in `gradle.properties`; repos (blamejared, theillusivec4, modmaven) in `build.gradle`.

- **Gas → Mekanism (`compat/mekanism/MekanismChemicalStore`).** Flask Harness STORE trinket +
  dist-neutral `PackChemical` component (id + amount, so nothing always-loaded imports
  Mekanism) + an `IChemicalHandler` over it, on the item AND the placed block-entity, gated by
  the trinket + `ModList.isLoaded("mekanism")`. Cap token recreated with Mekanism's own
  `mekanism:chemical_handler` name (item=void, block=sided) since the api artifact ships the
  interface but not the token. Right-rail vapor gauge. Recipe gated by `neoforge:mod_loaded`.
  **Verified in `runGameTestServer -Pmekanism`:** cap present, tier capacity, real hydrogen
  resolved, 3000 mB in / 3000 out. (Live pipe-to-pack not separately staged — see DECISIONS.)
- **Curios → back slot (`compat/curios/CuriosCompat`).** Registers each pack as a curio;
  `data/curios/slots/back.json` + the `curios:back` item tag put it in the back slot; a worn
  pack's trinkets keep ticking via `TrinketEffects.applyWornPack`. **Verified in
  `runClient -Pcurios`** (player has the slot, pack fits + equips). Opening the GUI while worn
  is a v1 follow-up (the menu binds to an inventory slot or a block-entity, not a Curios slot).
- **JEI → info pages (`compat/jei/PackworkJeiPlugin`).** `@JeiPlugin` (self-gating via JEI's
  annotation discovery); info pages for every pack tier, every trinket, and the handbook.
  **Verified in `runClient -Pjei`.**

To add ANOTHER integration later, mirror this: `compileOnly` the API (+ a `-P<mod>` runtime
flag), an `optional` block in `neoforge.mods.toml`, and one gated `compat/<mod>/` class.

### Original status (Phase 0/1)

Git repo initialized. NeoForge 1.21.1 scaffold cloned from Highroller (Neo **21.1.235**,
Parchment 2024.11.17, JDK 21, package `com.sappersquad.packwork`, mod id `packwork`).

**Built & compiling & committed:**
- **Phase 0** — five tier pack items (Canvas→Runed, one `PackTier` enum), component-backed
  item store (`PackInventory` over `ItemContainerContents`), a working GUI, contents
  persist through save/load. Item-handler capability exposed on the stack for any mod's
  automation.
- **Phase 1 — the sorting flagship (the reason this mod exists), verified in-game.**
  Stamped-leather tab rail (7 auto-tabs + Loose + custom tabs), rules engine
  (tag/mod-id/name/predicate), manual pins that beat rules, Loose catch-all, Tidy Up,
  search, flatten, custom tabs (create / rename / dye / stamp icon / reorder / delete),
  auto-routing on insert, data-driven category tags. Keybind-to-open (B) + native use.
- **Phase 2 (partial)** — tier crafting recipes (raw materials, no content-eating upgrades).

**Verified in-game** via the dev screenshot harness (`-Pautoshot`, see below), with pixels
inspected: the leather/brass panel renders, items store and display, the Food tab shows
only food, Combat only weapons/armor, tab selection routes correctly, a new custom tab
appears on the rail. **Six GameTests green** (persistence round-trip, routing, pins, Tidy
Up, nesting-block, fresh-pack default).

**Not built yet:** the trinket framework + material-tier upgrade UI (rest of Phase 2), the
four resource stores (Phase 3), and progression/JEI/quest/guide + `README.md`/`PUBLISHING.md`
(Phase 4). Curios wear-slot compat is still pending.

### How to see the GUI without driving the window
The gradle dev-client window can't be driven by desktop-control tooling (it's a raw java
process, not a Start-menu app). So there's a dev-only harness: `DevAutoShot` (gated on
`-Dpackwork.autoshot` / `./gradlew.bat runClient -Pautoshot`) boots a throwaway creative
world, fills a pack across every tab, opens it, switches tabs, and writes screenshots to
`run/client/screenshots/packwork_*.png` — then read those PNGs. Delete
`run/client/saves/packwork_autoshot` before re-running so world creation doesn't collide.

### Where things live (source map)
- `pack/` — `PackItem`, `PackTier` (SSOT ladder), `PackInventory` (live component store),
  `PackMenu` (virtual-tab menu + all action handlers + the Tinker's Kit tool roll: a
  `TransientCraftingContainer` + `ResultContainer`, its `RollResultSlot` refilling from pack
  stock after each craft, and `emptyRollIntoPack` on roll-up/close), `PackViewSlot`
  (rebinding grid cell).
- `sort/` — `SortRule`, `PredicateKind`, `TabDef`, `PackLayout` (component), `AutoTabs`
  (SSOT category table; `Auto.gate` makes a compartment trinket-gated), `TabView`,
  `SortEngine` (routing; `tabsFor(layout, Set<TrinketType>)` is the real entry point),
  `PackSorting` (Tidy Up).
- `block/` — `PackContainerBlock` (placeable, facing, opens the GUI, drops the pack stack),
  `PackContainerBlockEntity` (holds the pack as one `ItemStack`; tier-only client sync).
  Registered in `reg/ModBlocks` + `reg/ModBlockEntities`; block caps in `PackworkCapabilities`.
- `trinket/` — `TrinketType` (SSOT table, 18 fittings), `TrinketItem`, `TrinketAccess`,
  `TrinketEffects` (per-tick effects + the event handlers: Quick-Draw's break refill, the
  Angler's `ItemFishedEvent` stow, the Herbalist's `BlockEvent.BreakEvent` replant). The
  conservation-critical helpers — `smeltOnce`, `feedFrom`, `stowCatch`, `takeSeedFor`,
  `pullReplacement` — are public and gametested.
- `compat/` — one gated class per mod, the ONLY class importing that mod: `forgework/`
  (`ForgeworkFluxBridge`), `mekanism/` (`MekanismChemicalStore`), `curios/` (`CuriosCompat`),
  `jei/` (`PackworkJeiPlugin`, self-gated by `@JeiPlugin`). Each reached only behind
  `ModList.isLoaded` (JEI via annotation discovery).
- `guide/` — `HandbookItem` (opens the guide), `HandbookContent` (dist-neutral chapter data,
  interpolates SSOT numbers). The screen itself is `client/OutfitterHandbookScreen`.
- `client/` — `PackScreen` (the rail + controls), `OutfitterHandbookScreen`,
  `HandbookClientHooks`, `PackClientActions`, `PackKeyMappings`, `ClientSetup`, `DevAutoShot`.
- `net/` — `PackAction`, `PackActionPayload`, `OpenPackPayload`; wired in `PackworkNetwork`.
- `reg/` — `ModItems` (packs + trinkets off their enums, plus the `HANDBOOK` item),
  `ModMenus`, `ModComponents`, `ModCreativeTabs`. Caps in `PackworkCapabilities`.
- `gametest/PackworkGameTests` — the headless proof of persistence + sorting.
- `tools/GenTextures.java` — procedural leather/brass GUI + pack sprites (Java only; run
  `java tools/GenTextures.java`). Data: `data/packwork/tags/item/sorting/*`, `recipe/*`.

## The hard aesthetic rule — adventurer, never futuristic

The anchor materials are **leather, brass, canvas, glass vials, twine, wax, and faint
runes.** Every mechanic below must be skinnable in that language. If a feature can only be
expressed as sci-fi, it's the wrong feature — reskin it or cut it.

**Banned aesthetics:** circuit boards, screens/holograms/HUD-glow, neon, "modules / chips
/ cells / cores", batteries drawn as batteries, wires/cables, sci-fi naming. Energy is an
*arcane charge in a copper-wound crystal*, not RF in a battery. Gas is *bottled vapors in
alchemist's flasks*, not a plasma tank.

## Design pillars

Every feature must serve one. If it serves none, say so and recommend cutting it.

1. **The pack sorts itself — sorting is the soul.** The tabbed, rule-driven organization
   is the reason this mod exists and beats Sophisticated. Build and polish it before any
   resource store. A Packwork that stores five resource types but sorts items badly is a
   failed Packwork.
2. **One object, many stores — but always gear.** Items, fluids, gas, energy, and XP live
   in one pack, each surfaced as a physical fitting (a waterskin rack, a flask harness, a
   charge crystal, a soul vial), never as a tank-and-cable UI.
3. **Standard capabilities first, hard dependencies never.** Items/fluids/energy use
   NeoForge's own capabilities so any mod's automation works against a placed pack. Every
   cross-mod bridge (Forgework, Mekanism, Curios, JEI) is gated behind `ModList.isLoaded`
   with exactly one class allowed to import that mod, never classloading without it.
4. **Adventurer progression — materials tier, trinkets upgrade.** Pack tiers are a
   material ladder; capabilities are earned by crafting and installing thematic trinkets.
   No tech tree, no power requirement to *use* the pack.
5. **The GUI is the product.** It must feel like opening a real pack: stamped leather
   tabs down the side, gauges and trinket slots on a rail, a stitched search bar. If it
   looks like a spreadsheet, it isn't done.
6. **Pause, never punish** (suite house rule). The pack never voids contents on failure.
   Void/overflow behavior is opt-in via a trinket (the Compass Rose / void filter), never
   default. Death behavior is a flagged open question — do not guess (see below).

## The container model

- **Tiers:** Canvas → Leather → Studded → Reinforced → Runed. Each step adds compartment
  rows, upgrade-trinket slots, and resource-store capacity. Runed is the "impossibly
  organized" magic tier, gated behind amethyst/echo-shard-flavored materials.
- **How it's used:** usable from the hand and from the inventory (keybind to open), and
  **placeable in the world** as a block-entity pack (Sophisticated-style) so hoppers/pipes
  can feed it. Wearable via **Curios** (back slot) when present — gated, with a native
  fallback so Curios is never required.
- **Persistence:** pack contents live in a **data component** on the stack (1.21 component
  system) — verify the right pattern against Sophisticated Backpacks' 1.21 source and the
  decompiled `ItemContainerContents` before committing; a naive giant-component approach
  can be heavy. Contents must survive relog, drop, and placement.

## The sorting system — the flagship

- **Tabs are compartments.** A vertical rail of **stamped leather tabs** on the left of the
  GUI is the category selector (the "dropdown" from the brief). Center shows the selected
  compartment's grid.
- **Auto-tabs (ship these):** Food (via **Pantrywork** tags), Tools & Utility, Combat,
  Blocks & Building, Ores & Valuables, Brewing & Alchemy, Nature & Farming, Loot & Misc.
  Driven by item tags so they cover modded items for free.
- **Custom tabs:** player creates one, names it, **stamps it with any item's icon**, dyes
  the leather tag a color, and drags to reorder.
- **The rules engine (the "everything in between"):** each tab holds an ordered list of
  match rules — by item tag, by mod id, by name substring, by predicate (is-food /
  is-tool / is-armor / is-block) — plus **manual pins that always win.** Tab order is
  priority; the first matching tab claims an item. A **"Loose"** catch-all holds anything
  unmatched. New items auto-route on insert; a **"Tidy Up"** button re-runs the whole sort.
- **Search & flatten:** a search bar filters across all tabs; a "flatten" toggle collapses
  everything into one grid.
- **Data-driven categories:** ship the auto-tab tag lists as datapack JSON so servers and
  modpacks can retune categories without a code change.

## The five stores, re-skinned

Each store is **unlocked by installing its trinket**, not always-on. Capacity scales with
pack tier × trinket tier.

- **Items** → the compartments above (native).
- **Fluids** → **Waterskin Rack** trinket: N fluid tanks shown as glass gauges;
  bucket/tank interaction; standard NeoForge `FluidHandler` capability.
- **Gas / chemicals** → **Alchemist's Flask Harness** trinket: chemical tanks ("bottled
  vapors"); **Mekanism** chemical capability via gated compat in `compat/mekanism/`. The
  trinket and its UI only appear when Mekanism is loaded.
- **Energy** → **Charge Crystal** trinket (a copper-wound coil-jar): FE store via NeoForge
  `IEnergyStorage`; charges held/equipped tools from the pack. **Forgework** Flux bridges
  1:1 with FE (gated). Framed as arcane charge — never "battery/RF/cell".
- **XP** → **Soul Vial** trinket: stores XP points; siphon/pour keybinds; optional
  auto-mend of equipped gear from the reservoir. Flavor lineage: Bottle o' Enchanting.

## Upgrade trinkets

Adventurer-flavored fittings installed into tiered trinket slots:

All 18 are LIVE. One `TrinketType` entry + assets each; nothing else to edit.

- **Lodestone Charm** — magnet, pulls nearby items into the pack.
- **Quill & Ledger** — custom tabs match by rule, not just pins (files by the stamped icon's kind).
- **Compass Rose** — void filter (opt-in trash for chosen items). The only void path.
- **Tinker's Kit** — a leather tool roll unrolls across the pack's lower rows: a 3×3 bench that
  refills itself from pack stock after each craft.
- **Field Furnace** — cooks raw ore + raw food from pack contents, on pack fuel, at furnace rates.
- **Provisioner's Pouch** — eats the plainest thing in the pack before hunger bites.
- **Cartographer's Sleeve** — opens a gated **Charts & Bearings** compartment.
- **Angler's Creel** — opens a gated **The Catch** compartment; your catch lands in the pack.
- **Torchbearer's Loop** — sets a torch from pack stock when you're standing in the dark.
- **Herbalist's Bundle** — replants a grown crop from your own seed stock.
- **Repair Kit** — mends equipped gear.
- **Restock Strap** — auto-refills the hotbar from pack stock.
- **Bottomless Lining** — extra capacity, never truncating.
- **Quick-Draw Straps** — a broken held tool is replaced from pack stock.
- Plus the four store trinkets: Waterskin Rack, Soul Vial, Charge Crystal, Flask Harness.

## Technical architecture (NeoForge 1.21.1)

- **Contents** in a data component (verify pattern vs. Sophisticated + decompiled sources).
- **Menu/Screen:** custom `AbstractContainerScreen` with the tabbed layout; server-
  authoritative slot moves; state synced through the menu. **Remember:**
  `renderBg()` only fires from `renderBackground()` — an empty override = invisible screen.
- **Capabilities** exposed on the stack (item capability providers) and on the placed
  block-entity so other mods' pipes interact.
- **Single source of truth:** one registry/table drives compartments, auto-tab tag lists,
  and trinket definitions. Adding an auto-tab or a trinket should be one table entry plus
  assets, never edits across four files.

## Build & Run

Clone **Highroller's** toolchain (the suite's current standard): NeoForge **21.1.x**,
NeoGradle, Parchment, JDK 21. Package `com.sappersquad.packwork`, mod id `packwork`.

```powershell
cd C:\Users\alexh\Documents\Packwork
./gradlew.bat compileJava    # fast check - do this constantly
./gradlew.bat runData        # datagen; cheapest way to catch codec/recipe errors
./gradlew.bat build          # full jar
./gradlew.bat runClient      # dev client; ready at "Sound engine started"
```

Run `runClient` as a background task; watch for `Sound engine started` (ready) or
`Crash report` / `LoadingFailedException` (dead). Logs in `run/logs/latest.log`.

## Roadmap — each phase is shippable

- **Phase 0 — prove the loop. DONE.** Scaffold; pack items; component store; a real GUI;
  contents persist (gametest `contentsSurviveSaveLoad`).
- **Phase 1 — the sorting system (the headline). DONE & verified in-game.** Tabs, auto-tabs
  via tags, custom tabs, rules engine, manual pins, Tidy Up, search/flatten, data-driven
  categories, keybind-open.
- **Phase 2 — tiers + trinket framework. STARTED.** Tier recipes done. **Next:** (a) the
  trinket framework — a small `trinket/` package with a `Trinket` registry table (SSOT like
  `AutoTabs`), N trinket slots per tier (`PackTier.trinketSlots()` already returns the
  count), a trinket rail on the RIGHT of the GUI, and the "easy" trinkets (Feather Charm =
  no slowdown, Bottomless Lining = +capacity, Compass Rose = opt-in void, Lodestone Charm =
  magnet, Restock Strap, Repair Kit, Quick-Draw). (b) Curios back-slot compat in
  `compat/curios/` gated on `ModList.isLoaded("curios")`, native fallback. (c) A
  contents-preserving tier-upgrade recipe (or fold into the Phase 4 Outfitter's Bench).
- **Phase 3 — the four resource stores.** fluids → XP → energy → gas, each behind its
  trinket (Waterskin Rack / Soul Vial / Charge Crystal / Flask Harness), surfaced as gauges
  on the right rail. Standard NeoForge `FluidHandler`/`IEnergyStorage` caps; gated Forgework
  (Flux 1:1 FE) and Mekanism (chemicals) bridges, one class per mod under `compat/`.
- **Phase 4 — progression & release.** The in-house **Outfitter's Handbook** guide is
  DONE (`guide/` + `client/OutfitterHandbookScreen`, cribbed from PhytoForge's Lab Manual).
  `README.md` + `PUBLISHING.md` + `CHANGELOG.md` exist and are current. Remaining: JEI (see
  deferred integrations), a quest chapter, `CLAUDE.md`, store art, and the Outfitter's Bench
  block (also the home for a future block-level Forgework bridge).

### Fastest way to resume
`./gradlew.bat compileJava` (constant), `runGameTestServer` (logic — add `-Pforgework` to
exercise the Flux bridge against the local Forgework jar), `runClient -Pautoshot` (see the
GUI + the Handbook; screenshots land in `run/client/screenshots/`). The natural next build
is one of the deferred integrations above — each is a single gated `compat/<mod>/` class
plus (for a store) the Waterskin-Rack template. Note `runClient` does not self-exit after
autoshot; kill it once the screenshots are written.

## Cross-mod interop (all gated, never hard deps)

- **Pantrywork** food tags → the Food auto-tab.
- **Forgework** Flux ↔ energy store, 1:1 with FE.
- **Mekanism** chemicals → the gas store.
- **Curios** → wearing the pack (native fallback if absent).
- **JEI** → optional recipe/usage integration.

## Open questions for Alex — flag, don't guess

1. **Wear slot:** Curios back slot, inventory-only, or both?
2. **Death behavior:** does the pack (and contents) keep on death, drop, or offer a
   soulbound trinket? This is a balance call, not a default to assume.
3. **Nesting:** allow pack-in-pack, or block it? Nesting invites dupe bugs and lag.
4. **Quest home:** a Coinkeep Ledger chapter (like Highroller) or an FTB/standalone book?
