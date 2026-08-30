# Packwork

**A humble adventurer's pack that holds far more than it should — and quietly
organizes itself.** Drop an item in and the pack decides where it goes: stamped
leather tabs run down the side, each a compartment, and a rules engine claims every
item for the right one the moment it lands. It's the "much better Sophisticated
Backpacks" — the sorting is the whole point. NeoForge 1.21.1.

Leather, brass, canvas, glass vials. Never a circuit board.

**Full manual: [docs/](docs/)** — [sorting](docs/sorting.md) ·
[tiers & fittings](docs/tiers-and-fittings.md) ·
[stores & automation](docs/stores-and-automation.md) ·
[every config key](docs/config.md) · [for packmakers](docs/for-packmakers.md)

## The sorting (the reason it exists)

Open a pack and you get a rail of stamped leather tabs, a stitched search bar, and a
grid that keeps itself in order:

- **Auto-tabs, out of the box** — Food, Combat, Tools & Utility, Ores & Valuables,
  Brewing & Alchemy, Nature & Farming, Blocks & Building, and a **Loose** catch-all.
  Two more open up when you fit their trinket: **Charts & Bearings** and **The Catch**.
  They're driven by what an item *is* (any mod's pickaxe is a Tool, any mod's food is
  Food) plus item tags, so they cover modded items for free. Retune the tag lists in a
  datapack — no code.
- **A Loose tab catches anything no rule claims**, so nothing you drop in ever vanishes.
- **Make your own compartments** — name a tab, stamp it with any item's icon, dye the
  leather, drag to reorder. The stamp is a live filter: a pickaxe stamp gathers tools,
  a bread stamp gathers food. Priority is top-to-bottom; the first tab that wants an
  item gets it.
- **Pin an item by putting it where you want it** — drop it into any compartment it
  wouldn't sort to and the pack pins it there on the spot (a stitched note tells you so).
  Hover + **P** toggles a pin too. Pinned items wear a red ribbon and beat every rule.
- **Tidy Up** merges partial stacks and re-sorts the whole pack; **search** finds
  anything across every tab; **flatten** drops it all into one grid when you just want
  to rummage.
- **Every compartment picks its arrangement** — Tidy (the pack arranges it) or **Keep my
  layout** (items stay in the exact cells you drop them; new arrivals fill the gaps;
  Tidy Up still re-sorts once). One small switch under the grid.

## Tiers & trinkets

The pack grows two ways — the material it's made of, and the fittings you craft for it.

**Material ladder:** Canvas → Leather → Studded → Reinforced → Runed → **Sculkhide**,
and every step grows the pack THREE ways: more slots, another trinket socket, and
**deeper slots** — each slot holds one more vanilla stack per tier, from 64 a slot on
Canvas to **384 a slot** on Sculkhide (six stacks of cobble in one cell, filed under one
tab). Depth stays inside the pack: your cursor, a hopper, a fitting drawing from stock
all pull one legal stack at a time. (Extra slots stay the Bottomless Lining's job —
depth is the tier's, so the two never double-dip.)

**The ladder is a chain of rings:** sew a Canvas pack — a chest wrapped in wool, corners
tied with string — then every later tier is crafted *around the pack before it*: set it
in the middle of the bench and fill all nine cells, the tier's bulk material on the
edges, its fittings on the corners. Leather ringed with copper buckles, Studded copper
set with iron studs, Reinforced gold cornered with diamonds, Runed diamond bound in
netherite, and Sculkhide amethyst with echo shards from the Deep Dark. The upgrade craft
carries **everything** up: contents, compartments, pins, trinkets, name, and every
store. There is no recipe anywhere that can eat a filled pack.

**Trinkets** slot into the brass sockets on the right rail:

| Fitting | What it does |
|---|---|
| **Lodestone Charm** | Draws loose items nearby into the pack — and files what you mine: pickups the pack can sort (or is pinned on, or already holds) go straight in, new finds still go to your pockets. A title-strip switch turns pack-first pickup off per pack. |
| **Restock Strap** | Tops up your hotbar stacks from pack stock. |
| **Repair Kit** | Slowly mends the gear you're wearing and holding. |
| **Bottomless Lining** | Adds another run of slots (and never voids them if you pull it). |
| **Compass Rose** | Voids items you mark — the *only* way a pack throws anything away, and it's opt-in. |
| **Quick-Draw Straps** | When a tool breaks in your hand, the pack passes you another from stock. |
| **Quill & Ledger** | The rule editor: write your own filters on any custom compartment — by name, by mod, or by category chip — on a parchment sheet beside the pack. Written rules sort while it's fitted; stamps and pins work without it. |
| **Tinker's Kit** | Unrolls a leather tool roll across the pack's lower rows: a 3×3 bench fed straight from your stores. Shift-click from the pack to lay out a pattern; each cell tops itself back up after every craft, so one shift-click on the result runs the batch until the pack is out of makings. Its **Recipe Ledger** is a parchment sheet of everything craftable *from pack stock* — click a recipe to chalk it onto the roll as a ghost, click the result well to lay it out (all-or-nothing, straight from your stores). |
| **Field Furnace** | Banked campfire embers cook as you walk — raw ore and raw food only, never your building blocks. Burns fuel out of the pack at furnace rates. |
| **Provisioner's Pouch** | Feeds you before hunger bites, eating the plainest thing in the pack first. Anything with an effect on it — your golden apples — stays yours. |
| **Cartographer's Sleeve** | Opens a **Charts & Bearings** compartment: maps, compasses, clocks and the spyglass file themselves instead of scattering through Tools. |
| **Angler's Creel** | Opens **The Catch**, and your catch drops straight into the pack instead of bouncing off your chest. |
| **Torchbearer's Loop** | Sets a torch down from pack stock whenever you're standing somewhere genuinely dark. Stops the moment the light comes up. |
| **Herbalist's Bundle** | Replants a grown crop the instant you pull it, spending one seed from your own stock. |
| **Waterskin Rack** | Fits a fluid tank: a glass gauge you fill with a bucket or flask. |
| **Soul Vial** | Stores XP in a glass vial — siphon yours in, pour it back, and it auto-mends your Mending gear. |
| **Charge Crystal** | Holds an arcane charge (any mod's FE) in a copper-wound crystal and tops up the powered tools in your hands. With Forgework installed it also feeds your portable terminals, 1 Flux = 1 FE. |
| **Alchemist's Flask Harness** | A rack of flasks for bottled vapors — a chemical tank any Mekanism pipe fills. *(Needs Mekanism; craftable only when it's installed.)* |

## Set it down

Sneak-right-click a block face to **stand the pack in the world** — it sits there as a
leather-and-brass pack facing you, wearing its tier's own trim (canvas twine, brass studs,
riveted steel plates, or glowing runes; a Runed pack even glows). Break it (or middle-click
to pick it up) and you get the pack item back with every last thing still inside — contents,
layout, trinkets, and any fluid, charge, or XP. Right-click a placed pack to open the same
tabbed organizer.

