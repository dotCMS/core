package com.dotcms.analytics.content;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jose Castro
 * @since Nov 28th, 2024
 */
@JsonDeserialize(builder = ContentAnalyticsQuery.Builder.class)
public class ContentAnalyticsQuery implements Serializable {

    @JsonProperty()
    private final Set<String> measures;
    @JsonProperty()
    private final Set<String> dimensions;
    @JsonProperty()
    private final List<Map<String, String>> timeDimensions;
    @JsonProperty()
    private final List<Map<String, Object>> filters;
    @JsonProperty()
    private final List<String[]> order;
    @JsonProperty()
    private final int limit;
    @JsonProperty()
    private final int offset;

    private static final String SEPARATOR = ":";

    private ContentAnalyticsQuery(final Builder builder) {
        this.measures = builder.measures;
        this.dimensions = builder.dimensions;
        this.timeDimensions = builder.timeDimensions;
        this.filters = builder.filters;
        this.order = builder.order;
        this.limit = builder.limit;
        this.offset = builder.offset;
    }

    public Set<String> measures() {
        return this.measures;
    }

    public Set<String> dimensions() {
        return this.dimensions;
    }

    public List<Map<String, String>> timeDimensions() {
        return this.timeDimensions;
    }

    public List<Map<String, Object>> filters() {
        return this.filters;
    }

    public List<String[]> order() {
        return this.order;
    }

    public int limit() {
        return this.limit;
    }

    public int offset() {
        return this.offset;
    }

    public static ContentAnalyticsQuery.Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ContentAnalyticsQuery{" +
                "measures='" + measures + '\'' +
                ", dimensions='" + dimensions + '\'' +
                ", timeDimensions='" + timeDimensions + '\'' +
                ", filters='" + filters + '\'' +
                ", order='" + order + '\'' +
                ", limit='" + limit + '\'' +
                ", offset='" + offset + '\'' +
                '}';
    }

    public static class Builder {

        private Set<String> measures;
        private Set<String> dimensions;
        private final List<Map<String, String>> timeDimensions = new ArrayList<>();
        private final List<Map<String, Object>> filters = new ArrayList<>();
        private final List<String[]> order = new ArrayList<>();
        private int limit = 1000;
        private int offset = 0;

        public Builder measures(@Nonnull final String measures) {
            this.measures = Set.of(measures.split("\\s+"));
            return this;
        }

        public Builder dimensions(@Nonnull final String dimensions) {
            this.dimensions = Set.of(dimensions.split("\\s+"));
            return this;
        }

        public Builder timeDimensions(final String timeDimensions) {
            if (UtilMethods.isNotSet(timeDimensions)) {
                return this;
            }
            final String[] timeParams = timeDimensions.split(SEPARATOR);
            final Map<String, String> timeDimensionsData = new HashMap<>();
            timeDimensionsData.put("dimension", timeParams[0]);
            if (timeParams.length > 1) {
                timeDimensionsData.put("dateRange", timeParams[1]);
            } else {
                timeDimensionsData.put("dateRange", "Last week");
            }
            this.timeDimensions.add(timeDimensionsData);
            return this;
        }

        public Builder filters(final String filters) {
            if (UtilMethods.isNotSet(filters)) {
                return this;
            }
            final String[] filterArr = filters.split(SEPARATOR);
            for (final String filter : filterArr) {
                final String[] filterParams = filter.split("\\s+");
                final Map<String, Object> filterDataMap = new HashMap<>();
                filterDataMap.put("member", filterParams[0]);
                filterDataMap.put("operator", filterParams[1]);
                final String[] filterValues = filterParams[2].split(",");
                filterDataMap.put("values", filterValues);
                this.filters.add(filterDataMap);
            }
            return this;
        }

        public Builder order(final String order) {
            if (UtilMethods.isNotSet(order)) {
                return this;
            }
            final Set<String> orderCriteria = Set.of(order.split(SEPARATOR));
            for (final String orderCriterion : orderCriteria) {
                final String[] orderParams = orderCriterion.split("\\s+");
                this.order.add(orderParams);
            }
            return this;
        }

        public Builder limit(final int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(final int offset) {
            this.offset = offset;
            return this;
        }

        public ContentAnalyticsQuery build() {
            return new ContentAnalyticsQuery(this);
        }

    }

}

