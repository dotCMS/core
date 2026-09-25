package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dotmarketing.common.reindex.StarvedSliceDetector.SliceSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Unit tests for {@link StarvedSliceDetector}: how a server whose own share of the reindex journal
 * is empty recognises a share that another server is listed for but is not draining (issue #36482).
 *
 * <p>The journal is split across the servers that pinged recently, by {@code MOD(id, servers)}. A
 * server that pings but does not index — a dead worker, a stuck thread — leaves its share untouched
 * forever, and a full reindex hangs part-way with no error. The detector judges a share by
 * <em>progress</em>, not by timestamps: a share whose row count and lowest id stay the same for
 * longer than the threshold is not being drained.</p>
 */
public class StarvedSliceDetectorTest {

    private static final Duration THRESHOLD = Duration.ofSeconds(120);
    private static final Instant T0 = Instant.parse("2026-09-25T17:00:00Z");

    private final StarvedSliceDetector detector = new StarvedSliceDetector(THRESHOLD);

    /** The first sighting of a share only starts the clock: nothing is starved yet. */
    @Test
    public void firstObservation_isNeverStarved() {
        assertTrue(detector.starvedSlices(Map.of(1, new SliceSnapshot(336, 5L)), 0, 2, T0)
                .isEmpty());
    }

    /** A share that has not moved for longer than the threshold is starved. */
    @Test
    public void unchangedPastTheThreshold_isStarved() {
        detector.starvedSlices(Map.of(1, new SliceSnapshot(336, 5L)), 0, 2, T0);

        final List<Integer> starved = detector.starvedSlices(
                Map.of(1, new SliceSnapshot(336, 5L)), 0, 2, T0.plus(THRESHOLD).plusSeconds(1));

        assertEquals(List.of(1), starved);
    }

    /** Unchanged, but not yet for the whole threshold: not starved. */
    @Test
    public void unchangedWithinTheThreshold_isNotStarved() {
        detector.starvedSlices(Map.of(1, new SliceSnapshot(336, 5L)), 0, 2, T0);

        assertTrue(detector.starvedSlices(Map.of(1, new SliceSnapshot(336, 5L)), 0, 2,
                T0.plus(THRESHOLD).minusSeconds(1)).isEmpty());
    }

    /**
     * A share whose owner is draining — its count drops between samples — restarts the clock every
     * time, so a busy but healthy server is never taken over.
     */
    @Test
    public void progressRestartsTheClock() {
        detector.starvedSlices(Map.of(1, new SliceSnapshot(336, 5L)), 0, 2, T0);
        detector.starvedSlices(Map.of(1, new SliceSnapshot(200, 301L)), 0, 2,
                T0.plus(THRESHOLD).plusSeconds(1));

        assertTrue("the share moved, so the owner is draining",
                detector.starvedSlices(Map.of(1, new SliceSnapshot(200, 301L)), 0, 2,
                        T0.plus(THRESHOLD).plusSeconds(2)).isEmpty());
    }

    /** New rows arriving in a share also count as a change, not as a stall. */
    @Test
    public void newRowsAreAChange() {
        detector.starvedSlices(Map.of(1, new SliceSnapshot(10, 5L)), 0, 2, T0);

        assertTrue(detector.starvedSlices(Map.of(1, new SliceSnapshot(11, 5L)), 0, 2,
                T0.plus(THRESHOLD).plusSeconds(1)).isEmpty());
    }

    /** This server's own share is never reported: it is drained by the normal path. */
    @Test
    public void ownSlice_isNeverReported() {
        detector.starvedSlices(Map.of(0, new SliceSnapshot(10, 4L)), 0, 2, T0);

        assertTrue(detector.starvedSlices(Map.of(0, new SliceSnapshot(10, 4L)), 0, 2,
                T0.plus(THRESHOLD).plusSeconds(1)).isEmpty());
    }

    /**
     * When the number of servers changes, every share is a different set of rows, so what was
     * observed before means nothing and the clock restarts for all of them.
     */
    @Test
    public void serverCountChange_resetsEverything() {
        detector.starvedSlices(Map.of(1, new SliceSnapshot(336, 5L)), 0, 2, T0);

        assertTrue(detector.starvedSlices(Map.of(1, new SliceSnapshot(336, 5L)), 0, 3,
                T0.plus(THRESHOLD).plusSeconds(1)).isEmpty());
    }

    /** A share that empties and later refills is judged afresh, not by its old sighting. */
    @Test
    public void emptiedShare_isForgotten() {
        detector.starvedSlices(Map.of(1, new SliceSnapshot(336, 5L)), 0, 2, T0);
        detector.starvedSlices(Map.of(), 0, 2, T0.plusSeconds(10));

        assertTrue(detector.starvedSlices(Map.of(1, new SliceSnapshot(336, 5L)), 0, 2,
                T0.plus(THRESHOLD).plusSeconds(1)).isEmpty());
    }

    /** Several starved shares are all reported, lowest slice first. */
    @Test
    public void severalStarvedShares_areAllReported() {
        final Map<Integer, SliceSnapshot> stuck = Map.of(
                2, new SliceSnapshot(7, 9L), 1, new SliceSnapshot(3, 4L));
        detector.starvedSlices(stuck, 0, 3, T0);

        assertEquals(List.of(1, 2),
                detector.starvedSlices(stuck, 0, 3, T0.plus(THRESHOLD).plusSeconds(1)));
    }

    /**
     * Once a share is taken over, draining it changes its count — which is this server's own
     * progress, not the owner's. It must stay reported until it empties, or a large share would be
     * taken over one batch per threshold period.
     */
    @Test
    public void takenOverShare_staysReportedWhileItDrains() {
        detector.starvedSlices(Map.of(1, new SliceSnapshot(5000, 5L)), 0, 2, T0);
        detector.starvedSlices(Map.of(1, new SliceSnapshot(5000, 5L)), 0, 2,
                T0.plus(THRESHOLD).plusSeconds(1));

        assertEquals("draining it ourselves is not the owner coming back", List.of(1),
                detector.starvedSlices(Map.of(1, new SliceSnapshot(3000, 4001L)), 0, 2,
                        T0.plus(THRESHOLD).plusSeconds(2)));
    }

    /** A taken-over share that empties is released; if it refills it is judged afresh. */
    @Test
    public void takenOverShare_isReleasedOnceEmpty() {
        detector.starvedSlices(Map.of(1, new SliceSnapshot(10, 5L)), 0, 2, T0);
        detector.starvedSlices(Map.of(1, new SliceSnapshot(10, 5L)), 0, 2,
                T0.plus(THRESHOLD).plusSeconds(1));
        detector.starvedSlices(Map.of(), 0, 2, T0.plus(THRESHOLD).plusSeconds(2));

        assertTrue(detector.starvedSlices(Map.of(1, new SliceSnapshot(4, 900L)), 0, 2,
                T0.plus(THRESHOLD).plusSeconds(3)).isEmpty());
    }
}
