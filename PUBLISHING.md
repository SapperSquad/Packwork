# Packwork — publishing kit (current as of v0.1.0 "First Haul")

Everything needed to update the Modrinth / CurseForge **profile page** for a release.
**The rule:** `CHANGELOG.md`, `PUBLISHING.md`, and `README.md` get bumped in the same
pass — never one alone. The store page is the only thing most players read.

> **Not yet published.** Uploading to Modrinth / CurseForge / GitHub is Alex's call, per
> release. This file is the ready-to-paste copy; nothing here goes external automatically.

Art will live in `assets/` (icon, banner, gallery cards) — not drawn yet. Real in-game
shots already exist from the dev harness (`run/client/screenshots/packwork_*.png`); use
those as the gallery until promo cards are made.

**Files to upload — ADD as new versions; do not delete older ones.**

| Upload as version | File | Game version tag | Loader |
|---|---|---|---|
| `0.1.0+mc1.21.1` | `build/libs/packwork-0.1.0.jar` | 1.21.1 | neoforge |

**Migration notes for this release:** none — first release.

---

## Summary (the short-description field)

> An adventurer's pack that sorts itself: tabbed compartments, a rules engine, and
> trinkets for magnet, repair, and even a fluid tank. Leather and brass, never tech.

---

## Project description (paste into the body)

# The pack that packs itself

You know the drill: twenty stacks of loot, one backpack, and five minutes of dragging
things into rows. Packwork does that part for you. Drop an item in and it *goes
somewhere* — stamped leather tabs down the side are compartments, and a rules engine
claims every item for the right one the instant it lands. A Loose tab catches anything
no rule wanted, so nothing ever disappears.

## Sorting is the soul

- **Auto-tabs out of the box** — Food, Combat, Tools, Ores & Valuables, Brewing, Nature,
  Blocks, and Loose. They sort by what an item *is*, so a modded pickaxe is a Tool for
  free — no config, no per-mod support.
- **Make your own** — name a compartment, stamp it with any item icon, dye it, drag to
  reorder. **Pin** an item and it always lands there.
- **Tidy Up, search, flatten** — merge and re-sort with one button, find anything across
  every tab, or collapse it all into one grid when you just want to rummage.

## One pack, many fittings

Craft it up the ladder — Canvas, Leather, Studded, Reinforced, Runed — and upgrading a
full pack carries everything inside straight up. Then slot brass **trinkets** into it:

| Fitting | |
|---|---|
| Lodestone Charm | pulls loose items nearby into the pack |
| Restock Strap | refills your hotbar from pack stock |
| Repair Kit | slowly mends worn and held gear |
| Bottomless Lining | more slots, never voided |
| Compass Rose | the only way a pack throws anything out — and it's opt-in |
| Waterskin Rack | a real fluid tank, shown as a glass gauge |
| Soul Vial | stores your XP and auto-mends your gear from it |

## Needs nothing, plays with everything

Zero hard dependencies. The pack speaks NeoForge's own item and fluid capabilities, so
hoppers and pipes from any mod feed a placed pack with no special support. Pantrywork
food tags fold in when it's installed, and are ignored when it isn't.

*More stores are coming — a Charge Crystal for arcane charge and an Alchemist's Flask
Harness for bottled vapors — each a fitting you can see, never a tank-and-cable panel.*

---

## Gallery upload plan

| File | Caption |
|---|---|
| `banner.png` (featured, TODO) | "The pack that packs itself." |
| `packwork_tabs.png` (real shot) | The tab rail, mid-sort, with a fluid gauge on the side. |
| `packwork_combat.png` (real shot) | One compartment: weapons and armor, claimed automatically. |
| `packwork_search.png` (real shot) | Search "iron" — flattened to just what matches. |
| `card_trinkets.png` (TODO) | The six working fittings. |

**Art carrying version-specific claims** (trinket counts, resource-store list, MC
version): keep it in step with the changelog. Right now **fluids and XP** ship among the
resource stores (energy and gas don't yet) — don't let a banner claim four.

---

## Store voice

Playful, forge-y, second person. Concrete over adjective, player consequence over
implementation — "the Loose tab catches anything no rule claims, so nothing vanishes"
beats "a fallback bucket handles unmatched predicates." Never call the trinkets modules
or the energy store a battery; hold the adventurer line.

See `CHANGELOG.md` for the per-version description block.
