# Tiers & fittings

[← back to the manual](README.md)

A pack grows two ways, and they never overlap: **the material it's made of**, and **the
fittings you craft for it**.

## The material ladder

Six steps. Each one grows the pack three ways at once — wider (more slots), deeper (each
slot holds more), and one more brass socket on the rail.

| Tier | Slots | Stacks per slot | Sockets | Placed light |
|---|---|---|---|---|
| Canvas | 54 | ×1 (64 of a common item) | 0 | — |
| Leather | 108 | ×2 (128) | 1 | — |
| Studded | 162 | ×3 (192) | 2 | — |
| Reinforced | 216 | ×4 (256) | 3 | — |
| Runed | 256 | ×5 (320) | 4 | 8 |
| **Sculkhide** | 256 | ×6 (384) | 5 | 11 |

The top two tiers stop growing wide and keep growing **deep** — 256 slots is a hard ceiling
in the data component the contents live in, so past Reinforced the pack gets bottomless
rather than sprawling. All of these numbers are packmaker-tunable; see
[Configuration](config.md).

**Depth belongs to the tier; extra slots belong to the Bottomless Lining.** One axis each,
so there's no double-dipping to puzzle out.

Unstackable items never stack, at any tier. A Sculkhide pack holds 384 cobblestone in a
slot and exactly one pickaxe.

### Crafting up

The Canvas pack is a raw craft: a chest wrapped in wool, its corners tied with string.

**Every tier after that is a ring built around the pack you already own.** Set your pack in
the middle of the bench and fill all nine cells: the tier's bulk material on the four edges,
its fittings on the four corners. Turn the ring any way you like — rotations are free — but
edges and corners are not interchangeable.

| Craft | Edges | Corners |
|---|---|---|
| Leather | Leather | Copper ingots |
| Studded | Leather | Cut copper |
| Reinforced | Iron ingots | Diamonds |
| Runed | Diamonds | Netherite ingots |
| Sculkhide | Amethyst shards | Echo shards |

**The upgrade craft is safe, always.** The result carries contents, compartments, pins,
arrangement modes, fittings, the pack's name, and every store — water, XP, charge, embers,
vapors — straight up. There is no recipe anywhere in this mod that can eat a filled pack.

Because all nine cells are occupied, there's no way to underpay for an upgrade, and because
the pack itself is an ingredient, the ladder is a genuine chain: you can't skip a step.

## Fittings

Fittings slot into the brass sockets on the right rail. Craft one, drop it in a socket,
done — no power, no fuel, no setup. Any of them can be turned off by a packmaker
([Configuration](config.md)); a disabled fitting goes quietly inert in its socket rather
than breaking, and wakes up intact if it's turned back on.

### The stores

These four are the "one object, many stores" pillar. Each one shows up as a glass gauge on
the right rail — never a tank-and-cable panel. Details and automation in
[Stores & automation](stores-and-automation.md).

| Fitting | What it adds |
|---|---|
| **Waterskin Rack** | A fluid tank, 8 buckets on Canvas up to 48 on Sculkhide. Click the gauge with a bucket or flask to fill or empty it. |
| **Soul Vial** | Experience storage, 5,000 points on Canvas up to 30,000 on Sculkhide — and it quietly mends your Mending gear from the reservoir. |
| **Charge Crystal** | An arcane charge in copper-wound crystal: 100,000 FE on Canvas up to 600,000 on Sculkhide, topping up the powered tools in your hands. |
| **Alchemist's Flask Harness** | Bottled vapors — a chemical tank, 16,000 mB on Canvas up to 96,000 on Sculkhide. Only exists with Mekanism installed. |

### The pack's own habits

| Fitting | What it does |
|---|---|
| **Lodestone Charm** | Draws loose items nearby into the pack, and files what you mine: pickups the pack can sort go straight in. New finds still go to your pockets. |
| **Bottomless Lining** | Adds another run of slots. Breadth only — depth stays the tier's job. |
| **Restock Strap** | Tops up your hotbar stacks from pack stock as you spend them. |
| **Quick-Draw Straps** | When a tool breaks in your hand, the pack passes you another from stock. |
| **Repair Kit** | Slowly mends the gear you're wearing and holding. |
| **Compass Rose** | Bins items you mark, at the door. **The only way a pack ever throws anything away**, and it only ever touches what you listed. |
| **Quill & Ledger** | Unlocks the per-compartment rule editor — see [Sorting](sorting.md). |

