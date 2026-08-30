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

- **Worn-pack frames**: `./gradlew runClient -Pwornshot -Pcurios` stages seven framed
  checks of the on-your-back render at 1920x1080 with a long lens (FOV 38) on a sky pad —
  two tiers, over a chestplate, crouching, from the front, under an elytra, and with
  `show_worn_pack` off. Shots land in `run/client/screenshots/worn_*.png`. Two of them are
  committed here as **extras**: `gallery-9-worn-sculkhide.png` and
  `gallery-10-worn-canvas.png`. They are proof, not a hero frame — see the note in
  `../PUBLISHING.md` before putting either on the store page.

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
