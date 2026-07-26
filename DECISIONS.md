# Packwork — Decisions

Judgment calls already made, with reasons. Reopen only with new evidence, and say so.

- **Name: Packwork.** Chosen by Alex 2026-07-23 (theme: "Explorer's Pack"), from a slate
  that included Trailkeep, Bindle, and Haversack. Renameable like other suite mods, but
  this is the pick — don't churn on it without a reason. Matches the Forgework/Pantrywork
  `-work` family.

- **Theme: the humble adventurer's pack** — leather, brass, canvas, glass, faint runes.
  Picked over "Arcane Bag of Holding" and "Quartermaster's Case." The GUI opens like a
  real pack.

- **Non-futuristic is a hard constraint, not a preference.** No circuits, screens, neon,
  "modules/chips/cells", or sci-fi naming. Energy = arcane charge in a copper-wound
  crystal; gas = bottled vapors in flasks; XP = soul vials. See the banned list in
  `PROJECT_HANDOFF.md`.

- **Sorting is the flagship and ships first.** The tabbed rule-driven organizer is the
  reason to build this over using Sophisticated Backpacks. Resource stores come after it's
  genuinely good, not before.

- **Standard NeoForge capabilities; zero hard dependencies.** Every cross-mod bridge is
  gated behind `ModList.isLoaded`, one class per mod, never classloading without it.
  Pantrywork/Forgework/Mekanism/Curios/JEI are all optional.

- **Resource stores are trinket-gated, not always-on.** A fresh pack carries items only;
  fluids/gas/energy/XP each require crafting and installing their trinket. Keeps early
  packs simple and makes capability a progression reward.

- **Pause, never punish.** The pack never voids contents on failure. Void/overflow is
  opt-in via the Compass Rose trinket only.

- **No encumbrance.** Packwork has no pack-weight / movement penalty (Alex, 2026-07-23).
  A heavy pack never slows you down, so there is nothing for a "weightless" fitting to
  negate — see the Feather Charm cut below.

## Deliberately deferred to Alex (do not decide unilaterally)

Death behavior, wear slot (Curios vs. inventory vs. both), pack nesting, and the quest
home are open questions in `PROJECT_HANDOFF.md`. These are balance/feel calls — surface
options, don't pick.

## The four open questions — Alex's locked defaults (2026-07-23, reversible)

Alex chose "build now" over settling these; implemented cleanly and kept isolated so any
one can be flipped later without unpicking the rest:

1. **On death: pack drops as a single item retaining all contents** (Sophisticated-style).
   No soulbound in v1. (Currently the pack is just an item, so this is vanilla drop
   behavior — nothing special coded yet; revisit if a keep/soulbound trinket is added.)
2. **Wear slot: both.** Native inventory use + a keybind to open (default **B**, via
   `OpenPackPayload`, server finds the first pack carried) are DONE. Curios back-slot
   support is gated for Phase 2 (`ModList.isLoaded("curios")`, native fallback, never
   required).
3. **Nesting: blocked in v1.** `PackInventory.isItemValid` refuses any `PackItem`; a
   gametest pins it.
4. **Guide: in-house "Outfitter's Handbook"** modeled on PhytoForge's Lab Manual (client
   screen + content class). Not built yet — Phase 4.

## Architecture calls made while building Phase 0/1 (reopen with evidence)

- **Tabs are virtual views over one flat backing store, not physical partitions.** One
  `ItemContainerContents` component holds every item; a tab is a saved filter, and
  `PackViewSlot`s rebind to backing indices per active tab/search/page. Sorting never
  moves an item, so it can never lose or dupe one. This is the core reason the pack "sorts
  itself" without churn.
- **Contents resolved live from the bound slot, never captured.** `PackInventory` reads
  its stack through a supplier. A captured reference can be empty (client builds the menu a
  tick before its inventory slot syncs) and component writes to an empty stack are silent
  no-ops — which showed as an empty grid. See the commit "resolve the pack live".
- **Client rebuilds its view every tick** (`PackScreen.containerTick`) from the synced
  component, so tab filtering reflects live contents and the grid re-sorts as items move.
- **One serverbound action channel** (`PackActionPayload`, flat `action/arg/s1/s2`) drives
  every GUI verb; the client applies optimistically and sends, the server is the authority.
