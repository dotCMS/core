package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.detector.AbandonedJobDetector;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.liferay.portal.model.User;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration test for a re-queued (abandoned-then-retried) run not mistaking an
 * already-deleted folder for a new failure (#37063, FR-030, plan.md PO-7).
 * <p>
 * <b>No framework change needed to read the signal</b> — see plan.md PO-7's own history: this
 * plan proposed a new passthrough method on {@code JobQueueManagerAPI} not once but twice, and
 * both times the real answer turned out to already exist:
 * {@code APILocator.getJobQueueManagerAPI().getJobQueue().hasJobBeenInState(jobId,
 * JobState.ABANDONED)} was already public. T067 wires that read into
 * {@link FolderBulkDeleteProcessor}; this test proves the observable behavior it exists for.
 * <p>
 * Simulates the abandoned run the same way the framework's own precedent test does
 * ({@code JobQueueManagerAPIIntegrationTest#test_AbandonedJobDetection}): a job row inserted
 * directly into {@code job}/{@code job_queue}/{@code job_history} in {@code RUNNING} state with a
 * stale {@code updated_at}, then {@link AbandonedJobDetector#detectAbandonedJobs()} — public
 * specifically so a test can trigger it immediately rather than waiting for the scheduled tick —
 * called directly. Backdated by 60 minutes rather than lowering
 * {@code JOB_ABANDONMENT_THRESHOLD_MINUTES} (default 30): that config is read once into a
 * {@code static final} field at class-load time
 * ({@code AbandonedJobDetectorConfigProducer}), so a runtime {@code Config.setProperty} only
 * works if this happens to be the first test in the whole suite run to touch that class — not
 * something worth depending on when simply backdating further comfortably clears any threshold.
 */
@EnableWeld
public class FolderBulkDeleteResumeIT extends Junit5WeldBaseTest {

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    @Inject
    AbandonedJobDetector abandonedJobDetector;

    private static User admin;
    private static Host site;
    private static FolderAPI folderAPI;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        folderAPI = APILocator.getFolderAPI();
    }

    private Folder folder() {
        return new FolderDataGen().site(site).nextPersisted();
    }

    private String pathOf(final Folder folder) {
        return String.format("//%s/%s/", site.getHostname(), folder.getName());
    }

    private void ensureQueueStarted() throws Exception {
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Inserts a job directly into the queue's own tables, already {@code RUNNING} and already
     * stale — simulating a run whose worker died mid-flight, exactly the shape
     * {@code JobQueueManagerAPIIntegrationTest#test_AbandonedJobDetection} builds for the same
     * reason: there is no supported way to make a *real* submission arrive already old.
     */
    private String insertStaleRunningJob(final List<String> paths, final User submittingUser)
            throws Exception {

        final String jobId = UUID.randomUUID().toString();
        final String serverId = APILocator.getServerAPI().readServerId();
        final LocalDateTime staleTimestamp = LocalDateTime.now().minusMinutes(60);

        final List<Map<String, Object>> pathParams = new ArrayList<>();
        for (final String path : paths) {
            final Map<String, Object> p = new HashMap<>();
            p.put("path", path);
            pathParams.add(p);
        }
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", submittingUser.getUserId());
        parameters.put("paths", pathParams);
        final String parametersJson = new ObjectMapper().writeValueAsString(parameters);

        final DotConnect dc = new DotConnect();

        dc.setSQL("INSERT INTO job (id, queue_name, state, parameters, created_at, updated_at, "
                        + "started_at, execution_node) VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?)")
                .addParam(jobId)
                .addParam(FolderBulkDeleteHelper.QUEUE_NAME)
                .addParam(JobState.RUNNING.name())
                .addParam(parametersJson)
                .addParam(Timestamp.valueOf(staleTimestamp))
                .addParam(Timestamp.valueOf(staleTimestamp))
                .addParam(Timestamp.valueOf(staleTimestamp))
                .addParam(serverId)
                .loadResult();

        dc.setSQL("INSERT INTO job_queue (id, queue_name, state, created_at) VALUES (?, ?, ?, ?)")
                .addParam(jobId)
                .addParam(FolderBulkDeleteHelper.QUEUE_NAME)
                .addParam(JobState.RUNNING.name())
                .addParam(Timestamp.valueOf(staleTimestamp))
                .loadResult();

        dc.setSQL("INSERT INTO job_history (id, job_id, state, execution_node, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)")
                .addParam(UUID.randomUUID().toString())
                .addParam(jobId)
                .addParam(JobState.RUNNING.name())
                .addParam(serverId)
                .addParam(Timestamp.valueOf(staleTimestamp))
                .loadResult();

        return jobId;
    }

    @SuppressWarnings("unchecked")
    private List<BatchItemResult> resultsOf(final Job job) {
        final Map<String, Object> metadata = job.result().orElseThrow().metadata().orElseThrow();
        final List<Object> raw = (List<Object>) metadata.get("results");
        final ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module());
        return raw.stream()
                .map(r -> mapper.convertValue(r, BatchItemResult.class))
                .collect(Collectors.toList());
    }

    private BatchItemResult resultFor(final Job job, final String key) {
        return resultsOf(job).stream()
                .filter(r -> r.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no result recorded for " + key));
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}, via the real queue and the
     * real {@link AbandonedJobDetector}
     * Given Scenario: A run is abandoned mid-flight after already deleting one of its two
     * folders; the abandoned-job detector re-queues it and the framework's own worker re-runs it
     * ExpectedResult: The already-gone folder is reported {@code SUCCESS} — "already gone, which
     * is the intended end state" — not {@code PATH_NOT_FOUND}; the untouched folder is genuinely
     * deleted on this second attempt and also reported {@code SUCCESS} (FR-030)
     */
    @Test
    public void test_process_reQueuedAfterAbandonment_alreadyDeletedFolderReportedSuccess()
            throws Exception {

        ensureQueueStarted();

        final Folder alreadyGone = folder();
        final Folder stillThere = folder();
        final String alreadyGonePath = pathOf(alreadyGone);
        final String stillTherePath = pathOf(stillThere);

        // Simulates what the abandoned run's own worker got done before it died: one folder is
        // already gone, the other was never reached.
        folderAPI.delete(alreadyGone, admin, false);

        final String jobId =
                insertStaleRunningJob(List.of(alreadyGonePath, stillTherePath), admin);

        abandonedJobDetector.detectAbandonedJobs();

        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId).state() == JobState.ABANDONED);

        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId).state() == JobState.SUCCESS);

        final Job finished = jobQueueManagerAPI.getJob(jobId);

        final BatchItemResult alreadyGoneResult = resultFor(finished, alreadyGonePath);
        assertEquals(BatchItemStatus.SUCCESS, alreadyGoneResult.status(),
                "a folder a prior (abandoned) attempt already deleted must be reported SUCCESS on "
                        + "the re-queued run, not re-classified as a fresh PATH_NOT_FOUND failure");

        final BatchItemResult stillThereResult = resultFor(finished, stillTherePath);
        assertEquals(BatchItemStatus.SUCCESS, stillThereResult.status(),
                "the folder never reached by the abandoned run must still be genuinely deleted on "
                        + "the re-queued attempt");

        final Folder stillThereGone = folderAPI.find(stillThere.getInode(), admin, false);
        assertTrue(stillThereGone == null
                        || !com.dotmarketing.util.UtilMethods.isSet(stillThereGone.getInode()),
                "the second folder must actually be gone, not merely reported as such");
    }
}
