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
    //  packs - one silhouette, per-tier material ramp + escalating trim
    // =====================================================================

    // Body tones are digits 0(outline/darkest)..4(highlight); 'T' strap (drawn dark leather);
    // the buckle + tier trim are overlaid in code so canvas can wear twine and runed a gem.
    static final String[] PACK = {
    //   0123456789012345
        "................", // 0
        "......0000......", // 1  handle top
        "......0..0......", // 2  handle hole
        "....03443320....", // 3  flap top (rounded)
        "...0344332220...", // 4  flap
        "...0334TT3220...", // 5  flap, central strap begins
        "...0333TT3220...", // 6  flap
        "..02233TT32220..", // 7  flap widens (buckle sits here)
        "..01111TT11110..", // 8  hem shadow where flap meets body
        ".032222TT222210.", // 9  body + side pockets
        ".021222TT222120.", // 10 body, pocket seams
        ".021222TT222120.", // 11 body
        "..01222TT22210..", // 12 taper
        "...0122TT2210...", // 13 taper
        "....01111110....", // 14 base (dark contact)
        "................", // 15
    };

    static void genPacks() throws Exception {
        // ramp: {outline, shadow, mid, light, hi}
        genPack("canvas_pack",     new int[]{0xFF6A5836, 0xFF9C8B5F, 0xFFC0B084, 0xFFD8C99C, 0xFFEADCB4}, 0);
        genPack("leather_pack",    new int[]{0xFF241505, 0xFF4A3016, 0xFF6B4A26, 0xFF8A6234, 0xFFA87C46}, 1);
        genPack("studded_pack",    new int[]{0xFF1A1006, 0xFF35220F, 0xFF52371B, 0xFF6E4A26, 0xFF875E32}, 2);
        genPack("reinforced_pack", new int[]{0xFF1C1B18, 0xFF383129, 0xFF52493C, 0xFF6E6353, 0xFF89806C}, 3);
        genPack("runed_pack",      new int[]{0xFF160F26, 0xFF2A1E44, 0xFF3F2E60, 0xFF574080, 0xFF6E56A0}, 4);
    }

    static void genPack(String name, int[] ramp, int tier) throws Exception {
        Map<Character, Integer> p = new HashMap<>();
        p.put('0', ramp[0]); p.put('1', ramp[1]); p.put('2', ramp[2]);
        p.put('3', ramp[3]); p.put('4', ramp[4]);
        p.put('T', shade(ramp[1], -14)); // strap: darker than the body, same family
        drawItem(name, PACK, p);
        BufferedImage img = RENDERED.get(name);

        // brass (or twine) buckle centred on the flap hem
        boolean twine = tier == 0;
        int bLo = twine ? shade(ramp[1], -20) : BRASS_LO;
        int bMid = twine ? shade(ramp[2], -10) : BRASS;
        int bHi = twine ? ramp[3] : BRASS_HI;
        // buckle frame x6..9, y7..9
        fillRect(img, 6, 7, 4, 3, bLo);
        fillRect(img, 7, 7, 2, 1, bHi);
        img.setRGB(6, 8, bMid); img.setRGB(9, 8, bMid);
        img.setRGB(7, 8, shade(ramp[0], 10)); img.setRGB(8, 8, shade(ramp[0], 10)); // pin hole
        img.setRGB(7, 9, bMid); img.setRGB(8, 9, bLo);

        // per-tier trim climbing the ladder
        switch (tier) {
            case 0 -> { // canvas: twine cross-lashing on the flap
                int tw = shade(ramp[1], -22), th = ramp[4];
                img.setRGB(4, 4, tw); img.setRGB(5, 5, tw); img.setRGB(6, 6, th);
                img.setRGB(11, 4, tw); img.setRGB(10, 5, tw); img.setRGB(9, 6, th);
            }
            case 2 -> { // studded: brass studs around the flap edge
                int[][] studs = {{4, 4}, {6, 3}, {9, 3}, {11, 4}, {3, 6}, {12, 6}, {4, 9}, {11, 9}};
                for (int[] s : studs) { stud(img, s[0], s[1]); }
            }
            case 3 -> { // reinforced: riveted steel corner plates + edge band
                plate(img, 3, 3); plate(img, 10, 3); plate(img, 3, 10); plate(img, 10, 10);
                for (int x = 5; x <= 10; x += 2) img.setRGB(x, 4, 0xFFB8BAC2); // steel flap band
            }
            case 4 -> { // runed: glowing glyphs on the flap + a gem on the buckle
                int rune = 0xFFC2ABFF, glow = 0xFFEADCFF;
                img.setRGB(5, 4, rune); img.setRGB(5, 5, glow); img.setRGB(4, 5, rune);
                img.setRGB(10, 4, rune); img.setRGB(10, 5, glow); img.setRGB(11, 5, rune);
                img.setRGB(6, 12, rune); img.setRGB(9, 12, rune);
                img.setRGB(7, 8, 0xFF9C7BE8); img.setRGB(8, 8, 0xFFCBB4FF); // gem in the buckle
            }
            default -> {}
        }
        ImageIO.write(img, "PNG", new File(BASE + "/item/" + name + ".png"));
    }

    static void stud(BufferedImage img, int x, int y) {
        img.setRGB(x, y, BRASS_LO); img.setRGB(x, y - 1, BRASS); // a raised rivet with a top glint
        set(img, x, y - 2, BRASS_HI);
    }

    static void plate(BufferedImage img, int x, int y) {
        int lo = 0xFF5C5E67, mid = 0xFF9EA0A8, hi = 0xFFD0D2D8;
        fillRect(img, x, y, 3, 3, mid);
        img.setRGB(x, y, hi); img.setRGB(x + 1, y, hi);
        img.setRGB(x + 2, y + 2, lo); img.setRGB(x, y + 2, lo);
        img.setRGB(x + 2, y, 0xFFF6E29A); // a brass rivet in the corner
    }

    // =====================================================================
    //  trinkets - each its own readable silhouette
    // =====================================================================

    static void genTrinkets() throws Exception {
        // Lodestone Charm: a faceted slate-blue magnetite gem on a brass cap, iron bits clinging
        drawItem("lodestone_charm", LODESTONE,
                with(base(), '1', 0xFF2B3A55, '2', 0xFF44567A, '3', 0xFF6E82AB, '4', 0xFFA7BADF));
        // Compass Rose: round brass case, cream dial, red north needle
        drawItem("compass_rose", COMPASS, base());
        // Restock Strap: leather bandolier, brass buckle + two canvas pouches
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
        // Charge Crystal: faceted amber crystal wound in copper, on a brass base
        drawItem("charge_crystal", CRYSTAL,
                with(base(), '1', 0xFF8A4E12, '2', 0xFFC67A22, '3', 0xFFE8A23A, '4', 0xFFFBD06A));
        // Flask Harness: a wooden rack of two round vapour flasks
        drawItem("flask_harness", FLASKS,
                with(base(), '1', 0xFF5E3A8A, '2', 0xFF8A5CC0, '3', 0xFFB48CE0, '4', 0xFFD9C4F5));
    }

    //        0123456789012345
    static final String[] LODESTONE = {
        "................", // 0
        ".......nn.......", // 1  brass ring loop
        "......n..n......", // 2
        "......adda......", // 3  brass cap
        ".....143221.....", // 4  faceted magnetite gem
        "....14433221....", // 5
        "...144o322221...", // 6  specular glint
        "...1443322221...", // 7
        "...1433222221...", // 8
        "....14322221....", // 9
        "....13222221....", // 10
        ".....132221.....", // 11
        "......1221......", // 12
        "................", // 13
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
        "......nAan......", // 1  brass buckle
        "......rLLr......", // 2  strap
        "......rLLr......", // 3
        "......rLLr......", // 4
        ".wCccvrLLr......", // 5  left pouch
        ".wcacvrLLr......", // 6  brass stud
        ".wcccvrLLr......", // 7
        ".wvvvwrLLr......", // 8
        "......rLLrvccCw.", // 9  right pouch
        "......rLLrvcacw.", // 10 brass stud
        "......rLLrvcccw.", // 11
        "......rLLrwvvvw.", // 12
        "......rLLr......", // 13 strap tail
        ".......ll.......", // 14 pointed tip
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
        ".......oo.......", // 1  spark at the tip
        ".......11.......", // 2  crystal point
        "......1431......", // 3
        ".....144321.....", // 4
        "....14443221....", // 5
        "....1uUHuUu1....", // 6  copper coil band (dark wire)
        "....14o33221....", // 7  bright core
        "....13333221....", // 8
        ".....1uUHu1.....", // 9  lower copper band
        "......13221.....", // 10
        "......1221......", // 11 crystal base point
        "......adda......", // 12 brass mount
        "......nnnn......", // 13
        "................", // 14
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
        int scale = 14, gap = 10, cols = 6, bg = 0xFF3A3A40;
        int cell = 16 * scale;
        int rows = (RENDERED.size() + cols - 1) / cols;
        int mw = cols * cell + (cols + 1) * gap, mh = rows * cell + (rows + 1) * gap;
        BufferedImage m = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < mh; y++) for (int x = 0; x < mw; x++) m.setRGB(x, y, bg); // flat plain field
        int i = 0;
        for (Map.Entry<String, BufferedImage> e : RENDERED.entrySet()) {
            int cx = gap + (i % cols) * (cell + gap), cy = gap + (i / cols) * (cell + gap);
            BufferedImage s = e.getValue();
            for (int y = 0; y < 16; y++)
                for (int x = 0; x < 16; x++) {
                    int c = s.getRGB(x, y);
                    if ((c >>> 24) == 0) continue; // let the plain field show through
                    for (int dy = 0; dy < scale; dy++)
                        for (int dx = 0; dx < scale; dx++)
                            m.setRGB(cx + x * scale + dx, cy + y * scale + dy, c);
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

    static void panel(BufferedImage img, int x, int y, int w, int h, int hi, int lo) {
        for (int j = 0; j < h; j++) {
            int c = lerp(hi, lo, j / (float) h);
            hline(img, x, x + w - 1, y + j, c);
        }
        rect(img, x, y, w, h, shade(lo, -30));
    }

    static void genBlockLeather(String path) throws Exception {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(31);
        int hi = 0xFFCDC6BA, lo = 0xFFB6B0A4;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int c = lerp(hi, lo, y / 16f);
                c = shade(c, (int) ((valueNoise(x, y, rnd, 12) - 0.5f) * 18));
                img.setRGB(x, y, c);
            }
        }
        for (int x = 1; x < 15; x += 3) img.setRGB(x, 5, STITCH);
        for (int i = 0; i < 16; i++) {
            img.setRGB(0, i, shade(lo, -26)); img.setRGB(15, i, shade(lo, -26));
            img.setRGB(i, 0, shade(lo, -26)); img.setRGB(i, 15, shade(lo, -26));
        }
        ImageIO.write(img, "PNG", new File(path));
    }

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
        rivet(img, 4, 4); rivet(img, 11, 11);
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
