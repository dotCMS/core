package com.dotcms.experience.collectors.api;

import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotRuntimeException;

import java.time.Instant;
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
public class ApiMetricAPI {

    final RequestHashCalculator requestHashCalculator = new RequestHashCalculator();

    /**
     * Return all the summary from the temporal table
     *
     * @return Collection of Maps with the summary data.
     *
     * @see ApiMetricFactory
     * @see ApiMetricAPI
     */
    public static Collection<Map<String, Object>> getMetricTemporaryTableData() {
        try {
            return ApiMetricFactory.INSTANCE.getMetricTemporaryTableData();
        } finally {
            HibernateUtil.closeSessionSilently();
        }
    }

    /**
     * Save an Endpoint request to the metric_temporally_table.
     * <p>
     * This is saved on an async way.
     *
     * @param apiMetricType Metric to be saved
     * @param request       Request data
     */
    public void save(final ApiMetricType apiMetricType,
                     final ApiMetricWebInterceptor.RereadInputStreamRequest request) {

        final String requestHash = requestHashCalculator.calculate(apiMetricType, request);
        final ApiMetricRequest metricAPIHit = new ApiMetricRequest.Builder()
                .setMetric(apiMetricType.getMetric())
                .setTime(Instant.now())
                .setHash(requestHash)
                .build();

        ApiMetricFactorySubmitter.INSTANCE.saveAsync(metricAPIHit);
    }

    /**
     * Before beginning to collect endpoint requests, this function must be called. It saves a
     * starting register to the metric_temporally_table, indicating the initiation of data
     * collection
     */
    public void startCollecting() {
        try {
            LocalTransaction.wrap(ApiMetricFactory.INSTANCE::saveStartEvent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void flushTemporalTable() {
        try {
            LocalTransaction.wrap(() -> {
                ApiMetricFactory.INSTANCE.flushTemporaryTable();
                startCollecting();
            });
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Create the metrics_temp table
     */
    public void createTemporaryTable() {
        try {
            LocalTransaction.wrap(ApiMetricFactory.INSTANCE::createTemporaryTable);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Drop the metrics_temp table
     */
    public void dropTemporaryTable() {
        try {
            LocalTransaction.wrap(ApiMetricFactory.INSTANCE::dropTemporaryTable);
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

}
