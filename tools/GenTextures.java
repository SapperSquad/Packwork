import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Texture forge for Packwork. No art tools on this machine, so the leather-and-brass
 * GUI, pack sprites, trinkets and block faces are authored here (Java only). Run:
 *   java tools/GenTextures.java
 *
 * The GUI panel, tab and placed-block faces are drawn procedurally (large tiled
 * surfaces where grain reads well). The 16x16 ITEM sprites are hand-authored as
 * pixel-art char grids: every pixel is placed by hand with a top-left light source,
 * a per-material value ramp (outline -> shadow -> mid -> light -> highlight) and a
 * selective dark outline, then centred inside a consistent 14x14 box with a >=1px
 * margin so nothing bleeds to the slot edge. A montage is written to the scratchpad
 * (tools/sprite_montage.png) so the sheet can be eyeballed as pixels without a client.
 */
public class GenTextures {

    // GUI size must match PackMenu.IMAGE_W / IMAGE_H.
    static final int W = 176, H = 240;

    // palette (ARGB) for the procedural GUI/block surfaces
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

    static String BASE;

    public static void main(String[] args) throws Exception {
        BASE = "src/main/resources/assets/packwork/textures";
        new File(BASE + "/gui").mkdirs();
        new File(BASE + "/item").mkdirs();
        new File(BASE + "/block").mkdirs();

        genGui(BASE + "/gui/pack.png");
        genTab(BASE + "/gui/tab.png");
        genBlockTiers();                                   // per-tier leather + trimmed front faces
        genBlockBrass(BASE + "/block/pack_block_brass.png"); // shared brass (buckle + straps) for leather+
        genBlockTwine(BASE + "/block/pack_block_twine.png"); // canvas-only twine buckle (matches its item)

        // ---- hand-authored 16x16 item sprites ----
        genPacks();
        genTrinkets();
        drawItem("outfitters_handbook", HANDBOOK, base());

        System.out.println("Packwork textures generated.");
        writeMontage("tools/sprite_montage.png");
        System.out.println("Montage written to tools/sprite_montage.png");
        writeSmallPreview("tools/pack_small_preview.png");
        System.out.println("Small-size preview written to tools/pack_small_preview.png");
    }

    // =====================================================================
    //  char-grid pixel-art system
    // =====================================================================

    /** Fresh per-sprite palette seeded with the shared material ramps. Light = top-left. */
    static Map<Character, Integer> base() {
        Map<Character, Integer> p = new HashMap<>();
        // leather (warm brown)
        p.put('k', 0xFF241505); p.put('r', 0xFF3F2A14); p.put('l', 0xFF5E3F20);
        p.put('L', 0xFF7E5730); p.put('e', 0xFF9E6F42);
        // canvas (pale tan)
        p.put('w', 0xFF6A5836); p.put('v', 0xFF9C8B5F); p.put('c', 0xFFC0B084);
        p.put('C', 0xFFD8C99C); p.put('W', 0xFFEADCB4);
        // brass
        p.put('n', 0xFF6B4E18); p.put('d', 0xFF9A741F); p.put('a', 0xFFBE9432);
        p.put('A', 0xFFDCB456); p.put('B', 0xFFF6E29A);
        // steel
        p.put('z', 0xFF2E2F35); p.put('x', 0xFF5C5E67); p.put('s', 0xFF8C8E97);
        p.put('S', 0xFFB8BAC2); p.put('Q', 0xFFE4E6EC);
        // glass (cool)
        p.put('j', 0xFF35474A); p.put('g', 0xFF8FB0AE); p.put('G', 0xFFBAD6D2);
        // copper
        p.put('u', 0xFF6E3D1A); p.put('U', 0xFFA55F2A); p.put('H', 0xFFCC8A48);
        // indigo / rune
        p.put('p', 0xFF1A1330); p.put('q', 0xFF2C2049); p.put('i', 0xFF433164);
        p.put('I', 0xFF5C4585); p.put('m', 0xFFBFA6FF); p.put('M', 0xFFE7DCFF);
        // red ribbon / wax
        p.put('X', 0xFF7E2422); p.put('Y', 0xFFB23A38); p.put('Z', 0xFFD65A54);
        // white specular
        p.put('o', 0xFFFFFFFF);
        return p;
    }

    static Map<Character, Integer> with(Map<Character, Integer> p, Object... kv) {
        for (int i = 0; i < kv.length; i += 2) p.put((Character) kv[i], (Integer) kv[i + 1]);
        return p;
    }

    static final Map<String, BufferedImage> RENDERED = new java.util.LinkedHashMap<>();

