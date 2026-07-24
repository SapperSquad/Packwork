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
        genBlockLeather(BASE + "/block/pack_block.png");
        genBlockBrass(BASE + "/block/pack_block_brass.png");

        // ---- hand-authored 16x16 item sprites ----
        genPacks();
        genTrinkets();
        drawItem("outfitters_handbook", HANDBOOK, base());

        System.out.println("Packwork textures generated.");
        writeMontage("tools/sprite_montage.png");
        System.out.println("Montage written to tools/sprite_montage.png");
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

    static void genPacks() throws Exception {
        // 7-stop leather ramp per tier (dark -> light) + a flag for the material story
        heroPack("canvas_pack", 0, new int[]{
            0xFF544527, 0xFF6E5A34, 0xFF897249, 0xFFA68E5F, 0xFFC2AB79, 0xFFD8C494, 0xFFEEDDB2});
        heroPack("leather_pack", 1, new int[]{
            0xFF1C1108, 0xFF321F10, 0xFF4C3319, 0xFF684627, 0xFF875E38, 0xFFA6784B, 0xFFC49468});
        heroPack("studded_pack", 2, new int[]{
            0xFF150D06, 0xFF261809, 0xFF3A2811, 0xFF513920, 0xFF6B4E2E, 0xFF87663F, 0xFFA07C50});
        heroPack("reinforced_pack", 3, new int[]{
            0xFF16130E, 0xFF272319, 0xFF3B3527, 0xFF524A38, 0xFF6C6249, 0xFF897C5D, 0xFFA89A75});
        heroPack("runed_pack", 4, new int[]{
            0xFF130C22, 0xFF201430, 0xFF302144, 0xFF43305C, 0xFF574178, 0xFF6F5695, 0xFF8A70B4});
    }

    // brass and steel ramps (7-stop) shared by fittings/trim
    static final int[] BRASSR = {0xFF3E2C0E, 0xFF5E4514, 0xFF80611C, 0xFFA6842C, 0xFFC9A542, 0xFFE6C56E, 0xFFF8E6A0};
    static final int[] STEELR = {0xFF26272C, 0xFF3E4046, 0xFF585B62, 0xFF787C84, 0xFF9CA0A8, 0xFFC2C6CD, 0xFFE8EAEE};

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
        int[] strap = {shade(R[0], -4), shade(R[1], -10), shade(R[2], -14), shade(R[3], -14)};

        // geometry (in 32px space)
        double bcx = 16, bcy = 20.2, bhw = 12.7, bhh = 10.4;  // body (one rounded form, flatter base)
        double fcx = 16, fcy = 13.2, fhw = 12.6, fhh = 8.0;   // flap (draped over top+front)
        double lpx = 6.0, rpx = 26.0, pcy = 18.5, phw = 3.1, phh = 5.0; // side pockets (mid-body bulges)

        for (int y = 0; y < PS; y++) {
            for (int x = 0; x < PS; x++) {
                double px = x + 0.5, py = y + 0.5;
                int col = 0; double best = -2; int mat = -1; // mat: 0 leather,1 pocket,2 flap
                // body
                double vb = dome(px, py, bcx, bcy, bhw, bhh);
                if (vb > -1 && py > 12) { best = vb; mat = 0; }
                // pockets (slightly recessed/darker, drawn where they extend past the body sides)
                double vpl = dome(px, py, lpx, pcy, phw, phh);
                double vpr = dome(px, py, rpx, pcy, phw, phh);
                double vp = Math.max(vpl, vpr);
                if (vp > -1 && vp - 0.08 > best) { best = vp - 0.08; mat = 1; }
                // flap on top (front upper), wins over body/pockets
                double vf = dome(px, py, fcx, fcy, fhw, fhh);
                if (vf > -1 && py < 21.5) { best = vf + 0.06; mat = 2; }
                if (mat < 0) continue;

                double v = best;
                // ambient occlusion: darken the body just under the flap's hanging edge
                double flapEdge = ell(px, py, fcx, fcy, fhw, fhh); // ~0 at flap rim
                if (mat != 2 && py > fcy && flapEdge > -0.18 && flapEdge < 0.14) v -= 0.22 * (0.14 - Math.abs(flapEdge + 0.02)) / 0.16;
                col = ramp(R, v);
                img.setRGB(x, y, col);
            }
        }

        // top handle: a leather loop (drawn as a ring behind the flap top)
        for (int y = 2; y <= 9; y++)
            for (int x = 11; x <= 20; x++) {
                double o = ell(x + 0.5, y + 0.5, 15.8, 5.6, 4.6, 3.9);
                double in = ell(x + 0.5, y + 0.5, 15.8, 6.2, 2.5, 2.6);
                if (o > 0 && in < 0 && (img.getRGB(x, y) >>> 24) == 0) {
                    double v = 0.5 + 0.5 * (-((x - 15.8) / 4.6) * LX - ((y - 5.6) / 3.9) * LY);
                    img.setRGB(x, y, ramp(R, v - 0.12));
                }
            }

        // central strap running under the flap down to the base, with thickness
        for (int y = 6; y < 30; y++)
            for (int x = 14; x <= 17; x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) continue;
                double nx = (x - 15.5) / 2.0;
                double v = 0.5 - 0.28 * nx - 0.10;              // cylinder shade
                if (x == 14) v -= 0.28; if (x == 17) v -= 0.18; // edges = thickness
                if (x == 15) v += 0.10;
                img.setRGB(x, y, ramp(strap, v));
            }

        heroBuckle(img, tier);
        heroStitchAndTrim(img, tier, R);
        outline(img, shade(R[0], -14));
        centerCheck(name, img);
        ImageIO.write(img, "PNG", new File(BASE + "/item/" + name + ".png"));
        RENDERED.put(name, img);
    }

    /** A brass (or twine, for canvas) buckle on the flap front, with a bright specular glint. */
    static void heroBuckle(BufferedImage img, int tier) {
        boolean twine = tier == 0;
        int bx = 12, by = 15, bw = 8, bh = 7;
        for (int y = by; y < by + bh; y++)
            for (int x = bx; x < bx + bw; x++) {
                boolean frame = x <= bx + 1 || x >= bx + bw - 2 || y <= by + 1 || y >= by + bh - 2;
                if (!frame) continue; // hollow centre shows the strap through the buckle
                double nx = (x - (bx + bw / 2.0)) / (bw / 2.0), ny = (y - (by + bh / 2.0)) / (bh / 2.0);
                double v = 0.5 - 0.34 * nx - 0.30 * ny;
                if (twine) img.setRGB(x, y, ramp(new int[]{0xFF6A5836, 0xFF9C8B5F, 0xFFC0B084, 0xFFD8C99C, 0xFFEADCB4}, v - 0.05));
                else img.setRGB(x, y, ramp(BRASSR, v));
            }
        if (!twine) { // specular glint on the top-left of the frame
            img.setRGB(bx + 1, by, 0xFFF8E6A0); img.setRGB(bx + 2, by, 0xFFFDF0C0);
            img.setRGB(bx, by + 1, 0xFFF8E6A0);
        }
        // prong across the hollow
        for (int y = by + 2; y <= by + bh - 3; y++) img.setRGB(bx + bw / 2 - 1, y, twine ? 0xFF7A6540 : ramp(BRASSR, 0.5));
    }

    /** Stitching along the flap hem + the per-tier material story on top of the base render. */
    static void heroStitchAndTrim(BufferedImage img, int tier, int[] R) {
        int stitch = ramp(R, 0.92);
        // dashed stitch line just inside the flap's hanging edge (an arc)
        for (int x = 5; x <= 27; x++) {
            int y = (int) Math.round(13.5 + Math.sqrt(Math.max(0, 1 - Math.pow((x + 0.5 - 16) / 12.0, 2))) * 6.6);
            if (((x) & 1) == 0 && (img.getRGB(x, y) >>> 24) != 0) img.setRGB(x, y, stitch);
        }
        switch (tier) {
            case 0 -> { // canvas: woven crosshatch + a twine cross-lashing on the flap
                for (int y = 3; y < 30; y++)
                    for (int x = 2; x < 30; x++) {
                        if ((img.getRGB(x, y) >>> 24) == 0) continue;
                        if (((x + y) % 4 == 0)) img.setRGB(x, y, shade(img.getRGB(x, y), 8));
                        else if (((x - y + 40) % 4 == 0)) img.setRGB(x, y, shade(img.getRGB(x, y), -8));
                    }
                int tw = shade(R[1], -14);
                for (int i = 0; i < 6; i++) { setIf(img, 9 + i, 9 + i, tw); setIf(img, 22 - i, 9 + i, tw); }
            }
            case 1 -> leatherGrain(img, R);       // supple leather grain
            case 2 -> { // studded: leather grain + a ring of brass studs round the flap
                leatherGrain(img, R);
                int[][] studs = {{7, 9}, {11, 6}, {16, 5}, {21, 6}, {25, 9}, {6, 14}, {26, 14}, {9, 18}, {23, 18}};
                for (int[] s : studs) heroStud(img, s[0], s[1]);
            }
            case 3 -> { // reinforced: riveted steel corner plates + a steel band across the flap
                leatherGrain(img, R);
                heroPlate(img, 5, 6); heroPlate(img, 22, 6); heroPlate(img, 5, 22); heroPlate(img, 22, 22);
                for (int x = 10; x <= 21; x++) { // steel band
                    if ((img.getRGB(x, 11) >>> 24) != 0) img.setRGB(x, 11, ramp(STEELR, 0.5 - (x - 15.5) * 0.02));
                    if ((img.getRGB(x, 12) >>> 24) != 0) img.setRGB(x, 12, ramp(STEELR, 0.34));
                }
            }
            case 4 -> { // runed: deep-dyed leather grain + glowing glyphs + a gem in the buckle
                leatherGrain(img, R);
                int glow = 0xFFB9A0FF, glowHi = 0xFFEADCFF;
                int[][] g1 = {{9, 8}, {9, 9}, {9, 10}, {10, 9}, {8, 11}, {10, 11}}; // left glyph
                int[][] g2 = {{22, 8}, {22, 9}, {22, 10}, {21, 9}, {23, 9}, {22, 11}}; // right glyph
                for (int[] p : g1) setIf(img, p[0], p[1], glow);
                for (int[] p : g2) setIf(img, p[0], p[1], glow);
                setIf(img, 9, 9, glowHi); setIf(img, 22, 9, glowHi);
                for (int[] p : new int[][]{{15, 25}, {16, 25}, {15, 26}, {16, 26}}) setIf(img, p[0], p[1], glow);
                // gem in the buckle centre
                img.setRGB(15, 18, 0xFF8A6BE0); img.setRGB(16, 18, 0xFFB9A0FF);
                img.setRGB(15, 19, 0xFF6E4FC0); img.setRGB(16, 19, 0xFF9C82E8);
            }
            default -> {}
        }
    }

    static void leatherGrain(BufferedImage img, int[] R) {
        Random rnd = new Random(19);
        for (int y = 0; y < PS; y++)
            for (int x = 0; x < PS; x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) continue;
                // fine horizontal grain: subtle darker creases every few rows + a touch of variation
                int d = 0;
                if ((y % 5 == 0) && ((x + y) % 3 != 0)) d -= 7;
                d += (int) ((valueNoise(x, y, rnd, 6) - 0.5f) * 6);
                img.setRGB(x, y, shade(img.getRGB(x, y), d));
            }
    }

    static void heroStud(BufferedImage img, int x, int y) {
        if ((img.getRGB(x, y) >>> 24) == 0) return;
        img.setRGB(x, y, ramp(BRASSR, 0.7));
        setIf(img, x, y - 1, ramp(BRASSR, 0.95)); // top glint
        setIf(img, x + 1, y, ramp(BRASSR, 0.5));
        setIf(img, x, y + 1, ramp(BRASSR, 0.28)); // contact shadow
    }

    static void heroPlate(BufferedImage img, int x, int y) {
        for (int j = 0; j < 4; j++)
            for (int i = 0; i < 4; i++) {
                if ((img.getRGB(x + i, y + j) >>> 24) == 0) continue;
                double v = 0.55 - i * 0.05 - j * 0.06;
                img.setRGB(x + i, y + j, ramp(STEELR, v));
            }
        setIf(img, x, y, ramp(STEELR, 0.9));
        setIf(img, x + 3, y, ramp(BRASSR, 0.85)); // a brass rivet corner
        setIf(img, x + 3, y + 3, ramp(BRASSR, 0.7));
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
    }

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

    /** 32x32 light near-neutral leather face for the placed block: fine grain, worn mottling,
     *  a stitched seam and a quilted bevel. Kept light/neutral so the per-tier tint (multiply)
     *  controls the hue. Higher-res than before so a set-down pack matches the hero item. */
    static void genBlockLeather(String path) throws Exception {
        int N = 32;
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(31);
        int hi = 0xFFD2CBBE, lo = 0xFFB4AEA2;
        for (int y = 0; y < N; y++)
            for (int x = 0; x < N; x++) {
                int c = lerp(hi, lo, y / (float) N);
                // fine horizontal grain creases + low-amplitude worn mottling (structured, not noise-fill)
                int d = (int) ((valueNoise(x / 2, y, rnd, 12) - 0.5f) * 10);
                if (y % 6 == 0 && (x + y) % 3 != 0) d -= 8;
                if (y % 6 == 3) d += 5;
                c = shade(c, d);
                img.setRGB(x, y, c);
            }
        // a stitched seam a third of the way down (groove + light stitches)
        for (int x = 0; x < N; x++) img.setRGB(x, 10, shade(lo, -22));
        for (int x = 1; x < N; x += 4) { img.setRGB(x, 10, STITCH); img.setRGB(x + 1, 9, shade(STITCH, -30)); }
        // quilted bevel: light top/left, shadow bottom/right
        for (int i = 0; i < N; i++) {
            img.setRGB(i, 0, shade(hi, 14)); img.setRGB(0, i, shade(hi, 10));
            img.setRGB(i, N - 1, shade(lo, -28)); img.setRGB(N - 1, i, shade(lo, -24));
        }
        ImageIO.write(img, "PNG", new File(path));
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
                c = shade(c, (int) ((valueNoise(x, y / 2, rnd, 15) - 0.5f) * 8));
                img.setRGB(x, y, c);
            }
        for (int i = 0; i < N; i++) { img.setRGB(0, i, BRASS_LO); img.setRGB(N - 1, i, shade(BRASS_LO, -18)); }
        blockRivet(img, 7, 7); blockRivet(img, 24, 24); blockRivet(img, 24, 7); blockRivet(img, 7, 24);
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
