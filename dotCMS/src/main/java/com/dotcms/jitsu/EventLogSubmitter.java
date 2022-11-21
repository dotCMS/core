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
 * <li>EVENT_LOG_POSTING_THREADS: Max active posting threads
 * <li>EVENT_LOG_QUEUE_SIZE: Max size of the queue
 */
public class EventLogSubmitter {

    private final DotSubmitter submitter;

    EventLogSubmitter(){
        final SubmitterConfig config = new DotConcurrentFactory.SubmitterConfigBuilder()
            .poolSize(1)
            .maxPoolSize(Config.getIntProperty("EVENT_LOG_POSTING_THREADS", 10))
            .keepAliveMillis(1000)
            .queueCapacity(Config.getIntProperty("EVENT_LOG_QUEUE_SIZE", 10000))
            .rejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
            .build();
        this.submitter = DotConcurrentFactory.getInstance().getSubmitter("event-log-posting", config);

    }

    void logEvent(Host host, final String jsonEvent) {
        this.submitter.execute(new EventLogRunnable(host, jsonEvent));
    }

}