package com.dotmarketing.business.cache.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.instrument.Instrumentation;
import org.junit.Assume;
import org.junit.Test;
import com.google.common.base.Optional;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * Sanity tests for {@link CacheSizingUtil}.
 *
 * <p>Retained size is computed from live {@code Unsafe} field offsets, so the exact byte count
 * depends on the JVM object layout — compressed oops, and notably {@code -XX:+UseCompactObjectHeaders}
 * (JEP 519), which shrinks object headers from 12 to 8 bytes. Asserting hardcoded byte sizes is
 * therefore brittle (it broke when compact object headers were enabled). These tests instead assert
 * layout-independent invariants: sizes are positive, ordered (a wrapped value is larger than an empty
 * wrapper), and within a sane bound.
 */
public class CacheSizingUtilTest {

    final String fourLetters = "CRAP";

    final CacheSizingUtil cacheSizer = new CacheSizingUtil();

    @Test
    public void test_sizeof_works() {
        final long contentletSize = cacheSizer.sizeOf(new Contentlet());
        assertTrue("a Contentlet must report a positive retained size", contentletSize > 0);

        final long fourLettersSize = cacheSizer.sizeOf(fourLetters);
        assertTrue("a 4-char String must report a positive retained size", fourLettersSize > 0);
        // Sanity upper bound: a tiny String cannot plausibly retain ~1KB on any object layout.
        assertTrue("a 4-char String size looks implausibly large: " + fourLettersSize,
                fourLettersSize < 1024);
    }

    @Test
    public void test_sizeof_null_works() {
        assertEquals("null must size to 0", 0L, cacheSizer.sizeOf(null));
    }

    @Test
    public void test_sizeof_optional_works() {
        final long emptySize = cacheSizer.sizeOf(Optional.fromNullable(null));
        assertTrue("an empty Optional must report a positive retained size", emptySize > 0);

        final long presentSize = cacheSizer.sizeOf(Optional.of(fourLetters));
        // Wrapping a String must retain more than an empty Optional (it adds the referenced String).
        assertTrue("a present Optional (" + presentSize + ") must be larger than an empty one ("
                + emptySize + ")", presentSize > emptySize);
    }

    /**
     * Verifies the byte-buddy agent is preloaded for this module's tests (via the surefire
     * {@code -javaagent} wiring), so {@link CacheSizingUtil} sizes objects with the authoritative
     * {@link Instrumentation#getObjectSize(Object)} rather than the Unsafe fallback. Skips (rather
     * than fails) when run without the agent — e.g. an IDE launch that omits the {@code -javaagent}.
     */
    @Test
    public void instrumentationAgent_isPreloaded() {
        final Instrumentation inst;
        try {
            inst = ByteBuddyAgent.getInstrumentation();
        } catch (IllegalStateException noAgent) {
            Assume.assumeNoException(
                    "byte-buddy agent not preloaded; run via Maven surefire (sets -javaagent) to exercise "
                            + "the Instrumentation.getObjectSize path", noAgent);
            return;
        }
        assertNotNull(inst);
        // getObjectSize is shallow; sizeOf is retained, so retained(String) >= shallow(String).
        final long shallow = inst.getObjectSize(fourLetters);
        assertTrue("shallow size must be positive", shallow > 0);
        assertTrue("retained size must be >= shallow size",
                cacheSizer.sizeOf(fourLetters) >= shallow);
    }
}
