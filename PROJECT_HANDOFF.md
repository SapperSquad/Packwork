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
> first build); bump/label at publish. 17 GameTests green; jar builds clean.

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
Charge Crystal (arcane charge via `PackEnergyStorage` implementing `IEnergyStorage`, amber
gauge, any FE source fills it, tops up powered tools in hand, + the gated Forgework bridge
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
  `PackMenu` (virtual-tab menu + all action handlers), `PackViewSlot` (rebinding grid cell).
- `sort/` — `SortRule`, `PredicateKind`, `TabDef`, `PackLayout` (component), `AutoTabs`
  (SSOT category table), `TabView`, `SortEngine` (routing), `PackSorting` (Tidy Up).
- `block/` — `PackContainerBlock` (placeable, facing, opens the GUI, drops the pack stack),
  `PackContainerBlockEntity` (holds the pack as one `ItemStack`; tier-only client sync).
  Registered in `reg/ModBlocks` + `reg/ModBlockEntities`; block caps in `PackworkCapabilities`.
- `trinket/` — `TrinketType` (SSOT table), `TrinketItem`, `TrinketAccess`, `TrinketEffects`
  (per-tick effects + the Quick-Draw break handler + its dupe-safe `pullReplacement`).
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

- **Lodestone Charm** — magnet, pulls nearby items/xp into the pack.
- **Quill & Ledger** — custom tabs match by rule, not just pins (files by the stamped icon's
  kind). LIVE.
- **Compass Rose** — void filter (opt-in trash for chosen items).
- **Tinker's Kit** — a crafting grid inside the pack.
- **Field Furnace** — smelts from pack contents over time (campfire flavor).
- **Repair Kit** — mends stored/equipped gear.
- **Restock Strap** — auto-refills the hotbar from pack stock.
- **Bottomless Lining** — extra capacity per compartment.
- **Quick-Draw Straps** — a broken held tool is replaced from pack stock. LIVE.
- Plus the store trinkets (Waterskin Rack, Charge Crystal, Soul Vial live; Flask Harness
  deferred with the Gas store).

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
cd %USERPROFILE%\Documents\Packwork
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

## Open questions for SapperSquad — flag, don't guess

1. **Wear slot:** Curios back slot, inventory-only, or both?
2. **Death behavior:** does the pack (and contents) keep on death, drop, or offer a
   soulbound trinket? This is a balance call, not a default to assume.
3. **Nesting:** allow pack-in-pack, or block it? Nesting invites dupe bugs and lag.
4. **Quest home:** a Coinkeep Ledger chapter (like Highroller) or an FTB/standalone book?
