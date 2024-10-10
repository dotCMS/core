package com.dotcms.jobs.business.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.dotcms.jobs.business.api.events.EventProducer;
import com.dotcms.jobs.business.api.events.RealTimeJobMonitor;
import com.dotcms.jobs.business.error.CircuitBreaker;
import com.dotcms.jobs.business.error.ExponentialBackoffRetryStrategy;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.error.RetryStrategyProducer;
import com.dotcms.jobs.business.queue.JobQueue;
import com.dotcms.jobs.business.queue.JobQueueProducer;
import javax.inject.Inject;
import org.jboss.weld.bootstrap.api.helpers.RegistrySingletonProvider;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test class for verifying the CDI (Contexts and Dependency Injection) functionality of the
 * JobQueueManagerAPI implementation.
 */
@ExtendWith(WeldJunit5Extension.class)
public class JobQueueManagerAPICDITest {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(
            WeldInitiator.createWeld()
                    .containerId(RegistrySingletonProvider.STATIC_INSTANCE)
                    .beanClasses(JobQueueManagerAPIImpl.class, JobQueueConfig.class,
                            JobQueue.class, RetryStrategy.class, CircuitBreaker.class,
                            JobQueueProducer.class, JobQueueConfigProducer.class,
                            RetryStrategyProducer.class, RealTimeJobMonitor.class,
                            EventProducer.class)
    );

    @Inject
    private JobQueueManagerAPI jobQueueManagerAPI;

    @Inject
    private JobQueueManagerAPI jobQueueManagerAPI2;

    /**
     * Method to test: Multiple injections of JobQueueManagerAPI Given Scenario: Two separate
     * injections of JobQueueManagerAPI are requested ExpectedResult: Both injections refer to the
     * same instance, confirming singleton behavior
     */
    @Test
    void test_SingletonBehavior() {
        assertNotNull(jobQueueManagerAPI,
                "First JobQueueManagerAPI instance should not be null");
        assertNotNull(jobQueueManagerAPI2,
                "Second JobQueueManagerAPI instance should not be null");
        assertSame(jobQueueManagerAPI, jobQueueManagerAPI2,
                "Both JobQueueManagerAPI injections should refer to the same instance");
    }

    /**
     * Method to test: CDI injection of JobQueueManagerAPI
     * Given Scenario: A CDI environment is set up with necessary dependencies
     * ExpectedResult: JobQueueManagerAPI is correctly injected and is an instance of
     * JobQueueManagerAPIImpl
     */
    @Test
    void test_CDIInjection() {
        assertNotNull(jobQueueManagerAPI, "JobQueueManagerAPI should be injected");
        assertInstanceOf(JobQueueManagerAPIImpl.class, jobQueueManagerAPI,
                "JobQueueManagerAPI should be an instance of JobQueueManagerAPIImpl");
    }

    /**
     * Method to test: Dependency injection of JobQueueManagerAPIImpl fields
     * Given Scenario: JobQueueManagerAPIImpl is instantiated by the CDI container
     * ExpectedResult: All required dependencies are correctly injected into JobQueueManagerAPIImpl
     */
    @Test
    void test_JobQueueManagerAPIFields() {

        assertNotNull(jobQueueManagerAPI.getJobQueue(), "JobQueue should be injected");
        assertInstanceOf(JobQueue.class, jobQueueManagerAPI.getJobQueue(),
                "Injected object should implement JobQueue interface");

        assertNotNull(jobQueueManagerAPI.getCircuitBreaker(),
                "CircuitBreaker should be injected");
        assertNotNull(jobQueueManagerAPI.getDefaultRetryStrategy(),
                "Retry strategy should be injected");

        assertEquals(10, jobQueueManagerAPI.getThreadPoolSize(),
                "ThreadPoolSize should be greater than 0");
        assertInstanceOf(ExponentialBackoffRetryStrategy.class,
                jobQueueManagerAPI.getDefaultRetryStrategy(),
                "Retry strategy should be an instance of ExponentialBackoffRetryStrategy");
    }

}