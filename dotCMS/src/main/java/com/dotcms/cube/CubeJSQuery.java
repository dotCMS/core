package com.dotcms.cube;



import com.dotcms.cube.filters.Filter.Order;
import com.dotcms.cube.filters.LogicalFilter;
import com.dotcms.cube.filters.SimpleFilter;
import com.dotcms.cube.filters.SimpleFilter.Operator;
import com.dotcms.cube.filters.Filter;
import com.dotcms.util.JsonUtil;
import com.dotmarketing.util.UtilMethods;
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
        final Map<String, String> resultMap = new HashMap();

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
        private List<Filter> filters = new ArrayList<>();
        private List<OrderItem> orders = new ArrayList<>();

        public CubeJSQuery build(){
            if (!UtilMethods.isSet(dimensions) && !UtilMethods.isSet(measures)) {
                throw new IllegalStateException("Must set dimensions or measures");
            }

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
    }
}
