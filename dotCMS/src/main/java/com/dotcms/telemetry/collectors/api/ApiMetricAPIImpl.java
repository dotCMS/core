package com.dotcms.telemetry.collectors.api;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class ApiMetricAPIImpl implements ApiMetricAPI {

    private static final int BATCH_SIZE = Config.getIntProperty("TELEMETRY_METRIC_BATCH_SIZE", 50);
    private static final int FLUSH_INTERVAL_SECONDS = Config.getIntProperty("TELEMETRY_METRIC_FLUSH_INTERVAL_SECONDS", 30);
    
    private final RequestHashCalculator requestHashCalculator = new RequestHashCalculator();
    private final ApiMetricFactory apiMetricFactory;
    private final Queue<ApiMetricRequest> metricQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean processingBatch = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    @Inject
    public ApiMetricAPIImpl(final ApiMetricFactory apiMetricFactory) {
        this.apiMetricFactory = apiMetricFactory;
        initBatchProcessor();
    }
    
    /**
     * Initialize the batch processor to periodically flush queued metrics to the database
     */
    private void initBatchProcessor() {
        Logger.info(this, "Initializing telemetry metric batch processor with batch size: " + 
                BATCH_SIZE + ", flush interval: " + FLUSH_INTERVAL_SECONDS + " seconds");
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "telemetry-batch-processor");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleWithFixedDelay(
                this::processBatch, 
                FLUSH_INTERVAL_SECONDS, 
                FLUSH_INTERVAL_SECONDS, 
                TimeUnit.SECONDS
        );
    }
    
    /**
     * Process a batch of metrics from the queue
     */
    private void processBatch() {
        if (metricQueue.isEmpty() || !processingBatch.compareAndSet(false, true)) {
            return;
        }
        
        try {
            int processed = 0;
            while (!metricQueue.isEmpty() && processed < BATCH_SIZE) {
                ApiMetricRequest metric = metricQueue.poll();
                if (metric != null) {
                    try {
                        saveMetric(metric);
                        processed++;
                    } catch (Exception e) {
                        Logger.debug(this, "Error saving metric batch: " + e.getMessage(), e);
                    }
                }
            }
            
            if (processed > 0) {
                Logger.debug(this, "Processed " + processed + " telemetry metrics in batch");
            }
        } finally {
            processingBatch.set(false);
        }
    }
    
    /**
     * Saves a single metric to the database with transaction handling
     */
    @WrapInTransaction
    private void saveMetric(ApiMetricRequest metric) {
        try {
            apiMetricFactory.save(metric);
        } catch (Exception e) {
            Logger.debug(this, "Error saving telemetry metric: " + e.getMessage(), e);
        }
    }

    @CloseDBIfOpened
    @Override
    public Collection<Map<String, Object>> getMetricTemporaryTableData() {
        return this.apiMetricFactory.getMetricTemporaryTableData();
    }

    @Override
    public void save(final ApiMetricType apiMetricType,
                     final ApiMetricWebInterceptor.RereadInputStreamRequest request) {
        try {
            final String requestHash = requestHashCalculator.calculate(apiMetricType, request);
            final ApiMetricRequest metricAPIHit = new ApiMetricRequest.Builder()
                    .setMetric(apiMetricType.getMetric())
                    .setTime(Instant.now())
                    .setHash(requestHash)
                    .build();
            
            // Add to queue for batch processing instead of immediate async execution
            metricQueue.offer(metricAPIHit);
            
            // If queue is getting too large, trigger processing
            if (metricQueue.size() >= BATCH_SIZE * 2 && processingBatch.compareAndSet(false, true)) {
                ApiMetricFactorySubmitter.INSTANCE.saveAsync(this::processBatch);
            }
        } catch (Exception e) {
            // Don't let telemetry issues affect the main application
            Logger.debug(this, "Error queueing telemetry metric: " + e.getMessage(), e);
        }
    }

    @WrapInTransaction
    @Override
    public void startCollecting() {
        try {
            this.apiMetricFactory.saveStartEvent();
        } catch (final Exception e) {
            Logger.debug(this, "Error saving start event", e);
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    @Override
    public void flushTemporaryTable() {
        // Process any remaining metrics in the queue
        processBatch();
        
        try {
            this.apiMetricFactory.flushTemporaryTable();
        } catch (final Exception e) {
            Logger.debug(this, "Error flushing table", e);
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    @Override
    public void createTemporaryTable() {
        try {
            this.apiMetricFactory.createTemporaryTable();
        } catch (final Exception e) {
            Logger.debug(this, "Error creating table", e);
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    @Override
    public void dropTemporaryTable() {
        // Shutdown the batch processor
        if (scheduler != null) {
            try {
                scheduler.shutdown();
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Logger.warn(this, "Error shutting down telemetry batch processor", e);
            } finally {
                scheduler = null;
            }
        }
        
        // Process any remaining metrics
        processBatch();
        
        try {
            this.apiMetricFactory.dropTemporaryTable();
        } catch (final Exception e) {
            Logger.debug(this, "Error dropping table", e);
            throw new DotRuntimeException(e);
        }
    }
}
