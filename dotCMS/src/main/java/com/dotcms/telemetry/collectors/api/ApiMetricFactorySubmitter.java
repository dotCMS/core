package com.dotcms.telemetry.collectors.api;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Save the metrics without block the API client code
 */
public enum ApiMetricFactorySubmitter {

    INSTANCE;

    private static final String TELEMETRY_SUBMITTER_NAME = "telemetry-api-metric-submitter";
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;
    private static final int MAX_THREAD_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 1000;
    
    /**
     * Use AtomicReference for thread-safe initialization
     */
    private final AtomicReference<DotSubmitter> submitter = new AtomicReference<>();

    /**
     * Starts the API Metric Submitter thread pool, ensuring it's only initialized once.
     */
    public void start() {
        if (submitter.get() == null) {
            synchronized (this) {
                if (submitter.get() == null) {
                    Logger.info(this, "Initializing telemetry metrics submitter");
                    
                    // Create a bounded queue to prevent OOM errors during high load
                    final LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
                    
                    final DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter(
                            TELEMETRY_SUBMITTER_NAME,
                            new DotConcurrentFactory.SubmitterConfigBuilder()
                                .poolSize(DEFAULT_THREAD_POOL_SIZE)
                                .maxPoolSize(MAX_THREAD_POOL_SIZE)
                                .queueCapacity(QUEUE_CAPACITY)
                                .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                                .build()
                    );
                    
                    submitter.set(dotSubmitter);
                    Logger.info(this, "Telemetry metrics submitter initialized with pool size: " + 
                            DEFAULT_THREAD_POOL_SIZE + "/" + MAX_THREAD_POOL_SIZE + ", queue capacity: " + QUEUE_CAPACITY);
                }
            }
        }
    }
    
    /**
     * Save the metric in an Async way
     *
     * @param metrics
     */
    public void saveAsync(final Runnable metrics) {
        try {
            final DotSubmitter currentSubmitter = submitter.get();
            if (currentSubmitter != null) {
                currentSubmitter.execute(metrics);
            } else {
                Logger.debug(this, "Telemetry submitter not initialized, skipping metric collection");
            }
        } catch (RejectedExecutionException e) {
            // Don't let telemetry issues affect the main application
            // Just log at debug level and continue
            Logger.debug(this, "Telemetry queue is full, metric dropped: " + e.getMessage());
        } catch (Exception e) {
            Logger.debug(this, "Error submitting telemetry metric: " + e.getMessage(), e);
        }
    }

    /**
     * Properly shutdown the submitter
     */
    public void shutdownNow() {
        final DotSubmitter currentSubmitter = submitter.get();
        if (currentSubmitter != null) {
            Logger.info(this, "Shutting down telemetry metrics submitter");
            currentSubmitter.shutdownNow();
            submitter.set(null);
        }
    }
}
