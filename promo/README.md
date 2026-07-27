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

Store gallery = **Alex's 6 picks** (display order in the PUBLISHING table); the other
two frames are **extras** — kept for posts and future update galleries, not uploaded
to the store page. The committed night shot keeps its stronger Sculkhide-near-left
framing; the harness would regenerate it mirrored (order-consistent with the lineup),
and swapping that in stays Alex's call.

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
