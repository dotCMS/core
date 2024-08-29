package com.dotcms.jobs.business.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
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

public class JobQueueAPITest {

    private JobQueue mockJobQueue;

    private JobProcessor mockJobProcessor;

    private RetryStrategy mockRetryStrategy;

    private JobQueueAPIImpl jobQueueAPI;

    @Before
    public void setUp() {

        mockJobQueue = mock(JobQueue.class);
        mockJobProcessor = mock(JobProcessor.class);
        mockRetryStrategy = mock(RetryStrategy.class);

        jobQueueAPI = new JobQueueAPIImpl(mockJobQueue, 1);
        jobQueueAPI.registerProcessor("testQueue", mockJobProcessor);
        jobQueueAPI.setRetryStrategy("testQueue", mockRetryStrategy);
    }

    @Test
    public void test_createJob() {

        Map<String, Object> parameters = new HashMap<>();
        when(mockJobQueue.addJob(anyString(), anyMap())).thenReturn("job123");

        // Creating a job
        String jobId = jobQueueAPI.createJob("testQueue", parameters);

        assertEquals("job123", jobId);
        verify(mockJobQueue).addJob("testQueue", parameters);
    }

    @Test
    public void test_getJob() {

        Job mockJob = mock(Job.class);
        when(mockJobQueue.getJob("job123")).thenReturn(mockJob);

        // Getting a job
        Job result = jobQueueAPI.getJob("job123");

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
        List<Job> actualJobs = jobQueueAPI.getJobs(1, 10);

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

        jobQueueAPI.cancelJob("job123");

        verify(mockJobProcessor).cancel(mockJob);
        verify(mockJobQueue).updateJobStatus(any(Job.class));
    }

    @Test
    public void test_start() throws InterruptedException {

        assertFalse(jobQueueAPI.isStarted());

        jobQueueAPI.start();

        assertTrue(jobQueueAPI.isStarted());
        assertTrue(jobQueueAPI.awaitStart(5, TimeUnit.SECONDS));

        // Verify that jobs are being processed
        verify(mockJobQueue, timeout(5000).atLeastOnce()).nextJob();
    }

    @Test
    public void test_close() throws Exception {

        // Start the JobQueueAPI
        jobQueueAPI.start();
        assertTrue(jobQueueAPI.isStarted());
        assertTrue(jobQueueAPI.awaitStart(5, TimeUnit.SECONDS));

        AtomicInteger jobCheckCount = new AtomicInteger(0);
        when(mockJobQueue.nextJob()).thenAnswer(invocation -> {
            jobCheckCount.incrementAndGet();
            return null;
        });

        // Close the JobQueueAPI
        jobQueueAPI.close();

        // Wait for the JobQueueAPI to be fully stopped
        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> !jobQueueAPI.isStarted());

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

        // Verify that we can't start the JobQueueAPIImpl again after closing
        jobQueueAPI.start();
        assertFalse(jobQueueAPI.isStarted());

        // Verify that close() can be called multiple times without error
        jobQueueAPI.close();
        assertFalse(jobQueueAPI.isStarted());
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

        // Mock JobProcessor behavior
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);

        // Create a list to capture job states
        List<JobState> capturedStates = Collections.synchronizedList(new ArrayList<>());

        // Create a test watcher
        Consumer<Job> testWatcher = job -> {
            assertNotNull(job);
            assertEquals(jobId, job.id());
            capturedStates.add(job.state());
        };

        // Start the JobQueueAPI
        jobQueueAPI.start();

        // Register the watcher
        jobQueueAPI.watchJob(jobId, testWatcher);

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

        // Stop the JobQueueAPI
        jobQueueAPI.close();
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

        // Configure retry strategy
        when(mockRetryStrategy.shouldRetry(any(), any())).thenReturn(true);
        when(mockRetryStrategy.nextRetryDelay(any())).thenReturn(0L); // Immediate retry

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);

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
        jobQueueAPI.start();

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
        jobQueueAPI.close();
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

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);

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
        jobQueueAPI.start();

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
        jobQueueAPI.close();
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

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);

        // Configure job processor to always fail
        doThrow(new RuntimeException("Simulated failure")).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueAPI.start();

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
        jobQueueAPI.close();
    }

    @Test
    public void test_Job_SucceedsFirstAttempt() throws Exception {

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
        when(mockJob.markAsCompleted()).thenAnswer(inv -> {
            jobState.set(JobState.COMPLETED);
            return mockJob;
        });

        // Configure job queue
        when(mockJobQueue.nextJob()).thenReturn(mockJob, null);

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);

        // Configure job processor to succeed
        doAnswer(inv -> {
            Job job = inv.getArgument(0);
            job.markAsCompleted();
            return null;
        }).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueAPI.start();

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
        jobQueueAPI.close();
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

        // Configure progress tracker
        ProgressTracker mockProgressTracker = mock(ProgressTracker.class);
        when(mockJobProcessor.progressTracker(any())).thenReturn(mockProgressTracker);

        // Configure job processor to fail
        doThrow(new RuntimeException("Non-retryable error")).when(mockJobProcessor).process(any());

        // Start the job queue
        jobQueueAPI.start();

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
        jobQueueAPI.close();
    }

}
