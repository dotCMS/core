package com.dotcms.metrics;

import com.dotcms.analytics.metrics.AnalyticsAppPayload;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotConcurrentFactory.SubmitterConfig;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.jitsu.EventLogRunnable;
import com.dotcms.jitsu.EventsPayload;
import com.dotmarketing.util.Config;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Wrapper class to a {@link DotSubmitter} instance used to execute instances of {@link MetricsPayloadSenderRunnable}
 * <p>
 * It's configuration can be modified via the following properties:
 * <li>METRICS_SENDER_POSTING_THREADS: Initial active posting threads
 * <li>METRICS_SENDER_POSTING_THREADS_MAX: Max active posting threads
 * <li>METRICS_SENDER_QUEUE_SIZE: Max size of the queue
 *
 * @author vico
 */
public class MetricsSenderSubmitter {

    private final SubmitterConfig submitterConfig;

    public MetricsSenderSubmitter() {
        submitterConfig = new DotConcurrentFactory.SubmitterConfigBuilder()
            .poolSize(Config.getIntProperty("METRICS_SENDER_POSTING_THREADS", 8))
            .maxPoolSize(Config.getIntProperty("METRICS_SENDER_POSTING_THREADS_MAX", 16))
            .keepAliveMillis(1000)
            .queueCapacity(Config.getIntProperty("METRICS_SENDER_QUEUE_SIZE", 10000))
            .rejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
            .build();
    }

    /**
     * Sends metrics to analytics.
     *
     * @param analyticsAppPayload app to be used to resolve analytics url plus the payload
     */
    public void sendMetrics(final AnalyticsAppPayload<String> analyticsAppPayload) {
        DotConcurrentFactory
                .getInstance()
                .getSubmitter("metrics-send-posting", submitterConfig)
                .execute(
                        new MetricsPayloadSenderRunnable(
                                analyticsAppPayload.analyticsApp(),
                                analyticsAppPayload.payload()));
    }

    /**
     * Sends event to analytics.
     *
     * @param analyticsAppPayload app to be used to resolve analytics url plus the payload
     */
    public void logEvent(final AnalyticsAppPayload<EventsPayload> analyticsAppPayload) {
        DotConcurrentFactory
                .getInstance()
                .getSubmitter("event-log-posting", submitterConfig)
                .execute(new EventLogRunnable(analyticsAppPayload.analyticsApp(), analyticsAppPayload.payload()));
    }

}
