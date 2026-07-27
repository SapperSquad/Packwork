# Packwork — publishing kit (current as of v0.1.0, unreleased)

Everything for the Modrinth / CurseForge project page, ready to paste. **The rule:**
`CHANGELOG.md`, `PUBLISHING.md`, and `README.md` get bumped in the same pass — never one
alone. The store page is the only thing most players read, and every claim in it must
match shipped behavior exactly.

> **Not yet published.** Uploading to Modrinth / CurseForge / GitHub is Alex's call, per
> release. Nothing here goes external automatically.
> **TODO (Alex): pick the release version number** — the project is at `0.1.0` internally;
> replace `<VERSION>` below when you choose the public one.

Promo art lives in `promo/`: the icon and banner are composed by `java tools/GenPromo.java`
from the real in-game sprites, and the gallery shots come from the dev-harness shoot
(`./gradlew runClient -Pgallery -Pjei`), inspected as pixels before use.
Icon = `promo/icon-512.png`. Featured banner = `promo/banner-1920x640.png`.

**Files to upload — ADD as new versions; do not delete older ones.**

| Upload as version | File | Game version tag | Loader |
|---|---|---|---|
| `<VERSION>+mc1.21.1` | `build/libs/packwork-0.1.0.jar` (name follows the version bump) | 1.21.1 | NeoForge |

**Migration notes for this release:** none — first release.

---

## Summary (the short-description field)

> An adventurer's pack that sorts itself: tabbed compartments, a rules engine, pins,
> and brass-fitted trinkets — craft on the go, drink from a waterskin, bank your XP.
> Leather and twine, never tech.

---

## Project description (paste into the body)

# The pack that packs itself.

You know the drill: twenty stacks of loot, one bag, and five minutes of dragging things
into rows. **Packwork** does that part for you. Drop an item in and it *goes somewhere* —
stamped leather tabs down the side are compartments, and a rules engine files every item
into the right one the instant it lands. A Loose tab catches anything no rule wanted, so
nothing you drop in ever disappears.

## 🎒 Sorting is the soul

- **Auto-tabs out of the box** — Food, Combat, Tools & Utility, Ores & Valuables,
  Brewing & Alchemy, Nature & Farming, Blocks & Building, and Loose. They sort by what an
  item *is*, so any mod's pickaxe is a Tool and any mod's stew is Food — no config, no
  per-mod support. Two more compartments (Charts & Bearings, The Catch) open up when you
  fit the trinket that earns them, and every category list is a datapack tag, so packs
  can retune them without code.
- **Make your own compartments** — name a tab, stamp it with any item's icon, dye the
  leather, reorder the rail. The stamp is a live filter: a pickaxe stamp gathers tools,
  a bread stamp gathers food.
- **Pin an item by putting it where you want it** — drop it into any compartment it
  wouldn't sort to and the pack pins it there on the spot (a stitched note tells you so).
  Pinned items wear a red ribbon and beat every rule. Hover + `P` toggles a pin too.
- **Every compartment picks its arrangement** — Tidy (the pack arranges it) or **keep my
  layout** (items stay in the exact cells you drop them; new arrivals fill the gaps;
  Tidy Up still re-sorts once when you ask).
- **Tidy Up, search, flatten** — merge and re-sort with one button, find anything across
  every tab, or collapse it all into one grid when you just want to rummage.

## ⛏️ It files what you mine

