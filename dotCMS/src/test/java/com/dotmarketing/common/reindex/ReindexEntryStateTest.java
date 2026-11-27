package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link ReindexEntry#isParked()} (#37270).
 *
 * <p>A journal row's {@code priority} carries its band (ASAP 0, NORMAL 100, STRUCTURE 200,
 * REINDEX 300) plus one per failed attempt. When the retries run out the row is parked at
 * {@code ERROR (400)} plus the priority it had at its last attempt, and the dequeue never reads it
 * again. "Parked" is what the operator's clear deletes and what a retry requeues, so the boundary
 * between a row still being retried and a parked one has to be decided in one place.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false -Dtest=ReindexEntryStateTest
 * </pre>
 */
public class ReindexEntryStateTest {

    /**
     * Given rows that are fresh or still being retried, in every band, including the last value
     * below the parking threshold
     * Then none of them is parked.
     */
    @Test
    public void isParked_falseBelowError() {
        for (final int priority : new int[]{0, 1, 100, 101, 300, 301, 399}) {
            assertFalse("priority " + priority + " is not parked", entry(priority).isParked());
        }
    }

    /**
     * Given rows at or above {@code ERROR (400)}: the threshold itself and the values a save
     * (505) and a reindex entry (705) take when they park
     * Then all of them are parked.
     */
    @Test
    public void isParked_trueFromError() {
        for (final int priority : new int[]{400, 401, 505, 705}) {
            assertTrue("priority " + priority + " is parked", entry(priority).isParked());
        }
    }

    /**
     * The attempt count is still the last two digits of the priority; adding the parked state
     * must not change it.
     */
    @Test
    public void errorCount_unchanged() {
        assertEquals(0, entry(300).errorCount());
        assertEquals(5, entry(305).errorCount());
        assertEquals(5, entry(705).errorCount());
    }

    private static ReindexEntry entry(final int priority) {
        return ReindexEntry.builder().id(1L).identToIndex("id-1").priority(priority).build();
    }
}
