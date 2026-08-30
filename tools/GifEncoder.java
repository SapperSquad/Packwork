import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Packwork's promo GIF encoder. Java-only by house rule (this machine has no Python or
 * Node), and plain-javac-able like GenTextures / GenPromo:
 *
 * <pre>
 *   java tools/GifEncoder.java &lt;framesDir&gt; &lt;out.gif&gt; &lt;srcFps&gt; &lt;outFps&gt; &lt;downscale&gt;
 * </pre>
 *
 * <p>Reads {@code frame_*.png} from {@code framesDir} in name order, drops frames to land
 * on {@code outFps}, downscales by an integer factor, quantises every frame against ONE
 * global palette, and writes a looping GIF89a.
 *
 * <p><b>Two choices that matter for pixel art, both deliberate:</b>
 * <ul>
 *   <li><b>Nearest-neighbour downscale, never averaging.</b> Capture at GUI scale 2 and a
 *       1280x720 window and every GUI texel is exactly 2x2 device pixels, so a 2x nearest
 *       downscale is <em>lossless</em> for the GUI - the tab labels stay crisp. Box
 *       averaging would blur exactly the thing the GIF exists to show.</li>
 *   <li><b>One global palette, no dithering.</b> Per-frame palettes bloat the file and make
 *       flat leather shimmer between frames; dithering scatters noise across surfaces that
 *       are meant to be flat colour. The palette is median-cut over every Nth frame's
 *       colours, so it covers the whole sequence rather than the first shot.</li>
 * </ul>
 */
public class GifEncoder {

