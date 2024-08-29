package com.dotcms.jobs.business.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.jobs.business.error.CircuitBreaker;
import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.jobs.business.queue.JobQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

public class JobQueueManagerAPITest {

    private JobQueue mockJobQueue;

    private JobProcessor mockJobProcessor;

    private RetryStrategy mockRetryStrategy;

    private CircuitBreaker mockCircuitBreaker;

    private JobQueueManagerAPIImpl jobQueueManagerAPI;

    @Before
    public void setUp() {

        mockJobQueue = mock(JobQueue.class);
        mockJobProcessor = mock(JobProcessor.class);
        mockRetryStrategy = mock(RetryStrategy.class);
        mockCircuitBreaker = mock(CircuitBreaker.class);

        jobQueueManagerAPI = new JobQueueManagerAPIImpl(mockJobQueue, 1, mockCircuitBreaker);
        jobQueueManagerAPI.registerProcessor("testQueue", mockJobProcessor);
        jobQueueManagerAPI.setRetryStrategy("testQueue", mockRetryStrategy);
    }

    @Test
    public void test_createJob() {

        Map<String, Object> parameters = new HashMap<>();
        when(mockJobQueue.addJob(anyString(), anyMap())).thenReturn("job123");

        // Creating a job
        String jobId = jobQueueManagerAPI.createJob("testQueue", parameters);

        assertEquals("job123", jobId);
        verify(mockJobQueue).addJob("testQueue", parameters);
    }

    @Test
    public void test_getJob() {

        Job mockJob = mock(Job.class);
        when(mockJobQueue.getJob("job123")).thenReturn(mockJob);

        // Getting a job
        Job result = jobQueueManagerAPI.getJob("job123");

        assertEquals(mockJob, result);
        verify(mockJobQueue).getJob("job123");
    }

    @Test
    public void test_getJobs() {

        // Prepare test data
        Job job1 = mock(Job.class);
        Job job2 = mock(Job.class);
        List<Job> expectedJobs = Arrays.asList(job1, job2);

        // Mock the behavior of jobQueue.getJobs
        when(mockJobQueue.getJobs(1, 10)).thenReturn(expectedJobs);

        // Call the method under test
        List<Job> actualJobs = jobQueueManagerAPI.getJobs(1, 10);

        // Verify the results
        assertEquals(expectedJobs, actualJobs);
        verify(mockJobQueue).getJobs(1, 10);
    }

    @Test
    public void testCancelJob() {

        Job mockJob = mock(Job.class);
        when(mockJobQueue.getJob("job123")).thenReturn(mockJob);
        when(mockJob.queueName()).thenReturn("testQueue");
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.withState(any())).thenReturn(mockJob);

        when(mockJobProcessor.canCancel(mockJob)).thenReturn(true);

        jobQueueManagerAPI.cancelJob("job123");