- **Auto-tab priority is tuned, order = priority:** Food, Combat, Tools, Ores, Brewing,
  Nature, Blocks, then Loose. Combat precedes Tools (NeoForge tags swords `c:tools`); the
  broad `IS_BLOCK` Blocks tab sits last so Ores/Nature/Brewing claim their blocks first.
  Two gametests lock this.
- **Category rules live in code (`AutoTabs`, single source of truth); tag *membership* is
  datapack JSON** (`data/packwork/tags/item/sorting/*`) with optional modded includes
  (`#c:foods`, `#pantrywork:foods`, `required:false`) so packs retune without code and
  nothing breaks when a mod is absent.
- **Tier recipes craft from raw materials only (no cross-tier consumption).** Upgrading a
  filled pack to the next tier now goes through the `packwork:pack_upgrade` custom recipe,
  which copies contents+trinkets+name onto the new tier, so no craft ever eats a full pack.

## Phase 2/3 architecture calls (reopen with evidence)

- **A trinket is just its item sitting in a socket** — no separate "installed" flag. The
  `pack_trinkets` component IS the install state; sockets refuse duplicates and non-fittings.
- **Bottomless Lining never truncates.** Capacity is live (`TrinketAccess.capacity`); the
  component preserves slots past the current size, so pulling the Lining hides the extra
  items rather than voiding them (pause, never punish).
- **Compass Rose is the only void path** and is opt-in per item (press O on a hovered item;
  stored in `PackLayout.voidList`). Magnet + Compass Rose deliberately doubles as a trash
  collector.
## 2026-07-23 — finishing run (Alex's two calls + the unblocked work)

- **Feather Charm cut — no encumbrance system.** With no pack-weight penalty (above), a
  "weightless" fitting has no job, so it was removed rather than left as a dead craftable:
  gone from the `TrinketType` SSOT (so its item, creative-tab entry, and lang key drop
  automatically), plus its recipe / model / texture deleted and the socket gametest moved
  onto another fitting. Nothing references it. Reversible if an encumbrance mechanic is
  ever added.

- **Forgework Flux bridge is item-level, one direction, 1:1.** A fitted Charge Crystal
  tops up any Forgework portable terminal the player carries, 1 Flux = 1 FE — the same way
  the crystal already tops up FE tools in hand. Why not block-level like PhytoForge's
  bridge: Forgework's Flux is a *block* capability and its item-Flux lives in a bespoke
  `custom_data` tag reachable only through `PortableEnderTerminalItem.charge/getFlux`, and
  Packwork's pack has **no placed block-entity form**, so there is no block to carry a
  `FLOW_ENERGY` cap and no way to pull Flux back out of an item. The item-level hand-off is
  the honest maximum for both mods as they stand. Full block interop (a Forgework cable
  charging a *placed* pack) is **flagged** — it needs a pack block-entity, still-open scope.
  Gated `ModList.isLoaded("forgework")`, one class (`compat/forgework/ForgeworkFluxBridge`)
  imports `com.forgework.*`, never classloads without the mod. Verified live: 17/17
  gametests green with the local Forgework jar loaded in-process, the transfer test
  asserting exactly 1:1 conservation (`runGameTestServer -Pforgework`).

- **Quick-Draw Straps react to a tool actually breaking.** On `PlayerDestroyItemEvent`
  (server-side, `getHand() != null`), if a carried pack has the strap fitted, it pulls an
  identical item from the pack into that hand. Chosen over a per-tick swapper because the
  break event is precise: setting an item aside or swapping hotbar slots never triggers a
  refill, so it can't surprise you. It only hands back what the pack actually holds, so it
  can never dupe (a gametest pins the conservation core).

- **Quill & Ledger v1 files custom tabs by their stamped icon.** Without a rule-editor UI
  (still future), the ledger's observable job is: gate whether custom tabs match by RULE at
  all. Without it, a custom tab is pin-only (its stored rules are ignored). With it fitted,
  a custom tab evaluates its stored rules PLUS a category rule derived from the item it's
  stamped with (food / potion / weapon / armour / tool / block) — "stamp a tab with a
  pickaxe and it gathers your tools". Storage-free (derived at routing time), dupe-free
  (routing never moves items), removable (pull it and custom tabs fall back to pin-only,
  items just re-route). Pins still beat rules. The full free-form multi-rule EDITOR remains
  a future enhancement; this is the no-UI proxy that makes the fitting real in v1. **Flag
  for Alex:** if the stamp-derives-a-category behaviour feels too magic in play, the gate is
  isolated in `SortEngine.toView(TabDef, ledger)` and easy to dial back to "stored rules
  only".

