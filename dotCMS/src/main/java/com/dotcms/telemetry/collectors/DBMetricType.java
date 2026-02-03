package com.dotcms.telemetry.collectors;

import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotRuntimeException;

import java.util.Optional;

/**
 * Represents the MetaData of a Metric that we want to collect from DataBase
 *
 * @see MetricType
 */
public interface DBMetricType extends MetricType {

    String getSqlQuery();

    /**
     * Executes the metric query with connection management.
     *
     * This replicates the behavior of the original @CloseDBIfOpened annotation
     * that was on MetricsAPIImpl.getValue() but didn't fire on CDI beans.
     *
     * Connection-only management is used (no transaction) because metrics are
     * read-only SELECT queries that don't require transaction semantics.
     *
     * @return Optional containing the metric value, or empty if query returns no results
     */
    @Override
    default Optional<Object> getValue() {
        try {
            return DbConnectionFactory.wrapConnection(() ->
                    APILocator.getMetricsAPI().getValue(getSqlQuery())
            );
        } catch (final Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    @Override
    default Optional<MetricValue> getStat() {
        return getValue().map(value -> new MetricValue(this.getMetric(), value));
    }

}
