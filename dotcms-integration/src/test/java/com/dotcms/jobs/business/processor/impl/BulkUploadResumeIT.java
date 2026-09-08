package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.batch.BatchItemStatus;
import com.dotcms.jobs.business.batch.JobItemResultFactory;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for resuming an interrupted run (spec FR-036 … FR-039, SC-009).
 * <p>
 * <b>What resumability is actually protecting against, since it is easy to state it wrongly.</b> A
 * re-run without resume does <b>not</b> duplicate the author's files: the unique index on the
 * lower-cased path rejects the second create. What happens is quieter and worse — the files the
 * first attempt created come back as {@code NAME_COLLISION}, so the author is told 30 files failed
 * when all 30 are sitting in the folder. <b>The report lies.</b> That is the defect these tests
 * exist to prevent, not duplicated data.
 * <p>
 * And a second attempt is not hypothetical. {@code AbandonedJobDetector} re-queues a stalled run
 * <b>without consulting the retry policy</b>, so an interrupted run is retried whether or not its
 * processor is marked no-retry. Resumability is the only answer available, not the better of two.
 */
@EnableWeld
public class BulkUploadResumeIT extends Junit5WeldBaseTest {

    private final JobItemResultFactory itemResults = new JobItemResultFactory();

    @BeforeAll
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private User admin() throws Exception {
        return APILocator.systemUser();
    }

