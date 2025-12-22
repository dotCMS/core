package com.dotcms.telemetry.cache;

import com.dotcms.cache.DynamicTTLCache;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * CDI service for managing metric caching.
 *
 * <p>Provides configuration-driven caching for telemetry metrics. All caching behavior
 * is controlled via properties (no annotations), enabling runtime-configurable caching
 * that works for both code-based and future configuration-based metrics.</p>
 *
 * <p>Key Features:</p>
 * <ul>
 *     <li>Configuration-driven: All caching controlled via properties</li>
 *     <li>Per-metric TTL: Different cache durations per metric</li>
 *     <li>Runtime-configurable: Change caching without code changes</li>
 *     <li>Works by metric name: Supports config-based metrics</li>
 *     <li>Caffeine-backed: Uses {@link DynamicTTLCache} for performance</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @Inject
 * private MetricCacheManager cacheManager;
 *
 * public Optional<MetricValue> getMetricValue(MetricType metricType) {
 *     return cacheManager.get(
 *         metricType.getName(),
 *         () -> computeMetricValue(metricType)
 *     );
 * }
 * }</pre>
 *
 * @see MetricCacheConfig
 * @see DynamicTTLCache
 */
@ApplicationScoped
public class MetricCacheManager {

    private DynamicTTLCache<String, Object> cache;

    @Inject
    private MetricCacheConfig config;

    /**
     * No-args constructor required for CDI proxy creation.
     */
    public MetricCacheManager() {
        // CDI will inject dependencies after construction
    }

    /**
     * Post-construct initialization method called by CDI after dependency injection.
     */
    @javax.annotation.PostConstruct
    public void init() {
        final long maxSize = config.getMaxCacheSize();
        this.cache = new DynamicTTLCache<>(maxSize);
        Logger.info(this, String.format("MetricCacheManager initialized with max size: %d", maxSize));
    }

    /**
     * Get metric value with caching support.
     *
     * <p>If caching is enabled for the metric (via configuration), checks the cache first.
     * On cache miss, computes the value using the supplier and caches it with the configured TTL.
     * If caching is disabled for the metric, always computes the value directly.</p>
     *
     * <p>Configuration is checked via:</p>
     * <ul>
     *     <li>{@code telemetry.cache.metric.{name}.enabled} - Per-metric enable/disable</li>
     *     <li>{@code telemetry.cache.metric.{name}.ttl.seconds} - Per-metric TTL</li>
     *     <li>{@code telemetry.cache.default.ttl.seconds} - Default TTL</li>
     * </ul>
     *
     * @param metricName the metric name (used as cache key)
     * @param supplier function to compute value if cache miss or caching disabled
     * @param <T> the value type
     * @return optional containing cached or computed value
     */
    public <T> Optional<T> get(final String metricName, final Supplier<Optional<T>> supplier) {
        // Check if caching enabled for this metric (from config)
        if (!config.isCachingEnabled(metricName)) {
            Logger.debug(this, () -> String.format("Cache disabled for metric: %s", metricName));
            return supplier.get(); // No cache, compute directly
        }

        // Try cache first
        final T cached = (T) cache.getIfPresent(metricName);
        if (cached != null) {
            Logger.debug(this, () -> String.format("Cache hit for metric: %s", metricName));
            return Optional.of(cached);
        }

        // Cache miss - compute and cache
        Logger.debug(this, () -> String.format("Cache miss for metric: %s", metricName));
        final Optional<T> value = supplier.get();

        if (value.isPresent()) {
            final long ttl = config.getCacheTTL(metricName); // From config
            cache.put(metricName, value.get(), ttl);
            Logger.debug(this, () -> String.format("Cached metric %s with TTL %dms", metricName, ttl));
        } else {
            Logger.debug(this, () -> String.format("Metric %s returned empty value, not caching", metricName));
        }

        return value;
    }

    /**
     * Invalidate cache for a specific metric.
     *
     * <p>Removes the cached value for the given metric name, forcing the next
     * request to recompute the value.</p>
     *
     * @param metricName the metric name to invalidate
     */
    public void invalidate(final String metricName) {
        cache.invalidate(metricName);
        Logger.debug(this, () -> String.format("Invalidated cache for metric: %s", metricName));
    }

    /**
     * Invalidate all cached metrics.
     *
     * <p>Clears the entire cache, forcing all subsequent requests to recompute values.
     * Useful after configuration changes or for testing.</p>
     */
    public void invalidateAll() {
        cache.invalidateAll();
        Logger.debug(this, "Invalidated all metric caches");
    }

    /**
     * Get current cache size.
     *
     * @return estimated number of entries in the cache
     */
    public long size() {
        return cache.estimatedSize();
    }

    /**
     * Get cache statistics.
     *
     * <p>Returns hit rate, miss rate, eviction count, etc.</p>
     *
     * @return cache statistics
     */
    public String stats() {
        return cache.stats().toString();
    }
}