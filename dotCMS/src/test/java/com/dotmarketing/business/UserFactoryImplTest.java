package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@code @VisibleForTesting} DB-lookup counter added to
 * {@link UserFactoryImpl#loadUserById(String)} (issue #37186). The counter is the only way
 * SC-001 ("N distinct authors -> exactly N DB lookups") is observable in a test today — no
 * query-counting harness existed in this codebase before this change.
 *
 * <p>This test exercises the counter's own arithmetic in isolation (reset, increment), not
 * {@code loadUserById} itself — proving the counter actually increments on a real cache miss is
 * the job of the {@code dotcms-integration} test, which needs a live database.</p>
 */
public class UserFactoryImplTest {

    @Before
    public void resetCounter() {
        UserFactoryImpl.resetDbLookupCountForTesting();
    }

    @Test
    public void dbLookupCounter_startsAtZeroAfterReset() {
        assertEquals(0L, UserFactoryImpl.getDbLookupCountForTesting());
    }

    @Test
    public void dbLookupCounter_incrementsPerCall() {
        UserFactoryImpl.incrementDbLookupCount();
        assertEquals(1L, UserFactoryImpl.getDbLookupCountForTesting());

        UserFactoryImpl.incrementDbLookupCount();
        UserFactoryImpl.incrementDbLookupCount();
        assertEquals(3L, UserFactoryImpl.getDbLookupCountForTesting());
    }

    @Test
    public void resetDbLookupCountForTesting_zeroesAnAlreadyIncrementedCounter() {
        UserFactoryImpl.incrementDbLookupCount();
        UserFactoryImpl.incrementDbLookupCount();
        assertEquals(2L, UserFactoryImpl.getDbLookupCountForTesting());

        UserFactoryImpl.resetDbLookupCountForTesting();
        assertEquals(0L, UserFactoryImpl.getDbLookupCountForTesting());
    }
}
