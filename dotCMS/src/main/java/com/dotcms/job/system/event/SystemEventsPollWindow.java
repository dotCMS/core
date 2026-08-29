package com.dotcms.job.system.event;

/**
 * The range a single poll of the {@code system_event} queue will read, plus what the caller must be
 * told about it (issue #36827).
 *
 * <p>Immutable, and produced only by {@link SystemEventsCursorTracker#beginPoll(Long, long)}. The
 * query start time is captured when the window is created — <em>before</em> the read — which is what
 * keeps the cursor honest: the original defect advanced the mark to a clock reading taken after
 * processing had finished, covering wall-clock time the query never looked at.
 */
public class SystemEventsPollWindow {

    private final long readFloor;
    private final long queryStartTime;
    private final boolean clamped;
    private final long skippedSpanMillis;

    SystemEventsPollWindow(final long readFloor, final long queryStartTime, final boolean clamped,
                           final long skippedSpanMillis) {
        this.readFloor = readFloor;
        this.queryStartTime = queryStartTime;
        this.clamped = clamped;
        this.skippedSpanMillis = skippedSpanMillis;
    }

    /**
     * @return epoch millis of the oldest event this poll will read — the cursor less one overlap
     * window, so an event that committed after its {@code created} stamp is still caught
     */
    public long getReadFloor() {
        return this.readFloor;
    }

    /**
     * @return epoch millis captured immediately BEFORE the read; this becomes the next cursor
     */
    public long getQueryStartTime() {
        return this.queryStartTime;
    }

    /**
     * @return true when the stored cursor was older than the backlog bound and had to be clamped
     */
    public boolean isClamped() {
        return this.clamped;
    }

    /**
     * @return the span of time the clamp skipped over, in millis; zero when not clamped
     */
    public long getSkippedSpanMillis() {
        return this.skippedSpanMillis;
    }

    @Override
    public String toString() {
        return "SystemEventsPollWindow{readFloor=" + this.readFloor + ", queryStartTime="
                + this.queryStartTime + ", clamped=" + this.clamped + ", skippedSpanMillis="
                + this.skippedSpanMillis + '}';
    }
}
