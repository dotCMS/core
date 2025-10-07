package com.dotcms.cube;

import com.dotcms.cube.filters.Filter;
import com.dotcms.cube.filters.Filter.Order;
import com.dotcms.cube.filters.LogicalFilter;
import com.dotcms.cube.filters.SimpleFilter;
import com.dotcms.cube.filters.SimpleFilter.Operator;
import com.dotcms.util.DotPreconditions;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a CubeJS Query. You can use the {@link Builder} to create a CubeJSQuery, and then,
 * call the {@link CubeJSQuery#toString()} method to generate it. For instance, you can have the
 * following Java code:
 * <pre>
 *    {@code
 *    final CubeJSQuery cubeJSQuery = new Builder()
 *                 .dimensions("Events.experiment")
 *                 .measures("Events.count")
 *                 .filter("Events.variant", SimpleFilter.Operator.EQUALS, "B")
 *                 .build();
 *    }
 * </pre>
 * To generate this CubeJS query:
 * <pre>
 * {@code
 *   {
 *     "dimensions": [
 *       "Events.experiment"
 *     ],
 *     {
 *       "measures": [
 *         "Events.count"
 *       ],
 *       filters: [
 *         {
 *           member: "Events.variant",
 *           operator: "equals",
 *           values: ["B"]
 *         }
 *       ]
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * For more information on the CbeJS query format, please refer to the <a
 * href="https://cube.dev/docs/query-format">official CubeJS Query format documentation.</a>
 */
public class CubeJSQuery {
    final static ObjectMapper jsonMapper = new ObjectMapper();

    private final String[] dimensions;
    private final String[] measures;
    private final Filter[] filters;

    private final OrderItem[] orders;
    private final TimeDimension[] timeDimensions;

    private final long limit;
    private final long offset;

    private CubeJSQuery(final Builder builder) {
        this.dimensions = builder.dimensions;
        this.measures = builder.measures;
        this.filters = builder.filters.toArray(new Filter[0]);
        this.orders = builder.orders.toArray(new OrderItem[0]);
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.timeDimensions = builder.timeDimensions.toArray(new TimeDimension[0]);
    }

    @Override
    public String toString() {
        if (!UtilMethods.isSet(dimensions) && !UtilMethods.isSet(measures)) {
            throw new IllegalStateException("The 'dimensions' and 'measures' parameters must be set");
        }
        try {
            return JsonUtil.getJsonAsString(getMap());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the CubeJS Query as a Map composed of its different parameters. Keep in mind that
     * this is used for both generating the actual CubeJS query, and  the JSON data as String.
     *
     * @return The CubeJS Query as a Map of attributes and their values.
     */
    private Map<String, Object> getMap() {
        final Map<String, Object> map = new HashMap<>();

        if (UtilMethods.isSet(dimensions)) {
            map.put("dimensions", dimensions);
        }

        if (UtilMethods.isSet(measures)) {
            map.put("measures", measures);
        }

        if (filters.length > 0) {
            map.put("filters", getFiltersAsMap());
        }

        if (orders.length > 0) {
            map.put("order", getOrdersAsMap());
        }

        if (limit > 0) {
            map.put("limit", limit);
        }

        if (offset >= 0) {
            map.put("offset", offset);
        }

        if (timeDimensions.length > 0) {
            final Set<Map<String, Object>> correctedTimeDimensions = Arrays.stream(this.timeDimensions)
                    .map(timeDimension -> {
                        final Map<String, Object> dataMap = new LinkedHashMap<>();
                        dataMap.put("dimension", timeDimension.getDimension());
                        dataMap.put("granularity", timeDimension.getGranularity());
                        if (UtilMethods.isSet(timeDimension.getDateRange())) {
                            // If the 'dateRange' parameter is not set, then it must NOT be
                            // part of the data map, or the CubeJS query will fail
                            dataMap.put("dateRange", timeDimension.getDateRange());
                        }
                        return dataMap;
                    }).collect(Collectors.toSet());
            map.put("timeDimensions", correctedTimeDimensions);
        }

        return map;
    }

    private Map<String, String> getOrdersAsMap() {

        final Map<String, String> resultMap = new LinkedHashMap<>();

        for (final OrderItem order : orders) {
            resultMap.put(order.orderBy, order.order.name().toLowerCase());
        }

        return resultMap;
    }
    private List<Map<String, Object>> getFiltersAsMap() {
        return Arrays.stream(filters)
                .map(Filter::asMap)
                .collect(Collectors.toList());
    }

    public Filter[] filters() {
        return filters;
    }

    public OrderItem[] orders() {
        return orders;
    }

    public String[] dimensions() {
        return dimensions;
    }

    public String[] measures() {
        return measures;
    }

    public long limit() {
        return limit;
    }

    public long offset() {
        return offset;
    }

    public TimeDimension[] timeDimensions() {
        return timeDimensions;
    }

    public CubeJSQuery.Builder builder() {
        final Builder builder = new Builder()
                .dimensions(dimensions)
                .measures(measures)
                .filters(Arrays.asList(filters))
                .orders(Arrays.asList(orders));

        if (limit > 0) {
            builder.limit(limit);
        }

        if (offset > 0) {
            builder.offset(offset);
        }

        return builder;
    }

    public static class Builder {
        private String[] dimensions;
        private String[] measures;

        private Collection<Filter> filters = new ArrayList<>();
        private Collection<OrderItem> orders = new ArrayList<>();
        private long limit = -1;
        private long offset = -1;
        private final List<TimeDimension> timeDimensions = new ArrayList<>();

        /**
         * Merges two {@link CubeJSQuery} objects. Each section of the Query is merged by ignoring
         * duplicate values. For instance, if we have the following queries:
         * <pre>
         * Query #1:
         * {@code
         *     {
         *         "dimensions": [
         *              "Events.Experiment",
         *              "Events.variant"
         *         ]
         *     }
         * }
         * </pre>
         * <pre>
         * Query #2:
         * {@code
         *     {
         *         "dimensions": [
         *              "Events.Experiment",
         *              "Events.eventType"
         *         ]
         *     }
         * }
         * </pre>
         * The result is going to be:
         * <pre>
         * {@code
         *     {
         *         "dimensions": [
         *              "Events.Experiment",
         *              "Events.variant",
         *              "Events.eventType"
         *         ]
         *     }
         * }
         * </pre>
         * The same will happen with others section such as: measures, filters and order.
         *
         * @param cubeJSQuery1 The first {@link CubeJSQuery} to merge.
         * @param cubeJSQuery2 The second {@link CubeJSQuery} to merge.
         *
         * @return A new {@link CubeJSQuery} object with the merged values.
         */
        public static CubeJSQuery merge(final CubeJSQuery cubeJSQuery1, final CubeJSQuery cubeJSQuery2) {
            final Collection<String> dimensionsMerged = merge(
                    getEmptyIfIsNotSet(cubeJSQuery1.dimensions),
                    getEmptyIfIsNotSet(cubeJSQuery2.dimensions)
            );

            final Collection<String> measuresMerged = merge(
                    getEmptyIfIsNotSet(cubeJSQuery1.measures),
                    getEmptyIfIsNotSet(cubeJSQuery2.measures)
            );

            final Collection<Filter> filtersMerged = merge(
                    getEmptyIfIsNotSet(cubeJSQuery1.filters),
                    getEmptyIfIsNotSet(cubeJSQuery2.filters)
            );

            final Collection<OrderItem> ordersMerged = merge(
                    getEmptyIfIsNotSet(cubeJSQuery1.orders),
                    getEmptyIfIsNotSet(cubeJSQuery2.orders)
            );

            final Collection<TimeDimension> timeDimension = merge(
                    getEmptyIfIsNotSet(cubeJSQuery1.timeDimensions),
                    getEmptyIfIsNotSet(cubeJSQuery2.timeDimensions)
            );

            return new Builder()
                    .dimensions(dimensionsMerged)
                    .measures(measuresMerged)
                    .filters(filtersMerged)
                    .orders(ordersMerged)
                    .timeDimensions(timeDimension)
                    .build();
        }

        @NotNull
        private static <T> List<T> getEmptyIfIsNotSet(T[] array) {
            return UtilMethods.isSet(array) ? Arrays.asList(array) : Collections.emptyList();
        }

        public Builder dimensions(final Collection<String> dimensions) {
            this.dimensions = dimensions.toArray(new String[0]);
            return this;
        }

        public Builder measures(final Collection<String> measures) {
            this.measures = measures.toArray(new String[0]);
            return this;
        }

        public Builder orders(final Collection<OrderItem> orders) {
            this.orders = orders;
            return this;
        }

        public Builder filters(final Collection<Filter> filters) {
            this.filters = filters;
            return this;
        }

        private static <T> Collection<T> merge(final Collection<T> collection1, final Collection<T> collection2) {
            final Map<T, Boolean> map = new LinkedHashMap<>();

            final Iterable<T> all =
                    Iterables.unmodifiableIterable(
                            Iterables.concat(collection1, collection2));

            for (T item : all) {
                map.put(item, Boolean.TRUE);
            }

            return map.keySet();
        }

        public CubeJSQuery build(){

            return new CubeJSQuery(this);
        }

        public Builder dimensions(final String... dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder measures(final String... measures) {
            this.measures = measures;
            return this;
        }

        public Builder filter(final String member, Operator operator, final Object... values) {
            filters.add(new SimpleFilter(member, operator, values));
            return this;
        }

        public Builder filter(final LogicalFilter logicalFilter) {
            filters.add(logicalFilter);
            return this;
        }

        public Builder order(final String orderBy, final Order order) {
            orders.add(new OrderItem(orderBy, order));
            return this;
        }

        public Builder limit(final long limit) {

            DotPreconditions.checkArgument(limit >= 0, "Limit must be greater than or equal to 0");
            DotPreconditions.checkArgument(limit <= 50000, "Limit must be less than or equal to 50000");

            this.limit = limit;
            return this;
        }

        public Builder offset(final long offset) {
            DotPreconditions.checkArgument(offset >= 0, "Offset must be greater than or equal to 0");
            this.offset = offset;
            return this;
        }

        public Builder timeDimension(final String dimension, final String granularity) {
            return timeDimension(dimension, granularity, null);
        }

        public Builder timeDimension(final String dimension, final String granularity, final String dateRange) {
            this.timeDimensions.add(new TimeDimension(dimension, granularity, dateRange));
            return this;
        }

        public Builder timeDimensions(Collection<TimeDimension> timeDimensions) {
            this.timeDimensions.addAll(timeDimensions);
            return this;
        }
    }

    public static class TimeDimension {
        String dimension;
        String granularity;
        String dateRange;

        public TimeDimension(final String dimension,
                             final String granularity,
                             final String dateRange) {
            this.dimension = dimension;
            this.granularity = granularity;
            this.dateRange = dateRange;
        }

        public String getDimension() {
            return dimension;
        }

        public String getGranularity() {
            return granularity;
        }

        public String getDateRange() {
            return dateRange;
        }
    }

    public static class OrderItem {
        private final String orderBy;
        private final Order order;

        public OrderItem(final String orderBy, final Order order) {
            this.orderBy = orderBy;
            this.order = order;
        }

        public String getOrderBy() {
            return orderBy;
        }

        public Order getOrder() {
            return order;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OrderItem orderItem = (OrderItem) o;
            return Objects.equals(orderBy, orderItem.orderBy) && order == orderItem.order;
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderBy, order);
        }
    }

    public static Optional<String> extractSiteId(final String cubeJsQueryJson) {

        if (!cubeJsQueryJson.contains("\"request.siteId\"")) {
            return Optional.empty();
        }

        final JsonNode root;
        try {
            root = jsonMapper.readTree(cubeJsQueryJson);

            final JsonNode filters = root.path("filters");

            if (filters.isArray()) {

                for (JsonNode filter : filters) {
                    JsonNode member = filter.get("member");

                    if (member != null && "request.siteId".equals(member.asText())) {
                        JsonNode values = filter.get("values");

                        if (values != null && values.isArray() && values.size() > 0) {
                            return Optional.ofNullable(values.get(0).asText());
                        }
                    }
                }
            }
            return Optional.empty();
        } catch (JsonProcessingException e) {
            Logger.debug(CubeJSQuery.class,
                    () -> "Error trying to extract the Site Id from a CubeJS query: " + e.getMessage());
            return Optional.empty();
        }
    }
}
