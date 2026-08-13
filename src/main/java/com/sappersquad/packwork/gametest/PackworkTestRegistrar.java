package com.sappersquad.packwork.gametest;

import com.sappersquad.packwork.Packwork;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Registers every {@link PackTest}-annotated method on {@link PackworkGameTests} into the
 * vanilla {@code TEST_FUNCTION} registry (the 1.21.5+ registry-based gametest framework).
 * Names are the method names in snake_case, so `contentsSurviveSaveLoad` becomes
 * {@code packwork:contents_survive_save_load} - which must match its
 * {@code data/packwork/test_instance/contents_survive_save_load.json} (generated, see
 * {@code tools/GenTestInstances.java}).
 *
 * <p>Fabric: plain eager registration during mod init. The runner is fabric-api's
 * gametest module - {@code ./gradlew runGametest} boots a headless server with
 * {@code -Dfabric-api.gametest} that runs every loaded test instance and exits nonzero
 * on failure, exactly what NeoForge's runGameTestServer did.
 */
public final class PackworkTestRegistrar {

    public static void register() {
        for (Method m : PackworkGameTests.class.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(PackTest.class)) continue;
            final Method method = m;
            Consumer<GameTestHelper> fn = helper -> {
                try {
                    method.invoke(null, helper);
                } catch (InvocationTargetException e) {
                    // unwrap so a GameTestAssertException still reads as the test's own failure
                    if (e.getCause() instanceof RuntimeException re) throw re;
                    if (e.getCause() instanceof Error err) throw err;
                    throw new RuntimeException(e.getCause());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            };
            Registry.register(BuiltInRegistries.TEST_FUNCTION, Packwork.id(snakeCase(m.getName())), fn);
        }
    }

    /** camelCase -> snake_case, digits kept with their run ("open3x3" -> "open3x3"). */
    static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }

    private PackworkTestRegistrar() {}
}