- **The Outfitter's Handbook is in-house, zero-dep, modelled on PhytoForge's Lab Manual.**
  A guide item (`guide/HandbookItem`) opens a leather-and-brass `Screen`
  (`client/OutfitterHandbookScreen`) whose content (`guide/HandbookContent`, dist-neutral)
  interpolates real numbers from the `PackTier` / store-capacity SSOTs. Five chapters: The
  Pack, Sorting, Trinkets, Tiers & Upgrades, The Stores. Verified in-game via the autoshot
  harness with pixels inspected.

## 2026-07-23 — hero packs at 32×32; Charge Crystal recoloured (reopen with evidence)

- **The 5 pack item sprites are 32×32; everything else stays 16×16.** The packs are the icons
  the player stares at constantly (hand + inventory), so they earn the higher resolution and a
  form-shading renderer (`heroPack` in `tools/GenTextures.java`: rounded-form dome lighting, AO
  in seams, rim light, stitching, a specular buckle). `item/generated` renders a 32×32 sprite
  crisply at any GUI scale. Trinkets/handbook stay 16×16 (they read fine and cost less to
  author). The placed-block faces went 32×32 too so a set-down pack matches the held one.
- **Per-tier trim on the *block* is deferred.** *(Superseded 2026-07-24 — done via a `tier`
  blockstate property + per-tier models/textures; see "art pass 5" below.)*
- **Charge Crystal is cool blue, not amber (Alex, 2026-07-23).** Amber read as a candle flame;
  a cool faceted crystal wound in dark copper is unambiguous. **Update (art pass 4): the energy
  gauge on the rail is now the same cool crystal-blue** (`0xFF3EA9C4` fill on `0xFF15323B`
  glass in `PackScreen.drawEnergyGauge`) — Alex confirmed the icon and gauge should match. It
  stays distinct from the fluid gauge (which fills with the actual fluid's animated water-blue
  texture, deeper and more saturated than the flat crystal-blue).

## 2026-07-23 — art pass 4: the pack silhouette is a BACKPACK, not a pouch (reopen with evidence)

- **The hero form went from a rounded dome to a boxy backpack.** Art pass 3's `heroPack` lit the
  body as a single ellipse (`dome()`), so the packs read as pouches/orbs — the runed one as a
  magic orb. `heroPack` now builds the form from a **superellipse cushion** (`superForm`, power
  ~3.2–3.7): a boxy, gently tapered body with a flat bottom; a wide flap draped over the top
  third with a hard stitched hem + an AO shadow beneath it (the single strongest "this is a
  backpack" cue); a central closure strap through a brass buckle straddling the hem; a top
  grab-handle loop; and side pockets. **Every pass-3 fidelity gain is kept** (32×32 form-shading,
  AO, rim light, specular buckle glint, per-tier ladder) — this was a silhouette change, not a
  shading regression. The runed pack's glyphs now sit on a clearly pack-shaped body so it can't
  read as an orb.
- **Verified as pixels at small size, not just large.** `tools/GenTextures.java` writes
  `tools/pack_small_preview.png` — the five packs box-downscaled to 16px and 12px on a grey slot
  strip — so "does it read as a backpack in the hotbar?" is answerable offline without a client;
  the live hotbar/inventory/in-hand shots confirm it.
- **Per-tier block trim was deferred here, then done in art pass 5 (below).**

## 2026-07-24 — art pass 5: per-tier placed-block trim (reopen with evidence)

- **Rendering approach: a `tier` `EnumProperty<PackTier>` blockstate, NOT a BlockEntityRenderer.**
  The placed pack's geometry is static, so the cheapest, most standard way to show per-tier trim
  is to let the vanilla model system resolve it: a 5-value `tier` property picks one of five child
  models (each swapping textures over a shared `pack_shape` parent), across a 20-variant blockstate
  (facing × tier). A BER would mean custom render code for no benefit. The tier is set at placement
  from the pack item (`getStateForPlacement` reads `ctx.getItemInHand()`), and `setPackStack`
  re-sets it if the stored pack's tier ever differs (covers a bare-placement-then-stack test path
  and any whole-pack swap).
