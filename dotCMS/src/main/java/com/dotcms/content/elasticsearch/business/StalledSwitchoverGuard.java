package com.dotcms.content.elasticsearch.business;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Decides when a server that is not the designated ("lucky") server may perform the end-of-reindex
 * switchover itself.
 *
 * <p>Only the oldest server in the reindexing list promotes the rebuilt indices; every other server
 * waits for it. The list holds every server that pinged in the last few minutes, so a server that
 * keeps pinging but does not act — a dead worker, a stuck thread — keeps every other server waiting
 * forever: the queue is empty, the rebuilt indices are complete, and the full reindex still reads
 * as in progress (issue #36482).</p>
 *
 * <p>A waiting server steps in once it has waited for longer than the threshold for the same
 * reindex and the same lucky server. Either one changing is a different wait and restarts the
 * clock. Promoting the same indices twice leaves the same pointers, so the worst case of two
 * servers stepping in together is repeated work.</p>
 *
 * <p>Not thread-safe: callers are {@code synchronized}.</p>
 */
final class StalledSwitchoverGuard {

    private final Duration threshold;
    private String waitingFor;
    private String waitingOn;
    private Instant since;

    /**
     * @param threshold how long to wait for the lucky server before stepping in
     */
    StalledSwitchoverGuard(final Duration threshold) {
        this.threshold = threshold;
    }

    /**
     * Records one more wait and says whether it has now lasted long enough to step in.
     *
     * @param luckyServer   the server designated to switch over
     * @param reindexTarget the index being promoted, identifying this reindex; {@code null} when
     *                      unknown, in which case there is nothing to promote and the answer is no
     * @param now           the time of this wait
     * @return {@code true} when this server should perform the switchover itself
     */
    boolean shouldTakeOver(final String luckyServer, final String reindexTarget, final Instant now) {
        if (null == reindexTarget) {
            since = null;
            return false;
        }
        if (null == since || !Objects.equals(waitingFor, reindexTarget)
                || !Objects.equals(waitingOn, luckyServer)) {
            waitingFor = reindexTarget;
            waitingOn = luckyServer;
            since = now;
            return false;
        }
        return Duration.between(since, now).compareTo(threshold) > 0;
    }
}
