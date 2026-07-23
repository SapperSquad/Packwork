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

        genGui(base + "/gui/pack.png");
        genTab(base + "/gui/tab.png");

        // per-tier pack sprites, tinted brass fittings on leather
        genPack(base + "/item/canvas_pack.png", 0xFFCFC09A, 0xFFB4A47C, false);
        genPack(base + "/item/leather_pack.png", 0xFF7A5636, 0xFF5E4128, false);
        genPack(base + "/item/studded_pack.png", 0xFF6E4A2E, 0xFF523420, true);
        genPack(base + "/item/reinforced_pack.png", 0xFF5A4A3A, 0xFF3E3226, true);
        genPack(base + "/item/runed_pack.png", 0xFF4A3A52, 0xFF33253A, true);

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

    static void genPack(String path, int hi, int lo, boolean studs) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(11);
        // body 3..13 x, 4..15 y
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean body = x >= 3 && x <= 12 && y >= 4 && y <= 15;
                boolean flap = x >= 3 && x <= 12 && y >= 4 && y <= 8;
                if (!body) { img.setRGB(x, y, 0); continue; }
                float t = (y - 4) / 11f;
                int c = lerp(hi, lo, t);
                int n = (int) ((valueNoise(x, y, rnd, 3) - 0.5f) * 14);
                c = shade(c, n);
                if (flap) c = shade(c, 10); // flap catches light
                img.setRGB(x, y, c);
            }
        }
        // outline
        outline(img, LEATHER_EDGE);
        // flap seam (stitched)
        for (int x = 4; x <= 12; x += 2) img.setRGB(x, 9, STITCH);
        // brass buckle
        img.setRGB(7, 10, BRASS_HI); img.setRGB(8, 10, BRASS);
        img.setRGB(7, 11, BRASS); img.setRGB(8, 11, BRASS_LO);
        // straps
        vline(img, 5, 5, 15, shade(lo, -20));
        vline(img, 10, 5, 15, shade(lo, -20));
        if (studs) {
            img.setRGB(4, 12, BRASS_HI); img.setRGB(11, 12, BRASS_HI);
            img.setRGB(4, 14, BRASS); img.setRGB(11, 14, BRASS);
        }
        ImageIO.write(img, "PNG", new File(path));
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
