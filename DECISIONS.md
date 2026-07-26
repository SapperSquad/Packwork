# Packwork — Decisions

Judgment calls already made, with reasons. Reopen only with new evidence, and say so.

- **Name: Packwork.** Chosen by SapperSquad 2026-07-23 (theme: "Explorer's Pack"), from a slate
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

- **No encumbrance.** Packwork has no pack-weight / movement penalty (SapperSquad, 2026-07-23).
  A heavy pack never slows you down, so there is nothing for a "weightless" fitting to
  negate — see the Feather Charm cut below.

## Deliberately deferred to SapperSquad (do not decide unilaterally)

Death behavior, wear slot (Curios vs. inventory vs. both), pack nesting, and the quest
home are open questions in `PROJECT_HANDOFF.md`. These are balance/feel calls — surface
options, don't pick.

## The four open questions — SapperSquad's locked defaults (2026-07-23, reversible)

SapperSquad chose "build now" over settling these; implemented cleanly and kept isolated so any
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
- **Tier recipes craft from raw materials only (no cross-tier consumption).** *(SUPERSEDED
  2026-07-25 by SapperSquad's explicit call - see "the recipe chain" below. Every tier above Canvas
  now REQUIRES the previous tier's pack, and the preserving `packwork:pack_upgrade` craft IS
  the visible recipe; the raw-material recipes for tiers 2+ are deleted.)*

## Phase 2/3 architecture calls (reopen with evidence)

- **A trinket is just its item sitting in a socket** — no separate "installed" flag. The
  `pack_trinkets` component IS the install state; sockets refuse duplicates and non-fittings.
- **Bottomless Lining never truncates.** Capacity is live (`TrinketAccess.capacity`); the
  component preserves slots past the current size, so pulling the Lining hides the extra
  items rather than voiding them (pause, never punish).
- **Compass Rose is the only void path** and is opt-in per item (press O on a hovered item;
  stored in `PackLayout.voidList`). Magnet + Compass Rose deliberately doubles as a trash
  collector.
## 2026-07-23 — finishing run (SapperSquad's two calls + the unblocked work)

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

- **Quill & Ledger v1 files custom tabs by their stamped icon.** *(SUPERSEDED 2026-07-26 —
  SapperSquad played the stamp-gate proxy and couldn't tell what the fitting did, which was the
  verdict on the flag this entry carried. New model in the 2026-07-26 section: the stamp is
  the always-on baseline for every custom tab, and the ledger's legible job is the RULE
  EDITOR.)* Original call: without a rule-editor UI
  (still future), the ledger's observable job is: gate whether custom tabs match by RULE at
  all. Without it, a custom tab is pin-only (its stored rules are ignored). With it fitted,
  a custom tab evaluates its stored rules PLUS a category rule derived from the item it's
  stamped with (food / potion / weapon / armour / tool / block) — "stamp a tab with a
  pickaxe and it gathers your tools". Storage-free (derived at routing time), dupe-free
  (routing never moves items), removable (pull it and custom tabs fall back to pin-only,
  items just re-route). Pins still beat rules. The full free-form multi-rule EDITOR remains
  a future enhancement; this is the no-UI proxy that makes the fitting real in v1. **Flag
  for SapperSquad:** if the stamp-derives-a-category behaviour feels too magic in play, the gate is
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
- **Charge Crystal is cool blue, not amber (SapperSquad, 2026-07-23).** Amber read as a candle flame;
  a cool faceted crystal wound in dark copper is unambiguous. **Update (art pass 4): the energy
  gauge on the rail is now the same cool crystal-blue** (`0xFF3EA9C4` fill on `0xFF15323B`
  glass in `PackScreen.drawEnergyGauge`) — SapperSquad confirmed the icon and gauge should match. It
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
  `pack_block_twine` texture. *(Superseded 2026-07-24, SapperSquad's call: the earlier "the canvas item's
  twine buckle is a small, accepted divergence on the block" no longer holds — a set-down canvas
  pack now shows a twine buckle to match its item. Straps on the block remain brass for all tiers;
  making the canvas straps twine too was not requested and is a trivial follow-up if wanted.)*
- **Runed glow = block light emission + bright glyphs, not model emissive.** `lightLevel(state ->
  tier==RUNED ? 8 : 0)` makes a set-down Runed pack actually glow in the world, and the glyphs are
  high-contrast; this was chosen over an unverified per-face model-emissive flag and over
  duplicating geometry for an overlay quad. Verified in-game (the Runed block is visibly brighter
  than its neighbours).

