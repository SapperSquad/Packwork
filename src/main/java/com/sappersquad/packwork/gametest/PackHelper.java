package com.sappersquad.packwork.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.network.chat.Component;

/**
 * (1.21.10 branch) A {@link GameTestHelper} with the plain-String assert overloads the
 * suite has always used. 1.21.10's helper only takes {@link Component} messages (the
 * String overloads return in 1.21.11), and rewriting ~400 call sites per branch would be
 * churn for nothing - so the registrar hands every test one of these instead, built over
 * the SAME {@code testInfo} vanilla drives, and the 57 test bodies stay word-for-word
 * identical across the port branches.
 */
public class PackHelper extends GameTestHelper {

    public PackHelper(GameTestInfo info) {
        super(info);
    }

    public void assertTrue(boolean condition, String message) {
        assertTrue(condition, Component.literal(message));
    }

    public void assertFalse(boolean condition, String message) {
        assertFalse(condition, Component.literal(message));
    }

    public void fail(String message) {
        fail(Component.literal(message));
    }
}