        verify(mockJobProcessor).cancel(mockJob);
        verify(mockJobQueue).updateJobStatus(any(Job.class));
    }

    @Test
    public void test_start() throws InterruptedException {

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        assertFalse(jobQueueManagerAPI.isStarted());

        jobQueueManagerAPI.start();

        assertTrue(jobQueueManagerAPI.isStarted());
        assertTrue(jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS));

        // Verify that jobs are being processed
        verify(mockJobQueue, timeout(5000).atLeastOnce()).nextJob();
    }

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

        // Verify that no more jobs are being processed
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    int count = jobCheckCount.get();
                    Thread.sleep(500); // Wait a bit to see if count increases
                    return jobCheckCount.get()
                            == count; // If count hasn't increased, job processing has stopped
                });

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

    @Test
    public void test_watchJob() throws Exception {

        // Prepare test data
        String jobId = "testJobId";
        Job initialJob = mock(Job.class);
        when(initialJob.id()).thenReturn(jobId);
        when(initialJob.queueName()).thenReturn("testQueue");
        when(initialJob.state()).thenReturn(JobState.PENDING);

        // Mock behavior for job state changes
        Job runningJob = mock(Job.class);
        when(runningJob.id()).thenReturn(jobId);
        when(runningJob.queueName()).thenReturn("testQueue");
        when(runningJob.state()).thenReturn(JobState.RUNNING);
        when(initialJob.withState(JobState.RUNNING)).thenReturn(runningJob);

        Job completedJob = mock(Job.class);
        when(completedJob.id()).thenReturn(jobId);
        when(completedJob.queueName()).thenReturn("testQueue");
        when(completedJob.state()).thenReturn(JobState.COMPLETED);
        when(runningJob.markAsCompleted()).thenReturn(completedJob);

        // Mock JobQueue behavior
        when(mockJobQueue.getJob(jobId)).thenReturn(initialJob);
        when(mockJobQueue.nextJob()).thenReturn(initialJob).thenReturn(null);

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        // Mock JobProcessor behavior
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);
        when(runningJob.progress()).thenReturn(0f);
        when(runningJob.withProgress(anyFloat())).thenReturn(runningJob);

        // Create a list to capture job states
        List<JobState> capturedStates = Collections.synchronizedList(new ArrayList<>());

        // Create a test watcher
        Consumer<Job> testWatcher = job -> {
            assertNotNull(job);
            assertEquals(jobId, job.id());
            capturedStates.add(job.state());
        };

        // Start the JobQueueManagerAPI
        jobQueueManagerAPI.start();

        // Register the watcher
        jobQueueManagerAPI.watchJob(jobId, testWatcher);

        // Wait for job processing to complete
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> capturedStates.contains(JobState.COMPLETED));

        // Verify job processing
        verify(mockJobQueue, timeout(5000)).updateJobStatus(runningJob);
        verify(mockJobProcessor, timeout(5000)).process(runningJob);
        verify(mockJobQueue, timeout(5000)).updateJobStatus(completedJob);

        // Verify watcher received all job states
        assertTrue(capturedStates.contains(JobState.PENDING));
        assertTrue(capturedStates.contains(JobState.RUNNING));
        assertTrue(capturedStates.contains(JobState.COMPLETED));

        // Stop the JobQueueManagerAPI
        jobQueueManagerAPI.close();
    }

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

        when(mockJob.withState(any())).thenAnswer(inv -> {
            jobState.set(inv.getArgument(0));
            return mockJob;
        });
        when(mockJob.incrementRetry()).thenAnswer(inv -> {
            retryCount.incrementAndGet();
            return mockJob;
        });
        when(mockJob.markAsCompleted()).thenAnswer(inv -> {
            jobState.set(JobState.COMPLETED);
            return mockJob;
        });
        when(mockJob.markAsFailed(any())).thenAnswer(inv -> {
            jobState.set(JobState.FAILED);
            return mockJob;
        });

        // Set up the job queue to return our mock job twice (for initial attempt and retry)
        when(mockJobQueue.nextJob()).thenReturn(mockJob, mockJob, null);

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        // Configure retry strategy
        when(mockRetryStrategy.shouldRetry(any(), any())).thenReturn(true);
        when(mockRetryStrategy.nextRetryDelay(any())).thenReturn(0L); // Immediate retry

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);
        when(mockJob.progress()).thenReturn(0f);
        when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);

        // Configure job processor to fail on first attempt, succeed on second
        AtomicInteger processAttempts = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (processAttempts.getAndIncrement() == 0) {
                throw new RuntimeException("Simulated failure");
            }
            Job job = invocation.getArgument(0);
            job.markAsCompleted();
            return null;
        }).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(mockJobProcessor, times(2)).process(any());
                    assertEquals(JobState.COMPLETED, jobState.get());
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

    @Test
    public void test_JobRetry_retry_twice() throws Exception {

        // Create a mock job
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.queueName()).thenReturn("testQueue");

        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicLong lastRetryTimestamp = new AtomicLong(System.currentTimeMillis());

        when(mockJob.state()).thenAnswer(inv -> jobState.get());
        when(mockJob.retryCount()).thenAnswer(inv -> retryCount.get());
        when(mockJob.lastRetryTimestamp()).thenAnswer(inv -> lastRetryTimestamp.get());

        when(mockJob.withState(any())).thenAnswer(inv -> {
            jobState.set(inv.getArgument(0));
            return mockJob;
        });
        when(mockJob.incrementRetry()).thenAnswer(inv -> {
            retryCount.incrementAndGet();
            lastRetryTimestamp.set(System.currentTimeMillis());
            return mockJob;
        });
        when(mockJob.markAsCompleted()).thenAnswer(inv -> {
            jobState.set(JobState.COMPLETED);
            return mockJob;
        });
        when(mockJob.markAsFailed(any())).thenAnswer(inv -> {
            jobState.set(JobState.FAILED);
            return mockJob;
        });

        // Configure job queue to always return the mockJob until it's completed
        when(mockJobQueue.nextJob()).thenAnswer(inv ->
                jobState.get() != JobState.COMPLETED ? mockJob : null
        );

        // Configure retry strategy
        when(mockRetryStrategy.shouldRetry(any(), any())).thenReturn(true);
        when(mockRetryStrategy.nextRetryDelay(any())).thenReturn(100L); // Non-zero delay

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);
        when(mockJob.progress()).thenReturn(0f);
        when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);

        // Configure job processor to fail twice, succeed on third attempt
        AtomicInteger processAttempts = new AtomicInteger(0);
        doAnswer(invocation -> {
            int attempt = processAttempts.getAndIncrement();
            if (attempt < 2) {
                throw new RuntimeException("Simulated failure");
            }
            Job job = invocation.getArgument(0);
            job.markAsCompleted();
            return null;
        }).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(mockJobProcessor, times(3)).process(any());
                    assertEquals(JobState.COMPLETED, jobState.get());
                    assertEquals(2, retryCount.get());
                });

        // Verify state transitions
        InOrder inOrder = inOrder(mockJob);
        inOrder.verify(mockJob).withState(JobState.RUNNING);
        inOrder.verify(mockJob).markAsFailed(any());
        inOrder.verify(mockJob).withState(JobState.RUNNING);
        inOrder.verify(mockJob).markAsFailed(any());
        inOrder.verify(mockJob).withState(JobState.RUNNING);
        inOrder.verify(mockJob).markAsCompleted();

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

    @Test
    public void test_JobRetry_MaxRetryLimit() throws Exception {

        // Create a mock job
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.queueName()).thenReturn("testQueue");

        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);
        AtomicInteger retryCount = new AtomicInteger(0);
        AtomicLong lastRetryTimestamp = new AtomicLong(System.currentTimeMillis());
        int maxRetries = 3;

        when(mockJob.state()).thenAnswer(inv -> jobState.get());
        when(mockJob.retryCount()).thenAnswer(inv -> retryCount.get());
        when(mockJob.lastRetryTimestamp()).thenAnswer(inv -> lastRetryTimestamp.get());

        when(mockJob.withState(any())).thenAnswer(inv -> {
            jobState.set(inv.getArgument(0));
            return mockJob;
        });
        when(mockJob.incrementRetry()).thenAnswer(inv -> {
            retryCount.incrementAndGet();
            lastRetryTimestamp.set(System.currentTimeMillis());
            return mockJob;
        });
        when(mockJob.markAsFailed(any())).thenAnswer(inv -> {
            jobState.set(JobState.FAILED);
            return mockJob;
        });

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
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);

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
                    assertEquals(JobState.FAILED, jobState.get());
                    assertEquals(maxRetries, retryCount.get());
                });

        // Verify the job was not retried after reaching the max retry limit
        verify(mockRetryStrategy, times(maxRetries + 1)).
                shouldRetry(any(), any()); // Retries + final attempt
        verify(mockJobQueue, times(1)).removeJob(mockJob.id());

        // Stop the job queue
        jobQueueManagerAPI.close();
    }

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
        when(mockJob.markAsCompleted()).thenAnswer(inv -> {
            jobState.set(JobState.COMPLETED);
            return mockJob;
        });

        // Configure job queue
        when(mockJobQueue.nextJob()).thenReturn(mockJob, null);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);
        when(mockJob.progress()).thenReturn(0f);
        when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);

        // Configure job processor to succeed
        doAnswer(inv -> {
            Job job = inv.getArgument(0);
            job.markAsCompleted();
            return null;
        }).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    verify(mockJobProcessor, times(1)).process(any());
                    assertEquals(JobState.COMPLETED, jobState.get());
                });

        // Verify the job was processed only once and completed successfully
        verify(mockRetryStrategy, never()).shouldRetry(any(), any());
        verify(mockJobQueue, times(2)).updateJobStatus(any());
        verify(mockJobQueue, times(2)).updateJobStatus(
                argThat(job -> job.state() == JobState.COMPLETED));

        // Stop the job queue
        jobQueueManagerAPI.close();
    }

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
        when(mockJob.markAsFailed(any())).thenAnswer(inv -> {
            jobState.set(JobState.FAILED);
            return mockJob;
        });

        // Configure job queue
        when(mockJobQueue.nextJob()).thenReturn(mockJob, mockJob, null);

        // Configure retry strategy to not retry
        when(mockRetryStrategy.shouldRetry(any(), any())).thenReturn(false);

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);
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
        verify(mockJobQueue, never()).putJobBackInQueue(any());
        verify(mockJobQueue, times(1)).removeJob(mockJob.id());

        // Capture and verify the error details
        ArgumentCaptor<ErrorDetail> errorDetailCaptor = ArgumentCaptor.forClass(ErrorDetail.class);
        verify(mockJob).markAsFailed(errorDetailCaptor.capture());
        ErrorDetail capturedErrorDetail = errorDetailCaptor.getValue();
        assertEquals("Non-retryable error", capturedErrorDetail.exception().getMessage());

        // Stop the job queue
        jobQueueManagerAPI.close();
    }

    @Test
    public void test_JobProgressTracking() throws Exception {

        // Create a mock job
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("progress-test-job");
        when(mockJob.queueName()).thenReturn("testQueue");

        AtomicReference<Float> jobProgress = new AtomicReference<>(0f);
        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);

        when(mockJob.state()).thenAnswer(inv -> jobState.get());
        when(mockJob.withState(any())).thenAnswer(inv -> {
            jobState.set(inv.getArgument(0));
            return mockJob;
        });
        when(mockJob.markAsCompleted()).thenAnswer(inv -> {
            jobState.set(JobState.COMPLETED);
            return mockJob;
        });

        when(mockJob.progress()).thenAnswer(inv -> jobProgress.get());
        when(mockJob.withProgress(anyFloat())).thenAnswer(inv -> {
            jobProgress.set(inv.getArgument(0));
            return mockJob;
        });

        // Set up the job queue to return our mock job
        when(mockJobQueue.nextJob()).thenReturn(mockJob).thenReturn(null);
        when(mockJobQueue.getJob(anyString())).thenReturn(mockJob);

        // Create a real ProgressTracker
        ProgressTracker realProgressTracker = new DefaultProgressTracker();
        when(mockJobProcessor.progressTracker(any())).thenReturn(realProgressTracker);

        // Make the circuit breaker always allow requests
        when(mockCircuitBreaker.allowRequest()).thenReturn(true);

        // List to store progress updates
        List<Float> progressUpdates = Collections.synchronizedList(new ArrayList<>());

        // Configure the mockJobProcessor to update progress
        doAnswer(inv -> {

            for (int i = 0; i <= 10; i++) {
                float progress = i / 10f;
                realProgressTracker.updateProgress(progress);
                // Simulate the effect of updateJobProgress
                jobProgress.set(progress);
                Thread.sleep(50); // Simulate work
            }

            Job job = inv.getArgument(0);
            job.markAsCompleted();
            return null;
        }).when(mockJobProcessor).process(any());

        // Set up a job watcher to capture progress updates
        jobQueueManagerAPI.watchJob("progress-test-job", job -> {
            progressUpdates.add(job.progress());
        });

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertEquals(JobState.COMPLETED, jobState.get());
                });

        // Verify that progress was tracked correctly
        assertTrue(
                "Should have multiple progress updates",
                progressUpdates.size() > 1
        );
        assertEquals(
                0.0f, progressUpdates.get(0), 0.01f,
                "Initial progress should be 0"
        );
        assertEquals(
                1.0f, progressUpdates.get(progressUpdates.size() - 1), 0.01f,
                "Final progress should be 1"
        );

        // Verify that progress increased monotonically
        for (int i = 1; i < progressUpdates.size(); i++) {
            assertTrue("Progress should increase or stay the same",
                    progressUpdates.get(i) >= progressUpdates.get(i - 1)
            );
        }

        // Verify that the job was processed
        verify(mockJobProcessor, times(1)).process(any());
        verify(mockJobQueue, times(2)).updateJobStatus(any());
        verify(mockJobQueue, times(2)).
                updateJobStatus(argThat(job -> job.state() == JobState.COMPLETED));

        // Stop the job queue
        jobQueueManagerAPI.close();
    }

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

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);
        when(failingJob.progress()).thenReturn(0f);
        when(failingJob.withProgress(anyFloat())).thenReturn(failingJob);

        // Configure the processor to always throw an exception
        doThrow(new RuntimeException("Simulated failure")).when(mockJobProcessor).process(any());

        // Create a real CircuitBreaker with a low threshold for testing
        CircuitBreaker circuitBreaker = new CircuitBreaker(5, 60000);

        // Create JobQueueManagerAPIImpl with the real CircuitBreaker
        jobQueueManagerAPI = new JobQueueManagerAPIImpl(mockJobQueue, 1, circuitBreaker);
        jobQueueManagerAPI.registerProcessor("testQueue", mockJobProcessor);

        // Start the job queue
        jobQueueManagerAPI.start();

        // Wait for the circuit breaker to open (should happen after 5 failures)
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> !circuitBreaker.allowRequest());

        // Verify that the correct number of jobs were processed before the circuit opened
        verify(mockJobProcessor, times(5)).process(any());

        // Verify that no more jobs are processed while the circuit is open
        Thread.sleep(1000); // Wait a bit to ensure no more processing attempts
        verify(mockJobProcessor, times(5)).process(any());

        // Verify the final circuit breaker status
        assertTrue("Circuit breaker should be open", !circuitBreaker.allowRequest());
        assertEquals(5, circuitBreaker.getFailureCount(), "Failure count should be 5");

        jobQueueManagerAPI.close();
    }

    @Test
    public void testCircuitBreakerCloses() throws Exception {

        // Create a job that initially fails but then succeeds
        Job mockJob = mock(Job.class);
        when(mockJob.id()).thenReturn("job123");
        when(mockJob.queueName()).thenReturn("testQueue");

        // Set up the job queue to return our job
        when(mockJobQueue.nextJob()).thenReturn(mockJob);
        when(mockJobQueue.getJob(anyString())).thenReturn(mockJob);
        when(mockJob.withState(any())).thenReturn(mockJob);
        when(mockJob.markAsFailed(any())).thenReturn(mockJob);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);
        when(mockJob.progress()).thenReturn(0f);
        when(mockJob.withProgress(anyFloat())).thenReturn(mockJob);

        // Configure the processor to fail 5 times and then succeed
        AtomicInteger processCount = new AtomicInteger(0);
        doAnswer(inv -> {
            if (processCount.getAndIncrement() < 5) {
                throw new RuntimeException("Simulated failure");
            }

            Job processingJob = inv.getArgument(0);
            processingJob.markAsCompleted();
            return null;
        }).when(mockJobProcessor).process(any());

        AtomicReference<JobState> jobState = new AtomicReference<>(JobState.PENDING);
        when(mockJob.markAsCompleted()).thenAnswer(inv -> {
            jobState.set(JobState.COMPLETED);
            return mockJob;
        });

        // Create a real CircuitBreaker with a low threshold for testing
        CircuitBreaker circuitBreaker = new CircuitBreaker(5,
                1000); // Short reset timeout for testing

        // Create JobQueueManagerAPIImpl with the real CircuitBreaker
        jobQueueManagerAPI = new JobQueueManagerAPIImpl(mockJobQueue, 1, circuitBreaker);
        jobQueueManagerAPI.registerProcessor("testQueue", mockJobProcessor);

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
                    assertEquals(JobState.COMPLETED, jobState.get());
                });

        verify(mockJobProcessor, atLeast(6)).process(any());

        jobQueueManagerAPI.close();
    }

    @Test
    public void testManualCircuitBreakerReset() throws Exception {

        // Create a failing job
        Job failingJob = mock(Job.class);
        when(failingJob.id()).thenReturn("job123");
        when(failingJob.queueName()).thenReturn("testQueue");

        // Set up the job queue to return the failing job
        when(mockJobQueue.nextJob()).thenReturn(failingJob);
        when(mockJobQueue.getJob(anyString())).thenReturn(failingJob);
        when(failingJob.withState(any())).thenReturn(failingJob);
        when(failingJob.markAsFailed(any())).thenReturn(failingJob);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);
        when(failingJob.progress()).thenReturn(0f);
        when(failingJob.withProgress(anyFloat())).thenReturn(failingJob);

        // Configure the processor to always throw an exception
        doThrow(new RuntimeException("Simulated failure")).when(mockJobProcessor).process(any());

        // Create a real CircuitBreaker with a low threshold for testing
        CircuitBreaker circuitBreaker = new CircuitBreaker(5, 60000);

        // Create JobQueueManagerAPIImpl with the real CircuitBreaker
        jobQueueManagerAPI = new JobQueueManagerAPIImpl(mockJobQueue, 1, circuitBreaker);
        jobQueueManagerAPI.registerProcessor("testQueue", mockJobProcessor);

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

}
