package com.dotcms.job.system.event;

import com.dotcms.api.system.event.SystemEventsFactory;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Map;

/**
 * Compares how many system events a node authored against how many its own poller observed, and says
 * whether the gap is acceptable (issue #36827, AC-007 / AC-008).
 *
 * <p>Every node observes every event it authored — and then deliberately skips it, because the author
 * already handled it at publish time. So authored-by-me and observed-by-me must match. That identity
 * is what made the original defect measurable: the issue quantified 50.5% and 63.4% loss precisely by
 * comparing those two numbers.
 *
 * <p>AC-008 sets the bar at ≤1% measured by hand over 24 hours on a two-node cluster. Doing it here
 * makes the same measurement continuous, so a regression announces itself instead of waiting to be
 * discovered after the next support case.
 */
public class SystemEventsReconciliation {

    /**
     * The agreed loss tolerance (AC-008). Chosen as a starting point: it absorbs a node restart or the
     * purge boundary landing mid-window, while still being far below the 50-63% loss that prompted the
     * fix. Tightening it is a deliberate decision — see contracts/delivery-contract.md §3.
     */
    public static final double LOSS_TOLERANCE_PERCENT = 1.0d;

    /**
     * Compares authored against observed for one node over a window.
     *
     * <p>The window is half-open at the recent end: it counts events authored between
     * {@code windowMillis} ago and one poll interval ago. An event authored more recently than that
     * has not yet had a poll in which to be observed, so counting it would report a healthy node as
     * losing events.
     *
     * @param serverId      the node
     * @param windowMillis  how far back to count, from one poll interval ago
     * @param observedCount how many of its own events the node's poller actually saw
     * @return the comparison
     */
    public Result reconcile(final String serverId, final long windowMillis, final long observedCount)
            throws DotDataException {

        final long now = System.currentTimeMillis();
        final long from = now - windowMillis;

        // Bounded at the top by one poll interval. Without it the count includes events authored so
        // recently that the poller cannot yet have seen them, and every one of those reads as loss:
        // a node authoring five events an hour with one in flight at the boundary reported 20% loss
        // while perfectly healthy. Below a few hundred events per window that false alarm fires
        // routinely, which trains operators to ignore the one signal this exists to raise.
        final long until = now - SystemEventsConfig.getPollIntervalMillis();

        final DotConnect dc = new DotConnect();
        dc.setSQL("SELECT count(*) AS authored FROM system_event WHERE server_id = ? AND created >= ? AND created < ?");
        dc.addParam(serverId);
        dc.addParam(from);
        dc.addParam(until);

        final List<Map<String, Object>> results = dc.loadObjectResults();
        final long authored = results.isEmpty() ? 0L
                : ((Number) results.get(0).get("authored")).longValue();

        final Result result = new Result(authored, observedCount);
        log(serverId, result);
        return result;
    }

    private void log(final String serverId, final Result result) {
        if (result.isWithinTolerance()) {
            Logger.info(this, "System event reconciliation for server [" + serverId + "]: authored="
                    + result.getAuthoredCount() + ", observed=" + result.getObservedCount()
                    + ", loss=" + String.format("%.2f", result.getLossPercent()) + "%"
                    + undeliverableSuffix());
        } else {
            Logger.warn(this, "System event delivery is losing events on server [" + serverId
                    + "]: authored=" + result.getAuthoredCount() + ", observed="
                    + result.getObservedCount() + ", loss="
                    + String.format("%.2f", result.getLossPercent()) + "% exceeds the "
                    + LOSS_TOLERANCE_PERCENT + "% tolerance. Events published on this node are not "
                    + "reaching its own poller, which means they are very likely not reaching the "
                    + "other nodes either." + undeliverableSuffix());
        }
    }

    /**
     * Reports how many events this node has had to skip because their payloads cannot be read.
     *
     * <p>This is where that count belongs: it is a partial answer to the question the loss figure
     * raises. An event whose payload has no Jackson creator is authored, stored and then permanently
     * undeliverable, so it shows up as loss and no amount of cursor correctness will recover it. The
     * counter existed but nothing read it, which left the number invisible outside individual
     * warnings in the log.
     *
     * <p>Stated as "since startup" on purpose — unlike the loss figure it is cumulative, not measured
     * over this window.
     *
     * @return a suffix naming the count, or an empty string when there is nothing to report
     */
    private String undeliverableSuffix() {
        final long undeliverable = SystemEventsFactory.getUnreadablePayloadCount();
        return undeliverable == 0L ? ""
                : " Note: " + undeliverable + " event(s) since startup could not be deserialized and "
                        + "are permanently undeliverable; see the earlier warnings for the payload "
                        + "classes involved.";
    }

    /**
     * The outcome of one reconciliation.
     */
    public static class Result {

        private final long authoredCount;
        private final long observedCount;
        private final double lossPercent;

        Result(final long authoredCount, final long observedCount) {
            this.authoredCount = authoredCount;
            this.observedCount = observedCount;
            // An idle node authored nothing; that is health, not 100% loss. Observing more than were
            // authored (a duplicate inside the overlap window) is not loss either.
            this.lossPercent = authoredCount <= 0 ? 0.0d
                    : Math.max(0.0d, ((double) (authoredCount - observedCount) / authoredCount) * 100.0d);
        }

        public long getAuthoredCount() {
            return this.authoredCount;
        }

        public long getObservedCount() {
            return this.observedCount;
        }

        public double getLossPercent() {
            return this.lossPercent;
        }

        public boolean isWithinTolerance() {
            return this.lossPercent <= LOSS_TOLERANCE_PERCENT;
        }
    }
}
