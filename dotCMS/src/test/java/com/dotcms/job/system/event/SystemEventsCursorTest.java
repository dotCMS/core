package com.dotcms.job.system.event;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SystemEventsCursorTracker}, the pure cursor-advance logic behind the fix for
 * system event loss in a cluster (issue #36827).
 *
 * <p>These are deliberately DB-free. The rules that make the fix correct — advance from the query
 * start rather than from a post-processing clock read, seed without replaying the backlog, clamp a
 * stale cursor, never move backwards — are the easiest things to get subtly wrong and the most
 * expensive to diagnose in production, so they are pinned here where they run in milliseconds.
 *
 * <p>A poll is modelled as two calls: {@code beginPoll} computes the range to read, and
 * {@code completePoll} produces the next cursor. The split is what makes "do not advance when the
 * read fails" expressible — a caller that abandons the window simply never calls completePoll.
 */
public class SystemEventsCursorTest {

    private static final long OVERLAP_WINDOW_MILLIS = TimeUnit.SECONDS.toMillis(120);
    private static final long MAX_BACKLOG_MILLIS = TimeUnit.MINUTES.toMillis(60);

    private SystemEventsCursorTracker tracker() {
        return new SystemEventsCursorTracker(OVERLAP_WINDOW_MILLIS, MAX_BACKLOG_MILLIS);
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#completePoll(SystemEventsPollWindow)}
     * Given Scenario: A poll begins at a known instant and processing takes time afterwards
     * ExpectedResult: The new cursor is the instant the query STARTED, never a clock reading taken
     * after processing finished. This is the defect in the original code: the mark was set to
     * {@code new Date().getTime()} after the delegate returned, so it covered wall-clock time the
     * query had never actually read.
     */
    @Test
    public void test_cursor_advances_to_the_query_start_not_a_later_clock_reading() {
        final SystemEventsCursorTracker tracker = tracker();
        final long queryStart = 5_000L;

        final SystemEventsPollWindow window = tracker.beginPoll(1_000L, queryStart);

        assertEquals(queryStart, window.getQueryStartTime());
        // completePoll must not consult the clock — the value is fixed when the poll began.
        assertEquals(queryStart, tracker.completePoll(window));
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#beginPoll(Long, long)}
     * Given Scenario: No cursor row exists yet — a fresh install, or the first start after upgrade
     * ExpectedResult: The cursor seeds at "now" and the read floor is "now". A node must not replay
     * the retained backlog (up to 31 days) on its very first poll.
     */
    @Test
    public void test_first_run_seeds_at_now_without_replaying_the_backlog() {
        final SystemEventsCursorTracker tracker = tracker();
        final long now = 1_000_000L;

        final SystemEventsPollWindow window = tracker.beginPoll(null, now);

        assertTrue("A seeded poll must not reach back before the node started",
                window.getReadFloor() >= now);
        assertEquals(now, tracker.completePoll(window));
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#beginPoll(Long, long)}
     * Given Scenario: A steady-state poll with an existing cursor
     * ExpectedResult: The read floor sits one overlap window behind the cursor, so an event that
     * committed after its created timestamp is re-read rather than skipped forever
     */
    @Test
    public void test_read_floor_sits_one_overlap_window_behind_the_cursor() {
        final SystemEventsCursorTracker tracker = tracker();
        final long cursor = 10_000_000L;

        final SystemEventsPollWindow window = tracker.beginPoll(cursor, cursor + 5_000L);

        assertEquals(cursor - OVERLAP_WINDOW_MILLIS, window.getReadFloor());
        assertFalse(window.isClamped());
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#beginPoll(Long, long)}
     * Given Scenario: The node was down for three hours, so its persisted cursor is far older than
     * the configured backlog clamp
     * ExpectedResult: The cursor is clamped to the backlog bound, the window reports it was clamped,
     * and the skipped span is exposed so the operator can be told exactly what was missed
     */
    @Test
    public void test_stale_cursor_is_clamped_and_reports_the_skipped_span() {
        final SystemEventsCursorTracker tracker = tracker();
        final long now = TimeUnit.DAYS.toMillis(10);
        final long staleCursor = now - TimeUnit.HOURS.toMillis(3);

        final SystemEventsPollWindow window = tracker.beginPoll(staleCursor, now);

        assertTrue("A cursor older than the backlog bound must be clamped", window.isClamped());
        assertEquals(now - MAX_BACKLOG_MILLIS - OVERLAP_WINDOW_MILLIS, window.getReadFloor());
        assertEquals("The skipped span must be reported, not silently swallowed",
                (now - MAX_BACKLOG_MILLIS) - staleCursor, window.getSkippedSpanMillis());
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#beginPoll(Long, long)}
     * Given Scenario: The cursor is recent enough to sit inside the backlog bound
     * ExpectedResult: No clamp, and no skipped span
     */
    @Test
    public void test_recent_cursor_is_not_clamped() {
        final SystemEventsCursorTracker tracker = tracker();
        final long now = TimeUnit.DAYS.toMillis(10);
        final long recentCursor = now - TimeUnit.MINUTES.toMillis(5);

        final SystemEventsPollWindow window = tracker.beginPoll(recentCursor, now);

        assertFalse(window.isClamped());
        assertEquals(0L, window.getSkippedSpanMillis());
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#beginPoll(Long, long)}
     * Given Scenario: A poll is begun and then abandoned because the read threw
     * ExpectedResult: The next poll computes the same range. Abandoning a window must leave no trace,
     * so a failed read retries its range instead of skipping it — the original code advanced the mark
     * regardless of whether the read succeeded.
     */
    @Test
    public void test_abandoned_poll_does_not_advance_the_range() {
        final SystemEventsCursorTracker tracker = tracker();
        final long cursor = 10_000_000L;

        final SystemEventsPollWindow abandoned = tracker.beginPoll(cursor, cursor + 5_000L);
        // read throws here — completePoll is never called

        final SystemEventsPollWindow retry = tracker.beginPoll(cursor, cursor + 10_000L);

        assertEquals("A failed read must retry the same range, not skip it",
                abandoned.getReadFloor(), retry.getReadFloor());
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#completePoll(SystemEventsPollWindow)}
     * Given Scenario: The system clock jumps backwards, so the poll start is earlier than the stored
     * cursor
     * ExpectedResult: The cursor does not move backwards. Correctness must not depend on clock
     * synchronisation — that dependency is part of the original defect.
     */
    @Test
    public void test_cursor_never_moves_backwards_under_clock_skew() {
        final SystemEventsCursorTracker tracker = tracker();
        final long cursor = 10_000_000L;
        final long skewedNow = cursor - TimeUnit.MINUTES.toMillis(1);

        final SystemEventsPollWindow window = tracker.beginPoll(cursor, skewedNow);

        assertTrue("The cursor must never regress on a backwards clock jump",
                tracker.completePoll(window) >= cursor);
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#isAlreadyDelivered(String)} and
     * {@link SystemEventsCursorTracker#markDelivered(String, long)}
     * Given Scenario: The overlap window causes the same event to be read again on the next poll
     * ExpectedResult: The second read is suppressed. The window is what makes late commits visible;
     * without dedupe it would also make every event inside it be delivered repeatedly.
     */
    @Test
    public void test_dedupe_suppresses_re_delivery_within_the_window() {
        final SystemEventsCursorTracker tracker = tracker();
        final long cursor = 10_000_000L;

        assertFalse(tracker.isAlreadyDelivered("event-1"));
        tracker.markDelivered("event-1", cursor);
        assertTrue("An event already delivered must not be delivered again inside the window",
                tracker.isAlreadyDelivered("event-1"));
    }

    /**
     * Method to test: eviction of the dedupe set
     * Given Scenario: Polls advance far enough that previously delivered events fall out of the
     * overlap window
     * ExpectedResult: Their ids are evicted. The set must stay bounded by the event rate over the
     * window, never by total event volume — otherwise the fix leaks memory on a busy cluster.
     */
    @Test
    public void test_dedupe_set_evicts_everything_older_than_the_window() {
        final SystemEventsCursorTracker tracker = tracker();
        final long cursor = 10_000_000L;

        tracker.beginPoll(cursor, cursor);
        tracker.markDelivered("old-event", cursor - TimeUnit.MINUTES.toMillis(30));
        tracker.markDelivered("recent-event", cursor);

        // A later poll: the floor moves past the old event, which can never be returned again.
        final long muchLater = cursor + TimeUnit.MINUTES.toMillis(30);
        tracker.beginPoll(muchLater, muchLater);

        assertFalse("Ids older than the window must be evicted, or the set grows without bound",
                tracker.isAlreadyDelivered("old-event"));
        assertEquals("Only ids still inside the window are retained", 0, tracker.trackedEventCount());
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#isOutsideOverlapWindow(long, SystemEventsPollWindow)}
     * Given Scenario: An event whose created stamp predates the entire overlap window — the one
     * remaining way an event can still be lost after this fix
     * ExpectedResult: The tracker reports it, so US4 can warn instead of the loss being silent. A
     * bounded, observable residual is the point; a silent one is the bug.
     */
    @Test
    public void test_event_older_than_the_window_is_reported_not_silently_ignored() {
        final SystemEventsCursorTracker tracker = tracker();
        final long cursor = 10_000_000L;
        final SystemEventsPollWindow window = tracker.beginPoll(cursor, cursor);

        assertTrue(tracker.isOutsideOverlapWindow(cursor - TimeUnit.MINUTES.toMillis(10), window));
        assertFalse(tracker.isOutsideOverlapWindow(cursor - TimeUnit.SECONDS.toMillis(30), window));
    }

    // ---------------------------------------------------------------------------------------------
    // US4 - observability (AC-007). The residual limitation of this fix is that a transaction open
    // longer than the overlap window can still lose its events. That is acceptable only because it is
    // bounded, configurable and VISIBLE; a silent residual would be the original bug in miniature.
    // ---------------------------------------------------------------------------------------------

    private SystemEventsCursorTracker trackerWithLagThreshold(final int percent) {
        return new SystemEventsCursorTracker(OVERLAP_WINDOW_MILLIS, MAX_BACKLOG_MILLIS, percent);
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#isCommitLagApproachingWindow(long, long)}
     * Given Scenario: An event whose commit lag has reached the configured share of the overlap
     * window — it was delivered, but only just
     * ExpectedResult: Reported, so the operator learns the window is too small for this workload
     * BEFORE events start being lost, rather than afterwards
     */
    @Test
    public void test_commit_lag_approaching_the_window_is_reported() {
        final SystemEventsCursorTracker tracker = trackerWithLagThreshold(50);
        final long readAt = 10_000_000L;

        // 70% of a 120s window = 84s of lag: delivered, but the margin is thin.
        assertTrue(tracker.isCommitLagApproachingWindow(
                readAt - TimeUnit.SECONDS.toMillis(84), readAt));
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#isCommitLagApproachingWindow(long, long)}
     * Given Scenario: An event committed promptly, well inside the threshold
     * ExpectedResult: Not reported. The signal has to stay quiet in normal operation or it will be
     * ignored when it matters.
     */
    @Test
    public void test_normal_commit_lag_is_not_reported() {
        final SystemEventsCursorTracker tracker = trackerWithLagThreshold(50);
        final long readAt = 10_000_000L;

        assertFalse(tracker.isCommitLagApproachingWindow(
                readAt - TimeUnit.SECONDS.toMillis(5), readAt));
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#isCommitLagApproachingWindow(long, long)}
     * Given Scenario: The threshold is configured lower, so the warning fires earlier
     * ExpectedResult: The same lag that was quiet at 50% is reported at 10%
     */
    @Test
    public void test_lag_threshold_is_configurable() {
        final long readAt = 10_000_000L;
        final long lagOf20Seconds = readAt - TimeUnit.SECONDS.toMillis(20);

        assertFalse(trackerWithLagThreshold(50).isCommitLagApproachingWindow(lagOf20Seconds, readAt));
        assertTrue(trackerWithLagThreshold(10).isCommitLagApproachingWindow(lagOf20Seconds, readAt));
    }

    /**
     * Method to test: {@link SystemEventsCursorTracker#isCursorStale(long, long)}
     * Given Scenario: The cursor has not been written for far longer than the poll interval, because
     * the poller stopped or every read is failing
     * ExpectedResult: Reported. Before this fix a stalled poller was indistinguishable from a quiet
     * queue — both produced no output whatsoever.
     */
    @Test
    public void test_stalled_poller_is_detectable_from_the_cursor_age() {
        final SystemEventsCursorTracker tracker = tracker();
        final long now = 10_000_000L;
        final long pollIntervalMillis = TimeUnit.SECONDS.toMillis(5);

        assertTrue("A cursor untouched for minutes means the poller is not running",
                tracker.isCursorStale(now - TimeUnit.MINUTES.toMillis(5), now, pollIntervalMillis));
        assertFalse("A cursor written one interval ago is healthy",
                tracker.isCursorStale(now - pollIntervalMillis, now, pollIntervalMillis));
    }
}
