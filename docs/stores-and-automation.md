# Stores & automation

[← back to the manual](README.md)

One pack, five kinds of cargo. Items are always there; the other four are **fittings you
craft and slot**, never on by default, and each shows on the right rail as a piece of gear
you can point at — a rack of waterskins, a soul vial, a copper-wound crystal, a harness of
alchemist's flasks. There is no tank-and-cable panel anywhere in this mod.

## The four stores

| Store | Fitting | Canvas → Sculkhide | Speaks |
|---|---|---|---|
| Fluid | Waterskin Rack | 8,000 → 48,000 mB | Your loader's standard fluid capability |
| Experience | Soul Vial | 5,000 → 30,000 points | Packwork's own (XP has no cross-mod standard) |
| Energy | Charge Crystal | 100,000 → 600,000 FE | Your loader's standard energy capability |
| Chemical | Alchemist's Flask Harness | 16,000 → 96,000 mB | Mekanism's chemical capability |

All eight numbers are packmaker-tunable per tier — see [Configuration](config.md).

### By hand

- **Waterskin Rack** — click the glass gauge with a bucket or a flask on your cursor to fill
  or empty it. It spends exactly one container and hands the result back to your cursor,
  then your pockets, then the pack. Nothing ends up on the floor.
- **Soul Vial** — click the green gauge to siphon your experience in, Shift-click to pour it
  back out. While it holds anything, it quietly mends your Mending-enchanted gear.
- **Charge Crystal** — fills from any charger and tops up the powered tools in your hands.
  Its transfer rate scales with its capacity, so a bigger pack also moves charge faster.
- **Flask Harness** — bottled vapors, filled by any Mekanism chemical pipe.

### Shrinking a store is safe

If a packmaker lowers a capacity below what someone is already carrying, nothing is
stranded and nothing is destroyed. The store simply stops accepting more and pays out
normally until it's under the new ceiling. Same for slot counts and depth: over-deep slots
draw down as they're used. Pause, never punish.

## Automation

**Set the pack down** and it becomes a block that anyone's automation can talk to, because
it speaks your loader's own capability APIs rather than a Packwork-specific one:

- **Hoppers** push into it and pull out of it, both loaders, no extra blocks.
- **Item pipes / conduits / cables** from any mod that targets the standard item capability
  work against it.
- **Fluid pipes** fill and drain the Waterskin Rack.
- **Energy cables** charge and discharge the Charge Crystal.
- **Mekanism chemical pipes** fill the Flask Harness (with Mekanism installed).

Three rules hold at that boundary no matter who's pushing:

1. **Per-slot depth on the way in.** A slot fills to the tier's depth, not to 64.
2. **One vanilla stack on the way out.** Every extraction is clamped to a legal stack, so no
   impossible stack ever leaves the pack.
3. **No nesting.** A pack refuses to accept another pack, by hand or by pipe.

## Cross-mod bridges

Every one of these is optional and simply absent when its mod isn't installed. There are no
hard dependencies, ever.

| Mod | What lights up |
|---|---|
| **JEI** | Every craft renders as a real recipe, tier upgrades included — the full nine-cell ring with the pack in the middle — plus info pages for every pack and fitting. |
| **Curios** (NeoForge) / **Trinkets** (Fabric) | The pack rides the back slot, keeps ticking there, opens with Shift-B, and renders on your back. |
| **Mekanism** | The Flask Harness becomes a real chemical tank. Without Mekanism the fitting has no recipe and stays off the creative shelf — no dead craftables. |
| **Forgework** | A placed pack's Charge Crystal charges straight off Flux cables (1 Flux = 1 FE), and the crystal tops up carried Forgework terminals. |
| **Pantrywork** | Its food tags fold into the Food compartment. |

**Version note.** Mekanism and Forgework ship no builds past Minecraft 1.21.1, so those two
gates only ever light up there. On every other version and on Fabric they simply stay dark.
On Fabric the Charge Crystal's energy face is **Team Reborn Energy**, the Fabric ecosystem's
standard, bundled inside the jar — not a mod you install. Same reservoir, same numbers:
1 E = 1 FE.
