package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.NoRetryPolicy;
import com.dotcms.rest.api.v1.asset.bulkduplicate.FolderBulkDuplicateHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Liveness and no-retry for a bulk folder duplication (#37062, FR-028, FR-034).
 * <p>
 * Mirrors {@code FolderBulkDeleteHeartbeatIT}: one folder heavy enough that its copy outlasts a
 * short, test-only heartbeat interval several times, driven through the real queue. The heartbeat
 * must keep the run alive without being mistaken for progress. The no-retry decision is pinned on
 * the processor's annotation: forcing a job into ABANDONED through the queue would race the
 * abandonment detector.
 */
@EnableWeld
public class FolderBulkDuplicateHeartbeatIT extends Junit5WeldBaseTest {

    private static final int BIG_FOLDER_CONTENT_COUNT = 500;
    private static final String THRESHOLD_KEY = "JOB_ABANDONMENT_THRESHOLD_MINUTES";
    private static final String HEARTBEAT_INTERVAL_KEY =
            "FOLDER_BULK_DUPLICATE_HEARTBEAT_INTERVAL_MILLIS";
    private static final String HEARTBEAT_INTERVAL_MILLIS_UNDER_TEST = "2000";

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    private static User admin;
    private static Host site;
    private static ContentType contentType;
    private static String originalThreshold;
    private static String originalHeartbeatInterval;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        contentType = new ContentTypeDataGen().nextPersisted();

        originalThreshold = Config.getStringProperty(THRESHOLD_KEY, "30");
        // Lowered too, so a genuinely abandoned run (were one to happen here) would still be
        // caught quickly — but per the class javadoc, this alone cannot get the tick under 20s.
        Config.setProperty(THRESHOLD_KEY, "1");

        originalHeartbeatInterval = Config.getStringProperty(HEARTBEAT_INTERVAL_KEY, (String) null);
        Config.setProperty(HEARTBEAT_INTERVAL_KEY, HEARTBEAT_INTERVAL_MILLIS_UNDER_TEST);
    }

    @AfterAll
    public static void restore() {
        Config.setProperty(THRESHOLD_KEY, originalThreshold);
        // null (this key was never set before this test) is itself a valid value here — Config's
        // own trackOverrides treats setProperty(key, null) as the removal it should be.
        Config.setProperty(HEARTBEAT_INTERVAL_KEY, originalHeartbeatInterval);
    }

        /**
     * Method to test: {@link FolderBulkDuplicateProcessor#process(Job)}, via the real queue Given
     * Scenario: A single folder heavy enough that its own duplication outlasts the overridden
     * 2-second heartbeat interval several times over ExpectedResult:
     * {@code job.updatedAt()} — read fresh on every poll, the same cache-safe path
     * {@code HeartbeatIT} already established — advances more than once while the job is still
     * {@code RUNNING}, and {@code job.progress()} never leaves {@code 0.0} while running: a
     * single-folder run has nothing to report progress on until that one folder finishes, so any
     * mid-duplication movement could only come from the heartbeat mistakenly driving progress instead of
     * just liveness — exactly the distinction FR-024a and FR-024/FR-025 draw between the two
     * signals. The run must still reach {@code SUCCESS}, never {@code ABANDONED}.
     */
    @Test
    public void test_process_bigFolderDuplicate_heartbeatAdvancesUpdatedAt_neverAbandoned()
            throws Exception {

        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        final Folder big = new FolderDataGen().site(site).nextPersisted();
        for (int i = 0; i < BIG_FOLDER_CONTENT_COUNT; i++) {
            new ContentletDataGen(contentType.id()).host(site).folder(big).nextPersisted();
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", admin.getUserId());
        parameters.put("assetPaths", List.of("//" + site.getHostname() + big.getPath()));

        final String jobId = jobQueueManagerAPI.createJob(FolderBulkDuplicateHelper.QUEUE_NAME,
                parameters);

        final Set<LocalDateTime> updatedAtWhileRunning = new LinkedHashSet<>();
        final List<Float> progressWhileRunning = new ArrayList<>();

        Awaitility.await().atMost(5, TimeUnit.MINUTES)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    final Job current = jobQueueManagerAPI.getJob(jobId);
                    if (current.state() == JobState.RUNNING) {
                        current.updatedAt().ifPresent(updatedAtWhileRunning::add);
                        progressWhileRunning.add(current.progress());
                    }
                    return current.state() == JobState.SUCCESS;
                });

        assertTrue(updatedAtWhileRunning.size() >= 2,
                "updated_at must have advanced more than once while the single folder's duplication "
                        + "was still in progress — a heartbeat firing once is not distinguishable "
                        + "from the job simply having just started; observed: "
                        + updatedAtWhileRunning);

        for (final Float progress : progressWhileRunning) {
            assertEquals(0.0f, progress, 0.001f,
                    "a single-folder run has nothing to report progress on until that folder "
                            + "finishes — the heartbeat must never be mistaken for a progress "
                            + "update");
        }

        assertEquals(JobState.SUCCESS, jobQueueManagerAPI.getJob(jobId).state());
    }

    /**
     * Method to test: the retry policy of {@link FolderBulkDuplicateProcessor}
     * Given Scenario: A duplication run that was abandoned, for example when its node died
     * ExpectedResult: The processor carries {@link NoRetryPolicy}, so the queue manager sends an
     * abandoned run to ABANDONED_PERMANENTLY instead of running it again. Duplication is not
     * idempotent: a second run would create a second set of duplicates.
     */
    @Test
    public void test_processor_isNeverRetried() {
        assertTrue(FolderBulkDuplicateProcessor.class.isAnnotationPresent(NoRetryPolicy.class),
                "a re-run would duplicate every folder again");
    }
}
