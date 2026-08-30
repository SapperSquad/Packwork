# Sorting

[← back to the manual](README.md)

The tabs down the left side **are** the compartments. There is no separate "storage" behind
them — one flat store, many views onto it, and a rules engine deciding which view claims
what.

## How an item finds its home

When an item lands in the pack, Packwork walks the tab rail from **top to bottom** and gives
it to the first compartment whose rules want it. That means **tab order is priority**:
reorder the rail and you re-decide who gets first pick.

Three things beat the rules, in this order:

1. **A pin.** A pinned item stays in its compartment, full stop. Auto-sort never moves it.
2. **The first rule that matches**, walking the rail top to bottom.
3. **Loose**, which sits last and claims everything nobody else wanted.

That last line is the safety net: **there is no fourth case**. Nothing dropped into a pack
is ever destroyed, refused, or lost — at worst it lands in Loose.

## The compartments that ship

Eight are always there. Two more appear when you fit their trinket.

| Compartment | Claims | Needs |
|---|---|---|
| **The Catch** | Fish, fish buckets, the rod, kelp, lily pads, nautilus shells | Angler's Creel |
| **Food** | Anything edible, plus `#c:foods` and Pantrywork's food tags | — |
| **Combat** | Weapons, armour, arrows, shields, totems | — |
| **Charts & Bearings** | Maps, compasses, clocks, the spyglass, books | Cartographer's Sleeve |
| **Tools & Utility** | Tools, shears, flint and steel, leads | — |
| **Ores & Valuables** | Ores, ingots, raw materials, gems, nuggets, dusts, storage blocks | — |
| **Brewing & Alchemy** | Potions, nether wart, blaze powder, bottles, brewing reagents | — |
| **Nature & Farming** | Saplings, flowers, leaves, seeds, crops, bone meal | — |
| **Blocks & Building** | Everything else that's a placeable block | — |
| **Loose** | Everything no rule claimed | — |

That order is deliberate and a couple of entries earn a note:

- **The Catch sits above Food** — otherwise every cod you land files itself as rations.
- **Combat sits above Tools** — the community `c:tools` tag includes swords, so a weapon has
  to get first claim or it ends up with the pickaxes.
- **Charts sits above Tools** — a compass is tagged as a tool, and a compass in with your
  pickaxes is exactly the kind of small wrongness this mod exists to fix.
- **Blocks sits last of the real compartments** — it's the broad "is this a block" catch, so
  the specific compartments above it get their blocks first.

These rules are driven by **what an item is** (any mod's pickaxe is a Tool, any mod's stew is
Food) plus **item tags** — which means they cover modded items with no per-mod support and
no config. A packmaker can retune what counts as an ore or a food without touching code;
see [For packmakers](for-packmakers.md).

## Your own compartments

The **+** button on the rail makes one. Then, with the pack open:

| Do this | To |
|---|---|
| Hover an item, press **I** | Stamp the tab with that item's icon |
| Press **R** (or click Rename) | Name it |
| Middle-click the tab | Dye its leather |
| **[** and **]** | Slide it up or down the rail |
| Right-click the tab | Remove it (the contents just re-sort; nothing is lost) |

**The stamp is a live filter, not just a picture.** Stamp a tab with a pickaxe and it
gathers tools; stamp it with bread and it gathers food. That works on every custom
compartment, with no fitting required.

## Written rules — the Quill & Ledger

Want sharper filters than "things like this one"? Fit the **Quill & Ledger** and a quill
button appears under the grid of any custom compartment. It unfolds a parchment sheet where
you can write:

- **By name** — the item's name contains some text
- **By mod** — everything from a given mod id
- **By category** — a chip for Food, Tools, Weapons, Armor, Blocks, or Potions

Rules are a list; strike one off with the button on its row.

**The model, stated plainly:** written rules edit *and* match only while the Quill & Ledger
is fitted. Pull it out and they go to sleep — they are never deleted, and the compartment
falls back to its stamp and its pins. Put it back and everything wakes up exactly as you
left it. Pause, never punish.

## Pinning

A pin says "this lives here" and outranks every rule.

Three ways to make one, and they all do the same thing:

- **Just put it there.** Drop an item into a compartment its rules would *not* send it to,
  and the pack pins it there on the spot. A stitched parchment note names the tab so you
  know it happened.
- **Hover it and press P.**
- Press **P** again over a pinned item to release it.

A pinned item wears a small red ribbon. Tidy Up won't move it, a new rule won't steal it,
and re-sorting the whole pack leaves it exactly where you put it.

## The three tools across the title bar

- **Tidy Up** merges partial stacks and re-sorts the whole pack in one go.
- **Search** finds anything across every compartment at once.
- **Flatten** drops the whole pack into one grid for when you just want to rummage.

## Tidy vs. Keep my layout

Every compartment has an arrangement switch under the grid, next to the page count.

- **TIDY** (the default) — the pack arranges this compartment however it likes.
- **KEEP MY LAYOUT** — items stay in the exact cells you drop them in, and new arrivals fill
  the gaps. Tidy Up still works as a one-shot re-sort; the sorted order simply becomes your
  new starting layout, and the mode stays put.

The setting is per compartment, so you can keep a hand-arranged building kit next to a
food tab that looks after itself.

Under the hood a kept layout is strictly a **view**: it remembers "this cell shows that
backing slot" and nothing more. A stale entry can at worst draw in the wrong place for a
moment — it can never duplicate or lose an item.

## Pack-first pickup

With a **Lodestone Charm** fitted — carried or worn — anything you pick up that the pack can
already file goes straight in: something a compartment routes, something you've pinned, or
something the pack is already holding. Genuinely new finds still go to your pockets, so you
always see the thing you just discovered.

There's a toggle in the pack's title strip, per pack, and a server default in the config.

## Depth, and why nothing weird escapes

Higher tiers make each slot **deeper** — a Sculkhide slot holds 384 of a common item, six
whole stacks, under one tab. Depth lives entirely inside the pack. Anything that *leaves* —
your cursor, a hopper, a fitting drawing from stock — always comes out one legal vanilla
stack at a time, so the world outside never sees an impossible stack.
