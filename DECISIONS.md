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
