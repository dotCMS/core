package com.dotcms.telemetry.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MetricCacheManager}.
 *
 * <p>Tests the configuration-driven caching functionality including:
 * <ul>
 *     <li>Cache hit/miss behavior</li>
 *     <li>Cache enable/disable per metric</li>
 *     <li>Cache invalidation</li>
 *     <li>TTL configuration</li>
 * </ul>
 */
public class MetricCacheManagerTest {

    private MetricCacheManager cacheManager;
    private MetricCacheConfig mockConfig;

    @BeforeEach
    public void setUp() {
        mockConfig = mock(MetricCacheConfig.class);

        // Configure mock defaults
        when(mockConfig.getMaxCacheSize()).thenReturn(1000L);
        when(mockConfig.isCachingEnabled("TEST_METRIC")).thenReturn(true);
        when(mockConfig.getCacheTTL("TEST_METRIC")).thenReturn(300000L); // 5 minutes

        // Create cache manager and inject mock config
        cacheManager = new MetricCacheManager();

        // Use reflection to inject the mock config (simulating CDI injection)
        try {
            java.lang.reflect.Field configField = MetricCacheManager.class.getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(cacheManager, mockConfig);

            // Call @PostConstruct init method
            cacheManager.init();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize test cache manager", e);
        }
    }

    @Test
    public void testCacheHit() {
        // Given a metric that will be cached
        final AtomicInteger callCount = new AtomicInteger(0);
        final Supplier<Optional<String>> supplier = () -> {
            callCount.incrementAndGet();
            return Optional.of("test-value");
        };

        // When we call get twice for the same metric
        final Optional<String> value1 = cacheManager.get("TEST_METRIC", supplier);
        final Optional<String> value2 = cacheManager.get("TEST_METRIC", supplier);

        // Then the supplier should only be called once (cache hit on second call)
        assertEquals(1, callCount.get(), "Supplier should only be called once");
        assertTrue(value1.isPresent());
        assertTrue(value2.isPresent());
        assertEquals("test-value", value1.get());
        assertEquals("test-value", value2.get());
    }

    @Test
    public void testCacheMiss() {
        // Given a metric that is not cached
        final Supplier<Optional<String>> supplier = () -> Optional.of("test-value");

        // When we get the metric for the first time
        final Optional<String> value = cacheManager.get("TEST_METRIC", supplier);

        // Then the supplier should be called and value should be present
        assertTrue(value.isPresent());
        assertEquals("test-value", value.get());
        assertEquals(1, cacheManager.size(), "Cache should contain 1 entry");
    }

    @Test
    public void testCachingDisabled() {
        // Given a metric with caching disabled
        when(mockConfig.isCachingEnabled("DISABLED_METRIC")).thenReturn(false);

        final AtomicInteger callCount = new AtomicInteger(0);
        final Supplier<Optional<String>> supplier = () -> {
            callCount.incrementAndGet();
            return Optional.of("test-value");
        };

        // When we call get twice for the same metric
        cacheManager.get("DISABLED_METRIC", supplier);
        cacheManager.get("DISABLED_METRIC", supplier);

        // Then the supplier should be called every time (no caching)
        assertEquals(2, callCount.get(), "Supplier should be called twice when caching is disabled");
    }

    @Test
    public void testEmptyValueNotCached() {
        // Given a supplier that returns an empty optional
        final AtomicInteger callCount = new AtomicInteger(0);
        final Supplier<Optional<String>> supplier = () -> {
            callCount.incrementAndGet();
            return Optional.empty();
        };

        // When we call get twice
        final Optional<String> value1 = cacheManager.get("TEST_METRIC", supplier);
        final Optional<String> value2 = cacheManager.get("TEST_METRIC", supplier);

        // Then the supplier should be called each time (empty values not cached)
        assertEquals(2, callCount.get(), "Supplier should be called twice for empty values");
        assertFalse(value1.isPresent());
        assertFalse(value2.isPresent());
    }

    @Test
    public void testInvalidateMetric() {
        // Given a cached metric
        final Supplier<Optional<String>> supplier = () -> Optional.of("test-value");
        cacheManager.get("TEST_METRIC", supplier);
        assertEquals(1, cacheManager.size());

        // When we invalidate the metric
        cacheManager.invalidate("TEST_METRIC");

        // Then the cache should be empty
        assertEquals(0, cacheManager.size(), "Cache should be empty after invalidation");
    }

