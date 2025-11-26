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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for the JobQueueManagerAPI.
 * These tests verify the functionality of the job queue system in a real environment,
 * including job creation, processing, cancellation, retrying, and progress tracking.
 */
@EnableWeld
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(Lifecycle.PER_CLASS)
public class JobQueueManagerAPIIntegrationTest extends com.dotcms.Junit5WeldBaseTest {

    private static int attempts = 0;

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

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

        Config.setProperty("JOB_ABANDONMENT_DETECTION_INTERVAL_MINUTES", "1");
        Config.setProperty("JOB_ABANDONMENT_THRESHOLD_MINUTES", "2");
    }

    /**
     * Cleans up the test environment after all tests have run.
     * Closes the JobQueueManagerAPI and clears all jobs from the database.
     *
     * @throws Exception if there's an error during cleanup
     */
    @AfterAll
    void cleanUp() throws Exception {

        clearJobs();

        Config.setProperty("JOB_ABANDONMENT_DETECTION_INTERVAL_MINUTES", "5");
        Config.setProperty("JOB_ABANDONMENT_THRESHOLD_MINUTES", "30");

        if (null != jobQueueManagerAPI) {
            jobQueueManagerAPI.close();
        }
    }

    @BeforeEach
    void reset() {
        // Reset circuit breaker
        if(null != jobQueueManagerAPI) {
            jobQueueManagerAPI.getCircuitBreaker().reset();
        }

        // Reset retry attempts
        attempts = 0;
    }

    /**
     * Method to test: createJob and process execution in JobQueueManagerAPI
     * Given Scenario: A job is created and submitted to the queue
     * ExpectedResult: The job is successfully created, processed, and completed within the expected timeframe
     */
    @Order(1)
    @Test
    void test_CreateAndProcessJob() throws Exception {
        // Register a test processor
        jobQueueManagerAPI.registerProcessor("testQueue", TestJobProcessor.class);

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
            if (job.state() == JobState.SUCCESS) {
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
                    assertEquals(JobState.SUCCESS, job.state(),
                            "Job should be in SUCCESS state");
                });
    }

    /**
     * Method to test: Job retry mechanism in JobQueueManagerAPI
     * Given Scenario: A job is created that fails initially but succeeds after a certain number
     * of retries
     * ExpectedResult: The job is retried the configured number of times, eventually succeeds, and
     * is marked as COMPLETED
     * NOTE: I'm moving this test up as it is designed to pass only when a few retries are allowed
     * otherwise it will fail because of the CircuitBreaker blocking too many retries in a short time
     */
    @Test
    @Order(2)
    void test_JobRetry() throws Exception {
        final int maxRetries = RetryingJobProcessor.MAX_RETRIES;
        jobQueueManagerAPI.registerProcessor("retryQueue", RetryingJobProcessor.class);

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
        final Optional<JobProcessor> instance = jobQueueManagerAPI.getInstance(jobId);
        assertTrue(instance.isPresent(),()->"Should be able to create an instance of the job processor");
        RetryingJobProcessor processor = (RetryingJobProcessor)instance.get();

        CountDownLatch latch = new CountDownLatch(1);
        jobQueueManagerAPI.watchJob(jobId, job -> {
            if (job.state() == JobState.SUCCESS) {
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
                    assertEquals(JobState.SUCCESS, job.state(),
                            "Job should be in SUCCESS state");
                    assertEquals(maxRetries + 1, processor.getAttempts(),
                            "Job should have been attempted " + maxRetries + " times");
                });
    }

    /**
     * Method to test: Job failure handling in JobQueueManagerAPI
     * Given Scenario: A job is created that is designed to fail
     * ExpectedResult: The job fails, is marked as FAILED_PERMANENTLY, and contains the expected
     * error details
     */
    @Test
    @Order(3)
    void test_Failing_Permanently_Job() throws Exception {
        jobQueueManagerAPI.registerProcessor("failingQueue", FailingJobProcessor.class);
        RetryStrategy noRetriesStrategy = new ExponentialBackoffRetryStrategy(
                5000, 300000, 2.0, 0
        );
        jobQueueManagerAPI.setRetryStrategy("failingQueue", noRetriesStrategy);

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
                    assertEquals(JobState.FAILED_PERMANENTLY, job.state(),
                            "Job should be in FAILED_PERMANENTLY state");
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
    @Order(4)
    void test_CancelJob() throws Exception {
        jobQueueManagerAPI.registerProcessor("cancellableQueue", CancellableJobProcessor.class);

        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        Map<String, Object> parameters = new HashMap<>();
        final String jobId = jobQueueManagerAPI.createJob("cancellableQueue", parameters);

        //Get the instance of the job processor immediately after creating the job cuz once it gets cancelled, it will be removed from the map
        final Optional<JobProcessor> instance = jobQueueManagerAPI.getInstance(jobId);
        assertTrue(instance.isPresent(),()->"Should have been able to create an instance of the job processor");
        final CancellableJobProcessor processor = (CancellableJobProcessor)instance.get();

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
     * Method to test: Progress tracking functionality in JobQueueManagerAPI
     * Given Scenario: A job is created that reports progress during its execution
     * ExpectedResult: Progress updates are received, increase monotonically, and the job completes
     * with 100% progress
     */
    @Test
    @Order(5)
    void test_JobWithProgressTracker() throws Exception {
        // Register a processor that uses progress tracking
        jobQueueManagerAPI.registerProcessor("progressQueue", ProgressTrackingJobProcessor.class);

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
                    return job.state() == JobState.SUCCESS;
                });

        // Verify final job state
        Job completedJob = jobQueueManagerAPI.getJob(jobId);
        assertEquals(JobState.SUCCESS, completedJob.state(), "Job should be in SUCCESS state");
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
    @Order(6)
    void test_CombinedScenarios() throws Exception {
        // Register processors for different scenarios
        jobQueueManagerAPI.registerProcessor("successQueue", TestJobProcessor.class);
        jobQueueManagerAPI.registerProcessor("failQueue", FailingJobProcessor.class);
        jobQueueManagerAPI.registerProcessor("cancelQueue", CancellableJobProcessor.class);

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
        String cancelJob1Id = jobQueueManagerAPI.createJob("cancelQueue", new HashMap<>());
        String cancelJob2Id = jobQueueManagerAPI.createJob("cancelQueue",
                Map.of("wontCancel", true)
        );

        // Set up latches to track job completions
        CountDownLatch successLatch = new CountDownLatch(3);
        CountDownLatch failLatch = new CountDownLatch(1);
        CountDownLatch cancelLatch = new CountDownLatch(1);

        // Watch jobs
        jobQueueManagerAPI.watchJob(successJob1Id, job -> {
            if (job.state() == JobState.SUCCESS) {
                successLatch.countDown();
            }
        });
        jobQueueManagerAPI.watchJob(successJob2Id, job -> {
            if (job.state() == JobState.SUCCESS) {
                successLatch.countDown();
            }
        });
        jobQueueManagerAPI.watchJob(failJobId, job -> {
            if (job.state() == JobState.FAILED) {
                failLatch.countDown();
            }
        });
        jobQueueManagerAPI.watchJob(cancelJob1Id, job -> {
            if (job.state() == JobState.CANCELED) {
                cancelLatch.countDown();
            }
        });
        jobQueueManagerAPI.watchJob(cancelJob2Id, job -> {
            if (job.state() == JobState.SUCCESS) {
                successLatch.countDown();
            }
        });

        // Wait a bit before cancelling the job, just cancelJob1Id
        Awaitility.await().pollDelay(500, TimeUnit.MILLISECONDS).until(() -> true);
        jobQueueManagerAPI.cancelJob(cancelJob1Id);

        // Wait for all jobs to complete (or timeout after 30 seconds)
        boolean allCompleted = successLatch.await(30, TimeUnit.SECONDS)
                && failLatch.await(30, TimeUnit.SECONDS)
                && cancelLatch.await(30, TimeUnit.SECONDS);

        assertTrue(allCompleted, "All jobs should complete within the timeout period");

        // Verify final states
        assertEquals(JobState.SUCCESS, jobQueueManagerAPI.getJob(successJob1Id).state(),
                "First success job should be successful");
        assertEquals(JobState.SUCCESS, jobQueueManagerAPI.getJob(successJob2Id).state(),
                "Second success job should be successful");
        assertEquals(JobState.FAILED_PERMANENTLY, jobQueueManagerAPI.getJob(failJobId).state(),
                "Fail job should be in failed state");
        assertEquals(JobState.CANCELED, jobQueueManagerAPI.getJob(cancelJob1Id).state(),
                "Cancel job 1 should be canceled");
        assertEquals(JobState.SUCCESS, jobQueueManagerAPI.getJob(cancelJob2Id).state(),
                "Cancel job 2 should be successful");

        // Wait for job processing to complete as we have retries running
        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Job failedJob = jobQueueManagerAPI.getJob(failJobId);
                    assertEquals(2, failedJob.retryCount(),
                            "Job should have been retried " + 2 + " times");
                    assertEquals(JobState.FAILED_PERMANENTLY, failedJob.state(),
                            "Job should be in FAILED_PERMANENTLY state");
                    assertTrue(failedJob.result().isPresent(),
                            "Failed job should have a result");
                    assertTrue(failedJob.result().get().errorDetail().isPresent(),
                            "Failed job should have error details");
                });
    }

    /**
     * Tests the abandoned job detection functionality.
     * Given Scenario: A job exists in RUNNING state with an old timestamp
     * ExpectedResult: The job is detected as abandoned, marked accordingly and retried successfully
     */
    @Test
    @Order(7)
    void test_AbandonedJobDetection() throws Exception {

        final String jobId = UUID.randomUUID().toString();
        final String queueName = "abandonedQueue";
        final Map<String, Object> parameters = Collections.singletonMap("test", "value");
        final String serverId = APILocator.getServerAPI().readServerId();
        final LocalDateTime oldTimestamp = LocalDateTime.now().minusMinutes(5);

        // Create a job directly in the database in RUNNING state to simulate an abandoned job
        DotConnect dc = new DotConnect();

        // Insert into job table
        dc.setSQL("INSERT INTO job (id, queue_name, state, parameters, created_at, updated_at, started_at, execution_node) VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?)")
                .addParam(jobId)
                .addParam(queueName)
                .addParam(JobState.RUNNING.name())
                .addParam(new ObjectMapper().writeValueAsString(parameters))
                .addParam(Timestamp.valueOf(oldTimestamp))
                .addParam(Timestamp.valueOf(oldTimestamp))
                .addParam(Timestamp.valueOf(oldTimestamp))
                .addParam(serverId)
                .loadResult();

        // Insert into job_queue table
        dc.setSQL("INSERT INTO job_queue (id, queue_name, state, created_at) VALUES (?, ?, ?, ?)")
                .addParam(jobId)
                .addParam(queueName)
                .addParam(JobState.RUNNING.name())
                .addParam(Timestamp.valueOf(oldTimestamp))
                .loadResult();

        // Insert initial state into job_history
        dc.setSQL("INSERT INTO job_history (id, job_id, state, execution_node, created_at) VALUES (?, ?, ?, ?, ?)")
                .addParam(UUID.randomUUID().toString())
                .addParam(jobId)
                .addParam(JobState.RUNNING.name())
                .addParam(serverId)
                .addParam(Timestamp.valueOf(oldTimestamp))
                .loadResult();

        // Verify the job was created in RUNNING state
        Job initialJob = jobQueueManagerAPI.getJob(jobId);
        assertEquals(JobState.RUNNING, initialJob.state(),
                "Job should be in RUNNING state initially");

        // Start job queue manager if not started
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        // Register a processor for the abandoned job
        jobQueueManagerAPI.registerProcessor(queueName, AbbandonedJobProcessor.class);

        // The job should be marked as abandoned
        CountDownLatch latch = new CountDownLatch(1);
        jobQueueManagerAPI.watchJob(jobId, job -> {
            if (job.state() == JobState.ABANDONED) {
                latch.countDown();
            }
        });

        boolean abandoned = latch.await(3, TimeUnit.MINUTES);
        assertTrue(abandoned, "Job should be marked as abandoned within timeout period");

        // Verify the abandoned job state and error details
        Job abandonedJob = jobQueueManagerAPI.getJob(jobId);
        assertEquals(JobState.ABANDONED, abandonedJob.state(),
                "Job should be in ABANDONED state");
        assertTrue(abandonedJob.result().isPresent(),
                "Abandoned job should have a result");
        assertTrue(abandonedJob.result().get().errorDetail().isPresent(),
                "Abandoned job should have error details");
        assertTrue(abandonedJob.result().get().errorDetail().get().message()
                        .contains("abandoned due to no updates"),
                "Error message should indicate abandonment");

        // Verify the job was put back in queue for retry and completed successfully
        Awaitility.await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Job job = jobQueueManagerAPI.getJob(jobId);
                    assertEquals(JobState.SUCCESS, job.state(),
                            "Job should be in SUCCESS state");
                });

        // Verify job history contains the state transitions
        dc.setSQL("SELECT state FROM job_history WHERE job_id = ? ORDER BY created_at")
                .addParam(jobId);
        List<Map<String, Object>> history = dc.loadObjectResults();

        assertFalse(history.isEmpty(), "Job should have history records");
        assertEquals(JobState.RUNNING.name(), history.get(0).get("state"),
                "First state should be RUNNING");
        assertEquals(JobState.ABANDONED.name(), history.get(1).get("state"),
                "Second state should be ABANDONED");
        assertEquals(JobState.RUNNING.name(), history.get(2).get("state"),
                "Third state should be RUNNING");
        assertEquals(JobState.SUCCESS.name(), history.get(3).get("state"),
                "Latest state should be SUCCESS");
    }

    /**
     * Tests the abandoned job detection functionality.
     * Given Scenario: A job exists in RUNNING state with an old timestamp
     * ExpectedResult: The job is detected as abandoned, eventually marked as ABANDONED_PERMANENTLY
     */
    @Test
    @Order(8)
    void test_Abandoned_Permanetly_Job() throws Exception {

        final String jobId = UUID.randomUUID().toString();
        final String queueName = "abandonedQueue";
        final Map<String, Object> parameters = Collections.singletonMap("test", "value");
        final String serverId = APILocator.getServerAPI().readServerId();
        final LocalDateTime oldTimestamp = LocalDateTime.now().minusMinutes(5);

        // Create a job directly in the database in RUNNING state to simulate an abandoned job
        DotConnect dc = new DotConnect();

        // Insert into job table
        dc.setSQL("INSERT INTO job (id, queue_name, state, parameters, created_at, updated_at, started_at, execution_node) VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?)")
                .addParam(jobId)
                .addParam(queueName)
                .addParam(JobState.RUNNING.name())
                .addParam(new ObjectMapper().writeValueAsString(parameters))
                .addParam(Timestamp.valueOf(oldTimestamp))
                .addParam(Timestamp.valueOf(oldTimestamp))
                .addParam(Timestamp.valueOf(oldTimestamp))
                .addParam(serverId)
                .loadResult();

        // Insert into job_queue table
        dc.setSQL("INSERT INTO job_queue (id, queue_name, state, created_at) VALUES (?, ?, ?, ?)")
                .addParam(jobId)
                .addParam(queueName)
                .addParam(JobState.RUNNING.name())
                .addParam(Timestamp.valueOf(oldTimestamp))
                .loadResult();

        // Insert initial state into job_history
        dc.setSQL("INSERT INTO job_history (id, job_id, state, execution_node, created_at) VALUES (?, ?, ?, ?, ?)")
                .addParam(UUID.randomUUID().toString())
                .addParam(jobId)
                .addParam(JobState.RUNNING.name())
                .addParam(serverId)
                .addParam(Timestamp.valueOf(oldTimestamp))
                .loadResult();

        // Verify the job was created in RUNNING state
        Job initialJob = jobQueueManagerAPI.getJob(jobId);
        assertEquals(JobState.RUNNING, initialJob.state(),
                "Job should be in RUNNING state initially");

        // Start job queue manager if not started
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        // Register a processor for the abandoned job
        jobQueueManagerAPI.registerProcessor(queueName, AbbandonedJobProcessor.class);
        RetryStrategy noRetriesStrategy = new ExponentialBackoffRetryStrategy(
                5000, 300000, 2.0, 0
        );
        jobQueueManagerAPI.setRetryStrategy(queueName, noRetriesStrategy);

        // The job should be marked as abandoned
        CountDownLatch latch = new CountDownLatch(1);
        jobQueueManagerAPI.watchJob(jobId, job -> {
            if (job.state() == JobState.ABANDONED) {
                latch.countDown();
            }
        });

        boolean abandoned = latch.await(3, TimeUnit.MINUTES);
        assertTrue(abandoned, "Job should be marked as abandoned within timeout period");

        // Verify the abandoned job state and error details
        Job abandonedJob = jobQueueManagerAPI.getJob(jobId);
        assertEquals(JobState.ABANDONED, abandonedJob.state(),
                "Job should be in ABANDONED state");
        assertTrue(abandonedJob.result().isPresent(),
                "Abandoned job should have a result");
        assertTrue(abandonedJob.result().get().errorDetail().isPresent(),
                "Abandoned job should have error details");
        assertTrue(abandonedJob.result().get().errorDetail().get().message()
                        .contains("abandoned due to no updates"),
                "Error message should indicate abandonment");

        // Verify the job was put back in queue for retry and completed successfully
        Awaitility.await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Job job = jobQueueManagerAPI.getJob(jobId);
                    assertEquals(JobState.ABANDONED_PERMANENTLY, job.state(),
                            "Job should be in ABANDONED_PERMANENTLY state");
                });

        // Verify job history contains the state transitions
        dc.setSQL("SELECT state FROM job_history WHERE job_id = ? ORDER BY created_at")
                .addParam(jobId);
        List<Map<String, Object>> history = dc.loadObjectResults();

        assertFalse(history.isEmpty(), "Job should have history records");
        assertEquals(JobState.RUNNING.name(), history.get(0).get("state"),
                "First state should be RUNNING");
        assertEquals(JobState.ABANDONED.name(), history.get(1).get("state"),
                "Second state should be ABANDONED");
        assertEquals(JobState.ABANDONED_PERMANENTLY.name(), history.get(2).get("state"),
                "Latest state should be ABANDONED_PERMANENTLY");
    }

    static class AbbandonedJobProcessor implements JobProcessor {

        @Override
        public void process(Job job) {
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return Collections.emptyMap();
        }
    }

    static class ProgressTrackingJobProcessor implements JobProcessor {
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

    static class RetryingJobProcessor implements JobProcessor {

        public static final int MAX_RETRIES = 3;

        public RetryingJobProcessor() {
             // needed for instantiation purposes
        }

        @Override
        public void process(Job job) {
            attempts++;
            if (attempts <= MAX_RETRIES) {
                throw new RuntimeException("Simulated failure, attempt " + attempts);
            }
            // If we've reached here, we've exceeded maxRetries and the job should succeed
            System.out.println("Job succeeded after " + attempts + " attempts");
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

    static class FailingJobProcessor implements JobProcessor {

        @Override
        public void process(Job job) {
            throw new RuntimeException("Simulated failure");
        }

        @Override
        public Map<String, Object> getResultMetadata(Job job) {
            return new HashMap<>();
        }
    }

    static class CancellableJobProcessor implements JobProcessor, Cancellable {

        private final AtomicBoolean canceled = new AtomicBoolean(false);
        private final AtomicBoolean wasCanceled = new AtomicBoolean(false);

        @Override
        public void process(Job job) {

            if (job.parameters().containsKey("wontCancel")) {
                // Simulate some work
                Awaitility.await().pollDelay(5, TimeUnit.SECONDS).until(() -> true);
            } else {
                Awaitility.await().atMost(10, TimeUnit.SECONDS)
                        .until(canceled::get);

                // Simulate some additional work after cancellation
                Awaitility.await().pollDelay(1, TimeUnit.SECONDS).until(() -> true);
            }
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

    static class TestJobProcessor implements JobProcessor {

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
