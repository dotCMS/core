package com.dotcms.analytics.content;

import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.liferay.util.StringPool.COLON;
import static com.liferay.util.StringPool.COMMA;

/**
 * This class represents the parameters of a Content Analytics Query abstracting the complexity
 * of the underlying JSON format. The simplified REST Endpoint and the Content Analytics ViewTool
 * use this class so that parameters can be entered in a more user-friendly way.
 *
 * @author Jose Castro
 * @since Nov 28th, 2024
 */
@JsonDeserialize(builder = ContentAnalyticsQuery.Builder.class)
public class ContentAnalyticsQuery implements Serializable {

    public static final String MEASURES_ATTR = "measures";
    public static final String DIMENSIONS_ATTR = "dimensions";
    public static final String TIME_DIMENSIONS_ATTR = "timeDimensions";
    public static final String TIME_DIMENSIONS_DIMENSION_ATTR = "dimension";
    public static final String FILTERS_ATTR = "filters";
    public static final String ORDER_ATTR = "order";
    public static final String LIMIT_ATTR = "limit";
    public static final String OFFSET_ATTR = "offset";
    public static final String GRANULARITY_ATTR = "granularity";
    public static final String DATE_RANGE_ATTR = "dateRange";
    public static final String MEMBER_ATTR = "member";
    public static final String OPERATOR_ATTR = "operator";
    public static final String VALUES_ATTR = "values";

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

    private static final String SEPARATOR = COLON;

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

    /**
     * This builder creates the appropriate data structures that match the JSON format of the final
     * CubeJS query.
     */
    public static class Builder {

        private Set<String> measures;
        private Set<String> dimensions;
        private final List<Map<String, String>> timeDimensions = new ArrayList<>();
        private final List<Map<String, Object>> filters = new ArrayList<>();
        private final List<String[]> order = new ArrayList<>();
        private int limit = 1000;
        private int offset = 0;

        /**
         * The measures parameter contains a set of measures and each measure is an aggregation over
         * a certain column in your ClickHouse database table.
         *
         * @param measures A string with the measures separated by a space.
         *
         * @return The builder instance.
         */
        public Builder measures(final String measures) {
            this.measures = Set.of(measures.split("\\s+"));
            return this;
        }

        /**
         * The dimensions property contains a set of dimensions. You can think about a dimension as
         * an attribute related to a measure, e.g. the measure user_count can have dimensions like
         * country, age, occupation, etc.
         *
         * @param dimensions A string with the dimensions separated by a space.
         *
         * @return The builder instance.
         */
        public Builder dimensions(final String dimensions) {
            this.dimensions = Set.of(dimensions.split("\\s+"));
            return this;
        }

        /**
         * Time dimensions provide a convenient way to specify a time dimension with a filter. It is
         * an array of objects in timeDimension format. If no date range is provided, the default
         * value will be "Last week".
         *
         * @param timeDimensions A string with the time dimensions separated by a colon.
         *
         * @return The builder instance.
         */
        public Builder timeDimensions(final String timeDimensions) {
            if (UtilMethods.isNotSet(timeDimensions)) {
                return this;
            }
            final String[] timeParams = timeDimensions.split(SEPARATOR);
            final Map<String, String> timeDimensionsData = new HashMap<>();
            timeDimensionsData.put(TIME_DIMENSIONS_DIMENSION_ATTR, timeParams[0]);
            if (timeParams.length > 2) {
                timeDimensionsData.put(GRANULARITY_ATTR, timeParams[1]);
                timeDimensionsData.put(DATE_RANGE_ATTR, timeParams[2]);
            } else if (timeParams.length > 1) {
                timeDimensionsData.put(DATE_RANGE_ATTR, timeParams[1]);
            } else {
                timeDimensionsData.put(DATE_RANGE_ATTR, "Last week");
            }
            this.timeDimensions.add(timeDimensionsData);
            return this;
        }

        /**
         * Filters are applied differently to dimensions and measures. When you filter on a
         * dimension, you are restricting the raw data before any calculations are made. When you
         * filter on a measure, you are restricting the results after the measure has been
         * calculated. They are composed of: member, operator, and values.
         *
         * @param filters A string with the filters separated by a colon.
         *
         * @return The builder instance.
         */
        public Builder filters(final String filters) {
            if (UtilMethods.isNotSet(filters)) {
                return this;
            }
            final String[] filterArr = filters.split(SEPARATOR);
            for (final String filter : filterArr) {
                final String[] filterParams = filter.split("\\s+");
                final Map<String, Object> filterDataMap = new HashMap<>();
                filterDataMap.put(MEMBER_ATTR, filterParams[0]);
                filterDataMap.put(OPERATOR_ATTR, filterParams[1]);
                final String[] filterValues = filterParams[2].split(COMMA);
                filterDataMap.put(VALUES_ATTR, filterValues);
                this.filters.add(filterDataMap);
            }
            return this;
        }

        /**
         * This is an object where the keys are measures or dimensions to order by and their
         * corresponding values are either asc or desc. The order of the fields to order on is based
         * on the order of the keys in the object. If not provided, default ordering is applied. If
         * an empty object ([]) is provided, no ordering is applied.
         *
         * @param order A string with the order separated by a colon.
         *
         * @return The builder instance.
         */
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

        /**
         * A row limit for your query.
         *
         * @param limit The number of rows to limit the query. The default value is 1000.
         *
         * @return The builder instance.
         */
        public Builder limit(final int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * The number of initial rows to be skipped for your query. The default value is 0.
         *
         * @param offset The number of rows to skip.
         *
         * @return The builder instance.
         */
        public Builder offset(final int offset) {
            this.offset = offset;
            return this;
        }


        /**
         * This method builds the ContentAnalyticsQuery object based on all the specified
         * parameters for the query.
         *
         * @return The {@link ContentAnalyticsQuery} object.
         */
        public ContentAnalyticsQuery build() {
            return new ContentAnalyticsQuery(this);
        }

        /**
         * This method builds the ContentAnalyticsQuery object based on all the specified
         * parameters in the provided map.
         *
         * @param form A {@link Map} containing the query data.
         *
         * @return The {@link ContentAnalyticsQuery} object.
         */
        public ContentAnalyticsQuery build(final Map<String, Object> form) {
            this.measures((String) form.get(MEASURES_ATTR));
            this.dimensions((String) form.get(DIMENSIONS_ATTR));
            this.timeDimensions((String) form.get(TIME_DIMENSIONS_ATTR));
            this.filters((String) form.get(FILTERS_ATTR));
            this.order((String) form.get(ORDER_ATTR));
            this.limit((Integer) form.get(LIMIT_ATTR));
            this.offset((Integer) form.get(OFFSET_ATTR));
            return new ContentAnalyticsQuery(this);
        }

    }

}
