import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Packwork promo art: the Modrinth/CurseForge icon and the wide banner, composed from the
 * REAL in-game pack sprites so the store page can never drift from the shipped art.
 * Java-only tooling (no Python/Node on this machine), ASCII-only file, same conventions as
 * Reel Rivals' GenCards. Run: java tools/GenPromo.java  (from the project root).
 *
 * Writes promo/icon-512.png and promo/banner-1920x640.png.
 */
public final class GenPromo {

    static final String ITEMS = "src/main/resources/assets/packwork/textures/item/";
    static final String OUT = "promo/";
    static final String[] TIERS = {"canvas", "leather", "studded", "reinforced", "runed", "sculkhide"};

    // the shared leather-and-brass palette (from tools/GenTextures.java's ramps)
    static final Color LEATHER_DARKEST = new Color(0x1C, 0x11, 0x08);
    static final Color LEATHER_DARK = new Color(0x32, 0x1F, 0x10);
    static final Color LEATHER_MID = new Color(0x4C, 0x33, 0x19);
    static final Color BRASS = new Color(0xC9, 0xA5, 0x42);
    static final Color BRASS_LIGHT = new Color(0xE6, 0xC5, 0x6E);
    static final Color BRASS_PALE = new Color(0xF8, 0xE6, 0xA0);
    static final Color CREAM = new Color(0xEA, 0xD9, 0xA6);
    static final Color STITCH = new Color(0x8A, 0x65, 0x40);

    public static void main(String[] args) throws Exception {
        new File(OUT).mkdirs();
        icon();
        banner();
        System.out.println("Promo art written to " + OUT);
    }

    // ---------------------------------------------------------------- icon 512x512