### Jobs it does while you walk

| Fitting | What it does |
|---|---|
| **Tinker's Kit** | A leather tool roll that unrolls across the pack's bottom rows: a 3×3 bench fed straight from your stock. Shift-click from the pack lays **one** item on the bench (you're setting a pattern, not tipping a stack in), and every emptied cell tops itself back up after each craft — so one shift-click on the result runs the batch until the pack is dry. Roll up, or close the pack, and everything laid out comes home. |
| **Field Furnace** | Banked embers that cook raw ore and raw food at furnace rates while you travel. Feed it fuel from the pack. |
| **Provisioner's Pouch** | Eats the plainest safe thing in the pack when you're down to three haunches. Effects-bearing foods and anything in the `packwork:never_auto_eat` tag are left alone — your golden apples stay yours. |
| **Torchbearer's Loop** | Sets a torch from pack stock when you're standing in the dark. |
| **Herbalist's Bundle** | Replants a grown crop the moment you pull it, using a seed from your own stock. |
| **Overflow Valve** | Carries only so much of something you marked, and lets the surplus run out as you walk. Reads the same discard list the Compass Rose does. |
| **Compacting Press** | Squeezes nine into one inside the pack - ingots to blocks, nuggets to ingots - and only ever things that squeeze back out again. |

### The one discard list, and the two fittings that read it

A pack throws things away in exactly one way: you mark an item, and then you say how much of
it you'll still carry. Hover an item in the pack and press **O** to mark it. **Shift+O**
(with an Overflow Valve fitted) dials the keep level — 1, 2, 4, 8, 16 stacks, then round
again to nothing at all.

| Keep level | What happens | Which fitting |
|---|---|---|
| **nothing at all** (0) | The item never enters the pack. Binned at the door on pickup and by the magnet. | **Compass Rose** |
| **1–16 stacks** | The item comes in and files normally; anything above the level bleeds out of the valve as you walk. | **Overflow Valve** |

Both fittings on one pack is the useful case, not a conflict: the Rose handles everything
you set to nothing, the Valve handles everything you gave a number. Neither ever touches an
item you didn't mark, and the Valve never digs below the number you set. Take an item off
the list and its keep level goes with it.

The default a fresh mark lands on is `overflow_valve.default_keep_stacks` in
[the config](config.md) — 4 stacks as shipped.

### Fittings that add a compartment

| Fitting | Opens |
|---|---|
| **Cartographer's Sleeve** | **Charts & Bearings** — maps, compasses, clocks and the spyglass, filed on their own. |
| **Angler's Creel** | **The Catch** — your catch lands in the pack, in a compartment of its own. |

## The Recipe Ledger

The Tinker's Kit comes with a parchment browser: a searchable list of everything you can
craft **right now from what's in the pack** (counting full depth, and whatever's already on
the roll). Click a recipe to chalk it onto the roll as a ghost; click the result well and
the pack lays it out from stock — all or nothing, so a recipe it can't fully cover moves
nothing.

## Setting a pack down

Sneak-click a block face and the pack becomes a block, contents and all. Each tier shows
its own trim in the world — canvas weave and twine, leather grain, a brass stud ring,
riveted steel plates, glowing glyphs, echo-lit sculk hide — and the Runed and Sculkhide
packs give off light.

Breaking it gives back the same pack, byte for byte: the same contents, layout, pins,
fittings, name and stores. A placed pack is also the one that hoppers and pipes can reach —
see [Stores & automation](stores-and-automation.md).

## Wearing it

With **Curios** (NeoForge) or **Trinkets** (Fabric) installed, a pack rides the back slot.
Its fittings keep ticking while worn, **B** finds it when your pockets hold no pack, and
**Shift-B** opens the worn one outright.

A worn pack also **renders on your back**, showing its tier's own leather and trim. It steps
aside for an elytra and hides with invisibility. (Watched working on every **NeoForge**
build, with Curios; the layer ships in the Fabric builds too but has not been confirmed
there yet.) If you'd rather not see it, set
`show_worn_pack = false` in `packwork-client.toml` — that's your setting alone and never
leaves your machine.
