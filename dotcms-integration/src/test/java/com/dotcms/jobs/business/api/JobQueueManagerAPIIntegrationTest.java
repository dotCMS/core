package com.dotcms.jobs.business.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.jobs.business.error.ExponentialBackoffRetryStrategy;
import com.dotcms.jobs.business.error.RetryStrategy;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.Cancellable;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the JobQueueManagerAPI.
 * These tests verify the functionality of the job queue system in a real environment,
 * including job creation, processing, cancellation, retrying, and progress tracking.
 */
public class JobQueueManagerAPIIntegrationTest {

    private static JobQueueManagerAPI jobQueueManagerAPI;

    /**
     * Sets up the test environment before all tests are run.
     * Initializes the test environment and obtains an instance of JobQueueManagerAPI.
     *
     * @throws Exception if there's an error during setup
     */
    @BeforeAll
    static void setUp() throws Exception {

        // Initialize the test environment
        IntegrationTestInitService.getInstance().init();

        jobQueueManagerAPI = APILocator.getJobQueueManagerAPI();
    }

    /**
     * Cleans up the test environment after all tests have run.
     * Closes the JobQueueManagerAPI and clears all jobs from the database.
     *
     * @throws Exception if there's an error during cleanup
     */
    @AfterAll
    static void cleanUp() throws Exception {

        jobQueueManagerAPI.close();
        clearJobs();
    }

    @BeforeEach
    void reset() {
        // Reset circuit breaker
        jobQueueManagerAPI.getCircuitBreaker().reset();
    }

