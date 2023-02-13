package com.dotcms.cube;



import com.dotcms.cube.filters.Filter.Order;
import com.dotcms.cube.filters.LogicalFilter;
import com.dotcms.cube.filters.SimpleFilter;
import com.dotcms.cube.filters.SimpleFilter.Operator;
import com.dotcms.cube.filters.Filter;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Represents a Cube JS Query
 * You can use the {@link Builder} to create a CubeJSQuery and later using the
 * {@link CubeJSQuery#toString()}.
 *
 * Examples:
 *
 * <code>
 *    final CubeJSQuery cubeJSQuery = new Builder()
 *                 .dimensions("Events.experiment")
 *                 .measures("Events.count")
 *                 .filter("Events.variant", SimpleFilter.Operator.EQUALS, "B")
 *                 .build();
 * </code>
 *
 * To get:
 *
 * <code>
 *   {
 *   "dimensions": [
 *     "Events.experiment"
 *   ],
 *   {
 *   "measures": [
 *     "Events.count"
 *   ],
 *   filters: [
 *      {
 *          member: "Events.variant",
 *          operator: "equals",
 *          values: ["B"]
 *      }
 *   ]
 * }
 * </code>
 *
 * @see <a href="https://cube.dev/docs/query-format">CubeJS Query format</a>
 */
public class CubeJSQuery {

    private String[] dimensions;
    private String[] measures;
    private Filter[] filters;

    private OrderItem[] orders;

    private CubeJSQuery(final String[] dimensions,
            final String[] measures,
            final Filter[] filters,
            final OrderItem[] orderItems) {

        this.dimensions = dimensions;
        this.measures = measures;
        this.filters = filters;
        this.orders = orderItems;
    }

    @Override
    public String toString() {
        if (!UtilMethods.isSet(dimensions) && !UtilMethods.isSet(measures)) {
            throw new IllegalStateException("Must set dimensions or measures");
        }

        try {
            return JsonUtil.getJsonAsString(getMap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
                .map(filter -> filter.asMap())
                .collect(Collectors.toList());
    }

    public static class Builder {
        private String[] dimensions;
        private String[] measures;

        private Collection<Filter> filters = new ArrayList<>();
        private Collection<OrderItem> orders = new ArrayList<>();

        /**
         * Merge two {@link CubeJSQuery}, each section of the Query is merge ignoring duplicated values
         * for example If we have:
         *
         * Query 1:
         * <code>
         *     {
         *         "dimensions": [
         *              "Events.Experiment",
         *              "Events.variant"
         *         ]
         *     }
         * </code>
         *
         * * Query 2:
         * Query 1:
         * <code>
         *     {
         *         "dimensions": [
         *              "Events.Experiment",
         *              "Events.eventType"
         *         ]
         *     }
         * </code>
         *
         * The result is going to be:
         *
         * <code>
         *     {
         *         "dimensions": [
         *              "Events.Experiment",
         *              "Events.variant",
         *              "Events.eventType"
         *         ]
         *     }
         * </code>
         *
         * The same happens with others section like: measures, filters and order.
         *
         * @param cubeJSQuery1
         * @param cubeJSQuery2
         * @return
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

            return new Builder()
                    .dimensions(dimensionsMerged)
                    .measures(measuresMerged)
                    .filters(filtersMerged)
                    .orders(ordersMerged)
                    .build();
        }

        @NotNull
        private static <T> List<T> getEmptyIfIsNotSet(T[] array) {
            return UtilMethods.isSet(array) ? Arrays.asList(array) : Collections.emptyList();
        }

        private Builder dimensions(final Collection<String> dimensions) {
            this.dimensions = dimensions.toArray(new String[dimensions.size()]);
            return this;
        }

        private Builder measures(final Collection<String> measures) {
            this.measures = measures.toArray(new String[measures.size()]);
            return this;
        }

        private Builder orders(final Collection<OrderItem> orders) {
            this.orders = orders;
            return this;
        }

        private Builder filters(final Collection<Filter> filters) {
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

            return new CubeJSQuery(dimensions, measures,
                    filters.toArray(new Filter[filters.size()]),
                    orders.toArray(new OrderItem[orders.size()]));
        }

        public Builder dimensions(final String... dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder measures(final String... measures) {
            this.measures = measures;
            return this;
        }

        public Builder filter(final String member, Operator operator, final String... values) {
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
    }

    private static class OrderItem {
        private String orderBy;
        private Order order;

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
}
