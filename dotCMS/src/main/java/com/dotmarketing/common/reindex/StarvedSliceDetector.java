package com.dotmarketing.common.reindex;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recognises a share of the reindex journal that the server it belongs to is not draining.
 *
 * <p>{@link ReindexQueueFactory} splits the journal across the servers that pinged in the last few
 * minutes, each taking the rows where {@code MOD(id, servers)} equals its own position. The split
 * assumes every listed server is indexing. One that keeps pinging but does not index — a dead
 * worker, a stuck thread — leaves its share untouched for as long as it keeps pinging, and a full
 * reindex hangs part-way with no error and nothing in the log (issue #36482).</p>
 *
 * <p>A share is judged by <em>progress</em>, never by timestamps: its row count and lowest id are
 * sampled each time the server looks, and a share for which neither has changed for longer than the
 * threshold is reported as starved. {@code time_entered} cannot be used for this, because several
 * enqueue paths leave it at the column default (midnight of the day).</p>
 *
 * <p>Not thread-safe: it is only used from the single reindex thread.</p>
 */
final class StarvedSliceDetector {

    /** One share of the journal as seen in one sample: how many rows, and the lowest id among them. */
    record SliceSnapshot(long count, long minId) { }

    /** A share as last seen, and since when it has looked exactly like that. */
    private record Sighting(SliceSnapshot snapshot, Instant since) { }

    private final Duration threshold;
    private final Map<Integer, Sighting> sightings = new HashMap<>();
    /**
     * Shares already declared starved. Once this server starts draining one, its count changes
     * because of this server's own work, which says nothing about the owner — so a taken-over share
     * stays reported until it empties or the split changes.
     */
    private final Set<Integer> takenOver = new HashSet<>();
    private int observedSliceCount = -1;

    /**
     * @param threshold how long a share must stay unchanged before it is treated as abandoned
     */
    StarvedSliceDetector(final Duration threshold) {
        this.threshold = threshold;
    }

    /**
     * Records this sample and returns the shares that have not moved for longer than the threshold.
     *
     * @param snapshot   every non-empty share, keyed by slice number
     * @param mySlice    this server's own slice, which is never reported: the normal path drains it
     * @param sliceCount how many servers the journal is currently split across; when it changes,
     *                   every share is a different set of rows and all earlier sightings are dropped
     * @param now        the time of this sample
     * @return the starved slices, lowest first; empty when nothing is starved
     */
    List<Integer> starvedSlices(final Map<Integer, SliceSnapshot> snapshot, final int mySlice,
            final int sliceCount, final Instant now) {
        if (sliceCount != observedSliceCount) {
            sightings.clear();
            takenOver.clear();
            observedSliceCount = sliceCount;
        }
        // A share that emptied is forgotten, so if it refills it is judged afresh.
        sightings.keySet().retainAll(snapshot.keySet());
        takenOver.retainAll(snapshot.keySet());

        snapshot.forEach((slice, current) -> {
            final Sighting previous = sightings.get(slice);
            if (null == previous || !previous.snapshot().equals(current)) {
                sightings.put(slice, new Sighting(current, now));
            }
        });

        sightings.forEach((slice, sighting) -> {
            if (slice != mySlice
                    && Duration.between(sighting.since(), now).compareTo(threshold) > 0) {
                takenOver.add(slice);
            }
        });
        takenOver.remove(mySlice);
        return takenOver.stream().sorted().collect(Collectors.toList());
    }
}