## 2026-07-25 — per-slot DEPTH by tier (the deep-slot batch; reopen with evidence)

- **Depth semantics: each slot holds the item's own max stack x the tier's multiplier**
  (Canvas x1 ... Dragonhide x6 = 384 of a 64-stackable, 96 of a 16-stackable), matching
  Sophisticated's stack-upgrade model and SapperSquad's "64, 128, and so on". Unstackables never
  stack - a pack is organized, not magic enough to bundle swords. Tiers own DEPTH;
  the Bottomless Lining owns BREADTH (extra slots) - one axis each, no double-dipping,
  documented in the Handbook.
- **Persistence: a custom codec, verified against the sources, because vanilla WOULD corrupt.**
  `ItemStack.CODEC` hard-caps count at `intRange(1, 99)` and `ItemContainerContents.Slot.CODEC`
  routes through it, so saving a 384-deep slot through the vanilla codec fails at world save.
  `DeepContentsCodec` persists `{slot, item:{id, components}, count}` with the count as its own
  unbounded field, registered as `pack_contents`' persistent codec. The `count` field is
  deliberately REQUIRED so pre-depth saves FAIL the deep shape and fall through
  `Codec.withAlternative` to the vanilla codec - old packs load intact; if count were optional,
  legacy data would "succeed" as count 1 and silently shrink every stack. The NETWORK codec is
  untouched: the stream path writes counts as raw VarInts and never caps. A gametest round-trips
  the relog path, the block-entity chunk-save path, and the legacy fallback.
- **The escape hatches are closed at the choke points.** `PackInventory.insertItem` is
  overridden because the parent clamps inserts to the item's own max stack (verified in the
  NeoForge sources - `getSlotLimit` alone is ignored for merging); `extractItem` is overridden
  because the parent does NOT clamp and vanilla's cursor pickup calls
  `tryRemove(count, Integer.MAX_VALUE, ...)`. Rules: inserts fill to depth; EVERY pull out -
  cursor, hopper, trinket - pays out at most one vanilla stack. `PackViewSlot` stopped
  extending `StackCopySlot` (its `remove` is final) and inlines the copy-slot pattern with a
  clamped `remove`, plus a swap-guard in `mayPlace`: swapping the cursor into an oversized slot
  would hand the whole deep stack to the cursor via `setCarried` (a path that never touches
  `remove`), so a different-item place is refused while the slot is deeper than one stack.
- **Deep counts render exact, at 3/4 scale.** Vanilla anchors count text at the slot's right
  edge and three digits spill into the neighbouring cell ("64" + "384" smear together -
  seen in the autoshot). `PackScreen.renderSlotContents` draws >99 counts at 0.75 scale inside
  their own cell; numbers stay exact (no "2.5K" rounding), max is 384 = 3 digits.
- **Tidy Up merges INTO depth** via a `ToIntFunction<ItemStack>` depth argument on
  `PackSorting.tidy`; the 3-arg overload keeps vanilla-depth semantics for callers without a
  tiered pack.

## 2026-07-25 — the recipe chain + the Dragonhide tier (SapperSquad's calls; reopen with evidence)

- **Every tier above Canvas is crafted FROM the previous tier's pack.** SapperSquad explicitly
  reversed the raw-materials-only decision (see the superseded entry above). Canvas stays
  craftable from wool+string+chest; the preserving `packwork:pack_upgrade` recipe is now THE
  recipe per tier - pack + materials in, everything carried over - and the raw recipes for
  tiers 2+ are deleted, so there is no recipe anywhere that could eat a filled pack.
- **The upgrade now carries the STORES too.** Found while extending it: `assemble` only copied
  contents/layout/trinkets/name, so an upgrade silently dropped stored fluid/XP/energy/embers/
  chemical - a pause-never-punish violation. All five store components are copied now, and the
  chain gametest pins fluid+XP+energy+name+deep-contents across the Runed->Dragonhide step.
- **`PackUpgradeRecipe` gained an optional second material** (`material2`/`count2`,
  absent-safe for the existing JSONs) because one endgame gate wants two distinct reagents.
  Its stream codec is hand-rolled - seven fields exceeds `StreamCodec.composite`'s arity.
