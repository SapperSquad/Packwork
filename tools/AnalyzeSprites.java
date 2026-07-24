import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

/**
 * Sprite audit: for every Packwork 16x16 item/block sprite, report the opaque
 * bounding box, per-edge margins, whether it touches or exceeds the sheet edge,
 * and the geometric centre offset from (8,8). Also dumps a per-row occupancy map.
 * Run: java tools/AnalyzeSprites.java
 */
public class AnalyzeSprites {
    public static void main(String[] args) throws Exception {
        String item = "src/main/resources/assets/packwork/textures/item";
        String block = "src/main/resources/assets/packwork/textures/block";
        String[] names = new File(item).list((d, n) -> n.endsWith(".png"));
        Arrays.sort(names);
        System.out.printf("%-22s %-7s  L R T B   ctrOff  edge%n", "sprite", "box");
        System.out.println("---------------------------------------------------------------");
        for (String n : names) report(item + "/" + n, n);
        System.out.println("--- block faces (full-tile, edge-touch is expected) ---");
        for (String n : new File(block).list((d, x) -> x.endsWith(".png"))) report(block + "/" + n, n);
    }

    static void report(String path, String name) throws Exception {
        BufferedImage img = ImageIO.read(new File(path));
        int w = img.getWidth(), h = img.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1, count = 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((img.getRGB(x, y) >>> 24) > 8) {
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    count++;
                }
        if (maxX < 0) { System.out.printf("%-22s EMPTY%n", name); return; }
        int bw = maxX - minX + 1, bh = maxY - minY + 1;
        int mL = minX, mR = w - 1 - maxX, mT = minY, mB = h - 1 - maxY;
        // centre of bounding box vs sprite centre (7.5,7.5 for 16px)
        double cx = (minX + maxX) / 2.0, cy = (minY + maxY) / 2.0;
        double offX = cx - (w - 1) / 2.0, offY = cy - (h - 1) / 2.0;
        boolean touch = mL == 0 || mR == 0 || mT == 0 || mB == 0;
        System.out.printf("%-22s %2dx%-4d  %d %d %d %d   %+.1f,%+.1f  %s%n",
                name, bw, bh, mL, mR, mT, mB, offX, offY, touch ? "TOUCHES EDGE" : "ok");
    }
}