    /**
     * Method to test: createJob and process execution in JobQueueManagerAPI
     * Given Scenario: A job is created and submitted to the queue
     * ExpectedResult: The job is successfully created, processed, and completed within the expected timeframe
     */
    @Test
    void test_CreateAndProcessJob() throws Exception {

        // Register a test processor
        jobQueueManagerAPI.registerProcessor("testQueue", new TestJobProcessor());

        // Start the JobQueueManagerAPI
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        // Create a job
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("testParam", "testValue");
        String jobId = jobQueueManagerAPI.createJob("testQueue", parameters);

        assertNotNull(jobId, "Job ID should not be null");

        // Wait for the job to be processed
        CountDownLatch latch = new CountDownLatch(1);
        jobQueueManagerAPI.watchJob(jobId, job -> {
            if (job.state() == JobState.COMPLETED) {
                latch.countDown();
            }
        });

        boolean processed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(processed, "Job should be processed within 10 seconds");

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Job job = jobQueueManagerAPI.getJob(jobId);
                    assertEquals(JobState.COMPLETED, job.state(),
                            "Job should be in COMPLETED state");
                });
    }

    /**
     * Method to test: Job failure handling in JobQueueManagerAPI
     * Given Scenario: A job is created that is designed to fail
     * ExpectedResult: The job fails, is marked as FAILED, and contains the expected error details
     */
    @Test
    void test_FailingJob() throws Exception {

        jobQueueManagerAPI.registerProcessor("failingQueue", new FailingJobProcessor());
        RetryStrategy contentImportRetryStrategy = new ExponentialBackoffRetryStrategy(
                5000, 300000, 2.0, 0
        );
        jobQueueManagerAPI.setRetryStrategy("failingQueue", contentImportRetryStrategy);

        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        Map<String, Object> parameters = new HashMap<>();
        String jobId = jobQueueManagerAPI.createJob("failingQueue", parameters);

        CountDownLatch latch = new CountDownLatch(1);
        jobQueueManagerAPI.watchJob(jobId, job -> {
            if (job.state() == JobState.FAILED) {
                latch.countDown();
            }
        });

        boolean processed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(processed, "Job should be processed (and fail) within 10 seconds");

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Job job = jobQueueManagerAPI.getJob(jobId);
                    assertEquals(JobState.FAILED, job.state(),
                            "Job should be in FAILED state");
                    assertNotNull(job.result().get().errorDetail().get(),
                            "Job should have an error detail");
                    assertEquals("Simulated failure",
                            job.result().get().errorDetail().get().message(),
                            "Error message should match");
                });
    }

    /**
     * Method to test: cancelJob method in JobQueueManagerAPI
     * Given Scenario: A running job is canceled
     * ExpectedResult: The job is successfully canceled, its state is set to CANCELED, and the
     * processor acknowledges the cancellation
     */
    @Test
    void test_CancelJob() throws Exception {

        CancellableJobProcessor processor = new CancellableJobProcessor();
        jobQueueManagerAPI.registerProcessor("cancellableQueue", processor);

        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        Map<String, Object> parameters = new HashMap<>();
        String jobId = jobQueueManagerAPI.createJob("cancellableQueue", parameters);

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
            .until(() -> {
                Job job = jobQueueManagerAPI.getJob(jobId);
                return job.state() == JobState.RUNNING;
            });

        // Cancel the job
        jobQueueManagerAPI.cancelJob(jobId);

        CountDownLatch latch = new CountDownLatch(1);
        jobQueueManagerAPI.watchJob(jobId, job -> {
            if (job.state() == JobState.CANCELED) {
                latch.countDown();
            }
        });

        boolean processed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(processed, "Job should be canceled within 10 seconds");

        // Wait for job processing to complete
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Job job = jobQueueManagerAPI.getJob(jobId);
                    assertEquals(JobState.CANCELED, job.state(),
                            "Job should be in CANCELED state");
                    assertTrue(processor.wasCanceled(),
                            "Job processor should have been canceled");
                });
    }

    /**
     * Method to test: Job retry mechanism in JobQueueManagerAPI
     * Given Scenario: A job is created that fails initially but succeeds after a certain number
     * of retries
     * ExpectedResult: The job is retried the configured number of times, eventually succeeds, and
     * is marked as COMPLETED
     */
    @Test
    void test_JobRetry() throws Exception {

        int maxRetries = 3;
        RetryingJobProcessor processor = new RetryingJobProcessor(maxRetries);
        jobQueueManagerAPI.registerProcessor("retryQueue", processor);

        RetryStrategy retryStrategy = new ExponentialBackoffRetryStrategy(
                100, 1000, 2.0, maxRetries
        );
        jobQueueManagerAPI.setRetryStrategy("retryQueue", retryStrategy);

        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        Map<String, Object> parameters = new HashMap<>();
        String jobId = jobQueueManagerAPI.createJob("retryQueue", parameters);

        CountDownLatch latch = new CountDownLatch(1);
        jobQueueManagerAPI.watchJob(jobId, job -> {
            if (job.state() == JobState.COMPLETED) {
                latch.countDown();
            }
        });

        boolean processed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(processed, "Job should be processed within 30 seconds");

        // Wait for job processing to complete
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Job job = jobQueueManagerAPI.getJob(jobId);
                    assertEquals(JobState.COMPLETED, job.state(),
                            "Job should be in COMPLETED state");
                    assertEquals(maxRetries + 1, processor.getAttempts(),
                            "Job should have been attempted " + maxRetries + " times");
                });
    }

    /**
     * Method to test: Progress tracking functionality in JobQueueManagerAPI
     * Given Scenario: A job is created that reports progress during its execution
     * ExpectedResult: Progress updates are received, increase monotonically, and the job completes
     * with 100% progress
     */
    @Test
    void test_JobWithProgressTracker() throws Exception {

        // Register a processor that uses progress tracking
        ProgressTrackingJobProcessor processor = new ProgressTrackingJobProcessor();
        jobQueueManagerAPI.registerProcessor("progressQueue", processor);

        // Start the JobQueueManagerAPI
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        // Create a job
        Map<String, Object> parameters = new HashMap<>();
        String jobId = jobQueueManagerAPI.createJob("progressQueue", parameters);

        List<Float> progressUpdates = Collections.synchronizedList(new ArrayList<>());

        // Watch the job and collect progress updates
        jobQueueManagerAPI.watchJob(jobId, job -> {
            progressUpdates.add(job.progress());
        });

        // Wait for the job to complete
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    Job job = jobQueueManagerAPI.getJob(jobId);
                    return job.state() == JobState.COMPLETED;
                });

        // Verify final job state
        Job completedJob = jobQueueManagerAPI.getJob(jobId);
        assertEquals(JobState.COMPLETED, completedJob.state(), "Job should be in COMPLETED state");
        assertEquals(1.0f, completedJob.progress(), 0.01f, "Final progress should be 1.0");

        // Verify progress updates
        assertFalse(progressUpdates.isEmpty(), "Should have received progress updates");
        assertEquals(0.0f, progressUpdates.get(0), 0.01f, "Initial progress should be 0");
        assertEquals(1.0f, progressUpdates.get(progressUpdates.size() - 1), 0.01f, "Final progress should be 1");

        // Verify that progress increased monotonically
        for (int i = 1; i < progressUpdates.size(); i++) {
            assertTrue(progressUpdates.get(i) >= progressUpdates.get(i - 1),
                    "Progress should increase or stay the same");
        }
    }

    /**
     * Method to test: Multiple scenarios in JobQueueManagerAPI including success, failure, and
     * cancellation Given Scenario: Multiple jobs are created simultaneously with different expected
     * outcomes:
     * - Two jobs expected to succeed
     * - One job expected to fail and be retried
     * - One job to be canceled mid-execution ExpectedResult: All jobs reach their expected final
     * states within the timeout period:
     * - Successful jobs complete
     * - Failing job retries and ultimately fails
     * - Cancellable job is successfully canceled All job states and related details (retry counts,
     * error details, cancellation status) are verified
     */
    @Test
    void test_CombinedScenarios() throws Exception {

        // Register processors for different scenarios
        jobQueueManagerAPI.registerProcessor("successQueue", new TestJobProcessor());
        jobQueueManagerAPI.registerProcessor("failQueue", new FailingJobProcessor());
        jobQueueManagerAPI.registerProcessor("cancelQueue", new CancellableJobProcessor());

        // Set up retry strategy for failing jobs
        RetryStrategy retryStrategy = new ExponentialBackoffRetryStrategy(
                100, 1000, 2.0, 2
        );
        jobQueueManagerAPI.setRetryStrategy("failQueue", retryStrategy);

        // Ensure JobQueueManagerAPI is started
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        // Create jobs
        String successJob1Id = jobQueueManagerAPI.createJob("successQueue", new HashMap<>());
        String successJob2Id = jobQueueManagerAPI.createJob("successQueue", new HashMap<>());
        String failJobId = jobQueueManagerAPI.createJob("failQueue", new HashMap<>());
        String cancelJobId = jobQueueManagerAPI.createJob("cancelQueue", new HashMap<>());

        // Set up latches to track job completions
        CountDownLatch successLatch = new CountDownLatch(2);
        CountDownLatch failLatch = new CountDownLatch(1);
        CountDownLatch cancelLatch = new CountDownLatch(1);

        // Watch jobs
        jobQueueManagerAPI.watchJob(successJob1Id, job -> {
            if (job.state() == JobState.COMPLETED) {
                successLatch.countDown();
            }
        });
        jobQueueManagerAPI.watchJob(successJob2Id, job -> {
            if (job.state() == JobState.COMPLETED) {
                successLatch.countDown();
            }
        });
        jobQueueManagerAPI.watchJob(failJobId, job -> {
            if (job.state() == JobState.FAILED) {
                failLatch.countDown();
            }
        });
        jobQueueManagerAPI.watchJob(cancelJobId, job -> {
            if (job.state() == JobState.CANCELED) {
                cancelLatch.countDown();
            }
        });

        // Wait a bit before cancelling the job
        Awaitility.await().pollDelay(500, TimeUnit.MILLISECONDS).until(() -> true);
        jobQueueManagerAPI.cancelJob(cancelJobId);

        // Wait for all jobs to complete (or timeout after 30 seconds)
        boolean allCompleted = successLatch.await(30, TimeUnit.SECONDS)
                && failLatch.await(30, TimeUnit.SECONDS)
                && cancelLatch.await(30, TimeUnit.SECONDS);

        assertTrue(allCompleted, "All jobs should complete within the timeout period");

        // Verify final states
        assertEquals(JobState.COMPLETED, jobQueueManagerAPI.getJob(successJob1Id).state(),
                "First success job should be completed");
        assertEquals(JobState.COMPLETED, jobQueueManagerAPI.getJob(successJob2Id).state(),
                "Second success job should be completed");
        assertEquals(JobState.FAILED, jobQueueManagerAPI.getJob(failJobId).state(),
                "Fail job should be in failed state");
        assertEquals(JobState.CANCELED, jobQueueManagerAPI.getJob(cancelJobId).state(),
                "Cancel job should be canceled");

        // Wait for job processing to complete as we have retries running
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Job failedJob = jobQueueManagerAPI.getJob(failJobId);
                    assertEquals(2, failedJob.retryCount(),
                            "Job should have been retried " + 2 + " times");
                    assertEquals(JobState.FAILED, failedJob.state(),
                            "Job should be in FAILED state");
                    assertTrue(failedJob.result().isPresent(),
                            "Failed job should have a result");
                    assertTrue(failedJob.result().get().errorDetail().isPresent(),
                            "Failed job should have error details");
                });
    }

    private static class ProgressTrackingJobProcessor implements JobProcessor {
        @Override
        public void process(Job job) {
            ProgressTracker tracker = job.progressTracker().orElseThrow(
                    () -> new IllegalStateException("Progress tracker not set")
            );
            for (int i = 0; i <= 10; i++) {
                float progress = i / 10.0f;
                tracker.updateProgress(progress);

                // Simulate work being done
                Awaitility.await().pollDelay(500, TimeUnit.MILLISECONDS).until(() -> true);
            }
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return new HashMap<>();
        }
    }

    private static class RetryingJobProcessor implements JobProcessor {

        private final int maxRetries;
        private int attempts = 0;

        public RetryingJobProcessor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public void process(Job job) {
            attempts++;
            if (attempts <= maxRetries) {
                throw new RuntimeException("Simulated failure, attempt " + attempts);
            }
            // If we've reached here, we've exceeded maxRetries and the job should succeed
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("attempts", attempts);
            return metadata;
        }

        public int getAttempts() {
            return attempts;
        }
    }

    private static class FailingJobProcessor implements JobProcessor {

        @Override
        public void process(Job job) {
            throw new RuntimeException("Simulated failure");
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return new HashMap<>();
        }
    }

    private static class CancellableJobProcessor implements JobProcessor, Cancellable {

        private final AtomicBoolean canceled = new AtomicBoolean(false);
        private final AtomicBoolean wasCanceled = new AtomicBoolean(false);

        @Override
        public void process(Job job) {

            Awaitility.await().atMost(10, TimeUnit.SECONDS)
                    .until(canceled::get);

            // Simulate some additional work after cancellation
            Awaitility.await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
        }

        @Override
        public void cancel(Job job) {
            canceled.set(true);
            wasCanceled.set(true);
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return new HashMap<>();
        }

        public boolean wasCanceled() {
            return wasCanceled.get();
        }
    }

    private static class TestJobProcessor implements JobProcessor {

        @Override
        public void process(Job job) {
            // Simulate some work
            Awaitility.await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return new HashMap<>();
        }
    }

    /**
     * Helper method to clear all jobs from the database
     */
    private static void clearJobs() {
        try {
            new DotConnect().setSQL("delete from job_history").loadResult();
            new DotConnect().setSQL("delete from job_queue").loadResult();
            new DotConnect().setSQL("delete from job").loadResult();
        } catch (DotDataException e) {
            Logger.warn(JobQueueManagerAPIIntegrationTest.class, "Error cleaning up jobs", e);
        }
    }

}