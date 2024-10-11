package com.dotcms;

import com.dotcms.jobs.business.api.JobProcessorFactory;
import com.dotcms.jobs.business.api.JobProcessorScanner;
import com.dotcms.jobs.business.api.JobQueueConfig;
import com.dotcms.jobs.business.api.JobQueueConfigProducer;
import com.dotcms.jobs.business.api.JobQueueManagerAPIImpl;
import com.dotcms.jobs.business.api.events.EventProducer;
import com.dotcms.jobs.business.api.events.RealTimeJobMonitor;
import com.dotcms.jobs.business.error.CircuitBreaker;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.error.RetryStrategyProducer;
import com.dotcms.jobs.business.queue.JobQueue;
import com.dotcms.jobs.business.queue.JobQueueProducer;
import com.dotcms.rest.api.v1.job.JobQueueHelper;
import org.jboss.weld.bootstrap.api.helpers.RegistrySingletonProvider;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WeldJunit5Extension.class)
public class TestBaseJunit5WeldInitiator {

    @WeldSetup
    public static WeldInitiator weld = WeldInitiator.of(
            WeldInitiator.createWeld()
                    .containerId(RegistrySingletonProvider.STATIC_INSTANCE)
                    .beanClasses(JobQueueManagerAPIImpl.class, JobQueueConfig.class,
                            JobQueue.class, RetryStrategy.class, CircuitBreaker.class,
                            JobQueueProducer.class, JobQueueConfigProducer.class,
                            RetryStrategyProducer.class, RealTimeJobMonitor.class,
                            EventProducer.class, JobProcessorFactory.class, JobQueueHelper.class,
                            JobProcessorScanner.class
                    )
    );

    @AfterAll
    public static void tearDown() {
        if (weld != null && weld.isRunning()) {
            weld.shutdown();
            weld = null;
        }
    }

}
