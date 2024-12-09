package com.dotcms.jobs.business.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.jobs.business.api.events.JobCancelRequestEvent;
import com.dotcms.jobs.business.api.events.RealTimeJobMonitor;
import com.dotcms.jobs.business.detector.AbandonedJobDetector;
import com.dotcms.jobs.business.error.CircuitBreaker;
import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.error.JobCancellationException;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.error.RetryPolicyProcessor;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.queue.JobQueue;
import com.dotcms.jobs.business.queue.error.JobLockingException;
import com.dotcms.jobs.business.queue.error.JobNotFoundException;
import com.dotcms.jobs.business.queue.error.JobQueueDataException;
import com.dotcms.jobs.business.queue.error.JobQueueException;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

public class JobQueueManagerAPITest {

    /**
     * A simple job processor that does nothing.
     */
    static class SimpleCancellableJobProcessor implements JobProcessor, Cancellable {

        @Override
        public void process(Job job) throws JobProcessingException {
            // Do nothing
        }

        @Override
        public void cancel(Job job) {
            // Do nothing
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return null;
        }
    }

    /**
     * A complex cancellable job processor that simulates work and waits for cancellation.
     */
    static class ComplexCancellableJobProcessor implements JobProcessor, Cancellable {

        private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);
        private final CountDownLatch processingStarted = new CountDownLatch(1);
        private final CountDownLatch processingCompleted = new CountDownLatch(1);

        @Override
        public void process(Job job) throws JobProcessingException {
            processingStarted.countDown();
            // Simulate work and wait for cancellation
            Awaitility.await()
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .atMost(30, TimeUnit.SECONDS)
                    .until(cancellationRequested::get);

            processingCompleted.countDown();
        }

        @Override
        public void cancel(Job job) {
            cancellationRequested.set(true);
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return null;
        }

        public boolean awaitProcessingStart(long timeout, TimeUnit unit)
                throws InterruptedException {
            return processingStarted.await(timeout, unit);
        }

