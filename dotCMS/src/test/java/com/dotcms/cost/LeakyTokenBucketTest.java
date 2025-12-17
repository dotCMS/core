package com.dotcms.cost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/**
 * Unit tests for {@link LeakyTokenBucket}. Tests the token bucket rate limiting implementation including token refill,
 * draining, and concurrent access scenarios.
 */
public class LeakyTokenBucketTest extends UnitTestBase {

    /**
     * Test: New bucket should allow requests Expected: allow() returns true when bucket has tokens
     */
    @Test
    public void test_allow_newBucket_shouldReturnTrue() {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 100, 1000);

        // When
        boolean allowed = bucket.allow();

        // Then
        assertTrue("New bucket should allow requests", allowed);
    }

    /**
     * Test: Disabled bucket should always allow requests Expected: allow() returns true even when empty
     */
    @Test
    public void test_allow_disabledBucket_shouldAlwaysReturnTrue() {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(false, 100, 1000);
        bucket.drainFromBucket(Long.MAX_VALUE); // Empty the bucket

        // When
        boolean allowed = bucket.allow();

        // Then
        assertTrue("Disabled bucket should always allow requests", allowed);
    }

    /**
     * Test: Bucket should report correct token count Expected: getTokenCount() returns value capped at maximum
     */
    @Test
    public void test_getTokenCount_shouldBeCappedAtMaximum() {
        // Given
        long maxSize = 500;
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 100, maxSize);

        // When - trigger refill
        bucket.allow();

        // Then
        long count = bucket.getTokenCount();
        assertTrue("Token count should not exceed maximum", count <= maxSize);
    }

    /**
     * Test: drainFromBucket should reduce token count Expected: Token count decreases by drain amount
     */
    @Test
    public void test_drainFromBucket_shouldReduceTokens() {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 100, 1000);
        bucket.allow(); // Initialize and refill
        long initialCount = bucket.getTokenCount();

        // When
        bucket.drainFromBucket(50);

        // Then
        long finalCount = bucket.getTokenCount();
        assertEquals("Token count should decrease by drain amount",
                initialCount - 50, finalCount);
    }

    /**
     * Test: drainFromBucket should not go below zero Expected: Token count stays at 0, not negative
     */
    @Test
    public void test_drainFromBucket_shouldNotGoBelowZero() {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 100, 1000);
        bucket.allow();

        // When - drain more than available
        bucket.drainFromBucket(Long.MAX_VALUE);

        // Then
        long count = bucket.getTokenCount();
        assertEquals("Token count should be 0, not negative", 0, count);
    }

    /**
     * Test: Bucket should deny requests when empty Expected: allow() returns false when no tokens available
     */
    @Test
    public void test_allow_emptyBucket_shouldReturnFalse() {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 1, 100);
        bucket.allow(); // Initialize
        bucket.drainFromBucket(Long.MAX_VALUE); // Empty the bucket

        // When
        boolean allowed = bucket.allow();

        // Then
        assertFalse("Empty bucket should deny requests", allowed);
    }

    /**
     * Test: Bucket should refill over time Expected: Token count increases after time passes
     */
    @Test
    public void test_allow_shouldRefillOverTime() throws InterruptedException {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 100, 1000);
        bucket.allow();
        bucket.drainFromBucket(500);
        long countAfterDrain = bucket.getTokenCount();

        // When - wait for refill (need > 1 second for integer division)
        Thread.sleep(1100);
        bucket.allow(); // Trigger refill

        // Then
        long countAfterRefill = bucket.getTokenCount();
        assertTrue("Tokens should be refilled after time passes",
                countAfterRefill > countAfterDrain);
    }

    /**
     * Test: Token count should not exceed maximum bucket size Expected: Even with long time passage, tokens capped at
     * max
     */
    @Test
    public void test_refill_shouldNotExceedMaximum() throws InterruptedException {
        // Given
        long maxSize = 200;
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 1000, maxSize);

        // When - wait and trigger multiple refills
        Thread.sleep(1100);
        bucket.allow();
        Thread.sleep(1100);
        bucket.allow();

        // Then
        long count = bucket.getTokenCount();
        assertTrue("Token count should not exceed maximum: " + count, count <= maxSize);
    }

    /**
     * Test: Multiple drains should accumulate Expected: Sequential drains reduce count correctly
     */
    @Test
    public void test_multipleDrains_shouldAccumulate() {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 100, 1000);
        bucket.allow();
        long initialCount = bucket.getTokenCount();

        // When
        bucket.drainFromBucket(10);
        bucket.drainFromBucket(20);
        bucket.drainFromBucket(30);

        // Then
        long finalCount = bucket.getTokenCount();
        assertEquals("Multiple drains should accumulate",
                initialCount - 60, finalCount);
    }

    /**
     * Test: Zero drain should not change token count Expected: Token count remains the same
     */
    @Test
    public void test_drainFromBucket_withZero_shouldNotChange() {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 100, 1000);
        bucket.allow();
        long initialCount = bucket.getTokenCount();

        // When
        bucket.drainFromBucket(0);

        // Then
        long finalCount = bucket.getTokenCount();
        assertEquals("Zero drain should not change count", initialCount, finalCount);
    }

    /**
     * Test: Concurrent access should not corrupt token count Expected: Final count is consistent (no negative values)
     */
    @Test
    public void test_concurrentAccess_shouldNotCorruptState() throws InterruptedException {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 1000, 10000);
        bucket.allow(); // Initialize

        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger allowCount = new AtomicInteger(0);

        // When - concurrent allow() and drainFromBucket() calls
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (bucket.allow()) {
                            allowCount.incrementAndGet();
                        }
                        bucket.drainFromBucket(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        long finalCount = bucket.getTokenCount();
        assertTrue("Token count should not be negative: " + finalCount, finalCount >= 0);
    }

    /**
     * Test: Rapid successive calls should not add tokens without time passing Expected: No extra tokens added for
     * sub-second calls
     */
    @Test
    public void test_rapidCalls_shouldNotAddTokensWithoutTimeElapsed() {
        // Given
        long refreshRate = 100;
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, refreshRate, 10000);

        // When - rapid successive calls without waiting
        bucket.allow();
        long countAfterFirst = bucket.getTokenCount();

        bucket.allow();
        long countAfterSecond = bucket.getTokenCount();

        // Then - with fix, no tokens should be added for sub-second calls
        assertEquals("Rapid calls should not add tokens",
                countAfterFirst, countAfterSecond);
    }

    /**
     * Test: Custom constructor parameters Expected: Bucket uses provided values
     */
    @Test
    public void test_constructor_shouldUseProvidedValues() {
        // Given
        boolean customEnabled = true;
        long customRefresh = 50;
        long customMax = 250;

        // When
        LeakyTokenBucket bucket = new LeakyTokenBucket(customEnabled, customRefresh, customMax);

        // Then
        assertEquals("Should use custom enabled", customEnabled, bucket.enabled);
        assertEquals("Should use custom refresh rate", customRefresh, bucket.refreshPerSecond);
        assertEquals("Should use custom max size", customMax, bucket.maximumBucketSize);
    }

    /**
     * Test: Bucket at exactly maximum capacity Expected: No additional tokens added when full
     */
    @Test
    public void test_allow_atMaxCapacity_shouldNotOverfill() throws InterruptedException {
        // Given
        long maxSize = 100;
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 1000, maxSize);

        // Fill the bucket
        Thread.sleep(1100);
        bucket.allow();

        // When - bucket should be at or near max
        long countBefore = bucket.getTokenCount();

        Thread.sleep(1100);
        bucket.allow();

        long countAfter = bucket.getTokenCount();

        // Then
        assertTrue("Count before should be at max: " + countBefore, countBefore <= maxSize);
        assertTrue("Count after should still be at max: " + countAfter, countAfter <= maxSize);
    }

    /**
     * Test: Drain exactly all tokens Expected: Bucket is empty but not negative
     */
    @Test
    public void test_drainFromBucket_exactAmount_shouldBeZero() {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 100, 1000);
        bucket.allow();
        long currentCount = bucket.getTokenCount();

        // When
        bucket.drainFromBucket(currentCount);

        // Then
        assertEquals("Draining exact amount should result in zero", 0, bucket.getTokenCount());
    }

    /**
     * Test: Enabled flag should control rate limiting behavior Expected: Enabled bucket limits, disabled bucket allows
     * all
     */
    @Test
    public void test_enabledFlag_controlsRateLimiting() {
        // Given
        LeakyTokenBucket enabledBucket = new LeakyTokenBucket(true, 100, 1000);
        LeakyTokenBucket disabledBucket = new LeakyTokenBucket(false, 100, 1000);

        // Drain both buckets
        enabledBucket.allow();
        disabledBucket.allow();
        enabledBucket.drainFromBucket(Long.MAX_VALUE);
        disabledBucket.drainFromBucket(Long.MAX_VALUE);

        // When
        boolean enabledAllowed = enabledBucket.allow();
        boolean disabledAllowed = disabledBucket.allow();

        // Then
        assertFalse("Enabled empty bucket should deny", enabledAllowed);
        assertTrue("Disabled empty bucket should allow", disabledAllowed);
    }

    /**
     * Test: First call initializes lastRefill timestamp Expected: Subsequent calls within same second don't add tokens
     */
    @Test
    public void test_firstCall_initializesTimestamp() {
        // Given
        LeakyTokenBucket bucket = new LeakyTokenBucket(true, 100, 1000);

        // When - first call
        bucket.allow();
        long countAfterFirst = bucket.getTokenCount();

        // Immediate second call
        bucket.allow();
        long countAfterSecond = bucket.getTokenCount();

        // Then - no tokens should be added between immediate calls
        assertEquals("Immediate calls should not add tokens",
                countAfterFirst, countAfterSecond);
    }
}
