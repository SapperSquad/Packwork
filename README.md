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
| **Waterskin Rack** | Fits a fluid tank: a glass gauge you fill with a bucket or flask. |
| **Soul Vial** | Stores XP in a glass vial — siphon yours in, pour it back, and it auto-mends your Mending gear. |

*(Feather Charm, Quick-Draw Straps, and Quill & Ledger are craftable and slot in, but
their effects are still on the bench — see the changelog.)*

## Plays nice, needs nothing

Zero hard dependencies. The pack exposes **standard NeoForge capabilities** — an item
handler always, a fluid handler once a Waterskin Rack is fitted — so any mod's hoppers
and pipes work against a placed pack with no Packwork API. [Pantrywork](../Pantrywork)
food tags fold into the Food tab automatically when it's present, and are simply ignored
when it isn't.

## Using it

Right-click a pack in hand to open it, or press the **Open Pack** keybind (default **B**)
to open the first one you're carrying — no need to dig it out. On death it drops as a
single item with everything still inside.

## Build

```powershell
cd Packwork
./gradlew.bat compileJava        # fast check
./gradlew.bat runGameTestServer  # headless: persistence + sorting invariants
./gradlew.bat build              # the jar, in build/libs
./gradlew.bat runClient          # dev client
```

Textures are generated procedurally — `java tools/GenTextures.java` after editing them.

## On the roadmap

Two more resource stores are coming, each a physical fitting, never a tank-and-cable
panel: a **Charge Crystal** for arcane charge (bridging Forgework Flux 1:1) and an
**Alchemist's Flask Harness** for bottled vapors (Mekanism chemicals). Plus Curios
back-slot wear, an in-house Outfitter's Handbook, and JEI.

---

Built by **SapperSquad**. Sits beside Coinkeep, Highroller, Forgework, PhytoForge,
Gunsmith, Pantrywork, and Reel Rivals.
