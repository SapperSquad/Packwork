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

## Version ports — the reach campaign (ALL THREE WAVES DONE, 2026-08-13)

Downloads track version×loader coverage, not quality (measured: Pantrywork 519 @ 13
combos vs Packwork 32 @ 1), so 1.0.0 is being spread wide. **Wave 1: NeoForge
1.21.8 / 1.21.10 / 1.21.11 — DONE. Wave 2: NeoForge 26.1.2 / 26.2 — DONE.
Wave 3: Fabric 26.1 / 26.2 — DONE** (incl. the 26.2 stretch). Master stays the
1.21.1 line; each port lives on its own branch. Do NOT restructure to multiloader
yet (coordinator's call for this campaign; re-argued and upheld in the fabric
branches' DECISIONS — the parallel branch was cheap precisely because 26.x is
unobfuscated, and it kept six green NeoForge branches untouched).

### Branch layout and toolchain pins

| Branch | MC | Loader | Parchment | JEI | Wear | Jar |
|---|---|---|---|---|---|---|
| `master` | 1.21.1 | NeoForge 21.1.235 | 2024.11.17 | 19.21.1.312 | Curios 9.5.1+1.21.1 | `packwork-1.0.0.jar` |
| `port/1.21.8` | 1.21.8 | NeoForge 21.8.54 | 2025.09.14 | 24.2.0.6 | Curios 12.0.0+1.21.8 | `packwork-1.0.0+mc1.21.8.jar` |
| `port/1.21.10` | 1.21.10 | NeoForge 21.10.64 | 2025.10.12 | 26.3.0.31 | Curios 13.0.0+1.21.10 | `packwork-1.0.0+mc1.21.10.jar` |
| `port/1.21.11` | 1.21.11 | NeoForge 21.11.45 | 2025.12.20 | 27.23.0.71 | Curios 14.0.0+1.21.11 | `packwork-1.0.0+mc1.21.11.jar` |
| `port/26.1` | 26.1.2 | NeoForge 26.1.2.95 | — (unobf) | 29.22.0.73 | Curios 15.0.0+26.1.2 | `packwork-1.0.0+mc26.1.2.jar` |
| `port/26.2` | 26.2 | NeoForge 26.2.0.59 | — (unobf) | 30.20.0.154 | Curios 16.0.0+26.2 | `packwork-1.0.0+mc26.2.jar` |
| `fabric/26.1` | 26.1 (`~26.1`) | Fabric 0.19.3 + API 0.155.2+26.1.2 | — (unobf) | 29.22.0.73 | Trinkets Upd. 4.0.0-beta.3+26.1 | `packwork-1.0.0+mc26.1-fabric.jar` |
| `fabric/26.2` | 26.2 (`~26.2`) | Fabric 0.19.3 + API 0.157.0+26.2 | — (unobf) | 30.20.0.154 | Trinkets Upd. 4.1.0-beta.3+26.2 | `packwork-1.0.0+mc26.2-fabric.jar` |