- **Round-trip safety held: the blockstate tier is render-only.** Drops + middle-click still read
  the block entity's stored stack, never the blockstate, so break returns the exact right-tier
  pack and the place↔break round-trip stays byte-for-byte lossless. A gametest
  (`placedTierDrivesBlockstateAndDrop`) pins: the render tier follows the pack, the drop is the
  right tier, and a swap retracks — 24 tests green.
- **Colour is baked, tint handler removed.** Each tier's leather/front textures carry their own
  colour + trim (from the shared `TIER_RAMP` SSOT in `tools/GenTextures.java`), so the old block
  colour handler + neutral `pack_block.png` are gone. The 3D brass **straps** stay a shared brass
  texture across every tier (on-brand leather-and-brass), but the **buckle** is now a separate
  `#buckle` texture slot in `pack_shape` that defaults to the shared brass — so leather/studded/
  reinforced/runed are untouched — while the **canvas** child model repoints `#buckle` to a new
  `pack_block_twine` texture. *(Superseded 2026-07-24, Alex's call: the earlier "the canvas item's
  twine buckle is a small, accepted divergence on the block" no longer holds — a set-down canvas
  pack now shows a twine buckle to match its item. Straps on the block remain brass for all tiers;
  making the canvas straps twine too was not requested and is a trivial follow-up if wanted.)*
- **Runed glow = block light emission + bright glyphs, not model emissive.** `lightLevel(state ->
  tier==RUNED ? 8 : 0)` makes a set-down Runed pack actually glow in the world, and the glyphs are
  high-contrast; this was chosen over an unverified per-face model-emissive flag and over
  duplicating geometry for an overlay quad. Verified in-game (the Runed block is visibly brighter
  than its neighbours).

## 2026-07-25 — rail clicks, cursor conservation, and who's authoritative (reopen with evidence)

- **The rails count as INSIDE the GUI.** `PackScreen` overrides `hasClickedOutside(...)` to return
  false over the left tab rail and the right fittings rail (sockets + the gauge stack). Both rails
  are drawn beyond `imageWidth`, which is precisely the region vanilla reads as "drop what you're
  carrying". Consuming the press in `mouseClicked` was never enough: the drop fires on **release**
  (`AbstractContainerScreen.mouseReleased` → `slotClicked(null, -999, PICKUP)` →
  `AbstractContainerMenu.doClick` → `player.drop(getCarried(), true)`), and `PackScreen` doesn't
  override `mouseReleased`. Teaching `hasClickedOutside` about the rails kills it once, at the
  source, for every rail widget — gauges, tabs, and anything added to the rails later. (The
  trinket *sockets* were already safe: NeoForge patches `flag = false` whenever a real slot is
  under the cursor.) Chosen over overriding `mouseReleased`, which would have to re-derive the
  same region anyway and would fight vanilla's drag/quick-craft bookkeeping.
- **The fluid gauge spends exactly one container per click.** `FluidUtil.tryEmptyContainer` /
  `tryFillContainer` act on `container.copyWithCount(1)` and return ONE resulting item, so the old
  `setCarried(result.getResult())` replaced an entire carried stack with a single item. The menu
  now shrinks the cursor by one and hands the result back in priority order: merge onto the cursor
  → the player's pockets → the pack → (last resort) the floor. Written out rather than delegating
  to `FluidUtil.tryEmptyContainerAndStow`, because that helper drops at the player's feet the
  moment the inventory is full, and the pack should catch it first — pause, never punish.
- **Cursor/XP actions are server-authoritative; layout verbs stay optimistic.** `PackAction`
  now answers `serverAuthoritative()` (FLUID_INTERACT, XP_SIPHON, XP_POUR) and
  `PackClientActions.send` skips the local apply for those, with a second guard inside `PackMenu`
  (`isClient()`) so nothing can slip through another caller. Applying an item/XP move on both
  sides either double-applies or desyncs until the next sync; tabs, search, pins and paging are
  pure view state, so they keep their instant local apply.

## 2026-07-23 — art: hand-authored item sprites, procedural surfaces (reopen with evidence)

- **Item icons are hand-authored pixel art; big tiled surfaces stay procedural.** After the
  art-1 playtest, the 16x16 ITEM sprites (packs, trinkets, handbook) were moved to explicit
  char-grid pixel art in `tools/GenTextures.java` — every pixel placed by hand with a
  per-material value ramp, a top-left light source and a dark outline. The GUI panel, the tab,
  and the placed-block faces stayed procedural (noise-grained fills) because they're large
  tiled surfaces where a hand grid buys nothing. Rationale: procedural noise-fill read as
  "generated" on small icons; the ceiling for "wow" on 16x16 is deliberate pixel work.
