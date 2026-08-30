# Packwork — the manual

A humble adventurer's pack that holds far more than it should, and quietly organizes
itself. This is the Outfitter's Handbook you can read without launching the game, plus the
two pages the in-game book can't hold: **every config key**, and **what a packmaker needs
to know**.

Leather, brass, canvas, glass vials. Never a circuit board.

## Start here

| Page | What's in it |
|---|---|
| [Sorting](sorting.md) | Compartments, the rules engine, pins, search, Tidy vs. Keep-my-layout. The reason the mod exists. |
| [Tiers & fittings](tiers-and-fittings.md) | The six-step material ladder, per-slot depth, and all 20 fittings with what each one actually does. |
| [Stores & automation](stores-and-automation.md) | Fluids, XP, arcane charge and bottled vapors — and how hoppers, pipes and cables talk to a placed pack. |
| [Configuration](config.md) | Every key in `packwork-server.toml` and `packwork-client.toml`: default, range, what it changes. |
| [For packmakers](for-packmakers.md) | What to turn off, the tags the sorter reads, and adding your own compartment categories from a datapack. |

## The sixty-second version

1. **Craft a Canvas Pack** — a chest wrapped in wool, corners tied with string.
2. **Right-click to open it**, or press **B** to open the first pack you're carrying without
   digging it out. Wearing one in a back slot? **B** finds it when your pockets hold no
   pack, and **Shift-B** opens the worn one outright.
3. **Drop things in.** Stamped leather tabs down the left are compartments, and a rules
   engine claims each item the moment it lands. A **Loose** tab catches anything no rule
   wanted, so nothing you drop in ever vanishes.
4. **Put an item where you want it to live.** Drop it into a compartment its rules wouldn't
   send it to and the pack pins it there on the spot. Pins beat every rule, always.
5. **Craft up the ladder** when 54 slots stop being enough — and craft **fittings** when you
   want the pack to carry water, experience, arcane charge, or do a job while you walk.

Nothing here needs power, and nothing here needs another mod.

## Two promises

**Pause, never punish.** A pack never voids what's inside it. Not on an upgrade craft, not
when you shrink a capacity in the config, not when you turn a fitting off. The single
exception is the Compass Rose, which only destroys the items you personally marked. If
something can't happen, it stops — it doesn't eat your cobblestone to get out of the
situation.

**Standard first, hard dependencies never.** The stores speak your loader's own capability
API, so anybody's pipes and cables work against a placed pack with no bridge block. Every
cross-mod feature — Mekanism, Forgework, Curios, Trinkets, JEI, Pantrywork — is optional
and simply absent when its mod isn't installed.

## Where to send a bug

[github.com/SapperSquad/Packwork/issues](https://github.com/SapperSquad/Packwork/issues), or
the [Discord](https://discord.gg/mZ9CG6xh2A). Bring the Minecraft version and loader,
Packwork's version, the other mods in the pack, and what you were doing when it went
sideways. A screenshot of the open pack is worth a paragraph.

Translations are machine-drafted first passes in ten languages and would love a native
speaker's eye — see the README's Languages section for how (it's one file and one line).