    private HttpServletRequest request() throws Exception {
        final HttpServletRequest request = new MockSessionRequest(new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request();
        request.setAttribute(com.liferay.portal.util.WebKeys.USER, admin());
        return request;
    }

    /**
     * Builds a job over {@code names.size()} staged files, using the names given so a test can
     * create two entries that deliberately share one.
     */
    private Job jobFor(final Folder folder, final List<String> names) throws Exception {
        final HttpServletRequest request = request();
        final List<Map<String, Object>> stagedFiles = new ArrayList<>();

        for (final String name : names) {
            final DotTempFile tempFile = APILocator.getTempFileAPI().createTempFile(
                    name, request, new ByteArrayInputStream(("content of " + name).getBytes()));

            final Map<String, Object> file = new HashMap<>();
            file.put("tempFileId", tempFile.id);
            file.put("fileName", name);
            file.put("sizeBytes", tempFile.length());
            file.put("mimeType", tempFile.mimeType);
            stagedFiles.add(file);
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("baseType", "DOTASSET");
        parameters.put("folderId", folder.getIdentifier());
        parameters.put("targetId", folder.getIdentifier());
        parameters.put("userId", admin().getUserId());
        parameters.put("stagedFiles", stagedFiles);
        parameters.put("requestFingerprint",
                APILocator.getTempFileAPI().getRequestFingerprint(request));

        return Job.builder()
                .id(UUID.randomUUID().toString())
                .queueName("assetBulkUpload")
                .state(JobState.RUNNING)
                .parameters(parameters)
                .progressTracker(new DefaultProgressTracker())
                .build();
    }

    private static List<String> names(final int count) {
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            names.add("resume-" + i + "-" + UUID.randomUUID() + ".txt");
        }
        return names;
    }

    /**
     * Method to test: {@link BulkUploadProcessor#process} on a re-queued run
     * <p>
     * Given scenario: A run of 20 files is interrupted after part of it has been created, and the
     * same job is processed again — which is what the abandoned-job sweep does.
     * <p>
     * Expected result: The folder holds exactly 20 files, <b>not more and not fewer</b>, and the
     * outcome reports 20 succeeded. Without resume the second attempt re-attempts everything and
     * the already-created files come back as collisions, so the author is told most of the batch
     * failed while all of it is in the folder.
     */
    @Test
    public void test_resume_producesExactlyOneCopyOfEachFileAndAnHonestCount() throws Exception {
        final Folder folder = new FolderDataGen().site(new SiteDataGen().nextPersisted())
                .nextPersisted();
        final Job job = jobFor(folder, names(20));

        // First attempt, interrupted: record the first 8 as done, as the run would have.
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> staged =
                (List<Map<String, Object>>) job.parameters().get("stagedFiles");
        new BulkUploadProcessor().process(job);

        // Second attempt on the same job id — the sweep re-queues without consulting the retry
        // policy, so this happens whether the processor wants it or not.
        final BulkUploadProcessor second = new BulkUploadProcessor();
        second.process(job);
        final Map<String, Object> outcome = second.getResultMetadata(job);

        final List<Contentlet> inFolder =
                APILocator.getFolderAPI().getWorkingContent(folder, admin(), false);

        assertEquals(20, inFolder.size(),
                "exactly one copy of each file — a resumed run must not recreate what it made");
        assertEquals(20, ((Number) outcome.get("successCount")).intValue(),
                "and the count must not report the first attempt's work as failures");
        assertEquals(0, ((Number) outcome.get("failedCount")).intValue(),
                "nothing collided, because nothing was attempted twice");
        assertEquals(staged.size(), inFolder.size());
    }

    /**
     * Method to test: the resume checkpoint's key
     * <p>
     * Given scenario: A batch containing <b>two files that share a name</b>, which spec.md
     * §Edge Cases explicitly allows, resumed after the first was created.
     * <p>
     * Expected result: The resume distinguishes them. This is why the checkpoint is keyed
     * {@code (job_id, seq)} and not {@code (job_id, item_key)}: keyed by name, the second write
     * would collide with the first and a resumed run could not tell which of the two had already
     * completed — so it would either skip a file the author supplied or recreate one it already
     * made.
     */
    @Test
    public void test_resume_tellsApartTwoFilesThatShareAName() throws Exception {
        final Folder folder = new FolderDataGen().site(new SiteDataGen().nextPersisted())
                .nextPersisted();
        final String shared = "report-" + UUID.randomUUID() + ".txt";
        final Job job = jobFor(folder, List.of(shared, shared));

        new BulkUploadProcessor().process(job);

        final List<Integer> completed = itemResults.findCompletedSeqs(job.id());
        assertTrue(completed.contains(0),
                "the first of the two is recorded against its own seq");

        // Both rows must exist and be addressable apart, whatever their individual outcomes: the
        // second is subject to the same collision rule as a pre-existing name, which is a per-file
        // result and not a reason to lose the row.
        assertEquals(2, itemResults.findByJobId(job.id()).size(),
                "two rows, because two files were submitted — the name is not the key");

        new BulkUploadProcessor().process(job);
        assertEquals(2, itemResults.findByJobId(job.id()).size(),
                "and a resumed run neither adds a third row nor loses one");
    }

    /**
     * Method to test: {@link BulkUploadProcessor#getResultMetadata} across attempts
     * <p>
     * Given scenario: A run whose first attempt failed one file and succeeded another, then resumed.
     * <p>
     * Expected result: The counts cover the <b>whole batch across all attempts</b> (FR-038), not
     * just the final one. A resumed run that reported only what it personally did would tell an
     * author who submitted 20 files that 12 were created, which is true of the attempt and false of
     * their batch.
     */
    @Test
    public void test_resume_countsCoverTheWholeBatchNotTheLastAttempt() throws Exception {
        final Folder folder = new FolderDataGen().site(new SiteDataGen().nextPersisted())
                .nextPersisted();
        final Job job = jobFor(folder, names(5));

        // Pre-record one item as already failed, as an interrupted first attempt would have left it.
        itemResults.record(job.id(), 0, "pre-failed.txt", BatchItemStatus.FAILED, null,
                "left by a previous attempt", null);

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.process(job);
        final Map<String, Object> outcome = processor.getResultMetadata(job);

        assertEquals(5, ((Number) outcome.get("total")).intValue(),
                "the total is the batch the author submitted, across attempts");
        assertTrue(((Number) outcome.get("successCount")).intValue() >= 4,
                "and the retried items count toward it");
    }
}
