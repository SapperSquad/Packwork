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

## Status — greenfield

Nothing is built yet. This doc, `DECISIONS.md`, and the `packwork` agent are the whole
project. **First task: scaffold the NeoForge 1.21.1 project** (see Build & Run), then
Phase 0. Create `README.md`, `PUBLISHING.md`, and `CLAUDE.md` at the first shippable
milestone, not before.

Decide up front whether this is a git repo. Recommend `git init` immediately — Highroller
is versioned and it's saved it more than once. PhytoForge is *not* a repo and every delete
there is permanent; don't repeat that.

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

- **Phase 0 — prove the loop.** Scaffold; a Leather Pack item that opens a basic single-
  grid GUI; contents persist through relog/drop/placement.
- **Phase 1 — the sorting system (the headline).** Tabs, auto-tabs via tags, custom tabs,
  the rules engine, manual pins, Tidy Up, search/flatten, data-driven categories. Nail
  this before touching resource stores.
- **Phase 2 — tiers + trinket framework.** The material ladder and the "easy" trinkets
  (magnet, feather, void filter, restock, capacity, repair, quick-draw).
- **Phase 3 — the four resource stores.** In order: fluids → XP → energy → gas, each behind
  its trinket, with gated Forgework/Mekanism bridges.
- **Phase 4 — progression & release.** Crafting, an Outfitter's Bench for upgrading, JEI,
  a quest chapter, then `README.md` + `PUBLISHING.md` + store art.

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
