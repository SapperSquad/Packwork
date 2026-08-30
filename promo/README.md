# Packwork promo kit

Store-page art for Modrinth / CurseForge. Captions and the upload plan live in
`../PUBLISHING.md` (the gallery table there is the source of truth).

## Regenerating

- **Composed art** (icon, banner): `java tools/GenPromo.java` from the project root.
  Composes from the REAL in-game sprites in `src/main/resources/assets/packwork/textures/`,
  so it can never drift from shipped art. Rerun after any pack-sprite change.
- **Gallery shots**: `./gradlew runClient -Pgallery -Pjei` stages all eight shots at
  1920x1080 (a throwaway world; delete/rename `run/client/saves/packwork_autoshot` first).
  Shots land in `run/client/screenshots/gallery_*.png`; inspect them as pixels, then copy
  here under the `gallery-N-name.png` names from the PUBLISHING table.

## Files

Store gallery = **SapperSquad's 6 picks** (display order in the PUBLISHING table); the other
two frames are **extras** — kept for posts and future update galleries, not uploaded
to the store page. The committed night shot keeps its stronger Sculkhide-near-left
framing; the harness would regenerate it mirrored (order-consistent with the lineup),
and swapping that in stays SapperSquad's call.

| File | Store | What it is |
|---|---|---|
| `icon-512.png` | icon | Project icon: the Leather Pack on a stitched leather patch. |
| `banner-1920x640.png` | featured | Featured banner: name, tagline, the six-tier ladder. |
| `gallery-1-lineup.png` | pick 1 | The six placed tiers, daylight, sky pad — shot close and angled from the Canvas end (the hero camera), tiers ascending left-to-right Canvas → Sculkhide to match the banner. |
| `gallery-2-sorting.png` | pick 2 | The sorting GUI, mid-sort (tabs, sockets, gauges). |
| `gallery-3-ledger.png` | pick 3 | Tool roll + Recipe Ledger with a chalked ghost. |
| `gallery-4-rules.png` | pick 4 | The Quill & Ledger rule editor, written on. |
| `gallery-7-sculkhide-night.png` | pick 5 | The glowing tiers at night, up close. |
| `gallery-8-jei-ring.png` | pick 6 | An upgrade ring rendered in JEI. |
| `gallery-5-keep.png` | extra | Keep-my-layout holding a player's arrangement. |
| `gallery-6-pickup-pin.png` | extra | Drop-to-pin note + the pack-first pickup toggle. |

## 1.1.0 additions

- **Worn-pack PROOF frames**: `./gradlew runClient -Pwornshot -Pcurios` stages seven framed
  checks of the on-your-back render at 1920x1080 on a bare sky pad - two tiers, over a
  chestplate, crouching, from the front, under an elytra, and with `show_worn_pack` off.
  Shots land in `run/client/screenshots/worn_*.png`. This chain answers "does it render".
  None of its frames are committed any more; the HERO shoot below supersedes them.

## 1.2.0 additions — the sorting GIF

`promo/packwork-sorting.gif` — 640x360, 300 frames at 7cs (14.3 fps), 21 seconds, 6.8 MB.
The one asset the outreach kit rides on: the player never organises anything and the pack
visibly does. Five beats — the mess, the shift-click dump, the compartments proving they
were filled all along, bread pinned into Ores & Valuables, and Tidy Up before it closes on
the pack standing in the world (which loops cleanly back to the pack opening).

Regenerate in two steps:

1. `./gradlew runClient -Pgifshot` — a tick-counted script writes one framebuffer PNG per
   client tick to `run/client/screenshots/gifshot/` (400 frames = 20 s at 20 tps). It forces
   **1280x720 at GUI scale 2** on purpose: every GUI texel is then exactly 2x2 device pixels,
   so the encoder's 2x nearest downscale is lossless for the GUI and the text stays crisp.
2. `java tools/GifEncoder.java run/client/screenshots/gifshot promo/packwork-sorting.gif 20 15 2`
   — drops 20 fps to 15, downscales 2x nearest, quantises every frame against ONE global
   median-cut palette (per-frame palettes bloat the file and make flat leather shimmer), and
   writes a GIF89a with the NETSCAPE2.0 loop block.

**Three things learned making it, so the next one is cheaper:**

- **There is no cursor in a framebuffer capture.** Minecraft never draws one — the OS does —
  so a recording shows nothing where the pointer is. The script moves the REAL cursor with
  `glfwSetCursorPos` and lets vanilla's slot HIGHLIGHT be the pointer. `devHover` is no use:
  the screen recomputes the hovered slot from the mouse every frame.
- **The dump has to run FLATTENED.** Filed into compartments, most of what you shift-click
  lands on a tab you are not looking at — the first cut showed items vanish from the pockets
  and never appear, which reads as "it ate them". Shot 3 then un-flattens and shows the
  compartments were being filled the whole time.
