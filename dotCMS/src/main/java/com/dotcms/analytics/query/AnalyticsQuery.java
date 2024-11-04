package com.dotcms.analytics.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.Set;

/**
 * Encapsulates a simple query for the analytics backend
 * Example:
 * <pre>
 *     {
 *     "query": {
 *         "dimensions": [
 *             "Events.experiment",
 *             "Events.variant",
 *             "Events.lookBackWindow"
 *         ],
 *         "measures": [
 *             "Events.count"
 *         ],
 *         "filters": "Events.variant = ['B']",
 *         "limit": 100,
 *         "offset": 1,
 *         "timeDimensions": "Events.day day",
 *         "order": "Events.day ASC"
 *     }
 * }
 *
 * @see AnalyticsQueryParser
 * </pre>
 * @author jsanca
 */
@JsonDeserialize(builder = AnalyticsQuery.Builder.class)
public class AnalyticsQuery implements Serializable {

    private final Set<String> dimensions; // ["Events.referer", "Events.experiment", "Events.variant", "Events.utcTime", "Events.url", "Events.lookBackWindow", "Events.eventType"]
    private final Set<String> measures; // ["Events.count", "Events.uniqueCount"]
    private final String filters; //  Events.variant = ["B"] or Events.experiments = ["B"]
    private final long limit;
    private final long offset;
    private final String timeDimensions; // Events.day day
    private String order; // Events.day ASC

    private AnalyticsQuery(final Builder builder) {
        this.dimensions = builder.dimensions;
        this.measures = builder.measures;
        this.filters = builder.filters;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.timeDimensions = builder.timeDimensions;
        this.order = builder.order;
    }

    public Set<String> getDimensions() {
        return dimensions;
    }

    public Set<String> getMeasures() {
        return measures;
    }

    public String getFilters() {
        return filters;
    }

    public long getLimit() {
        return limit;
    }

    public long getOffset() {
        return offset;
    }

    public String getTimeDimensions() {
        return timeDimensions;
    }

    public String getOrder() {
        return order;
    }

    public static class Builder {

        @JsonProperty()
        private Set<String> dimensions;
        @JsonProperty()
        private Set<String> measures;
        @JsonProperty()
        private String filters;
        @JsonProperty()
        private long limit;
        @JsonProperty()
        private long offset;
        @JsonProperty()
        private String timeDimensions;
        @JsonProperty()
        private String order;


        public Builder dimensions(Set<String> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder measures(Set<String> measures) {
            this.measures = measures;
            return this;
        }

        public Builder filters(String filters) {
            this.filters = filters;
            return this;
        }

        public Builder limit(long limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(long offset) {
            this.offset = offset;
            return this;
        }

        public Builder timeDimensions(String timeDimensions) {
            this.timeDimensions = timeDimensions;
            return this;
        }

        public Builder order(String orders) {
            this.order = orders;
            return this;
        }

        public AnalyticsQuery build() {
            return new AnalyticsQuery(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AnalyticsQuery{" +
                "dimensions=" + dimensions +
                ", measures=" + measures +
                ", filters='" + filters + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                ", timeDimensions='" + timeDimensions + '\'' +
                ", order='" + order + '\'' +
                '}';
    }
}

