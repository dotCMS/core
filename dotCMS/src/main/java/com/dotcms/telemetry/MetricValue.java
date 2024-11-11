package com.dotcms.telemetry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.lang.math.NumberUtils;

import java.text.DecimalFormat;

/**
 * Represents the value for a {@link MetricType}
 */
public class MetricValue {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    @JsonUnwrapped
    final Metric metric;

    final Object value;

    public MetricValue(final Metric metric, final Object value) {
        this.metric = metric;
        this.value = value;
    }

    /**
     * Check if the value of the Metric is a numeric value.
     *
     * @return true if the value is a numeric value
     */
    @JsonIgnore
    public boolean isNumeric() {
        return NumberUtils.isNumber(value.toString());
    }

    public Metric getMetric() {
        return metric;
    }

    /**
     * Return the value of the Metric if it is a numeric value then return it as a String formatted
     * with two decimals
     *
     * @return
     */
    public Object getValue() {
        if (isNumeric()) {
            return FORMAT.format(Double.parseDouble(value.toString()));
        } else {
            return value;
        }
    }

    @Override
    public String toString() {
        return "MetricValue{" +
                "metric=" + metric +
                ", value=" + value +
                '}';
    }

}
