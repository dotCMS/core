package com.dotcms.telemetry.collectors.api;

import com.dotmarketing.exception.DotDataException;

import java.util.Collection;
import java.util.Map;

/**
 * A utility class to interact with the metrics_temp table, providing the more basic methods to
 * create/drop the table or save/flush data.
 * <p>
 * This class encapsulates the queries for saving/flushing data into the metrics_temp and
 * creating/dropping the whole table.
 * <p>
 * The metrics_temp is a special table designed to store the request to the endpoints we wish to
 * track. Later, the data in this table is summarized and stored as part of the MetricSnapshot.
 */
public interface ApiMetricFactory {

    String GET_DATA_FROM_TEMPORARY_METRIC_TABLE =
            "SELECT " +
                    "metric_type->>'feature' as feature, " +
                    "metric_type->>'category' as category, " +
                    "metric_type->>'name' as name, " +
                    "COUNT(*) / %1$.2f AS average_per_hour, " +
                    "COUNT(distinct hash) / %1$.2f AS unique_average_per_hour " +
                    "FROM metrics_temp " +
                    "GROUP BY metric_type->>'feature', " +
                    "metric_type->>'category', " +
                    "metric_type->>'name' " +
                    "HAVING metric_type->>'name' IS NOT null";
    String OVERALL_QUERY = "SELECT (EXTRACT(epoch FROM now()) - EXTRACT" +
            "(epoch FROM MIN(timestamp)))/3600 " +
            "as overall FROM metrics_temp";

    /**
     * Save request on the metrics_temp
     *
     * @param apiMetricRequest request
     */
    void save(final ApiMetricRequest apiMetricRequest);

    /**
     * Save a register with just the current time as timestamp, it is used to mark when we start
     * collecting the data.
     */
    void saveStartEvent();

    /**
     * Drop all the registers on the table
     */
    void flushTemporaryTable();

    /**
     * Create the metrics_temp table
     *
     * @throws DotDataException if something wrong happened
     */
    void createTemporaryTable() throws DotDataException;

    /**
     * Drop the metrics_temp table
     *
     * @throws DotDataException if something wrong happened
     */
    void dropTemporaryTable() throws DotDataException;

    /**
     * Return all the summary from the temporal table
     *
     * @return a collection of maps with the summary data.
     *
     * @see ApiMetricFactory
     * @see ApiMetricAPI
     */
    Collection<Map<String, Object>> getMetricTemporaryTableData();

}
