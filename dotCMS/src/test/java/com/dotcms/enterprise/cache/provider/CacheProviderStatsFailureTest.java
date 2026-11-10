package com.dotcms.enterprise.cache.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.metrics.binders.CacheMetrics;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.business.cache.provider.caffine.CaffineCache;
import com.dotmarketing.business.cache.provider.guava.GuavaCache;
import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Provider failures must remain in stats collections so metrics cannot publish incomplete totals. */
class CacheProviderStatsFailureTest {

    private final CaffineCache healthy = mock(CaffineCache.class);
    private final GuavaCache failing = mock(GuavaCache.class);
    private Cache<String, List<CacheProvider>> chains;
    private Map<String, List<CacheProvider>> previous;
    private CacheProviderAPIImpl api;

    /** Isolates configured chains without initializing providers or touching cache data. */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        final Field field = CacheProviderAPIImpl.class.getDeclaredField("configuredChainsPerRegion");
        field.setAccessible(true);
        chains = (Cache<String, List<CacheProvider>>) field.get(null);
        previous = new HashMap<>(chains.asMap());
        chains.invalidateAll();
        chains.put("metrics-failure-test", List.of(healthy, failing));
        when(healthy.getName()).thenReturn("Healthy Provider");
        when(failing.getName()).thenReturn("Failing Provider");
        final CacheProviderStats stats = new CacheProviderStats(new CacheStats(), "Healthy Provider");
        stats.addStatRecord(new CacheStats().addStat(CacheStats.REGION, "healthy")
                .addStat(CacheStats.REGION_SIZE, 7));
        when(healthy.getStats()).thenReturn(stats);
        api = new CacheProviderAPIImpl();
    }

    /** Restores the shared provider registry after every test. */
    @AfterEach
    void tearDown() {
        chains.invalidateAll();
        chains.putAll(previous);
    }

    /** An exception must retain a failed provider instead of quietly understating totals. */
    @Test
    void stats_throwingProviderRemainsUnavailable() {
        when(failing.getStats()).thenThrow(new IllegalStateException("expected test failure"));

        assertFailedProviderPreserved();
    }

    /** A null result is incomplete data, not proof that the provider has no regions. */
    @Test
    void stats_nullProviderResultRemainsUnavailable() {
        when(failing.getStats()).thenReturn(null);

        assertFailedProviderPreserved();
    }

    /** Legitimately empty providers keep their successful collection state. */
    @Test
    void stats_emptyProviderRemainsAvailable() {
        when(failing.getStats()).thenReturn(new CacheProviderStats(new CacheStats(), "Failing Provider"));

        final List<CacheProviderStats> stats = api.getStats();
        assertEquals(2, stats.size());
        assertTrue(stats.stream().allMatch(CacheProviderStats::isAvailable));
    }

    /** Checks the actual provider collector and the metrics binder together. */
    private void assertFailedProviderPreserved() {
        final List<CacheProviderStats> stats = api.getStats();
        assertEquals(2, stats.size());
        assertFalse(stats.stream().filter(provider -> provider.getProviderName().equals("Failing Provider"))
                .findFirst().orElseThrow().isAvailable());
        final DotCacheAdministrator administrator = mock(DotCacheAdministrator.class);
        when(administrator.getCacheStatsList()).thenReturn(stats);
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        try (MockedStatic<CacheLocator> locator = mockStatic(CacheLocator.class)) {
            locator.when(CacheLocator::getCacheAdministrator).thenReturn(administrator);
            new CacheMetrics().bindTo(registry);

            assertTrue(Double.isNaN(registry.get("dotcms.cache.size.total").gauge().value()));
            assertTrue(Double.isNaN(registry.get("dotcms.cache.providers.total").gauge().value()));
        } finally {
            registry.close();
        }
    }
}
