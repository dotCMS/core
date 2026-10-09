package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The switchover's minimum-runtime guard gives a fresh reindex index a short settle window. It
 * must not turn into an open-ended wait when the index name claims a creation time far in the
 * future: such a name is wrong, and waiting for the clock to reach it protects nothing (issue
 * #37282).
 */
public class ContentletIndexAPIImplMinimumRuntimeGuardTest {

    private static final long MINIMUM = 30_000L;

    /** A reindex that has run past the minimum may switch over. */
    @Test
    public void passes_whenElapsedReachesTheMinimum() {
        assertTrue(ContentletIndexAPIImpl.minimumRuntimeGuardPasses(MINIMUM, MINIMUM));
        assertTrue(ContentletIndexAPIImpl.minimumRuntimeGuardPasses(5 * 60_000L, MINIMUM));
    }

    /** A reindex still inside the settle window waits. */
    @Test
    public void waits_whileInsideTheSettleWindow() {
        assertFalse(ContentletIndexAPIImpl.minimumRuntimeGuardPasses(0L, MINIMUM));
        assertFalse(ContentletIndexAPIImpl.minimumRuntimeGuardPasses(MINIMUM - 1, MINIMUM));
    }

    /**
     * A name only slightly ahead of the clock — the size of ordinary clock drift between nodes —
     * still waits; the wait is bounded by the minimum plus that drift.
     */
    @Test
    public void waits_whenNameIsAheadByNoMoreThanTheMinimum() {
        assertFalse(ContentletIndexAPIImpl.minimumRuntimeGuardPasses(-2_000L, MINIMUM));
        assertFalse(ContentletIndexAPIImpl.minimumRuntimeGuardPasses(-MINIMUM, MINIMUM));
    }

    /**
     * A name ahead of the clock by more than the minimum cannot be a settling index; the guard
     * lets the switchover proceed instead of waiting for the clock to reach the bogus time.
     */
    @Test
    public void passes_whenNameIsAheadByMoreThanTheMinimum() {
        assertTrue(ContentletIndexAPIImpl.minimumRuntimeGuardPasses(-MINIMUM - 1, MINIMUM));
        assertTrue(ContentletIndexAPIImpl.minimumRuntimeGuardPasses(-60 * 60_000L, MINIMUM));
    }
}
