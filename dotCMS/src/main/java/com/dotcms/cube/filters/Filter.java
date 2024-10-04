package com.dotcms.cube.filters;

import java.util.Map;

/**
 * Represents a CubeJs Query Filter
 *
 * @see <a href="https://cube.dev/docs/query-format#filters-formate">Filters format</a>
 *
 * @author Freddy Rodriguez
 * @since Jan 27th, 2023
 */
public interface Filter {

    static LogicalFilter.Builder and(){
        return null;
    }

    Map<String, Object> asMap();

    enum Order {
        ASC, DESC;
    }

}
