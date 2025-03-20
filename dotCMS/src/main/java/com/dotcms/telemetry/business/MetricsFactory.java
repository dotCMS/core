package com.dotcms.telemetry.business;

import com.dotmarketing.exception.DotDataException;

import java.util.List;
import java.util.Optional;

/**
 * This class provides low-level database access to Metrics-related information.
 */
public interface MetricsFactory {

    Optional<Object> getValue(final String sqlQuery) throws DotDataException;

    /**
     * Execute a query that returns a list of String objects. The query should retrieve a field
     * called value, as shown in the example below:
     * <pre>
     * {@code
     * SELECT identifier as value FROM template
     * }
     * </pre>
     * <p>
     * This method will iterate through the results returned by the query and use the values from
     * the value field to create a Collection of Strings.
     *
     * @param sqlQuery the query to be executed.
     *
     * @return a Collection of Strings with the values returned by the query.
     *
     * @throws DotDataException if an error occurs while executing the query.
     */
    Optional<List<String>> getList(final String sqlQuery) throws DotDataException;

    int getSchemaDBVersion() throws DotDataException;

}