    static void icon() throws Exception {
        int N = 512;
        BufferedImage img = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // a stitched leather patch with softly rounded corners
        int r = 64;
        g.setColor(LEATHER_DARK);
        g.fillRoundRect(8, 8, N - 16, N - 16, r, r);
        grain(img, 12, 12, N - 24, N - 24, 6);
        // brass border, double-ruled
        g.setColor(BRASS);
        g.setStroke(new BasicStroke(10));
        g.drawRoundRect(14, 14, N - 28, N - 28, r, r);
        g.setColor(BRASS_LIGHT);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(28, 28, N - 56, N - 56, r - 16, r - 16);
        // stitched inner seam
        g.setColor(STITCH);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                1, new float[]{14, 10}, 0));
        g.drawRoundRect(44, 44, N - 88, N - 88, r - 28, r - 28);
        // corner tacks
        for (int[] t : new int[][]{{34, 34}, {N - 46, 34}, {34, N - 46}, {N - 46, N - 46}}) {
            g.setColor(new Color(0x2A, 0x1C, 0x10));
            g.fillOval(t[0], t[1], 14, 14);
            g.setColor(BRASS_PALE);
            g.fillOval(t[0] + 1, t[1] + 1, 9, 9);
        }

        // THE pack: the leather-tier hero sprite (the everyman backpack), nearest x12 = 384
        BufferedImage pack = ImageIO.read(new File(ITEMS + "leather_pack.png"));
        int s = 12, pw = pack.getWidth() * s;
        drawSpriteShadow(img, pack, (N - pw) / 2 + 10, (N - pw) / 2 + 26, s);
        drawSprite(img, pack, (N - pw) / 2, (N - pw) / 2 + 12, s);

        g.dispose();
        ImageIO.write(img, "PNG", new File(OUT + "icon-512.png"));
    }

    // ---------------------------------------------------------------- banner 1920x640

    static void banner() throws Exception {
        int W = 1920, H = 640;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // the leather field, darker toward the edges
        g.setColor(LEATHER_DARK);
        g.fillRect(0, 0, W, H);
        grain(img, 0, 0, W, H, 8);
        vignette(img);

        // brass frame with corner plates
        g.setColor(BRASS);
        g.setStroke(new BasicStroke(8));
        g.drawRect(14, 14, W - 28, H - 28);
        g.setColor(BRASS_LIGHT);
        g.setStroke(new BasicStroke(2));
        g.drawRect(26, 26, W - 52, H - 52);
        for (int[] c : new int[][]{{14, 14}, {W - 62, 14}, {14, H - 62}, {W - 62, H - 62}}) {
            g.setColor(BRASS);
            g.fillRect(c[0], c[1], 48, 48);
            g.setColor(BRASS_PALE);
            g.fillRect(c[0] + 6, c[1] + 6, 12, 12);
        }

        // the six-tier ladder, ascending, lower right, standing on a stitched ground line
        int base = H - 110;
        int x = 1010;
        for (int t = 0; t < TIERS.length; t++) {
            BufferedImage pack = ImageIO.read(new File(ITEMS + TIERS[t] + "_pack.png"));
            int s = 4 + t / 2;                       // the ladder literally grows: x4 -> x6
            int pw = pack.getWidth() * s;
            int y = base - pack.getHeight() * s;
            drawSpriteShadow(img, pack, x + 6, y + 10, s);
            drawSprite(img, pack, x, y, s);
            x += pw - 20;                             // slight overlap, like packs in a row
        }
        g.setColor(STITCH);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                1, new float[]{18, 12}, 0));
        g.drawLine(990, base + 14, W - 70, base + 14);

        // the name, stamped in brass with a deep shadow
        Font name = pickFont(Font.BOLD, 170);
        g.setFont(name);
        drawStamped(g, "PACKWORK", 84, 260);

        // the tagline, cream, plain words
        g.setFont(pickFont(Font.PLAIN, 56));
        g.setColor(new Color(0, 0, 0, 140));
        g.drawString("The pack that packs itself.", 92, 356);
        g.setColor(CREAM);
        g.drawString("The pack that packs itself.", 88, 352);

        // the sub-line: what it actually is, no adjectives
        g.setFont(pickFont(Font.PLAIN, 34));
        g.setColor(new Color(0xC8, 0xB8, 0x92));
        g.drawString("Tabbed, self-sorting adventurer's pack - NeoForge 1.21.1", 90, 420);

        // the maker's stamp, small, bottom-left
        g.setFont(pickFont(Font.BOLD, 30));
        g.setColor(STITCH);
        g.drawString("SapperSquad", 90, H - 52);

        g.dispose();
        ImageIO.write(img, "PNG", new File(OUT + "banner-1920x640.png"));
    }

    // ---------------------------------------------------------------- helpers

    /** Georgia reads forge-y and ships with Windows; fall back to logical Serif. */
    static Font pickFont(int style, int size) {
        Font f = new Font("Georgia", style, size);
        if (!f.getFamily().equalsIgnoreCase("Georgia")) f = new Font(Font.SERIF, style, size);
        return f;
    }

    /** Brass lettering with a hard drop shadow and a pale top-light, like a struck stamp. */
    static void drawStamped(Graphics2D g, String text, int x, int y) {
        g.setColor(new Color(0, 0, 0, 170));
        g.drawString(text, x + 8, y + 8);
        g.setColor(new Color(0x5E, 0x45, 0x14));
        for (int dx = -3; dx <= 3; dx += 2)
            for (int dy = -3; dy <= 3; dy += 2)
                g.drawString(text, x + dx, y + dy);
        g.setColor(BRASS);
        g.drawString(text, x, y);
        g.setColor(BRASS_LIGHT);
        g.drawString(text, x - 1, y - 2);
    }

    /** Nearest-neighbour sprite scale - the pixels stay pixels. */
    static void drawSprite(BufferedImage dst, BufferedImage sprite, int x, int y, int s) {
        for (int sy = 0; sy < sprite.getHeight(); sy++)
            for (int sx = 0; sx < sprite.getWidth(); sx++) {
                int c = sprite.getRGB(sx, sy);
                if ((c >>> 24) == 0) continue;
                for (int j = 0; j < s; j++)
                    for (int i = 0; i < s; i++) {
                        int px = x + sx * s + i, py = y + sy * s + j;
                        if (px >= 0 && py >= 0 && px < dst.getWidth() && py < dst.getHeight())
                            dst.setRGB(px, py, c);
                    }
            }
    }

    /** The sprite's silhouette as a soft dark drop, drawn before the sprite itself. */
    static void drawSpriteShadow(BufferedImage dst, BufferedImage sprite, int x, int y, int s) {
        int shadow = 0x66000000;
        for (int sy = 0; sy < sprite.getHeight(); sy++)
            for (int sx = 0; sx < sprite.getWidth(); sx++) {
                if ((sprite.getRGB(sx, sy) >>> 24) == 0) continue;
                for (int j = 0; j < s; j++)
                    for (int i = 0; i < s; i++) {
                        int px = x + sx * s + i, py = y + sy * s + j;
                        if (px < 0 || py < 0 || px >= dst.getWidth() || py >= dst.getHeight()) continue;
                        int under = dst.getRGB(px, py);
                        dst.setRGB(px, py, blend(under, shadow));
                    }
            }
    }

    /** Coarse leather grain: a smooth low-frequency mottle + a soft crease every few rows. */
    static void grain(BufferedImage img, int x0, int y0, int w, int h, int strength) {
        for (int y = y0; y < y0 + h; y++)
            for (int x = x0; x < x0 + w; x++) {
                if (x < 0 || y < 0 || x >= img.getWidth() || y >= img.getHeight()) continue;
                int c = img.getRGB(x, y);
                if ((c >>> 24) == 0) continue;
                double n = smooth(x, y, 40) - 0.5;
                double crease = -0.35 * Math.max(0, Math.cos(y * Math.PI * 2 / 90.0));
                int d = (int) Math.round(strength * (n + crease));
                img.setRGB(x, y, shade(c, d));
            }
    }

    /** Darken toward the edges so the middle carries the light. */
    static void vignette(BufferedImage img) {
        int W = img.getWidth(), H = img.getHeight();
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                double dx = (x - W / 2.0) / (W / 2.0), dy = (y - H / 2.0) / (H / 2.0);
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > 0.6) img.setRGB(x, y, shade(img.getRGB(x, y), (int) (-22 * (d - 0.6) / 0.4)));
            }
    }

    // value-noise on a coarse lattice with smoothstep, same idea as GenTextures.smoothNoise
    static double smooth(int x, int y, int cell) {
        double gx = (double) x / cell, gy = (double) y / cell;
        int x0 = (int) Math.floor(gx), y0 = (int) Math.floor(gy);
        double fx = gx - x0, fy = gy - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash(x0, y0), b = hash(x0 + 1, y0), c = hash(x0, y0 + 1), d = hash(x0 + 1, y0 + 1);
        return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }

    static double hash(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return ((h ^ (h >> 16)) & 0x7fffffff) / (double) 0x7fffffff;
    }

    static int shade(int argb, int d) {
        int a = argb & 0xFF000000;
        int r = clamp(((argb >> 16) & 0xFF) + d), gg = clamp(((argb >> 8) & 0xFF) + d), b = clamp((argb & 0xFF) + d);
        return a | (r << 16) | (gg << 8) | b;
    }

    static int blend(int under, int over) {
        int oa = (over >>> 24), ua = (under >>> 24);
        if (oa == 0) return under;
        double f = oa / 255.0;
        int r = (int) (((under >> 16) & 0xFF) * (1 - f) + ((over >> 16) & 0xFF) * f);
        int gg = (int) (((under >> 8) & 0xFF) * (1 - f) + ((over >> 8) & 0xFF) * f);
        int b = (int) ((under & 0xFF) * (1 - f) + (over & 0xFF) * f);
        return (Math.max(ua, oa) << 24) | (r << 16) | (gg << 8) | b;
    }

    static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    private GenPromo() {}
}
