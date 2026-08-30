import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Lang audit: every locale file must carry exactly the keys en_us carries - no missing
 * lines (which fall back to raw key text in game) and no orphans left behind by a rename.
 * It also flags any value whose printf placeholders drifted from the English, which is the
 * one translation mistake that CRASHES a screen instead of just reading oddly.
 *
 * <p>Java only - this machine has no Python or Node. Run from the repo root:
 * <pre>java tools/CheckLang.java</pre>
 * Exits non-zero on any problem so it can gate a release pass.
 */
public class CheckLang {

    private static final String DIR = "src/main/resources/assets/packwork/lang";
    /** Keys that live only in the drafted locales - notes to translators, never shown in game. */
    private static final Set<String> META = Set.of("packwork.translation.status");

    public static void main(String[] args) throws IOException {
        Path dir = Paths.get(DIR);
        Map<String, String> en = read(dir.resolve("en_us.json"));
        System.out.println("en_us: " + en.size() + " keys");

        int problems = 0;
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> s = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : s) if (!p.getFileName().toString().equals("en_us.json")) files.add(p);
        }
        files.sort(Comparator.comparing(Path::toString));

        for (Path p : files) {
            Map<String, String> loc = read(p);
            String name = p.getFileName().toString();
            List<String> missing = new ArrayList<>();
            List<String> extra = new ArrayList<>();
            List<String> fmt = new ArrayList<>();
            for (String k : en.keySet()) if (!loc.containsKey(k)) missing.add(k);
            for (String k : loc.keySet()) if (!en.containsKey(k) && !META.contains(k)) extra.add(k);
            for (Map.Entry<String, String> e : loc.entrySet()) {
                String enV = en.get(e.getKey());
                if (enV == null) continue;
                if (placeholders(enV) != placeholders(e.getValue())) fmt.add(e.getKey());
            }
            boolean ok = missing.isEmpty() && extra.isEmpty() && fmt.isEmpty();
            System.out.println((ok ? "  OK   " : "  FAIL ") + name + "  (" + loc.size() + " keys)");
            if (!missing.isEmpty()) { System.out.println("    missing: " + missing); problems++; }
            if (!extra.isEmpty())   { System.out.println("    orphan:  " + extra); problems++; }
            if (!fmt.isEmpty())     { System.out.println("    %s count differs: " + fmt); problems++; }
        }
        if (problems > 0) {
            System.out.println(problems + " problem group(s)");
            System.exit(1);
        }
        System.out.println("all locales match en_us");
    }

    private static int placeholders(String s) {
        int n = 0;
        for (int i = 0; i + 1 < s.length(); i++) if (s.charAt(i) == '%' && s.charAt(i + 1) == 's') n++;
        return n;
    }

    /**
     * A deliberately small reader for the flat one-object-of-strings shape Minecraft lang
     * files use. Handles \" \\ \n escapes; anything fancier is not valid here anyway.
     */
    private static Map<String, String> read(Path p) throws IOException {
        String src = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        Map<String, String> out = new LinkedHashMap<>();
        List<String> strings = new ArrayList<>();
        StringBuilder cur = null;
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (cur == null) {
                if (c == '"') cur = new StringBuilder();
            } else if (c == '\\' && i + 1 < src.length()) {
                char n = src.charAt(++i);
                cur.append(n == 'n' ? '\n' : n);
            } else if (c == '"') {
                strings.add(cur.toString());
                cur = null;
            } else {
                cur.append(c);
            }
        }
        for (int i = 0; i + 1 < strings.size(); i += 2) out.put(strings.get(i), strings.get(i + 1));
        return out;
    }
}
