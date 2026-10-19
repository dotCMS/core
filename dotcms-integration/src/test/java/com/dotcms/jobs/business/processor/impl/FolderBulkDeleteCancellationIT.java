package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.rest.api.v1.asset.bulkdelete.FolderBulkDeleteHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for cancellation (#37063, spec US3 — "a cancelled run never leaves a
 * half-deleted folder", non-negotiable per the spec's own priority rationale).
 * <p>
 * Drives the <b>real</b> async job queue (`createJob` + `cancelJob`), not a hand-built
 * {@link Job} passed straight to {@link FolderBulkDeleteProcessor#process(Job)} — cancellation is
 * inherently about a second thread interrupting a first one, which only the real queue exercises.
 * <p>
 * <b>Design note carried over from implementation</b>: this processor never interrupts an
 * in-flight {@code FolderAPI.delete(...)} call — the cancellation flag is only checked *before*
 * starting the next top-level folder. That is what makes FR-023's "fully deleted or fully
 * untouched, never partial" guarantee free: no delete call is ever cut short, so whichever folder
 * happens to be running when cancellation lands simply finishes normally, and only the folders not
 * yet started are skipped. These tests do not need to land cancellation at a precise instant for
 * correctness — only for exercising "some done, some skipped" rather than "all or nothing", which
 * is why one of the four folders carries enough content to take measurably longer than the others.
 */
@EnableWeld
public class FolderBulkDeleteCancellationIT extends Junit5WeldBaseTest {

    private static final int BIG_FOLDER_CONTENT_COUNT = 40;

    /**
     * A cancelled run's outcome is read back through the <b>real</b> queue — {@code createJob} /
     * {@code getJob} — which round-trips {@code Job#result()} through {@code PostgresJobQueue}'s
     * own JSON column (see its {@code writeValueAsString} calls). Unlike {@code
     * FolderBulkDeleteProcessorIT}, which calls {@code process(job)} and {@code
     * getResultMetadata(job)} directly in the same JVM and therefore sees the very {@link
     * BatchItemResult} instances the processor built, this test's {@code results} entries come
     * back as generic {@code LinkedHashMap}s with no type information — a plain cast throws {@code
     * ClassCastException}. Converting each one back through the same {@code @JsonDeserialize(as =
     * BatchItemResult.class)} annotation {@code AbstractBatchItemResult} already carries recovers
     * the typed view symmetrically with however the queue serialized it.
     */
    private static final ObjectMapper RESULT_MAPPER =
            new ObjectMapper().registerModule(new Jdk8Module());

    private static BatchItemResult toBatchItemResult(final Object raw) {
        return RESULT_MAPPER.convertValue(raw, BatchItemResult.class);
    }

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    private static User admin;
    private static Host site;
    private static FolderAPI folderAPI;
    private static ContentType contentType;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
        folderAPI = APILocator.getFolderAPI();
        contentType = new ContentTypeDataGen().nextPersisted();
    }

    private Folder folder() {
        return new FolderDataGen().site(site).nextPersisted();
    }

    private String pathOf(final Folder folder) {
        return String.format("//%s/%s/", site.getHostname(), folder.getName());
    }

    /**
     * Submits four folders — the second carrying enough content that its own delete call takes
     * measurably longer than the other three, near-empty ones — cancels as soon as the queue
     * reports the job {@code RUNNING}, and waits for the terminal {@code CANCELED} state.
     *
     * @return the four folders, in submission order, plus the terminal job
     */
    private CancelledRun submitFourFoldersAndCancel() throws Exception {

        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        final Folder first = folder();
        final Folder big = folder();
        for (int i = 0; i < BIG_FOLDER_CONTENT_COUNT; i++) {
            new ContentletDataGen(contentType.id()).host(site).folder(big).nextPersisted();
        }
        final Folder third = folder();
        final Folder fourth = folder();
        final List<Folder> folders = List.of(first, big, third, fourth);

        final List<Map<String, Object>> pathParams = new ArrayList<>();
        for (final Folder f : folders) {
            final Map<String, Object> p = new HashMap<>();
            p.put("path", pathOf(f));
            pathParams.add(p);
        }
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", admin.getUserId());
        parameters.put("paths", pathParams);

        final String jobId = jobQueueManagerAPI.createJob(FolderBulkDeleteHelper.QUEUE_NAME,
                parameters);

        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId).state() == JobState.RUNNING);

        jobQueueManagerAPI.cancelJob(jobId);

        Awaitility.await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId).state() == JobState.CANCELED);

        final Job finished = jobQueueManagerAPI.getJob(jobId);
        return new CancelledRun(folders, finished);
    }

    private record CancelledRun(List<Folder> folders, Job job) {
    }

    /**
     * Method to test: cancellation of a {@link FolderBulkDeleteProcessor} run via the real queue
     * Given Scenario: Four folders submitted, cancellation requested while the run is in flight
     * ExpectedResult: The run ends in `CANCELED`; every folder is recorded exactly once, as either
     * `SUCCESS` or `SKIPPED` — never `FAILED` — and every `SKIPPED` folder still exists, untouched
     * (US3 scenarios 1-2, SC-005, FR-026, FR-027)
     */
    @Test
    public void test_cancellation_betweenFolders_completedGone_untouchedRemainderSkipped()
            throws Exception {

        final CancelledRun run = submitFourFoldersAndCancel();

        assertEquals(JobState.CANCELED, run.job().state());

        @SuppressWarnings("unchecked")
        final List<Object> rawResults =
                (List<Object>) run.job().result().orElseThrow().metadata().orElseThrow()
                        .get("results");
        final List<BatchItemResult> results = rawResults.stream()
                .map(FolderBulkDeleteCancellationIT::toBatchItemResult)
                .collect(Collectors.toList());
        assertEquals(4, results.size(), "every folder must produce exactly one outcome record");

        for (final BatchItemResult result : results) {
            assertTrue(result.status() == BatchItemStatus.SUCCESS
                            || result.status() == BatchItemStatus.SKIPPED,
                    "a cancelled run must never FAIL a folder it never attempted: " + result);
        }

        for (final BatchItemResult result : results) {
            if (result.status() == BatchItemStatus.SKIPPED) {
                final String skippedPath = result.key();
                final Folder skippedFolder = run.folders().stream()
                        .filter(f -> pathOf(f).equals(skippedPath))
                        .findFirst().orElseThrow();
                final Folder stillThere = folderAPI.find(skippedFolder.getInode(), admin, false);
                assertTrue(stillThere != null && UtilMethods.isSet(stillThere.getInode()),
                        "a SKIPPED folder must be untouched: " + skippedPath);
            }
        }
    }

    /**
     * Method to test: FR-023's all-or-nothing guarantee under cancellation
     * Given Scenario: The same four-folder cancelled run, focused on the folder large enough to
     * still be mid-delete when cancellation was requested
     * ExpectedResult: That folder — inspected directly, not just via the outcome record — is
     * either completely gone (folder and every one of its contentlets) or completely present
     * (folder and every one of its contentlets); never a state with the folder gone but content
     * left behind, or the folder present with only some content removed (FR-023, US3 scenario 3 —
     * the Constitution-flagged rollback proof)
     */
    @Test
    public void test_cancellation_bigFolder_endsFullyDeletedOrFullyPresent_neverPartial()
            throws Exception {

        final CancelledRun run = submitFourFoldersAndCancel();
        final Folder big = run.folders().get(1);

        final Folder foundFolder = folderAPI.find(big.getInode(), admin, false);
        final boolean folderGone = foundFolder == null || !UtilMethods.isSet(foundFolder.getInode());

        final List<Contentlet> remainingContent =
                APILocator.getContentletAPI().findContentletsByFolder(big, admin, false);

        if (folderGone) {
            assertTrue(remainingContent.isEmpty(),
                    "the big folder is gone but " + remainingContent.size()
                            + " of its contentlets survived it — a partially-rolled-back subtree");
        } else {
            assertEquals(BIG_FOLDER_CONTENT_COUNT, remainingContent.size(),
                    "the big folder still exists but is missing some of its own content — "
                            + "a partially-deleted subtree");
        }
    }

    /**
     * Method to test: {@link FolderBulkDeleteProcessor#getResultMetadata(Job)} on a cancelled run
     * Given Scenario: The same four-folder cancelled run
     * ExpectedResult: The outcome records where the run stopped, so the remainder can be
     * resubmitted without the author having to guess which folders were never reached (FR-028)
     */
    @Test
    public void test_cancellation_outcomeRecordsWhereTheRunStopped() throws Exception {

        final CancelledRun run = submitFourFoldersAndCancel();

        final Map<String, Object> metadata =
                run.job().result().orElseThrow().metadata().orElseThrow();

        assertTrue(((Number) metadata.get("skippedCount")).intValue() > 0,
                "this run must have left at least one folder unattempted to be a meaningful "
                        + "cancellation test");
        assertTrue(metadata.containsKey("stoppedAt") && metadata.get("stoppedAt") != null,
                "a cancelled run's outcome must record where it stopped (FR-028)");
    }
}
