package com.sappersquad.packwork.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks one Packwork gametest method: {@code public static void name(GameTestHelper)}.
 *
 * <p>(1.21.11 port) 1.21.5 removed the annotation-driven gametest framework outright -
 * tests are registry entries now ({@code TEST_FUNCTION} in code + one
 * {@code data/packwork/test_instance/<name>.json} per test). This annotation keeps the
 * 57 tests reading exactly as they always did; {@link PackworkTestRegistrar} scans for
 * it and registers each method, and the instance JSONs are generated from the same
 * method list (see {@code tools/GenTestInstances.java}). Adding a test = one annotated
 * method + re-running the generator.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PackTest {}
