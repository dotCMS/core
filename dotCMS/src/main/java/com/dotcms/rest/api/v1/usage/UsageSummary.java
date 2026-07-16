package com.dotcms.rest.api.v1.usage;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Summary of key business metrics for the usage dashboard.
 * 
 * <p>This class uses a dynamic, category-based structure that adapts to
 * available metrics based on the active profile (MINIMAL, STANDARD, FULL).
 * Metrics are organized by category as defined by their {@code @DashboardMetric}
 * annotation.</p>
 * 
 * <p>Each category contains a map of metric names to values, allowing the
 * dashboard to display whatever metrics are available without requiring
 * hardcoded field mappings.</p>
 */
public final class UsageSummary {

    /**
     * Metrics organized by category. Each category contains a map of
     * metric names to their values. Only metrics available for the
     * active profile are included.
     */
    @JsonProperty
    private final Map<String, Map<String, Object>> metrics;
    
    @JsonProperty
    private final Instant lastUpdated;

    private UsageSummary(final Builder builder) {
        this.metrics = builder.metrics;
        this.lastUpdated = builder.lastUpdated;
    }

    /**
     * Returns all metrics organized by category.
     * 
     * @return map where keys are category names (e.g., "content", "site", "user", "system")
     *         and values are maps of metric names to their values
     */
    public Map<String, Map<String, Object>> getMetrics() {
        return metrics;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Map<String, Map<String, Object>> metrics;
        private Instant lastUpdated;

        public Builder metrics(final Map<String, Map<String, Object>> metrics) {
            this.metrics = metrics;
            return this;
        }

        public Builder lastUpdated(final Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public UsageSummary build() {
            return new UsageSummary(this);
        }
    }

}