- **The 6th tier is DRAGONHIDE** (flag for SapperSquad - veto welcome; alternatives considered:
  Wyrmhide, Drakeskin). End-gated: Runed pack + 4 shulker shells + 4 dragon's breath. The
  Runed upgrade also picked up 2 echo shards as material2, preserving the deleted raw recipe's
  Deep Dark gate. Numbers: 256 slots (the component cap - the top tiers grow DEEP, not wide),
  5 sockets, depth x6, stores at 6x (48-bucket waterskin, 30k XP, 600k FE, 96k mB), light 11
  when placed (above Runed's 8).
- **Dragonhide's look: near-black charcoal-plum hide + brick-laid scale scallops + pale BONE
  claws + an ember-pink breath gem.** Deliberately darker and heavier than Runed's mid-indigo
  so the step reads at a glance; the trim is drawn shapes (scales, claws, a gem with a halo)
  per the de-noising rule. Blockstate variants went 20 -> 24; same `pack_shape` geometry.
- **The gauge rail shrinks to fit.** Five sockets plus gauges outran the panel, so
  `PackScreen.gaugeHeight()` compresses gauges (40px down to a 22px floor) to fit the panel
  height; the rail's click-consumption region uses the same computation.

## 2026-07-25 — the Recipe Ledger (craft-on-the-go browser; reopen with evidence)

- **In-house parchment browser, NOT vanilla's `RecipeBookComponent`.** SapperSquad asked for the
  recipe book; the vanilla component was evaluated honestly and rejected for two structural
  reasons: (1) its craftability + auto-place plumbing (`StackedContents` fill,
  `ServerPlaceRecipe`) is hardwired to the PLAYER inventory, and the whole point here is
  craftable-from-PACK-stock with items never moving until the player crafts; (2) it slides the
  GUI right and occupies the left flank - where the compartment rail lives. Fighting both
  means overriding nearly everything the component does, so the in-house sheet is less code
  and honest about it.
- **The Ledger is pure client-side paint; ONE server verb moves items.** The craftable list is
  computed client-side over synced data (`StackedContents` filled from the pack at full depth
  plus the roll, `canCraft` per 3x3-able crafting recipe - the same check vanilla's book runs,
  pointed at the pack). Clicking a recipe CHALKS it: a translucent ghost on the roll's empty
  cells (vanilla's own ghost-overlay render pattern), zero item movement. Clicking the result
  well with a chalk active sends `LAY_OUT_GHOST` (server-authoritative): the server re-resolves
  the recipe, SIMULATES a pack slot for every cell first, and only if the whole pattern is
  covered pulls exactly one item per cell - all-or-nothing, gametested (an uncoverable cake
  lays nothing and spends nothing). Special recipes (empty ingredient list, e.g. our own
  upgrades) are refused on both sides.
- **Recompute cadence:** on open, on search change, and every 40 ticks while visible - the
  same order of work vanilla's book does per inventory change.

## 2026-07-26 — Quill & Ledger rework: stamp = baseline, ledger = rule editor (SapperSquad's call)

- **Stamp-family matching is ALWAYS-ON for custom tabs — no trinket needed.** Stamp a tab
  with a pickaxe and it gathers tools, out of the box. SapperSquad played the v1 stamp-gate proxy
  and couldn't tell what the fitting did; that was the verdict on the flag the v1 entry
  carried (see the superseded entry above). The stamp rule derives at routing time in
  `SortEngine.toView` — storage-free, dupe-free, and now unconditional.
- **The Quill & Ledger's legible job is the per-tab RULE EDITOR.** With it fitted, opening a
  custom compartment shows a quill button under the grid; it unfolds a parchment sheet
  (`PackScreen.drawRulesSheet`, same chrome as the Recipe Ledger) where the player writes
  filters — a text box filed **by name** or **by mod**, and six **category chips** (Food /
  Tools / Weapons / Armor / Blocks / Potions) that toggle predicate rules — and strikes them
  off again. Two new layout verbs (`ADD_TAB_RULE` / `REMOVE_TAB_RULE`), server-validated
  hard (custom tabs only, known types, trimmed value ≤64, real predicate names, no dupes,
  16-rule cap, and the ledger must actually be fitted).
- **Written rules EDIT and MATCH only while the ledger is fitted; they are never deleted by
  pulling it.** Chosen as the most legible model: the fitting's presence answers both "can I
  write?" and "do my writings sort?" with one yes. Pull the ledger and tabs fall back to
  stamp + pins — pause, never punish: the authored rules stay in the component and wake when
  it returns, and items merely re-route (a tab is a filter, nothing is stored per-tab).
  Pins still beat rules everywhere.
- Gametests: `stampFilesAlwaysLedgerGatesWrittenRules`, `ruleEditorWritesAndStrikesLedgerGated`
  (gating, benching, striking, dupe/junk refusal), plus the updated pin-priority test.

## 2026-07-26 — JEI real recipes, per-cell materials, and the pinning gesture (reopen with evidence)

- **Pack upgrades render in JEI's own crafting category via a category extension, not a custom
  category.** The upgrade IS a crafting recipe (`RecipeType.CRAFTING`), so the standard move is
  `ICraftingCategoryExtension<PackUpgradeRecipe>` registered through
  `registerVanillaCategoryExtensions` — it shows up exactly where players look for "how do I make
  this", beside the Canvas recipe, with JEI's shapeless marker. A bespoke category would have
  moved the ladder into its own tab nobody checks. The preserving behaviour rides the result
  slot's tooltip (`packwork.jei.upgrade.preserves`). The API shapes were verified against the
  pinned 19.21.1.312 api jar via javap, not memory.
- **Upgrade material counts mean CELLS, not items (supersedes the summed-count matcher).**
  Vanilla consumes exactly one item per occupied cell when a craft is taken (`ResultSlot.onTake`
  shrinks each slot by 1 — verified in the 1.21.1 sources), so counting item totals let a stacked
  cell match while the craft consumed one: a 75% discount on the Dragonhide upgrade. `matches()`
  now tallies one per cell and demands the exact spread, like every vanilla recipe; the JSONs are
  unchanged (count = number of cells). A gametest pins the stacked case as a non-match.
- **The pinning gesture is "drop it where you want it".** Placing an item into a tab whose rules
  would NOT route it there auto-pins it to that tab; placing it where it belongs does nothing.
  Chosen because the old failure mode was exactly this gesture: drop bread into Valuables, the
  next sort snatches it back, and the player concludes the pack is haunted. Implementation:
  `PackViewSlot.setByPlayer` is the ONE vanilla hook that fires only for the player's own hand
  (safeInsert/swap; programmatic writes use `set` — verified in the decompiled Slot class), and
  the decision is deferred to after `clicked` resolves so the view is never rebound mid-click.
  Both sides run it from mirrored state, so no new packet. Pin/unpin feedback is an in-screen
  stitched parchment note (vanilla's actionbar overlay dims under the container-screen gradient,
  so an in-GUI note is the legible spot). P stays the explicit toggle and shows the same note.

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

## 2026-07-25 — the Tinker's Kit and the second trinket batch (reopen with evidence)

- **The tool roll unrolls OVER the pack's lower rows, it is not a separate screen or tab.** The
  compartment rail is for compartments; a bench is not one, so it does not get a tab. Instead a
  latch in the title strip (visible only with a kit fitted) unrolls a leather tool roll across the
  bottom three grid rows — the pack keeps its top three rows, so you can still see and shift-click
  the stock you're crafting from. Rejected: a separate crafting screen (breaks "one object"), a
  rail panel (the rail is already four gauges tall on a Runed pack), and permanently reserving
  grid space (the GUI must stay uncluttered for the ~everyone who hasn't crafted a kit).
- **The 3x3 + result slots exist on EVERY pack menu, always.** They're added unconditionally and
  simply go inactive without the kit. The alternative — adding slots when the trinket is present —
  reintroduces exactly the client/server slot-count mismatch that overran the container packet and
  dropped the player twice during Phase 2. `isActive`/`mayPlace`/`mayPickup` all gate on
  `rollActive()`, so an inactive bench refuses every interaction even from a hostile client.
- **Shift-click lays ONE item in the next EMPTY cell**, rather than vanilla's "tip the stack into
  the first slot that takes it". You're setting out a shape, so three shift-clicks of wheat should
  be a row of three, not a pile of three in one corner. It deliberately returns EMPTY from
  `quickMoveStack` afterwards so vanilla's quick-move loop stops at one per click. *(Found in a
  live playtest: with the vanilla ordering, four shift-clicks of planks stacked into cell 0 and
  the bench offered to make a button.)*
- **The bench refills each emptied cell from pack stock after every craft.** This is what makes it
  "craft on the go" rather than "a crafting table you have to hand-feed": set the pattern once and
  one shift-click on the result runs the batch until the pack is out of makings. It can only ever
  put back what it takes out of the store (`pullOneFromPack` extracts, never mints), so the batch
  stops dead when stock runs out — a gametest pins 12 planks' worth in play at every step, and
  proves a fourth click on a dry pack is a no-op rather than a free craft.
- **Nothing is ever stranded on the bench.** Rolling up and closing the pack both empty the 3x3
  back into the pack (then pockets, then floor), via the same `emptyRollIntoPack`. The return path
  passes `allowVoid=false` to `insertIntoPack`: a Compass Rose can bin what you deliberately marked
  on the way IN, but a hand-back must hand back.
- **Two trinkets pay for themselves in SORTING, not just in effects.** `AutoTabs.Auto` gained a
  `gate` field, so the Cartographer's Sleeve and the Angler's Creel each add a whole compartment
  with one entry in the table plus a tag JSON — the single-source-of-truth rule holding. Both are
  slotted at their proper PRIORITY, not appended: The Catch has to out-rank Food or every cod files
  itself as rations. `SortEngine` also inserts a newly-unlocked compartment at its default priority
  inside an OLD pack's saved tab order, so fitting a creel to a long-used pack still works.
- **The Field Furnace is deliberately narrow, twice over.** It cooks only raw ore and raw food (a
  furnace would happily turn your cobblestone into stone and your logs into charcoal, which is no
  favour when it happens behind your back), and it burns only what a datapack tag
  (`packwork:furnace_fuel`) calls fuel. *The fuel restriction came out of a live playtest: with
  "anything with a burn time" it quietly ate an oak plank off the top of the pack.* It also checks
  there is room for the output BEFORE the raw item leaves its slot, so the swap is atomic.
- **The Provisioner's Pouch eats the PLAINEST thing, not the cheapest.** Nutrition alone picked the
  golden apple (4) over bread (5). The rule is now: no effects attached at all, and not on the
  datapack `packwork:never_auto_eat` list. That keeps golden apples, suspicious stew and chorus
  fruit out of its reach for the right reason — a food you went to trouble for is not rations.
- **Cut from the candidate list: Prospector's Pan.** Every honest version of it either duplicated
  the Lodestone Charm (pull ore into the pack) or needed a HUD readout of what's underground, which
  is the futuristic register the whole mod is avoiding. No pillar, no entry. Flagged rather than
  shipped as filler.

## 2026-07-25 — art: the packs were BUSY, not detailed (reopen with evidence)

- **SapperSquad's read was right and the cause was measurable.** Four separate high-frequency sources were
  stacked on a 32px sprite that lands in a 16px slot: `leatherGrain` added **per-pixel random ±3**
  on top of its crease pattern; the canvas tier ran a **1px-pitch crosshatch at ±8 over the entire
  sprite**; the hem stitch was **every other pixel at near-white**; and the studs/plates/runes were
  1px sprinkles. Individually defensible, together they read as speckle and dissolved the form.
- **The rule now: big forms read first, and every piece of detail is a deliberate SHAPE.** A new
  `smoothNoise` (coarse lattice + smoothstep interpolation) replaced every per-pixel `valueNoise`
  call across the item sprites AND the block faces; grain is a soft 7-row crease plus one large
  mottle; the canvas weave is a 2-on/2-off rib at ±5; the hem is a 3-on/1-off thread on an unbroken
  groove. Studs are 2×2 with a contact shadow (six of them, not nine), plates are bevelled 4×4 with
  a brass rivet, runes are drawn strokes with a 1px bloom. **Detail was sharpened, not removed** —
  which is what SapperSquad asked for ("I don't mind the detail").
- **The closure strap was reading as a hole.** Its ramp started at the tier's darkest stop shaded
  down again, so the edges bottomed out near-black and punched a dark slot down the middle of every
  pack, stealing the buckle's contrast. It's darker LEATHER now with a tapered tip, and the buckle
  gained a hard outer edge so it reads as one crisp object.
- Verified at 16px and 12px via `tools/pack_small_preview.png` before touching the client, then in
  the client. Re-run `java tools/GenTextures.java` + `java tools/AnalyzeSprites.java` after any
  sprite change.

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
- **Pin marker is a red ribbon + brass tack**, not a subtle corner dot — SapperSquad flagged the old
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
