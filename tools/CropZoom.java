import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Crop a region of a screenshot and upscale it (nearest) for close inspection.
 *  Run: java tools/CropZoom.java <in.png> <x> <y> <w> <h> <scale> <out.png> */
public class CropZoom {
    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File(a[0]));
        int x = Integer.parseInt(a[1]), y = Integer.parseInt(a[2]);
        int w = Integer.parseInt(a[3]), h = Integer.parseInt(a[4]), s = Integer.parseInt(a[5]);
        BufferedImage out = new BufferedImage(w * s, h * s, BufferedImage.TYPE_INT_ARGB);
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++) {
                int px = Math.min(img.getWidth() - 1, Math.max(0, x + i));
                int py = Math.min(img.getHeight() - 1, Math.max(0, y + j));
                int c = img.getRGB(px, py);
                for (int dy = 0; dy < s; dy++)
                    for (int dx = 0; dx < s; dx++)
                        out.setRGB(i * s + dx, j * s + dy, c);
            }
        ImageIO.write(out, "PNG", new File(a[6]));
        System.out.println("wrote " + a[6]);
    }
}
