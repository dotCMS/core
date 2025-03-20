package com.dotcms.telemetry.collectors;

import com.dotcms.telemetry.MetricType;
import com.dotcms.telemetry.MetricValue;
import com.dotmarketing.business.APILocator;

import java.util.Optional;

/**
 * Represents the MetaData of a Metric that we want to collect from DataBase
 *
 * @see MetricType
 */
public interface DBMetricType extends MetricType {

    String getSqlQuery();

    @Override
    default Optional<Object> getValue() {
        return APILocator.getMetricsAPI().getValue(getSqlQuery());
    }

    @Override
    default Optional<MetricValue> getStat() {
        return getValue().map(value -> new MetricValue(this.getMetric(), value));
    }

}
