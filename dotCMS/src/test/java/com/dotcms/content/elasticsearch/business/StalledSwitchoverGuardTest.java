package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;

/**
 * Unit tests for {@link StalledSwitchoverGuard}: when a server that is not the designated
 * ("lucky") server may perform the end-of-reindex switchover itself (issue #36482).
 *
 * <p>Only the oldest listed server promotes the rebuilt indices. If that server keeps pinging but
 * does not act, every other server waits for it forever and the full reindex never finishes, even
 * with the queue empty. The guard lets a waiting server step in once it has waited, for the same
 * reindex and the same lucky server, for longer than the threshold.</p>
 */
public class StalledSwitchoverGuardTest {

    private static final Duration THRESHOLD = Duration.ofSeconds(120);
    private static final Instant T0 = Instant.parse("2026-09-25T17:00:00Z");

    private final StalledSwitchoverGuard guard = new StalledSwitchoverGuard(THRESHOLD);

    /** The first wait only starts the clock. */
    @Test
    public void firstWait_doesNotTakeOver() {
        assertFalse(guard.shouldTakeOver("lucky", "working_1", T0));
    }

    /** Waiting past the threshold for the same reindex and lucky server takes over. */
    @Test
    public void waitingPastTheThreshold_takesOver() {
        guard.shouldTakeOver("lucky", "working_1", T0);

        assertTrue(guard.shouldTakeOver("lucky", "working_1", T0.plus(THRESHOLD).plusSeconds(1)));
    }

    /** Still within the threshold: keep waiting. */
    @Test
    public void waitingWithinTheThreshold_keepsWaiting() {
        guard.shouldTakeOver("lucky", "working_1", T0);

        assertFalse(guard.shouldTakeOver("lucky", "working_1", T0.plus(THRESHOLD).minusSeconds(1)));
    }

    /** A different reindex is a new wait: the clock restarts. */
    @Test
    public void newReindex_restartsTheClock() {
        guard.shouldTakeOver("lucky", "working_1", T0);

        assertFalse(guard.shouldTakeOver("lucky", "working_2", T0.plus(THRESHOLD).plusSeconds(1)));
    }

    /** A different lucky server (the list changed) is a new wait: the clock restarts. */
    @Test
    public void newLuckyServer_restartsTheClock() {
        guard.shouldTakeOver("lucky", "working_1", T0);

        assertFalse(guard.shouldTakeOver("other", "working_1", T0.plus(THRESHOLD).plusSeconds(1)));
    }

    /** No reindex target known: never take over, there is nothing to promote. */
    @Test
    public void unknownReindex_neverTakesOver() {
        guard.shouldTakeOver("lucky", null, T0);

        assertFalse(guard.shouldTakeOver("lucky", null, T0.plus(THRESHOLD).plusSeconds(1)));
    }
}