- **A tab tooltip lies straight across the compartment.** The cursor has to step off the rail
  a beat after each tab click, onto an empty grid cell — highlight stays, tooltip goes.

Size check, since it was measured rather than guessed: the same 300 frames at full 1280x720
come to **18.5 MB** — under Reddit's budget but three times the size and over Discord's
10 MB embed cap, for detail nobody sees at post scale. 640x360 is the right answer.

## 1.2.0 additions — the worn-pack HERO frames and the turntable

`./gradlew runClient -Pwornhero -Pcurios` shoots the store frames the proof chain never
could, then captures a turntable clip. Three stills at 1920x1080 (cropped here to
1440x810), plus 100 spin frames.

| File | Store | What it is |
|---|---|---|
| `gallery-9-worn-sculkhide.png` | **candidate** | Sculkhide worn, three-quarter, echo veins lit; camp and campfire behind. |
| `gallery-10-worn-canvas.png` | extra | Canvas worn, same framing — the other end of the ladder. |
| `gallery-11-worn-over-armor.png` | extra | Runed over a netherite chestplate: it rides proud of plate, no z-fighting. |
| `packwork-worn-spin.gif` | — | 640x360, 100 frames at 5cs = 20 fps, 5.0 s, 4.2 MB. A full 360° turntable that loops seamlessly. Discord/pitch asset. |

**The three-quarter angle needed a trick, and it is the useful part of this chain.** Vanilla's
third-person camera always sits directly behind you — you cannot orbit your own back. What you
*can* do is turn the avatar under a fixed camera: for a Player the camera reads `yRot`, while
the renderer reads `yBodyRot` and `yHeadRot`. Overriding the latter two **every client tick**
(vanilla's `tickHeadTurn` drags the body back toward the head, so once is not enough, and both
the `…O` previous-tick fields must be written or the avatar shivers between the old angle and
the new) turns the body in place and leaves the camera where it is. Sweeping that angle
0→360 over the clip is the turntable.

**Two framing gotchas, both of which cost a take:**

- **Vanilla's FOV option floors at 30.** It is an `OptionInstance.IntRange(30, 110)`
  (`Options.java`), and a value below 30 is refused and falls back to the **default 70** —
  silently. A take shot at "26" came out wider than one shot at 34, which is the only way that
  failure ever announces itself.
- **The real framing control is a wall behind the camera.** With the longest legal lens the
  subject still left a 1920x1080 frame two-thirds empty sky. The third-person camera wants to
  sit 4 blocks back but collision-checks its way in, so a block wall four blocks behind the
  player pulls it to about 3.2 and the subject grows by half again. It is behind the camera,
  so it is never in shot. At three blocks it came in too far and cropped the pack.

**Recommendation, for SapperSquad to accept or ignore:** `gallery-9-worn-sculkhide.png` has
earned a store slot — worn rendering is 1.1.0's headline and this is the first frame of it
worth posting. The natural swap is for `gallery-8-jei-ring.png` (pick 6), which sells a
compatibility detail rather than the mod. The six picks are his call, so nothing has been
changed in the PUBLISHING table.

## 1.2.0 additions — the place-at-death micro-clip

`promo/packwork-death-place.gif` — 640x360, 100 frames at 5cs (20 fps), 5.0 s, 4.7 MB.
`./gradlew runClient -Pdeathclip`. Survival player at a camp with a loaded Runed pack, a
fatal fall, the death screen, and then standing back up to find the pack **standing upright
where they fell** with an empty hotbar. It is the only visual 1.1.0's `death.handling`
config has, and it says "your stuff doesn't scatter" in one beat.

The clip flips the config with `PackworkConfig.setRemote(...)`, not `setLocalForTesting`:
on an integrated server the client takes the config-sync payload on login and `get()` then
prefers the REMOTE overlay, so a local-only override is read by nobody — including the death
handler, which runs on the server in the same JVM.

**Four takes, and each failure is worth knowing:**

- **`setRespawnPosition` on a fresh world is refused** ("no home bed or charged respawn
  anchor"), the message lands in the chat overlay where it sits in frame, and the respawn
  goes to world spawn — underground, on take one. The script teleports the player back
  instead, on three ticks running, because a single teleport races the position packet the
  respawn sends after it.
- **Respawning in single-player parks the client on "Loading terrain…"** for a couple of real
  seconds. Those frames are a blurred panorama with a caption. The capture now freezes the
  frame counter (so the script waits too) while `ReceivingLevelScreen` is up, which also
  gives the clip a clean cut from death screen to standing.
- **Do not stand the player on the pack's line.** Directly behind, their own body hides the
  thing the clip is about — take three was an empty meadow. Standing 2.5 blocks to one side
  puts the pack off-centre and clear.
- **The window pauses itself on lost focus** and records the Game Menu over a frozen world.
  Every capture chain now sets `pauseOnLostFocus = false` on the first tick; it cost a take
  here and it would have cost one elsewhere eventually.
