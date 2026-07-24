import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Procedural texture forge for Packwork. No art tools on this machine, so the
 * leather-and-brass GUI and pack sprites are generated here (Java only, crib of
 * Workstead's tools style). Run:  java tools/GenTextures.java
 *
 * Aesthetic anchors: tanned leather with a grain, brass frame + rivets, a canvas
 * search strip, recessed stitched slots. No flat gray, no sci-fi.
 */
public class GenTextures {

    // GUI size must match PackMenu.IMAGE_W / IMAGE_H.
    static final int W = 176, H = 240;

    // palette (ARGB)
    static final int LEATHER_HI = 0xFF7A5636;
    static final int LEATHER_LO = 0xFF5E4128;
    static final int LEATHER_EDGE = 0xFF3E2A18;
    static final int BRASS = 0xFFC9A24B;
    static final int BRASS_HI = 0xFFE7CC82;
    static final int BRASS_LO = 0xFF8A6A28;
    static final int CANVAS = 0xFFC8B892;
    static final int CANVAS_LO = 0xFFA89A74;
    static final int SLOT_HOLE = 0xFF3C2A19;
    static final int SLOT_SH = 0xFF2A1C10;
    static final int SLOT_HI = 0xFF8A6540;
    static final int STITCH = 0xFFD9C79A;

    public static void main(String[] args) throws Exception {
        String base = "src/main/resources/assets/packwork/textures";
        new File(base + "/gui").mkdirs();
        new File(base + "/item").mkdirs();
        new File(base + "/block").mkdirs();

        genGui(base + "/gui/pack.png");
        genTab(base + "/gui/tab.png");

        // placed-pack block faces: a LIGHT leather base (tinted per tier by a block colour
        // handler - multiply darkens, so the base is the lightest tier), and an untinted
        // brass tile for the buckle + straps so brass stays brass on every tier.
        genBlockLeather(base + "/block/pack_block.png");
        genBlockBrass(base + "/block/pack_block_brass.png");

        // trinket fittings - each its OWN silhouette + palette so none reads as a mini-pack
        genDisc(base + "/item/lodestone_charm.png", 0xFF4C5A72, 0xFF2E3850);      // dark magnetite disc
        genCompass(base + "/item/compass_rose.png");                             // brass compass star
        genBandolier(base + "/item/restock_strap.png");                          // strap + pouches
        genPouch(base + "/item/bottomless_lining.png", 0xFF3B2C63);              // void drawstring pouch
        genToolRoll(base + "/item/repair_kit.png");                             // canvas tool roll
        genCrossStraps(base + "/item/quick_draw_straps.png");                    // crossed holster straps
        genLedger(base + "/item/quill_and_ledger.png");                         // book + quill
        genWaterskin(base + "/item/waterskin_rack.png");                        // leather waterskin
        genVial(base + "/item/soul_vial.png", 0xFF74C043);                       // green soul vial
        genCrystal(base + "/item/charge_crystal.png", 0xFFE79A2E);               // amber charge crystal
        genFlasks(base + "/item/flask_harness.png", 0xFFB08AD8);                 // rack of vapor flasks

        // the in-house guide book: a leather-bound handbook with brass corners
        genBook(base + "/item/outfitters_handbook.png");

        // per-tier pack sprites: a shaped satchel, tier by colour + trim
        genPack(base + "/item/canvas_pack.png",     0xFFD8C99E, 0xFFB6A578, 0);
        genPack(base + "/item/leather_pack.png",    0xFF8A5E38, 0xFF5E4128, 1);
        genPack(base + "/item/studded_pack.png",    0xFF6E4A2E, 0xFF4A3020, 2);
        genPack(base + "/item/reinforced_pack.png", 0xFF5A4E42, 0xFF3A322A, 3);
        genPack(base + "/item/runed_pack.png",      0xFF4E3E5E, 0xFF2E2440, 4);

        System.out.println("Packwork textures generated.");
    }

    // ---------- GUI ----------

    static void genGui(String path) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(7);

        // leather field with vertical gradient + grain
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                float t = y / (float) H;
                int c = lerp(LEATHER_HI, LEATHER_LO, t);
                // grain
                int n = (int) ((valueNoise(x, y, rnd, 0) - 0.5f) * 16);
                c = shade(c, n);
                img.setRGB(x, y, c);
            }
        }

        // brass outer frame
        brassFrame(img, 0, 0, W, H, 3);

        // title recess strip (leather, slightly darker, with a stitched underline)
        fillRect(img, 6, 5, W - 12, 12, shade(LEATHER_LO, -8));
        stitchH(img, 8, 16, W - 8);

        // search strip (canvas) at y18..30
        panel(img, 7, 18, W - 14, 13, CANVAS, CANVAS_LO);
        stitchRect(img, 7, 18, W - 14, 13);

        // item grid 9x6 at (8,34) pitch 18
        for (int r = 0; r < 6; r++)
            for (int c = 0; c < 9; c++)
                slot(img, 8 + c * 18, 34 + r * 18);

        // divider between grid and player inv
        int dy = 150;
        fillRect(img, 6, dy, W - 12, 2, BRASS_LO);
        fillRect(img, 6, dy + 1, W - 12, 1, BRASS_HI);

        // player inventory (3 rows at 158) + hotbar (216)
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                slot(img, 8 + c * 18, 158 + r * 18);
        for (int c = 0; c < 9; c++)
            slot(img, 8 + c * 18, 216);

        ImageIO.write(img, "PNG", new File(path));
    }

    /**
     * A leather category tab, 26x24. Left edge is a brass binding; the leather body
     * carries a grain and a stitched border; the right edge is dark so it tucks under
     * the panel frame. The screen draws the item icon on top and dims inactive tabs.
     */
    static void genTab(String path) throws Exception {
        int tw = 26, th = 24;
        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(5);
        for (int y = 0; y < th; y++) {
            for (int x = 0; x < tw; x++) {
                int c;
                if (x >= tw - 2) {
                    c = LEATHER_EDGE; // right edge tucks under the panel
                } else if (x < 3) {
                    c = x == 0 ? BRASS_LO : (x == 1 ? BRASS : BRASS_HI); // brass binding
                } else {
                    float t = y / (float) th;
                    c = lerp(LEATHER_HI, LEATHER_LO, t);
                    int n = (int) ((valueNoise(x, y, rnd, 2) - 0.5f) * 14);
                    c = shade(c, n);
                }
                img.setRGB(x, y, c);
            }
        }
        // rounded-ish corners on the left (bite out a couple pixels)
        img.setRGB(0, 0, 0); img.setRGB(0, th - 1, 0);
        // stitched top & bottom on the leather body
        for (int x = 5; x < tw - 3; x += 3) { img.setRGB(x, 2, STITCH); img.setRGB(x, th - 3, STITCH); }
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A recessed 18x18 stitched slot with the item hole at +1,+1 (16x16). */
    static void slot(BufferedImage img, int x, int y) {
        int px = x - 1, py = y - 1; // recess top-left
        // hole
        fillRect(img, px, py, 18, 18, SLOT_HOLE);
        // top/left shadow (recessed)
        hline(img, px, px + 17, py, SLOT_SH);
        vline(img, px, py, py + 17, SLOT_SH);
        // bottom/right highlight
        hline(img, px, px + 17, py + 17, SLOT_HI);
        vline(img, px + 17, py, py + 17, SLOT_HI);
    }

    static void brassFrame(BufferedImage img, int x, int y, int w, int h, int t) {
        for (int i = 0; i < t; i++) {
            int col = i == 0 ? BRASS_HI : (i == t - 1 ? BRASS_LO : BRASS);
            rect(img, x + i, y + i, w - 2 * i, h - 2 * i, col);
        }
        // corner rivets
        rivet(img, x + 4, y + 4);
        rivet(img, x + w - 5, y + 4);
        rivet(img, x + 4, y + h - 5);
        rivet(img, x + w - 5, y + h - 5);
    }

    static void rivet(BufferedImage img, int cx, int cy) {
        fillRect(img, cx - 1, cy - 1, 3, 3, BRASS_LO);
        img.setRGB(cx, cy, BRASS_HI);
    }

    static void panel(BufferedImage img, int x, int y, int w, int h, int hi, int lo) {
        for (int j = 0; j < h; j++) {
            int c = lerp(hi, lo, j / (float) h);
            hline(img, x, x + w - 1, y + j, c);
        }
        // inset dark border
        rect(img, x, y, w, h, shade(lo, -30));
    }

    // ---------- pack sprite ----------

    // per-row [xstart,xend] silhouette of a satchel: narrow handle, a flap, a body that
    // bulges into side pockets, then tapers to the base. Index = y (null rows are empty).
    static final int[][] PACK_ROWS = {
            null, null,                                 // 0,1
            {6, 9}, {6, 9},                             // 2,3  handle loop
            {3, 12}, {3, 12},                           // 4,5  flap top
            {2, 13}, {2, 13}, {2, 13}, {2, 13},         // 6-9  flap / upper body
            {1, 14}, {1, 14}, {1, 14},                  // 10-12 body + side pockets
            {2, 13}, {2, 13}, {3, 12},                  // 13-15 taper to base
    };

    /** A shaped adventurer's pack: body + flap + buckle + straps + side pockets, tier-trimmed. */
    static void genPack(String path, int hi, int lo, int tier) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(11 + tier);
        int edge = shade(lo, -46);

        // body fill with a vertical gradient + grain
        for (int y = 0; y < 16; y++) {
            if (PACK_ROWS[y] == null) continue;
            for (int x = PACK_ROWS[y][0]; x <= PACK_ROWS[y][1]; x++) {
                int c = lerp(hi, lo, clampf((y - 2) / 13f));
                c = shade(c, (int) ((valueNoise(x, y, rnd, 3) - 0.5f) * 12));
                img.setRGB(x, y, c);
            }
        }
        // hollow the handle loop
        img.setRGB(7, 3, 0); img.setRGB(8, 3, 0);
        // flap catches light (y4..9), then a shadow seam separates flap from body
        for (int y = 4; y <= 9; y++)
            for (int x = 2; x <= 13; x++)
                if (opaque(img, x, y)) img.setRGB(x, y, shade(img.getRGB(x, y), 12));
        for (int x = 2; x <= 13; x++) if (opaque(img, x, 10)) img.setRGB(x, 10, shade(img.getRGB(x, 10), -26));
        for (int x = 3; x <= 12; x += 2) if (opaque(img, x, 9)) img.setRGB(x, 9, STITCH); // flap stitch

        // side-pocket seams
        for (int y = 10; y <= 13; y++) { darkPix(img, 3, y, lo, -22); darkPix(img, 12, y, lo, -22); }
        // two front straps
        for (int y = 5; y <= 15; y++) { darkPix(img, 5, y, lo, -34); darkPix(img, 10, y, lo, -34); }
        // brass buckles on the straps + a central clasp
        buckle(img, 5, 8); buckle(img, 10, 8);
        img.setRGB(7, 11, BRASS_HI); img.setRGB(8, 11, BRASS); img.setRGB(7, 12, BRASS); img.setRGB(8, 12, BRASS_LO);

        // per-tier trim: canvas twine -> leather -> studs -> steel plates -> runes
        switch (tier) {
            case 0 -> { img.setRGB(6, 6, STITCH); img.setRGB(9, 6, STITCH); img.setRGB(7, 7, STITCH); img.setRGB(8, 7, STITCH); }
            case 2 -> { for (int x = 3; x <= 12; x += 3) img.setRGB(x, 5, BRASS_HI); }
            case 3 -> { plate(img, 2, 6); plate(img, 11, 6); plate(img, 1, 11); plate(img, 12, 11); }
            case 4 -> {
                int rune = 0xFFC2ABFF;
                img.setRGB(6, 6, rune); img.setRGB(7, 7, rune); img.setRGB(6, 8, rune);
                img.setRGB(9, 6, rune); img.setRGB(10, 7, rune); img.setRGB(9, 8, rune);
            }
            default -> {}
        }
        outline(img, edge);
        ImageIO.write(img, "PNG", new File(path));
    }

    static boolean opaque(BufferedImage img, int x, int y) {
        return x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight() && (img.getRGB(x, y) >>> 24) != 0;
    }

    static void darkPix(BufferedImage img, int x, int y, int base, int d) {
        if (opaque(img, x, y)) img.setRGB(x, y, shade(base, d));
    }

    static void buckle(BufferedImage img, int x, int y) {
        img.setRGB(x, y, BRASS_HI); img.setRGB(x, y + 1, BRASS_LO);
    }

    static void plate(BufferedImage img, int x, int y) {
        int steel = 0xFFB9B4AC;
        img.setRGB(x, y, steel); img.setRGB(x + 1, y, shade(steel, -20));
        img.setRGB(x, y + 1, shade(steel, -20)); img.setRGB(x + 1, y + 1, shade(steel, -40));
    }

    static float clampf(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    // ---------- placed-pack block faces ----------

    /** A 16x16 LIGHT, fairly NEUTRAL leather tile - grain + a stitched seam. The base is kept
     *  close to grey so the per-tier tint (multiply) controls the hue instead of a warm brown
     *  base washing every tier the same colour. */
    static void genBlockLeather(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(31);
        int hi = 0xFFCDC6BA, lo = 0xFFB6B0A4; // light near-neutral leather so a tier tint reads
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int c = lerp(hi, lo, y / 16f);
                c = shade(c, (int) ((valueNoise(x, y, rnd, 12) - 0.5f) * 18));
                img.setRGB(x, y, c);
            }
        }
        // a stitched flap seam a third of the way down
        for (int x = 1; x < 15; x += 3) img.setRGB(x, 5, STITCH);
        // darker leather edging
        for (int i = 0; i < 16; i++) {
            img.setRGB(0, i, shade(lo, -26)); img.setRGB(15, i, shade(lo, -26));
            img.setRGB(i, 0, shade(lo, -26)); img.setRGB(i, 15, shade(lo, -26));
        }
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A 16x16 brass tile for the buckle + straps (never tinted). */
    static void genBlockBrass(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(37);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int c = lerp(BRASS_HI, BRASS_LO, y / 16f);
                c = shade(c, (int) ((valueNoise(x, y, rnd, 15) - 0.5f) * 14));
                img.setRGB(x, y, c);
            }
        }
        // a couple of rivets
        rivet(img, 4, 4); rivet(img, 11, 11);
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A 16x16 leather-bound handbook: leather cover, brass corners, a page edge and a bookmark. */
    static void genBook(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(23);
        // cover body 2..13 x, 2..14 y
        for (int y = 2; y <= 14; y++) {
            for (int x = 2; x <= 13; x++) {
                int c = lerp(LEATHER_HI, LEATHER_LO, (y - 2) / 12f);
                c = shade(c, (int) ((valueNoise(x, y, rnd, 9) - 0.5f) * 12));
                img.setRGB(x, y, c);
            }
        }
        // spine down the left (darker leather)
        for (int y = 2; y <= 14; y++) { img.setRGB(2, y, shade(LEATHER_LO, -24)); img.setRGB(3, y, shade(LEATHER_LO, -14)); }
        // page block on the right edge (canvas/parchment)
        for (int y = 3; y <= 13; y++) { img.setRGB(13, y, CANVAS); img.setRGB(12, y, y % 2 == 0 ? CANVAS_LO : CANVAS); }
        // brass corners
        img.setRGB(4, 3, BRASS_HI); img.setRGB(5, 3, BRASS); img.setRGB(4, 4, BRASS);
        img.setRGB(11, 3, BRASS_HI); img.setRGB(10, 3, BRASS); img.setRGB(11, 4, BRASS);
        img.setRGB(4, 13, BRASS); img.setRGB(11, 13, BRASS);
        // a small brass clasp/emblem centred
        img.setRGB(7, 8, BRASS_HI); img.setRGB(8, 8, BRASS); img.setRGB(7, 9, BRASS); img.setRGB(8, 9, BRASS_LO);
        // red bookmark tail hanging past the bottom
        img.setRGB(9, 13, 0xFFB4595A); img.setRGB(9, 14, 0xFFB4595A); img.setRGB(9, 15, 0xFF8A3E3F);
        outline(img, LEATHER_EDGE);
        ImageIO.write(img, "PNG", new File(path));
    }

    // ---------- distinct trinket silhouettes (fix: they all read as the same leather tag) ----------

    /** A round magnetite medallion on a brass cord, with a red horseshoe magnet (Lodestone Charm). */
    static void genDisc(String path, int face, int rim) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(41);
        img.setRGB(7, 0, BRASS); img.setRGB(8, 0, BRASS);
        img.setRGB(6, 1, BRASS_HI); img.setRGB(9, 1, BRASS_HI);
        int cx = 7, cy = 9, r2 = 30;
        for (int y = 3; y <= 15; y++)
            for (int x = 1; x <= 14; x++) {
                int dx = x - cx, dy = y - cy, d = dx * dx + dy * dy;
                if (d <= r2) img.setRGB(x, y, shade(d >= r2 - 12 ? rim : face,
                        (int) ((valueNoise(x, y, rnd, 4) - 0.5f) * 10)));
            }
        int red = 0xFFC64B4B;
        for (int y = 7; y <= 11; y++) { img.setRGB(5, y, red); img.setRGB(9, y, red); }
        img.setRGB(6, 7, red); img.setRGB(7, 7, red); img.setRGB(8, 7, red);
        img.setRGB(5, 11, 0xFFECECEC); img.setRGB(9, 11, 0xFFECECEC);
        outline(img, shade(rim, -46));
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A brass compass rose: a ring around a four-point star, north tip red (Compass Rose). */
    static void genCompass(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int cx = 8, cy = 8;
        for (int y = 1; y <= 15; y++)
            for (int x = 1; x <= 15; x++) {
                int dx = x - cx, dy = y - cy, d = dx * dx + dy * dy;
                if (d <= 49 && d >= 30) img.setRGB(x, y, d >= 44 ? BRASS_LO : BRASS);
                else if (d < 30) img.setRGB(x, y, 0xFF2A2418);
            }
        for (int i = -4; i <= 4; i++) {
            img.setRGB(cx, cy + i, i < 0 ? 0xFFC64B4B : BRASS_HI);
            img.setRGB(cx + i, cy, BRASS_HI);
        }
        img.setRGB(cx, cy, 0xFFFFE9B0);
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A diagonal leather bandolier with pouches and a brass buckle (Restock Strap). */
    static void genBandolier(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int strap = 0xFF7A4E2A, dark = shade(strap, -30);
        for (int i = 0; i < 16; i++)
            for (int w = -1; w <= 1; w++) {
                int x = i + w;
                if (x >= 0 && x < 16) img.setRGB(x, i, w == 1 ? dark : strap);
            }
        pouchBox(img, 3, 6, 0xFF6E4526);
        pouchBox(img, 9, 10, 0xFF6E4526);
        img.setRGB(7, 7, BRASS_HI); img.setRGB(8, 8, BRASS);
        outline(img, shade(strap, -50));
        ImageIO.write(img, "PNG", new File(path));
    }

    static void pouchBox(BufferedImage img, int x, int y, int c) {
        fillRect(img, x, y, 4, 4, c);
        hline(img, x, x + 3, y + 3, shade(c, -26));
        img.setRGB(x + 1, y, STITCH); img.setRGB(x + 2, y, STITCH);
    }

    /** A dark drawstring pouch with a violet void swirl (Bottomless Lining). */
    static void genPouch(String path, int base) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(53);
        for (int x = 6; x <= 9; x++) { img.setRGB(x, 3, shade(base, 24)); img.setRGB(x, 4, base); }
        img.setRGB(5, 4, STITCH); img.setRGB(10, 4, STITCH);
        for (int y = 5; y <= 15; y++)
            for (int x = 3; x <= 12; x++) {
                int dx = x - 7, dy = y - 10;
                if (dx * dx + dy * dy <= 27)
                    img.setRGB(x, y, shade(base, (int) ((valueNoise(x, y, rnd, 7) - 0.5f) * 14)));
            }
        int glow = 0xFF9C86D8;
        img.setRGB(7, 10, glow); img.setRGB(8, 10, glow); img.setRGB(8, 11, glow);
        img.setRGB(6, 11, glow); img.setRGB(6, 9, glow); img.setRGB(9, 9, glow);
        outline(img, shade(base, -40));
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A rolled canvas tool-kit with a hammer + awl poking out the top (Repair Kit). */
    static void genToolRoll(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int canvas = 0xFFC2B187, canvasLo = 0xFF9E8E66;
        for (int y = 5; y <= 15; y++)
            for (int x = 3; x <= 12; x++)
                img.setRGB(x, y, lerp(canvas, canvasLo, (x - 3) / 9f));
        for (int y = 6; y <= 15; y += 4) hline(img, 3, 12, y, 0xFF7A4E2A);
        img.setRGB(5, 2, 0xFFB9B4AC); img.setRGB(6, 2, 0xFF8A857C); img.setRGB(5, 3, 0xFFB9B4AC); img.setRGB(5, 4, 0xFF6E4526);
        img.setRGB(9, 2, 0xFF9AA0A6); img.setRGB(9, 3, 0xFF9AA0A6); img.setRGB(9, 4, 0xFF6E4526);
        outline(img, 0xFF5E4128);
        ImageIO.write(img, "PNG", new File(path));
    }

    /** Two crossed holster straps with a central brass ring (Quick-Draw Straps). */
    static void genCrossStraps(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int strap = 0xFF9A4340, dark = shade(strap, -30);
        for (int i = 2; i <= 13; i++) {
            img.setRGB(i, i, strap); img.setRGB(i, i - 1, dark);
            img.setRGB(15 - i, i, strap); img.setRGB(15 - i, i - 1, dark);
        }
        for (int[] d : new int[][]{{6,7},{9,7},{6,8},{9,8},{7,6},{8,6},{7,9},{8,9}}) img.setRGB(d[0], d[1], BRASS);
        img.setRGB(7, 7, 0xFF2A2418); img.setRGB(8, 8, 0xFF2A2418);
        outline(img, shade(strap, -50));
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A blue ledger book with a white quill laid across it (Quill & Ledger). */
    static void genLedger(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int cover = 0xFF3E5A86, coverLo = 0xFF2C426A;
        for (int y = 3; y <= 13; y++)
            for (int x = 3; x <= 12; x++)
                img.setRGB(x, y, lerp(cover, coverLo, (y - 3) / 10f));
        for (int y = 3; y <= 13; y++) {
            img.setRGB(3, y, shade(coverLo, -20));
            img.setRGB(12, y, CANVAS); img.setRGB(11, y, y % 2 == 0 ? CANVAS_LO : CANVAS);
        }
        img.setRGB(4, 4, BRASS); img.setRGB(11, 4, BRASS);
        for (int i = 0; i < 8; i++) img.setRGB(5 + i, 12 - i, 0xFFF2F2F2);
        img.setRGB(4, 13, 0xFF2A2418);
        img.setRGB(12, 5, 0xFFDADADA); img.setRGB(13, 4, 0xFFBFBFBF);
        outline(img, 0xFF1E2A44);
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A rounded leather waterskin with a corked neck and a water sheen (Waterskin Rack). */
    static void genWaterskin(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(61);
        int skin = 0xFF7A5636, skinLo = 0xFF5E4128;
        fillRect(img, 7, 2, 2, 3, 0xFF4A3020);
        img.setRGB(6, 3, BRASS); img.setRGB(9, 3, BRASS);
        for (int y = 5; y <= 15; y++)
            for (int x = 2; x <= 13; x++) {
                int dx = x - 7, dy = y - 10;
                if (dx * dx + (dy * dy * 3) / 4 <= 34)
                    img.setRGB(x, y, shade(lerp(skin, skinLo, (y - 5) / 10f),
                            (int) ((valueNoise(x, y, rnd, 8) - 0.5f) * 10)));
            }
        img.setRGB(5, 8, 0xFF6FA8D6); img.setRGB(5, 9, 0xFF6FA8D6); img.setRGB(6, 8, 0xFF8FC0E4);
        hline(img, 3, 12, 11, 0xFF4A3020);
        outline(img, 0xFF3E2A18);
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A slim corked glass vial of glowing liquid (Soul Vial; colour-swappable). */
    static void genVial(String path, int liquid) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int glass = 0xFFBFD6D0, glassLo = 0xFF8FA9A4;
        fillRect(img, 6, 1, 4, 3, 0xFF8A5E30); img.setRGB(6, 3, BRASS_LO); img.setRGB(9, 3, BRASS_LO);
        fillRect(img, 6, 4, 4, 2, glassLo);
        for (int y = 6; y <= 15; y++)
            for (int x = 4; x <= 11; x++) {
                if ((x <= 4 || x >= 11) && y >= 14) continue;
                img.setRGB(x, y, (x == 4 || x == 11) ? glassLo : glass);
            }
        for (int y = 9; y <= 14; y++)
            for (int x = 5; x <= 10; x++)
                img.setRGB(x, y, shade(liquid, (y - 9) * -4));
        img.setRGB(6, 10, shade(liquid, 40));
        img.setRGB(5, 7, 0xFFFFFFFF); img.setRGB(5, 8, 0xFFEAF2F0);
        outline(img, 0xFF3A4A46);
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A tall faceted crystal wrapped in copper wire (Charge Crystal). */
    static void genCrystal(String path, int col) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int hi = shade(col, 44), lo = shade(col, -44);
        int[][] rows = {{7,8},{6,9},{6,9},{5,10},{5,10},{5,10},{6,9},{6,9},{7,8},{7,8}};
        for (int i = 0; i < rows.length; i++) {
            int y = 2 + i;
            for (int x = rows[i][0]; x <= rows[i][1]; x++)
                img.setRGB(x, y, x <= 7 ? hi : (x >= 9 ? lo : col));
        }
        for (int i = 0; i < 9; i++) img.setRGB(8, 2 + i, shade(hi, 24));
        int cu = 0xFFB5702E;
        for (int x = 5; x <= 10; x++) img.setRGB(x, x % 2 == 0 ? 8 : 9, cu);
        outline(img, shade(lo, -40));
        ImageIO.write(img, "PNG", new File(path));
    }

    /** A small wooden rack holding two round flasks of bottled vapour (Flask Harness). */
    static void genFlasks(String path, int vapor) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int wood = 0xFF6E4A2A;
        vline(img, 2, 4, 15, wood); vline(img, 13, 4, 15, wood);
        hline(img, 2, 13, 10, shade(wood, 20)); hline(img, 2, 13, 15, wood);
        flask(img, 5, vapor);
        flask(img, 10, vapor);
        outline(img, shade(wood, -40));
        ImageIO.write(img, "PNG", new File(path));
    }

    static void flask(BufferedImage img, int cx, int vapor) {
        int glass = 0xFFCFE0DA;
        img.setRGB(cx, 4, 0xFF8A5E30); img.setRGB(cx, 5, glass);
        for (int y = 6; y <= 9; y++)
            for (int x = cx - 2; x <= cx + 1; x++) {
                int dx = x - cx, dy = y - 8;
                if (dx * dx + dy * dy <= 5) img.setRGB(x, y, y >= 7 ? shade(vapor, (y - 7) * -8) : glass);
            }
        img.setRGB(cx - 2, 7, 0xFFFFFFFF);
    }

    static void outline(BufferedImage img, int col) {
        int w = img.getWidth(), h = img.getHeight();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) continue;
                boolean edge = false;
                for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h || (img.getRGB(nx, ny) >>> 24) == 0) edge = true;
                }
                if (edge) img.setRGB(x, y, col);
            }
    }

    // ---------- helpers ----------

    static void stitchH(BufferedImage img, int x0, int y, int x1) {
        for (int x = x0; x < x1; x += 3) img.setRGB(x, y, STITCH);
    }

    static void stitchRect(BufferedImage img, int x, int y, int w, int h) {
        for (int i = x + 1; i < x + w - 1; i += 3) { img.setRGB(i, y + 1, STITCH); img.setRGB(i, y + h - 2, STITCH); }
        for (int j = y + 1; j < y + h - 1; j += 3) { img.setRGB(x + 1, j, STITCH); img.setRGB(x + w - 2, j, STITCH); }
    }

    static float valueNoise(int x, int y, Random rnd, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 144269504;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        return ((h & 0xFF) / 255f);
    }

    static int lerp(int a, int b, float t) {
        int aa = (a >> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t), g = (int) (ag + (bg - ag) * t), bl = (int) (ab + (bb - ab) * t), al = (int) (aa + (ba - aa) * t);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }

    static int shade(int c, int d) {
        int a = (c >> 24) & 0xFF, r = clamp(((c >> 16) & 0xFF) + d), g = clamp(((c >> 8) & 0xFF) + d), b = clamp((c & 0xFF) + d);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

    static void fillRect(BufferedImage img, int x, int y, int w, int h, int c) {
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) set(img, x + i, y + j, c);
    }

    static void rect(BufferedImage img, int x, int y, int w, int h, int c) {
        hline(img, x, x + w - 1, y, c); hline(img, x, x + w - 1, y + h - 1, c);
        vline(img, x, y, y + h - 1, c); vline(img, x + w - 1, y, y + h - 1, c);
    }

    static void hline(BufferedImage img, int x0, int x1, int y, int c) { for (int x = x0; x <= x1; x++) set(img, x, y, c); }
    static void vline(BufferedImage img, int x, int y0, int y1, int c) { for (int y = y0; y <= y1; y++) set(img, x, y, c); }

    static void set(BufferedImage img, int x, int y, int c) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        img.setRGB(x, y, c);
    }
}
