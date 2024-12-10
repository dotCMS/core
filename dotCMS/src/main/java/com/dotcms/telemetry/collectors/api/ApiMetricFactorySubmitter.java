package com.dotcms.telemetry.collectors.api;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;

/**
 * Utility class for submitting a metric to be stored on the Telemetry Endpoint. All the request are
 * send on async way.
 */
public enum ApiMetricFactorySubmitter {

    INSTANCE;

    static final int API_METRIC_SUBMITTER_POOL_SIZE = Config.getIntProperty(
            "API_METRIC_SUBMITTER_POOL_SIZE", 1);
    static final int API_METRIC_SUBMITTER_MAX_POOL_SIZE = Config.getIntProperty(
            "API_METRIC_SUBMITTER_MAX_POOL_SIZE", 40);

    private DotSubmitter submitter;

    ApiMetricFactorySubmitter() {

    }

    /**
     * Start the Submitter to be ready to send Metric
     */
    public void start() {
        final String submitterName = "MetricApiHitStorageSubmitter" + Thread.currentThread().getName();

        submitter = DotConcurrentFactory.getInstance().getSubmitter(submitterName,
                new DotConcurrentFactory.SubmitterConfigBuilder()
                        .poolSize(API_METRIC_SUBMITTER_POOL_SIZE)
                        .maxPoolSize(API_METRIC_SUBMITTER_MAX_POOL_SIZE)
                        .queueCapacity(Integer.MAX_VALUE)
                        .build()
        );

    }

    /**
     * Request the telemetry endpoint asynchronously.
     *
     * @param metricAPIRequest request
     */
    public void saveAsync(final ApiMetricRequest metricAPIRequest) {
        submitter.submit(() -> save(metricAPIRequest));
    }

    @WrapInTransaction
    private void save(final ApiMetricRequest metricAPIRequest) {
        try {
            CDIUtils.getBeanThrows(ApiMetricFactory.class).save(metricAPIRequest);
        } catch (final Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Stop the submitter.
     */
    public void shutdownNow() {
        submitter.shutdownNow();
    }

}
