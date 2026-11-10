package com.dotcms.metrics.binders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.metrics.MetricsConfig;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Cache region gauges must refresh real provider snapshots without a full scan per gauge. */
class CacheMetricsTest {

    private final DotCacheAdministrator administrator = mock(DotCacheAdministrator.class);
    private final AtomicLong nanos = new AtomicLong();
    private final AtomicReference<List<CacheProviderStats>> snapshots = new AtomicReference<>();
    // Keep startup stats alive so weak-reference NaNs cannot conceal the stale-value regression.
    private List<CacheProviderStats> initialSnapshot;
    private SimpleMeterRegistry registry;
    private MockedStatic<CacheLocator> locator;

    /** Uses fresh snapshot objects, raw names needing label normalization, and a deterministic clock. */
    @BeforeEach
    void setUp() {
        final Clock clock = mock(Clock.class);
        when(clock.monotonicTime()).thenAnswer(ignored -> nanos.get());
        when(clock.wallTime()).thenReturn(1L);
        registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
        initialSnapshot = List.of(snapshot(1));
        snapshots.set(initialSnapshot);
        when(administrator.getCacheStatsList()).thenAnswer(ignored -> snapshots.get());
        locator = mockStatic(CacheLocator.class);
        locator.when(CacheLocator::getCacheAdministrator).thenReturn(administrator);
        new CacheMetrics().bindTo(registry);
    }

    /** Removes meters and static mocks between tests. */
    @AfterEach
    void tearDown() {
        registry.close();
        locator.close();
    }

    /** All nine region gauges must use the replacement snapshot after the cache TTL. */
    @Test
    void regionGauges_refreshAllValuesFromCurrentSnapshot() {
        assertValues(1);
        snapshots.set(List.of(snapshot(2)));
        expireSnapshot();

        assertValues(2);
        verify(administrator, times(2)).getCacheStatsList();
    }

    /** Re-reading all gauges must not collect all providers and regions once per value. */
    @Test
    void regionGauges_shareOneSnapshotWithinCacheTtl() {
        assertValues(1);
        assertValues(1);

        verify(administrator, times(1)).getCacheStatsList();
    }

    /** Regions with the same raw name must remain isolated by provider identity. */
    @Test
    void regionLookup_matchesBothProviderAndRegion() {
        final CacheProviderStats otherProvider = new CacheProviderStats(new CacheStats(), "Other-Provider");
        otherProvider.addStatRecord(new CacheStats()
                .addStat(CacheStats.REGION, "region.foo")
                .addStat(CacheStats.REGION_SIZE, 99));
        snapshots.set(List.of(otherProvider, snapshot(2)));
        expireSnapshot();

        assertEquals(6.0, gauge("size"), 0.001);
    }

    /** A removed region is unknown, not its old value or a fabricated idle zero. */
    @Test
    void disappearedRegion_reportsUnknown() {
        snapshots.set(List.of());
        expireSnapshot();

        assertTrue(Double.isNaN(gauge("size")));
        assertTrue(Double.isNaN(gauge("hits")));
        verify(administrator, times(2)).getCacheStatsList();
    }

    /** A failed collection is bounded, visible as unknown, and can recover after the TTL. */
    @Test
    void collectionFailure_reportsUnknownAndRecoversWithoutRepeatedScans() {
        when(administrator.getCacheStatsList()).thenThrow(new IllegalStateException("unavailable"));
        expireSnapshot();

        assertTrue(Double.isNaN(gauge("size")));
        assertTrue(Double.isNaN(gauge("hits")));
        verify(administrator, times(2)).getCacheStatsList();

        org.mockito.Mockito.doAnswer(ignored -> snapshots.get())
                .when(administrator).getCacheStatsList();
        snapshots.set(List.of(snapshot(2)));
        expireSnapshot();
        assertValues(2);
        verify(administrator, times(3)).getCacheStatsList();
    }

    /** Advances the registry clock beyond the configured cache TTL without sleeping. */
    private void expireSnapshot() {
        nanos.addAndGet(TimeUnit.SECONDS.toNanos(Math.max(1, MetricsConfig.METRIC_CACHE_TTL_SECONDS)) + 1);
    }

    /** Checks parsing and freshness for every existing region metric. */
    private void assertValues(final int scale) {
        final Map<String, Double> values = Map.of(
                "size", 3.0,
                "hits", 7.0,
                "hit_rate", 25.0,
                "configured_size", 100.0,
                "loads", 9.0,
                "avg_load_time_ms", 1.5,
                "evictions", 2.0,
                "memory_bytes", 1024.0,
                "memory_per_object_bytes", 64.0);
        values.forEach((name, expected) -> assertEquals(expected * scale, gauge(name), 0.001, name));
    }

    /** Reads a gauge using the existing public provider/region label format. */
    private double gauge(final String name) {
        return registry.get("dotcms.cache.region." + name)
                .tag("provider", "provider_a").tag("region", "region_foo").gauge().value();
    }

    /** Creates a provider response rather than mutating the original startup snapshot in place. */
    private CacheProviderStats snapshot(final int scale) {
        final CacheStats region = new CacheStats()
                .addStat(CacheStats.REGION, "region.foo")
                .addStat(CacheStats.REGION_SIZE, 3L * scale)
                .addStat(CacheStats.REGION_HITS, 7L * scale)
                .addStat(CacheStats.REGION_HIT_RATE, 25 * scale + "%")
                .addStat(CacheStats.REGION_CONFIGURED_SIZE, "size:" + 100 * scale + " | ttl:300s")
                .addStat(CacheStats.REGION_LOAD, 9L * scale)
                .addStat(CacheStats.REGION_AVG_LOAD_TIME, 1.5 * scale + " ms")
                .addStat(CacheStats.REGION_EVICTIONS, 2L * scale)
                .addStat(CacheStats.REGION_MEM_TOTAL, scale + " KB")
                .addStat(CacheStats.REGION_MEM_PER_OBJECT, 64 * scale + " bytes");
        final CacheProviderStats provider = new CacheProviderStats(new CacheStats(), "Provider-A");
        provider.addStatRecord(region);
        return provider;
    }
}
