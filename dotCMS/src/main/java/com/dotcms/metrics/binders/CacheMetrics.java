package com.dotcms.metrics.binders;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.metrics.MetricsConfig;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.business.cache.transport.CacheTransport;
import com.dotmarketing.business.cache.transport.NullTransport;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * Comprehensive metric binder for dotCMS cache-related metrics.
 * 
 * This binder provides detailed metrics for each cache region including:
 * - Cache hit rates and hit counts
 * - Cache sizes (current, configured, memory usage)
 * - Cache performance (load times, evictions)
 * - Provider-level aggregates
 * 
 * Metrics are tagged with cache provider name and region name for easy filtering.
 */
public class CacheMetrics implements MeterBinder {
    
    private static final String METRIC_PREFIX = "dotcms.cache";

    /** Raw identities keep sanitized metric labels out of snapshot lookups. */
    private record RegionKey(String provider, String region) {}

    /** Provider count includes empty providers, which cannot be inferred from the region map. */
    private record CacheSnapshot(int providers, Map<RegionKey, CacheStats> regions) {}
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
            
            // Cache administrator availability
            Gauge.builder(METRIC_PREFIX + ".administrator.available", this, cache -> cacheAdmin != null ? 1.0 : 0.0)
                .description("Whether the cache administrator is available")
                .register(registry);
            
            if (cacheAdmin != null) {
                // One bounded provider collection serves every aggregate and region gauge.
                final LoadingCache<Boolean, Optional<CacheSnapshot>> current = Caffeine.newBuilder()
                        .expireAfterWrite(Math.max(1, MetricsConfig.METRIC_CACHE_TTL_SECONDS), TimeUnit.SECONDS)
                        .ticker(registry.config().clock()::monotonicTime)
                        .build(ignored -> loadSnapshot(cacheAdmin));
                final Supplier<Optional<CacheSnapshot>> snapshots = () -> current.get(Boolean.TRUE);
                registerProviderLevelMetrics(registry, snapshots);
                registerRegionLevelMetrics(registry, snapshots);
                registerTransportMetrics(registry);
            }
            
            Logger.info(this, "Comprehensive cache metrics registered successfully");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register cache metrics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Register cluster cache-transport metrics (issue #36803): silent invalidation drops
     * and persistent rewire failures were previously invisible to monitoring.
     */
    private void registerTransportMetrics(MeterRegistry registry) {

        Gauge.builder(METRIC_PREFIX + ".transport.invalidations.dropped", this,
                        m -> activeTransport().map(t -> (double) t.getDroppedMessages()).orElse(0.0))
                .description("Cluster cache invalidations dropped, after the transport first came up,"
                        + " because it was not initialized")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + ".transport.invalidations.dropped.startup", this,
                        m -> activeTransport().map(t -> (double) t.getStartupDroppedMessages()).orElse(0.0))
                .description("Cluster cache invalidations dropped before the transport was first"
                        + " initialized (expected during startup, not an alerting signal)")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + ".transport.invalidations.failed", this,
                        m -> activeTransport().map(t -> (double) t.getFailedMessages()).orElse(0.0))
                .description("Cluster cache invalidations the transport attempted but failed to publish")
                .register(registry);

        // Reported raw, with no startup grace period: a gauge's job is to state the current
        // fact, and every node reads 0 here for the first seconds of its life. Alert on it with
        // a duration clause (Prometheus "for: 2m") rather than on the instantaneous value, or
        // alert on the cache-transport health check, which applies the grace period itself.
        Gauge.builder(METRIC_PREFIX + ".transport.initialized", this,
                        m -> activeTransport().map(t -> t.isInitialized() ? 1.0 : 0.0).orElse(1.0))
                .description("Whether the cluster cache transport is initialized (1) or dropping invalidations (0)")
                .register(registry);

