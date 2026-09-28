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
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.rest.api.v1.asset.bulkduplicate.FolderBulkDuplicateHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.awaitility.Awaitility;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Cancellation of a bulk folder duplication, driven through the real queue (#37062, US4).
 * <p>
 * Mirrors {@code FolderBulkDeleteCancellationIT}: four folders, the second large enough to still
 * be copying when cancellation arrives. Cancellation is honoured between folders, never inside one,
 * so every folder ends fully duplicated or not duplicated at all.
 */
@EnableWeld
public class FolderBulkDuplicateCancellationIT extends Junit5WeldBaseTest {

    private static final int BIG_FOLDER_CONTENT_COUNT = 40;

    /** Results come back from the queue's JSON column as maps; this reads them back as records. */
    private static final ObjectMapper RESULT_MAPPER =
            new ObjectMapper().registerModule(new Jdk8Module());

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
        contentType = new ContentTypeDataGen().host(site).nextPersisted();
    }

    private Folder folder() {
        return new FolderDataGen().site(site).nextPersisted();
    }

    private String pathOf(final Folder folder) {
        return "//" + site.getHostname() + folder.getPath();
    }

    /** How many contentlets sit directly under a folder at this path, 0 when it does not exist. */
    private long contentletsAt(final String folderPath) throws Exception {
        return new DotConnect()
                .setSQL("select count(*) as n from identifier where host_inode = ? "
                        + "and parent_path = ? and asset_type = 'contentlet'")
                .addParam(site.getIdentifier())
                .addParam(folderPath)
                .getInt("n");
    }

    private boolean duplicateExists(final Folder source) throws Exception {
        final Folder copy = folderAPI.findFolderByPath("/" + source.getName() + "_copy/", site,
                admin, false);
        return copy != null && copy.getInode() != null;
    }

    /**
     * Submits four folders, the second holding many contentlets, cancels once the job is running,
     * and waits for it to end.
     */
    private Job submitFourFoldersAndCancel(final List<Folder> folders) throws Exception {
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(5, TimeUnit.SECONDS);
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", admin.getUserId());
        parameters.put("assetPaths", folders.stream().map(this::pathOf).toList());
        final String jobId = jobQueueManagerAPI.createJob(FolderBulkDuplicateHelper.QUEUE_NAME,
                parameters);

        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId).state() == JobState.RUNNING);
        jobQueueManagerAPI.cancelJob(jobId);
        Awaitility.await().atMost(60, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> jobQueueManagerAPI.getJob(jobId).state() == JobState.CANCELED);

        return jobQueueManagerAPI.getJob(jobId);
    }

    private BatchItemResult resultFor(final List<BatchItemResult> results, final Folder folder) {
        return results.stream().filter(r -> r.key().equals(pathOf(folder))).findFirst()
                .orElseThrow();
    }

    /** The submitted folders whose outcome has the given status. */
    private List<Folder> foldersWith(final List<Folder> folders,
            final List<BatchItemResult> results, final BatchItemStatus status) {
        return folders.stream().filter(f -> resultFor(results, f).status() == status).toList();
    }

    @SuppressWarnings("unchecked")
    private static List<BatchItemResult> resultsOf(final Job job) {
        return ((List<Object>) job.result().orElseThrow().metadata().orElseThrow().get("results"))
                .stream()
                .map(raw -> RESULT_MAPPER.convertValue(raw, BatchItemResult.class))
                .toList();
    }

    /**
     * Method to test: cancelling a {@link FolderBulkDuplicateProcessor} run through the real queue
     * Given Scenario: Four folders, cancelled while the run is in flight
     * ExpectedResult: The job ends CANCELED; every folder has exactly one record, SUCCESS or
     * SKIPPED and never FAILED; at least one folder was never reached, and stoppedAt names the first
     * of them; each SKIPPED folder carries no reason and has no duplicate; each
     * SUCCESS folder's duplicate holds everything its source holds, so none is partial
     */
    @Test
    public void test_cancellation_betweenFolders_neverLeavesAPartialDuplicate() throws Exception {
        final Folder big = folder();
        for (int i = 0; i < BIG_FOLDER_CONTENT_COUNT; i++) {
            new ContentletDataGen(contentType.id()).host(site).folder(big).nextPersisted();
        }
        final List<Folder> folders = List.of(folder(), big, folder(), folder());

        final Job job = submitFourFoldersAndCancel(folders);

        assertEquals(JobState.CANCELED, job.state());
        final List<BatchItemResult> results = resultsOf(job);
        assertEquals(4, results.size(), "every folder must have exactly one outcome record");

        assertTrue(results.stream().allMatch(r -> r.status() == BatchItemStatus.SUCCESS
                || r.status() == BatchItemStatus.SKIPPED), "never FAILED: " + results);
        // The run must actually stop: cancelled while the large second folder is copying, at
        // least the last folder is never reached. Without this an ignored cancel would pass.
        assertTrue(results.stream().anyMatch(r -> r.status() == BatchItemStatus.SKIPPED),
                "cancellation must leave the unreached folders skipped: " + results);

        for (final Folder skipped : foldersWith(folders, results, BatchItemStatus.SKIPPED)) {
            assertTrue(resultFor(results, skipped).reason().isEmpty(),
                    "a cancellation skip carries no reason");
            assertTrue(!duplicateExists(skipped), "a skipped folder must not be duplicated");
        }
        // Where it stopped, so the remainder can be resubmitted deliberately (FR-032): the first
        // folder the run never reached, in submission order.
        final String firstUnreached = results.stream()
                .filter(r -> r.status() == BatchItemStatus.SKIPPED && r.reason().isEmpty())
                .findFirst().orElseThrow().key();
        assertEquals(firstUnreached,
                job.result().orElseThrow().metadata().orElseThrow().get("stoppedAt"),
                "a cancelled run must record where it stopped");

        for (final Folder completed : foldersWith(folders, results, BatchItemStatus.SUCCESS)) {
            assertEquals(contentletsAt(completed.getPath()),
                    contentletsAt("/" + completed.getName() + "_copy/"),
                    "a completed duplicate must hold everything: " + completed.getName());
        }
    }
}
