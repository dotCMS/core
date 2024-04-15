package com.dotcms.cube.filters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a Filter to a CubeJS Query with the following properties:
 *
 * <ul>
 *     <li>member: Dimensions or measure to be used in the filter.</li>
 *     <li>operator: Any values in {@link Operator}</li>
 *     <li>values: An Array of values for the filter</li>
 * </ul>
 *
 * Example:
 *
 * <code>
 *         final CubeJSQuery cubeJSQuery = new Builder()
 *                 .dimensions("Events.experiment")
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
 * @see <a href="https://cube.dev/docs/query-format#filters-format">Filters Format</>
 */
public class SimpleFilter implements Filter {
    private String member;
    private Operator operator;
    private Object[] values;

    public SimpleFilter(final String member, final Operator operator, final Object[] values) {
        this.member = member;
        this.operator = operator;
        this.values = values;
    }

    public String getMember() {
        return member;
    }

    public Operator getOperator() {
        return operator;
    }

    public String[] getValues() {
        if (values == null) {
            return new String[0];
        }

        return Arrays.stream(values).map(Object::toString).toArray(String[]::new);
    }

    @Override
    public Map<String, Object> asMap() {

        final Map<String, Object> map = new HashMap<>();

        map.put("member", member);
        map.put("operator", operator.getKey());
        map.put("values", values);

        return map;
    }

    public enum Operator {
        EQUALS("equals"), NOT_EQUALS("notEquals"), CONTAINS("contains"), NOT_CONTAINS("notContains");

        private String key;
        Operator(final String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleFilter that = (SimpleFilter) o;
        return member.equals(that.member) && operator == that.operator && Arrays.equals(
                values, that.values);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(member, operator);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }
}