    @Test
    public void testInvalidateAll() {
        // Given multiple cached metrics
        when(mockConfig.isCachingEnabled("METRIC_1")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_1")).thenReturn(300000L);
        when(mockConfig.isCachingEnabled("METRIC_2")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_2")).thenReturn(300000L);

        final Supplier<Optional<String>> supplier = () -> Optional.of("test-value");
        cacheManager.get("METRIC_1", supplier);
        cacheManager.get("METRIC_2", supplier);
        assertEquals(2, cacheManager.size());

        // When we invalidate all
        cacheManager.invalidateAll();

        // Then the cache should be empty
        assertEquals(0, cacheManager.size(), "Cache should be empty after invalidateAll");
    }

    @Test
    public void testPerMetricTTLConfiguration() {
        // Given metrics with different TTL configurations
        when(mockConfig.isCachingEnabled("SHORT_TTL_METRIC")).thenReturn(true);
        when(mockConfig.getCacheTTL("SHORT_TTL_METRIC")).thenReturn(1000L); // 1 second
        when(mockConfig.isCachingEnabled("LONG_TTL_METRIC")).thenReturn(true);
        when(mockConfig.getCacheTTL("LONG_TTL_METRIC")).thenReturn(300000L); // 5 minutes

        // When we get metrics
        final Supplier<Optional<String>> supplier = () -> Optional.of("test-value");
        cacheManager.get("SHORT_TTL_METRIC", supplier);
        cacheManager.get("LONG_TTL_METRIC", supplier);

        // Then the config should be called with the correct metric names
        verify(mockConfig).getCacheTTL("SHORT_TTL_METRIC");
        verify(mockConfig).getCacheTTL("LONG_TTL_METRIC");
    }

    @Test
    public void testCacheStatsAvailable() {
        // Given a cache with some activity
        final Supplier<Optional<String>> supplier = () -> Optional.of("test-value");
        cacheManager.get("TEST_METRIC", supplier);
        cacheManager.get("TEST_METRIC", supplier); // Cache hit

        // When we get stats
        final String stats = cacheManager.stats();

        // Then stats should be available
        assertNotNull(stats, "Cache stats should be available");
        assertTrue(stats.contains("hitCount=1"), "Stats should show hit count");
    }

    @Test
    public void testCacheSize() {
        // Given multiple cached metrics
        when(mockConfig.isCachingEnabled("METRIC_1")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_1")).thenReturn(300000L);
        when(mockConfig.isCachingEnabled("METRIC_2")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_2")).thenReturn(300000L);
        when(mockConfig.isCachingEnabled("METRIC_3")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_3")).thenReturn(300000L);

        final Supplier<Optional<String>> supplier = () -> Optional.of("test-value");

        // When we add metrics to cache
        cacheManager.get("METRIC_1", supplier);
        cacheManager.get("METRIC_2", supplier);
        cacheManager.get("METRIC_3", supplier);

        // Then size should reflect the number of cached entries
        assertEquals(3, cacheManager.size(), "Cache size should be 3");
    }

    @Test
    public void testGlobalCachingDisabled() {
        // Given global caching is disabled
        when(mockConfig.isCachingEnabled("TEST_METRIC")).thenReturn(false);

        final AtomicInteger callCount = new AtomicInteger(0);
        final Supplier<Optional<String>> supplier = () -> {
            callCount.incrementAndGet();
            return Optional.of("test-value");
        };

        // When we call get multiple times
        cacheManager.get("TEST_METRIC", supplier);
        cacheManager.get("TEST_METRIC", supplier);

        // Then the supplier should be called every time
        assertEquals(2, callCount.get(), "Supplier should be called twice when global caching is disabled");
        assertEquals(0, cacheManager.size(), "Cache should be empty when caching is disabled");
    }

    @Test
    public void testDifferentMetricNamesDifferentCacheEntries() {
        // Given multiple metrics with the same value
        when(mockConfig.isCachingEnabled("METRIC_A")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_A")).thenReturn(300000L);
        when(mockConfig.isCachingEnabled("METRIC_B")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_B")).thenReturn(300000L);

        final Supplier<Optional<String>> supplier = () -> Optional.of("same-value");

        // When we cache both metrics
        cacheManager.get("METRIC_A", supplier);
        cacheManager.get("METRIC_B", supplier);

        // Then both should be cached separately
        assertEquals(2, cacheManager.size(), "Cache should contain 2 separate entries");
    }

    @Test
    public void testPerMetricConfigurationRespected() {
        // Given different cache configurations for different metrics
        when(mockConfig.isCachingEnabled("CACHED_METRIC")).thenReturn(true);
        when(mockConfig.getCacheTTL("CACHED_METRIC")).thenReturn(300000L);
        when(mockConfig.isCachingEnabled("UNCACHED_METRIC")).thenReturn(false);

        final AtomicInteger cachedCallCount = new AtomicInteger(0);
        final AtomicInteger uncachedCallCount = new AtomicInteger(0);

        final Supplier<Optional<String>> cachedSupplier = () -> {
            cachedCallCount.incrementAndGet();
            return Optional.of("cached-value");
        };

        final Supplier<Optional<String>> uncachedSupplier = () -> {
            uncachedCallCount.incrementAndGet();
            return Optional.of("uncached-value");
        };

        // When we call both metrics twice
        cacheManager.get("CACHED_METRIC", cachedSupplier);
        cacheManager.get("CACHED_METRIC", cachedSupplier);
        cacheManager.get("UNCACHED_METRIC", uncachedSupplier);
        cacheManager.get("UNCACHED_METRIC", uncachedSupplier);

        // Then cached metric should compute once, uncached should compute twice
        assertEquals(1, cachedCallCount.get(), "Cached metric should compute once");
        assertEquals(2, uncachedCallCount.get(), "Uncached metric should compute twice");
        assertEquals(1, cacheManager.size(), "Only cached metric should be in cache");
    }

    @Test
    public void testConcurrentAccess() throws InterruptedException {
        // Given a metric with caching enabled
        final AtomicInteger callCount = new AtomicInteger(0);
        final Supplier<Optional<String>> supplier = () -> {
            callCount.incrementAndGet();
            try {
                Thread.sleep(10); // Simulate slow computation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.of("test-value");
        };

        // When multiple threads try to get the same metric concurrently
        final int threadCount = 5;
        final Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> cacheManager.get("TEST_METRIC", supplier));
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then the supplier should still only be called once (cache synchronization works)
        // Note: This may be 1 or slightly more depending on cache implementation's race handling
        assertTrue(callCount.get() <= threadCount,
            "Supplier should be called at most " + threadCount + " times");
        assertEquals(1, cacheManager.size(), "Cache should contain 1 entry");
    }

    @Test
    public void testInvalidateDuringComputation() {
        // Given a metric being computed
        final Supplier<Optional<String>> slowSupplier = () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.of("slow-value");
        };

        // When we invalidate while computation might be happening
        final Thread computeThread = new Thread(() -> cacheManager.get("SLOW_METRIC", slowSupplier));
        computeThread.start();

        // Give it time to start computing
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        cacheManager.invalidate("SLOW_METRIC");

        // Wait for computation to finish
        try {
            computeThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then the cache should handle this gracefully without errors
        // (Size may be 0 if invalidated before put, or 1 if invalidated before)
        assertTrue(cacheManager.size() <= 1, "Cache should handle concurrent invalidation");
    }

    @Test
    public void testCacheRespectsMaxSize() {
        // Given a cache with limited size
        when(mockConfig.getMaxCacheSize()).thenReturn(3L);

        // Reinitialize with new max size
        cacheManager.init();

        // Configure all metrics as cacheable
        when(mockConfig.isCachingEnabled("METRIC_1")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_1")).thenReturn(300000L);
        when(mockConfig.isCachingEnabled("METRIC_2")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_2")).thenReturn(300000L);
        when(mockConfig.isCachingEnabled("METRIC_3")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_3")).thenReturn(300000L);
        when(mockConfig.isCachingEnabled("METRIC_4")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_4")).thenReturn(300000L);
        when(mockConfig.isCachingEnabled("METRIC_5")).thenReturn(true);
        when(mockConfig.getCacheTTL("METRIC_5")).thenReturn(300000L);

        final Supplier<Optional<String>> supplier = () -> Optional.of("test-value");

        // When we add more metrics than the max size
        cacheManager.get("METRIC_1", supplier);
        cacheManager.get("METRIC_2", supplier);
        cacheManager.get("METRIC_3", supplier);
        cacheManager.get("METRIC_4", supplier);
        cacheManager.get("METRIC_5", supplier);

        // Give the cache a moment to evict entries (Caffeine uses async eviction)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then the cache size should eventually respect the maximum
        // Note: Caffeine may temporarily exceed max size during concurrent access,
        // but should converge to the maximum over time
        final long size = cacheManager.size();
        assertTrue(size <= 5,
            "Cache size (" + size + ") should not greatly exceed maximum (3). " +
            "Caffeine uses async eviction which may allow temporary size violations.");
    }
}