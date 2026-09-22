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
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper;
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
 * Integration test for the liveness heartbeat wired into a single top-level folder's delete
 * (#37063, FR-024a, plan.md PO-4).
 * <p>
 * <b>Why the abandonment threshold is lowered by {@code Config.setProperty} here but that same
 * trick was rejected for {@code FolderBulkDeleteResumeIT}.</b> Two different consumers read this
 * property, with two different caching behaviors: {@code AbandonedJobDetectorConfigProducer} caches
 * it in a {@code static final} field at class-load time, so a runtime override only works for
 * whichever test happens to load that class first — which is why the resume test backdates a
 * timestamp instead. {@code FolderBulkDeleteProcessor}'s own heartbeat-interval computation (T071)
 * must read {@code Config.getIntProperty(...)} fresh on every {@code process(Job)} call — no
 * caching of its own — specifically so a test can control it reliably. This test exercises that
 * second, uncached read; it does not touch {@code AbandonedJobDetector} at all.
 * <p>
 * <b>Deliberately slow, not a mistake.</b> The threshold's minute granularity floors the derived
 * heartbeat interval at 60/3 = 20 seconds even at its lowest configurable value (1 minute), so
 * proving the heartbeat fires more than once mid-delete needs a delete that genuinely runs longer
 * than that — a large content count, not an artificial delay hook (none exists for
 * {@code FolderAPI.delete}, and adding one only for this test would test the hook, not the real
 * call). Matches this suite's own accepted cost for "expensive but real" tests elsewhere (the
 * cancellation and overlap-guard tests' own "big folder" technique), just with a bigger folder and
 * a longer budget.
 */
@EnableWeld
public class FolderBulkDeleteHeartbeatIT extends Junit5WeldBaseTest {

    private static final int BIG_FOLDER_CONTENT_COUNT = 500;
    private static final String THRESHOLD_KEY = "JOB_ABANDONMENT_THRESHOLD_MINUTES";

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    private static User admin;
    private static Host site;
    private static ContentType contentType;
    private static String originalThreshold;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        contentType = new ContentTypeDataGen().nextPersisted();

        originalThreshold = Config.getStringProperty(THRESHOLD_KEY, "30");
        // The lowest value the config's minute granularity allows — see the class javadoc on why
        // this floors the derived heartbeat interval at 20 seconds, not something smaller.
        Config.setProperty(THRESHOLD_KEY, "1");
    }

    @AfterAll
    public static void restore() {
        Config.setProperty(THRESHOLD_KEY, originalThreshold);
    }

    private String pathOf(final Folder folder) {
        return String.format("//%s/%s/", site.getHostname(), folder.getName());
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#process(Job)}, via the real queue Given
     * Scenario: A single folder heavy enough that its own delete call outlasts one derived
     * heartbeat interval (20 seconds at the lowest configurable threshold) ExpectedResult:
     * {@code job.updatedAt()} — read fresh on every poll, the same cache-safe path
     * {@code HeartbeatIT} already established — advances more than once while the job is still
     * {@code RUNNING}, and {@code job.progress()} never leaves {@code 0.0} while running: a
     * single-folder run has nothing to report progress on until that one folder finishes, so any
     * mid-delete movement could only come from the heartbeat mistakenly driving progress instead of
     * just liveness — exactly the distinction FR-024a and FR-024/FR-025 draw between the two
     * signals. The run must still reach {@code SUCCESS}, never {@code ABANDONED}.
     */
    @Test
    public void test_process_bigFolderDelete_heartbeatAdvancesUpdatedAt_neverAbandoned()
            throws Exception {

        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        final Folder big = new FolderDataGen().site(site).nextPersisted();
        for (int i = 0; i < BIG_FOLDER_CONTENT_COUNT; i++) {
            new ContentletDataGen(contentType.id()).host(site).folder(big).nextPersisted();
        }

        final Map<String, Object> pathParam = new HashMap<>();
        pathParam.put("path", pathOf(big));
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", admin.getUserId());
        parameters.put("paths", List.of(pathParam));

        final String jobId = jobQueueManagerAPI.createJob(FolderBulkDeleteHelper.QUEUE_NAME,
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
                "updated_at must have advanced more than once while the single folder's delete "
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
}