All six NeoForge branches: NeoGradle userdev 7.1.38 (unchanged — the official MDKs
through 26.2 still use it), Gradle 9.6.1. JDK: 21 through 1.21.11; **Java 25 from
26.1** (Mojang ships 25; the foojay resolver already in settings.gradle
auto-provisioned Adoptium 25 into `~/.gradle/jdks` — nothing to install by hand, the
house daemon-JDK worry didn't bite on NeoForge). Parchment ends at 1.21.11: 26.x is
unobfuscated, nothing to map. NeoForge 26.x versioning: the first THREE components
are the MC version (26.1.2.95 = MC 26.1.2); the 26.2 line is beta-numbered on the
maven but the artifact id carries no suffix. **Mekanism and Forgework ship no builds
past 1.21.1** (re-checked modmaven 2026-08-13), so on every port branch those two
gates simply never light; their compat classes still compile against the pinned
1.21.1 API jars. Port branches carry `version = "${mod_version}+mc${minecraft_version}"`
in `build.gradle` and the datagen run is `runClientData` (NeoForge 21.4+ split datagen).

The two `fabric/*` branches: non-remapping **`net.fabricmc.fabric-loom` 1.17.19**
(the DejaView/Blockives house recipe — no mappings line, plain `implementation`, no
remapJar), Gradle 9.6.1 (same wrapper), Java 25 toolchain/release with the DAEMON
pinned to 26 via `gradle/gradle-daemon-jvm.properties` (Loom rejects 26.x on an
older daemon — the house worry DID bite on Fabric). Team Reborn Energy 5.0.0
jar-in-jar'd (`include`); Trinkets Updated + JEI compileOnly with `-Ptrinkets` /
`-Pjei` runtime flags; Mekanism/Forgework compat classes DELETED there (no Fabric
artifact exists to compile against — gas gates dark, Flask Harness recipe
`fabric:load_conditions`-gated, creative tab hides the fitting).
Bar per branch: `compileJava` clean, (`runClientData` clean on NeoForge; Fabric has
no datagen — the generated resources are committed), **58/58 gametests (57 packwork
+ vanilla's always_pass) × plain / -Pcurios / -Pjei -Pcurios** (Fabric: × plain /
`-Ptrinkets` / `-Ptrinkets -Pjei`, on fabric-api's runner: `./gradlew runGametest`),
jar built with the right name AND the full version inside its
`neoforge.mods.toml` / `fabric.mod.json`. The 1.21.11, 26.1, and fabric/26.1
branches had the full GUI verified as pixels via `-Pautoshot` (26.2 and fabric/26.2
got the spot-check: pack GUI + placed trim + on Fabric the JEI ring, identical to
their 26.1 siblings' sets).

### The drift map, 1.21.11 → 26.2 (wave 2's findings — wave 3 starts from this)

Wave 2 was far lighter than wave 1 (82 compile errors to 26.1.2, then 36 to 26.2)
because port/1.21.11 already carried the 26.x-era foundations. What actually moved:

- **26.1 vanilla, the big one — `ItemContainerContents` rebuilt on
  `ItemStackTemplate`, and every stack-shaped read now runs `ItemStack.validateStrict`,
  which NULLS any count past the item's own max stack size.** A 384-deep Sculkhide
  slot stored fine and read back EMPTY — five depth gametests caught it (the compile
  was green; only the suite saw it). The store now rides Packwork's own holder,
  **`pack/PackContents`** (same shape, no validation on read, template wire codec with
  raw VarInt counts); `DeepContentsCodec` kept the exact serialized form — 1.21.x-era
  saves read intact, the pre-depth legacy fallback still works, and 26.1's
  `ItemStackTemplate.MAP_CODEC` omits count-1 so the item field is byte-identical to
  the old `SINGLE_ITEM_CODEC` shape.
- **The native-transfer rewrite (the wave-2 mandate) landed with it.** `PackInventory`
  IS the transactional `ResourceHandler` now — one implementation of the three rules
  (per-slot DEPTH via `getCapacity`, NESTING refusal via `isValid`, ONE-VANILLA-STACK
  extract via the `extract` clamp) shared by the standard capability, the menu,
  trinkets, and sorting; `PackTransfer.PackItemHandler` and `LiveComponentHandler` are
  gone. The legacy-shaped conveniences (`insertItem`/`extractItem`/`getStackInSlot`/
  `setStackInSlot`) each run one root `Transaction` over the native path, so the
  gametest bodies stayed word-for-word and green means the same thing. The menu binds
  a live `ItemAccess` per host — `forPlayerSlot` (carried) / `VanillaContainerWrapper`
  over the host container (placed, worn, client mirror) — commits restore the
  component patch onto the ORIGINAL stack instance and fire `setChanged`, which is
  exactly the old live-supplier contract. Trinket sockets ride `ResourceHandlerSlot`;
  the waterskin gauge click is a native two-way `ResourceHandlerUtil.move` on a
  one-count copy in its own holder (one container per click, conservation intact).
  **Trap for any ItemAccess-handler subclass:** `ItemAccessItemHandler` captures its
  access item at CONSTRUCTION (`validItem`) — a client menu builds a tick before its
  host stack syncs, so the capture is AIR and every read is dead forever (the Phase-1
  resolve-live lesson in native clothing; it showed as empty trinket sockets in the
  autoshot). Override `getResourceFrom`/`getAmountFrom`/`isValid` to re-derive.
- **26.1 vanilla, mechanical:** `GuiGraphics` → `GuiGraphicsExtractor` (same package;
  draw verbs renamed: drawString→`text`, drawCenteredString→`centeredText`,
  renderOutline→`outline`, renderItem→`item`, renderFakeItem→`fakeItem`,
  renderItemDecorations→`itemDecorations`, drawWordWrap→`textWithWordWrap`; `blit`/
  `blitSprite`/`fill`/scissor/pose survive). Screens: `render` →
  `extractRenderState`, `renderBg` is structurally DEAD — container panels draw in
  `extractBackground` (own stratum, composed by the frame; the old
  renderBg-only-fires-from-renderBackground trap can't exist), `renderLabels` →
  `extractLabels`, widgets `renderWidget` → public `extractWidgetRenderState`;
  `imageWidth/Height` are final ctor args. **The gui renderer honours ALPHA now: a
  0xRRGGBB color renders fully transparent** (the pack title and page indicator
  vanished; always 0xFFRRGGBB). `ClickType` → `ContainerInput` (same constants);
  `handleInventoryMouseClick` → `handleContainerInput`. `RecipeSerializer` is a
  RECORD of (codec, streamCodec); `assemble` lost its registry arg;
  `group()`/`showNotification()` went abstract; `SlotDisplay.ItemStackSlotDisplay`
  takes `ItemStackTemplate`, `ItemSlotDisplay` takes `Holder<Item>`.
  `registerItem/Block` third arg is a `Supplier`/`UnaryOperator` of Properties.
  `nonEmptyStream` → `nonEmptyItemCopyStream`; `getCraftingRemainder` returns
  nullable `ItemStackTemplate`; `ItemStack.SINGLE_ITEM_CODEC` gone (template map
  codec is the drop-in). **Constructing an `ItemStack` in a static initializer now
  throws "Components not bound yet"** (components bind to the registry holder after
  freeze; `@EventBusSubscriber` classes classload during scan) — build such stacks
  lazily. Fluid client info moved off `IClientFluidTypeExtensions` onto `FluidModel`
  (`modelManager.getFluidStateModelSet().get(state)` → `stillMaterial().sprite()` +
  `fluidTintSource().colorAsStack(stack)`). World time rides the CLOCK system
  (`server.clockManager().setTotalTicks(overworldClock, t)`); `LevelSettings` folds
  difficulty/hardcore into `DifficultySettings` and dropped GameRules;
  `Minecraft.resizeDisplay` → `resizeGui`. NeoForge: `BlockEvent.BreakEvent` →
  `event.level.block.BreakBlockEvent` (fires both sides; guard on ServerPlayer).
- **26.2 (from 26.1) was ONE family:** the screen layer moved onto `Gui` —
  `mc.screen` → `mc.gui.screen()`, `mc.setScreen` → `mc.gui.setScreen`,
  `Options.hideGui` → `mc.gui.hud.toggle()`/`isHidden()`, `getMainRenderTarget` →
  `gameRenderer.mainRenderTarget()`. Nothing else; zero storage/menu code changed.
- **The deprecated legacy transfer layer still SHIPS in both 26.1.2.95 and 26.2.0.59**
  (checked the class lists) — wave 1's "26.x removes it" expectation has not landed
  yet. Packwork no longer cares: main code is native; the only deprecated-API riders
  left are the gametest assertion views (`IItemHandler.of` et al), PackFluidHandler's
  FluidAction convenience overloads (kept so test bodies stay word-for-word), and the
  dormant 1.21.1-only compat classes.

### Wave 3 (Fabric) — DONE 2026-08-13; how the prediction held, and what it missed

The shape-analysis above held exactly: `PackContents` ported verbatim, `PackInventory`
is the three rules on a `SlottedStorage<ItemVariant>` over `ContainerItemContext`
(one wrinkle: the native face is a NESTED view, because `SlottedStorage`'s default
`getSlots()` collides with the legacy-shaped convenience of the same name), and the
conveniences run root `Transaction`s so the menu/trinkets/sorting/test bodies stayed
intact. `transfer/LiveStackStorage` is the `ItemAccess.forStack` analogue — in-place
replace-the-patch write-through over the live stack. Fluid stays **millibuckets in the
`pack_fluid` component** (own `PackFluidContent`, same serialized shape as
SimpleFluidContent); Fabric droplets (81/mB) exist only at the transfer face, whole-mB
moves only. Energy is **Team Reborn Energy jar-in-jar** over the same `pack_energy`
ints, 1 E = 1 FE (`transfer/PackEnergyFace`, the SimpleEnergyItem pattern with our
component and tier caps). Wear is **Trinkets Updated** (Patbox fork — the original
Trinkets ends at 1.21.1, Accessories at 1.21.10; the fork `provides: trinkets`, which
is what every gate checks) in the built-in `chest/back` slot;
`compat/trinkets/TrinketsCompat` mirrors CuriosCompat's surface method for method.

**Mixins — exactly three, plus one classtweaker line** (Fabric has no event for these):
`ItemEntityMixin` (playerTouch HEAD → pack-first pickup), `FishingHookMixin` (redirect
the loot roll → Angler's Creel), `LivingEntityMixin` (onEquippedItemBroken TAIL →
Quick-Draw; break-only — the NeoForge used-to-nothing refill has no Fabric hook,
documented narrowing). The classtweaker opens `ShapedRecipe.pattern` for the tool
roll's shaped-grid arrangement (private in pure vanilla; NeoForge patches it).

**What the prediction missed — every one found by the SUITE or the AUTOSHOT, none by
the compile** (the reason those bars exist):
- `LiveStackStorage` must snapshot a COPY — `SingleStackStorage` snapshots the
  instance (safe for its own reference-swapping ops, corrupt for in-place
  write-through), so rollbacks restored nothing: the Field Furnace's simulated
  room-check doubled output and `StorageUtil.move`'s aborted probe-extract drained
  the waterskin. Two gametests caught it on the first run.
- Trinkets Updated reads entity→slot bindings ONLY from the `trinkets` namespace
  (`data/trinkets/entities/*.json`, verified in the loader bytecode) and ships none
  itself — without our `packwork_back.json` binding player→chest/back, NO player
  slots exist at all. The pack tag rides at `data/trinkets/tags/item/chest/back.json`.
- JEI-Fabric discovers plugins via the **`jei_mod_plugin` entrypoint**, not the
  annotation scan (entrypoints are lazy — the one-class gate still holds), AND its
  crafting index reads the client's synced RecipeMap, where fabric-api's recipe sync
  is **opt-in per serializer**: `RecipeSynchronization.synchronizeRecipeSerializer`.
  Without both, the ladder rendered as info pages but never as real recipes — the
  wave-4 failure in Fabric clothes. The autoshot logs the greppable probe
  `crafting index holds N pack-upgrade recipes` (want 5).
- Pure-vanilla drift from the NeoForge-patched view (these ARE compile-visible):
  `recipeMap()`→`getRecipes()`, `ItemStack.getBurnTime`→`FuelValues.burnDuration`,
  `renderSlotContents`→an `extractSlot` override, `getGuiLeft/Top`→own accessors,
  `Screen.hasShiftDown`→a GLFW poll (Shift-B rides it; Fabric has no key-modifier
  system, OPEN_WORN ships unbound), `ItemContainerContents.getSlots`→copyInto
  counting, `onDataPacket`/`handleUpdateTag`→a dual-shape `loadAdditional` (vanilla
  applies BE update packets through `loadWithComponents`), `invalidateCapabilities`→
  nothing (lookups re-derive), `BlockEntityType` ctor private→
  `FabricBlockEntityTypeBuilder`, `CreativeModeTab.builder()`→`FabricCreativeModeTab`.
- Fabric API's own transfer mixins patch HOPPERS onto the lookups, so a placed pack
  stays hopper-automatable with zero extra code — full pillar-3 parity.
- `fabric/26.2` was the wave-2 26.2 screen family re-applied verbatim + pins;
  compiled first try, suite green first try.

**EMI status (honest):** EMI ships no 26.x build at all (checked Modrinth
2026-08-13), so there is nothing to be compatible WITH yet. When it arrives: the
upgrade recipes now sync to clients and carry vanilla `display()`s, so any viewer
reading the client RecipeMap can index and draw them; the positioned-ring extension
itself is JEI-only, and no native EMI plugin was built (campaign call).

**Maintenance story across 8 branches:** master (1.21.1) is the feature line;
`port/*` are MC-drift-only; `fabric/*` are loader-plumbing-only. A feature lands on
master, sweeps the ports with the drift maps below, then crosses to Fabric by
touching only the loader surface (the reg/net/capabilities/client-plumbing files +
the three mixins — the full list is the fabric branches' DECISIONS entry). The
storage internals, sorting engine, GUI drawing, recipes, and store math are shared
text on every branch. If Fabric targets multiply or FEATURES (not plumbing) start
drifting between loaders, that is the evidence gate for revisiting multiloader.

### The drift map, 1.21.1 → 1.21.11 (what broke where — wave 2 starts from this)

Ported newest-first: master → `port/1.21.11` was the big climb (~330 compile errors),
then swept backward (`port/1.21.10` from it: 3 drift categories; `port/1.21.8` from
that: revert the 21.9-era changes). By era:

- **1.21.2 (recipes/consumables):** recipe JSON ingredients are plain strings
  (`"minecraft:string"` / `"#minecraft:wool"`), not `{"item": ...}` objects — ALL 25
  recipe JSONs converted (the loader drops old-shape files SILENTLY; the
  craftability-sweep gametest is what caught it). `Recipe#getIngredients` /
  `canCraftInDimensions` / `getResultItem` are gone → `placementInfo()` + `display()`
  (PackUpgradeRecipe carries both; the JEI-validator gametest now pins the new
  contract). Recipe lookups key by `ResourceKey<Recipe<?>>`. **Clients stopped
  receiving recipes** → the Recipe Ledger's craftable scan + chalk arrangement moved
  SERVER-side (new `LEDGER_REFRESH`/`REQUEST_GHOST` verbs answered by
  `LedgerSyncPayload`/`GhostSyncPayload`; the client is pure paint again, and
  `PackMenu.arrangeOn3x3` is still the one shared arrangement helper). Fuel times
  live on `level.fuelValues()`; `InteractionResultHolder` folded into
  `InteractionResult`; `BlockEntityType.Builder` → plain constructor;
  `DirectionProperty` → `EnumProperty<Direction>`.
- **1.21.4 (client items):** every item needs `assets/packwork/items/<id>.json`
  (25 added, plain `minecraft:model` wrappers).
- **1.21.5 (items/gametests):** SwordItem/DiggerItem/ArmorItem classes dissolved →
  `PredicateKind` reads components now, tuned to keep SapperSquad's routing (axes carry
  WEAPON but stay Tools; swords carry TOOL but stay Combat via `#minecraft:swords`;
  armor = humanoid slot + attribute modifiers so pumpkins/elytra don't file as
  plate). Consume effects moved off FoodProperties onto CONSUMABLE (Provisioner).
  `appendHoverText` takes TooltipDisplay + Consumer. **GameTests are registry
  entries** → `@PackTest` annotation + `PackworkTestRegistrar` (scans into
  `TEST_FUNCTION`) + one generated `data/packwork/test_instance/<name>.json` per test
  (`tools/GenTestInstances.java` regenerates; run it after adding/renaming a test).
- **1.21.6 (GUI/storage):** the render pipeline — `blit`/`blitSprite` take a
  `RenderPipeline`, `pose()` is a 2D `Matrix3x2fStack` (no z-translates; layering is
  submission order), tooltips are `set*TooltipForNextFrame`, `setShaderColor` is gone
  (tints ride the draw calls). BlockEntity save/load is `ValueOutput`/`ValueInput`.
  `Screenshot.grab` grew a downscale arg.
- **1.21.9 (transfer/input):** **the capability rework** — legacy
  `Capabilities.ItemHandler/FluidHandler/EnergyStorage` tokens REPLACED by
  `Capabilities.Item/Fluid/Energy` on the transactional transfer API
  (`ResourceHandler`, `ItemAccess`, `Transaction`); official word: a legacy handler
  CANNOT be wrapped into a ResourceHandler, only the reverse (`IItemHandler.of`).
  Packwork's answer on 1.21.10/11: `transfer/PackTransfer.java` — the ONE
  version-specific file — extends NeoForge's `ItemAccessItemHandler` et al with the
  pack's three rules (per-slot DEPTH capacity, nesting refusal, one-vanilla-stack
  extract) while the menu/trinkets/sorting keep the battle-tested legacy-shaped
  internals (`PackInventory` — deprecated interfaces, still shipped). Screen input
  became event records (`MouseButtonEvent`/`KeyEvent`), `Inventory.selected/items`
  went private, `PacketDistributor.sendToServer` → `ClientPacketDistributor`,
  KeyMapping categories became typed, TriState vanillafied, `serverLevel()` folded
  into a covariant `level()`.
- **1.21.10 quirks:** `renderOutline` is `submitOutline` on this one version only;
  GameTestHelper lost the String assert overloads (they return in 1.21.11) —
  `gametest/PackHelper` (a thin GameTestHelper subclass, 1.21.8/10 branches only)
  carries them so the 57 test bodies stay word-for-word identical on every branch.
- **1.21.11:** `ResourceLocation` RENAMED `Identifier` (`ResourceKey.location()` →
  `identifier()`); `GameRules` moved to `world.level.gamerules`.

**Deprecation clock for wave 2 (26.x):** the whole legacy transfer layer
(`IItemHandler`, `ComponentItemHandler`, `FluidHandlerItemStack`, `IEnergyStorage`,
old `FluidUtil`) is deprecated-for-removal from 21.9 — Packwork's internals
(`PackInventory`/`LiveComponentHandler`, `PackFluidHandler`, `PackEnergyStorage`,
`PackMenu`'s FluidUtil gauge path, `ItemHandlerCopySlot`) still ride it everywhere.
Expect 26.x to REMOVE it: wave 2's big-ticket item is rewriting the internal store
handlers natively on the transfer API (PackTransfer is the template; the menu slots
have `ResourceHandlerSlot` waiting). Likely also gone by 26.x: the compatibility
String overloads and other 1.21.x grace shims. Everything else (recipes, ledger sync,
client items, test registry, GUI pipeline) is already on the 26.x-era foundations.

## Status — 1.1.0 on all eight branches, awaiting SapperSquad's upload

> Newest first. Full source map and roadmap below. Version is **1.1.0** (stamped
> 2026-08-30; jar = `packwork-1.1.0.jar`); the upload itself is SapperSquad's, from
> `PUBLISHING.md`. **66 GameTests green** (plain, `-Pcurios`, and
> `-Pcurios -Pjei -Pmekanism -Pforgework` combined); `runData` and the full jar build
> clean; the built jar's own `neoforge.mods.toml` reads `version="1.1.0"` and all
> eleven lang files are inside it (verified by extraction).
>
> **All eight branches now carry 1.1.0** (see the port-sweep table below): six NeoForge and
> both Fabric, each green on its own suite and each jar's metadata read back by extraction.
> The worn pack renders on every NeoForge build and has been looked at on each; the Fabric
> layer ships but has not been photographed — see "The Fabric worn render, and why it is not
> verified".

**2026-08-30 — the ADOPTION WAVE (1.1.0 "Field Kit"). Master done; ports pending.**
The wave that makes the mod easy to run, easy to tune, and easy to talk about. Nothing is
taken away and no existing pack changes. Six items, each its own commit:

1. **The worn pack renders on your back** (`client/WornPackLayer`, registered from
   `ClientSetup.addPlayerLayers` only when Curios is loaded; the worn stack is read through
   `CuriosCompat`, so the one-class gate holds). It reuses the per-tier **BLOCK model**, so
   every tier's trim carries onto the shoulders for free. It rides `PlayerModel.body`
   (crouch/swim/mount poses come along), steps aside for an elytra, hides with invisibility,
   and honours the client-only `show_worn_pack`.
   **The geometry, since it is fiddly:** after `body.translateAndRotate` the pose is in
   BLOCK units, y-DOWN, back at +z. `translate(0, 0.30, chest.isEmpty() ? 0.27 : 0.32)` then
   `Axis.XP.rotationDegrees(180)` maps block-space up→up and the block's NORTH face (the
   flap, which carries the trim) outward; `scale(0.50)` then `translate(-0.5,-0.5,-0.5)`
   centres it. The first pass (0.62 / y 0.36 / z 0.16) swallowed the whole torso and sat
   buried in the spine — **found only in the pixels**.
   **Verified as pixels** via a new `-Pwornshot` DevAutoShot chain (sky pad, third-person
   back and front, 1920x1080 at FOV 38, seven framed checks): Canvas and Sculkhide, over a
   diamond chestplate, crouching, from the front, under an elytra, and with the toggle off.
   Two harness traps found in the doing, both of which FAKE a pass: `Inventory.clearContent()`
   empties the armor row (clearing after equipping silently wiped the chestplate and the
   elytra), and a minimised dev window writes a 70-byte PNG that logs as a success — `grab()`
   now refuses below 64x64 and logs an error.
2. **Handbook chapter 6: Field Reports.** `HandbookContent` gained a `LinkEntry` to its
   sealed union plus `ISSUES_URL`/`DISCORD_URL` constants; the screen draws links in brass
   with a rule, brightening on hover, and routes clicks through vanilla's
   `ConfirmLinkScreen.confirmLinkNow` (the player sees the URL and says yes; it hands the
   book back either way). Entries are ordered so **both links land on the page the chapter
   opens on** — the first shoot put Discord behind a pager click.
3. **Ten machine-drafted locales** (zh_cn, ru_ru, pt_br, de_de, fr_fr, es_es, ja_jp, ko_kr,
   pl_pl, uk_ua), 133 keys each, every file carrying a `packwork.translation.status` line
   saying it has not had a native pass. New `tools/CheckLang.java` (Java-only) fails on a
   missing key, an orphan from a rename, and on a drifted `%s` count — the one translation
   mistake that crashes a screen. New `packwork.handbook.report` lang key on the Handbook
   item's tooltip so a translator can point players at the door that reaches the author;
   zh_cn's says GitHub/Discord and that the author cannot see MC百科 comments. **Verified as
   pixels**: the whole autoshot chain ran under `lang:zh_cn`, CJK renders in the pack GUI
   with no overflow. **Scope call left open for SapperSquad:** the Handbook's long prose stays
   English (see DECISIONS).
4. **`docs/`** — index, sorting, tiers & fittings, stores & automation, **every config key
   with default and range**, and a **for-packmakers** page (what to turn off, the exact tag
   list each compartment reads and the order they are checked in, datapack tag conventions).
   `docs/README.md` is the folder index, so the GitHub tree URL renders as the manual: that
   is the Modrinth `wiki_url`.
5. **Store copy** — modpack permission stated outright, a **verified** Fabric positioning
   line (Sophisticated Backpacks' Modrinth project lists `forge`,`neoforge` and no fabric,
   checked 2026-08-30, and it is otherwise current with 26.1.2/26.2 builds), the upload
   table at 1.1.0 across all 8 rows, a 1.1.0 changelog block, and a paste-ready table for
   the empty Modrinth fields (issues / source / wiki / discord).
6. **Stamped 1.1.0** on `gradle.properties mod_version` (the single source).

**Open for SapperSquad:** the two worn-render frames in `promo/` are honest proof but not hero
frames (a stone pad and a lot of sky) — the worn pack is this release's headline and
deserves a framing pass before it displaces one of the six store picks.

### 1.1.0 port sweep — where every branch actually stands

| Branch | 1.1.0? | Worn render | Suite | Jar |
|---|---|---|---|---|
| `master` (1.21.1) | **yes** | **yes, pixel-verified** | 66 × 3 combos | `packwork-1.1.0.jar` |
| `port/1.21.8` | **yes** | **yes, pixel-verified** | 67 × 3 combos | `packwork-1.1.0+mc1.21.8.jar` |
| `port/1.21.10` | **yes** | **yes, pixel-verified** | 67 × 3 combos | `packwork-1.1.0+mc1.21.10.jar` |
| `port/1.21.11` | **yes** | **yes, pixel-verified** | 67 × 3 combos | `packwork-1.1.0+mc1.21.11.jar` |
| `port/26.1` | **yes** | **yes, pixel-verified** | 67 × 3 combos | `packwork-1.1.0+mc26.1.2.jar` |
| `port/26.2` | **yes** | **yes, pixel-verified** | 67 × 3 combos | `packwork-1.1.0+mc26.2.jar` |
| `fabric/26.1` | **yes** | shipped, **not yet seen** | 67 × 3 combos | `packwork-1.1.0+mc26.1-fabric.jar` |
| `fabric/26.2` | **yes** | shipped, **not yet seen** | 67 × 3 combos | `packwork-1.1.0+mc26.2-fabric.jar` |

All eight branches are stamped 1.1.0, green, and build a jar whose own metadata was read
back out by extraction. The one open item is the Fabric worn render — the layer is written
and registered but has never been photographed; see "The Fabric worn render, and why it is
not verified" below before touching it.

**Scope discovery worth knowing up front:** the config core (`117b1e6` + `d7120e8`) had
never been swept to the ports either, so each port branch had to take BOTH the config core
and the adoption wave. The config core is the expensive half — 39 files, new registries
(`ModConditions`, `ModAttachments`), death handling on `LivingDropsEvent`, and a
`neoforge:conditions` line in all 18 fitting recipes (which on the port branches must be
merged onto the branch's own plain-string ingredient shape, not master's `{"item":...}`).

**The 26.x-era drift checklist, in the order it bites** (identical on 1.21.8 → 26.2 unless
noted; every entry javap'd out of the branch's own classes, not remembered):
`ResourceLocation` → `Identifier` (1.21.11+ only) · `readResourceLocation()` →
`readIdentifier()` · `sp.serverLevel()` → `sp.level()` · `getMinBuildHeight()/getMaxBuildHeight()`
→ `getMinY()/getMaxY()` · `AttachmentType.Builder.serialize` takes a **MapCodec**, so the
kept-packs codec needs `.fieldOf("packs")` · `FMLEnvironment.dist` → `FMLEnvironment.getDist()` ·
`appendHoverText` takes `TooltipDisplay` + a `Consumer` · `mouseClicked` takes a
`MouseButtonEvent` · `GuiGraphics.drawString` → `GuiGraphicsExtractor.text` (26.x) ·
`mc.screen`/`mc.setScreen` → `mc.gui.screen()`/`mc.gui.setScreen` (26.2 only) ·
`mc.getWindow().getWindow()` → `.handle()` · `mc.resizeDisplay()` → `mc.resizeGui()` (26.x) ·
`options.hideGui` → `mc.gui.hud.toggle()/isHidden()` (26.x) · `LevelSettings` takes
`DifficultySettings` and no GameRules (26.x) / `new GameRules(enabledFeatures)` (1.21.8–11) ·
world time via `server.clockManager().setTotalTicks` (26.x).

**Gametest conventions per branch, and two traps that cost real time:**
- Every port branch registers tests as registry entries: `@GameTest(template="empty")` →
  `@PackTest`, then re-run `java tools/GenTestInstances.java`.
- **1.21.8 and 1.21.10 take `PackHelper`, not `GameTestHelper`** (those versions dropped the
  String assert overloads). The incoming config tests take `GameTestHelper` and must be
  converted — AND `tools/GenTestInstances.java`'s regex only matched `(GameTestHelper`, so it
  found 9 tests, **wiped the 57 committed instance JSONs** (it clears the directory), and wrote
  9. The regex accepts either helper type now on both branches.
- The standard-capability assertions differ: master uses `Capabilities.FluidHandler.ITEM`
  directly, the ports use their own `fluidCap()/itemCap()/energyCap()` helpers. A blanket
  rename of the token to `fluidCap(pack)` also rewrites the BODY of `fluidCap` into infinite
  recursion — it compiles clean and the suite dies on a StackOverflowError in the server tick
  loop. Fixed on 1.21.8; every other branch's suite proves it isn't there.

### The two pieces of 1.1.0 — both built, 2026-08-30

**1. The worn layer on the new render pipeline — DONE on all four branches, pixel-verified.**
Minecraft **1.21.10** is where it changed, and the drift map that pointed here was wrong on
two counts, both checked against the branches' own class files rather than remembered:
`AvatarRenderer` / `AvatarRenderState` arrive in **1.21.10**, not 26.x, and
`BlockRenderDispatcher` is still present on 1.21.10 and 1.21.11 — it is simply no longer how
a layer draws a block. `PlayerModel` moves to `net.minecraft.client.model.player` at
**1.21.11**, not 26.x. What actually differs, and it splits the four branches in two:

- **1.21.10 / 1.21.11** — one call: `collector.submitBlock(pose, blockState, light, overlay,
  outlineColor)`, right where `renderSingleBlock` used to be.
- **26.1 / 26.2** — two steps: `EntityRendererProvider.Context.getBlockModelResolver()` gives
  a `BlockModelResolver`; `resolver.update(blockModelRenderState, blockState,
  BlockDisplayContext.create())` bakes it; then `brs.submit(pose, collector, light, overlay,
  outline)`. The baked `BlockModelRenderState` is parked on the player's render state under a
  second `ContextKey`, exactly as vanilla parks the enderman's carried block on its own,
  because the collector draws later in the frame.

Registration is identical on all four: `EntityRenderersEvent.AddLayers` →
`event.getSkins()` / `event.getPlayerRenderer(skin)` (which returns
`AvatarRenderer<AbstractClientPlayer>`, so the layer's `RenderLayerParent` needs no cast).
The worn STACK still rides the render state. Two traps there:

- `AvatarRenderer` is generic now, so `AvatarRenderer.class` is a RAW `Class<AvatarRenderer>`
  and will not fit `registerEntityModifier`'s
  `Class<? extends EntityRenderer<? extends E, ? extends S>>` — one unchecked cast, used on
  1.21.10 / 1.21.11.
- That same renderer draws **mannequins**, which are `Avatar`s but not players. On 26.x
  NeoForge grew `registerAvatarEntityModifier(AvatarRenderStateModifier)` for exactly this,
  which the 26.x branches take; either way the modifier asks `instanceof Player` before it
  reads a pack.

**Verified as pixels on all four** (`./gradlew runClient -Pwornshot -Pcurios`, seven framed
checks at 1280x900, "every scene checked" in the closing line): Canvas reads as canvas with
its twine V and stitched hem, Sculkhide as echo-lit hide, both seated between the shoulders
with no gap at the spine; over a diamond chestplate the pack rides proud with no z-fighting;
the crouch tips it forward with the torso; the front view is clean through the chest; the
elytra frame is wings and no pack; the toggle-off frame is a bare back.

**2. The Fabric 1.1.0 port — DONE on both branches**, `fabric/26.1` and `fabric/26.2`.
`SimpleToml` and `PackworkConfig` went across verbatim (that was the point of hand-rolling
the parser), so `packwork-server.toml` has byte-identical keys on both loaders. The three
genuinely Fabric-shaped pieces, as predicted plus one correction:

- **Recipe gating** — `TrinketEnabledCondition implements ResourceCondition`
  (`getType()` + `test(RegistryOps.RegistryInfoLookup)`), type built with
  `ResourceConditionType.create(Identifier, MapCodec)` and registered via
  `ResourceConditions.register(...)` from the mod initializer. All eighteen fitting recipes
  carry `{"condition": "packwork:trinket_enabled", "trinket": "<id>"}` in
  `fabric:load_conditions`; the Flask Harness carries it beside its existing
  `fabric:all_mods_loaded` entry.
- **The death stash** — `AttachmentRegistry.builder().initializer(ArrayList::new)
  .persistent(ItemStack.CODEC.listOf()...).copyOnDeath().buildAndRegister(id)`. Fabric takes
  a plain `Codec` where NeoForge wants a `MapCodec`. Restored on
  `ServerPlayerEvents.AFTER_RESPAWN`.
- **The fourth mixin** — and it does NOT need to sweep a drop list. `LivingEntity.drop(stack,
  randomly, thrownFromHand)` is the one choke point every death drop funnels through: the
  pockets via `Inventory.dropAll`, the armour row via `EntityEquipment.dropAll`, and — read
  out of the Trinkets Updated jar's own bytecode rather than assumed — a worn trinket via
  `Player.drop` as well. So one narrow `@Inject(HEAD, cancellable)` guarded on
  `isDeadOrDying()` covers carried AND worn packs, which is the same coverage the NeoForge
  branches get from `LivingDropsEvent`. A grave mod that takes custody earlier means the call
  never happens; `keepInventory` means nothing is dropped at all.

Also across: the Field Reports handbook chapter (the screen's `renderLink` takes
`GuiGraphicsExtractor` here, not `GuiGraphics`), the Handbook tooltip's two lines, the ten
locales, `tools/CheckLang.java`, `docs/`, the README's manual/languages sections, the
CHANGELOG's 1.1.0 entry, and the 1.1.0 stamp. Nine new gametests (67 × 3 combos on both
branches) adapted to Fabric: `claimDeathDrop(player, stack)` in place of `sweepPackDrops`,
`getAttachedOrCreate` in place of `getData`, `PackFluidContent.getAmount()` in place of the
NeoForge `FluidStack`, and droplet-vs-millibucket arithmetic in the waterskin assertion.

### The Fabric worn render, and why it is not verified

The layer exists on both Fabric branches and compiles, and it uses the same 26.x two-step
draw as `port/26.x`. Two Fabric-specific choices:

- Registration is `LivingEntityRenderLayerRegistrationCallback.EVENT`, filtered on
  `renderer instanceof AvatarRenderer`; the callback hands over the
  `EntityRendererProvider.Context`, which is where the `BlockModelResolver` comes from.
- Fabric API has **no render-state modifier event**. Rather than add a fifth mixin, the layer
  looks the player back up by the entity id vanilla already writes onto
  `AvatarRenderState.id` (`Minecraft.getInstance().level.getEntity(state.id) instanceof
  Player`). Mannequins share the renderer, fail the check, and draw nothing.
  Fabric's `RenderStateDataKey` + `FabricRenderState.setData/getData` (its analogue of
  NeoForge's `ContextKey`) is used for the baked `BlockModelRenderState`.

**What blocks the shoot, and it is not the renderer.** The `-Pwornshot` chain equips through
`TrinketsCompat.equipWorn`, which writes the slot through its `TrinketSlotAccess` — and the
wearer's own client is never told. The scene check refused all seven frames and printed what
the client actually holds:

```
[wornshot] canvas_back NOT SHOT - the scene is wrong: the back slot holds 0 minecraft:air,
not a pack (was it cleared?) - this side sees: chest/back[1]=0 minecraft:air |
```

…while the server-side line immediately above it reads `[trinkets] equipped in back slot ->
Canvas Pack`. So the SLOT syncs (its size is right) and the CONTENTS do not. Raising the
inventory's own `TrinketInventoryImpl.markUpdate()` flag changed nothing, and that change was
backed out rather than shipped as an unproven `impl`-class import. Everything server-side —
pack-first pickup routing, the worn GUI host, the gametests — reads that slot correctly.

**Next steps for whoever picks this up, in order:**

1. Trinkets Updated renders trinkets on players itself (`TrinketRenderer`,
   `TrinketRendererRegistry`), so its *normal* equip path almost certainly does sync. Equip
   through that path in the harness — a Trinkets container menu, or whatever supported
   programmatic equip the mod exposes — before assuming anything is broken.
2. `TrinketsCompat.devDescribeSlots(player)` is already there for exactly this: it prints
   what the calling side holds, so the next run says "sync" instead of leaving a bare back to
   be misread as a broken renderer.
3. Only if 1 and 2 say the contents genuinely never reach the wearer: the layer would still
   draw on OTHER players (they track you), and the gap would be your own third-person view.
   That is a real product question for SapperSquad, not a silent one.

Do **not** start by rewriting the layer. The harness is what is failing, and it is failing
loudly on purpose.

**2026-07-26 release stamping — 1.0.0 (SapperSquad's final calls after his confirm pass).**
Version **1.0.0** stamped everywhere: `gradle.properties mod_version` (the single source —
`neoforge.mods.toml` takes `${mod_version}`), PUBLISHING's placeholders + upload table
(`1.0.0+mc1.21.1`, `packwork-1.0.0.jar`), CHANGELOG release header **1.0.0 "First Haul" —
2026-07-26** (the old internal 0.1.0 header retitled "internal milestone, never
published" so First Haul names exactly one release). Environment metadata locked:
**Client Required / Server Required** in the upload table ("Needed on both sides: on
servers, install on the server and every client") — `neoforge.mods.toml` audited, every
dependency `side="BOTH"`, no `clientSideOnly` flag to contradict it. Gallery = **SapperSquad's 6
picks** in display order (lineup hero / sorting / Recipe Ledger / rule editor / Sculkhide
night in its committed stronger framing, NOT the mirrored regen / JEI ring);
keep-layout + pickup-pin stay in `promo/` as non-store extras (noted in both READMEs).
Verified at the stamp: full matrix green (57 plain, 57 `-Pcurios`, 57 all-flags),
`runData` clean, `build` clean, and the built jar's own `neoforge.mods.toml` reads
`version="1.0.0"`. Tagged `v1.0.0` (annotated, not pushed).

**2026-07-26 open-from-worn — the Curios-slot GUI gap is CLOSED (SapperSquad's ask).** A pack worn
in the back slot opens directly: same tabbed organizer, fully live (sort, pin, trinkets,
stores, rule editor, Tinker's Kit bench), reading and writing the worn stack.
- **Third menu host.** `PackStackSlotContainer` generalized to getter/setter/dirty factories
  (`forBlock` / `forWorn` / `clientSide`); `PackMenu` grew `HostKind {CARRIED, BLOCK, WORN}` +
  `serverForWorn`/`clientForWorn`. The open packet's leading boolean became a host-kind BYTE
  (0 carried / 1 block / 2 worn — all three writers funnel through `PackItem`), tier still
  rides so client and server build the same socket count. The gate held: `CuriosCompat` is
  still the only class importing curios; it builds the host's accessors (`wornHost`), which
  LIVE-RESOLVE the Curios inventory every access — never a captured stack or handler.
- **Keybinds.** B's server scan now falls through to the worn pack when the pockets hold no
  pack (same order as pickup routing: inventory, then worn). New **Shift-B** (`OPEN_WORN`,
  NeoForge `KeyModifier.SHIFT` — constructor verified in the patched KeyMapping sources)
  asks for the worn pack outright, falling back to the pocket scan. Both rebindable;
  Controls + Handbook + README + PUBLISHING all say so.
- **No dupe window.** Unequip-while-open: the live supplier collapses to EMPTY, `stillValid`
  flips, and the server's container tick closes the menu; `clicked`/`quickMoveStack`/
  `handleAction` all refuse on a dead host (also closing the latent carried-pack `/clear`
  hole), and the roll-cleanup path hands the bench contents back to the player instead of
  writing onto the departed stack.
- **Curios semantics source-verified (9.5.1 jar, not memory):** `IDynamicStackHandler extends
  IItemHandlerModifiable` returns live instances; `DynamicStackHandler` keeps
  `previousStacks` and Curios' per-tick diff syncs in-place component writes (why
  `applyWornPack` already persisted); the inventory capability provider returns null when
  `getEntitySlots(entity)` is empty. That last one bit: the player slot-assignment default
  that covered the live client did NOT hold on the headless GameTestServer — fixed by
  shipping an explicit `data/packwork/curios/entities/back.json` (documented modern-Curios
  shape), which also hardens real dedicated servers.
- **Tests: 57 green** with and without `-Pcurios` (3 new gated: `wornOpenBindsAndListsGated`,
  `wornWritesPersistToEquippedStackGated`, `wornUnequipClosesWithoutDupeGated` — mock players
  need `CuriosCompat.equipWorn`'s `reset()` fallback since they never fire the join event
  Curios inits on). CHANGELOG/README/PUBLISHING updated — the worn-GUI honest note is GONE
  from the store page; DECISIONS v1 entry superseded. **Wants SapperSquad's eyes in-game:** the
  worn-open feel (B vs Shift-B), and that a menu opened FROM the Curios screen area closes
  cleanly when he swaps the pack out.

**2026-07-26 gallery hero reframe — the lineup re-shot close, ladder reading left-to-right.**
Review verdict on `promo/gallery-1-lineup.png`: the dead-on stand-back framing left the six
packs a small band mid-frame — a test screenshot, not a store hero. Re-shot rather than
cropped (the row was only ~135px tall in the source; no 1:1 crop could make it a hero).
- `DevAutoShot` gallery chain grew a `G_HERO_W` step: after placement, `heroCam` steps in
  close (`placedFirst -0.7x, -2.1z, yaw 22, pitch 14`) — the same slightly-low,
  slightly-angled vantage as the night shot. Pad rise for the gallery went 26 → 64: the
  angled camera looks OVER the row, and one random seed grew a hilltop tree into that
  sightline (caught as pixels on the first re-shoot).
- **SapperSquad's order call: tiers ascend left-to-right for the VIEWER, Canvas → Sculkhide,
  matching the banner.** The cameras face south and east is frame-left, so `placeBlocks`
  now lays the row east-to-west descending (Canvas at +x = `placedFirst`, Sculkhide at +0 =
  `placedLast`); the night camera mirrored with it (`placedLast +1.8x, -2.4z, yaw -20`) so a
  future regen of the night shot also reads ascending. Committed `gallery-7` kept as-is —
  its old Sculkhide-near-left glow framing is the stronger image; swapping in the mirrored
  regen is SapperSquad's call.
- Final frame verified as pixels at 1920x1080 (unchanged size): near Canvas carries the
  frame, tiers ascend left-to-right, all six trims identifiable at a glance (twine / buckle
  / studs / steel plates+band / glyphs / echo-cyan veins), backdrop pure sky. GUI shots
  untouched — the lineup was the only distant world shot.
- **PUBLISHING.md (SapperSquad confirmed): 1.21.1 / NeoForge only for launch.** Requirements line
  now reads "**1.21.1 / NeoForge** (21.1.235+). No other dependencies, ever. Other loaders
  and versions may follow based on demand." Page audited: no other version claims, upload
  table stays exactly `1.21.1` / `NeoForge`, no ranges. `promo/README.md` description
  trued up; gallery captions still accurate as written ("Canvas to Sculkhide" is now
  literally the left-to-right read).

**2026-07-23 art pass 4 (backpack silhouette) — DONE & verified in-game.** SapperSquad reviewed the
hero packs: shading was good but the silhouette had come out too ROUND — the packs read like
pouches/orbs (the runed one like a magic orb), not rugged backpacks. Reshaped in-pipeline,
keeping every fidelity gain from pass 3.
- **The 5 pack sprites are now unmistakable backpacks.** `heroPack` in `tools/GenTextures.java`
  swapped its elliptical `dome()` form for a **superellipse cushion** (`superForm`): a boxy,
  gently tapered body with a flat bottom, plus a **prominent wide flap** draped over the top
  third with a hard stitched hem + AO shadow beneath it, a central closure strap through a brass
  buckle straddling the hem, a top grab-handle loop, and side pockets. Kept from pass 3: the
  32×32 form-shading, AO, rim light, specular buckle glint, and the per-tier material ladder
  (canvas weave+twine → leather grain+buckle → brass studs ringing the flap → riveted steel
  plates+band → deep leather+glowing runes+gem). **Verified as pixels at hotbar size**: a new
  offline preview (`tools/pack_small_preview.png`, box-downscaled to 16px & 12px) plus the live
  hotbar/inventory-row/in-hand/GUI-host-slot shots — it reads as a pack even shrunk.
- **Energy gauge unified with the Charge Crystal (SapperSquad confirmed: match them).** The right-rail
  energy gauge in `PackScreen.drawEnergyGauge` went from amber to the crystal's cool blue
  (`0xFF3EA9C4` fill on `0xFF15323B` glass), so the icon and the gauge agree; still distinct
  from the deeper water-blue fluid gauge.
- **Placed block:** the block MODEL is boxy (body + draped flap + handle + buckle + straps) and
  its leather/brass faces + per-tier tint matched the item's material language. **Per-tier block
  trim was the noted follow-up at the time** — now done, see the next entry.
- 23 GameTests green; `compileJava` clean; version stays **0.1.0**.

**2026-07-26 release dressing — the store kit + the gallery shoot (nothing published).**
`PUBLISHING.md` is now the genuine paste-ready store page (summary, full body, gallery
captions, requirements, first-release changelog block; `<VERSION>` is SapperSquad's visible TODO).
New `promo/` kit: icon-512 + banner-1920x640 composed from the real sprites by
`tools/GenPromo.java`, plus eight gallery shots from the new `-Pgallery` DevAutoShot chain
(1920x1080, sky-pad staging so the backdrop never depends on the seed, reflective cursor
parking because `glfwSetCursorPos` doesn't update MouseHandler's cached position). **This
run doubled as the wave-4 visual verification — every shot inspected as pixels:** sorting
GUI, Recipe Ledger ghost, rule editor sheet, keep-my-layout, drop-to-pin note + pickup
toggle, the JEI ring (pack centered, no shapeless marker, overlay respecting the rails),
the six-tier lineup, and the Sculkhide/Runed placed-glow at night (light emission
confirmed working — the earlier "no glow" read was camera distance). The wave-4
"visuals pending" flag is CLEARED. 54 GameTests green.

**2026-07-26 playtest wave 4 — JEI real recipes, legible pinning, the rule editor, per-tab
arrangement, the upgrade ring.** SapperSquad's live-client playtest called four things; all four
are landed, plus two field follow-ups from his re-tests (the JEI validator root-cause fix
and the full-ring recipes, items 1 and 5 below). 45 GameTests green; version stays **0.1.0**. **Visuals pending:** his client ran through most of
this wave and client launches were blocked after it closed, so the new GUI work is
logic-verified + gametested but NOT yet seen as pixels. The harness is ready for it:
`./gradlew.bat runClient -Pautoshot -Pjei` now walks the auto-pin drop (note + ribbon), the
quill/rule sheet (write by name + a category chip), the arrangement switch (a real
pick-up/put-down into a kept cell), and a JEI recipe view of the Studded upgrade —
screenshots `packwork_autopin` / `packwork_rules_*` / `packwork_keep_layout` /
`packwork_jei_upgrade`. Also new: the JEI plugin declares the tab rail, fittings rail, and
open parchment sheets as GUI extra areas so JEI's ingredient list stays off them — check
that as pixels too.

7. **Pack-first pickup (SapperSquad's ask: "cobblestone won't automatically go into my backpack").**
   With a Lodestone fitted (carried or Curios-worn), `ItemEntityPickupEvent.Pre` (fires
   before all vanilla processing — verified in the 21.1.235 sources; mutating the entity
   stack is the documented pattern) routes pickups the pack can FILE straight in: non-Loose
   route, pinned anywhere, or already held. Loose-bound finds fall through to vanilla.
   Partial fits shrink the ground stack by exactly what was inserted and leave the rest to
   vanilla; Rose+void-list bins on pickup (magnet contract); packs never intercepted; per-pack
   title-strip toggle persisted as `PackLayout.packFirst` (default ON). 8 gametests
   (`packFirst*`), 53 total green. Trap note: the handler takes `Player`, not `ServerPlayer` —
   mock players aren't ServerPlayers and the narrow gate silently disabled the feature in tests.

6. **The ladder redesigned + tier 6 renamed SCULKHIDE (SapperSquad's calls, second ring pass).**
   "Recipes look good" but he re-cut the materials and pivoted tier 6 off the dragon:
   Studded = copper/iron, Reinforced = gold/DIAMOND, Runed = diamond/NETHERITE, tier 6 =
   amethyst/echo shards (old Runed ring) and renamed — **Sculkhide** implemented
   (coordinator's proposal; alternates Echobound / Wardenhide flagged for veto). Balance
   recorded as HIS design: tier 6's marginal ring is cheaper than tier 5's; the gate is the
   required Runed pack. Art realigned: sculk-teal hide ramp, echo-cyan veins replace bone
   claws, echo gem replaces the breath gem (ECHOR ramp in GenTextures), Studded studs
   brass→iron; verified at 16/12px + 8x zoom. Full registry rename (no migration,
   pre-release); found+fixed: tier 6 was missing from the `curios:back` tag. Canvas/Leather
   rings and the ring structure unchanged.

5. **The upgrade RING (SapperSquad's playtest calls: nine cells, pack centered, diamonds).** Every
   pack recipe now fills all 9 cells with the previous pack in the CENTER: bulk material on
   the edges, fittings on the corners. Ladder: Canvas = wool edges/string corners/chest
   heart (raw); Leather = leather/copper; Studded = leather/CUT COPPER (studs read as
   worked copper, fixing the old studs-iron/plates-copper art swap); Reinforced =
   iron/DIAMOND (SapperSquad's explicit call); Runed = amethyst/echo shards; Dragonhide = shulker
   shells/dragon's breath. `PackUpgradeRecipe` is shaped now — `(from, to, edges, corners)`
   plain Ingredients, matches() demands the 3x3 with the pack at cell 4, rotations free,
   edges/corners NOT interchangeable (gametest pins the swap as non-match), underpay closed
   by construction (all cells occupied = full price). JEI draws the positioned ring, no
   shapeless marker; `getIngredients()` stays the honest row-major ring (still what gets the
   recipe past JEI's validator). material2/SizedIngredient machinery removed.

4. **Per-tab arrangement switch (SapperSquad's call: Tidy / Keep-my-layout).** Every compartment
   gets a mode button under the grid (next to the quill): TIDY = today's auto-arranged view;
   KEEP = items stay in the exact cells the player drops them, new arrivals fill gaps,
   Tidy Up still re-sorts once (the sorted order becomes the new starting layout; MODE
   stays). Architecture: `PackLayout.ManualTab` persists per-tab `cell → backing-slot`
   pairs — strictly VIEW-ONLY over the one flat store (a stale entry can mis-draw at
   worst, never dupe/lose). `PackMenu.buildKeptOrder` renders it deterministically on both
   sides (remembered cells → arrival gap-fill → empties bind to free backing slots);
   player placements/pickups persist through the same `setByPlayer`→`clicked` flush as
   auto-pin; stale entries prune as the player works, never in the per-tick rebuild.
   Codec is `optionalFieldOf` so old packs load untouched. Gametest
   `keepMyLayoutHoldsCellsAndConserves`: cell held, gap-fill, relog round-trip, Tidy Up
   reset, toggle-back re-sort, conservation at every step.

3. **Quill & Ledger rework (SapperSquad's call: stamp = baseline, ledger = rule editor).** He
   couldn't tell what the stamp-gate proxy did — that was the verdict on the DECISIONS flag.
   Stamp-family matching (pickaxe stamp gathers tools) is now ALWAYS-ON for custom tabs, no
   trinket; the ledger's new job is the per-tab rule editor: with it fitted and a custom tab
   open, a quill button under the grid unfolds a parchment sheet — write filters by name / by
   mod (text box) or by category chip (six kinds), strike them off per row. New layout verbs
   ADD_TAB_RULE / REMOVE_TAB_RULE, hard server validation. Model picked & documented: written
   rules edit AND match only while the ledger is fitted; pulling it benches them (never
   deletes — pause, never punish) and tabs fall back to stamp+pins. Pins beat everything.
   Trinket desc, JEI info, Handbook (Sorting + Trinkets) all rewritten to match.

1. **JEI renders the pack ladder as REAL recipes (bug; field-tested twice).** The compat
   plugin only registered info pages, so "how do I make each pack" showed lore.
   `PackworkJeiPlugin` registers an `ICraftingCategoryExtension<PackUpgradeRecipe>` (via
   `registerVanillaCategoryExtensions`, verified against the pinned JEI 19.21.1.312 API jar):
   previous-tier pack + material cells in, next pack out, shapeless-marked, and the result's
   tooltip notes that contents/layout/trinkets/name/stores all carry up. **SapperSquad's field test
   showed the extension alone was NOT enough** — root cause verified in the JEI runtime
   sources: `VanillaRecipes.getCraftingRecipes` runs every crafting recipe through
   `CategoryRecipeValidator.hasValidInputsAndOutputs`, which silently drops (DEBUG-only log)
   any non-special recipe whose `getIngredients()` is empty — before `isHandled`/extensions
   are ever consulted. `PackUpgradeRecipe.getIngredients()` now returns the honest
   cell-by-cell list (pack first, one entry per material cell), so JEI's own scan accepts and
   indexes the recipes; harmless elsewhere (vanilla book shows only unlocked recipes — we
   award none; the Recipe Ledger still can't list upgrades from stock since packs can't nest).
   The plugin logs one greppable line — grep **`Packwork JEI:`** — reporting the upgrade
   count, so discovery + display eligibility are provable from any log. Gametest
   `upgradeRecipesCarryDisplayableIngredients` mirrors the validator's exact checks.
   Trinket + handbook + Canvas recipes are plain JSONs and always rendered. Info pages stay
   as supplements. **Also found under this: the upgrade could be UNDERPAID** — `matches()`
   summed item counts but vanilla crafting consumes one item per grid cell
   (`ResultSlot.onTake`), so 4 shells stacked in one cell bought the craft for 1. Materials
   now count per CELL, exact (`found[m] == count`), `canCraftInDimensions` demands 1 + total
   cells, and the JEI layout is literally the gesture. Gametests updated (spread inputs; a
   stacked input is pinned as NOT matching).
2. **Pinning is legible (SapperSquad: "I don't understand what it means").** Three layers:
   (a) tooltip copy in plain words ("[P] Keep in this tab — auto-sort won't move it");
   (b) feedback — a stitched parchment note over the panel names the tab on every pin/unpin;
   (c) the natural gesture — placing an item into a tab its rules would NOT route it to
   auto-pins it there. Mechanism: `PackViewSlot.setByPlayer` (the one hook vanilla fires only
   for the player's own hand — place/merge/swap, verified in the decompiled sources) records
   the placement; `PackMenu.clicked` flushes it AFTER the click resolves (rebinding mid-click
   would fight vanilla's bookkeeping) and applies the pin identically on both sides — no new
   packet. Handbook + README/PUBLISHING copy updated. Gametest `droppingIntoForeignTabAutoPins`.

**2026-07-25 playtest wave 3 — DEPTH, the recipe chain, the Dragonhide tier, the Recipe
Ledger.** SapperSquad's four asks, all landed. 41 GameTests green; version stays **0.1.0**.

1. **Per-slot DEPTH by tier (the headline).** Every slot holds `maxStack × (ordinal+1)`:
   Canvas 64 → Dragonhide 384 of a 64-stackable (pearls 16→96; unstackables never stack).
   The hard part was persistence: `ItemStack.CODEC` caps counts at 99 (verified in sources),
   so `pack/DeepContentsCodec` persists `{slot, item:{id,components}, count}` with an
   unbounded count and a `Codec.withAlternative` fallback that loads pre-depth saves intact
   (count REQUIRED on the deep shape so legacy data falls through instead of shrinking to 1).
   Network sync needed nothing (stream codecs are uncapped VarInts). Escape hatches closed:
   `PackInventory.insertItem` (parent clamps to item max — verified), `extractItem` (parent
   doesn't clamp at all), `PackViewSlot.remove` (vanilla pickup passes Integer.MAX_VALUE) +
   a swap-guard in `mayPlace` (swap hands the slot stack to the cursor without `remove`).
   Rule: inserts fill to depth, every pull out is ≤ one vanilla stack. Tidy merges INTO
   depth. Deep counts render exact at 3/4 scale inside their own cell (vanilla's 3-digit
   spill smeared into the neighbour — seen and fixed as pixels). Gametests: tier depth
   scaling, relog + block-entity NBT + legacy round-trips, never-escapes drain, tidy depth.

2. **The recipe chain (supersedes raw-materials-only — SapperSquad's call).** Canvas stays a raw
   craft; every higher tier is `packwork:pack_upgrade` FROM the previous pack (raw recipes
   for tiers 2+ deleted). Found + fixed: the upgrade was silently dropping the five STORE
   components (fluid/XP/energy/embers/chemical) — all carried now, gametested with deep
   contents + name + trinkets across Runed→Dragonhide. The recipe gained an optional
   `material2`/`count2` (hand-rolled stream codec; 7 fields beats composite's arity).

3. **DRAGONHIDE, the 6th tier (name flagged for SapperSquad — alternatives: Wyrmhide, Drakeskin).**
   Runed pack + 4 shulker shells + 4 dragon's breath. 256 slots (component cap — the top
   tiers grow DEEP, not wide), 5 sockets, depth ×6, stores ×6, placed light 11. Art: near
   black charcoal-plum hide, brick-laid scale scallops, pale bone claws, ember-pink breath
   gem — clearly a step past Runed at hotbar size (checked in `pack_small_preview.png` and
   in-game). Blockstate went 20→24 variants; the gauge rail shrinks (`gaugeHeight()`) so
   5 sockets + gauges stay inside the panel. Runed upgrade also gained 2 echo shards
   (keeping the deleted raw recipe's Deep Dark gate).

4. **The Recipe Ledger (SapperSquad said the word on the recipe book).** Path taken: IN-HOUSE
   parchment browser, not `RecipeBookMenu` — vanilla's craftability + auto-place plumbing is
   hardwired to the player inventory and its layout claims the tab rail's flank; fighting
   both is more code than the sheet. Client-side: searchable, scrollable list of every
   3×3-able recipe craftable FROM PACK STOCK (StackedContents at full depth + the roll),
   recomputed on open/search/40 ticks. Click chalks a GHOST onto the roll (vanilla's own
   ghost-overlay render pattern; zero movement). Clicking the result well sends the ONE
   server verb `LAY_OUT_GHOST`: simulate-first, all-or-nothing pull of one item per cell
   from pack stock. Gametested (uncoverable recipe moves nothing). The GUI recentres while
   the ledger is open so it never clips the screen edge (found as pixels, fixed).

**2026-07-25 playtest wave 2 — bug fix, art de-noise, 7 new fittings, craft-on-the-go.** SapperSquad
playtested and called four things. All four landed.

1. **BUG: the waterskin gauge threw your bucket on the floor. FIXED, verified in a live client.**
   Three real bugs on one path. (a) The gauges and the tab rail are drawn OUTSIDE the panel rect,
   which is exactly the region vanilla treats as "clicked outside the GUI"; consuming the press
   was never enough because the drop fires on **release**
   (`AbstractContainerScreen.mouseReleased` → `slotClicked(null, -999, PICKUP)` →
   `AbstractContainerMenu.doClick` → `player.drop`), and `PackScreen` doesn't override
   `mouseReleased`. `PackScreen.hasClickedOutside` now returns false over both rails, killing it
   for every rail widget at once. (b) `FluidUtil.tryEmptyContainer/tryFillContainer` act on ONE
   container and return one item, so `setCarried(result)` was replacing a whole carried stack with
   a single bucket — the menu now spends exactly one and hands the result back (cursor → pockets →
   pack → floor). (c) FLUID_INTERACT / XP_SIPHON / XP_POUR are **server-authoritative** now
   (`PackAction.serverAuthoritative()` + an `isClient()` guard in `PackMenu`); layout verbs keep
   their optimistic apply. **Live proof** (`runClient -Pautoshot` dispatches a real press+release
   at the gauge): 1 bucket → empty bucket ON THE CURSOR, nothing on the ground; 3 water buckets →
   2 stay on the cursor + 1 empty bucket in the pockets; an empty bucket fills back. 3 new
   conservation gametests.

2. **ART: the packs read busy, not detailed. DE-NOISED.** Diagnosis confirmed at the pixel level:
   `leatherGrain` added **per-pixel random ±3** on top of a 5-row crease, the canvas tier ran a
   1px-pitch crosshatch at ±8 over the whole sprite, the hem stitch was every-other-pixel at
   near-white, and the trim was 1px sprinkles. All of it is high-frequency detail that turns to
   mush the moment a 32px sprite lands in a 16px slot. Fixes, all in `tools/GenTextures.java`:
   a new `smoothNoise` (coarse lattice + smoothstep) replaces every per-pixel `valueNoise` call;
   grain is now a soft 7-row crease plus one large mottle; the canvas weave is a 2-on/2-off rib at
   ±5; the hem is a 3-on/1-off dashed thread on an unbroken groove; studs became **2×2 shapes with
   a contact shadow** (6, not 9), plates became **bevelled 4×4 with a brass rivet**, runes became
   drawn strokes with a 1px bloom. Also: the closure strap was bottoming out on the ramp's darkest
   stop and reading as a black slot punched through the pack — it's darker leather with a tapered
   tip now, and the buckle got a hard outer edge. Same treatment applied to the placed-block faces
   so a set-down pack still matches. Re-checked at 16px and 12px via `tools/pack_small_preview.png`.

3. **SEVEN new fittings** (SSOT entries in `TrinketType`, effects in `TrinketEffects`, recipes,
   sprites, Handbook entries, lang, gametests): **Tinker's Kit** (below), **Field Furnace**
   (cooks raw ore + raw food on pack fuel, at furnace rates, via a `pack_embers` component),
   **Provisioner's Pouch** (eats the plainest thing in the pack when you're down to 3 haunches —
   effects-bearing foods and the datapack `packwork:never_auto_eat` tag are left alone),
   **Cartographer's Sleeve** and **Angler's Creel** (each opens a **fitting-gated compartment** —
   `AutoTabs.Auto` gained a `gate` field, so a trinket adds a compartment with ONE table entry),
   **Torchbearer's Loop** (sets a torch from pack stock when you're in the dark), **Herbalist's
   Bundle** (replants a grown crop from your own seed stock).

4. **CRAFT ON THE GO — the Tinker's Kit.** A leather **tool roll** unrolls across the pack's
   bottom three grid rows (a latch appears in the title strip only when the kit is fitted): a 3×3
   bench plus a brass-ringed result well, drawn as leather and canvas, never a workbench UI. The
   pack keeps its top three rows so you can still see and reach your stock. **Shift-click from the
   pack lays ONE item on the bench** (you're setting a pattern, not tipping a stack in), and after
   every craft each emptied cell **tops itself back up from pack stock** — so one shift-click on
   the result runs the batch until the pack is out of makings. Shift-clicking the result puts the
   output in the pack first, your pockets second. Rolling up — or closing the pack — returns
   everything laid out. **Conservation is gametested end to end**: 12 planks' worth in play, 12 at
   every step, and no free craft once the pack is dry.

**34 GameTests green**; `compileJava` + `runData` clean; version stays **0.1.0**.

**2026-07-24 art pass 5 (per-tier placed-block trim) — DONE & verified in-game.** The deferred
follow-up: a set-down pack now shows its tier's detailing in the world, not just a tinted base.
- **A `tier` `EnumProperty<PackTier>` blockstate** (5 values) drives per-tier models + textures
  statically. It's set from the placed pack item in `PackContainerBlock.getStateForPlacement`
  (and re-synced in `PackContainerBlockEntity.setPackStack` for the test/stack-swap path).
  **Contents still live on the block entity**; the blockstate is render-only and never feeds the
  drop, so the place↔break round-trip is byte-for-byte lossless and the break returns the
  right-tier item exactly as before.
- **Per-tier faces, from `tools/GenTextures.java` (shared `TIER_RAMP`):** each tier gets a
  colour-baked `pack_<tier>_leather` (body/sides/top) + a trimmed `pack_<tier>_front` (the flap
  face) carrying the item ladder — canvas weave+twine → leather grain → **brass stud ring** →
  **riveted steel corner plates + band** → **glowing runed glyphs + gem**. Trim is kept clear of
  the 3D brass buckle/straps. One shared `pack_shape` model holds the geometry; five tiny child
  models (`pack_<tier>`) swap textures; a 20-variant blockstate (facing × tier) picks them. The
  old block colour handler + neutral `pack_block.png` were removed (colour is baked now).
- **Runed glow:** the Runed tier emits block light (`lightLevel` on the `tier` property) and its
  glyphs are bright/high-contrast, so a set-down Runed pack visibly glows.
- **Verified in-game** (`runClient -Pautoshot`): all five tiers placed side by side show distinct
  trim; the placed-pack GUI still opens/binds (title "Studded Pack", contents + gauges); a
  break/replace check breaks the Runed pack (drop logged as `packwork:runed_pack`) and re-places a
  Leather pack, whose render retracks with no stale trim. **24 GameTests green** (added
  `placedTierDrivesBlockstateAndDrop`: the render tier tracks the pack, the drop stays right-tier,
  a swap retracks). `compileJava` clean; version stays **0.1.0**.

**2026-07-23 art pass 3 (hero packs) — DONE & verified in-game.** SapperSquad asked for hero art on
the 5 packs plus cleanup on 3 weak trinkets.
- **The 5 packs are now 32×32 hero sprites**, rendered by a form-shading model in
  `tools/GenTextures.java` (`heroPack`): a rounded body + draped flap dome lit top-left, AO in
  the flap seam, a rim light, a stitched hem, a brass buckle with a specular glint, a strap
  with thickness, side pockets and a handle. Per-tier material story: canvas weave+twine →
  leather grain+buckle → brass studs → riveted steel plates+band → runed glyphs+gem. Item
  model is `item/generated`, which renders a 32×32 sprite crisply in-slot/in-hand.
- **The placed-block faces (`pack_block.png`, `pack_block_brass.png`) went to 32×32** with real
  leather grain + a stitched seam + a bevel, and brushed brass + rivets, so a set-down pack
  matches the held item. (Per-tier trim on the *block* is still just the tint — studs/plates/
  runes on the block would need per-tier block textures + models; noted as a follow-up.)
- **3 trinkets cleaned:** Restock Strap → bold central brass buckle + two studded pouches;
  Charge Crystal → cool-blue faceted crystal wound in dark copper on a brass mount (kills the
  candle-flame read); Lodestone Charm → dark magnetite stone on a cord, no antennae. (The
  energy gauge was still amber here; art pass 4 unified it to the crystal's cool blue.)
- Verified in-game (`runClient -Pautoshot`, single clean run): hero packs in-hand, in the
  hotbar/inventory row, the GUI host slot, and three placed blocks in-world; all 26×29-centred,
  no edge touch; 23 GameTests green.

**2026-07-23 art pass 2 — DONE & verified in-game.** SapperSquad playtested art pass 1 and called
two shots: sprites sitting low / bleeding off the slot, and art that was "okay, not blown
away." Both actioned.
- **Centering sweep (all sprites, not a spot-check).** The item generator was rebuilt so every
  16x16 sprite is authored inside a shared centred box with a >=1px margin. Before: nearly
  every sprite touched the bottom edge (B-margin 0, sitting low) and `restock_strap` filled the
  whole 16x16; `charge_crystal` was 6x10. After: every item sprite is centred, no edge touch
  (`java tools/AnalyzeSprites.java` prints the bounding box + per-edge margin + centre offset
  for each — use it to re-audit any future sprite). Verified in-game: varied items (tools,
  full blocks, potions, tall/tiny items) all sit centred in the grid, and the host-slot pack
  no longer hangs low.
- **Art lifted from procedural to hand-authored.** `tools/GenTextures.java` now authors each
  item sprite as a pixel-art char grid (a per-material value ramp + top-left light + dark
  outline) instead of noise-fill. The 5 packs are a real material ladder (twine → buckle →
  studs → steel plates → runes+gem). The four murky icons SapperSquad named are redesigned: Quill &
  Ledger (legible book + quill), Charge Crystal (copper-wound faceted crystal, not a flame),
  Quick-Draw Straps (two buckled belts, not a red ✗), Soul Vial vs Flask Harness vs Waterskin
  now clearly distinct. Run `java tools/GenTextures.java` to regen; it also writes
  `tools/sprite_montage.png` (a flat-background lineup of every sprite for pixel inspection).
- **Bolder pin marker.** A pinned slot now wears a red ribbon fold + brass tack in the corner
  (`PackScreen.drawPinRibbon`), replacing the faint pin-head; verified in-game.
- **The GUI panel, tab and placed-block faces stayed procedural** (large tiled surfaces where
  grain reads well) — only the 16x16 item icons became hand-authored.

**2026-07-23 placeable pack — DONE & verified.** Packs are now placeable in the world and
automatable through block capabilities.
- **Block + block entity (`block/`).** Sneak-right-click a face to set a pack down; it renders
  as the pack (tier-tinted, facing the player) and breaking it returns the pack item with
  every field intact. The block entity holds the pack as ONE `ItemStack`, so place→break is a
  lossless move of that stack, never a re-serialisation — a gametest proves items + trinkets +
  layout + each store round-trip byte-for-byte, dupe-safe. No BlockItem (the pack item places
  it), no loot table (`getDrops` returns the stack).
- **Block capabilities.** The placed pack exposes standard item / fluid / energy block caps
  (each trinket-gated), so hoppers/pipes/cables interact with it; inserted items auto-route
  into the right compartment because sorting is virtual over the flat store. Every external
  write marks the block entity dirty.
- **Same GUI, generalised.** `PackMenu` binds to a carried slot OR the block entity (via a
  hidden synced host slot); the carried path is unchanged. Both verified in-game.
- **Forgework block-level charging — WORKS (gated).** Forgework Flux is its own block cap, not
  standard FE, so it isn't free — but the block entity let me register a gated 1:1 `FLOW_ENERGY`
  adapter (`ForgeworkFluxBridge.register`), so a Forgework cable charges a *placed* pack. Live
  proof: `runGameTestServer -Pforgework` lands 5,000 Flux = 5,000 FE.

**2026-07-23 finishing run — DONE & verified.** Forgework Flux bridge, the two remaining
trinkets, the guide, and the Feather cut all landed. Details:
- **Forgework Flux bridge (`compat/forgework/ForgeworkFluxBridge`).** A fitted Charge
  Crystal tops up any Forgework portable terminal you carry, 1 Flux = 1 FE (item-level,
  because Forgework Flux is a block cap + hardcoded item-Flux and the pack has no block
  form — see DECISIONS). Gated `ModList.isLoaded("forgework")`, one class imports
  `com.forgework.*`, never classloads without it. **Verified live:** 17/17 gametests green
  with the local Forgework jar in the runtime (`runGameTestServer -Pforgework`), the
  transfer test asserting exact 1:1 conservation.
- **Quick-Draw Straps — LIVE.** On `PlayerDestroyItemEvent`, a broken held tool is replaced
  from pack stock (dupe-safe; only replaces what the pack holds). `TrinketEffects`.
- **Quill & Ledger — LIVE.** Custom tabs go from pin-only to rule-matching when it's
  fitted: they evaluate stored rules plus a category rule derived from the tab's stamped
  icon. Gated in `SortEngine.toView(TabDef, ledger)`, threaded from `PackMenu` (both sides).
- **Outfitter's Handbook — LIVE & verified in-game.** `guide/HandbookItem` opens
  `client/OutfitterHandbookScreen` (leather/brass, five chapters); content in
  `guide/HandbookContent` interpolates real SSOT numbers. Autoshot screenshots inspected.
- **Feather Charm — CUT.** No encumbrance system, so no job. Removed from the SSOT + assets.

**Phase 2 — trinket framework, DONE & verified in-game.** Right-rail brass sockets
(count = tier), craftable fittings off a `TrinketType` SSOT table. Working: Lodestone
(magnet), Restock (hotbar top-up), Repair (slow mend), Bottomless (grows capacity, never
truncates), Compass Rose (opt-in void, the only void path — press O on a hovered item),
Quick-Draw (break-replace), Quill & Ledger (custom-tab rules). Effects run server-side per
`PlayerTickEvent` (or the break event), throttled/bounded. Preserving tier-upgrade recipe
(`packwork:pack_upgrade`) carries contents+trinkets+name up a tier so no craft eats a pack.

**Phase 3 — fluids + XP + energy stores, DONE & verified in-game.** Three of four stores,
each trinket-gated and stacked as a gauge on the right rail: Waterskin Rack (fluid tank,
`FluidHandler.ITEM`, glass gauge, click-with-cursor fill/drain via `FluidUtil`); Soul Vial
(XP via `PackXpStore`, green gauge, click siphon / shift pour, auto-mends Mending gear);
Charge Crystal (arcane charge via `PackEnergyStorage` implementing `IEnergyStorage`, cool
crystal-blue gauge, any FE source fills it, tops up powered tools in hand, + the gated Forgework bridge
above). All three capabilities are exposed **only when the fitting is present**. The 4th
store — Gas (Flask Harness + Mekanism chemical cap) — is **deferred** (needs the Mekanism
dep); leave the shipped stores as the template: component + (gated) capability + STORE
trinket + gauge + gametest.

**All four resource stores now ship** (fluids/XP/energy always; gas via the gated Mekanism
integration), and **Curios wear + JEI are in** (all soft deps — see the integrations section).
The placed-pack block-entity carries the block-level Forgework + Mekanism caps. Still open: a
dedicated Outfitter's Bench upgrade station (the preserving recipe covers the need for now), a
quest chapter, `CLAUDE.md`, and opening the pack GUI from the Curios slot. **README.md /
PUBLISHING.md / CHANGELOG.md exist** — keep their copy in step with code.

## The three optional integrations — DONE (gas / Curios / JEI), soft-dep verified

All three are strict SOFT deps: `compileOnly` the API in `build.gradle`, `ModList.isLoaded`-
gated, one class per mod under `compat/`. The mod builds, loads, and passes all 22 GameTests
with **none** present (verified). Runtime inclusion is opt-in per flag so the default stays
dependency-free: `./gradlew runClient -Pjei -Pcurios -Pmekanism` (any subset). Pinned
versions in `gradle.properties`; repos (blamejared, theillusivec4, modmaven) in `build.gradle`.

- **Gas → Mekanism (`compat/mekanism/MekanismChemicalStore`).** Flask Harness STORE trinket +
  dist-neutral `PackChemical` component (id + amount, so nothing always-loaded imports
  Mekanism) + an `IChemicalHandler` over it, on the item AND the placed block-entity, gated by
  the trinket + `ModList.isLoaded("mekanism")`. Cap token recreated with Mekanism's own
  `mekanism:chemical_handler` name (item=void, block=sided) since the api artifact ships the
  interface but not the token. Right-rail vapor gauge. Recipe gated by `neoforge:mod_loaded`.
  **Verified in `runGameTestServer -Pmekanism`:** cap present, tier capacity, real hydrogen
  resolved, 3000 mB in / 3000 out. (Live pipe-to-pack not separately staged — see DECISIONS.)
- **Curios → back slot (`compat/curios/CuriosCompat`).** Registers each pack as a curio;
  `data/curios/slots/back.json` + the `curios:back` item tag put it in the back slot; a worn
  pack's trinkets keep ticking via `TrinketEffects.applyWornPack`. **Verified in
  `runClient -Pcurios`** (player has the slot, pack fits + equips). Opening the GUI while worn
  is a v1 follow-up (the menu binds to an inventory slot or a block-entity, not a Curios slot).
- **JEI → info pages (`compat/jei/PackworkJeiPlugin`).** `@JeiPlugin` (self-gating via JEI's
  annotation discovery); info pages for every pack tier, every trinket, and the handbook.
  **Verified in `runClient -Pjei`.**

To add ANOTHER integration later, mirror this: `compileOnly` the API (+ a `-P<mod>` runtime
flag), an `optional` block in `neoforge.mods.toml`, and one gated `compat/<mod>/` class.

### Original status (Phase 0/1)

Git repo initialized. NeoForge 1.21.1 scaffold cloned from Highroller (Neo **21.1.235**,
Parchment 2024.11.17, JDK 21, package `com.sappersquad.packwork`, mod id `packwork`).

**Built & compiling & committed:**
- **Phase 0** — five tier pack items (Canvas→Runed, one `PackTier` enum), component-backed
  item store (`PackInventory` over `ItemContainerContents`), a working GUI, contents
  persist through save/load. Item-handler capability exposed on the stack for any mod's
  automation.
- **Phase 1 — the sorting flagship (the reason this mod exists), verified in-game.**
  Stamped-leather tab rail (7 auto-tabs + Loose + custom tabs), rules engine
  (tag/mod-id/name/predicate), manual pins that beat rules, Loose catch-all, Tidy Up,
  search, flatten, custom tabs (create / rename / dye / stamp icon / reorder / delete),
  auto-routing on insert, data-driven category tags. Keybind-to-open (B) + native use.
- **Phase 2 (partial)** — tier crafting recipes (raw materials, no content-eating upgrades).

**Verified in-game** via the dev screenshot harness (`-Pautoshot`, see below), with pixels
inspected: the leather/brass panel renders, items store and display, the Food tab shows
only food, Combat only weapons/armor, tab selection routes correctly, a new custom tab
appears on the rail. **Six GameTests green** (persistence round-trip, routing, pins, Tidy
Up, nesting-block, fresh-pack default).

**Not built yet:** the trinket framework + material-tier upgrade UI (rest of Phase 2), the
four resource stores (Phase 3), and progression/JEI/quest/guide + `README.md`/`PUBLISHING.md`
(Phase 4). Curios wear-slot compat is still pending.

### How to see the GUI without driving the window
The gradle dev-client window can't be driven by desktop-control tooling (it's a raw java
process, not a Start-menu app). So there's a dev-only harness: `DevAutoShot` (gated on
`-Dpackwork.autoshot` / `./gradlew.bat runClient -Pautoshot`) boots a throwaway creative
world, fills a pack across every tab, opens it, switches tabs, and writes screenshots to
`run/client/screenshots/packwork_*.png` — then read those PNGs. Delete
`run/client/saves/packwork_autoshot` before re-running so world creation doesn't collide.

### Where things live (source map)
- `pack/` — `PackItem`, `PackTier` (SSOT ladder), `PackInventory` (live component store),
  `PackMenu` (virtual-tab menu + all action handlers + the Tinker's Kit tool roll: a
  `TransientCraftingContainer` + `ResultContainer`, its `RollResultSlot` refilling from pack
  stock after each craft, and `emptyRollIntoPack` on roll-up/close), `PackViewSlot`
  (rebinding grid cell).
- `sort/` — `SortRule`, `PredicateKind`, `TabDef`, `PackLayout` (component), `AutoTabs`
  (SSOT category table; `Auto.gate` makes a compartment trinket-gated), `TabView`,
  `SortEngine` (routing; `tabsFor(layout, Set<TrinketType>)` is the real entry point),
  `PackSorting` (Tidy Up).
- `block/` — `PackContainerBlock` (placeable, facing, opens the GUI, drops the pack stack),
  `PackContainerBlockEntity` (holds the pack as one `ItemStack`; tier-only client sync).
  Registered in `reg/ModBlocks` + `reg/ModBlockEntities`; block caps in `PackworkCapabilities`.
- `trinket/` — `TrinketType` (SSOT table, 18 fittings), `TrinketItem`, `TrinketAccess`,
  `TrinketEffects` (per-tick effects + the event handlers: Quick-Draw's break refill, the
  Angler's `ItemFishedEvent` stow, the Herbalist's `BlockEvent.BreakEvent` replant). The
  conservation-critical helpers — `smeltOnce`, `feedFrom`, `stowCatch`, `takeSeedFor`,
  `pullReplacement` — are public and gametested.
- `compat/` — one gated class per mod, the ONLY class importing that mod: `forgework/`
  (`ForgeworkFluxBridge`), `mekanism/` (`MekanismChemicalStore`), `curios/` (`CuriosCompat`),
  `jei/` (`PackworkJeiPlugin`, self-gated by `@JeiPlugin`). Each reached only behind
  `ModList.isLoaded` (JEI via annotation discovery).
- `guide/` — `HandbookItem` (opens the guide), `HandbookContent` (dist-neutral chapter data,
  interpolates SSOT numbers). The screen itself is `client/OutfitterHandbookScreen`.
- `client/` — `PackScreen` (the rail + controls), `OutfitterHandbookScreen`,
  `HandbookClientHooks`, `PackClientActions`, `PackKeyMappings`, `ClientSetup`, `DevAutoShot`.
- `net/` — `PackAction`, `PackActionPayload`, `OpenPackPayload`; wired in `PackworkNetwork`.
- `reg/` — `ModItems` (packs + trinkets off their enums, plus the `HANDBOOK` item),
  `ModMenus`, `ModComponents`, `ModCreativeTabs`. Caps in `PackworkCapabilities`.
- `gametest/PackworkGameTests` — the headless proof of persistence + sorting.
- `tools/GenTextures.java` — procedural leather/brass GUI + pack sprites (Java only; run
  `java tools/GenTextures.java`). Data: `data/packwork/tags/item/sorting/*`, `recipe/*`.

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

All 18 are LIVE. One `TrinketType` entry + assets each; nothing else to edit.

- **Lodestone Charm** — magnet, pulls nearby items into the pack.
- **Quill & Ledger** — custom tabs match by rule, not just pins (files by the stamped icon's kind).
- **Compass Rose** — void filter (opt-in trash for chosen items). The only void path.
- **Tinker's Kit** — a leather tool roll unrolls across the pack's lower rows: a 3×3 bench that
  refills itself from pack stock after each craft.
- **Field Furnace** — cooks raw ore + raw food from pack contents, on pack fuel, at furnace rates.
- **Provisioner's Pouch** — eats the plainest thing in the pack before hunger bites.
- **Cartographer's Sleeve** — opens a gated **Charts & Bearings** compartment.
- **Angler's Creel** — opens a gated **The Catch** compartment; your catch lands in the pack.
- **Torchbearer's Loop** — sets a torch from pack stock when you're standing in the dark.
- **Herbalist's Bundle** — replants a grown crop from your own seed stock.
- **Repair Kit** — mends equipped gear.
- **Restock Strap** — auto-refills the hotbar from pack stock.
- **Bottomless Lining** — extra capacity, never truncating.
- **Quick-Draw Straps** — a broken held tool is replaced from pack stock.
- Plus the four store trinkets: Waterskin Rack, Soul Vial, Charge Crystal, Flask Harness.

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

- **Phase 0 — prove the loop. DONE.** Scaffold; pack items; component store; a real GUI;
  contents persist (gametest `contentsSurviveSaveLoad`).
- **Phase 1 — the sorting system (the headline). DONE & verified in-game.** Tabs, auto-tabs
  via tags, custom tabs, rules engine, manual pins, Tidy Up, search/flatten, data-driven
  categories, keybind-open.
- **Phase 2 — tiers + trinket framework. STARTED.** Tier recipes done. **Next:** (a) the
  trinket framework — a small `trinket/` package with a `Trinket` registry table (SSOT like
  `AutoTabs`), N trinket slots per tier (`PackTier.trinketSlots()` already returns the
  count), a trinket rail on the RIGHT of the GUI, and the "easy" trinkets (Feather Charm =
  no slowdown, Bottomless Lining = +capacity, Compass Rose = opt-in void, Lodestone Charm =
  magnet, Restock Strap, Repair Kit, Quick-Draw). (b) Curios back-slot compat in
  `compat/curios/` gated on `ModList.isLoaded("curios")`, native fallback. (c) A
  contents-preserving tier-upgrade recipe (or fold into the Phase 4 Outfitter's Bench).
- **Phase 3 — the four resource stores.** fluids → XP → energy → gas, each behind its
  trinket (Waterskin Rack / Soul Vial / Charge Crystal / Flask Harness), surfaced as gauges
  on the right rail. Standard NeoForge `FluidHandler`/`IEnergyStorage` caps; gated Forgework
  (Flux 1:1 FE) and Mekanism (chemicals) bridges, one class per mod under `compat/`.
- **Phase 4 — progression & release.** The in-house **Outfitter's Handbook** guide is
  DONE (`guide/` + `client/OutfitterHandbookScreen`, cribbed from PhytoForge's Lab Manual).
  `README.md` + `PUBLISHING.md` + `CHANGELOG.md` exist and are current. Remaining: JEI (see
  deferred integrations), a quest chapter, `CLAUDE.md`, store art, and the Outfitter's Bench
  block (also the home for a future block-level Forgework bridge).

### Fastest way to resume
`./gradlew.bat compileJava` (constant), `runGameTestServer` (logic — add `-Pforgework` to
exercise the Flux bridge against the local Forgework jar), `runClient -Pautoshot` (see the
GUI + the Handbook; screenshots land in `run/client/screenshots/`). The natural next build
is one of the deferred integrations above — each is a single gated `compat/<mod>/` class
plus (for a store) the Waterskin-Rack template. Note `runClient` does not self-exit after
autoshot; kill it once the screenshots are written.

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