    /** Sample every Nth frame when building the palette - the whole sequence, cheaply. */
    private static final int PALETTE_STRIDE = 10;
    private static final int MAX_COLORS = 256;

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("usage: java tools/GifEncoder.java <framesDir> <out.gif> "
                    + "<srcFps> <outFps> <downscale>");
            System.exit(2);
        }
        File dir = new File(args[0]);
        File out = new File(args[1]);
        int srcFps = Integer.parseInt(args[2]);
        int outFps = Integer.parseInt(args[3]);
        int scale = Integer.parseInt(args[4]);

        File[] all = dir.listFiles((d, n) -> n.startsWith("frame_") && n.endsWith(".png"));
        if (all == null || all.length == 0) throw new IllegalStateException("no frame_*.png in " + dir);
        Arrays.sort(all);
        System.out.println("frames on disk: " + all.length + " in " + dir);

        // ---- pick the frames that land on outFps -------------------------------------
        List<File> picked = new ArrayList<>();
        int outCount = (int) Math.round(all.length * (outFps / (double) srcFps));
        for (int i = 0; i < outCount; i++) {
            int src = (int) Math.floor(i * (srcFps / (double) outFps));
            picked.add(all[Math.min(src, all.length - 1)]);
        }
        System.out.println("keeping " + picked.size() + " frames at " + outFps + " fps ("
                + String.format(java.util.Locale.ROOT, "%.1f", picked.size() / (double) outFps) + "s)");

        // ---- load + downscale ---------------------------------------------------------
        List<BufferedImage> frames = new ArrayList<>(picked.size());
        for (File f : picked) frames.add(downscale(ImageIO.read(f), scale));
        int w = frames.get(0).getWidth(), h = frames.get(0).getHeight();
        System.out.println("output size: " + w + "x" + h);

        // ---- one global palette over the whole sequence -------------------------------
        IndexColorModel palette = buildPalette(frames);
        System.out.println("palette: " + palette.getMapSize() + " colours");

        // ---- write ---------------------------------------------------------------------
        // GIF delays are whole CENTISECONDS, so the played-back rate is almost never exactly
        // the fps asked for (15 -> 7cs -> 14.3fps). Report what the file will actually do
        // rather than what was requested: a duration that quietly drifts from the storyboard
        // is the kind of thing nobody notices until the voiceover doesn't line up.
        int delayCs = Math.max(1, (int) Math.round(100.0 / outFps));
        writeGif(out, frames, palette, delayCs);
        double realFps = 100.0 / delayCs;
        System.out.printf(java.util.Locale.ROOT,
                "wrote %s - %d frames, %dcs/frame = %.1f fps, %.1fs of playback, %.2f MB%n",
                out, frames.size(), delayCs, realFps, frames.size() * delayCs / 100.0,
                out.length() / 1024.0 / 1024.0);
    }

    // =====================================================================
    //  scaling
    // =====================================================================

    /** Integer nearest-neighbour downscale. scale=1 is a straight copy to a known type. */
    static BufferedImage downscale(BufferedImage src, int scale) {
        int w = src.getWidth() / scale, h = src.getHeight() / scale;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                out.setRGB(x, y, src.getRGB(x * scale, y * scale) & 0xFFFFFF);
            }
        }
        return out;
    }

    // =====================================================================
    //  median-cut palette
    // =====================================================================

    /** One box of colours in RGB space, split on its longest axis until we have enough. */
    private record Box(List<int[]> colors) {
        int longestAxis() {
            int[] lo = {255, 255, 255}, hi = {0, 0, 0};
            for (int[] c : colors) {
                for (int a = 0; a < 3; a++) {
                    lo[a] = Math.min(lo[a], c[a]);
                    hi[a] = Math.max(hi[a], c[a]);
                }
            }
            int best = 0, bestSpan = -1;
            for (int a = 0; a < 3; a++) {
                int span = hi[a] - lo[a];
                if (span > bestSpan) { bestSpan = span; best = a; }
            }
            return best;
        }

        int span() {
            int[] lo = {255, 255, 255}, hi = {0, 0, 0};
            for (int[] c : colors) {
                for (int a = 0; a < 3; a++) {
                    lo[a] = Math.min(lo[a], c[a]);
                    hi[a] = Math.max(hi[a], c[a]);
                }
            }
            return Math.max(hi[0] - lo[0], Math.max(hi[1] - lo[1], hi[2] - lo[2]));
        }

        /** Weighted mean, so a box's representative sits where the pixels actually are. */
        int[] average() {
            long r = 0, g = 0, b = 0, n = 0;
            for (int[] c : colors) {
                long wgt = c[3];
                r += (long) c[0] * wgt; g += (long) c[1] * wgt; b += (long) c[2] * wgt; n += wgt;
            }
            if (n == 0) return new int[]{0, 0, 0};
            return new int[]{(int) (r / n), (int) (g / n), (int) (b / n)};
        }
    }

    static IndexColorModel buildPalette(List<BufferedImage> frames) {
        // histogram over sampled frames: {r, g, b, count}
        Map<Integer, Integer> hist = new HashMap<>();
        for (int i = 0; i < frames.size(); i += PALETTE_STRIDE) {
            BufferedImage f = frames.get(i);
            for (int y = 0; y < f.getHeight(); y++) {
                for (int x = 0; x < f.getWidth(); x++) {
                    hist.merge(f.getRGB(x, y) & 0xFFFFFF, 1, Integer::sum);
                }
            }
        }
        List<int[]> colors = new ArrayList<>(hist.size());
        for (Map.Entry<Integer, Integer> e : hist.entrySet()) {
            int c = e.getKey();
            colors.add(new int[]{(c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, e.getValue()});
        }
        System.out.println("distinct colours sampled: " + colors.size());

        List<Box> boxes = new ArrayList<>();
        boxes.add(new Box(colors));
        while (boxes.size() < MAX_COLORS) {
            // split the box with the widest colour spread - that is where banding shows
            int pick = -1, bestSpan = 0;
            for (int i = 0; i < boxes.size(); i++) {
                Box b = boxes.get(i);
                if (b.colors().size() < 2) continue;
                int s = b.span();
                if (s > bestSpan) { bestSpan = s; pick = i; }
            }
            if (pick < 0) break;
            Box b = boxes.remove(pick);
            int axis = b.longestAxis();
            List<int[]> sorted = new ArrayList<>(b.colors());
            sorted.sort((x, y) -> Integer.compare(x[axis], y[axis]));
            int mid = sorted.size() / 2;
            boxes.add(new Box(new ArrayList<>(sorted.subList(0, mid))));
            boxes.add(new Box(new ArrayList<>(sorted.subList(mid, sorted.size()))));
        }

        int n = boxes.size();
        byte[] r = new byte[n], g = new byte[n], bl = new byte[n];
        for (int i = 0; i < n; i++) {
            int[] avg = boxes.get(i).average();
            r[i] = (byte) avg[0]; g[i] = (byte) avg[1]; bl[i] = (byte) avg[2];
        }
        return new IndexColorModel(8, n, r, g, bl);
    }

    /** Map a truecolour frame onto the palette, nearest colour, no dithering. */
    static BufferedImage toIndexed(BufferedImage src, IndexColorModel palette, Map<Integer, Integer> cache) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, palette);
        var raster = out.getRaster();
        int n = palette.getMapSize();
        byte[] pr = new byte[n], pg = new byte[n], pb = new byte[n];
        palette.getReds(pr); palette.getGreens(pg); palette.getBlues(pb);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y) & 0xFFFFFF;
                Integer idx = cache.get(rgb);
                if (idx == null) {
                    int cr = (rgb >> 16) & 0xFF, cg = (rgb >> 8) & 0xFF, cb = rgb & 0xFF;
                    int best = 0, bestD = Integer.MAX_VALUE;
                    for (int i = 0; i < n; i++) {
                        int dr = cr - (pr[i] & 0xFF), dg = cg - (pg[i] & 0xFF), db = cb - (pb[i] & 0xFF);
                        int d = dr * dr + dg * dg + db * db;
                        if (d < bestD) { bestD = d; best = i; }
                    }
                    idx = best;
                    cache.put(rgb, idx);
                }
                raster.setSample(x, y, 0, idx);
            }
        }
        return out;
    }

    // =====================================================================
    //  GIF89a writing
    // =====================================================================

    static void writeGif(File out, List<BufferedImage> frames, IndexColorModel palette, int delayCs)
            throws Exception {
        ImageWriter writer = ImageIO.getImageWritersBySuffix("gif").next();
        try (FileImageOutputStream stream = new FileImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);

            ImageWriteParam param = writer.getDefaultWriteParam();
            Map<Integer, Integer> cache = new HashMap<>();
            for (int i = 0; i < frames.size(); i++) {
                BufferedImage indexed = toIndexed(frames.get(i), palette, cache);
                IIOMetadata meta = writer.getDefaultImageMetadata(
                        new ImageTypeSpecifier(indexed), param);
                configure(meta, delayCs, i == 0);
                writer.writeToSequence(new IIOImage(indexed, null, meta), param);
            }
            writer.endWriteSequence();
        }
        writer.dispose();
    }

    /**
     * Per-frame delay, plus the NETSCAPE2.0 application extension on the first frame - that
     * block, and only that block, is what makes a GIF loop forever. Written by hand into the
     * metadata tree because ImageIO exposes no other way to reach it.
     */
    static void configure(IIOMetadata meta, int delayCs, boolean first) throws Exception {
        String format = meta.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);

        IIOMetadataNode gce = child(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", "none");
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "FALSE");
        gce.setAttribute("delayTime", Integer.toString(delayCs));
        gce.setAttribute("transparentColorIndex", "0");

        if (first) {
            IIOMetadataNode appExts = child(root, "ApplicationExtensions");
            IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
            app.setAttribute("applicationID", "NETSCAPE");
            app.setAttribute("authenticationCode", "2.0");
            // sub-block 1, then a little-endian loop count of 0 = forever
            app.setUserObject(new byte[]{1, 0, 0});
            appExts.appendChild(app);
        }
        meta.setFromTree(format, root);
    }

    /** The named direct child of a metadata node, created if the writer left it out. */
    static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        root.appendChild(node);
        return node;
    }
}
