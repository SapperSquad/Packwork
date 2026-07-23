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

## Status — Phases 0-2 done; Phase 3 at 3 of 4 stores (fluids/XP/energy); rest scoped

> Newest first. Full source map and roadmap below.

**Phase 2 — trinket framework, DONE & verified in-game.** Right-rail brass sockets
(count = tier), eight craftable fittings off a `TrinketType` SSOT table. Working:
Lodestone (magnet), Restock (hotbar top-up), Repair (slow mend), Bottomless (grows
capacity, never truncates), Compass Rose (opt-in void, the only void path — press O on
a hovered item). Feather / Quick-Draw / Quill&Ledger are registered + socketed but
**inert** (Feather needs a weight mechanic that's an SapperSquad balance call — flagged). Effects
run server-side per `PlayerTickEvent`, throttled/bounded. Preserving tier-upgrade recipe
(`packwork:pack_upgrade`) carries contents+trinkets+name up a tier so no craft eats a pack.

**Phase 3 — fluids + XP + energy stores, DONE & verified in-game.** Three of four stores,
each trinket-gated and stacked as a gauge on the right rail: Waterskin Rack (fluid tank,
`FluidHandler.ITEM`, glass gauge, click-with-cursor fill/drain via `FluidUtil`); Soul Vial
(XP via `PackXpStore`, green gauge, click siphon / shift pour, auto-mends Mending gear);
Charge Crystal (arcane charge via `PackEnergyStorage` implementing `IEnergyStorage`, amber
gauge, any FE source fills it, tops up powered tools in hand). All three capabilities are
exposed **only when the fitting is present**. **Still to do in Phase 3:** Gas (Flask Harness
+ gated Mekanism chemical cap) — needs the Mekanism API dep; and the Charge Crystal's gated
Forgework Flux 1:1 bridge (`compat/forgework/`). Follow the three shipped stores as the
template: component + (gated) capability + STORE trinket + gauge + gametest.

**Still open:** Curios back-slot compat (needs the Curios API as a `compileOnly` dep + its
maven repo — not in the local cache; native use + keybind already satisfy "wear it"). The
Outfitter's Bench block (the upgrade recipe covers the preserve-contents need for now).
Phase 4: Outfitter's Handbook guide, JEI, and store art. **README.md / PUBLISHING.md /
CHANGELOG.md now exist** (written at this milestone) — keep their copy in step with code.

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
- `client/` — `PackScreen` (the rail + controls), `PackClientActions`, `PackKeyMappings`,
  `ClientSetup`, `DevAutoShot`.
- `net/` — `PackAction`, `PackActionPayload`, `OpenPackPayload`; wired in `PackworkNetwork`.
- `reg/` — `ModItems`, `ModMenus`, `ModComponents`, `ModCreativeTabs`. Caps in
  `PackworkCapabilities`.
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
- **Quill & Ledger** — smarter auto-sort (enables the rules engine tiers / multi-rule tabs).
- **Feather Charm** — weightless; no slowdown from a heavy pack.
- **Compass Rose** — void filter (opt-in trash for chosen items).
- **Tinker's Kit** — a crafting grid inside the pack.
- **Field Furnace** — smelts from pack contents over time (campfire flavor).
- **Repair Kit** — mends stored/equipped gear.
- **Restock Strap** — auto-refills the hotbar from pack stock.
- **Bottomless Lining** — extra capacity per compartment.
- **Quick-Draw Straps** — fast tool swap from the pack.
- Plus the four store trinkets (Waterskin Rack, Flask Harness, Charge Crystal, Soul Vial).

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
- **Phase 4 — progression & release.** Outfitter's Bench (tier upgrade preserving
  contents + trinket install), JEI, quest chapter, the in-house **Outfitter's Handbook**
  guide (crib PhytoForge `client/LabManualScreen` + `ManualContent`), then `README.md` +
  `PUBLISHING.md` + `CLAUDE.md` + store art. Create those three docs ONLY here.

### Fastest way to resume
`./gradlew.bat compileJava` (constant), `runGameTestServer` (logic), `runClient -Pautoshot`
(see the GUI). The trinket rail is the natural next build: the RIGHT side of the pack panel
is deliberately empty for it, `PackTier.trinketSlots()` already sizes it, and the action
channel (`PackAction`) is the pattern to add install/remove verbs.

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
