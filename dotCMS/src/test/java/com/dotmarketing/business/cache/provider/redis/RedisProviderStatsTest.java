package com.dotmarketing.business.cache.provider.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.metrics.binders.CacheMetrics;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/** Legacy Jedis statistics must not disguise failed discovery or key counts as an empty provider. */
class RedisProviderStatsTest {

    @Mock
    private JedisPool pool;
    @Mock
    private Jedis jedis;
    @Mock
    private Connection connection;
    private AutoCloseable mocks;
    private RedisProvider provider;

    /** Supplies two cursor pages and legacy lookup stubs without starting a Redis server. */
    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        provider = new RedisProvider();
        final Field field = RedisProvider.class.getDeclaredField("readPool");
        field.setAccessible(true);
        field.set(provider, pool);
        when(pool.getResource()).thenReturn(jedis);
        when(jedis.getClient()).thenReturn(connection);
        when(connection.getBulkReply()).thenReturn("used_memory:4096\r\n");
        when(jedis.info("memory")).thenReturn("used_memory:4096\r\n");
        when(jedis.keys("*")).thenReturn(Set.of("group;first", "group;second", "other;third"));
        when(jedis.keys("group;*")).thenReturn(Set.of("group;first", "group;second"));
        when(jedis.keys("other;*")).thenReturn(Set.of("other;third"));
        when(jedis.scan(eq("0"), any(ScanParams.class)))
                .thenReturn(new ScanResult<>("next", List.of("group;first")));
        when(jedis.scan(eq("next"), any(ScanParams.class)))
                .thenReturn(new ScanResult<>("0", List.of("group;second", "other;third",
                        "unrelated-key", ";empty-group")));
    }

    /** Releases Mockito state after each standalone collection. */
    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    /** Healthy collection borrows once, requests INFO explicitly, and counts groups while scanning. */
    @Test
    void stats_collectOnceWithExplicitCommands() {
        final CacheProviderStats stats = provider.getStats();
        assertTrue(stats.isAvailable());
        assertEquals(Map.of("group", "2", "other", "1"), sizes(stats));
        verify(pool).getResource();
        verify(jedis).info("memory");
        verify(jedis).scan(eq("0"), any(ScanParams.class));
        verify(jedis).scan(eq("next"), any(ScanParams.class));
        verify(jedis, never()).keys(any(String.class));
    }

    /** Redis memory reports can exceed the signed-int range without invalidating the collection. */
    @Test
    void stats_memoryReportSupportsLargeRedisInstances() {
        final long bytes = 3L * 1024 * 1024 * 1024;
        when(jedis.info("memory")).thenReturn("used_memory:" + bytes + "\r\n");

        final CacheProviderStats stats = provider.getStats();
        assertTrue(stats.isAvailable());
        assertTrue(stats.getStats().stream().anyMatch(row -> NumberFormat.getInstance().format(bytes)
                .equals(row.getStatValue(CacheStats.REGION_CONFIGURED_SIZE))));
    }

    /** Failed pool acquisition is unavailable rather than a successful empty provider. */
    @Test
    void stats_poolFailureIsUnavailable() {
        when(pool.getResource()).thenThrow(new IllegalStateException("unavailable"));

        assertUnavailable(provider.getStats());
    }

    /** Discovery errors must not be swallowed by the provider\u0027s legacy group-list fallback. */
    @Test
    void stats_groupDiscoveryFailureIsUnavailable() {
        when(jedis.keys("*")).thenThrow(new IllegalStateException("unavailable"));
        when(jedis.scan(eq("0"), any(ScanParams.class))).thenThrow(new IllegalStateException("unavailable"));

        assertUnavailable(provider.getStats());
    }

    /** Errors after partial discovery must not turn the missing region counts into zeros. */
    @Test
    void stats_laterKeyCollectionFailureIsUnavailable() {
        when(jedis.keys("group;*")).thenThrow(new IllegalStateException("unavailable"));
        when(jedis.scan(eq("next"), any(ScanParams.class))).thenThrow(new IllegalStateException("unavailable"));

        assertUnavailable(provider.getStats());
    }

    /** A healthy empty keyspace remains distinct from any collection failure. */
    @Test
    void stats_emptyKeyspaceRemainsAvailable() {
        when(jedis.keys("*")).thenReturn(Set.of());
        when(jedis.scan(eq("0"), any(ScanParams.class))).thenReturn(new ScanResult<>("0", List.of()));

        final CacheProviderStats stats = provider.getStats();
        assertTrue(stats.isAvailable());
        assertTrue(stats.getStats().isEmpty());
    }

    /** Carries the real provider failure through the actual cache metrics snapshot loader. */
    private void assertUnavailable(final CacheProviderStats stats) {
        assertFalse(stats.isAvailable());
        final DotCacheAdministrator administrator = mock(DotCacheAdministrator.class);
        when(administrator.getCacheStatsList()).thenReturn(List.of(stats));
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

    /** Reads the public legacy row values without exposing production fields for tests. */
    private Map<String, String> sizes(final CacheProviderStats stats) {
        return stats.getStats().stream().collect(Collectors.toMap(
                row -> row.getStatValue(CacheStats.REGION), row -> row.getStatValue(CacheStats.REGION_SIZE)));
    }
}