        public boolean awaitProcessingCompleted(long timeout, TimeUnit unit)
                throws InterruptedException {
            return processingCompleted.await(timeout, unit);
        }
    }

    private JobQueue mockJobQueue;

    private JobProcessor mockJobProcessor;

    private SimpleCancellableJobProcessor mockCancellableProcessor;

    private RetryStrategy mockRetryStrategy;

    private CircuitBreaker mockCircuitBreaker;

    private JobQueueManagerAPI jobQueueManagerAPI;

    private RetryPolicyProcessor retryPolicyProcessor;

    private AbandonedJobDetector abandonedJobDetector;

    private JobProcessorDiscovery jobProcessorDiscovery;

    /**
     * Factory to create mock JobProcessor instances for testing.
     * This is how we instruct the JobQueueManagerAPI to use our mock processors.
     */
    JobProcessorFactory jobProcessorFactory = new JobProcessorFactory(){
        @Override
        JobProcessor newInstance(Class<? extends JobProcessor> processorClass) {
            //Cancellable mock processor
            if(SimpleCancellableJobProcessor.class.equals(processorClass)){
                return mockCancellableProcessor;
            }
            //For this we need the real deal
            if(ComplexCancellableJobProcessor.class.equals(processorClass)){
                return new ComplexCancellableJobProcessor();
            }
            //Default mock processor
            return mockJobProcessor;
        }
    };

    @Before
    public void setUp() {

        mockJobQueue = mock(JobQueue.class);
        mockJobProcessor = mock(JobProcessor.class);
        mockCancellableProcessor = mock(SimpleCancellableJobProcessor.class);
        mockRetryStrategy = mock(RetryStrategy.class);
        mockCircuitBreaker = mock(CircuitBreaker.class);
        retryPolicyProcessor = mock(RetryPolicyProcessor.class);
        abandonedJobDetector = mock(AbandonedJobDetector.class);
        jobProcessorDiscovery = mock(JobProcessorDiscovery.class);

        jobQueueManagerAPI = newJobQueueManagerAPI(
                mockJobQueue, mockCircuitBreaker, mockRetryStrategy, jobProcessorFactory,
                retryPolicyProcessor, abandonedJobDetector,
                jobProcessorDiscovery, 1
        );

        jobQueueManagerAPI.registerProcessor("testQueue", JobProcessor.class);
        jobQueueManagerAPI.setRetryStrategy("testQueue", mockRetryStrategy);
    }

    /**
     * Method to test: createJob in JobQueueManagerAPI
     * Given Scenario: Valid queue name and parameters are provided
     * ExpectedResult: Job is created successfully and correct job ID is returned
     */
    @Test
    public void test_createJob() throws DotDataException, JobQueueException {

        Map<String, Object> parameters = new HashMap<>();
        when(mockJobQueue.createJob(anyString(), anyMap())).thenReturn("job123");

        // Creating a job
        String jobId = jobQueueManagerAPI.createJob("testQueue", parameters);

        assertEquals("job123", jobId);
        verify(mockJobQueue).createJob("testQueue", parameters);
    }

    /**
     * Method to test: getJob in JobQueueManagerAPI
     * Given Scenario: Valid job ID is provided
     * ExpectedResult: Correct job is retrieved from the job queue
     */
    @Test
    public void test_getJob() throws DotDataException, JobQueueDataException, JobNotFoundException {

        Job mockJob = mock(Job.class);
        when(mockJobQueue.getJob("job123")).thenReturn(mockJob);

        // Getting a job
        Job result = jobQueueManagerAPI.getJob("job123");

        assertEquals(mockJob, result);
        verify(mockJobQueue).getJob("job123");
    }

    /**
     * Method to test: getJobs in JobQueueManagerAPI
     * Given Scenario: Valid page and pageSize parameters are provided
     * ExpectedResult: Correct list of jobs is retrieved from the job queue
     */
    @Test
    public void test_getJobs() throws DotDataException, JobQueueDataException {

        // Prepare test data
        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);
        List<Job> expectedJobs = Arrays.asList(job1, job2);
        final var paginatedResult = JobPaginatedResult.builder()
                .jobs(expectedJobs)
                .total(2)
                .page(1)
                .pageSize(10)
                .build();

        // Mock the behavior of jobQueue.getJobs
        when(mockJobQueue.getJobs(1, 10)).thenReturn(paginatedResult);

        // Call the method under test
        final var actualResult = jobQueueManagerAPI.getJobs(1, 10);

        // Verify the results
        assertEquals(expectedJobs, actualResult.jobs());
        verify(mockJobQueue).getJobs(1, 10);
    }


    /**
     * Method to test: getJobs for a particular queue in JobQueueManagerAPI
     * Given Scenario: Valid page and pageSize parameters are provided
     * ExpectedResult: Correct list of jobs is retrieved from the job queue
     */
    @Test
    public void test_getJobsFromQueue() throws DotDataException, JobQueueDataException {

        // Prepare test data
        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);
        List<Job> expectedJobs = Arrays.asList(job1, job2);
        final var paginatedResult = JobPaginatedResult.builder()
                .jobs(expectedJobs)
                .total(2)
                .page(1)
                .pageSize(10)
                .build();

        // Mock the behavior of jobQueue.getJobs
        when(mockJobQueue.getJobs("testQueue",1, 10)).thenReturn(paginatedResult);

        // Call the method under test
        final var actualResult = jobQueueManagerAPI.getJobs("testQueue", 1, 10);

        // Verify the results
        assertEquals(expectedJobs, actualResult.jobs());
        verify(mockJobQueue).getJobs("testQueue", 1, 10);
    }


    /**
     * Method to test: getActiveJobs for a particular queue in JobQueueManagerAPI
     * Given Scenario: Valid page and pageSize parameters are provided
     * ExpectedResult: Correct list of jobs is retrieved from the job queue
     */
    @Test
    public void test_getActiveJobsFromQueue() throws DotDataException, JobQueueDataException {

        // Prepare test data
        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);
        List<Job> expectedJobs = Arrays.asList(job1, job2);
        final var paginatedResult = JobPaginatedResult.builder()
                .jobs(expectedJobs)
                .total(2)
                .page(1)
                .pageSize(10)
                .build();

        // Mock the behavior of jobQueue.getJobs
        when(mockJobQueue.getActiveJobs("testQueue",1, 10)).thenReturn(paginatedResult);

        // Call the method under test
        final var actualResult = jobQueueManagerAPI.getActiveJobs("testQueue", 1, 10);

        // Verify the results
        assertEquals(expectedJobs, actualResult.jobs());
        verify(mockJobQueue).getActiveJobs("testQueue", 1, 10);
    }


    /**
     * Method to test: getCompletedJobs for a particular queue in JobQueueManagerAPI
     * Given Scenario: Valid page and pageSize parameters are provided
     * ExpectedResult: Correct list of jobs is retrieved from the job queue
     */
    @Test
    public void test_getCompletedJobsFromQueue() throws DotDataException, JobQueueDataException {

        // Prepare test data
        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);
        List<Job> expectedJobs = Arrays.asList(job1, job2);
        final var paginatedResult = JobPaginatedResult.builder()
                .jobs(expectedJobs)
                .total(2)
                .page(1)
                .pageSize(10)
                .build();

        // Mock the behavior of jobQueue.getJobs
        when(mockJobQueue.getCompletedJobs("testQueue",1, 10)).thenReturn(paginatedResult);

        // Call the method under test
        final var actualResult = jobQueueManagerAPI.getCompletedJobs("testQueue", 1, 10);

        // Verify the results
        assertEquals(expectedJobs, actualResult.jobs());
        verify(mockJobQueue).getCompletedJobs("testQueue", 1, 10);
    }


    /**
     * Method to test: getCanceledJobs for a particular queue in JobQueueManagerAPI
     * Given Scenario: Valid page and pageSize parameters are provided
     * ExpectedResult: Correct list of jobs is retrieved from the job queue
     */
    @Test
    public void test_getCanceledJobsFromQueue() throws DotDataException, JobQueueDataException {

        // Prepare test data
        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);
        List<Job> expectedJobs = Arrays.asList(job1, job2);
        final var paginatedResult = JobPaginatedResult.builder()
                .jobs(expectedJobs)
                .total(2)
                .page(1)
                .pageSize(10)
                .build();

        // Mock the behavior of jobQueue.getJobs
        when(mockJobQueue.getCanceledJobs("testQueue",1, 10)).thenReturn(paginatedResult);

        // Call the method under test
        final var actualResult = jobQueueManagerAPI.getCanceledJobs("testQueue", 1, 10);

        // Verify the results
        assertEquals(expectedJobs, actualResult.jobs());
        verify(mockJobQueue).getCanceledJobs("testQueue", 1, 10);
    }


    /**
     * Method to test: getFailedJobs for a particular queue in JobQueueManagerAPI
     * Given Scenario: Valid page and pageSize parameters are provided
     * ExpectedResult: Correct list of jobs is retrieved from the job queue
     */
    @Test
    public void test_getFailedJobsFromQueue() throws DotDataException, JobQueueDataException {

        // Prepare test data
        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);
        List<Job> expectedJobs = Arrays.asList(job1, job2);
        final var paginatedResult = JobPaginatedResult.builder()
                .jobs(expectedJobs)
                .total(2)
                .page(1)
                .pageSize(10)
                .build();

        // Mock the behavior of jobQueue.getJobs
        when(mockJobQueue.getFailedJobs("testQueue",1, 10)).thenReturn(paginatedResult);

        // Call the method under test
        final var actualResult = jobQueueManagerAPI.getFailedJobs("testQueue", 1, 10);

        // Verify the results
        assertEquals(expectedJobs, actualResult.jobs());
        verify(mockJobQueue).getFailedJobs("testQueue", 1, 10);
    }


    /**
     * Method to test: getAbandonedJobs for a particular queue in JobQueueManagerAPI
     * Given Scenario: Valid page and pageSize parameters are provided
     * ExpectedResult: Correct list of jobs is retrieved from the job queue
     */
    @Test
    public void test_getAbandonedJobsFromQueue() throws DotDataException, JobQueueDataException {

        // Prepare test data
        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);
        List<Job> expectedJobs = Arrays.asList(job1, job2);
        final var paginatedResult = JobPaginatedResult.builder()
                .jobs(expectedJobs)
                .total(2)
                .page(1)
                .pageSize(10)
                .build();

        // Mock the behavior of jobQueue.getJobs
        when(mockJobQueue.getAbandonedJobs("testQueue",1, 10)).thenReturn(paginatedResult);

        // Call the method under test
        final var actualResult = jobQueueManagerAPI.getAbandonedJobs("testQueue", 1, 10);

        // Verify the results
        assertEquals(expectedJobs, actualResult.jobs());
        verify(mockJobQueue).getAbandonedJobs("testQueue", 1, 10);
    }


    /**
     * Method to test: getSuccessfulJobs for a particular queue in JobQueueManagerAPI
     * Given Scenario: Valid page and pageSize parameters are provided
     * ExpectedResult: Correct list of jobs is retrieved from the job queue
     */
    @Test
    public void test_getSuccessfulJobsFromQueue() throws DotDataException, JobQueueDataException {

        // Prepare test data
        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);
        List<Job> expectedJobs = Arrays.asList(job1, job2);
        final var paginatedResult = JobPaginatedResult.builder()
                .jobs(expectedJobs)
                .total(2)
                .page(1)
                .pageSize(10)
                .build();

        // Mock the behavior of jobQueue.getJobs
        when(mockJobQueue.getSuccessfulJobs("testQueue",1, 10)).thenReturn(paginatedResult);

        // Call the method under test
        final var actualResult = jobQueueManagerAPI.getSuccessfulJobs("testQueue", 1, 10);

        // Verify the results
        assertEquals(expectedJobs, actualResult.jobs());
        verify(mockJobQueue).getSuccessfulJobs("testQueue", 1, 10);
    }


    /**
     * Method to test: start in JobQueueManagerAPI
     * Given Scenario: JobQueueManagerAPI is not started
     * ExpectedResult: JobQueueManagerAPI starts successfully and begins processing jobs
     */
    @Test
    public void test_start()
            throws InterruptedException, JobQueueDataException, JobLockingException {

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        assertFalse(jobQueueManagerAPI.isStarted());

        jobQueueManagerAPI.start();

        assertTrue(jobQueueManagerAPI.isStarted());
        assertTrue(jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS));

        // Verify that jobs are being processed
        verify(mockJobQueue, timeout(5000).atLeastOnce()).nextJob();
    }

    /**
     * Method to test: close in JobQueueManagerAPI
     * Given Scenario: JobQueueManagerAPI is running
     * ExpectedResult: JobQueueManagerAPI stops successfully and no more jobs are processed
     */
    @Test
    public void test_close() throws Exception {

        // Start the JobQueueManagerAPI
        jobQueueManagerAPI.start();
        assertTrue(jobQueueManagerAPI.isStarted());
        assertTrue(jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS));

        AtomicInteger jobCheckCount = new AtomicInteger(0);
        when(mockJobQueue.nextJob()).thenAnswer(invocation -> {
            jobCheckCount.incrementAndGet();
            return null;
        });

        // Close the JobQueueManagerAPI
        jobQueueManagerAPI.close();

        // Wait for the JobQueueManagerAPI to be fully stopped
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> !jobQueueManagerAPI.isStarted());

        int currentJobCount = jobCheckCount.get();

        // Verify that no more jobs are being processed by waiting for two seconds
        long startTime = System.currentTimeMillis();
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> System.currentTimeMillis() - startTime >= 2000);

        assertEquals(currentJobCount, jobCheckCount.get());

        // Try to start a new job and verify it's not processed
        Job mockJob = mock(Job.class);
        when(mockJobQueue.nextJob()).thenReturn(mockJob);

        // Wait and verify that the job was not processed
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(mockJobProcessor, never()).process(any(Job.class)));

        // Verify that we can't start the JobQueueManagerAPIImpl again after closing
        jobQueueManagerAPI.start();
        assertFalse(jobQueueManagerAPI.isStarted());

        // Verify that close() can be called multiple times without error
        jobQueueManagerAPI.close();
        assertFalse(jobQueueManagerAPI.isStarted());
    }

    /**
     * Method to test: Job retry mechanism in JobQueueManagerAPI
     * Given Scenario: Job fails on first attempt but succeeds on retry
     * ExpectedResult: Job is retried once and completes successfully
     */
    @Test
    public void test_JobRetry_single_retry() throws Exception {

        // Create a mock job
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.queueName()).thenReturn("testQueue");

        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);
        when(mockJob.state()).thenAnswer(inv -> jobState.get());

        AtomicInteger retryCount = new AtomicInteger(0);
        when(mockJob.retryCount()).thenAnswer(inv -> retryCount.get());

        when(mockJob.completedAt()).thenAnswer(inv -> Optional.of(LocalDateTime.now()));

        when(mockJob.withState(any())).thenAnswer(inv -> {
            jobState.set(inv.getArgument(0));
            return mockJob;
        });
        when(mockJob.markAsRunning()).thenAnswer(inv -> {
            jobState.set(JobState.RUNNING);
            return mockJob;
        });
        when(mockJob.incrementRetry()).thenAnswer(inv -> {
            retryCount.incrementAndGet();
            return mockJob;
        });
        when(mockJob.markAsSuccessful(any())).thenAnswer(inv -> {
            jobState.set(JobState.SUCCESS);
            return mockJob;
        });
        when(mockJob.markAsFailed(any())).thenAnswer(inv -> {
            jobState.set(JobState.FAILED);
            return mockJob;
        });

        when(mockJob.withProgressTracker(any(DefaultProgressTracker.class))).thenReturn(mockJob);

        // Set up the job queue to return our mock job twice (for initial attempt and retry)
        when(mockJobQueue.nextJob()).thenReturn(mockJob, mockJob, null);

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        // Configure retry strategy
        when(mockRetryStrategy.shouldRetry(any(), any())).thenReturn(true);
        when(mockRetryStrategy.nextRetryDelay(any())).thenReturn(0L); // Immediate retry

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJob.progressTracker()).thenReturn(Optional.ofNullable(mockProgressTracker));
        when(mockJob.progress()).thenReturn(0f);
        when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);

        // Configure job processor to fail on first attempt, succeed on second
        AtomicInteger processAttempts = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (processAttempts.getAndIncrement() == 0) {
                throw new RuntimeException("Simulated failure");
            }
            Job job = invocation.getArgument(0);
            job.markAsSuccessful(any());
            return null;
        }).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(mockJobProcessor, times(2)).process(any());
                    assertEquals(JobState.SUCCESS, jobState.get());
                });

        // Additional verifications
        verify(mockJobQueue, atLeast(3)).updateJobStatus(any());
        verify(mockRetryStrategy, times(1)).shouldRetry(any(), any());
        verify(mockRetryStrategy, times(1)).nextRetryDelay(any());
        verify(mockJob, times(1)).incrementRetry();

        assertEquals(1, retryCount.get());

        // Stop the job queue
        jobQueueManagerAPI.close();
    }

    /**
     * Method to test: Job retry mechanism in JobQueueManagerAPI
     * Given Scenario: Job fails twice before succeeding on third attempt
     * ExpectedResult: Job is retried twice and completes successfully
     */
    @Test
    public void test_JobRetry_retry_twice() throws Exception {

        // Create a mock job
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.queueName()).thenReturn("testQueue");

        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicReference<LocalDateTime> lastRetry = new AtomicReference<>(LocalDateTime.now());

        when(mockJob.state()).thenAnswer(inv -> jobState.get());
        when(mockJob.retryCount()).thenAnswer(inv -> retryCount.get());
        when(mockJob.completedAt()).thenAnswer(inv -> Optional.of(lastRetry.get()));

        when(mockJob.withState(any())).thenAnswer(inv -> {
            jobState.set(inv.getArgument(0));
            return mockJob;
        });
        when(mockJob.markAsRunning()).thenAnswer(inv -> {
            jobState.set(JobState.RUNNING);
            return mockJob;
        });
        when(mockJob.incrementRetry()).thenAnswer(inv -> {
            retryCount.incrementAndGet();
            lastRetry.set(LocalDateTime.now());
            return mockJob;
        });
        when(mockJob.markAsSuccessful(any())).thenAnswer(inv -> {
            jobState.set(JobState.SUCCESS);
            return mockJob;
        });
        when(mockJob.markAsFailed(any())).thenAnswer(inv -> {
            jobState.set(JobState.FAILED);
            return mockJob;
        });

        when(mockJob.withProgressTracker(any(DefaultProgressTracker.class))).thenReturn(mockJob);

        // Configure job queue to always return the mockJob until it's completed
        when(mockJobQueue.nextJob()).thenAnswer(inv ->
                jobState.get() != JobState.SUCCESS ? mockJob : null
        );

        // Configure retry strategy
        when(mockRetryStrategy.shouldRetry(any(), any())).thenReturn(true);
        when(mockRetryStrategy.nextRetryDelay(any())).thenReturn(100L); // Non-zero delay

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJob.progressTracker()).thenReturn(Optional.ofNullable(mockProgressTracker));
        when(mockJob.progress()).thenReturn(0f);
        when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);

        // Configure job processor to fail twice, succeed on third attempt
        AtomicInteger processAttempts = new AtomicInteger(0);
        doAnswer(invocation -> {
            int attempt = processAttempts.getAndIncrement();
            if (attempt < 2) {
                throw new RuntimeException("Simulated failure");
            }
            return null;
        }).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(mockJobProcessor, times(3)).process(any());
                    assertEquals(JobState.SUCCESS, jobState.get());
                    assertEquals(2, retryCount.get());
                });

        // Verify state transitions
        InOrder inOrder = inOrder(mockJob);
        inOrder.verify(mockJob).markAsRunning();
        inOrder.verify(mockJob).markAsFailed(any());
        inOrder.verify(mockJob).markAsRunning();
        inOrder.verify(mockJob).markAsFailed(any());
        inOrder.verify(mockJob).markAsRunning();
        inOrder.verify(mockJob).markAsSuccessful(any());

        // Verify retry behavior
        verify(mockRetryStrategy, atLeast(2)).shouldRetry(any(), any());
        verify(mockRetryStrategy, atLeast(2)).nextRetryDelay(any());
        verify(mockJob, times(2)).incrementRetry();
        verify(mockJobQueue, atLeast(2)).putJobBackInQueue(mockJob);
        verify(mockJobQueue, atLeast(4)).updateJobStatus(any());
        verify(mockJobQueue, atLeast(3)).nextJob();

        // Stop the job queue
        jobQueueManagerAPI.close();
    }

    /**
     * Method to test: Job retry mechanism in JobQueueManagerAPI
     * Given Scenario: Job fails repeatedly and reaches max retry limit
     * ExpectedResult: Job is retried up to max limit and then marked as failed
     */
    @Test
    public void test_JobRetry_MaxRetryLimit() throws Exception {

        // Create a mock job
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.queueName()).thenReturn("testQueue");

        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicReference<LocalDateTime> lastRetry = new AtomicReference<>(LocalDateTime.now());
        int maxRetries = 3;

        when(mockJob.state()).thenAnswer(inv -> jobState.get());
        when(mockJob.retryCount()).thenAnswer(inv -> retryCount.get());
        when(mockJob.completedAt()).thenAnswer(inv -> Optional.of(lastRetry.get()));

        when(mockJob.withState(any())).thenAnswer(inv -> {
            jobState.set(inv.getArgument(0));
            return mockJob;
        });
        when(mockJob.markAsRunning()).thenAnswer(inv -> {
            jobState.set(JobState.RUNNING);
            return mockJob;
        });
        when(mockJob.incrementRetry()).thenAnswer(inv -> {
            retryCount.incrementAndGet();
            lastRetry.set(LocalDateTime.now());
            return mockJob;
        });
        when(mockJob.markAsFailed(any())).thenAnswer(inv -> {
            jobState.set(JobState.FAILED);
            return mockJob;
        });
        when(mockJob.markAsFailedPermanently()).thenAnswer(inv -> {
            jobState.set(JobState.FAILED_PERMANENTLY);
            return mockJob;
        });

        when(mockJob.withProgressTracker(any(DefaultProgressTracker.class))).thenReturn(mockJob);

        // Configure job queue
        when(mockJobQueue.nextJob()).thenReturn(mockJob, mockJob, mockJob, mockJob, mockJob, null);

        // Configure retry strategy
        when(mockRetryStrategy.shouldRetry(any(), any())).thenAnswer(
                inv -> retryCount.get() < maxRetries);
        when(mockRetryStrategy.nextRetryDelay(any())).thenReturn(0L);

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJob.progressTracker()).thenReturn(Optional.ofNullable(mockProgressTracker));

        // Configure job processor to always fail
        doThrow(new RuntimeException("Simulated failure")).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(mockJobProcessor, times(maxRetries + 1)).
                            process(any()); // Initial attempt + retries
                    assertEquals(JobState.FAILED_PERMANENTLY, jobState.get());
                    assertEquals(maxRetries, retryCount.get());
                });

        // Verify the job was not retried after reaching the max retry limit
        verify(mockRetryStrategy, times(maxRetries + 1)).
                shouldRetry(any(), any()); // Retries + final attempt

        // Stop the job queue
        jobQueueManagerAPI.close();
    }

    /**
     * Method to test: Job processing in JobQueueManagerAPI
     * Given Scenario: Job succeeds on first attempt
     * ExpectedResult: Job is processed once and completes successfully without retries
     */
    @Test
    public void test_Job_SucceedsFirstAttempt() throws Exception {

        // Create a mock job
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.queueName()).thenReturn("testQueue");

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);

        when(mockJob.state()).thenAnswer(inv -> jobState.get());
        when(mockJob.withState(any())).thenAnswer(inv -> {
            jobState.set(inv.getArgument(0));
            return mockJob;
        });
        when(mockJob.markAsRunning()).thenAnswer(inv -> {
            jobState.set(JobState.RUNNING);
            return mockJob;
        });
        when(mockJob.markAsSuccessful(any())).thenAnswer(inv -> {
            jobState.set(JobState.SUCCESS);
            return mockJob;
        });
        when(mockJob.withProgressTracker(any(DefaultProgressTracker.class))).thenReturn(mockJob);

        // Configure job queue
        when(mockJobQueue.nextJob()).thenReturn(mockJob, null);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJob.progressTracker()).thenReturn(Optional.ofNullable(mockProgressTracker));
        when(mockJob.progress()).thenReturn(0f);
        when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);

        // Configure job processor to succeed
        doAnswer(inv -> {
            Job job = inv.getArgument(0);
            job.markAsSuccessful(any());
            return null;
        }).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(mockJobProcessor, times(1)).process(any());
                    assertEquals(JobState.SUCCESS, jobState.get());
                });

        // Verify the job was processed only once and completed successfully
        verify(mockRetryStrategy, never()).shouldRetry(any(), any());
        verify(mockJobQueue, times(2)).updateJobStatus(any());
        verify(mockJobQueue, times(2)).updateJobStatus(
                argThat(job -> job.state() == JobState.SUCCESS));

        // Stop the job queue
        jobQueueManagerAPI.close();
    }

    /**
     * Method to test: Job retry mechanism in JobQueueManagerAPI
     * Given Scenario: Job fails with a non-retryable error
     * ExpectedResult: Job is not retried and is marked as failed
     */
    @Test
    public void test_Job_NotRetryable() throws Exception {

        // Create a mock job
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.queueName()).thenReturn("testQueue");

        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);

        when(mockJob.state()).thenAnswer(inv -> jobState.get());
        when(mockJob.withState(any())).thenAnswer(inv -> {
            jobState.set(inv.getArgument(0));
            return mockJob;
        });
        when(mockJob.markAsRunning()).thenAnswer(inv -> {
            jobState.set(JobState.RUNNING);
            return mockJob;
        });
        when(mockJob.markAsFailed(any())).thenAnswer(inv -> {
            jobState.set(JobState.FAILED);
            return mockJob;
        });
        when(mockJob.withProgressTracker(any(DefaultProgressTracker.class))).thenReturn(mockJob);

        // Configure job queue
        when(mockJobQueue.nextJob()).thenReturn(mockJob, mockJob, null);

        // Configure retry strategy to not retry
        when(mockRetryStrategy.shouldRetry(any(), any())).thenReturn(false);

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJob.progressTracker()).thenReturn(Optional.ofNullable(mockProgressTracker));
        when(mockJob.progress()).thenReturn(0f);
        when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);

        // Configure job processor to fail
        doThrow(new RuntimeException("Non-retryable error")).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(mockJobProcessor, times(1)).process(any());
                    assertEquals(JobState.FAILED, jobState.get());
                });

        // Verify the job was not retried
        verify(mockRetryStrategy, times(1)).shouldRetry(any(), any());
        verify(mockJobQueue, times(1)).putJobBackInQueue(any());

        // Capture and verify the error details
        ArgumentCaptor<JobResult> jobResultCaptor = ArgumentCaptor.forClass(JobResult.class);
        verify(mockJob).markAsFailed(jobResultCaptor.capture());
        ErrorDetail capturedErrorDetail = jobResultCaptor.getValue().errorDetail().get();
        assertEquals("Non-retryable error", capturedErrorDetail.message());

        // Stop the job queue
        jobQueueManagerAPI.close();
    }

    /**
     * Method to test: Circuit breaker mechanism in JobQueueManagerAPI
     * Given Scenario: Multiple job failures occur
     * ExpectedResult: Circuit breaker opens after threshold is reached
     */
    @Test
    public void test_CircuitBreaker_Opens() throws Exception {

        // Create a job that always fails
        Job failingJob = mock(Job.class);
        when(failingJob.id()).thenReturn("job123");
        when(failingJob.queueName()).thenReturn("testQueue");

        // Set up the job queue to return the failing job a limited number of times
        AtomicInteger jobCount = new AtomicInteger(0);
        when(mockJobQueue.nextJob()).thenAnswer(invocation -> {
            if (jobCount.getAndIncrement() < 10) { // Limit to 10 jobs
                return failingJob;
            }
            return null;
        });
        when(mockJobQueue.getJob(anyString())).thenReturn(failingJob);
        when(failingJob.withState(any())).thenReturn(failingJob);
        when(failingJob.markAsFailed(any())).thenReturn(failingJob);
        when(failingJob.markAsRunning()).thenReturn(failingJob);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(failingJob.progressTracker()).thenReturn(Optional.ofNullable(mockProgressTracker));
        when(failingJob.progress()).thenReturn(0f);
        when(failingJob.withProgress(anyFloat())).thenReturn(failingJob);

        // Configure the processor to always throw an exception
        doThrow(new RuntimeException("Simulated failure")).when(mockJobProcessor).process(any());

        // Create a real CircuitBreaker with a low threshold for testing
        CircuitBreaker circuitBreaker = new CircuitBreaker(5, 60000);

        // Create JobQueueManagerAPIImpl with the real CircuitBreaker
        JobQueueManagerAPI jobQueueManagerAPI = newJobQueueManagerAPI(
                mockJobQueue, circuitBreaker, mockRetryStrategy, jobProcessorFactory,
                retryPolicyProcessor, abandonedJobDetector,
                jobProcessorDiscovery, 1
        );

        jobQueueManagerAPI.registerProcessor("testQueue", JobProcessor.class);

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for the circuit breaker to open (should happen after 5 failures)
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> !circuitBreaker.allowRequest());

        // Verify that the correct number of jobs were processed before the circuit opened
        verify(mockJobProcessor, times(5)).process(any());

        // Verify that no more jobs are processed while the circuit is open waiting for two seconds
        long startTime = System.currentTimeMillis();
        Awaitility.await()
                .atMost(3, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> System.currentTimeMillis() - startTime >= 2000);

        verify(mockJobProcessor, times(5)).process(any());

        // Verify the final circuit breaker status
        assertFalse("Circuit breaker should be open", circuitBreaker.allowRequest());
        assertEquals(5, circuitBreaker.getFailureCount(), "Failure count should be 5");

        jobQueueManagerAPI.close();
    }

    /**
     * Method to test: Circuit breaker mechanism in JobQueueManagerAPI
     * Given Scenario: Circuit breaker is open and then jobs start succeeding
     * ExpectedResult: Circuit breaker closes after successful job completions
     */
    @Test
    public void test_CircuitBreaker_Closes() throws Exception {

        // Create a job that initially fails but then succeeds
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.queueName()).thenReturn("testQueue");

        // Set up the job queue to return our job
        when(mockJobQueue.nextJob()).thenReturn(mockJob);
        when(mockJobQueue.getJob(anyString())).thenReturn(mockJob);
        when(mockJob.withState(any())).thenReturn(mockJob);
        when(mockJob.markAsRunning()).thenReturn(mockJob);
        when(mockJob.markAsFailed(any())).thenReturn(mockJob);
        when(mockJob.withProgressTracker(any(DefaultProgressTracker.class))).thenReturn(mockJob);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJob.progressTracker()).thenReturn(Optional.ofNullable(mockProgressTracker));
        when(mockJob.progress()).thenReturn(0f);
        when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);

        // Configure the processor to fail 5 times and then succeed
        AtomicInteger processCount = new AtomicInteger(0);
        doAnswer(inv -> {
            if (processCount.getAndIncrement() < 5) {
                throw new RuntimeException("Simulated failure");
            }

            Job processingJob = inv.getArgument(0);
            processingJob.markAsSuccessful(any());
            return null;
        }).when(mockJobProcessor).process(any());

        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);
        when(mockJob.markAsSuccessful(any())).thenAnswer(inv -> {
            jobState.set(JobState.SUCCESS);
            return mockJob;
        });

        // Create a real CircuitBreaker with a low threshold for testing
        CircuitBreaker circuitBreaker = new CircuitBreaker(5,
                1000); // Short reset timeout for testing

        // Create JobQueueManagerAPIImpl with the real CircuitBreaker
        JobQueueManagerAPI jobQueueManagerAPI = newJobQueueManagerAPI(
                mockJobQueue, circuitBreaker, mockRetryStrategy, jobProcessorFactory,
                retryPolicyProcessor, abandonedJobDetector,
                jobProcessorDiscovery, 1
        );
        jobQueueManagerAPI.registerProcessor("testQueue", JobProcessor.class);

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for the circuit breaker to open
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertFalse(circuitBreaker.allowRequest()));

        // Wait for the circuit breaker to close
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertTrue(circuitBreaker.allowRequest());
                    assertEquals(JobState.SUCCESS, jobState.get());
                });

        verify(mockJobProcessor, atLeast(6)).process(any());

        jobQueueManagerAPI.close();
    }

    /**
     * Method to test: Circuit breaker reset in JobQueueManagerAPI
     * Given Scenario: Circuit breaker is open
     * ExpectedResult: Circuit breaker closes immediately after manual reset
     */
    @Test
    public void test_CircuitBreaker_Reset() throws Exception {

        // Create a failing job
        Job failingJob = mock(Job.class);
        when(failingJob.id()).thenReturn("job123");
        when(failingJob.queueName()).thenReturn("testQueue");

        // Set up the job queue to return the failing job
        when(mockJobQueue.nextJob()).thenReturn(failingJob);
        when(mockJobQueue.getJob(anyString())).thenReturn(failingJob);
        when(failingJob.withState(any())).thenReturn(failingJob);
        when(failingJob.markAsFailed(any())).thenReturn(failingJob);
        when(failingJob.markAsRunning()).thenReturn(failingJob);
        when(failingJob.withProgressTracker(any(DefaultProgressTracker.class))).thenReturn(
                failingJob);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(failingJob.progressTracker()).thenReturn(Optional.ofNullable(mockProgressTracker));
        when(failingJob.progress()).thenReturn(0f);
        when(failingJob.withProgress(anyFloat())).thenReturn(failingJob);

        // Configure the processor to always throw an exception
        doThrow(new RuntimeException("Simulated failure")).when(mockJobProcessor).process(any());

        // Create a real CircuitBreaker with a low threshold for testing
        CircuitBreaker circuitBreaker = new CircuitBreaker(5, 60000);

        // Create JobQueueManagerAPIImpl with the real CircuitBreaker
        JobQueueManagerAPI jobQueueManagerAPI = newJobQueueManagerAPI(
                mockJobQueue, circuitBreaker, mockRetryStrategy, jobProcessorFactory,
                retryPolicyProcessor, abandonedJobDetector,
                jobProcessorDiscovery, 1
        );
        jobQueueManagerAPI.registerProcessor("testQueue", JobProcessor.class);

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for the circuit breaker to open
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> !circuitBreaker.allowRequest());

        // Verify that the circuit breaker is open
        assertFalse("Circuit breaker should be open", circuitBreaker.allowRequest());

        // Manually reset the circuit breaker
        circuitBreaker.reset();

        // Verify that the circuit breaker is now closed
        assertTrue("Circuit breaker should be closed after reset", circuitBreaker.allowRequest());
        assertEquals(0, circuitBreaker.getFailureCount(), "Failure count should be reset to 0");

        jobQueueManagerAPI.close();
    }

    /**
     * Method to test: cancelJob in JobQueueManagerAPI
     * Given Scenario: Valid job ID for a cancellable job is provided
     * ExpectedResult: Job is successfully canceled and its status is updated
     */
    @Test
    public void test_simple_cancelJob2()
            throws DotDataException, JobQueueException, JobCancellationException {

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            // Set up the job queue manager to return our mock cancellable processor
            final String testQueue = "CancellableTestQueue";
            final String jobIdIn = "job2";

            // Create a mock job
            Job mockJob = mock(Job.class);
            when(mockJob.queueName()).thenReturn(testQueue);
            when(mockJob.id()).thenReturn(jobIdIn);
            when(mockJob.state()).thenReturn(JobState.RUNNING);
            when(mockJob.withState(any(JobState.class))).thenReturn(mockJob);

            // Mock job queue operations
            when(mockJobQueue.createJob(anyString(), anyMap())).thenReturn(jobIdIn);
            when(mockJobQueue.getJob(anyString())).thenReturn(mockJob);
            doNothing().when(mockJobQueue).updateJobStatus(any(Job.class));

            // Create Mock Job Event
            JobCancelRequestEvent mockEvent = mock(JobCancelRequestEvent.class);
            when(mockEvent.getJob()).thenReturn(mockJob);

            // Mock system events
            LocalSystemEventsAPI localSystemEventsAPI = mock(LocalSystemEventsAPI.class);
            apiLocator.when(APILocator::getLocalSystemEventsAPI).thenReturn(localSystemEventsAPI);

            // Handle the event notification
            doAnswer(invocation -> {
                JobCancelRequestEvent event = invocation.getArgument(0);
                ((JobQueueManagerAPIImpl) jobQueueManagerAPI).onCancelRequestJob(event);
                return null;
            }).when(localSystemEventsAPI).notify(any(JobCancelRequestEvent.class));

            jobQueueManagerAPI.registerProcessor(testQueue, SimpleCancellableJobProcessor.class);

            // Create a job so we can cancel it
            final String jobIdOut = jobQueueManagerAPI.createJob(testQueue, Map.of());
            assertEquals(jobIdIn, jobIdOut);

            // Perform the cancellation
            jobQueueManagerAPI.cancelJob(jobIdOut);

            // Verify that the cancel method was called on our mock processor
            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> {
                        verify(mockCancellableProcessor).cancel(mockJob);
                    });

        }
    }

    /**
     * Method to test: Job cancellation in JobQueueManagerAPI
     * Given Scenario: Running job is canceled
     * ExpectedResult: Job is successfully canceled and its state transitions are correct
     */
    @Test
    public void test_complex_cancelJob() throws Exception {

        try (MockedStatic<APILocator> apiLocator = mockStatic(APILocator.class)) {

            final String testQueue = "myTestQueue";
            final String jobId = "job5644";

            // Create a mock job
            Job mockJob = mock(Job.class);
            when(mockJob.queueName()).thenReturn(testQueue);
            when(mockJob.id()).thenReturn(jobId);
            when(mockJob.state()).thenReturn(JobState.RUNNING);
            when(mockJob.withState(any(JobState.class))).thenReturn(mockJob);

            // Configure JobQueue
            when(mockJobQueue.getJob(jobId)).thenReturn(mockJob);
            when(mockJobQueue.nextJob()).thenReturn(mockJob).thenReturn(null);
            when(mockJobQueue.hasJobBeenInState(any(), eq(JobState.CANCEL_REQUESTED),
                    eq(JobState.CANCELLING))).thenReturn(true);
            when(mockJobQueue.createJob(anyString(), anyMap())).thenReturn(jobId);

            // List to capture job state updates
            List<JobState> stateUpdates = new CopyOnWriteArrayList<>();

            when(mockJob.withState(any())).thenAnswer(inv -> {
                stateUpdates.add(inv.getArgument(0));
                return mockJob;
            });
            when(mockJob.markAsRunning()).thenAnswer(inv -> {
                stateUpdates.add(JobState.RUNNING);
                return mockJob;
            });
            when(mockJob.markAsCanceled(any())).thenAnswer(inv -> {
                stateUpdates.add(JobState.CANCELED);
                return mockJob;
            });
            when(mockJob.markAsSuccessful(any())).thenAnswer(inv -> {
                stateUpdates.add(JobState.SUCCESS);
                return mockJob;
            });
            when(mockJob.markAsFailed(any())).thenAnswer(inv -> {
                stateUpdates.add(JobState.FAILED);
                return mockJob;
            });
            when(mockJob.progress()).thenReturn(0f);
            when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);
            when(mockJob.withProgressTracker(any(DefaultProgressTracker.class))).thenReturn(
                    mockJob);

            // Create Mock Job Event
            JobCancelRequestEvent mockEvent = mock(JobCancelRequestEvent.class);
            when(mockEvent.getJob()).thenReturn(mockJob);

            // Mock system events
            LocalSystemEventsAPI localSystemEventsAPI = mock(LocalSystemEventsAPI.class);
            apiLocator.when(APILocator::getLocalSystemEventsAPI).thenReturn(localSystemEventsAPI);

            // Handle the event notification
            doAnswer(invocation -> {
                JobCancelRequestEvent event = invocation.getArgument(0);
                ((JobQueueManagerAPIImpl) jobQueueManagerAPI).onCancelRequestJob(event);
                return null;
            }).when(localSystemEventsAPI).notify(any(JobCancelRequestEvent.class));

            // Register the test processor
            jobQueueManagerAPI.registerProcessor(testQueue, ComplexCancellableJobProcessor.class);

            // Configure circuit breaker
            when(mockCircuitBreaker.allowRequest()).thenReturn(true);

            // Start the job queue manager
            jobQueueManagerAPI.start();

            final String jobIdOut = jobQueueManagerAPI.createJob(testQueue, Map.of());

            final Optional<JobProcessor> instance = jobQueueManagerAPI.getInstance(jobIdOut);
            assertTrue(instance.isPresent());
            // Use our TestJobProcessor
            final ComplexCancellableJobProcessor testJobProcessor = (ComplexCancellableJobProcessor) instance.get();

            // Wait for the job to start processing
            Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .until(() -> testJobProcessor.awaitProcessingStart(100, TimeUnit.MILLISECONDS));

            // Cancel the job
            jobQueueManagerAPI.cancelJob(jobId);

            // Wait for the job to complete (which should be due to cancellation)
            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .until(() -> testJobProcessor.awaitProcessingCompleted(100,
                            TimeUnit.MILLISECONDS));

            Awaitility.await()
                    .atMost(10, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .until(() -> stateUpdates.contains(JobState.CANCELED));

            // Clean up
            jobQueueManagerAPI.close();
        }
    }

    /**
     * Method to test: calculateBackoffTime in JobQueueManagerAPI
     * Given Scenario: Various empty queue counts and maximum empty queue count
     * ExpectedResult: Correct backoff times are calculated
     */
    @Test
    public void test_calculateBackoffTime() {

        JobQueueManagerAPIImpl jobQueueManager = (JobQueueManagerAPIImpl) jobQueueManagerAPI;

        assertEquals(1000L, jobQueueManager.calculateBackoffTime(0, 30));
        assertEquals(2000L, jobQueueManager.calculateBackoffTime(1, 30));
        assertEquals(4000L, jobQueueManager.calculateBackoffTime(2, 30));
        assertEquals(8000L, jobQueueManager.calculateBackoffTime(3, 30));
        assertEquals(16000L, jobQueueManager.calculateBackoffTime(4, 30));
        assertEquals(30000L, jobQueueManager.calculateBackoffTime(5, 30));
        assertEquals(30000L, jobQueueManager.calculateBackoffTime(6, 30));
        assertEquals(30000L, jobQueueManager.calculateBackoffTime(30, 30));
    }

    /**
     * Creates a new instance of the JobQueueManagerAPI with the provided configurations.
     *
     * @param jobQueue                           The job queue to be managed.
     * @param circuitBreaker                     The circuit breaker to handle job processing
     *                                           failures.
     * @param retryStrategy                      The strategy to use for retrying failed jobs.
     * @param threadPoolSize                     The size of the thread pool for job processing.
     * @return A newly created instance of JobQueueManagerAPI.
     */
    private JobQueueManagerAPI newJobQueueManagerAPI(JobQueue jobQueue,
            CircuitBreaker circuitBreaker,
            RetryStrategy retryStrategy,
            JobProcessorFactory jobProcessorFactory,
            RetryPolicyProcessor retryPolicyProcessor,
            AbandonedJobDetector abandonedJobDetector,
            JobProcessorDiscovery jobProcessorDiscovery,
            int threadPoolSize) {

        final var realTimeJobMonitor = new RealTimeJobMonitor();

        return new JobQueueManagerAPIImpl(
                jobQueue, new JobQueueConfig(threadPoolSize),
                circuitBreaker, retryStrategy, realTimeJobMonitor,
                jobProcessorFactory, retryPolicyProcessor, abandonedJobDetector,
                jobProcessorDiscovery
        );
    }

}
