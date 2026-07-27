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

| File | What it is |
|---|---|
| `icon-512.png` | Project icon: the Leather Pack on a stitched leather patch. |
| `banner-1920x640.png` | Featured banner: name, tagline, the six-tier ladder. |
| `gallery-1-lineup.png` | The six placed tiers, daylight, sky pad. |
| `gallery-2-sorting.png` | The sorting GUI, mid-sort (tabs, sockets, gauges). |
| `gallery-3-ledger.png` | Tool roll + Recipe Ledger with a chalked ghost. |
| `gallery-4-rules.png` | The Quill & Ledger rule editor, written on. |
| `gallery-5-keep.png` | Keep-my-layout holding a player's arrangement. |
| `gallery-6-pickup-pin.png` | Drop-to-pin note + the pack-first pickup toggle. |
| `gallery-7-sculkhide-night.png` | The glowing tiers at night, up close. |
| `gallery-8-jei-ring.png` | An upgrade ring rendered in JEI. |
