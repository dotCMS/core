package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.batch.BatchFailureReason;
import com.dotcms.jobs.business.batch.BatchItemResult;
import com.dotcms.jobs.business.batch.BatchItemStatus;
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
 * Integration tests for two batches contending for one name (spec FR-041, FR-042, FR-042a, SC-010).
 * <p>
 * <b>The collision is not this feature's rule and must not become one.</b> The unique index on the
 * lower-cased path already decides who wins; what this feature adds is telling the loser <i>why</i>
 * in terms they can act on. A collision reported as an unclassified failure sends an author looking
 * for a problem with their file, when the fix is to rename it.
 * <p>
 * Case-insensitivity matters for the same reason: {@code Report.pdf} and {@code report.pdf} are one
 * contended name, so a client that suggests a differently-cased alternative is suggesting the same
 * collision again.
 */
@EnableWeld
public class BulkUploadConcurrencyIT extends Junit5WeldBaseTest {

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

    private Job jobFor(final Folder folder, final List<String> names) throws Exception {
        final HttpServletRequest request = request();
        final List<Map<String, Object>> stagedFiles = new ArrayList<>();

        for (final String name : names) {
            final DotTempFile tempFile = APILocator.getTempFileAPI().createTempFile(
                    name, request, new ByteArrayInputStream(("body of " + name).getBytes()));

            final Map<String, Object> file = new HashMap<>();
            file.put("tempFileId", tempFile.id);
            file.put("fileName", name);
            file.put("sizeBytes", tempFile.length());
            file.put("mimeType", tempFile.mimeType);
            stagedFiles.add(file);
        }

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("baseType", "FILEASSET");
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

    private BatchFailureReason reasonFor(final Map<String, Object> outcome, final String key) {
        @SuppressWarnings("unchecked")
        final List<BatchItemResult> results = (List<BatchItemResult>) outcome.get("results");
        return results.stream()
                .filter(r -> r.key().equals(key))
                .findFirst()
                .flatMap(BatchItemResult::reason)
                .orElse(null);
    }

    /**
     * Method to test: the run's outcome when a name is already taken
     * <p>
     * Given scenario: Two batches, run one after the other, each carrying a file of the same name
     * into the same folder.
     * <p>
     * Expected result: Exactly one file exists, and the second run reports {@code NAME_COLLISION}
     * <b>for that file only</b> (FR-041, FR-042, SC-010). Reporting it as unclassified sends the
     * author looking for a problem with their file when the fix is to rename it — and the batch
     * must not abort, because the collision is one file's problem and not the batch's.
     */
    @Test
    public void test_run_reportsACollisionAsItsOwnReasonAndKeepsGoing() throws Exception {
        final Folder folder = new FolderDataGen().site(new SiteDataGen().nextPersisted())
                .nextPersisted();
        final String contended = "contended-" + UUID.randomUUID() + ".txt";
        final String other = "free-" + UUID.randomUUID() + ".txt";

        new BulkUploadProcessor().process(jobFor(folder, List.of(contended)));

        final BulkUploadProcessor second = new BulkUploadProcessor();
        final Job secondJob = jobFor(folder, List.of(contended, other));
        second.process(secondJob);
        final Map<String, Object> outcome = second.getResultMetadata(secondJob);

        assertEquals(BatchFailureReason.NAME_COLLISION, reasonFor(outcome, contended),
                "the loser must be told the name is taken, not merely that it failed. Recorded: "
                        + outcome.get("results"));
        assertEquals(BatchItemStatus.SUCCESS, statusFor(outcome, other),
                "and the rest of the batch still lands — a collision is one file's problem");

        final List<Contentlet> inFolder =
                APILocator.getFolderAPI().getWorkingContent(folder, admin(), false);
        assertEquals(2, inFolder.size(),
                "exactly one file of the contended name, plus the one that was free");
    }

    /**
     * Method to test: the collision rule's case sensitivity
     * <p>
     * Given scenario: {@code Report.pdf} exists and {@code report.pdf} is uploaded into the same
     * folder.
     * <p>
     * Expected result: A collision (FR-042a). The unique index is on the <b>lower-cased</b> path, so
     * these are one contended name — and a client that offers a differently-cased alternative is
     * offering the same collision again.
     */
    @Test
    public void test_run_treatsNamesDifferingOnlyByCaseAsOneName() throws Exception {
        final Folder folder = new FolderDataGen().site(new SiteDataGen().nextPersisted())
                .nextPersisted();
        final String stem = "Report-" + UUID.randomUUID();

        new BulkUploadProcessor().process(jobFor(folder, List.of(stem + ".txt")));

        final BulkUploadProcessor second = new BulkUploadProcessor();
        final Job secondJob = jobFor(folder, List.of(stem.toLowerCase() + ".txt"));
        second.process(secondJob);
        final Map<String, Object> outcome = second.getResultMetadata(secondJob);

        assertEquals(BatchFailureReason.NAME_COLLISION,
                reasonFor(outcome, stem.toLowerCase() + ".txt"),
                "case does not make a second name; the index is on the lower-cased path");
        assertTrue(APILocator.getFolderAPI().getWorkingContent(folder, admin(), false).size() <= 1,
                "and only one of the two exists");
    }

    private BatchItemStatus statusFor(final Map<String, Object> outcome, final String key) {
        @SuppressWarnings("unchecked")
        final List<BatchItemResult> results = (List<BatchItemResult>) outcome.get("results");
        return results.stream().filter(r -> r.key().equals(key)).findFirst()
                .map(BatchItemResult::status).orElse(null);
    }
}
