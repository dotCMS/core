package com.dotcms.telemetry.collectors;

import com.dotcms.telemetry.MetricCalculationError;
import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.telemetry.collectors.api.ApiMetricAPI;
import com.dotcms.telemetry.util.MetricCaches;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

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
    private ApiMetricAPI apiStatAPI;

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
        final Collection<MetricValue> stats = new ArrayList<>();
        final Collection<MetricValue> noNumberStats = new ArrayList<>();
        Collection<MetricCalculationError> errors = new ArrayList<>();

        try {
            openDBConnection();

            // Use CDI-discovered metrics if available, otherwise fall back to static collection
            final Collection<MetricType> collectors = getMetricCollectors();

            for (final MetricType metricType : collectors) {
                // If the metricNameSet is not empty and the metricNameSet does not contain the metricType name, skip it
                if (!metricNameSet.isEmpty() && !metricNameSet.contains(metricType.getName())) {
                    continue;
                }
                try {
                    getMetricValue(metricType).ifPresent(metricValue -> {
                        if (metricValue.isNumeric()) {
                            stats.add(metricValue);
                        } else {
                            noNumberStats.add(metricValue);
                        }
                    });
                } catch (final Throwable e) {
                    errors.add(new MetricCalculationError(metricType.getMetric(), e.getMessage()));
                    Logger.debug(MetricStatsCollector.class, () ->
                            "Error while calculating Metric " + metricType.getName() + ": " + e.getMessage());
                }
            }
        } finally {
            DbConnectionFactory.closeSilently();
        }

        MetricCaches.flushAll();

        return new MetricsSnapshot.Builder()
                .stats(stats)
                .notNumericStats(noNumberStats)
                .errors(errors)
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
     * Get stats and clean up temporary API table.
     *
     * @return the {@link MetricsSnapshot} with all the calculated metrics.
     */
    public MetricsSnapshot getStatsAndCleanUp() {
        final MetricsSnapshot stats = getStats();
        apiStatAPI.flushTemporaryTable();
        return stats;
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

    private void openDBConnection() {
        DbConnectionFactory.getConnection();
    }
}