- **Every item sprite shares one centred box with a >=1px margin.** The overflow/low-sitting
  bug was systemic (most sprites anchored to the bottom edge, `restock_strap` filled the whole
  tile). The fix is a discipline, not a one-off nudge: sprites are drawn inside a consistent
  centred bounding box and `tools/AnalyzeSprites.java` audits box + margins + centre offset for
  every sprite. Re-run it after touching any icon; block FACE tiles are allowed to fill 16x16.
- **Pin marker is a red ribbon + brass tack**, not a subtle corner dot — Alex flagged the old
  one as too easy to miss. It clips only the slot's corner and rides above the item (z=300).

## 2026-07-23 — the placeable pack block

- **A placed pack IS its item stack, held on the block entity.** The block entity stores one
  `ItemStack` - the pack, with all its components (layout, flat item store, trinkets,
  fluid/XP/energy). Placing moves that stack from hand to block entity; breaking drops it
  straight back (via `getDrops` reading the `BLOCK_ENTITY` loot param). Nothing is
  re-serialised into a bespoke block format, so the round-trip cannot drop or dupe a field -
  the drop is literally the same stack. A gametest asserts every field survives place→break
  byte-for-byte, and that the whole thing is dupe-safe. This was the single hard requirement.
- **No separate BlockItem — the pack item places the block.** Sneak-right-click a block face
  places it (non-sneak still opens the GUI); the block has no BlockItem and no loot table.
  So a pack in the hand and a pack in the world are the same object, one tier ladder.
- **Placement is trinket-gated capabilities on the block entity.** The placed pack exposes
  NeoForge's standard item / fluid / energy block capabilities (each gated by its trinket),
  so hoppers, pipes, and cables interact with it. Because sorting is virtual over one flat
  store, an item a hopper pushes in just auto-routes into the right compartment. Every
  external write marks the block entity dirty so it persists.
- **The GUI generalised, not forked.** `PackMenu` now resolves its live pack stack from
  either a player-inventory slot (carried) or the block entity (placed). The placed case adds
  one hidden, inactive "host" slot so the block entity's stack - with all components - syncs
  to the viewing client through vanilla slot-sync, exactly as a carried pack rides its
  inventory slot. Slot counts stay identical client/server (the open packet carries a
  block-vs-slot flag + pos + tier). The carried path is unchanged.
- **Forgework block-level charging works — but NOT "for free."** Forgework's Flux is its OWN
  block capability (`FLOW_ENERGY`), not NeoForge's standard FE, so a Forgework cable does not
  touch the pack's standard energy cap. The block entity made the real bridge possible: the
  gated `ForgeworkFluxBridge.register` exposes a 1:1 `IFlowEnergyStorage` adapter over the
  pack's FE store on the block entity (like PhytoForge's bridge), so a Forgework cable/battery
  charges a *placed* pack directly. Verified live in a combined runtime
  (`runGameTestServer -Pforgework`): receiving 5,000 Flux lands 5,000 FE in the reservoir.
  Standard-FE mods (Mekanism-style cables) charge it through the standard cap with no compat.
- **Render: one block, tier-tinted, faces the player.** A squat leather box with a brass
  buckle + straps, `FACING` by placement. Tier colour comes from a block colour handler
  reading the block entity's tier (synced light — tier only, never the 256-slot contents),
  multiplied over a light-leather base texture. Verified in-game with pixels inspected.

## 2026-07-23 — the three deferred integrations lit up (gas / Curios / JEI)

All three stay strict SOFT deps: `compileOnly` the API + `ModList.isLoaded`-gated + exactly one
class importing each mod, never classloaded without it. Verified: the mod builds, loads, and
passes all 22 GameTests with NONE of the three present (the default runtime carries no interop
dep; runtime inclusion is opt-in per gradle flag). Maven wiring lives in `build.gradle` +
`gradle.properties`; the repos (blamejared, theillusivec4, modmaven) resolve in this env.

