package com.dotcms.telemetry.util;

import java.util.Arrays;

/**
 * Collection of all the Cache regions used on the Telemetry feature
 */
public enum MetricCaches {

    SITE_SEARCH_INDICES(new MetricCache<>(IndicesSiteSearchUtil.INSTANCE::getESIndices));

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
