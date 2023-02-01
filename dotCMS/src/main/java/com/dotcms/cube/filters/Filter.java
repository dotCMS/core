package com.dotcms.cube.filters;

import java.util.Map;

/**
 * Represents a CubeJs Query Filter
 *
 * @see <a href="https://cube.dev/docs/query-format#filters-formate">Filters format</a>
 */
public interface Filter {

    static LogicalFilter.Builder and(){
        return null;
    }

    Map<String, Object> asMap();

    public enum Order {
        ASC, DESC;
    }
}
