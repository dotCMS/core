package com.dotcms.cache.lettuce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.metrics.binders.CacheMetrics;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vavr.Lazy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

/** Redis statistics must collect all region counts in one cluster-scoped keyspace pass. */
class RedisCacheStatsTest {

    @Mock
    private RedisClient<String, Object> client;
    private AutoCloseable mocks;
    private RedisCache cache;

    /** Supplies multiple cursor batches, including dotted and overlapping group identities. */
    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        cache = new RedisCache(Lazy.of(() -> client));
        when(client.getMembers("GROUP_KEY")).thenReturn(Set.of("foo", "foo.bar", "other", "empty"));
        doAnswer(invocation -> {
            final String prefix = ((String) invocation.getArgument(0)).replaceFirst("\\*$", "");
            final Consumer<Collection<String>> consumer = invocation.getArgument(2);
            for (final List<String> batch : List.of(
                    List.of("PREFIX_KEY.foo.first.", "PREFIX_KEY.foo.bar.nested."),
                    List.of("PREFIX_KEY.other.second.", "PREFIX_KEY.foobar.not-foo."))) {
                consumer.accept(batch.stream().filter(key -> key.startsWith(prefix)).collect(Collectors.toList()));
            }
            return null;
        }).when(client).scanKeys(anyString(), anyInt(), any());
    }

    /** Releases Mockito state after each isolated collection. */
    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    /** All counts retain existing prefix semantics without a full Redis SCAN per group. */
    @Test
    void stats_countAllGroupsWithOneScan() {
        assertEquals(Map.of("dotCMS: foo", "2", "dotCMS: foo.bar", "1",
                "dotCMS: other", "1", "dotCMS: empty", "0"), sizes(cache.getStats()));
        verify(client).scanKeys(eq("PREFIX_KEY.*"), anyInt(), any());
        verify(client, org.mockito.Mockito.times(1)).scanKeys(anyString(), anyInt(), any());
    }

    /** Empty providers keep their placeholder row without accessing the keyspace. */
    @Test
    void stats_noGroupsDoesNotScan() {
        when(client.getMembers("GROUP_KEY")).thenReturn(Set.of());

        assertEquals(Map.of("n/a", "0"), sizes(cache.getStats()));
        verify(client, never()).scanKeys(anyString(), anyInt(), any());
    }

    /** An unavailable group registry still leaves a visible provider rather than throwing. */
    @Test
    void stats_groupLookupFailureDoesNotScan() {
        when(client.getMembers("GROUP_KEY")).thenThrow(new IllegalStateException("unavailable"));

        final CacheProviderStats stats = cache.getStats();
        assertEquals(Map.of("n/a", "0"), sizes(stats));
        assertUnavailableMetrics(stats);
        verify(client, never()).scanKeys(anyString(), anyInt(), any());
    }

    /** A failed scan preserves all group rows and the existing zero-count fallback. */
    @Test
    void stats_scanFailurePreservesProviderRows() {
        doThrow(new IllegalStateException("unavailable"))
                .when(client).scanKeys(anyString(), anyInt(), any());

        final CacheProviderStats stats = cache.getStats();
        assertEquals(Map.of("dotCMS: foo", "0", "dotCMS: foo.bar", "0",
                "dotCMS: other", "0", "dotCMS: empty", "0"), sizes(stats));
        assertUnavailableMetrics(stats);
        verify(client, org.mockito.Mockito.times(1)).scanKeys(anyString(), anyInt(), any());
    }

    /** Internal collection failures stay visible as unknown rather than healthy empty totals. */
    private void assertUnavailableMetrics(final CacheProviderStats stats) {
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

    /** Reads the public stats values without widening production visibility. */
    private Map<String, String> sizes(final CacheProviderStats stats) {
        return stats.getStats().stream().collect(Collectors.toMap(
                region -> region.getStatValue(CacheStats.REGION),
                region -> region.getStatValue(CacheStats.REGION_SIZE)));
    }
}