A placed pack speaks **standard NeoForge capabilities**, so hoppers, pipes, and cables just
work against it: an item handler always (drop items in and they auto-file into the right
compartment), a fluid tank once a Waterskin Rack is fitted, and an energy store once a
Charge Crystal is. Feed it, drain it, charge it — no Packwork API, no bridge block.

## Plays nice, needs nothing

Zero hard dependencies. Every cross-mod touch is gated and simply absent when its mod is.
[Pantrywork](../Pantrywork) food tags fold into the Food tab when it's present. With
**Forgework** installed, a placed pack with a Charge Crystal also charges straight off
Forgework's own Flux cables (1 Flux = 1 FE) — the block-level interop a hand-held pack
can't do — and the crystal tops up your carried Forgework terminals besides.

## Using it

Right-click a pack in hand to open it, or press the **Open Pack** keybind (default **B**)
to open the first one you're carrying — no need to dig it out. On death it drops as a
single item with everything still inside — or, if the server says so, stays with you
through the respawn, or sets itself down as a block where you fell
(`death.handling` in [`config/packwork-server.toml`](docs/config.md)).

New to it? Craft the **Outfitter's Handbook** (a book and a piece of leather) and
right-click it. Six chapters — the pack, sorting, trinkets, tiers, the stores, and where to
send a field report — walk you through the whole thing, in-game.

## Languages — translation PRs welcome

English is hand-written. Ten more locales ship as **machine-drafted first passes that have
not been read by a native speaker**: Simplified Chinese, Russian, Brazilian Portuguese,
German, French, Spanish, Japanese, Korean, Polish, and Ukrainian. Each carries a
`packwork.translation.status` line saying exactly that (it is never shown in game).

If one of them reads wrong to you, a pull request fixing it is genuinely welcome — one key,
one file, one line, all fine. A new language is just as welcome: copy
`src/main/resources/assets/packwork/lang/en_us.json` to your locale code and translate the
values. Two house rules: keep every `%s` exactly as it appears in the English (they are
filled in at runtime, and a missing one crashes the screen it is on), and save as UTF-8
**without** a BOM. `java tools/CheckLang.java` from the repo root checks both for you and
tells you which keys are still missing.

The Outfitter's Handbook's long-form prose is still English only — it is a few thousand
words and would be worse machine-translated than left alone. `docs/` covers the same
ground in the browser.

## Build

```powershell
cd Packwork
./gradlew.bat compileJava        # fast check
./gradlew.bat runGameTestServer  # headless: persistence + sorting invariants
./gradlew.bat build              # the jar, in build/libs
./gradlew.bat runClient          # dev client
```

Textures are generated procedurally — `java tools/GenTextures.java` after editing them.

## Optional friends (each simply absent when its mod isn't)

Zero hard dependencies — every one of these lights up only when its mod is installed:

- **Mekanism** — the Alchemist's Flask Harness becomes a real chemical tank; Mekanism pipes
  fill a placed pack with bottled vapors.
- **Curios** — wear the pack in the **back** slot and open it right off your shoulders
  (B finds it when your pockets hold no pack; **Shift-B** opens the worn one outright).
  Its trinkets keep working while worn.
- **Forgework** — a placed pack's Charge Crystal charges straight off Forgework's Flux cables
  (1 Flux = 1 FE), and the crystal tops up your carried Forgework terminals.
- **JEI** — every craft renders as a real recipe, tier upgrades included (previous pack +
  materials in, next pack out), plus info pages for every pack, trinket, and the handbook.
- **Pantrywork** — its food tags fold into the Food tab.

Everything else — the sorting, the tiers, the fluid/XP/charge stores, the placeable block,
and the Outfitter's Handbook — is always there, no mods required.

---

Built by **SapperSquad**. Sits beside Coinkeep, Highroller, Forgework, PhytoForge,
Gunsmith, Pantrywork, and Reel Rivals.

## License

The source code in this repository is MIT-licensed - see [LICENSE](LICENSE).
The brand assets in [promo/](promo/) (banner, icon, gallery art) and the
store-page copy in [PUBLISHING.md](PUBLISHING.md) are (c) SapperSquad, all
rights reserved - see [promo/LICENSE](promo/LICENSE). Store listings on
Modrinth/CurseForge are published All Rights Reserved by policy.
