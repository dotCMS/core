package com.dotcms.telemetry;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a snapshot of metric statistics, including all calculated metrics, separated into
 * numeric and non-numeric categories. It also contains a list of errors, indicating any metrics
 * that encountered exceptions during calculation.
 */
public class MetricsSnapshot {

    /**
     * Collection of Numeric Metrics
     */
    final Collection<MetricValue> stats;

    /**
     * Collection of No Numeric Metrics
     */
    final Collection<MetricValue> notNumericStats;

    /**
     * Metric that thrown an Exception during the calculation process
     */
    final Collection<MetricCalculationError> errors;

    public MetricsSnapshot(final Builder builder) {
        this.stats = builder.stats;
        this.notNumericStats = builder.notNumericStats;
        this.errors = builder.errors;
    }

    @JsonProperty
    public Collection<MetricValue> getStats() {
        return stats;
    }

    @JsonProperty
    public Collection<MetricCalculationError> getErrors() {
        return errors;
    }

    @JsonAnyGetter
    public Map<String, Object> getNotNumericStats() {
        final Map<String, Object> result = new HashMap<>();
        for (final MetricValue stat : notNumericStats) {
            Object value = stat.getValue();
            if (stat.getValue() instanceof String) {
                value = stat.getValue().toString();
            }
            result.put(stat.getMetric().getName(), value);
        }
        return result;
    }

    @Override
    public String toString() {
        return "MetricsSnapshot{" +
                "stats=" + stats +
                ", notNumericStats=" + notNumericStats +
                ", errors=" + errors +
                '}';
    }

    public static class Builder {
        private Collection<MetricValue> stats;
        private Collection<MetricValue> notNumericStats;
        private Collection<MetricCalculationError> errors;

        public Builder stats(Collection<MetricValue> stats) {
            this.stats = stats;
            return this;
        }

        public Builder notNumericStats(Collection<MetricValue> notNumericStats) {
            this.notNumericStats = notNumericStats;
            return this;
        }

        public Builder errors(Collection<MetricCalculationError> errors) {
            this.errors = errors;
            return this;
        }

        public MetricsSnapshot build() {
            return new MetricsSnapshot(this);
        }
    }

}
