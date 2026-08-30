package com.dotcms.job.system.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * The cursor-advance logic for the {@code system_event} poller (issue #36827) — deliberately pure and
 * database-free, so the rules that make the fix correct are unit-testable in milliseconds.
 *
 * <p>A poll is two calls. {@link #beginPoll(Long, long)} computes the range to read and captures the
 * instant the poll started; {@link #completePoll(SystemEventsPollWindow)} produces the next cursor
 * from that captured instant. The split is what makes "do not advance when the read fails"
 * expressible — a caller whose read throws simply never completes the window, and because this class
 * holds no state, the next poll recomputes exactly the same range.
 *
 * <p>Three invariants live here:
 *
 * <ol>
 *   <li>The cursor is only ever set to a timestamp captured <em>before</em> a read, never to a clock
 *       reading taken after processing — the original defect.</li>
 *   <li>The read floor sits one overlap window behind the cursor, so an event whose transaction
 *       commits after its {@code created} stamp is re-read instead of being skipped forever.</li>
 *   <li>The cursor never moves backwards, even if the system clock does. Correctness must not depend
 *       on clock synchronisation between nodes — that dependency is part of the bug.</li>
 * </ol>
 */
public class SystemEventsCursorTracker {

    /**
     * How many poll intervals a cursor may go unwritten before the poller is considered stalled.
     * Generous enough to absorb a slow poll or a missed tick, tight enough that a stopped poller is
     * noticed in under a minute at the default 5-second cadence.
     */
    static final int STALE_CURSOR_INTERVAL_MULTIPLIER = 6;

    /**
     * Read on each use rather than captured once, so that a value changed in the system table takes
     * effect on the next poll. An operator raising the overlap window in response to a commit-lag
     * warning should not have to restart the node to act on the warning they were just given.
     */
    private final LongSupplier overlapWindowMillis;
    private final LongSupplier maxBacklogMillis;
    private final IntSupplier lagWarnThresholdPercent;

    /** Events this node observed that it had authored itself; the input to reconciliation. */
    private final AtomicLong observedOwnEventCount = new AtomicLong(0L);

    /**
     * Ids delivered inside the current overlap window, mapped to their {@code created} stamp so they
     * can be evicted once the window moves past them. Bounded by the event rate over the window, not
     * by total event volume. Deliberately in memory only: after a restart the window may be
     * re-delivered, which the at-least-once contract permits.
     */
    private final Map<String, Long> deliveredEvents = new ConcurrentHashMap<>();

    public SystemEventsCursorTracker(final long overlapWindowMillis, final long maxBacklogMillis) {
        this(overlapWindowMillis, maxBacklogMillis,
                SystemEventsConfig.DEFAULT_LAG_WARN_THRESHOLD_PERCENT);
    }

    public SystemEventsCursorTracker(final long overlapWindowMillis, final long maxBacklogMillis,
                                     final int lagWarnThresholdPercent) {
        this(() -> overlapWindowMillis, () -> maxBacklogMillis, () -> lagWarnThresholdPercent);
    }

    /**
     * Live-config constructor used by the running poller: each value is re-read on use, so a change
     * takes effect on the next poll rather than at the next restart.
     */
    public SystemEventsCursorTracker(final LongSupplier overlapWindowMillis,
                                     final LongSupplier maxBacklogMillis,
                                     final IntSupplier lagWarnThresholdPercent) {
        this.overlapWindowMillis = overlapWindowMillis;
        this.maxBacklogMillis = maxBacklogMillis;
        this.lagWarnThresholdPercent = lagWarnThresholdPercent;
    }

    /**
     * Builds a tracker bound to live configuration.
     *
     * @return a tracker that re-reads its settings on every poll
     */
    public static SystemEventsCursorTracker fromConfig() {
        return new SystemEventsCursorTracker(SystemEventsConfig::getOverlapWindowMillis,
                SystemEventsConfig::getMaxBacklogMillis,
                SystemEventsConfig::getLagWarnThresholdPercent);
    }

    /**
     * Computes the range for a poll.
     *
     * @param storedCursor the node's persisted cursor, or {@code null} when it has never polled
     * @param now          epoch millis at the start of this poll
     * @return the window to read
     */
    public SystemEventsPollWindow beginPoll(final Long storedCursor, final long now) {

        if (null == storedCursor) {
            // First start, or the first poll after upgrade. Seed at "now" rather than at zero: a node
            // must never replay the retained backlog, which can be up to DELETE_EVENTS_OLDER_THAN days.
            evictOlderThan(now);
            return new SystemEventsPollWindow(now, now, false, 0L);
        }

        final long backlogBound = now - this.maxBacklogMillis.getAsLong();
        final boolean clamped = storedCursor < backlogBound;
        final long effectiveCursor = clamped ? backlogBound : storedCursor;
        final long skippedSpanMillis = clamped ? backlogBound - storedCursor : 0L;

        // Never regress on a backwards clock jump — re-reading is harmless under the at-least-once
        // contract, but moving the cursor backwards is not something callers should have to reason about.
        final long queryStartTime = Math.max(now, storedCursor);

        final long readFloor = effectiveCursor - this.overlapWindowMillis.getAsLong();

        // Anything older than the new floor can never be returned by a query again, so retaining its
        // id would only grow the map.
        evictOlderThan(readFloor);

        return new SystemEventsPollWindow(readFloor, queryStartTime, clamped, skippedSpanMillis);
    }

    /**
     * Produces the cursor to persist after a poll has been read successfully. Deliberately does not
     * consult the clock: the value was fixed when the poll began.
     *
     * @param window the window returned by {@link #beginPoll(Long, long)}
     * @return the new cursor value
     */
    public long completePoll(final SystemEventsPollWindow window) {
        return window.getQueryStartTime();
    }

    /**
     * @param eventId the event's stable identifier
     * @return true when this event has already been delivered inside the current overlap window
     */
    public boolean isAlreadyDelivered(final String eventId) {
        return this.deliveredEvents.containsKey(eventId);
    }

    /**
     * Records an event as delivered, so the overlap window's re-read does not deliver it again.
     *
     * @param eventId     the event's stable identifier
     * @param createdDate the event's created stamp, used to evict the entry once it leaves the window
     */
    public void markDelivered(final String eventId, final long createdDate) {
        this.deliveredEvents.put(eventId, createdDate);
    }

    /**
     * @return how many event ids are currently retained for dedupe; bounded by the event rate over
     * the overlap window, never by total event volume
     */
    public int trackedEventCount() {
        return this.deliveredEvents.size();
    }

    /**
     * Reports an event whose created stamp predates the entire overlap window — the one remaining way
     * an event can be lost after this fix. Exposed so the loss is observable rather than silent.
     *
     * @param createdDate the event's created stamp
     * @param window      the window it was read under
     * @return true when the event fell outside the window
     */
    public boolean isOutsideOverlapWindow(final long createdDate, final SystemEventsPollWindow window) {
        return createdDate < window.getReadFloor();
    }

    /**
     * Drops dedupe entries the overlap window has moved past.
     *
     * @param floor the new read floor
     */
    private void evictOlderThan(final long floor) {
        this.deliveredEvents.values().removeIf(createdDate -> createdDate < floor);
    }

    /**
     * Reports an event whose commit lag has reached the configured share of the overlap window. It
     * was delivered, but only just — this is the early signal that the window is too small for the
     * workload, raised while events are still arriving rather than after they start disappearing.
     *
     * @param createdDate   the event's created stamp
     * @param firstReadTime when this node first read the event
     * @return true when the lag has reached the warning threshold
     */
    public boolean isCommitLagApproachingWindow(final long createdDate, final long firstReadTime) {
        final long lagMillis = firstReadTime - createdDate;
        final long thresholdMillis =
                (this.overlapWindowMillis.getAsLong() * this.lagWarnThresholdPercent.getAsInt()) / 100;
        return lagMillis >= thresholdMillis;
    }

    /**
     * Reports a cursor that has not been written for several poll intervals, which means the poller
     * is not running or every read is failing. Without this a stalled poller is indistinguishable
     * from a quiet queue: both produce silence.
     *
     * @param cursorModDateMillis when the cursor was last written
     * @param now                 current epoch millis
     * @param pollIntervalMillis  the configured poll cadence
     * @return true when the poller appears stalled
     */
    public boolean isCursorStale(final long cursorModDateMillis, final long now,
                                 final long pollIntervalMillis) {
        return (now - cursorModDateMillis) > (pollIntervalMillis * STALE_CURSOR_INTERVAL_MULTIPLIER);
    }

    /**
     * Records that this node observed an event it had authored itself. Every node sees every event it
     * authored and then skips it, so this count should match the number of events the database says
     * this node authored over the same period — the comparison reconciliation makes.
     */
    public void recordObservedOwnEvent() {
        this.observedOwnEventCount.incrementAndGet();
    }

    /**
     * @return how many of its own events this node has observed since the last reconciliation
     */
    public long getObservedOwnEventCount() {
        return this.observedOwnEventCount.get();
    }

    /**
     * Resets the observed counter and returns its previous value, so a reconciliation window starts
     * clean without losing the count it is about to report on.
     *
     * @return the count before the reset
     */
    public long resetObservedOwnEventCount() {
        return this.observedOwnEventCount.getAndSet(0L);
    }
}
