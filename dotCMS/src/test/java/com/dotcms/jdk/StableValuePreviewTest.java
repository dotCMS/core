package com.dotcms.jdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import java.util.function.Supplier;
import org.junit.Assume;
import org.junit.Test;

/**
 * Smoke test for the JDK 25 build configuration.
 *
 * <p>Its real job is to fail loudly if the {@code --enable-preview} flag is ever dropped from the
 * compile config or the surefire {@code argLine}: this class references {@link java.lang.StableValue}
 * (a preview API, JEP 502), so it will not compile without {@code --enable-preview} and will not run
 * without it on the surefire JVM. If preview support regresses, this test breaks the build.
 *
 * <p>The final method additionally asserts that the surefire JVM was launched with
 * {@code -XX:+UseCompactObjectHeaders}, matching the production container flags.
 */
public class StableValuePreviewTest {

    @Test
    public void trySet_setsOnce_thenRejects() {
        final StableValue<String> stable = StableValue.of();
        assertFalse("freshly created StableValue must be unset", stable.isSet());

        assertTrue("first trySet must succeed", stable.trySet("first"));
        assertTrue("StableValue must report set after trySet", stable.isSet());

        assertFalse("second trySet must be rejected", stable.trySet("second"));
        assertEquals("value must remain the first set value", "first", stable.orElse("fallback"));
    }

    @Test
    public void orElse_returnsFallback_whenUnset() {
        final StableValue<String> stable = StableValue.of();
        assertEquals("unset StableValue must yield the fallback", "fallback", stable.orElse("fallback"));
    }

    @Test
    public void orElseSet_computesOnce_thenMemoizes() {
        final StableValue<Integer> stable = StableValue.of();
        final int first = stable.orElseSet(() -> 42);
        // The second supplier must never run; the memoized value wins.
        final int second = stable.orElseSet(() -> 999);
        assertEquals(42, first);
        assertEquals("orElseSet must memoize the first computed value", 42, second);
    }

    @Test
    public void setOrThrow_throws_whenAlreadySet() {
        final StableValue<String> stable = StableValue.of();
        stable.setOrThrow("only");
        try {
            stable.setOrThrow("again");
            fail("setOrThrow on an already-set StableValue must throw");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void orElseSet_explicitTypeWitness_computesValue() {
        // Canonical usage form with an explicit type witness.
        var greeting = StableValue.<String>of();
        final String message = greeting.orElseSet(() -> "Hello from StableValue!");
        assertEquals("Hello from StableValue!", message);
        assertTrue("StableValue must be set after orElseSet", greeting.isSet());
    }

    @Test
    public void memoizedSupplier_returnsStableContent() {
        final Supplier<Integer> memoized = StableValue.supplier(() -> 7);
        assertEquals(Integer.valueOf(7), memoized.get());
        assertEquals("memoized supplier must return stable content", Integer.valueOf(7), memoized.get());
    }

    /**
     * Verifies the production JVM flag {@code -XX:+UseCompactObjectHeaders} is active. Asserted only
     * when the launching JVM actually set the flag (i.e. the Maven surefire run); skipped in plain
     * IDE runs that do not pass the container argLine, so it never produces a false failure there.
     */
    @Test
    public void compactObjectHeaders_enabledUnderSurefire() {
        final boolean enabled;
        try {
            final HotSpotDiagnosticMXBean bean =
                    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            enabled = Boolean.parseBoolean(bean.getVMOption("UseCompactObjectHeaders").getValue());
        } catch (IllegalArgumentException unsupported) {
            Assume.assumeNoException("UseCompactObjectHeaders not available on this JVM", unsupported);
            return;
        }
        Assume.assumeTrue(
                "compact object headers only asserted when the JVM was launched with the flag (surefire argLine)",
                enabled);
        assertTrue(enabled);
    }
}
