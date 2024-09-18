package com.dotcms.jitsu;

import java.util.concurrent.ThreadPoolExecutor;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotConcurrentFactory.SubmitterConfig;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Config;

/**
 * Wrapper class to a {@link DotSubmitter} instance used to execute instances of {@link EventLogRunnable}
 * <p>
 * It's configuration can be modified via the following properties:
 * <li>EVENT_LOG_POSTING_THREADS: Initial active posting threads
 * <li>EVENT_LOG_POSTING_THREADS_MAX: Max active posting threads
 * <li>EVENT_LOG_QUEUE_SIZE: Max size of the queue
 */
public class EventLogSubmitter {

    private final SubmitterConfig submitterConfig;

    public EventLogSubmitter() {
        submitterConfig = new DotConcurrentFactory.SubmitterConfigBuilder()
            .poolSize(Config.getIntProperty("EVENT_LOG_POSTING_THREADS", 8))
            .maxPoolSize(Config.getIntProperty("EVENT_LOG_POSTING_THREADS_MAX", 16))
            .keepAliveMillis(1000)
            .queueCapacity(Config.getIntProperty("EVENT_LOG_QUEUE_SIZE", 10000))
            .rejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
            .build();
    }

    public void logEvent(final Host host, final EventsPayload eventPayload) {
        DotConcurrentFactory
            .getInstance()
            .getSubmitter("event-log-posting", submitterConfig)
            .execute(new EventLogRunnable(host, eventPayload));
    }

    public void logEvent(final EventLogRunnable runnable) {
        DotConcurrentFactory
            .getInstance()
            .getSubmitter("event-log-posting", submitterConfig)
            .execute(runnable);
    }

}
