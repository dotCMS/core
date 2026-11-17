package com.dotcms.jobs.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.JobProcessor;
import com.dotcms.jobs.business.processor.ProgressTracker;
import com.dotcms.util.IntegrationTestInitService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the job-queue framework's liveness heartbeat — #37063 (Content Drive bulk
 * folder delete backend), spec FR-024a.
 * <p>
 * The framework's existing progress poller ({@code JobQueueManagerAPIImpl.updateJobProgress})
 * only persists {@code job.updated_at} when the reported progress value strictly increases
 * ({@code progress > previousProgress}). A processor whose progress can only be reported in coarse
 * units (here: completed top-level folders) needs a way to tell the framework "I am still working"
 * during a single long-running unit of work, without inventing a fake progress value the client
 * would see as real precision. This test proves the new capability exists and does exactly that:
 * advances {@code job.updated_at} independent of the reported progress value, and without firing a
 * client-visible {@link com.dotcms.jobs.business.api.events.JobProgressUpdatedEvent}.
 * <p>
 * <b>Started as T005's Foundational-phase Red evidence</b> — written before {@code ProgressTracker}
 * had a {@code heartbeat()} method on {@code main}; T010 added it, and this test now exercises the
 * real implementation rather than proving its absence.
 */
@EnableWeld
public class HeartbeatIT extends com.dotcms.Junit5WeldBaseTest {

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    @BeforeAll
    static void setUp() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @AfterAll
    static void cleanUp() {
        clearJobs();
    }

    /**
     * Method to test: a processor calling {@code ProgressTracker.heartbeat()} while never calling
     * {@code updateProgress}
     * <p>
     * Given Scenario: A job whose processor does real, slow work and periodically signals liveness
     * via heartbeat only — the shape this feature's per-folder delete loop will actually use,
     * since nothing inside one folder's delete is observable (FR-025)
     * <p>
     * Expected Result: {@code job.updated_at} (read back through {@code JobQueueManagerAPI.getJob},
     * which is cache-safe the same way {@code updateJobProgress} already is) moves past its value
     * at the first heartbeat while the job is still running, so the advance can only have come from
     * a heartbeat and not from the job completing; {@code job.progress()} never leaves {@code 0.0};
     * and no progress-driven job update reaches a watcher, proving no
     * {@code JobProgressUpdatedEvent} fired for any heartbeat call
     */
    @Test
    void test_heartbeat_advancesUpdatedAt_withoutChangingProgressOrFiringProgressEvent()
            throws Exception {

        HeartbeatOnlyJobProcessor.reset();
        jobQueueManagerAPI.registerProcessor("heartbeatQueue", HeartbeatOnlyJobProcessor.class);

        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        final String jobId = jobQueueManagerAPI.createJob("heartbeatQueue", new HashMap<>());

        // Progress values observed via the same watch mechanism test_JobWithProgressTracker uses -
        // if heartbeat() ever fired a JobProgressUpdatedEvent through the progress-changed path,
        // a non-zero value would show up here
        final List<Float> observedProgress = new CopyOnWriteArrayList<>();
        jobQueueManagerAPI.watchJob(jobId, job -> observedProgress.add(job.progress()));

        try {
            // Let the first heartbeat land, then capture updated_at as the "before" baseline
            Awaitility.await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .until(() -> HeartbeatOnlyJobProcessor.heartbeatsSent.get() >= 1);

            final LocalDateTime updatedAtAfterFirstHeartbeat =
                    jobQueueManagerAPI.getJob(jobId).updatedAt().orElseThrow(
                            () -> new IllegalStateException("expected updated_at to be set"));

            // heartbeat() only raises a flag: the framework's progress poller turns it into a write
            // of updated_at on its next tick, which runs every few seconds. The processor keeps
            // heartbeating and cannot finish until released below, so wait for that write to show
            // up. The processor is still running when it does, which rules out the job completing
            // as the source of the new value, and the two values are a poller tick apart, so they
            // cannot come out equal.
            Awaitility.await().atMost(10, TimeUnit.SECONDS)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .until(() -> jobQueueManagerAPI.getJob(jobId).updatedAt()
                            .map(updatedAt -> updatedAt.isAfter(updatedAtAfterFirstHeartbeat))
                            .orElse(false));

            assertEquals(JobState.RUNNING, jobQueueManagerAPI.getJob(jobId).state(),
                    "updated_at must advance on a heartbeat while the job is still running, "
                            + "not because the job finished");
        } finally {
            HeartbeatOnlyJobProcessor.release.countDown();
        }

        Awaitility.await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId).state() == JobState.SUCCESS);

        final Job finished = jobQueueManagerAPI.getJob(jobId);
        assertEquals(0.0f, finished.progress(), 0.001f,
                "a processor that only calls heartbeat() must never be credited with progress");

        for (final Float observed : observedProgress) {
            assertEquals(0.0f, observed, 0.001f,
                    "heartbeat() must never drive a progress-changed notification to watchers");
        }
        assertFalse(HeartbeatOnlyJobProcessor.heartbeatsSent.get() < 2,
                "test setup error: fewer than 2 heartbeats were actually sent");
    }

    public static class HeartbeatOnlyJobProcessor implements JobProcessor {

        static final java.util.concurrent.atomic.AtomicInteger heartbeatsSent =
                new java.util.concurrent.atomic.AtomicInteger(0);

        /** Held by the processor until the test has seen what it needs, then released. */
        static volatile CountDownLatch release = new CountDownLatch(1);

        /** Bounds the loop so a test that fails before releasing it cannot leave the job running. */
        private static final int MAX_HEARTBEATS = 30;

        /** Clears state shared through statics, so a rerun of the test starts from zero. */
        static void reset() {
            heartbeatsSent.set(0);
            release = new CountDownLatch(1);
        }

        public HeartbeatOnlyJobProcessor() {
            // Do nothing - JobProcessorFactory instantiates this reflectively, from a different
            // package (com.dotcms.jobs.business.api), so both the class and this constructor must
            // be public: JobProcessorFactory#createInstance calls getDeclaredConstructor().newInstance()
            // with no setAccessible(true), which throws IllegalAccessException across packages
            // otherwise (see JobQueueHelperIntegrationTest.DemoJobProcessor for the same pattern).
        }

        @Override
        public void process(final Job job) {
            final ProgressTracker tracker = job.progressTracker().orElseThrow(
                    () -> new IllegalStateException("Progress tracker not set"));

            // Simulates one long-running unit of work (one top-level folder's blocking delete)
            // that can only signal liveness, never a finer-grained percentage. It keeps working,
            // and heartbeating, until the test releases it
            final CountDownLatch released = release;
            try {
                for (int i = 0; i < MAX_HEARTBEATS && !released.await(600, TimeUnit.MILLISECONDS); i++) {
                    tracker.heartbeat();
                    heartbeatsSent.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public Map<String, Object> getResultMetadata(final Job job) {
            return new HashMap<>();
        }
    }

    private static void clearJobs() {
        try {
            new com.dotmarketing.common.db.DotConnect().setSQL("delete from job_history")
                    .loadResult();
            new com.dotmarketing.common.db.DotConnect().setSQL("delete from job_queue")
                    .loadResult();
            new com.dotmarketing.common.db.DotConnect().setSQL("delete from job").loadResult();
        } catch (com.dotmarketing.exception.DotDataException e) {
            com.dotmarketing.util.Logger.warn(HeartbeatIT.class, "Error cleaning up jobs", e);
        }
    }
}