        Gauge.builder(METRIC_PREFIX + ".transport.rewire.failures", this,
                        m -> (double) ClusterFactory.getRewireFailures())
                .description("Consecutive cluster cache-transport rewire failures (reset on success)")
                .register(registry);
    }

    /**
     * The cache transport, but only when it is one that actually carries cluster invalidations.
     *
     * Returns empty for {@link NullTransport} as well as for an unresolvable transport, so the
     * gauges above fall back to their "nothing to report" defaults. This matters for
     * {@code transport.initialized}: {@code NullTransport.isInitialized()} is false whenever it
     * has been shut down, so without this guard every node configured with no real transport
     * would publish {@code transport.initialized=0} and alert as if it were dropping
     * invalidations -- while {@code CacheTransportHealthCheck} reported the same node healthy.
     *
     * Uses the {@code DotCacheAdministrator.getTransport()} interface method rather than
     * casting {@code getImplementationObject()} to {@code ChainableCacheAdministratorImpl},
     * which throws {@code ClassCastException} for any other administrator implementation.
     */
    private Optional<CacheTransport> activeTransport() {
        try {
            final CacheTransport transport = CacheLocator.getCacheAdministrator().getTransport();
            return transport == null || transport instanceof NullTransport
                    ? Optional.empty()
                    : Optional.of(transport);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Register provider-level aggregate metrics.
     */
    private void registerProviderLevelMetrics(MeterRegistry registry,
            Supplier<Optional<CacheSnapshot>> snapshots) {
        Gauge.builder(METRIC_PREFIX + ".regions.total", snapshots,
                state -> readSnapshotValue(state, snapshot -> snapshot.regions().size()))
            .description("Total number of cache regions across all providers")
            .strongReference(true)
            .register(registry);

        Gauge.builder(METRIC_PREFIX + ".providers.total", snapshots,
                state -> readSnapshotValue(state, CacheSnapshot::providers))
            .description("Total number of cache providers")
            .strongReference(true)
            .register(registry);

        Gauge.builder(METRIC_PREFIX + ".hits.total", snapshots,
                state -> readSnapshotValue(state, snapshot -> sumStats(snapshot, CacheStats.REGION_HITS)))
            .description("Total cache hits across all providers and regions")
            .strongReference(true)
            .register(registry);

        Gauge.builder(METRIC_PREFIX + ".size.total", snapshots,
                state -> readSnapshotValue(state, snapshot -> sumStats(snapshot, CacheStats.REGION_SIZE)))
            .description("Total cache size across all providers and regions")
            .strongReference(true)
            .register(registry);

        Gauge.builder(METRIC_PREFIX + ".hit_rate.overall", snapshots,
                state -> readSnapshotValue(state, this::getOverallHitRate))
            .description("Request-weighted cache hit rate for regions reporting native request counters")
            .strongReference(true)
            .register(registry);
    }

    /**
     * Register detailed metrics for each cache region.
     */
    private void registerRegionLevelMetrics(MeterRegistry registry,
            Supplier<Optional<CacheSnapshot>> snapshots) {
        // shortcut: region identities are fixed at startup; revisit for post-startup region discovery.
        for (final RegionKey region : snapshots.get().map(CacheSnapshot::regions).orElse(Map.of()).keySet()) {
            final Supplier<CacheStats> current =
                    () -> snapshots.get().map(snapshot -> snapshot.regions().get(region)).orElse(null);
            registerRegionMetrics(registry, sanitizeMetricName(region.provider()),
                    sanitizeMetricName(region.region()), current);
        }
    }

    /** Collects provider and region facts once per TTL; failures stay unknown until the next retry. */
    private Optional<CacheSnapshot> loadSnapshot(final DotCacheAdministrator cacheAdmin) {
        try {
            final List<CacheProviderStats> providers = cacheAdmin.getCacheStatsList();
            final Map<RegionKey, CacheStats> current = new HashMap<>();
            for (final CacheProviderStats provider : providers) {
                for (final CacheStats region : provider.getStats()) {
                    current.put(new RegionKey(provider.getProviderName(),
                            getStatValue(region, CacheStats.REGION, "unknown")), region);
                }
            }
            return Optional.of(new CacheSnapshot(providers.size(), Map.copyOf(current)));
        } catch (Exception e) {
            Logger.warn(this, "Failed to collect cache metrics", e);
            return Optional.empty();
        }
    }

    /**
     * Register metrics for a specific cache region.
     */
    private void registerRegionMetrics(MeterRegistry registry, String providerName, String regionName, Supplier<CacheStats> regionStats) {
        // Cache region size (current number of objects)
        Gauge.builder(METRIC_PREFIX + ".region.size", regionStats, stats -> 
                readRegionValue(stats, current -> getStatDoubleValue(current, CacheStats.REGION_SIZE)))
            .description("Current number of objects in cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .strongReference(true)
            .register(registry);
        
        // Cache region hits
        Gauge.builder(METRIC_PREFIX + ".region.hits", regionStats, stats -> 
                readRegionValue(stats, current -> getStatDoubleValue(current, CacheStats.REGION_HITS)))
            .description("Number of cache hits for this region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .strongReference(true)
            .register(registry);
        
        // Cache region hit rate (as percentage 0-100)
        Gauge.builder(METRIC_PREFIX + ".region.hit_rate", regionStats, stats -> 
                readRegionValue(stats, current -> getStatDoubleValue(current, CacheStats.REGION_HIT_RATE)))
            .description("Cache hit rate percentage for this region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .strongReference(true)
            .register(registry);
        
        // Cache region configured size
        Gauge.builder(METRIC_PREFIX + ".region.configured_size", regionStats, stats -> 
                readRegionValue(stats, this::getConfiguredSizeValue))
            .description("Maximum configured size for this cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .strongReference(true)
            .register(registry);
        
        // Cache region load count
        Gauge.builder(METRIC_PREFIX + ".region.loads", regionStats, stats -> 
                readRegionValue(stats, current -> getStatDoubleValue(current, CacheStats.REGION_LOAD)))
            .description("Number of cache loads for this region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .strongReference(true)
            .register(registry);
        
        // Cache region average load time
        Gauge.builder(METRIC_PREFIX + ".region.avg_load_time_ms", regionStats, stats -> 
                readRegionValue(stats, current -> getStatDoubleValue(current, CacheStats.REGION_AVG_LOAD_TIME)))
            .description("Average load time in milliseconds for this region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .strongReference(true)
            .register(registry);
        
        // Cache region evictions
        Gauge.builder(METRIC_PREFIX + ".region.evictions", regionStats, stats -> 
                readRegionValue(stats, current -> getStatDoubleValue(current, CacheStats.REGION_EVICTIONS)))
            .description("Number of evictions for this cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .strongReference(true)
            .register(registry);
        
        // Cache region memory usage
        Gauge.builder(METRIC_PREFIX + ".region.memory_bytes", regionStats, stats -> 
                readRegionValue(stats, this::getMemoryBytesValue))
            .description("Total memory usage in bytes for this cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .strongReference(true)
            .register(registry);
        
        // Cache region memory per object
        Gauge.builder(METRIC_PREFIX + ".region.memory_per_object_bytes", regionStats, stats -> 
                readRegionValue(stats, this::getMemoryPerObjectBytesValue))
            .description("Average memory per object in bytes for this cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .strongReference(true)
            .register(registry);
    }
    
    /** Reads a current region snapshot, exposing unavailable regions as unknown rather than stale or zero. */
    private double readRegionValue(final Supplier<CacheStats> snapshots,
            final ToDoubleFunction<CacheStats> reader) {
        final CacheStats current = snapshots.get();
        return current == null ? Double.NaN : reader.applyAsDouble(current);
    }

    // ====================================================================
    // HELPER METHODS FOR ACCESSING CACHE STATISTICS
    // ====================================================================
    
    /** Preserves unknown collection state for aggregate gauges, just as for individual regions. */
    private double readSnapshotValue(final Supplier<Optional<CacheSnapshot>> snapshots,
            final ToDoubleFunction<CacheSnapshot> reader) {
        return snapshots.get().map(reader::applyAsDouble).orElse(Double.NaN);
    }

    /** Sums an already collected stat without scanning the providers again. */
    private double sumStats(final CacheSnapshot snapshot, final String key) {
        return snapshot.regions().values().stream().mapToDouble(stats -> getStatDoubleValue(stats, key)).sum();
    }

    /** Uses native request counters: provider-specific "loads" are not a reliable miss count. */
    private double getOverallHitRate(final CacheSnapshot snapshot) {
        double totalHits = 0;
        double totalRequests = 0;
        for (final CacheStats region : snapshot.regions().values()) {
            final OptionalLong hits = region.getHitCount();
            final OptionalLong requests = region.getRequestCount();
            if (hits.isPresent() && requests.isPresent()) {
                totalHits += hits.getAsLong();
                totalRequests += requests.getAsLong();
            }
        }
        return totalRequests > 0 ? totalHits / totalRequests * 100 : Double.NaN;
    }

    /**
     * Get a string value from cache stats with fallback.
     */
    private String getStatValue(CacheStats stats, String key, String defaultValue) {
        try {
            String value = stats.getStatValue(key);
            return UtilMethods.isSet(value) ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Get configured size from cache stats, handling different provider formats:
     * - CaffineCache: "size:1,000 | ttl:300s" (complex format with TTL)
     * - GuavaCache: "1,000" (simple NumberFormat output)
     */
    private double getConfiguredSizeValue(CacheStats stats) {
        try {
            String value = stats.getStatValue(CacheStats.REGION_CONFIGURED_SIZE);
            if (!UtilMethods.isSet(value)) {
                return 0.0;
            }
            
            value = value.trim();
            
            // Handle CaffineCache format: "size:1,000 | ttl:300s"
            if (value.startsWith("size:")) {
                String sizeStr = value.substring(5); // Remove "size:" prefix
                if (sizeStr.contains(" |")) {
                    sizeStr = sizeStr.substring(0, sizeStr.indexOf(" |")); // Remove TTL part
                }
                // Remove commas and parse
                sizeStr = sizeStr.replace(",", "").trim();
                if (!sizeStr.isEmpty() && !"NaN".equalsIgnoreCase(sizeStr)) {
                    double result = Double.parseDouble(sizeStr);
                    return Double.isNaN(result) || Double.isInfinite(result) ? 0.0 : result;
                }
            } else {
                // Handle GuavaCache format: direct NumberFormat output like "1,000"
                return getStatDoubleValue(stats, CacheStats.REGION_CONFIGURED_SIZE);
            }
        } catch (Exception e) {
            Logger.debug(this, "Failed to parse configured size from value '" + stats.getStatValue(CacheStats.REGION_CONFIGURED_SIZE) + "': " + e.getMessage());
        }
        return 0.0;
    }
    
    /**
     * Returns memory bytes, excluding legacy aliases of cumulative serialized write volume.
     */
    private double getMemoryBytesValue(CacheStats stats) {
        try {
            if (stats.getStatValue(CacheStats.REGION_BYTES_WRITTEN) != null) {
                return 0.0;
            }
            // Try REGION_MEM_TOTAL first, then fall back to REGION_MEM_TOTAL_PRETTY
            String value = stats.getStatValue(CacheStats.REGION_MEM_TOTAL);
            if (!UtilMethods.isSet(value)) {
                value = stats.getStatValue(CacheStats.REGION_MEM_TOTAL_PRETTY);
            }
            
            if (!UtilMethods.isSet(value)) {
                return 0.0;
            }
            
            return parsePrettyByteValue(value);
        } catch (Exception e) {
            Logger.debug(this, "Failed to parse memory bytes value: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Returns memory per object, excluding legacy aliases of serialized write averages.
     */
    private double getMemoryPerObjectBytesValue(CacheStats stats) {
        try {
            if (stats.getStatValue(CacheStats.REGION_AVG_SERIALIZED_SIZE) != null) {
                return 0.0;
            }
            String value = stats.getStatValue(CacheStats.REGION_MEM_PER_OBJECT);
            if (!UtilMethods.isSet(value)) {
                value = stats.getStatValue(CacheStats.REGION_MEM_PER_OBJECT_PRETTY);
            }
            
            if (!UtilMethods.isSet(value)) {
                return 0.0;
            }
            
            return parsePrettyByteValue(value);
        } catch (Exception e) {
            Logger.debug(this, "Failed to parse memory per object bytes value: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Parses byte counts and pretty units, including H22 single-letter K, M, and G suffixes.
     */
    private double parsePrettyByteValue(String value) {
        if (!UtilMethods.isSet(value)) {
            return 0.0;
        }
        
        value = value.trim().toLowerCase().replaceFirst("\\s+([gmk])$", " $1b");
        
        // Handle different units
        double multiplier = 1.0;
        String numericPart = value;
        
        if (value.endsWith(" gb") || value.endsWith("gb")) {
            multiplier = 1024.0 * 1024.0 * 1024.0;
            numericPart = value.replaceAll("\\s*gb$", "");
        } else if (value.endsWith(" mb") || value.endsWith("mb")) {
            multiplier = 1024.0 * 1024.0;
            numericPart = value.replaceAll("\\s*mb$", "");
        } else if (value.endsWith(" kb") || value.endsWith("kb")) {
            multiplier = 1024.0;
            numericPart = value.replaceAll("\\s*kb$", "");
        } else if (value.endsWith(" bytes") || value.endsWith("bytes")) {
            multiplier = 1.0;
            numericPart = value.replaceAll("\\s*bytes$", "");
        } else if (value.endsWith(" b") || value.endsWith("b")) {
            multiplier = 1.0;
            numericPart = value.replaceAll("\\s*b$", "");
        }
        
        try {
            numericPart = numericPart.replace(",", "").trim();
            if (numericPart.isEmpty()) {
                return 0.0;
            }
            
            double result = Double.parseDouble(numericPart) * multiplier;
            return Double.isNaN(result) || Double.isInfinite(result) ? 0.0 : result;
        } catch (NumberFormatException e) {
            Logger.debug(this, "Failed to parse numeric part '" + numericPart + "' from value '" + value + "'");
            return 0.0;
        }
    }

    /**
     * Get a numeric value from cache stats as double with fallback to 0.
     * 
     * Handles various formats from cache providers:
     * - Caffeine/Guava: "1,234" (with commas), "45.67%" (percentages), "12.3 ms" (with units)  
     * - DecimalFormat can produce "NaN", "∞", "-∞" for invalid floating point values
     * - H22Cache: Uses UtilMethods.prettyByteify() for memory values
     */
    private double getStatDoubleValue(CacheStats stats, String key) {
        try {
            String value = stats.getStatValue(key);
            
            if (!UtilMethods.isSet(value)) {
                return 0.0;
            }
            
            value = value.trim();
            
            // Handle DecimalFormat outputs for invalid floating point numbers
            if ("NaN".equalsIgnoreCase(value) || 
                "∞".equals(value) || "-∞".equals(value) ||
                "Infinity".equalsIgnoreCase(value) || "-Infinity".equalsIgnoreCase(value)) {
                return 0.0;
            }
            
            // Handle percentage values (e.g., "99.73%", "NaN%")
            if (value.endsWith("%")) {
                String numPart = value.substring(0, value.length() - 1).trim();
                if ("NaN".equalsIgnoreCase(numPart) || numPart.isEmpty()) {
                    return 0.0;
                }
                value = numPart;
            }
            
            // Handle time values (e.g., "15.5 ms", "NaN ms")
            if (value.endsWith(" ms")) {
                String numPart = value.substring(0, value.length() - 3).trim();
                if ("NaN".equalsIgnoreCase(numPart) || numPart.isEmpty()) {
                    return 0.0;
                }
                value = numPart;
            }
            
            // Handle size values with commas from NumberFormat (e.g., "10,000")
            value = value.replace(",", "");
            
            // Handle empty values after processing
            if (value.isEmpty()) {
                return 0.0;
            }
            
            // Final safety check before parsing
            if ("NaN".equalsIgnoreCase(value) || 
                "∞".equals(value) || "-∞".equals(value) ||
                "Infinity".equalsIgnoreCase(value) || "-Infinity".equalsIgnoreCase(value)) {
                return 0.0;
            }
            
            double result = Double.parseDouble(value);
            // Return 0 for NaN or infinite values (additional safety)
            return Double.isNaN(result) || Double.isInfinite(result) ? 0.0 : result;
            
        } catch (NumberFormatException e) {
            Logger.debug(this, "Failed to parse numeric value '" + stats.getStatValue(key) + "' for key '" + key + "': " + e.getMessage());
            return 0.0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get stat value for key '" + key + "': " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Sanitize metric names for Prometheus compatibility.
     */
    private String sanitizeMetricName(String name) {
        if (!UtilMethods.isSet(name)) {
            return "unknown";
        }
        // Replace invalid characters with underscores for Prometheus compatibility
        return name.toLowerCase()
                  .replaceAll("[^a-z0-9_]", "_")
                  .replaceAll("_+", "_")
                  .replaceAll("^_|_$", "");
    }
}