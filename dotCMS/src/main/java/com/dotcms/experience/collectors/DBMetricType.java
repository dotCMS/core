package com.dotcms.experience.collectors;

import com.dotcms.experience.MetricType;
import com.dotcms.experience.MetricValue;
import com.dotcms.experience.business.MetricsAPI;

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
        return MetricsAPI.INSTANCE.getValue(getSqlQuery());
    }

    @Override
    default Optional<MetricValue> getStat() {
        return getValue()
                .map(value -> new MetricValue(this.getMetric(), value));

    }

}