- **Maven coordinates (pinned, verified to resolve):** JEI `mezz.jei:jei-1.21.1-neoforge-api`
  + `-neoforge` `19.21.1.312` (maven.blamejared.com); Curios
  `top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1:api` (+ full for runtime)
  (maven.theillusivec4.top / blamejared); Mekanism `mekanism:Mekanism:1.21.1-10.7.19.85:api`
  (+ full) (modmaven.dev). Combined dev test: `-Pjei` / `-Pcurios` / `-Pmekanism` add the full
  mod to the runtime.

- **GAS = Alchemist's Flask Harness (Mekanism). LIVE-verified in a combined headless runtime.**
  A STORE trinket + a dist-neutral `PackChemical` component (a chemical id string + an amount,
  so `ModComponents` never imports Mekanism) + `compat/mekanism/MekanismChemicalStore` (the one
  class touching `mekanism.*`) implementing `IChemicalHandler` over the component, exposed on the
  pack item AND the placed block-entity, gated by the trinket + `ModList.isLoaded("mekanism")`.
  The capability TOKEN is recreated with Mekanism's own `mekanism:chemical_handler` name and the
  standard MultiTypeCapability shape (item = void, block = sided Direction) - the api-only
  artifact ships `IChemicalHandler` but NOT the token, so recreating the interned token is the
  correct way to interop. **Verified:** `runGameTestServer -Pmekanism` loads Mekanism and the
  gated test exposes the cap (1 tank, tier capacity), resolves the real `mekanism:hydrogen`
  chemical, and inserts 3000 mB / extracts exactly 3000 - 1:1, no loss. A violet "bottled
  vapors" gauge shows on the right rail (dist-neutral render, only when Mekanism is loaded).
  The recipe carries a `neoforge:mod_loaded` condition so the Flask Harness is only craftable
  with Mekanism (no dead craftable). NOTE: interop with Mekanism's PIPES relies on the token
  match, which is the standard shape; the handler + registration + chemical resolution are
  proven in-runtime, a live pipe-to-pack transfer was not separately staged.

- **CURIOS = the back wear slot. LIVE-verified.** `compat/curios/CuriosCompat` (the one class
  touching `top.theillusivec4.curios.*`) registers every pack tier as a curio during common
  setup (gated); a `data/curios/slots/back.json` + a `curios:back` item tag assign the packs to
  the back slot. A worn pack's trinkets keep running - `curioTick` funnels into the new
  `TrinketEffects.applyWornPack`, so magnet/restock/repair/soul-vial/charge work worn exactly as
  pocketed. Native inventory-use + the B keybind stay the fallback when Curios is absent.
  **Verified** in `runClient -Pcurios`: the player has a back slot, the pack is assigned to it,
  and it equips (logged). Opening the GUI from the worn slot is NOT wired in v1 (open it from
  the inventory) - a flagged follow-up, since the menu binds to an inventory slot or a
  block-entity, not a Curios slot.

- **JEI = recipe/usage lookup. LIVE.** `compat/jei/PackworkJeiPlugin` (the one class touching
  `mezz.jei.*`) is a `@JeiPlugin` - JEI discovers it by annotation and loads it only when JEI is
  present, so it self-gates (no ModList check needed). Adds an in-JEI info page for every pack
  tier (with its slot/socket counts, from the SSOT), every trinket (reusing its tooltip blurb),
  and the handbook. Verified in `runClient -Pjei`.

## Superseded — the three "inert trinkets" note

- **Feather / Quick-Draw / Quill & Ledger** were shipped-but-inert in Phase 2. As of the
  2026-07-23 finishing run: Feather is **cut** (no encumbrance), and Quick-Draw and Quill &
  Ledger are **live** (see above). No inert craftables remain.
- **The open packet carries the pack tier** so the client builds the same trinket-socket
  count as the server before its inventory slot syncs — a mismatch overran the container
  packet and dropped the player. Learned the hard way (two crashes).
- **Fluids = one tank per pack, tier-scaled capacity, gated capability.** A single tank (not
  a multi-tank rack) for v1: simpler, and the glass gauge reads clearly. The
  `FluidHandler.ITEM` cap returns null without a Waterskin so a bare pack is inert to fluid
  automation. Fill/drain is a click on the gauge with the cursor item (`FluidUtil`).
- **Curios compat deferred, not cut.** It needs the Curios API as a `compileOnly` dependency
  (+ its maven repo) which isn't in the local cache; native inventory-use + the B keybind
  already satisfy "wear it," so this is an enhancement, gated `ModList.isLoaded("curios")`
  when added.
