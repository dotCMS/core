package com.dotcms.metrics.binders;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.List;

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
    
    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
            
            // Cache administrator availability
            Gauge.builder(METRIC_PREFIX + ".administrator.available", this, cache -> cacheAdmin != null ? 1.0 : 0.0)
                .description("Whether the cache administrator is available")
                .register(registry);
            
            if (cacheAdmin != null) {
                registerProviderLevelMetrics(registry, cacheAdmin);
                registerRegionLevelMetrics(registry, cacheAdmin);
            }
            
            Logger.info(this, "Comprehensive cache metrics registered successfully");
            
        } catch (Exception e) {
            Logger.error(this, "Failed to register cache metrics: " + e.getMessage(), e);
        }
    }
    
    /**
     * Register provider-level aggregate metrics.
     */
    private void registerProviderLevelMetrics(MeterRegistry registry, DotCacheAdministrator cacheAdmin) {
        // Total cache regions across all providers
        Gauge.builder(METRIC_PREFIX + ".regions.total", this, metrics -> getTotalCacheRegions())
            .description("Total number of cache regions across all providers")
            .register(registry);
        
        // Total cache providers
        Gauge.builder(METRIC_PREFIX + ".providers.total", this, metrics -> getTotalCacheProviders())
            .description("Total number of cache providers")
            .register(registry);
        
        // Total cache hits across all providers
        Gauge.builder(METRIC_PREFIX + ".hits.total", this, metrics -> getTotalCacheHits())
            .description("Total cache hits across all providers and regions")
            .register(registry);
        
        // Total cache size across all providers
        Gauge.builder(METRIC_PREFIX + ".size.total", this, metrics -> getTotalCacheSize())
            .description("Total cache size across all providers and regions")
            .register(registry);
        
        // Overall cache hit rate
        Gauge.builder(METRIC_PREFIX + ".hit_rate.overall", this, metrics -> getOverallHitRate())
            .description("Overall cache hit rate across all providers and regions")
            .register(registry);
    }
    
    /**
     * Register detailed metrics for each cache region.
     */
    private void registerRegionLevelMetrics(MeterRegistry registry, DotCacheAdministrator cacheAdmin) {
        try {
            List<CacheProviderStats> statsList = cacheAdmin.getCacheStatsList();
            
            for (CacheProviderStats providerStats : statsList) {
                String providerName = sanitizeMetricName(providerStats.getProviderName());
                
                for (CacheStats regionStats : providerStats.getStats()) {
                    String regionName = sanitizeMetricName(getStatValue(regionStats, CacheStats.REGION, "unknown"));
                    
                    registerRegionMetrics(registry, providerName, regionName, regionStats);
                }
            }
            
        } catch (Exception e) {
            Logger.warn(this, "Failed to register region-level metrics: " + e.getMessage());
        }
    }
    
    /**
     * Register metrics for a specific cache region.
     */
    private void registerRegionMetrics(MeterRegistry registry, String providerName, String regionName, CacheStats regionStats) {
        // Cache region size (current number of objects)
        Gauge.builder(METRIC_PREFIX + ".region.size", regionStats, stats -> 
                getStatDoubleValue(stats, CacheStats.REGION_SIZE))
            .description("Current number of objects in cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .register(registry);
        
        // Cache region hits
        Gauge.builder(METRIC_PREFIX + ".region.hits", regionStats, stats -> 
                getStatDoubleValue(stats, CacheStats.REGION_HITS))
            .description("Number of cache hits for this region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .register(registry);
        
        // Cache region hit rate
        Gauge.builder(METRIC_PREFIX + ".region.hit_rate", regionStats, stats -> 
                getStatDoubleValue(stats, CacheStats.REGION_HIT_RATE))
            .description("Cache hit rate percentage for this region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .register(registry);
        
        // Cache region configured size
        Gauge.builder(METRIC_PREFIX + ".region.configured_size", regionStats, stats -> 
                getStatDoubleValue(stats, CacheStats.REGION_CONFIGURED_SIZE))
            .description("Maximum configured size for this cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .register(registry);
        
        // Cache region load count
        Gauge.builder(METRIC_PREFIX + ".region.loads", regionStats, stats -> 
                getStatDoubleValue(stats, CacheStats.REGION_LOAD))
            .description("Number of cache loads for this region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .register(registry);
        
        // Cache region average load time
        Gauge.builder(METRIC_PREFIX + ".region.avg_load_time_ms", regionStats, stats -> 
                getStatDoubleValue(stats, CacheStats.REGION_AVG_LOAD_TIME))
            .description("Average load time in milliseconds for this region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .register(registry);
        
        // Cache region evictions
        Gauge.builder(METRIC_PREFIX + ".region.evictions", regionStats, stats -> 
                getStatDoubleValue(stats, CacheStats.REGION_EVICTIONS))
            .description("Number of evictions for this cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .register(registry);
        
        // Cache region memory usage
        Gauge.builder(METRIC_PREFIX + ".region.memory_bytes", regionStats, stats -> 
                getStatDoubleValue(stats, CacheStats.REGION_MEM_TOTAL))
            .description("Total memory usage in bytes for this cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .register(registry);
        
        // Cache region memory per object
        Gauge.builder(METRIC_PREFIX + ".region.memory_per_object_bytes", regionStats, stats -> 
                getStatDoubleValue(stats, CacheStats.REGION_MEM_PER_OBJECT))
            .description("Average memory per object in bytes for this cache region")
            .tag("provider", providerName)
            .tag("region", regionName)
            .register(registry);
    }
    
    // ====================================================================
    // HELPER METHODS FOR ACCESSING CACHE STATISTICS
    // ====================================================================
    
    private double getTotalCacheRegions() {
        try {
            DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
            if (cacheAdmin != null) {
                List<CacheProviderStats> statsList = cacheAdmin.getCacheStatsList();
                return statsList.stream()
                    .mapToInt(stats -> stats.getStats().size())
                    .sum();
            }
            return 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get total cache regions: " + e.getMessage());
            return 0;
        }
    }
    
    private double getTotalCacheProviders() {
        try {
            DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
            if (cacheAdmin != null) {
                List<CacheProviderStats> statsList = cacheAdmin.getCacheStatsList();
                return statsList.size();
            }
            return 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get total cache providers: " + e.getMessage());
            return 0;
        }
    }
    
    private double getTotalCacheHits() {
        try {
            DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
            if (cacheAdmin != null) {
                List<CacheProviderStats> statsList = cacheAdmin.getCacheStatsList();
                return statsList.stream()
                    .flatMap(providerStats -> providerStats.getStats().stream())
                    .mapToDouble(regionStats -> getStatDoubleValue(regionStats, CacheStats.REGION_HITS))
                    .sum();
            }
            return 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get total cache hits: " + e.getMessage());
            return 0;
        }
    }
    
    private double getTotalCacheSize() {
        try {
            DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
            if (cacheAdmin != null) {
                List<CacheProviderStats> statsList = cacheAdmin.getCacheStatsList();
                return statsList.stream()
                    .flatMap(providerStats -> providerStats.getStats().stream())
                    .mapToDouble(regionStats -> getStatDoubleValue(regionStats, CacheStats.REGION_SIZE))
                    .sum();
            }
            return 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to get total cache size: " + e.getMessage());
            return 0;
        }
    }
    
    private double getOverallHitRate() {
        try {
            DotCacheAdministrator cacheAdmin = CacheLocator.getCacheAdministrator();
            if (cacheAdmin != null) {
                List<CacheProviderStats> statsList = cacheAdmin.getCacheStatsList();
                
                double totalHits = 0;
                double totalRequests = 0;
                
                for (CacheProviderStats providerStats : statsList) {
                    for (CacheStats regionStats : providerStats.getStats()) {
                        double hits = getStatDoubleValue(regionStats, CacheStats.REGION_HITS);
                        double loads = getStatDoubleValue(regionStats, CacheStats.REGION_LOAD);
                        
                        totalHits += hits;
                        totalRequests += (hits + loads); // Total requests = hits + misses (loads)
                    }
                }
                
                return totalRequests > 0 ? (totalHits / totalRequests) * 100 : 0;
            }
            return 0;
        } catch (Exception e) {
            Logger.debug(this, "Failed to calculate overall hit rate: " + e.getMessage());
            return 0;
        }
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
     * Get a numeric value from cache stats as double with fallback to 0.
     */
    private double getStatDoubleValue(CacheStats stats, String key) {
        try {
            String value = stats.getStatValue(key);
            
            if (!UtilMethods.isSet(value)) {
                return 0.0;
            }
            
            // Handle percentage values (e.g., "99.73%")
            if (value.endsWith("%")) {
                value = value.substring(0, value.length() - 1);
            }
            
            // Handle size values with commas (e.g., "10,000")
            value = value.replace(",", "");
            
            // Handle empty or non-numeric values
            if (value.trim().isEmpty()) {
                return 0.0;
            }
            
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            Logger.debug(this, "Failed to parse numeric value for key '" + key + "': " + e.getMessage());
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