Fit a **Lodestone Charm** and the pack doesn't just magnet nearby drops — it takes over
your pickups. Anything you pick up that the pack knows where to put (it sorts to a
compartment, it's pinned, or the pack already holds some) goes **straight into the pack**,
filed under the right tab, while new, unknown finds still land in your pockets. Mine a
vein of cobble and your inventory stays clean. A small switch on the pack turns
pack-first pickup off per pack.

## 🧳 Six tiers, and the slots go DEEP

Canvas → Leather → Studded → Reinforced → Runed → **Sculkhide**. Every tier is crafted as
a full **ring around the pack before it** — its hide or plating on the edges, its
fittings on the corners: copper buckles, iron studs, gold with diamonds, diamond bound in
netherite, and finally amethyst cornered with echo shards from the Deep Dark. The upgrade
carries **everything** inside straight up — contents, compartments, pins, trinkets, name,
every store. No recipe in this mod can eat a filled pack.

And every step cuts the slots **deeper**: one vanilla stack per slot on Canvas, up to
**six stacks in a single slot** (384 cobble) on Sculkhide — filed under one tab, while
your cursor, hoppers, and fittings only ever pull legal stacks back out.

## 🔧 Brass sockets, eighteen trinkets

Craftable fittings slot into the brass rail — and pull back out any time:

| Fitting | |
|---|---|
| Lodestone Charm | the magnet + pack-first pickup: it files what you mine |
| Restock Strap | refills your hotbar from pack stock |
| Repair Kit | slowly mends worn and held gear |
| Bottomless Lining | more slots, never voided |
| Compass Rose | the only way a pack throws anything out — opt-in, per item |
| Quick-Draw Straps | a tool breaks in your hand, the pack hands you another |
| Quill & Ledger | the rule editor — write your own filters (name, mod, category) per compartment |
| Tinker's Kit | a leather tool roll unrolls inside the pack: a 3×3 bench fed from your stores, with a parchment **Recipe Ledger** of everything craftable from pack stock |
| Field Furnace | banked embers cook raw ore and raw food as you walk |
| Provisioner's Pouch | feeds you before hunger bites, plainest rations first |
| Cartographer's Sleeve | opens Charts & Bearings for maps, compasses, clocks |
| Angler's Creel | opens The Catch — your catch lands straight in the pack |
| Torchbearer's Loop | sets a torch from pack stock when you stand in the dark |
| Herbalist's Bundle | replants harvested crops from your own seed stock |
| Waterskin Rack | a real fluid tank, shown as a glass gauge |
| Soul Vial | banks your XP, pours it back, auto-mends Mending gear |
| Charge Crystal | an arcane charge in copper-wound glass — standard FE, tops up your powered tools |
| Alchemist's Flask Harness | bottled vapors — a chemical tank (with Mekanism) |

New to the pack? Craft the **Outfitter's Handbook** — five in-game chapters on sorting,
trinkets, tiers, and the stores, with every number pulled live from the code.

## 🏕️ Set it down, pipe it up

Sneak-right-click to stand a pack in the world — leather and brass, wearing its tier's
own trim (twine, studs, plates, glowing runes, or the Sculkhide's echo-lit hide). Break
it and you get the pack back with everything still inside. A placed pack speaks
NeoForge's own item, fluid, and energy capabilities, so hoppers, pipes, and cables feed
it with no bridge block — and piped-in items auto-file into the right compartment,
because the sorting is virtual over one store.

## 🤝 Needs nothing, plays with everything

**Zero hard dependencies.** Every cross-mod touch lights up only when its mod is present:

- **JEI** — every craft renders as a real recipe (the tier rings included), plus info
  pages for every pack, trinket, and the handbook.
- **Curios** — wear the pack in the back slot; its trinkets keep working worn.
- **Mekanism** — the Flask Harness becomes a real chemical tank its pipes can fill.
- **Forgework** — Flux cables charge a placed pack, 1 Flux = 1 FE, and the Charge
  Crystal tops up carried Forgework terminals.
- **Pantrywork** — its food tags fold into the Food tab.

Remove any of them and Packwork carries on without it.

## ⚙️ Requirements & honest notes

- **1.21.1 / NeoForge** (21.1.235+). No other dependencies, ever. Other loaders and
  versions may follow based on demand.
- Opening a worn pack's GUI straight from the Curios slot isn't wired yet — open it from
  your inventory (its trinkets work fine while worn).
- The pack never voids anything on failure. The only trash path is the Compass Rose,
  and it's opt-in per item.

---

## Gallery upload plan

| File | Caption |
|---|---|
| `promo/banner-1920x640.png` (featured) | The pack that packs itself. |
| `promo/gallery-1-lineup.png` | The six-tier ladder, set down side by side — Canvas to Sculkhide, each wearing its own trim. |
| `promo/gallery-2-sorting.png` | The pack, open: stamped leather compartments, brass fittings, glass gauges — mid-sort. |
| `promo/gallery-3-ledger.png` | The Tinker's Kit tool roll and its Recipe Ledger — a chalked recipe, ready to lay out from pack stock. |
| `promo/gallery-4-rules.png` | The Quill & Ledger's rule editor: write your own filters, strike them off — pins always win. |
| `promo/gallery-5-keep.png` | Keep-my-layout: your arrangement, held — new arrivals fill the gaps. |
| `promo/gallery-6-pickup-pin.png` | Drop an item where you want it and it pins there — and the Lodestone files what you mine. |
| `promo/gallery-7-sculkhide-night.png` | A Sculkhide pack set down at night, its echo-gem lighting the camp. |
| `promo/gallery-8-jei-ring.png` | Every upgrade is a full ring around the pack — shown in JEI exactly as you place it. |
| `promo/icon-512.png` (project icon) | — |

**Art carrying version-specific claims** (trinket counts, tier list, MC version): keep it
in step with the changelog. All four resource stores ship (fluids, XP, energy always; gas
with Mekanism); the mod-gated integrations (gas, Curios, JEI, Forgework, Pantrywork)
light up only with their mod, so don't imply they work standalone.

---

## Changelog for the first release (paste into the upload)

> **<VERSION> — First Haul.** The pack that packs itself: a tabbed, self-sorting
> adventurer's pack for NeoForge 1.21.1.
> - Tabbed, rule-driven sorting: eight auto-tabs, two trinket-gated compartments, custom
>   tabs with live stamps, drop-to-pin, per-compartment Tidy / keep-my-layout, Tidy Up,
>   search, and flatten.
> - Six material tiers, each crafted as a preserving ring around the previous pack —
>   nothing inside is ever lost — with per-slot depth up to six stacks a slot on
>   Sculkhide.
> - Eighteen brass-socket trinkets, including pack-first pickup (the Lodestone files
>   what you mine), the Tinker's Kit bench with its Recipe Ledger, and the Quill &
>   Ledger rule editor.
> - Four resource stores as gear, never tech: waterskin fluids, soul-vial XP,
>   charge-crystal FE — and bottled vapors with Mekanism.
> - Placeable, automatable packs speaking standard NeoForge capabilities.
> - Soft-dep integrations: JEI, Curios, Mekanism, Forgework, Pantrywork. Zero hard
>   dependencies.
> - The Outfitter's Handbook: an in-game guide whose numbers can't go stale.

The full internal history lives in `CHANGELOG.md`.

Categories: `storage`, `game-mechanics`, `adventure`. License: Alex's call at publish.

---

## Store voice

Playful, forge-y, second person. Concrete over adjective, player consequence over
implementation — "the Loose tab catches anything no rule claims, so nothing vanishes"
beats "a fallback bucket handles unmatched predicates." Never call the trinkets modules
or the energy store a battery; hold the adventurer line.