    /** Render a 16-row x 16-col char grid to a sprite PNG. '.'/' ' are transparent. */
    static void drawItem(String name, String[] g, Map<Character, Integer> pal) throws Exception {
        if (g.length != 16) throw new IllegalStateException(name + ": " + g.length + " rows (need 16)");
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            String row = g[y];
            if (row.length() != 16)
                throw new IllegalStateException(name + " row " + y + " len " + row.length() + ": [" + row + "]");
            for (int x = 0; x < 16; x++) {
                char ch = row.charAt(x);
                if (ch == '.' || ch == ' ') continue;
                Integer c = pal.get(ch);
                if (c == null) throw new IllegalStateException(name + " row " + y + " col " + x + ": unknown char '" + ch + "'");
                img.setRGB(x, y, c);
            }
        }
        ImageIO.write(img, "PNG", new File(BASE + "/item/" + name + ".png"));
        RENDERED.put(name, img);
    }

    // =====================================================================
    //  packs - HERO art (32x32), form-shaded: rounded body + draped flap with a
    //  stitched hem, a brass buckle with a specular glint, straps with thickness,
    //  side pockets, a top handle, per-tier material story + a selective outline.
    //  Not noise-fill: every surface is lit by a top-left light against a rounded
    //  form model, with ambient occlusion in the fold seams and a rim light.
    // =====================================================================

    static final int PS = 32; // hero pack sprite size
    // light direction (top-left-front), normalised
    static final double LX, LY, LZ;
    static {
        double lx = -0.55, ly = -0.62, lz = 0.56, l = Math.sqrt(lx * lx + ly * ly + lz * lz);
        LX = lx / l; LY = ly / l; LZ = lz / l;
    }

    // SSOT: the 7-stop leather ramp (dark -> light) per tier, shared by the hero item sprite
    // AND the placed-block faces so a set-down pack matches the one in hand. Index = tier ordinal.
    static final int[][] TIER_RAMP = {
        {0xFF544527, 0xFF6E5A34, 0xFF897249, 0xFFA68E5F, 0xFFC2AB79, 0xFFD8C494, 0xFFEEDDB2}, // canvas
        {0xFF1C1108, 0xFF321F10, 0xFF4C3319, 0xFF684627, 0xFF875E38, 0xFFA6784B, 0xFFC49468}, // leather
        {0xFF150D06, 0xFF261809, 0xFF3A2811, 0xFF513920, 0xFF6B4E2E, 0xFF87663F, 0xFFA07C50}, // studded
        {0xFF16130E, 0xFF272319, 0xFF3B3527, 0xFF524A38, 0xFF6C6249, 0xFF897C5D, 0xFFA89A75}, // reinforced
        {0xFF130C22, 0xFF201430, 0xFF302144, 0xFF43305C, 0xFF574178, 0xFF6F5695, 0xFF8A70B4}, // runed
    };
    static final String[] TIER_ID = {"canvas", "leather", "studded", "reinforced", "runed"};

    static void genPacks() throws Exception {
        for (int t = 0; t < TIER_ID.length; t++) heroPack(TIER_ID[t] + "_pack", t, TIER_RAMP[t]);
    }

    // brass and steel ramps (7-stop) shared by fittings/trim
    static final int[] BRASSR = {0xFF3E2C0E, 0xFF5E4514, 0xFF80611C, 0xFFA6842C, 0xFFC9A542, 0xFFE6C56E, 0xFFF8E6A0};
    static final int[] STEELR = {0xFF26272C, 0xFF3E4046, 0xFF585B62, 0xFF787C84, 0xFF9CA0A8, 0xFFC2C6CD, 0xFFE8EAEE};
    // twine ramp (7-stop): the pale tan cord of the canvas buckle, extending the hero item's
    // twine palette {6A5836,9C8B5F,C0B084,D8C99C,EADCB4} down two darker stops for form shadow.
    static final int[] TWINER = {0xFF3E3218, 0xFF574726, 0xFF6A5836, 0xFF8A7550, 0xFF9C8B5F, 0xFFC0B084, 0xFFD8C99C};

    static int ramp(int[] r, double v) {
        if (v < 0) v = 0; if (v > 1) v = 1;
        double f = v * (r.length - 1);
        int i = (int) f;
        if (i >= r.length - 1) return r[r.length - 1];
        return lerp(r[i], r[i + 1], (float) (f - i));
    }

    /** Signed "insideness" of a rounded ellipse: >0 inside, ~0 at the rim (in 0..1 radius units). */
    static double ell(double x, double y, double cx, double cy, double hw, double hh) {
        double nx = (x - cx) / hw, ny = (y - cy) / hh;
        return 1.0 - Math.sqrt(nx * nx + ny * ny);
    }

    /** Dome brightness of a rounded form at (x,y) for centre/half-extents, lit top-left. -1 = outside. */
    static double dome(double x, double y, double cx, double cy, double hw, double hh) {
        double nx = (x - cx) / hw, ny = (y - cy) / hh;
        double r2 = nx * nx + ny * ny;
        if (r2 > 1.0) return -1;
        double nz = Math.sqrt(1 - r2);
        double diff = -(nx * LX + ny * LY) + nz * LZ; // surface normal (nx,ny,nz) . light
        double v = 0.42 + 0.58 * diff;
        double r = Math.sqrt(r2);
        if (r > 0.82) v -= (r - 0.82) * 1.7; // form shadow rolling into the rim
        if (r > 0.5 && nx < -0.15 && ny < 0.1) v += (r - 0.5) * 0.5; // rim light on the top-left edge
        return v;
    }

    static void heroPack(String name, int tier, int[] R) throws Exception {
        BufferedImage img = new BufferedImage(PS, PS, BufferedImage.TYPE_INT_ARGB);
        // The closure strap is DARKER LEATHER, not a black slot. The old ramp started at the
        // tier's darkest stop shaded down again, which read as a hole punched through the pack
        // and stole all the contrast from the buckle.
        int[] strap = {shade(R[1], -12), shade(R[2], -10), shade(R[3], -8), shade(R[4], -6)};

        // ---- geometry (32px space): an unmistakable BACKPACK, not a pouch ----
        //  * body is a rounded RECTANGLE (superellipse), gently tapered, with a flat base
        //  * a wide flap is draped over the top third, overhanging the body sides, with a
        //    hard hem line the closure strap crosses -- the single strongest backpack cue
        double bcx = 16, bcy = 19.0, bhh = 11.2;             // body centre + half-height (y ~7.8..30.2)
        double bhwTop = 9.4, bhwBot = 11.8;                  // taper: narrow at the top, wide at the base
        double fcx = 16, fcy = 9.8, fhw = 12.2, fhh = 5.8;   // flap: wide rounded box over the top
        double flapBottom = fcy + fhh;                       // ~15.6, the hem line
        double plcx = 5.2, prcx = 26.8, pcy = 23.0, phw = 3.3, phh = 4.9; // side pockets

        for (int y = 0; y < PS; y++) {
            for (int x = 0; x < PS; x++) {
                double px = x + 0.5, py = y + 0.5;
                int mat = -1; double best = -2; // mat: 0 body, 1 pocket, 2 flap
                // body (trapezoid: half-width grows toward the base)
                double ry = (py - bcy) / bhh;
                double bhw = bhwTop + (bhwBot - bhwTop) * (ry + 1) / 2;
                double vb = superForm(px, py, bcx, bcy, bhw, bhh, 3.7, 1.7, -0.02);
                if (vb > -1) { best = vb; mat = 0; }
                // side pockets, in front of the body's lower sides, a touch recessed
                double vpl = superForm(px, py, plcx, pcy, phw, phh, 3.0, 1.5, -0.05);
                double vpr = superForm(px, py, prcx, pcy, phw, phh, 3.0, 1.5, -0.05);
                double vp = Math.max(vpl, vpr);
                if (vp > -1) { best = vp; mat = 1; }
                // flap draped on top, wins over body/pockets in the upper third
                double vf = superForm(px, py, fcx, fcy, fhw, fhh, 3.2, 1.4, 0.05);
                if (vf > -1) { best = vf; mat = 2; }
                if (mat < 0) continue;

                double v = best;
                // ambient occlusion: darken the body just beneath the flap's hanging hem
                if (mat != 2 && py > flapBottom && py < flapBottom + 2.6) {
                    double fx = Math.abs(px - fcx) / fhw;
                    if (fx < 0.92) v -= 0.26 * (1 - (py - flapBottom) / 2.6) * (1 - fx * 0.4);
                }
                // seam shadow at the top edge where a pocket is sewn to the body
                if (mat == 1 && py < pcy - phh + 2.2) v -= 0.10;
                img.setRGB(x, y, ramp(R, v));
            }
        }

        heroHandle(img, R);                       // top grab loop, peeking over the flap
        heroFlapStrap(img, strap);                // the closure strap down the front
        heroBuckle(img, tier);                    // brass buckle straddling the hem
        heroStitchAndTrim(img, tier, R, fcx, fcy, fhw, fhh);
        outline(img, shade(R[0], -16));
        centerCheck(name, img);
        ImageIO.write(img, "PNG", new File(BASE + "/item/" + name + ".png"));
        RENDERED.put(name, img);
    }

    /** Rounded-box (superellipse) cushion brightness, lit top-left. -1 = outside the form.
     *  power ~3-4 = boxy silhouette; nzScale scales the puffiness; lift shifts overall value. */
    static double superForm(double px, double py, double cx, double cy, double hw, double hh,
                            double power, double nzScale, double lift) {
        double nx = (px - cx) / hw, ny = (py - cy) / hh;
        double e = Math.pow(Math.abs(nx), power) + Math.pow(Math.abs(ny), power);
        if (e > 1) return -1;
        double nz = Math.sqrt(1 - e) * nzScale + 0.35;
        double gx = nx == 0 ? 0 : Math.pow(Math.abs(nx), power - 1) * Math.signum(nx);
        double gy = ny == 0 ? 0 : Math.pow(Math.abs(ny), power - 1) * Math.signum(ny);
        double nl = Math.sqrt(gx * gx + gy * gy + nz * nz);
        gx /= nl; gy /= nl; nz /= nl;
        double diff = -(gx * LX + gy * LY) + nz * LZ;
        double v = 0.30 + 0.64 * diff + lift;
        if (e > 0.72) v -= (e - 0.72) * 0.85; // form shadow rolling into the rim
        return v;
    }

    /** A leather grab-handle loop at the top centre, peeking over the flap (drawn behind it).
     *  Lit on its outer top-left, shaded on the inner edge, so the loop reads as a loop. */
    static void heroHandle(BufferedImage img, int[] R) {
        double cx = 16, cy = 4.3, ohw = 4.3, ohh = 3.8, ihw = 2.3, ihh = 2.5;
        for (int y = 0; y <= 8; y++)
            for (int x = 10; x <= 22; x++) {
                double o = ell(x + 0.5, y + 0.5, cx, cy, ohw, ohh);
                double in = ell(x + 0.5, y + 0.5, cx, cy, ihw, ihh);
                if (o > 0 && in < 0 && (img.getRGB(x, y) >>> 24) == 0) {
                    double nx = (x + 0.5 - cx) / ohw, ny = (y + 0.5 - cy) / ohh;
                    double v = 0.52 - nx * LX * 0.8 - ny * LY * 0.8;
                    if (o < 0.20) v -= 0.16;       // the loop's outer rim rolls into shadow
                    if (in > -0.22) v -= 0.20;     // and so does the inner hole's lip
                    img.setRGB(x, y, ramp(R, v - 0.08));
                }
            }
    }

    /** The closure strap running from under the flap down the front, with thickness and a
     *  tapered tip. Kept as darker LEATHER, not a black slot: the old edge shading bottomed
     *  out on the ramp's darkest stop, which read as a hole and dominated the whole sprite. */
    static void heroFlapStrap(BufferedImage img, int[] strap) {
        for (int y = 10; y <= 26; y++) {
            int x0 = 14, x1 = 17;
            if (y == 26) { x0 = 15; x1 = 16; }                    // the strap's tapered tip
            for (int x = x0; x <= x1; x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) continue;     // stay on the pack
                double nx = (x - 15.5) / 2.0;
                double v = 0.62 - 0.26 * nx;                      // cylinder: lit left, shaded right
                if (x == 14) v -= 0.14;                           // its own thickness at the edges
                if (x == 17) v -= 0.20;
                img.setRGB(x, y, ramp(strap, v));
            }
        }
    }

    /** A brass (or twine, for canvas) buckle on the flap front straddling the hem, with a glint.
     *  The frame carries a hard dark outer edge so the buckle reads as one crisp object against
     *  the leather instead of blurring into it. */
    static void heroBuckle(BufferedImage img, int tier) {
        boolean twine = tier == 0;
        int[] mat = twine ? TWINER : BRASSR;
        int bx = 12, by = 13, bw = 8, bh = 7;
        int outline = twine ? shade(TWINER[0], -14) : shade(BRASSR[0], -12);
        for (int y = by; y < by + bh; y++)
            for (int x = bx; x < bx + bw; x++) {
                boolean frame = x <= bx + 1 || x >= bx + bw - 2 || y <= by + 1 || y >= by + bh - 2;
                if (!frame) continue; // hollow centre shows the strap through the buckle
                boolean outer = x == bx || x == bx + bw - 1 || y == by || y == by + bh - 1;
                double nx = (x - (bx + bw / 2.0)) / (bw / 2.0), ny = (y - (by + bh / 2.0)) / (bh / 2.0);
                double v = 0.56 - 0.30 * nx - 0.26 * ny;
                // twine rides the ramp's PALE stops - a canvas pack's closure is cord, not dark leather
                if (twine) v = 0.46 + 0.50 * v;
                img.setRGB(x, y, outer ? lerp(ramp(mat, v), outline, 0.45f) : ramp(mat, v));
            }
        if (!twine) { // specular glint along the top-left of the frame
            img.setRGB(bx + 2, by + 1, 0xFFF8E6A0);
            img.setRGB(bx + 3, by + 1, 0xFFFDF0C0);
            img.setRGB(bx + 1, by + 2, 0xFFF8E6A0);
        }
        // prong across the hollow
        for (int y = by + 2; y <= by + bh - 3; y++)
            img.setRGB(bx + bw / 2 - 1, y, ramp(mat, twine ? 0.66 : 0.55));
    }

    /**
     * Stitching along the flap hem + the per-tier material story on top of the base render.
     *
     * <p>ART RULE (2026-07-25): big forms read first. Everything here is a DELIBERATE shape with
     * clean edges - a dashed seam, a 2x2 stud, a bevelled plate, a drawn rune. Nothing is a
     * per-pixel sprinkle, because a 32px sprite lands in a 16px slot and any 1px-pitch pattern
     * turns straight to mush there.
     */
    static void heroStitchAndTrim(BufferedImage img, int tier, int[] R,
                                  double fcx, double fcy, double fhw, double fhh) {
        // A real stitched seam: 3-on/1-off dashes on a soft groove, so it reads as a line of
        // thread instead of a row of sparks. (Was every-other-pixel at near-white.)
        int stitch = ramp(R, 0.88);
        int groove = shade(R[1], -8);
        for (int x = 4; x <= 27; x++) {
            if (x >= 14 && x <= 17) continue;              // the closure strap crosses here
            double nx = (x + 0.5 - fcx) / fhw;
            if (Math.abs(nx) > 0.95) continue;
            double ny = Math.pow(Math.max(0, 1 - Math.pow(Math.abs(nx), 3.2)), 1 / 3.2);
            int y = (int) Math.round(fcy + ny * fhh) - 1;
            if (y < 0 || y >= PS) continue;
            if ((img.getRGB(x, y) >>> 24) == 0) continue;
            if (x % 4 != 3) img.setRGB(x, y, stitch);
            setIf(img, x, y + 1, groove);                  // the seam's own shadow, unbroken
        }
        switch (tier) {
            case 0 -> { // canvas: a soft woven rib (2 on, 2 off) + a clean twine lashing
                canvasWeave(img);
                int tw = shade(R[1], -12), twHi = ramp(R, 0.86);
                for (int i = 0; i < 6; i++) { setIf(img, 10 + i, 5 + i, tw); setIf(img, 22 - i, 5 + i, tw); }
                setIf(img, 15, 10, twHi); setIf(img, 16, 10, twHi); // the knot where they cross
            }
            case 1 -> leatherGrain(img, R);       // supple leather grain
            case 2 -> { // studded: leather grain + six real brass studs ringing the flap
                leatherGrain(img, R);
                int[][] studs = {{7, 7}, {12, 4}, {19, 4}, {24, 7}, {6, 12}, {25, 12}};
                for (int[] s : studs) heroStud(img, s[0], s[1]);
            }
            case 3 -> { // reinforced: riveted steel corner plates + a bevelled band across the flap
                leatherGrain(img, R);
                heroPlate(img, 5, 6); heroPlate(img, 23, 6); heroPlate(img, 5, 23); heroPlate(img, 23, 23);
                for (int x = 10; x <= 21; x++) {          // three rows so the band has a lit edge
                    setIf(img, x, 10, ramp(STEELR, 0.82));
                    setIf(img, x, 11, ramp(STEELR, 0.56));
                    setIf(img, x, 12, ramp(STEELR, 0.24));
                }
            }
            case 4 -> { // runed: deep-dyed leather + drawn glyphs with a soft halo + a set gem
                leatherGrain(img, R);
                int glow = 0xFFC0A8FF, glowHi = 0xFFF0E6FF;
                // two drawn runes: a stem with a crossbar and a foot - strokes, not sparkles
                int[][] g1 = {{8, 6}, {8, 7}, {8, 8}, {8, 9}, {8, 10}, {7, 8}, {9, 8}, {7, 11}, {9, 11}};
                int[][] g2 = {{23, 6}, {23, 7}, {23, 8}, {23, 9}, {23, 10}, {22, 7}, {24, 7}, {22, 10}, {24, 10}};
                for (int[] p : g1) setIf(img, p[0], p[1], glow);
                for (int[] p : g2) setIf(img, p[0], p[1], glow);
                setIf(img, 8, 8, glowHi); setIf(img, 23, 8, glowHi);
                // a sigil low on the body, drawn as a small diamond
                for (int[] p : new int[][]{{16, 23}, {15, 24}, {17, 24}, {16, 25}}) setIf(img, p[0], p[1], glow);
                runeHalo(img, glow);                       // soft bloom so the glyphs glow, not speckle
                // gem set in the buckle centre
                img.setRGB(15, 16, 0xFF8A6BE0); img.setRGB(16, 16, 0xFFC0A8FF);
                img.setRGB(15, 17, 0xFF6E4FC0); img.setRGB(16, 17, 0xFF9C82E8);
            }
            default -> {}
        }
    }

    /** Canvas: a low-contrast 2-on/2-off weave rib in both directions. Coarse enough to survive
     *  a 2:1 downscale, quiet enough that the pack's form still reads first. */
    static void canvasWeave(BufferedImage img) {
        for (int y = 0; y < PS; y++)
            for (int x = 0; x < PS; x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) continue;
                int d = ((x % 4) < 2 ? 3 : -3) + ((y % 4) < 2 ? 2 : -2);
                img.setRGB(x, y, shade(img.getRGB(x, y), d));
            }
    }

    /**
     * Leather grain, LOW frequency. The old version added per-pixel random +/-3 on top of a
     * 5-row crease, which is the speckle that made the packs read as busy; at hotbar size it
     * dissolved the form into static. This is a soft crease every 7 rows plus one large, gently
     * interpolated mottle - visible as leather up close, invisible as noise when small.
     */
    static void leatherGrain(BufferedImage img, int[] R) {
        for (int y = 0; y < PS; y++)
            for (int x = 0; x < PS; x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) continue;
                double crease = Math.cos((y + 1) * Math.PI * 2 / 7.0);
                int d = (int) Math.round(-2.5 * Math.max(0, crease));      // only the trough darkens
                d += (int) Math.round(5.0 * (smoothNoise(x, y, 6, 11) - 0.5)); // big soft mottle
                if (d != 0) img.setRGB(x, y, shade(img.getRGB(x, y), d));
            }
    }

    /** A 2x2 brass stud, lit top-left, with a contact shadow - a shape, not a sprinkle. */
    static void heroStud(BufferedImage img, int x, int y) {
        if ((img.getRGB(x, y) >>> 24) == 0) return;
        setIf(img, x, y, ramp(BRASSR, 0.93));
        setIf(img, x + 1, y, ramp(BRASSR, 0.72));
        setIf(img, x, y + 1, ramp(BRASSR, 0.64));
        setIf(img, x + 1, y + 1, ramp(BRASSR, 0.40));
        setIf(img, x + 2, y + 2, ramp(BRASSR, 0.12)); // contact shadow, one pixel
    }

    /** A bevelled 4x4 steel plate with a brass rivet: lit top-left edge, shaded bottom-right. */
    static void heroPlate(BufferedImage img, int x, int y) {
        for (int j = 0; j < 4; j++)
            for (int i = 0; i < 4; i++) {
                if ((img.getRGB(x + i, y + j) >>> 24) == 0) continue;
                double v = 0.58;
                if (i == 0 || j == 0) v = 0.84;                 // lit bevel
                if (i == 3 || j == 3) v = 0.22;                 // shaded bevel
                if ((i == 3 && j == 0) || (i == 0 && j == 3)) v = 0.52;
                img.setRGB(x + i, y + j, ramp(STEELR, v));
            }
        setIf(img, x + 1, y + 1, ramp(BRASSR, 0.88));           // brass rivet head
        setIf(img, x + 2, y + 2, ramp(BRASSR, 0.46));           // its shaded side
    }

    /** One-pixel bloom around the rune strokes so they read as GLOWING rather than as stray dots. */
    static void runeHalo(BufferedImage img, int glow) {
        BufferedImage src = new BufferedImage(PS, PS, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PS; y++) for (int x = 0; x < PS; x++) src.setRGB(x, y, img.getRGB(x, y));
        for (int y = 0; y < PS; y++)
            for (int x = 0; x < PS; x++) {
                if (src.getRGB(x, y) != glow) continue;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= PS || ny >= PS) continue;
                    int c = img.getRGB(nx, ny);
                    if ((c >>> 24) == 0 || c == glow) continue;
                    img.setRGB(nx, ny, lerp(c, glow, 0.30f));
                }
            }
    }

    static void setIf(BufferedImage img, int x, int y, int c) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) return;
        if ((img.getRGB(x, y) >>> 24) != 0) img.setRGB(x, y, c);
    }

    /** Darken opaque pixels that border transparency, for a clean selective 1px outline. */
    static void outline(BufferedImage img, int col) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] edge = new boolean[w][h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) continue;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h || (img.getRGB(nx, ny) >>> 24) == 0) { edge[x][y] = true; break; }
                }
            }
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (edge[x][y]) img.setRGB(x, y, blendToward(img.getRGB(x, y), col, 0.7f));
    }

    /** Blend a colour toward a target by t (keeps a hint of the surface hue in the outline). */
    static int blendToward(int c, int t, float f) { return lerp(c, t, f); }

    /** Print a warning if the opaque content isn't reasonably centred / hits the sheet edge. */
    static void centerCheck(String name, BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight(), minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            if ((img.getRGB(x, y) >>> 24) > 8) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y); }
        if (maxX < 0) return;
        if (minX == 0 || minY == 0 || maxX == w - 1 || maxY == h - 1)
            System.out.println("  ! " + name + " touches the sheet edge (box " + minX + "," + minY + " -> " + maxX + "," + maxY + ")");
    }

    // =====================================================================
    //  trinkets - each its own readable silhouette
    // =====================================================================

    static void genTrinkets() throws Exception {
        // Lodestone Charm: a dark iron-grey magnetite stone on a cord, faint violet sheen
        drawItem("lodestone_charm", LODESTONE,
                with(base(), '1', 0xFF20242C, '2', 0xFF363C46, '3', 0xFF525A66, '4', 0xFF6E6E90, '5', 0xFF9A94C0));
        // Compass Rose: round brass case, cream dial, red north needle
        drawItem("compass_rose", COMPASS, base());
        // Restock Strap: a leather bandolier - bold central brass buckle + two studded pouches
        drawItem("restock_strap", RESTOCK, base());
        // Bottomless Lining: dark drawstring pouch with a violet void mouth
        drawItem("bottomless_lining", BOTTOMLESS,
                with(base(), '1', 0xFF3A2A5E, '2', 0xFF6A4EA0, '3', 0xFFA98CD8, '4', 0xFFE2D4F6));
        // Repair Kit: rolled canvas tool wrap, hammer + awl poking out
        drawItem("repair_kit", REPAIR, base());
        // Quick-Draw Straps: two BUCKLED brown leather straps crossed (not a red X)
        drawItem("quick_draw_straps", QUICKDRAW, base());
        // Quill & Ledger: open ledger + a white feather quill with an ink nib
        drawItem("quill_and_ledger", QUILL, base());
        // Waterskin Rack: a corked leather waterskin with a water sheen
        drawItem("waterskin_rack", WATERSKIN,
                with(base(), '1', 0xFF2E6FA8, '2', 0xFF4E93C6, '3', 0xFF8FC0E4));
        // Soul Vial: single tall corked vial of glowing green soul-liquid + a wisp
        drawItem("soul_vial", SOULVIAL,
                with(base(), '1', 0xFF1F6B2E, '2', 0xFF37A03F, '3', 0xFF63C85A, '4', 0xFF9CE88F));
        // Charge Crystal: cool faceted crystal wound in dark copper, on a brass mount (not a flame)
        drawItem("charge_crystal", CRYSTAL,
                with(base(), '1', 0xFF16515E, '2', 0xFF2E8CA0, '3', 0xFF5CC3D6, '4', 0xFFAEEFF7));
        // Flask Harness: a wooden rack of two round vapour flasks
        drawItem("flask_harness", FLASKS,
                with(base(), '1', 0xFF5E3A8A, '2', 0xFF8A5CC0, '3', 0xFFB48CE0, '4', 0xFFD9C4F5));

        // ---- 2026-07-25 batch: seven more fittings, authored in the de-noised style ----
        // Tinker's Kit: a leather tool roll with two tools stood in it
        drawItem("tinkers_kit", TINKERS, base());
        // Field Furnace: a brass brazier of banked embers
        drawItem("field_furnace", FURNACE,
                with(base(), '1', 0xFF7A2A0C, '2', 0xFFC85A16, '3', 0xFFF29A32, '4', 0xFFFFD46B));
        // Provisioner's Pouch: a canvas ration pouch with a loaf over the lip
        drawItem("provisioners_pouch", PROVISIONER,
                with(base(), '1', 0xFF8A5A24, '2', 0xFFC98E44));
        // Cartographer's Sleeve: a rolled chart in a leather sleeve, wax-sealed
        drawItem("cartographers_sleeve", CARTOGRAPHER, base());
        // Angler's Creel: a woven creel with a fish tail over the lid
        drawItem("anglers_creel", CREEL,
                with(base(), '1', 0xFF4A6E7E, '3', 0xFF8FB6C6));
        // Torchbearer's Loop: a torch on a corded loop, lit
        drawItem("torchbearers_loop", TORCHBEARER,
                with(base(), '1', 0xFF7A2A0C, '3', 0xFFF29A32, '4', 0xFFFFD46B));
        // Herbalist's Bundle: a twine-tied cloth bundle with a green sprig
        drawItem("herbalists_bundle", HERBALIST,
                with(base(), '1', 0xFF2F5A24, '2', 0xFF4E8C36, '3', 0xFF79B84E));
    }

    static final String[] TINKERS = {
        "................", // 0
        "...zSSz..zSz....", // 1  a hammer head and a chisel stood in the roll
        "...zssz..zsz....", // 2
        "....LL....L.....", // 3  wooden handles
        "....LL....L.....", // 4
        "....LL....L.....", // 5
        "..rrrrrrrrrrrr..", // 6  the roll's top hem
        "..rLLLLLLLLLLr..", // 7
        "..rLeLLLLLLeLr..", // 8  lit creases where it's rolled
        "..rLLLnaaanLLr..", // 9  brass tie buckle
        "..rLLLnABAnLLr..", // 10
        "..rLLLnaaanLLr..", // 11
        "..rLeLLLLLLeLr..", // 12
        "..rrrrrrrrrrrr..", // 13 bottom hem
        "................", // 14
        "................", // 15
    };

    static final String[] FURNACE = {
        "................", // 0
        "................", // 1
        ".......4........", // 2  a lick of flame
        "......343.......", // 3
        ".....34443......", // 4
        "....n32223n.....", // 5  embers over the rim
        "...nAaaaaaAn....", // 6  brass bowl
        "...nA12121An....", // 7  banked coals
        "...nA11211An....", // 8
        "....nAaaaAn.....", // 9  bowl underside
        ".....ndddn......", // 10
        "......ndn.......", // 11 stem
        ".....nadan......", // 12
        "....ndaaadn.....", // 13 foot
        "................", // 14
        "................", // 15
    };

    static final String[] PROVISIONER = {
        "................", // 0
        "......1221......", // 1  a round loaf over the lip
        ".....122221.....", // 2
        "....11222211....", // 3
        "....11111111....", // 4  crust
        "...wvcCCCCcvw...", // 5  canvas pouch mouth
        "...wcCCCCCCcw...", // 6
        "...wcCnaaancw...", // 7  brass clasp
        "...wcCnABAncw...", // 8
        "...wcCnaaancw...", // 9
        "...wcCCCCCCcw...", // 10
        "...wcCCCCCCcw...", // 11
        "...wvcCCCCcvw...", // 12
        "....wvccccvw....", // 13
        ".....wwwwww.....", // 14 base
        "................", // 15
    };

    static final String[] CARTOGRAPHER = {
        "................", // 0
        "................", // 1
        "....CCCCCCCC....", // 2  the chart rolled out either end
        "...CWWWWWWWWC...", // 3
        "..rrrrrrrrrrrr..", // 4  leather sleeve
        "..rLLLLLLLLLLr..", // 5
        "..rLeLLLLLLLLr..", // 6
        "..rLLLXYYXLLLr..", // 7  wax seal
        "..rLLLXZZXLLLr..", // 8
        "..rLLLXYYXLLLr..", // 9
        "..rLeLLLLLLLLr..", // 10
        "..rrrrrrrrrrrr..", // 11
        "...CWWWWWWWWC...", // 12
        "....CCCCCCCC....", // 13
        "................", // 14
        "................", // 15
    };

    static final String[] CREEL = {
        "................", // 0
        "................", // 1
        "........1.......", // 2  a tail over the lid
        ".......131......", // 3
        "......13331.....", // 4
        "...wcCcCcCcCw...", // 5  woven wicker, low contrast so it reads as weave not check
        "...wCcCcCcCcw...", // 6
        "...wvvvvvvvvw...", // 7  a bound band
        "...wcCnaaancw...", // 8  brass clasp
        "...wcCnABAncw...", // 9
        "...wcCnaaancw...", // 10
        "...wvvvvvvvvw...", // 11 second band
        "...wcCcCcCcCw...", // 12
        "...wCcCcCcCcw...", // 13
        "....wwwwwwww....", // 14 base
        "................", // 15
    };

    static final String[] TORCHBEARER = {
        "................", // 0
        ".......4........", // 1  flame
        "......343.......", // 2
        ".....34443......", // 3
        ".....13341......", // 4
        "......1331......", // 5  embers settling on the head
        ".......rLr......", // 6  shaft
        ".......rLr......", // 7
        "......rrLrr.....", // 8  the cord wrapped round it
        "......rLLLr.....", // 9
        ".......rLr......", // 10
        ".......rLr......", // 11
        ".......rLr......", // 12
        "......naaan.....", // 13 brass ferrule
        ".......nan......", // 14
        "................", // 15
    };

    static final String[] HERBALIST = {
        "................", // 0
        "......3...3.....", // 1  a sprig of green
        ".....323.323....", // 2
        "......32232.....", // 3
        ".......121......", // 4  stem
        "....wvccccvw....", // 5  the bundle, gathered at the neck
        "...wcCCCCCCcw...", // 6
        "...wcCCCCCCcw...", // 7
        "...wvvvvvvvvw...", // 8  twine tie
        "...wcCCCCCCcw...", // 9
        "...wcCCCCCCcw...", // 10
        "...wvvvvvvvvw...", // 11 second tie
        "...wcCCCCCCcw...", // 12
        "....wvccccvw....", // 13
        ".....wwwwww.....", // 14
        "................", // 15
    };

    //        0123456789012345
    static final String[] LODESTONE = {
        "................", // 0
        ".......rr.......", // 1  leather cord
        ".......rr.......", // 2
        ".....122221.....", // 3  magnetite stone
        "....12344421....", // 4
        "...1234554321...", // 5  violet sheen on the top-left
        "...1235543321...", // 6
        "...1234433221...", // 7
        "...1233332221...", // 8
        "...1123322211...", // 9
        "....11332211....", // 10
        "....11222111....", // 11
        ".....112211.....", // 12
        "......1111......", // 13 base
        "................", // 14
        "................", // 15
    };

    static final String[] COMPASS = {
        "................", // 0
        ".....nAAaan.....", // 1  brass bezel top
        "...nABccccadn...", // 2
        "..nAcccccccddn..", // 3
        ".nAccccYcccccdn.", // 4  dial + red north tip
        ".ncccccYYcccccn.", // 5
        ".nccccYZZYccccn.", // 6  needle widest
        ".ncccccBaccccdn.", // 7  brass centre pin
        ".nccccccQoccccn.", // 8  white south begins
        ".ncccccSQcccccn.", // 9
        ".ndcccccSccccdn.", // 10 south tip
        "..ndcccccccdn...", // 11
        "...nddccccddn...", // 12
        ".....ndddan.....", // 13
        "................", // 14
        "................", // 15
    };

    static final String[] RESTOCK = {
        "................", // 0
        "......rLLr......", // 1  bandolier belt
        "...wcccccccw....", // 2  top pouch
        "...cCvaaavCc....", // 3  flap + brass stud
        "...wcccccccw....", // 4
        "......rLLr......", // 5  belt
        ".....nAAAAn.....", // 6  bold brass buckle
        ".....nArrAn.....", // 7  frame, belt through it
        ".....nArrAn.....", // 8
        ".....nAAAAn.....", // 9
        "......rLLr......", // 10 belt
        "...wcccccccw....", // 11 bottom pouch
        "...cCvaaavCc....", // 12 flap + brass stud
        "...wcccccccw....", // 13
        "......rLLr......", // 14 belt tail
        "................", // 15
    };

    static final String[] BOTTOMLESS = {
        "................", // 0
        "......d..d......", // 1  drawstring aglets
        ".....rl..lr.....", // 2  drawstring
        "....lrLLLLrl....", // 3  cinched neck
        "...lLL1441LLl...", // 4  void mouth (violet, glow centre)
        "..lLLL1441LLLl..", // 5
        "..rLLLL11LLLLr..", // 6  void closing into the leather
        "..rLLLLLLLLLLr..", // 7  pouch belly
        ".rLLLLLLLLLLLLr.", // 8
        ".rLLLLLLLLLLLLr.", // 9
        ".rlLLLLLLLLLLlr.", // 10 belly shadow
        "..rlLLLLLLLLlr..", // 11
        "...rllLLLLllr...", // 12 base
        ".....rllllr.....", // 13
        "................", // 14
        "................", // 15
    };

    static final String[] REPAIR = {
        "................", // 0
        "..zSSSz.........", // 1  hammer head (steel)
        "..zSQQz.l.......", // 2  head + handle peg
        "..zSSSz.Ll......", // 3
        "..zxssz.Ll......", // 4  head underside + wood handle
        ".......LLl......", // 5  handle (diagonal)
        "........LLl.....", // 6
        ".........LLl....", // 7
        "..........Ll....", // 8  handle meets the anvil
        "...zsSSSSSSsz...", // 9  anvil top face
        "....zxxxxxxz....", // 10 anvil under-shadow
        "......zssz......", // 11 anvil waist
        ".....zsSSsz.....", // 12 anvil base flare
        "....zssssssz....", // 13 anvil base
        "................", // 14
        "................", // 15
    };

    static final String[] QUICKDRAW = {
        "................", // 0
        "...rLl....rLl...", // 1  two leather belts
        "...rLl....rLl...", // 2
        "...rLl....rLl...", // 3
        "..naaan...rLl...", // 4  left brass buckle
        "..nAlAn...rLl...", // 5  buckle frame + belt through it
        "..naBan...rLl...", // 6  buckle prong
        "..nAlAn..naaan..", // 7  right buckle begins
        "..naaan..nAlAn..", // 8
        "...rLl...naBan..", // 9  right prong
        "...rLl...nAlAn..", // 10
        "...rLl...naaan..", // 11
        "...rLl....rLl...", // 12 belt tails
        "...rLl....rLl...", // 13
        "...rl......rl...", // 14 pointed tips
        "................", // 15
    };

    static final String[] QUILL = {
        "................", // 0
        "............oo..", // 1  feather tip
        "...........oQo..", // 2  white quill (barb + shaft)
        "..........oQo...", // 3
        ".........oQo....", // 4
        "........oQo.....", // 5
        ".......oQo......", // 6
        ".rrrrrrrorrrrrr.", // 7  ledger top cover + shaft crossing
        ".rCCCCCowCCCCCr.", // 8  cream pages + centre gutter
        ".rCCCCoCwCCCCCr.", // 9
        ".rCCCzCCwCCCCCr.", // 10 ink nib on the page
        ".rCvvCCCwCvvCCr.", // 11 ruled lines
        ".rCCCCCCwCCCCCr.", // 12
        ".rrrrrrrrrrrrrr.", // 13 ledger bottom cover
        "................", // 14
        "................", // 15
    };

    static final String[] WATERSKIN = {
        "................", // 0
        "......rLr.......", // 1  cork
        "......rLr.......", // 2
        ".....naaan......", // 3  brass collar
        "....reLLLer.....", // 4  shoulders
        "...reLLLLLLer...", // 5
        "..re3LLLLLLLer..", // 6  body + water sheen (blue)
        "..re33LLLLLLer..", // 7
        "..reL3LLLLLLer..", // 8  specular
        "..reLLLLLLLLer..", // 9
        "..reLLLLLLLLer..", // 10
        "..rreLLLLLLerr..", // 11 seam
        "...reLLLLLLer...", // 12
        "....rrLLLLrr....", // 13 base
        "................", // 14
        "................", // 15
    };

    static final String[] SOULVIAL = {
        "................", // 0
        "......rLLr......", // 1  cork
        "......kllk......", // 2  cork rim
        "......jggj......", // 3  neck
        ".....jgGGgj.....", // 4  neck flares
        "....jgGGgggj....", // 5  empty glass above liquid
        "....j344332j....", // 6  liquid surface (hi)
        "....jo33222j....", // 7  specular
        "....j322212j....", // 8
        "....j222111j....", // 9
        "....j221111j....", // 10
        "....j211111j....", // 11
        ".....j1111j.....", // 12 rounded base
        "......j11j......", // 13
        "................", // 14
        "................", // 15
    };

    static final String[] CRYSTAL = {
        "................", // 0
        ".......4........", // 1  spark at the tip
        ".......14.......", // 2  crystal point
        "......1442......", // 3  faceted (light left / mid right)
        ".....134432.....", // 4
        "....13444322....", // 5
        "....1uUHuUu2....", // 6  dark copper wire band
        "....13444322....", // 7
        "....13443222....", // 8
        ".....134322.....", // 9
        ".....1uUHu2.....", // 10 lower copper wire band
        "......1322......", // 11
        ".......12.......", // 12 base point
        "......nBBn......", // 13 brass mount
        "......naan......", // 14
        "................", // 15
    };

    static final String[] FLASKS = {
        "................", // 0
        "................", // 1
        "..LLLLLLLLLLLL..", // 2  wooden top rail
        "..llllllllllll..", // 3
        "..l..r....r..l..", // 4  uprights + flask corks
        "..l..j....j..l..", // 5  necks
        "..l.jgj..jgj.l..", // 6  bulb tops
        "..lj4o2j.j4o2jl.", // 7  round bulbs, vapour + specular
        "..lj332j.j332jl.", // 8
        "..lj222j.j222jl.", // 9
        "..lj221j.j221jl.", // 10
        "..l.jj....jj.l..", // 11 bulb bases
        "..l..........l..", // 12
        "..LLLLLLLLLLLL..", // 13 bottom shelf
        "..llllllllllll..", // 14
        "................", // 15
    };

    static final String[] HANDBOOK = {
        "................", // 0
        "..BrrrrrrrrrrB..", // 1  cover top + brass corners
        "..rLLLLLLLLLCr..", // 2  cover + page edge (right)
        "..rLeLLLLLLLCr..", // 3  leather highlight
        "..rLLnaaanLLCr..", // 4  brass clasp
        "..rLLnaBanLLCr..", // 5  clasp emboss
        "..rLLnaaanLLCr..", // 6
        "..rLeLLLLLLLCr..", // 7
        "..rLLLLLLLLLCr..", // 8
        "..rLLLLLLLLLCr..", // 9
        "..rLeLLLLLLLCr..", // 10
        "..BrrrrrrrrrrB..", // 11 cover bottom + corners
        ".....YY.........", // 12 bookmark tail
        ".....YY.........", // 13
        ".....XX.........", // 14
        "................", // 15
    };

    // =====================================================================
    //  montage - upscaled sheet of every generated item sprite (scratchpad)
    // =====================================================================

    static void writeMontage(String path) throws Exception {
        int cell = 224, gap = 10, cols = 6, bg = 0xFF3A3A40; // sprites normalise to a 224px cell
        int rows = (RENDERED.size() + cols - 1) / cols;
        int mw = cols * cell + (cols + 1) * gap, mh = rows * cell + (rows + 1) * gap;
        BufferedImage m = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < mh; y++) for (int x = 0; x < mw; x++) m.setRGB(x, y, bg); // flat plain field
        int i = 0;
        for (Map.Entry<String, BufferedImage> e : RENDERED.entrySet()) {
            int cx0 = gap + (i % cols) * (cell + gap), cy0 = gap + (i / cols) * (cell + gap);
            BufferedImage s = e.getValue();
            int n = s.getWidth(), scale = Math.max(1, cell / n);
            for (int y = 0; y < n; y++)
                for (int x = 0; x < n; x++) {
                    int c = s.getRGB(x, y);
                    if ((c >>> 24) == 0) continue; // let the plain field show through
                    for (int dy = 0; dy < scale; dy++)
                        for (int dx = 0; dx < scale; dx++)
                            m.setRGB(cx0 + x * scale + dx, cy0 + y * scale + dy, c);
                }
            i++;
        }
        new File(path).getParentFile().mkdirs();
        ImageIO.write(m, "PNG", new File(path));
    }

    /** Box-average downscale of a square ARGB sprite (premultiplied so edges don't fringe). */
    static BufferedImage downscale(BufferedImage s, int n) {
        int S = s.getWidth();
        BufferedImage o = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        double sc = S / (double) n;
        for (int y = 0; y < n; y++)
            for (int x = 0; x < n; x++) {
                double a = 0, r = 0, g = 0, b = 0, cnt = 0;
                int x0 = (int) (x * sc), x1 = (int) ((x + 1) * sc), y0 = (int) (y * sc), y1 = (int) ((y + 1) * sc);
                for (int sy = y0; sy < Math.max(y0 + 1, y1); sy++)
                    for (int sx = x0; sx < Math.max(x0 + 1, x1); sx++) {
                        int c = s.getRGB(sx, sy);
                        double al = (c >>> 24) & 0xFF;
                        a += al; r += al * ((c >> 16) & 0xFF); g += al * ((c >> 8) & 0xFF); b += al * (c & 0xFF); cnt++;
                    }
                int A = (int) Math.round(a / cnt);
                int R = a > 0 ? (int) Math.round(r / a) : 0, G = a > 0 ? (int) Math.round(g / a) : 0, B = a > 0 ? (int) Math.round(b / a) : 0;
                o.setRGB(x, y, (A << 24) | (R << 16) | (G << 8) | B);
            }
        return o;
    }

    /** The five packs downscaled to hotbar sizes (16/12px) on a stone-grey slot strip, so the
     *  "does it read as a backpack when small?" question can be judged without launching a client. */
    static void writeSmallPreview(String path) throws Exception {
        String[] packs = {"canvas_pack", "leather_pack", "studded_pack", "reinforced_pack", "runed_pack"};
        int[] sizes = {16, 12};
        int slot = 22, gap = 4, up = 8; // each cell upscaled by `up` for eyeballing
        int cols = packs.length, rows = sizes.length;
        int cellW = slot * up, cellH = slot * up;
        int mw = cols * cellW + (cols + 1) * gap, mh = rows * cellH + (rows + 1) * gap;
        BufferedImage m = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < mh; y++) for (int x = 0; x < mw; x++) m.setRGB(x, y, 0xFF2B2B30);
        for (int ri = 0; ri < rows; ri++) {
            int n = sizes[ri];
            for (int ci = 0; ci < cols; ci++) {
                BufferedImage src = RENDERED.get(packs[ci]);
                BufferedImage small = downscale(src, n);
                int cx0 = gap + ci * (cellW + gap), cy0 = gap + ri * (cellH + gap);
                // grey slot background with a vanilla-ish bevel
                for (int y = 0; y < cellH; y++) for (int x = 0; x < cellW; x++) m.setRGB(cx0 + x, cy0 + y, 0xFF8B8B8B);
                int off = ((slot - n) / 2) * up; // centre the small sprite in the slot
                for (int y = 0; y < n; y++)
                    for (int x = 0; x < n; x++) {
                        int c = small.getRGB(x, y);
                        if ((c >>> 24) < 8) continue;
                        for (int dy = 0; dy < up; dy++)
                            for (int dx = 0; dx < up; dx++)
                                m.setRGB(cx0 + off + x * up + dx, cy0 + off + y * up + dy, c);
                    }
            }
        }
        new File(path).getParentFile().mkdirs();
        ImageIO.write(m, "PNG", new File(path));
    }

    // =====================================================================
    //  procedural GUI / tab / block surfaces (unchanged - tiled grain reads well)
    // =====================================================================

    static void genGui(String path) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(7);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                float t = y / (float) H;
                int c = lerp(LEATHER_HI, LEATHER_LO, t);
                int n = (int) ((valueNoise(x, y, rnd, 0) - 0.5f) * 16);
                c = shade(c, n);
                img.setRGB(x, y, c);
            }
        }
        brassFrame(img, 0, 0, W, H, 3);
        fillRect(img, 6, 5, W - 12, 12, shade(LEATHER_LO, -8));
        stitchH(img, 8, 16, W - 8);
        panel(img, 7, 18, W - 14, 13, CANVAS, CANVAS_LO);
        stitchRect(img, 7, 18, W - 14, 13);
        for (int r = 0; r < 6; r++)
            for (int c = 0; c < 9; c++)
                slot(img, 8 + c * 18, 34 + r * 18);
        int dy = 150;
        fillRect(img, 6, dy, W - 12, 2, BRASS_LO);
        fillRect(img, 6, dy + 1, W - 12, 1, BRASS_HI);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                slot(img, 8 + c * 18, 158 + r * 18);
        for (int c = 0; c < 9; c++)
            slot(img, 8 + c * 18, 216);
        ImageIO.write(img, "PNG", new File(path));
    }

    static void genTab(String path) throws Exception {
        int tw = 26, th = 24;
        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(5);
        for (int y = 0; y < th; y++) {
            for (int x = 0; x < tw; x++) {
                int c;
                if (x >= tw - 2) {
                    c = LEATHER_EDGE;
                } else if (x < 3) {
                    c = x == 0 ? BRASS_LO : (x == 1 ? BRASS : BRASS_HI);
                } else {
                    float t = y / (float) th;
                    c = lerp(LEATHER_HI, LEATHER_LO, t);
                    int n = (int) ((valueNoise(x, y, rnd, 2) - 0.5f) * 14);
                    c = shade(c, n);
                }
                img.setRGB(x, y, c);
            }
        }
        img.setRGB(0, 0, 0); img.setRGB(0, th - 1, 0);
        for (int x = 5; x < tw - 3; x += 3) { img.setRGB(x, 2, STITCH); img.setRGB(x, th - 3, STITCH); }
        ImageIO.write(img, "PNG", new File(path));
    }

    static void slot(BufferedImage img, int x, int y) {
        int px = x - 1, py = y - 1;
        fillRect(img, px, py, 18, 18, SLOT_HOLE);
        hline(img, px, px + 17, py, SLOT_SH);
        vline(img, px, py, py + 17, SLOT_SH);
        hline(img, px, px + 17, py + 17, SLOT_HI);
        vline(img, px + 17, py, py + 17, SLOT_HI);
    }

    static void brassFrame(BufferedImage img, int x, int y, int w, int h, int t) {
        for (int i = 0; i < t; i++) {
            int col = i == 0 ? BRASS_HI : (i == t - 1 ? BRASS_LO : BRASS);
            rect(img, x + i, y + i, w - 2 * i, h - 2 * i, col);
        }
        rivet(img, x + 4, y + 4);
        rivet(img, x + w - 5, y + 4);
        rivet(img, x + 4, y + h - 5);
        rivet(img, x + w - 5, y + h - 5);
    }

    static void rivet(BufferedImage img, int cx, int cy) {
        fillRect(img, cx - 1, cy - 1, 3, 3, BRASS_LO);
        img.setRGB(cx, cy, BRASS_HI);
    }

    /** A domed brass rivet for the 32px block brass tile. */
    static void blockRivet(BufferedImage img, int cx, int cy) {
        for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) set(img, cx + i, cy + j, BRASS_LO);
        set(img, cx, cy, BRASS_HI); set(img, cx - 1, cy - 1, BRASS); set(img, cx, cy - 1, BRASS_HI);
    }

    static void panel(BufferedImage img, int x, int y, int w, int h, int hi, int lo) {
        for (int j = 0; j < h; j++) {
            int c = lerp(hi, lo, j / (float) h);
            hline(img, x, x + w - 1, y + j, c);
        }
        rect(img, x, y, w, h, shade(lo, -30));
    }

    // =====================================================================
    //  per-tier placed-block faces: leather (body/sides/top) + a trimmed FRONT (the flap
    //  face). Colour is baked from the shared TIER_RAMP (no tint), and the per-tier trim
    //  from the hero-item ladder is carried onto the front so a set-down pack shows its
    //  tier -- canvas weave+twine, leather grain, brass studs, riveted steel plates+band,
    //  runed glowing glyphs+gem. The 3D brass buckle/straps are a shared brass texture.
    // =====================================================================

    static void genBlockTiers() throws Exception {
        for (int t = 0; t < TIER_ID.length; t++) {
            genBlockLeatherTier(t);
            genBlockFrontTier(t);
        }
    }

    /** Plain colour-baked leather for the body/sides/top/handle: grain, a stitched seam, a bevel. */
    static void genBlockLeatherTier(int tier) throws Exception {
        int N = 32;
        int[] R = TIER_RAMP[tier];
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        int hi = R[5], lo = R[2];
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                int c = lerp(hi, lo, y / (float) N);
                // low-frequency mottle + one soft crease per 8 rows (was per-pixel static)
                int d = (int) Math.round(5.0 * (smoothNoise(x, y, 7, 12 + tier) - 0.5));
                d += (int) Math.round(-3.0 * Math.max(0, Math.cos(y * Math.PI * 2 / 8.0)));
                img.setRGB(x, y, shade(c, d));
            }
        int stitch = ramp(R, 0.88);
        for (int x = 0; x < N; x++) img.setRGB(x, 11, shade(lo, -18));                 // seam groove
        for (int x = 0; x < N; x++) if (x % 4 != 3) img.setRGB(x, 10, stitch);          // 3-on/1-off thread
        for (int i = 0; i < N; i++) {                                                   // quilted bevel
            img.setRGB(i, 0, shade(hi, 14)); img.setRGB(0, i, shade(hi, 10));
            img.setRGB(i, N - 1, shade(lo, -28)); img.setRGB(N - 1, i, shade(lo, -24));
        }
        ImageIO.write(img, "PNG", new File(BASE + "/block/pack_" + TIER_ID[tier] + "_leather.png"));
    }

    /** The flap FRONT face: colour-baked grain + a hem stitch line + the tier's trim. The 3D
     *  brass buckle covers the centre box (x11..21, y15..26) and the straps the bottom corners,
     *  so trim is kept to the flap's edges, top band and flanks where it actually reads. */
    static void genBlockFrontTier(int tier) throws Exception {
        int N = 32;
        int[] R = TIER_RAMP[tier];
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        int hi = R[5], lo = R[3];
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                int c = lerp(hi, lo, y / (float) N);
                int d = (int) Math.round(4.0 * (smoothNoise(x, y, 6, 41 + tier) - 0.5));
                d += (int) Math.round(-2.5 * Math.max(0, Math.cos(y * Math.PI * 2 / 7.0)));
                img.setRGB(x, y, shade(c, d));
            }
        // hem stitch line near the bottom (the flap's hanging edge): a groove + a 3-on/1-off thread
        int stitch = ramp(R, 0.88);
        for (int x = 0; x < N; x++) img.setRGB(x, 29, shade(lo, -20));
        for (int x = 0; x < N; x++) if (x % 4 != 3) img.setRGB(x, 28, stitch);
        boolean[][] free = new boolean[N][N]; // buckle box + strap corners are hidden by 3D brass
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++)
            free[x][y] = !(x >= 11 && x <= 21 && y >= 15 && y <= 26)
                    && !(y >= 26 && (x <= 10 || x >= 21));
        switch (tier) {
            case 0 -> { // canvas: a soft 2-on/2-off weave rib + a clean twine lashing
                for (int y = 1; y < 27; y++)
                    for (int x = 1; x < N - 1; x++) {
                        if (!free[x][y]) continue;
                        int d = ((x % 4) < 2 ? 3 : -3) + ((y % 4) < 2 ? 2 : -2);
                        img.setRGB(x, y, shade(img.getRGB(x, y), d));
                    }
                int tw = shade(R[1], -8), twHi = ramp(R, 0.86);
                for (int i = 0; i < 10; i++) { // an X of twine across the upper flap
                    blockSet(img, free, 7 + i, 4 + i, tw); blockSet(img, free, 25 - i, 4 + i, tw);
                }
                blockSet(img, free, 16, 13, twHi); blockSet(img, free, 15, 13, twHi); // the crossing knot
            }
            case 1 -> {} // leather: the grain carries it
            case 2 -> { // studded: six real brass studs ringing the flap (was eleven 1px sprinkles)
                int[][] studs = {{5, 6}, {13, 4}, {19, 4}, {26, 6}, {4, 15}, {27, 15}};
                for (int[] s : studs) blockStud(img, free, s[0], s[1]);
            }
            case 3 -> { // reinforced: riveted steel corner plates + a bevelled band across the top
                blockPlate(img, free, 2, 3); blockPlate(img, free, 25, 3);
                blockPlate(img, free, 2, 21); blockPlate(img, free, 25, 21);
                for (int x = 9; x <= 22; x++) {
                    blockSet(img, free, x, 10, ramp(STEELR, 0.84));
                    blockSet(img, free, x, 11, ramp(STEELR, 0.56));
                    blockSet(img, free, x, 12, ramp(STEELR, 0.22));
                }
            }
            case 4 -> { // runed: drawn glyphs flanking the buckle + a set gem
                int glow = 0xFFC0A8FF, glowHi = 0xFFF0E6FF, gem = 0xFF8A6BE0, gemHi = 0xFFCBB8FF;
                int[][] g = {{6, 7}, {6, 8}, {6, 9}, {6, 10}, {6, 11}, {5, 9}, {7, 9}, {5, 12}, {7, 12},
                        {25, 7}, {25, 8}, {25, 9}, {25, 10}, {25, 11}, {24, 8}, {26, 8}, {24, 11}, {26, 11},
                        {6, 20}, {6, 21}, {6, 22}, {5, 21}, {7, 21},
                        {25, 20}, {25, 21}, {25, 22}, {24, 21}, {26, 21}};
                for (int[] p : g) blockSet(img, free, p[0], p[1], glow);
                blockSet(img, free, 6, 9, glowHi); blockSet(img, free, 25, 9, glowHi);
                blockHalo(img, free, glow);
                // a gem set into the top band, above the buckle
                for (int[] p : new int[][]{{15, 6}, {16, 6}, {15, 7}, {16, 7}}) blockSet(img, free, p[0], p[1], gem);
                blockSet(img, free, 15, 6, gemHi); blockSet(img, free, 16, 7, gemHi);
            }
            default -> {}
        }
        ImageIO.write(img, "PNG", new File(BASE + "/block/pack_" + TIER_ID[tier] + "_front.png"));
    }

    static void blockSet(BufferedImage img, boolean[][] free, int x, int y, int c) {
        if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight() || !free[x][y]) return;
        img.setRGB(x, y, c);
    }

    /** A 2x2 brass stud with a contact shadow - the same shape the hero item wears. */
    static void blockStud(BufferedImage img, boolean[][] free, int x, int y) {
        blockSet(img, free, x, y, ramp(BRASSR, 0.93));
        blockSet(img, free, x + 1, y, ramp(BRASSR, 0.72));
        blockSet(img, free, x, y + 1, ramp(BRASSR, 0.64));
        blockSet(img, free, x + 1, y + 1, ramp(BRASSR, 0.40));
        blockSet(img, free, x + 2, y + 2, ramp(BRASSR, 0.12));
    }

    /** A bevelled 5x5 steel plate with a brass rivet - lit top-left, shaded bottom-right. */
    static void blockPlate(BufferedImage img, boolean[][] free, int x, int y) {
        for (int j = 0; j < 5; j++)
            for (int i = 0; i < 5; i++) {
                double v = 0.58;
                if (i == 0 || j == 0) v = 0.84;
                if (i == 4 || j == 4) v = 0.22;
                if ((i == 4 && j == 0) || (i == 0 && j == 4)) v = 0.52;
                blockSet(img, free, x + i, y + j, ramp(STEELR, v));
            }
        blockSet(img, free, x + 2, y + 2, ramp(BRASSR, 0.88));  // brass rivet head
        blockSet(img, free, x + 3, y + 3, ramp(BRASSR, 0.46));  // its shaded side
    }

    /** One-pixel bloom around the block-face rune strokes, matching the hero item's glow. */
    static void blockHalo(BufferedImage img, boolean[][] free, int glow) {
        int N = img.getWidth();
        BufferedImage src = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) src.setRGB(x, y, img.getRGB(x, y));
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                if (src.getRGB(x, y) != glow) continue;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx < 0 || ny < 0 || nx >= N || ny >= N || !free[nx][ny]) continue;
                    int c = img.getRGB(nx, ny);
                    if (c == glow) continue;
                    img.setRGB(nx, ny, lerp(c, glow, 0.30f));
                }
            }
    }

    /** 32x32 brushed-brass face for the placed block's buckle + straps: vertical sheen + rivets. */
    static void genBlockBrass(String path) throws Exception {
        int N = 32;
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(37);
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                // vertical brushed sheen: a bright column toward the left third
                double sheen = 1 - Math.min(1, Math.abs(x - N * 0.38) / (N * 0.5));
                int c = lerp(BRASS_LO, BRASS_HI, (float) (0.35 + 0.5 * sheen));
                c = shade(c, (int) Math.round(5.0 * (smoothNoise(x, y, 6, 15) - 0.5))); // soft, not speckled
                img.setRGB(x, y, c);
            }
        for (int i = 0; i < N; i++) { img.setRGB(0, i, BRASS_LO); img.setRGB(N - 1, i, shade(BRASS_LO, -18)); }
        blockRivet(img, 7, 7); blockRivet(img, 24, 24); blockRivet(img, 24, 7); blockRivet(img, 7, 24);
        ImageIO.write(img, "PNG", new File(path));
    }

    /** 32x32 twine-wrap face for the CANVAS placed block's buckle: woven pale-tan cord, no metal.
     *  Colour is baked from the same twine ramp as the hero item's twine buckle, so a set-down
     *  canvas pack's closure matches the one in hand. Only the canvas model repoints its #buckle
     *  here; every other tier keeps the shared brass, so their buckles are untouched. */
    static void genBlockTwine(String path) throws Exception {
        int N = 32;
        int[] tw = TWINER;
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(61);
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                // gentle top-left sheen so the raised buckle box still catches light - in pale tan,
                // not gold. The value floor is kept high so the buckle face lands on the pale twine
                // stops (C0B084/D8C99C), reading clearly lighter and cooler than the brass buckle.
                double sheen = 1 - Math.min(1, Math.abs(x - N * 0.40) / (N * 0.55));
                int c = ramp(tw, 0.62 + 0.30 * sheen);
                c = shade(c, (int) Math.round(4.0 * (smoothNoise(x, y, 6, 21) - 0.5)));
                // woven twine: a 2-on/2-off rib in both directions - reads as cord, never metal,
                // and coarse enough not to shimmer when the block is seen from across the room
                c = shade(c, ((x % 4) < 2 ? 4 : -4) + ((y % 4) < 2 ? 3 : -3));
                img.setRGB(x, y, c);
            }
        for (int i = 0; i < N; i++) { img.setRGB(0, i, shade(tw[2], -4)); img.setRGB(N - 1, i, shade(tw[1], -2)); }
        ImageIO.write(img, "PNG", new File(path));
    }

    // ---------- helpers ----------

    static void stitchH(BufferedImage img, int x0, int y, int x1) {
        for (int x = x0; x < x1; x += 3) img.setRGB(x, y, STITCH);
    }

    static void stitchRect(BufferedImage img, int x, int y, int w, int h) {
        for (int i = x + 1; i < x + w - 1; i += 3) { img.setRGB(i, y + 1, STITCH); img.setRGB(i, y + h - 2, STITCH); }
        for (int j = y + 1; j < y + h - 1; j += 3) { img.setRGB(x + 1, j, STITCH); img.setRGB(x + w - 2, j, STITCH); }
    }

    /**
     * Value noise on a COARSE lattice, smoothstep-interpolated: a large soft mottle rather than
     * per-pixel static. This is the de-noising workhorse - every surface that used to sprinkle
     * {@code valueNoise} at 1px pitch now samples this at a 5-7px cell, so the variation reads as
     * material at full size and simply vanishes when the sprite is shrunk into a slot.
     */
    static double smoothNoise(double x, double y, int cell, int seed) {
        double fx = x / cell, fy = y / cell;
        int x0 = (int) Math.floor(fx), y0 = (int) Math.floor(fy);
        double tx = fx - x0, ty = fy - y0;
        tx = tx * tx * (3 - 2 * tx);
        ty = ty * ty * (3 - 2 * ty);
        double a = lattice(x0, y0, seed), b = lattice(x0 + 1, y0, seed);
        double c = lattice(x0, y0 + 1, seed), d = lattice(x0 + 1, y0 + 1, seed);
        double top = a + (b - a) * tx, bot = c + (d - c) * tx;
        return top + (bot - top) * ty;
    }

    static double lattice(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 144269504;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        return (h & 0xFF) / 255.0;
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
