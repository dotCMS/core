package com.dotcms.telemetry;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Represent an error that occurs while calculating the metric.
 */
public class MetricCalculationError {

    @JsonUnwrapped
    private final Metric metric;
    private final String error;

    public MetricCalculationError(Metric metric, String error) {
        this.metric = metric;
        this.error = error;
    }

    public Metric getType() {
        return metric;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "MetricCalculationError{" +
                "metric=" + metric +
                ", error='" + error + '\'' +
                '}';
    }

}
