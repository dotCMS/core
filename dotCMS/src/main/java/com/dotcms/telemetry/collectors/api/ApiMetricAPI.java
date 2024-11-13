package com.dotcms.telemetry.collectors.api;

import java.util.Collection;
import java.util.Map;

/**
 * A utility class to interact with the metric_temporally_table table, providing methods to save or
 * flush data. This class encapsulates the logic for saving and flushing data into the
 * metric_temporally_table.
 * <p>
 * The metric_temporally_table is a special table designed to store the request to the endpoints we
 * wish to track. Later, the data in this table is summarized and stored as part of the
 * MetricSnapshot.
 */
public interface ApiMetricAPI {

    /**
     * Return all the summary from the temporal table
     *
     * @return Collection of Maps with the summary data.
     *
     * @see ApiMetricFactory
     * @see ApiMetricAPI
     */
    Collection<Map<String, Object>> getMetricTemporaryTableData();

    /**
     * Save an Endpoint request to the metric_temporally_table.
     * <p>
     * This is saved on an async way.
     *
     * @param apiMetricType Metric to be saved
     * @param request       Request data
     */
    void save(final ApiMetricType apiMetricType, final ApiMetricWebInterceptor.RereadInputStreamRequest request);

    /**
     * Before beginning to collect endpoint requests, this function must be called. It saves a
     * starting register to the metric_temporally_table, indicating the initiation of data
     * collection
     */
    void startCollecting();

    void flushTemporaryTable();

    /**
     * Create the metrics_temp table
     */
    void createTemporaryTable();

    /**
     * Drop the metrics_temp table
     */
    void dropTemporaryTable();

}
