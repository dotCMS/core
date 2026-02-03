package com.dotcms.telemetry.collectors;

import com.dotcms.telemetry.MetricCalculationError;
import com.dotcms.telemetry.MetricTiming;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.telemetry.ProfileType;
import com.dotcms.telemetry.business.TimeoutConfig;
import com.dotcms.telemetry.cache.MetricCacheConfig;
import com.dotcms.telemetry.cache.MetricCacheManager;
import com.dotcms.telemetry.util.MetricCaches;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Collects and generates all metrics for telemetry reporting.
 * 
 * <p>Automatically discovers all {@code @ApplicationScoped MetricType} implementations
 * via CDI's {@link Instance} injection. Metrics are collected and aggregated into
 * a {@link MetricsSnapshot} for reporting.</p>
 * 
 * <p>For dashboard-specific metrics, see {@link DashboardMetricsProvider}.</p>
 *
 * @author Freddy Rodriguez
 * @since Jan 8th, 2024
 */
@ApplicationScoped
public class MetricStatsCollector {
    
    @Inject
    private Instance<MetricType> metricTypes;
    
    @Inject
    private MetricCacheConfig config;

    @Inject
    private MetricCacheManager cacheManager;

    @Inject
    private TimeoutConfig timeoutConfig;

    /**
     * Calculate a MetricSnapshot by iterating through all the MetricType collections.
     *
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public MetricsSnapshot getStats() {
        return getStats(Set.of());
    }
    
    /**
     * Calculate a MetricSnapshot by iterating through all the MetricType collections.
     *
     * @param metricNameSet the set of metric names to filter by (empty set means all metrics)
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public MetricsSnapshot getStats(final Set<String> metricNameSet) {
        return getStats(metricNameSet, null);
    }
    
    /**
     * Calculate a MetricSnapshot by iterating through all the MetricType collections.
     *
     * @param metricNameSet the set of metric names to filter by (empty set means all metrics)
     * @param profileOverride optional profile override (if null, uses default profile from config)
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public MetricsSnapshot getStats(final Set<String> metricNameSet, final ProfileType profileOverride) {
        return getStats(metricNameSet, profileOverride, false);
    }

    /**
     * Calculate a MetricSnapshot by iterating through all the MetricType collections.
     *
     * @param metricNameSet the set of metric names to filter by (empty set means all metrics)
     * @param profileOverride optional profile override (if null, uses default profile from config)
     * @param bypassCache if true, invalidates cache before collection for fresh values
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public MetricsSnapshot getStats(final Set<String> metricNameSet, final ProfileType profileOverride,
                                    final boolean bypassCache) {
        // Invalidate cache if bypass requested
        if (bypassCache) {
            Logger.info(this, "Cache bypass requested - invalidating all metric caches");
            cacheManager.invalidateAll();
        }

        final Collection<MetricValue> stats = new ArrayList<>();
        final Collection<MetricValue> noNumberStats = new ArrayList<>();
        final Collection<MetricCalculationError> errors = new ArrayList<>();
        final Collection<MetricTiming> timings = new ArrayList<>();

        // Create executor for timeout enforcement
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            // Get active profile - use override if provided, otherwise use default from config
            final ProfileType activeProfile = profileOverride != null ? profileOverride : config.getActiveProfile();
            Logger.debug(this, () -> String.format("Collecting metrics for profile: %s%s",
                activeProfile, profileOverride != null ? " (override)" : ""));

            // Use CDI-discovered metrics if available, otherwise fall back to static collection
            final Collection<MetricType> allCollectors = getMetricCollectors();
            final Collection<MetricType> collectors = allCollectors
                    .stream()
                    .filter(metric -> {
                        // Apply name filter if provided
                        if (!metricNameSet.isEmpty() && !metricNameSet.contains(metric.getName())) {
                            return false;
                        }
                        // Apply profile filter
                        return ProfileFilter.matches(metric, activeProfile);
                    })
                    .collect(Collectors.toList());

            Logger.debug(this, () -> String.format("Filtered to %d metrics matching profile %s (from %d total)",
                    collectors.size(), activeProfile, allCollectors.size()));

            // Collect metrics with timing and timeout enforcement
            for (final MetricType metricType : collectors) {
                final long startTime = System.currentTimeMillis();
                final boolean[] flags = new boolean[3]; // [0] = timedOut, [1] = completed, [2] = cacheMiss
                final long[] timingData = new long[1]; // [0] = computationTimeMs

                try {
                    // Submit metric collection as a task with timeout
                    final Callable<Optional<MetricValue>> task = () -> cacheManager.get(
                        metricType.getName(),
                        () -> {
                            // Supplier called = cache miss
                            flags[2] = true;
                            final long computationStart = System.currentTimeMillis();
                            try {
                                final Optional<MetricValue> result = getMetricValue(metricType);
                                timingData[0] = System.currentTimeMillis() - computationStart;
                                return result;
                            } catch (final DotDataException e) {
                                timingData[0] = System.currentTimeMillis() - computationStart;
                                Logger.error(this, "Error getting metric value for " + metricType.getName(), e);
                                return Optional.empty();
                            }
                        }
                    );
                    final Future<Optional<MetricValue>> future = executor.submit(task);

                    try {
                        // Wait for result with timeout
                        final Optional<MetricValue> metricValueOpt = future.get(
                                timeoutConfig.getMetricTimeoutMillis(),
                                TimeUnit.MILLISECONDS
                        );

                        // Process successful result
                        metricValueOpt.ifPresent(metricValue -> {
                            if (metricValue.isNumeric()) {
                                stats.add(metricValue);
                            } else {
                                noNumberStats.add(metricValue);
                            }
                        });

                        flags[1] = true; // completed

                    } catch (final TimeoutException e) {
                        // Metric exceeded timeout
                        flags[0] = true; // timedOut
                        future.cancel(true);  // Interrupt the task
                        errors.add(new MetricCalculationError(metricType.getMetric(),
                                String.format("Timeout after %dms", timeoutConfig.getMetricTimeoutMillis())));
                        Logger.warn(this, String.format("Metric '%s' timed out after %dms",
                                metricType.getName(), timeoutConfig.getMetricTimeoutMillis()));
                    }

                } catch (final Throwable e) {
                    // Other errors during metric collection
                    errors.add(new MetricCalculationError(metricType.getMetric(), e.getMessage()));
                    Logger.debug(MetricStatsCollector.class, () ->
                            "Error while calculating Metric " + metricType.getName() + ": " + e.getMessage());
                } finally {
                    // Record timing regardless of outcome
                    final long duration = System.currentTimeMillis() - startTime;
                    final boolean timedOut = flags[0];
                    final boolean completed = flags[1];
                    final boolean cacheMiss = flags[2];
                    final boolean cacheHit = !cacheMiss;
                    final long computationMs = timingData[0];
                    final boolean slow = completed && duration > timeoutConfig.getSlowThresholdMillis();

                    final MetricTiming timing = new MetricTiming(
                            metricType.getName(),
                            duration,
                            cacheHit,
                            computationMs,
                            timedOut,
                            slow
                    );
                    timings.add(timing);

                    // Log slow metrics for optimization
                    if (slow) {
                        Logger.warn(this, String.format(
                                "Slow metric detected: '%s' took %dms (threshold: %dms)%s",
                                metricType.getName(), duration, timeoutConfig.getSlowThresholdMillis(),
                                cacheHit ? " [cache hit]" : " [cache miss]"));
                    }

                    final String metricName = metricType.getName();
                    final long finalDuration = duration;
                    final boolean finalTimedOut = timedOut;
                    final boolean finalSlow = slow;
                    final boolean finalCacheHit = cacheHit;
                    final long finalComputationMs = computationMs;
                    Logger.debug(this, () -> String.format(
                            "Metric '%s' completed in %dms%s%s%s%s",
                            metricName, finalDuration,
                            finalTimedOut ? " (TIMED OUT)" : "",
                            finalSlow ? " (SLOW)" : "",
                            finalCacheHit ? " [cache hit]" : " [cache miss]",
                            !finalCacheHit && finalComputationMs > 0 ? String.format(" (computation: %dms)", finalComputationMs) : ""));
                }
            }
        } finally {
            // Shutdown executor and close DB connection
            executor.shutdownNow();
        }

        MetricCaches.flushAll();

        return new MetricsSnapshot.Builder()
                .stats(stats)
                .notNumericStats(noNumberStats)
                .errors(errors)
                .timings(timings)
                .build();
    }

    /**
     * Get all metric collectors via CDI discovery.
     * 
     * <p>CDI guarantees that {@code @Inject Instance<T>} fields are never null.
     * If no beans are found, CDI injects an empty {@code Instance} that iterates to nothing.
     * This method iterates over all discovered MetricType implementations.</p>
     * 
     * @return collection of all MetricType implementations discovered by CDI
     */
    private Collection<MetricType> getMetricCollectors() {
        final Collection<MetricType> collectors = new ArrayList<>();

        Logger.debug(this, "Starting MetricType discovery via CDI Instance<MetricType>");

        // CDI guarantees metricTypes is never null - it injects an empty Instance if no beans found
        for (MetricType metricType : metricTypes) {
            collectors.add(metricType);
            Logger.debug(this, () -> String.format("MetricStatsCollector discovered: %s", metricType.getClass().getName()));
        }

        if (collectors.isEmpty()) {
            Logger.warn(this, "MetricStatsCollector discovered 0 MetricType implementations via CDI! This indicates a CDI scanning/configuration issue.");
        } else {
            Logger.debug(this, () -> String.format("MetricStatsCollector discovered %d MetricType implementations via CDI", collectors.size()));
        }

        return collectors;
    }

    /**
     * Get stats and clean up.
     * Uses the default profile from configuration (typically MINIMAL for dashboard).
     *
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public MetricsSnapshot getStatsAndCleanUp() {
        return getStats();
    }
    
    /**
     * Get stats with a specific profile and clean up.
     * Used by cron jobs to collect all metrics regardless of dashboard profile setting.
     *
     * @param profile the profile to use for metric collection
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public MetricsSnapshot getStatsAndCleanUp(final ProfileType profile) {
        return getStats(Set.of(), profile);
    }

    private Optional<MetricValue> getMetricValue(final MetricType metricType) throws DotDataException {
        final Optional<MetricValue> metricStatsOptional = metricType.getStat();

        if (metricStatsOptional.isPresent()) {
            final MetricValue metricValue = metricStatsOptional.get();

            if (metricValue.getValue() instanceof Boolean) {
                return Optional.of(new MetricValue(metricValue.getMetric(),
                        Boolean.getBoolean(metricValue.getValue().toString()) ? 1 : 0));
            }
        }

        return metricStatsOptional;
    }
}
