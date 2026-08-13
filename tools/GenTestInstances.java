import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * (1.21.11+ branches) Regenerates data/packwork/test_instance/*.json - one per
 * {@code @PackTest} method in PackworkGameTests - after adding or renaming a test.
 * 1.21.5 turned gametests into registry entries: the function registers in code
 * (PackworkTestRegistrar scans the annotations) but each test ALSO needs a datapack
 * instance JSON naming its environment/structure/timeout, and this writes those.
 *
 * <p>Run from the project root: {@code java tools/GenTestInstances.java}
 * (Java only - this machine has no Python/Node; same convention as GenTextures.)
 */
public class GenTestInstances {

    private static final Path SOURCE = Path.of(
            "src/main/java/com/sappersquad/packwork/gametest/PackworkGameTests.java");
    private static final Path OUT_DIR = Path.of("src/main/resources/data/packwork/test_instance");
    private static final Pattern TEST_METHOD = Pattern.compile(
            "@PackTest\\s+public static void (\\w+)\\(GameTestHelper");

    public static void main(String[] args) throws IOException {
        String src = Files.readString(SOURCE, StandardCharsets.UTF_8);
        Matcher m = TEST_METHOD.matcher(src);
        List<String> names = new ArrayList<>();
        while (m.find()) names.add(m.group(1));
        if (names.isEmpty()) throw new IllegalStateException("no @PackTest methods found - regex drift?");

        Files.createDirectories(OUT_DIR);
        // wipe stale instances so a renamed test can't leave a dangling registry reference
        try (var listing = Files.list(OUT_DIR)) {
            for (Path p : listing.toList()) Files.delete(p);
        }
        for (String name : names) {
            String snake = name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
            String json = """
                    {
                      "type": "minecraft:function",
                      "function": "packwork:%s",
                      "environment": "minecraft:default",
                      "structure": "minecraft:empty",
                      "max_ticks": 400
                    }
                    """.formatted(snake);
            Files.writeString(OUT_DIR.resolve(snake + ".json"), json, StandardCharsets.UTF_8);
        }
        System.out.println("wrote " + names.size() + " test instances to " + OUT_DIR);
    }
}
