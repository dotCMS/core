package com.dotcms.experience.util;

import com.dotcms.experience.collectors.api.ApiMetricAPI;

import java.util.Arrays;

/**
 * Collection of all the Cache regions used on the Telemetry feature
 */
public enum MetricCaches {

    SITE_SEARCH_INDICES(new MetricCache<>(IndicesSiteSearchUtil.INSTANCE::getESIndices)),
    TEMPORARY_TABLA_DATA(new MetricCache<>(ApiMetricAPI::getMetricTemporaryTableData));

    private final MetricCache<?> metricCache;

    MetricCaches(final MetricCache<?> metricCache) {
        this.metricCache = metricCache;
    }

    public static void flushAll() {
        Arrays.stream(MetricCaches.values()).parallel().forEach(metricCaches -> metricCaches.cache().flush());
    }

    public <T> T get() {
        return (T) metricCache.get();
    }

    public MetricCache cache() {
        return metricCache;
    }

}
