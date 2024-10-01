package com.dotcms.jobs.business.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.dotcms.jobs.business.error.ErrorDetail;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobPaginatedResult;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.queue.error.JobNotFoundException;
import com.dotcms.jobs.business.queue.error.JobQueueException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for PostgresJobQueue implementation
 */
public class PostgresJobQueueIntegrationTest {

    private static JobQueue jobQueue;

    @BeforeAll
    static void setUp() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        jobQueue = new PostgresJobQueue();
    }

    @AfterEach
    void cleanUpEach() {
        clearJobs();
    }

    /**
     * Method to test: createJob and getJob in PostgresJobQueue
     * Given Scenario: A job is created with specific parameters
     * ExpectedResult: The job can be retrieved and its properties match the input
     */
    @Test
    void test_createJob_and_getJob() throws JobQueueException {

        String queueName = "testQueue";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("key", "value");

        String jobId = jobQueue.createJob(queueName, parameters);
        assertNotNull(jobId);

        Job job = jobQueue.getJob(jobId);
        assertNotNull(job);
        assertEquals(queueName, job.queueName());
        assertEquals(JobState.PENDING, job.state());
        assertEquals(parameters, job.parameters());
    }

    /**
     * Method to test: getActiveJobs in PostgresJobQueue
     * Given Scenario: Multiple active jobs are created
     * ExpectedResult: All active jobs are retrieved correctly
     */
    @Test
    void test_getActiveJobs() throws JobQueueException {

        String queueName = "testQueue";
        for (int i = 0; i < 5; i++) {
            jobQueue.createJob(queueName, new HashMap<>());
        }

        JobPaginatedResult result = jobQueue.getActiveJobs(queueName, 1, 10);
        assertEquals(5, result.jobs().size());
        assertEquals(5, result.total());
    }

    /**
     * Method to test: getCompletedJobs in PostgresJobQueue
     * Given Scenario: Multiple jobs are created and completed
     * ExpectedResult: All completed jobs within the given time range are retrieved
     */
    @Test
    void testGetCompletedJobs() throws JobQueueException {

        String queueName = "testQueue";
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // Create and complete some jobs
        for (int i = 0; i < 3; i++) {
            String jobId = jobQueue.createJob(queueName, new HashMap<>());
            Job job = jobQueue.getJob(jobId);
            Job completedJob = job.markAsCompleted(null);
            jobQueue.updateJobStatus(completedJob);
        }

        JobPaginatedResult result = jobQueue.getCompletedJobs(queueName, startDate, endDate, 1, 10);
        assertEquals(3, result.jobs().size());
        assertEquals(3, result.total());
        result.jobs().forEach(job -> assertEquals(JobState.COMPLETED, job.state()));
    }

    /**
     * Method to test: getFailedJobs in PostgresJobQueue
     * Given Scenario: Multiple jobs are created and set to failed state
     * ExpectedResult: All failed jobs are retrieved correctly
     */
    @Test
    void test_getFailedJobs() throws JobQueueException {

        // Create and fail some jobs
        for (int i = 0; i < 2; i++) {
            String jobId = jobQueue.createJob("testQueue", new HashMap<>());
            Job job = jobQueue.getJob(jobId);
            Job failedJob = Job.builder().from(job)
                    .state(JobState.FAILED)
                    .build();
            jobQueue.updateJobStatus(failedJob);
        }

        JobPaginatedResult result = jobQueue.getFailedJobs(1, 10);
        assertEquals(2, result.jobs().size());
        assertEquals(2, result.total());
        result.jobs().forEach(job -> assertEquals(JobState.FAILED, job.state()));
    }

    /**
     * Method to test: updateJobStatus in PostgresJobQueue
     * Given Scenario: A job's status is updated
     * ExpectedResult: The job's status is correctly reflected in the database
     */
    @Test
    void test_updateJobStatus() throws JobQueueException {

        String jobId = jobQueue.createJob("testQueue", new HashMap<>());
        Job job = jobQueue.getJob(jobId);

        Job updatedJob = Job.builder().from(job)
                .state(JobState.RUNNING)
                .progress(0.5f)
                .build();

        jobQueue.updateJobStatus(updatedJob);

        Job fetchedJob = jobQueue.getJob(jobId);
        assertEquals(JobState.RUNNING, fetchedJob.state());
        assertEquals(0.5f, fetchedJob.progress(), 0.001);
    }

    /**
     * Method to test: nextJob in PostgresJobQueue
     * Given Scenario: Multiple threads attempt to get the next job concurrently
     * ExpectedResult: Each job is processed exactly once and all jobs are eventually completed
     */
    @Test
    void test_nextJob() throws Exception {

        final int NUM_JOBS = 10;
        final int NUM_THREADS = 5;
        String queueName = "testQueue";

        // Create jobs
        Set<String> createdJobIds = new HashSet<>();
        for (int i = 0; i < NUM_JOBS; i++) {
            String jobId = jobQueue.createJob(queueName, new HashMap<>());
            createdJobIds.add(jobId);
        }

        // Set to keep track of processed job IDs
        Set<String> processedJobIds = Collections.synchronizedSet(new HashSet<>());

        // Create and start threads
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            Thread thread = new Thread(() -> {
                try {
                    while (true) {
                        Job nextJob = jobQueue.nextJob();
                        if (nextJob == null) {
                            break;  // No more jobs to process
                        }
                        // Ensure this job hasn't been processed before
                        assertTrue(processedJobIds.add(nextJob.id()),
                                "Job " + nextJob.id() + " was processed more than once");
                        assertEquals(JobState.PENDING, nextJob.state());

                        // Simulate some processing time
                        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                                .pollInterval(100, TimeUnit.MILLISECONDS).until(() -> {
                                    return true;
                                });

                        // Mark job as completed
                        Job completedJob = nextJob.markAsCompleted(null);
                        jobQueue.updateJobStatus(completedJob);
                    }
                } catch (Exception e) {
                    fail("Exception in thread: " + e.getMessage());
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all jobs were processed
        assertEquals(NUM_JOBS, processedJobIds.size(), "Not all jobs were processed");
        assertEquals(createdJobIds, processedJobIds, "Processed jobs don't match created jobs");

        // Verify no more jobs are available
        assertNull(jobQueue.nextJob(), "There should be no more jobs available");

        // Verify all jobs are in COMPLETED state
        for (String jobId : createdJobIds) {
            Job job = jobQueue.getJob(jobId);
            assertEquals(JobState.COMPLETED, job.state(),
                    "Job " + jobId + " is not in COMPLETED state");
        }
    }

    /**
     * Method to test: getJob in PostgresJobQueue with non-existent ID
     * Given Scenario: Attempt to retrieve a job with a non-existent ID
     * ExpectedResult: JobNotFoundException is thrown
     */
    @Test
    void testJobNotFound() {
        assertThrows(JobNotFoundException.class, () -> jobQueue.getJob("non-existent-id"));
    }

    /**
     * Method to test: updateJobProgress in PostgresJobQueue
     * Given Scenario: A job's progress is updated multiple times
     * ExpectedResult: The job's progress is correctly updated in the database
     */
    @Test
    void test_updateJobProgress() throws JobQueueException {

        String jobId = jobQueue.createJob("testQueue", new HashMap<>());

        jobQueue.updateJobProgress(jobId, 0.75f);

        Job updatedJob = jobQueue.getJob(jobId);
        assertEquals(0.75f, updatedJob.progress(), 0.001);

        jobQueue.updateJobProgress(jobId, 0.85f);

        updatedJob = jobQueue.getJob(jobId);
        assertEquals(0.85f, updatedJob.progress(), 0.001);
    }

    /**
     * Method to test: getJobs in PostgresJobQueue
     * Given Scenario: Jobs with various states are created
     * ExpectedResult: All jobs are retrieved correctly with proper pagination
     */
    @Test
    void test_getJobs() throws JobQueueException {

        // Create a mix of jobs with different states
        String queueName = "testQueue";
        for (int i = 0; i < 3; i++) {
            jobQueue.createJob(queueName, new HashMap<>());
        }
        String runningJobId = jobQueue.createJob(queueName, new HashMap<>());
        Job runningJob = jobQueue.getJob(runningJobId);
        jobQueue.updateJobStatus(runningJob.withState(JobState.RUNNING));

        String completedJobId = jobQueue.createJob(queueName, new HashMap<>());
        Job completedJob = jobQueue.getJob(completedJobId);
        jobQueue.updateJobStatus(completedJob.markAsCompleted(null));

        // Get all jobs
        JobPaginatedResult result = jobQueue.getJobs(1, 10);

        assertEquals(5, result.jobs().size());
        assertEquals(5, result.total());
        assertEquals(1, result.page());
        assertEquals(10, result.pageSize());

        // Verify job states
        Map<JobState, Integer> stateCounts = new HashMap<>();
        for (Job job : result.jobs()) {
            stateCounts.put(job.state(), stateCounts.getOrDefault(job.state(), 0) + 1);
        }
        assertEquals(3, stateCounts.getOrDefault(JobState.PENDING, 0));
        assertEquals(1, stateCounts.getOrDefault(JobState.RUNNING, 0));
        assertEquals(1, stateCounts.getOrDefault(JobState.COMPLETED, 0));
    }

    /**
     * Method to test: getUpdatedJobsSince in PostgresJobQueue
     * Given Scenario: Jobs are created and updated at different times
     * ExpectedResult: Only jobs updated after the specified time are retrieved
     */
    @Test
    void test_getUpdatedJobsSince() throws JobQueueException, InterruptedException {

        String queueName = "testQueue";

        // Create initial jobs
        String job1Id = jobQueue.createJob(queueName, new HashMap<>());
        String job2Id = jobQueue.createJob(queueName, new HashMap<>());

        Awaitility.await().atMost(1, TimeUnit.SECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .until(() -> true);// Ensure some time passes
        LocalDateTime checkpointTime = LocalDateTime.now();
        Awaitility.await().atMost(1, TimeUnit.SECONDS)
                .pollInterval(50, TimeUnit.MILLISECONDS)
                .until(() -> true);// Ensure some more time passes

        // Update job1 and create a new job after the checkpoint
        Job job1 = jobQueue.getJob(job1Id);
        jobQueue.updateJobStatus(job1.withState(JobState.RUNNING));
        String job3Id = jobQueue.createJob(queueName, new HashMap<>());

        Set<String> jobIdsToCheck = new HashSet<>(Arrays.asList(job1Id, job2Id, job3Id));
        List<Job> updatedJobs = jobQueue.getUpdatedJobsSince(jobIdsToCheck, checkpointTime);

        assertEquals(2, updatedJobs.size());
        Set<String> updatedJobIds = updatedJobs.stream().map(Job::id).collect(Collectors.toSet());
        assertTrue(updatedJobIds.contains(job1Id));
        assertTrue(updatedJobIds.contains(job3Id));
        assertFalse(updatedJobIds.contains(job2Id));
    }

    /**
     * Method to test: putJobBackInQueue in PostgresJobQueue
     * Given Scenario: A failed job is put back into the queue
     * ExpectedResult: The job is reset to PENDING state and can be retrieved by nextJob
     */
    @Test
    void test_putJobBackInQueue() throws JobQueueException {

        String queueName = "testQueue";
        String jobId = jobQueue.createJob(queueName, new HashMap<>());

        // Simulate job processing
        Job job = jobQueue.getJob(jobId);
        jobQueue.updateJobStatus(job.withState(JobState.RUNNING));

        // Simulate job failure
        final var jobResult = JobResult.builder()
                .errorDetail(ErrorDetail.builder()
                        .message("Simulated error")
                        .stackTrace(stackTrace(new RuntimeException("Simulated error")))
                        .exceptionClass("java.lang.RuntimeException")
                        .timestamp(LocalDateTime.now())
                        .processingStage("Simulated stage")
                        .build())
                .build();
        jobQueue.updateJobStatus(job.markAsFailed(jobResult));

        // Put the job back in the queue
        jobQueue.putJobBackInQueue(job);

        // Verify the job maintains the FAILED state, needed for the retry mechanism to work
        Job retrievedJob = jobQueue.getJob(jobId);
        assertEquals(JobState.FAILED, retrievedJob.state());

        // Verify the job can be retrieved by nextJob
        Job nextJob = jobQueue.nextJob();
        assertNotNull(nextJob);
        assertEquals(jobId, nextJob.id());
        assertEquals(JobState.FAILED, nextJob.state());
    }

    /**
     * Method to test: removeJobFromQueue in PostgresJobQueue
     * Given Scenario: A job is removed from the queue
     * ExpectedResult: The job cannot be retrieved by nextJob after removal
     */
    @Test
    void test_removeJobFromQueue() throws JobQueueException {

        String queueName = "testQueue";
        String jobId = jobQueue.createJob(queueName, new HashMap<>());

        // Verify job exists
        Job job = jobQueue.getJob(jobId);
        assertNotNull(job);

        // Remove the job
        jobQueue.removeJobFromQueue(jobId);

        // Verify job is not returned by nextJob
        assertNull(jobQueue.nextJob());
    }

    /**
     * Method to test: createJob, updateJobStatus, and getJob in PostgresJobQueue
     * Given Scenario: A job is created, all its fields are modified, the job is updated,
     *                 and then retrieved again
     * ExpectedResult: All job fields are correctly updated and retrieved, demonstrating
     *                 proper persistence and retrieval of all job attributes
     */
    @Test
    void test_createUpdateAndRetrieveJob() throws JobQueueException {

        String queueName = "testQueue";
        Map<String, Object> initialParameters = new HashMap<>();
        initialParameters.put("initialKey", "initialValue");

        // Create initial job
        String jobId = jobQueue.createJob(queueName, initialParameters);
        Job initialJob = jobQueue.getJob(jobId);
        assertNotNull(initialJob);

        // Modify all fields
        JobResult jobResult = JobResult.builder()
                .errorDetail(ErrorDetail.builder()
                        .message("Test error")
                        .exceptionClass("TestException")
                        .timestamp(LocalDateTime.now())
                        .stackTrace("Test stack trace")
                        .processingStage("Test stage")
                        .build())
                .metadata(Collections.singletonMap("metaKey", "metaValue"))
                .build();

        Job updatedJob = Job.builder()
                .from(initialJob)
                .state(JobState.COMPLETED)
                .progress(0.75f)
                .startedAt(Optional.of(LocalDateTime.now().minusHours(1)))
                .completedAt(Optional.of(LocalDateTime.now()))
                .retryCount(2)
                .result(Optional.of(jobResult))
                .build();

        // Update the job
        jobQueue.updateJobStatus(updatedJob);

        // Retrieve the updated job
        Job retrievedJob = jobQueue.getJob(jobId);

        // Verify all fields
        assertEquals(jobId, retrievedJob.id());
        assertEquals(queueName, retrievedJob.queueName());
        assertEquals(JobState.COMPLETED, retrievedJob.state());
        assertEquals(initialParameters, retrievedJob.parameters());
        assertEquals(0.75f, retrievedJob.progress(), 0.001);
        assertTrue(retrievedJob.startedAt().isPresent());
        assertTrue(retrievedJob.completedAt().isPresent());
        assertNotNull(retrievedJob.executionNode());
        assertEquals(2, retrievedJob.retryCount());
        assertTrue(retrievedJob.result().isPresent());
        assertEquals("Test error",
                retrievedJob.result().get().errorDetail().get().message());
        assertEquals("Test stack trace",
                retrievedJob.result().get().errorDetail().get().stackTrace());
        assertEquals("TestException",
                retrievedJob.result().get().errorDetail().get().exceptionClass());
        assertEquals("Test stage",
                retrievedJob.result().get().errorDetail().get().processingStage());
        assertEquals(Collections.singletonMap("metaKey", "metaValue"),
                retrievedJob.result().get().metadata().get());
    }

    /**
     * Method to test: getJobs in PostgresJobQueue with pagination
     * Given Scenario: Multiple jobs are created and retrieved using pagination
     * ExpectedResult: Jobs are correctly paginated and retrieved in the expected order
     */
    @Test
    void test_getJobsPagination() throws JobQueueException {

        String queueName = "paginationTestQueue";
        int totalJobs = 25;
        int pageSize = 10;

        // Create jobs
        List<String> createdJobIds = new ArrayList<>();
        for (int i = 0; i < totalJobs; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("index", i);
            String jobId = jobQueue.createJob(queueName, params);
            createdJobIds.add(jobId);
        }

        // Test first page
        JobPaginatedResult page1 = jobQueue.getJobs(1, pageSize);
        assertEquals(pageSize, page1.jobs().size());
        assertEquals(totalJobs, page1.total());
        assertEquals(1, page1.page());
        assertEquals(pageSize, page1.pageSize());

        // Test second page
        JobPaginatedResult page2 = jobQueue.getJobs(2, pageSize);
        assertEquals(pageSize, page2.jobs().size());
        assertEquals(totalJobs, page2.total());
        assertEquals(2, page2.page());
        assertEquals(pageSize, page2.pageSize());

        // Test last page
        JobPaginatedResult page3 = jobQueue.getJobs(3, pageSize);
        assertEquals(5, page3.jobs().size());  // 25 total, 20 in first two pages, 5 in last
        assertEquals(totalJobs, page3.total());
        assertEquals(3, page3.page());
        assertEquals(pageSize, page3.pageSize());

        // Verify no overlap between pages
        Set<String> jobIdsPage1 = page1.jobs().stream().map(Job::id).collect(Collectors.toSet());
        Set<String> jobIdsPage2 = page2.jobs().stream().map(Job::id).collect(Collectors.toSet());
        Set<String> jobIdsPage3 = page3.jobs().stream().map(Job::id).collect(Collectors.toSet());

        assertEquals(pageSize, jobIdsPage1.size());
        assertEquals(pageSize, jobIdsPage2.size());
        assertEquals(5, jobIdsPage3.size());

        assertTrue(Collections.disjoint(jobIdsPage1, jobIdsPage2));
        assertTrue(Collections.disjoint(jobIdsPage1, jobIdsPage3));
        assertTrue(Collections.disjoint(jobIdsPage2, jobIdsPage3));

        // Verify all jobs are retrieved
        Set<String> allRetrievedJobIds = new HashSet<>();
        allRetrievedJobIds.addAll(jobIdsPage1);
        allRetrievedJobIds.addAll(jobIdsPage2);
        allRetrievedJobIds.addAll(jobIdsPage3);
        assertEquals(new HashSet<>(createdJobIds), allRetrievedJobIds);

        // Test invalid page
        JobPaginatedResult invalidPage = jobQueue.getJobs(10, pageSize);
        assertTrue(invalidPage.jobs().isEmpty());
        assertEquals(totalJobs, invalidPage.total());
        assertEquals(10, invalidPage.page());
        assertEquals(pageSize, invalidPage.pageSize());
    }

    /**
     * Method to test: hasJobBeenInState in PostgresJobQueue
     * Given Scenario: A job is created and its state is updated
     * ExpectedResult: The job's state history is correctly validated
     */
    @Test
    void test_hasJobBeenInState() throws JobQueueException {

        String queueName = "testQueue";
        String jobId = jobQueue.createJob(queueName, new HashMap<>());

        Job job = jobQueue.getJob(jobId);

        // Make sure it is validating properly the given states

        jobQueue.updateJobStatus(job.withState(JobState.RUNNING));
        assertTrue(jobQueue.hasJobBeenInState(jobId, JobState.RUNNING));

        assertFalse(jobQueue.hasJobBeenInState(jobId, JobState.CANCELED));

        jobQueue.updateJobStatus(job.withState(JobState.COMPLETED));
        assertTrue(jobQueue.hasJobBeenInState(jobId, JobState.COMPLETED));

        assertFalse(jobQueue.hasJobBeenInState(jobId, JobState.CANCELLING));

        jobQueue.updateJobStatus(job.withState(JobState.CANCELLING));
        assertTrue(jobQueue.hasJobBeenInState(jobId, JobState.CANCELLING));
    }

    /**
     * Helper method to clear all jobs from the database
     */
    private void clearJobs() {
        try {
            new DotConnect().setSQL("delete from job_history").loadResult();
            new DotConnect().setSQL("delete from job_queue").loadResult();
            new DotConnect().setSQL("delete from job").loadResult();
        } catch (DotDataException e) {
            Logger.error(this, "Error clearing jobs", e);
        }
    }

    /**
     * Generates and returns the stack trace of the exception as a string. This is a derived value
     * and will be computed only when accessed.
     *
     * @param exception The exception for which to generate the stack trace.
     * @return A string representation of the exception's stack trace, or null if no exception is
     * present.
     */
    private String stackTrace(final Throwable exception) {
        if (exception != null) {
            return Arrays.stream(exception.getStackTrace())
                    .map(StackTraceElement::toString)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        }
        return null;
    }

}