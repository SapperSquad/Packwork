# Packwork

**A humble adventurer's pack that holds far more than it should — and quietly
organizes itself.** Drop an item in and the pack decides where it goes: stamped
leather tabs run down the side, each a compartment, and a rules engine claims every
item for the right one the moment it lands. It's the "much better Sophisticated
Backpacks" — the sorting is the whole point. NeoForge 1.21.1.

Leather, brass, canvas, glass vials. Never a circuit board.

## The sorting (the reason it exists)

Open a pack and you get a rail of stamped leather tabs, a stitched search bar, and a
grid that keeps itself in order:

- **Auto-tabs, out of the box** — Food, Combat, Tools & Utility, Ores & Valuables,
  Brewing & Alchemy, Nature & Farming, Blocks & Building, and a **Loose** catch-all.
  They're driven by what an item *is* (any mod's pickaxe is a Tool, any mod's food is
  Food) plus item tags, so they cover modded items for free. Retune the tag lists in a
  datapack — no code.
- **A Loose tab catches anything no rule claims**, so nothing you drop in ever vanishes.
- **Make your own compartments** — name a tab, stamp it with any item's icon, dye the
  leather, drag to reorder. Priority is top-to-bottom; the first tab that wants an item
  gets it.
- **Pin an item** to a compartment (hover it, press **P**) and it always lands there,
  beating every rule.
- **Tidy Up** merges partial stacks and re-sorts the whole pack; **search** finds
  anything across every tab; **flatten** drops it all into one grid when you just want
  to rummage.

## Tiers & trinkets

The pack grows two ways — the material it's made of, and the fittings you craft for it.

**Material ladder:** Canvas → Leather → Studded → Reinforced → Runed, more slots and
more trinket sockets each step. Upgrading a *full* pack carries its contents, layout,
and trinkets straight up — a craft never eats what's inside.

**Trinkets** slot into the brass sockets on the right rail:

| Fitting | What it does |
|---|---|
| **Lodestone Charm** | Draws loose items nearby into the pack. |
| **Restock Strap** | Tops up your hotbar stacks from pack stock. |
| **Repair Kit** | Slowly mends the gear you're wearing and holding. |
| **Bottomless Lining** | Adds another run of slots (and never voids them if you pull it). |
| **Compass Rose** | Voids items you mark — the *only* way a pack throws anything away, and it's opt-in. |
| **Quick-Draw Straps** | When a tool breaks in your hand, the pack passes you another from stock. |
| **Quill & Ledger** | Custom compartments start filing by rule, not just pins — each gathers items that share the kind of the item it's stamped with. |
| **Waterskin Rack** | Fits a fluid tank: a glass gauge you fill with a bucket or flask. |
| **Soul Vial** | Stores XP in a glass vial — siphon yours in, pour it back, and it auto-mends your Mending gear. |
| **Charge Crystal** | Holds an arcane charge (any mod's FE) in a copper-wound crystal and tops up the powered tools in your hands. With Forgework installed it also feeds your portable terminals, 1 Flux = 1 FE. |
| **Alchemist's Flask Harness** | A rack of flasks for bottled vapors — a chemical tank any Mekanism pipe fills. *(Needs Mekanism; craftable only when it's installed.)* |

## Set it down

Sneak-right-click a block face to **stand the pack in the world** — it sits there as a
leather-and-brass pack, tinted for its tier and facing you. Break it (or middle-click to
pick it up) and you get the pack item back with every last thing still inside — contents,
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
single item with everything still inside.

New to it? Craft the **Outfitter's Handbook** (a book and a piece of leather) and
right-click it. Five chapters — the pack, sorting, trinkets, tiers, and the stores — walk
you through the whole thing, in-game.

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
- **Curios** — wear the pack in the **back** slot, and its trinkets keep working while worn.
- **Forgework** — a placed pack's Charge Crystal charges straight off Forgework's Flux cables
  (1 Flux = 1 FE), and the crystal tops up your carried Forgework terminals.
- **JEI** — look up any pack tier, trinket, or the handbook from the recipe screen.
- **Pantrywork** — its food tags fold into the Food tab.

Everything else — the sorting, the tiers, the fluid/XP/charge stores, the placeable block,
and the Outfitter's Handbook — is always there, no mods required.

---

Built by **SapperSquad**. Sits beside Coinkeep, Highroller, Forgework, PhytoForge,
Gunsmith, Pantrywork, and Reel Rivals.
