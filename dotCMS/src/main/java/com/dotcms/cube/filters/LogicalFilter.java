package com.dotcms.cube.filters;

import com.dotcms.cube.filters.SimpleFilter.Operator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a Boolean or Logical Operator to be used in a CubeJS Filters Query.
 *
 * Example how it could be used:
 *
 * <code>
 *        final CubeJSQuery cubeJSQuery = new Builder()
 *                 .dimensions("Events.experiment", "Events.variant")
 *                 .filter(
 *                         LogicalFilter.Builder.or()
 *                                 .add("Events.variant", SimpleFilter.Operator.EQUALS, "B")
 *                                 .add("Events.experiment", SimpleFilter.Operator.EQUALS, "B")
 *                                 .build()
 *                 )
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
 *   filters: [
 *      {
 *          "or": [
 *              {
 *                  member: "Events.variant",
 *                  operator: "equals",
 *                  values: ["B"]
 *              },
 *              {
 *                  member: "Events.experiment",
 *                  operator: "equals",
 *                  values: ["B"]
 *              }
 *          ]
 *     }
 *   ]
 * }
 * </code>
 *
 * @see <a href="https://cube.dev/docs/query-format#boolean-logical-operators">Boolean logical operator</>
 */
public class LogicalFilter implements Filter {

    public static enum Type {
        AND, OR;
    }

    private Type type;
    private Filter[] filters;
    private LogicalFilter(final Type type, final Filter[] filters) {
        this.type = type;
        this.filters = filters;
    }

    @Override
    public Map<String, Object> asMap() {
        final String logicalOperator = type.name().toLowerCase();

        final Map<String, Object> map = new HashMap<>();

        map.put(logicalOperator,
                Arrays.stream(filters)
                        .map(filter -> filter.asMap())
                        .collect(Collectors.toList()));

        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogicalFilter that = (LogicalFilter) o;
        return type == that.type && Arrays.equals(filters, that.filters);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type);
        result = 31 * result + Arrays.hashCode(filters);
        return result;
    }

    public static class Builder {
        private Type type;
        private List<Filter> filters = new ArrayList<>();

        private Builder(final Type type){
            this.type = type;
        }

        public Builder add(final String member, Operator operator, final String... values){
            filters.add(new SimpleFilter(member, operator, values));
            return this;
        }

        public Builder add(final SimpleFilter filter){
            filters.add(filter);
            return this;
        }

        public Builder add(final LogicalFilter logicalFilter){
            filters.add(logicalFilter);
            return this;
        }

        public LogicalFilter build(){
            return new LogicalFilter(type, filters.toArray(new Filter[filters.size()]));
        }

        public static Builder and(){
            return new Builder(Type.AND);
        }

        public static Builder or(){
            return new Builder(Type.OR);
        }
    }
}
