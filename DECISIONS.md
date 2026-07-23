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
  filled pack to the next tier while preserving contents needs a custom recipe / the
  Outfitter's Bench — deferred to Phase 4 so no craft can ever eat a full pack's contents.
