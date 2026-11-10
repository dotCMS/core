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
import com.dotmarketing.business.cache.provider.caffine.CaffineCache;
import com.dotmarketing.util.UtilMethods;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    /** Provider gauges must use the same bounded snapshot, including empty providers. */
    @Test
    void aggregateGauges_shareRegionSnapshotAndRefresh() {
        assertAggregates(1, 1);
        assertValues(1);
        assertAggregates(1, 1);
        verify(administrator, times(1)).getCacheStatsList();

        snapshots.set(List.of(snapshot(2),
                new CacheProviderStats(new CacheStats(), "Empty-Provider")));
        expireSnapshot();

        assertAggregates(2, 2);
        assertValues(2);
        verify(administrator, times(2)).getCacheStatsList();
    }

    /** Caffeine loads already include hits; idle and unsupported counts must not invent a hit rate. */
    @Test
    void overallHitRate_usesNativeCaffeineRequestCounts() {
        for (final int[] counts : new int[][]{{1, 0}, {3, 1}, {0, 1}, {0, 0}}) {
            snapshots.set(List.of(caffeineSnapshot(counts[0], counts[1])));
            expireSnapshot();

            final double actual = aggregate("hit_rate.overall");
            final int requests = counts[0] + counts[1];
            if (requests == 0) {
                assertTrue(Double.isNaN(actual));
            } else {
                assertEquals(100.0 * counts[0] / requests, actual, 0.001);
            }
        }
    }

    /** Request-weighted aggregation must not average region percentages or infer misses from loads. */
    @Test
    void overallHitRate_weightsRegionsByNativeRequests() {
        final CacheProviderStats provider = caffeineSnapshot(9, 1);
        provider.addStatRecord(caffeineSnapshot(1, 89).getStats().get(0));
        snapshots.set(List.of(provider));
        expireSnapshot();

        assertEquals(10.0, aggregate("hit_rate.overall"), 0.001);
    }

    /** A provider without native hit/request counters cannot supply a reliable aggregate rate. */
    @Test
    void overallHitRate_withoutNativeCountersReportsUnknown() {
        assertTrue(Double.isNaN(aggregate("hit_rate.overall")));
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
        assertTrue(Double.isNaN(aggregate("size.total")));
        assertTrue(Double.isNaN(aggregate("providers.total")));
        verify(administrator, times(2)).getCacheStatsList();

        org.mockito.Mockito.doAnswer(ignored -> snapshots.get())
                .when(administrator).getCacheStatsList();
        snapshots.set(List.of(snapshot(2)));
        expireSnapshot();
        assertValues(2);
        verify(administrator, times(3)).getCacheStatsList();
    }

    /** H22's actual formatter uses single-letter units for both memory gauges. */
    @Test
    void memoryGauges_parseH22PrettyByteUnits() {
        for (final long bytes : new long[]{512, 1536, 1572864, 1610612736}) {
            final CacheStats region = new CacheStats()
                    .addStat(CacheStats.REGION, "region.foo")
                    .addStat(CacheStats.REGION_MEM_TOTAL_PRETTY, UtilMethods.prettyByteify(bytes))
                    .addStat(CacheStats.REGION_MEM_PER_OBJECT, UtilMethods.prettyByteify(bytes));
            final CacheProviderStats provider = new CacheProviderStats(new CacheStats(), "Provider-A");
            provider.addStatRecord(region);
            snapshots.set(List.of(provider));
            expireSnapshot();

            org.junit.jupiter.api.Assertions.assertAll(
                    () -> assertEquals(bytes, gauge("memory_bytes"), 0.001, "total: " + bytes),
                    () -> assertEquals(bytes, gauge("memory_per_object_bytes"), 0.001,
                            "per object: " + bytes));
        }
    }

    /** Existing long units, raw byte counts, and malformed-value fallbacks must stay compatible. */
    @Test
    void memoryGauges_preserveExistingByteFormats() {
        final Map<String, Double> formats = Map.of(
                "1.5 KB", 1536.0, "1.5 MB", 1572864.0, "1.5 GB", 1610612736.0,
                "512 bytes", 512.0, "512 B", 512.0, "512", 512.0, "invalid", 0.0);
        formats.forEach((value, expected) -> {
            final CacheStats region = new CacheStats()
                    .addStat(CacheStats.REGION, "region.foo")
                    .addStat(CacheStats.REGION_MEM_TOTAL, value)
                    .addStat(CacheStats.REGION_MEM_PER_OBJECT_PRETTY, value);
            final CacheProviderStats provider = new CacheProviderStats(new CacheStats(), "Provider-A");
            provider.addStatRecord(region);
            snapshots.set(List.of(provider));
            expireSnapshot();

            assertEquals(expected, gauge("memory_bytes"), 0.001, value);
            assertEquals(expected, gauge("memory_per_object_bytes"), 0.001, value);
        });
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

    /** Reads aggregate gauges without provider/region labels. */
    private double aggregate(final String name) {
        return registry.get("dotcms.cache." + name).gauge().value();
    }

    /** Checks the aggregate facts and evaluates the fifth gauge to catch redundant provider scans. */
    private void assertAggregates(final int scale, final int providers) {
        assertEquals(1.0, aggregate("regions.total"), 0.001);
        assertEquals(providers, aggregate("providers.total"), 0.001);
        assertEquals(7.0 * scale, aggregate("hits.total"), 0.001);
        assertEquals(3.0 * scale, aggregate("size.total"), 0.001);
        aggregate("hit_rate.overall");
    }

    /** Uses real Caffeine stats so tests also verify the provider publishes native counters. */
    private CacheProviderStats caffeineSnapshot(final int hits, final int misses) {
        final CaffineCache cache = new CaffineCache();
        final String group = "metrics-regression-" + UUID.randomUUID();
        try {
            cache.put(group, "present", "value");
            for (int i = 0; i < hits; i++) {
                cache.get(group, "present");
            }
            for (int i = 0; i < misses; i++) {
                cache.get(group, "missing");
            }
            final CacheProviderStats provider = new CacheProviderStats(new CacheStats(), cache.getName());
            cache.getStats().getStats().stream()
                    .filter(region -> group.equals(region.getStatValue(CacheStats.REGION)))
                    .forEach(provider::addStatRecord);
            assertEquals(1, provider.getStats().size());
            return provider;
        } finally {
            cache.remove(group);
        }
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

    /** Ordinary memory statistics retain their existing units and values. */
    @Test
    public void testMemoryStatisticsRemainAvailable() throws Exception {
        final CacheStats stats = new CacheStats()
                .addStat(CacheStats.REGION_MEM_TOTAL, 2048)
                .addStat(CacheStats.REGION_MEM_PER_OBJECT, "1 KB");

        assertEquals(2048, memoryMetric("getMemoryBytesValue", stats), 0);
        assertEquals(1024, memoryMetric("getMemoryPerObjectBytesValue", stats), 0);
    }

    /** Legacy H22 aliases must not turn serialized write statistics into memory gauges. */
    @Test
    public void testSerializedWriteAliasesAreNotReportedAsMemory() throws Exception {
        final CacheStats stats = new CacheStats()
                .addStat(CacheStats.REGION_BYTES_WRITTEN, "16 KB")
                .addStat(CacheStats.REGION_AVG_SERIALIZED_SIZE, "4 KB")
                .addStat(CacheStats.REGION_MEM_TOTAL_PRETTY, "16 KB")
                .addStat(CacheStats.REGION_MEM_PER_OBJECT, "4 KB");

        assertEquals(0, memoryMetric("getMemoryBytesValue", stats), 0);
        assertEquals(0, memoryMetric("getMemoryPerObjectBytesValue", stats), 0);
    }

    /** Exercises existing private readers without widening production visibility. */
    private double memoryMetric(final String name, final CacheStats stats) throws Exception {
        final Method method = CacheMetrics.class.getDeclaredMethod(name, CacheStats.class);
        method.setAccessible(true);
        return (double) method.invoke(new CacheMetrics(), stats);
    }
}
