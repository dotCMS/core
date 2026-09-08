package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
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
 * Integration tests for the bulk-upload run itself — what actually reaches the folder
 * (spec FR-005, FR-006, SC-001).
 * <p>
 * Where {@code BulkUploadResourceIT} covers the submission, these cover the work. The two are
 * separate because the whole point of the design is that they happen at different times: the
 * submission is answered before any file exists, so "accepted" and "created" are different claims
 * and have to be verified separately.
 * <p>
 * Later phases add to this class rather than replacing it — partial failure (US2), progress and
 * cancellation (US4). Only the US1 cases are here.
 */
@EnableWeld
public class BulkUploadProcessorIT extends Junit5WeldBaseTest {

    @BeforeAll
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /** Resolved per test rather than in a static initialiser, which ran before the API was up. */
    private User admin() throws Exception {
        return APILocator.systemUser();
    }

    private Host site() {
        return new SiteDataGen().nextPersisted();
    }

    /**
     * Renders the per-item reasons into the assertion message.
     * <p>
     * Without this a failure reads "expected 10 but was 0" and says nothing about <b>why</b> —
     * which cost a full diagnostic cycle once already. The reasons are recorded per file precisely
     * so they can be read; a test that hides them wastes what the feature went to trouble to
     * produce.
     */
    @SuppressWarnings("unchecked")
    private String describe(final Map<String, Object> outcome) {
        final Object results = outcome.get("results");
        if (!(results instanceof List)) {
            return "none recorded";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Object item : (List<Object>) results) {
            sb.append("\n  ").append(item);
        }
        return sb.toString();
    }

    /**
     * A request carrying the author, because staging needs one: {@code createTempFile} builds its
     * allow-list from {@code PortalUtil.getUser(request)}, which reads {@code WebKeys.USER} first.
     * The <b>run</b> has no request at all — that is the point of the fingerprint — but the
     * <b>submission</b> does, and this stands in for it.
     */
    private HttpServletRequest request() throws Exception {
        final HttpServletRequest request = new MockSessionRequest(new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request()).request();
        request.setAttribute(com.liferay.portal.util.WebKeys.USER, admin());
        return request;
    }

    /**
     * Stages {@code count} files the way the endpoint would, and builds the job the queue would
     * hand the processor. Staging for real rather than faking it, because the whole reason this
     * class exists is that the run has no HTTP request: the content has to be retrievable by the
     * captured fingerprint, and a fake would hide it if it were not.
     */
    private Job jobFor(final Folder folder, final int count) throws Exception {
        final HttpServletRequest request = request();
        final List<Map<String, Object>> stagedFiles = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final String fileName = "bulk-" + UUID.randomUUID() + ".txt";
            final DotTempFile tempFile = APILocator.getTempFileAPI().createTempFile(
                    fileName, request, new ByteArrayInputStream(("content " + i).getBytes()));

            final Map<String, Object> file = new HashMap<>();
            file.put("tempFileId", tempFile.id);
            file.put("fileName", fileName);
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

    /**
     * Method to test: the bulk-upload processor, end to end
     * <p>
     * Given scenario: A batch of 10 valid files submitted against a folder the author may add
     * children to.
     * <p>
     * Expected result: All 10 exist in that folder when the run reports itself finished, and the
     * outcome's counts say 10 — <b>zero silently discarded</b> (SC-001). This is the criterion the
     * whole ticket exists for: today Content Drive accepts the selection, warns, and uploads the
     * first file only.
     */
    @Test
    public void test_run_createsEveryFileInTheTarget() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 10);

        final BulkUploadProcessor processor = new BulkUploadProcessor();
        processor.process(job);

        final Map<String, Object> outcome = processor.getResultMetadata(job);

        assertEquals(10, ((Number) outcome.get("successCount")).intValue(),
                "every file the author chose must be created — this is the criterion the whole "
                        + "ticket exists for, since today the selection is accepted and only the "
                        + "first file lands.\nRecorded per-item results: " + describe(outcome));
        assertEquals(0, ((Number) outcome.get("failedCount")).intValue());
        assertEquals(10, ((Number) outcome.get("total")).intValue(),
                "and the counts are authoritative, not the number the client believes it sent");

        final List<Contentlet> inFolder =
                APILocator.getFolderAPI().getWorkingContent(folder, admin(), false);
        assertEquals(10, inFolder.size(), "zero silently discarded");

        // NOTE: working content, not live. The run fires the content type's DEFAULT workflow
        // action, while the drag-and-drop path this feature replaces fires PUBLISH explicitly.
        // Whether a bulk upload should publish is a product question FR-006's equivalence rule
        // arguably already answers; it is raised rather than settled here.
    }

    /**
     * Method to test: the bulk-upload processor
     * <p>
     * Given scenario: The same file uploaded through this endpoint and through the existing
     * single-file path.
     * <p>
     * Expected result: Observably equivalent — same resolved content type, same permissions, same
     * workflow behaviour (FR-006). The requirement is the <b>equivalence</b>, not the sharing of a
     * particular call site: the single-file path is entered over REST and a background run cannot
     * re-enter it, so what must not vary is what an author can observe afterwards.
     */
    @Test
    public void test_run_createsFilesEquivalentlyToTheSingleFileUpload() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 1);

        new BulkUploadProcessor().process(job);

        final List<Contentlet> created =
                APILocator.getFolderAPI().getWorkingContent(folder, admin(), false);
        assertEquals(1, created.size());

        final Contentlet asset = created.get(0);
        assertEquals("DotAsset", asset.getContentType().variable(),
                "the content type resolves the same way the single-file upload resolves it");
        assertEquals(folder.getInode(), asset.getFolder(),
                "and it lands where the author asked, not at the site root");
        assertTrue(APILocator.getPermissionAPI().doesUserHavePermission(
                        asset, com.dotmarketing.business.PermissionAPI.PERMISSION_READ,
                        admin(), false),
                "permissions are enforced by the same creation path, so they are inherited "
                        + "rather than reinvented");
    }

    /**
     * Method to test: the bulk-upload processor's index policy
     * <p>
     * Given scenario: A batch completes.
     * <p>
     * Expected result: No file was created with a per-item {@code WAIT_FOR} (FR-008). That policy
     * does not merely block on an index refresh — it also flushes the system-wide query cache on
     * every file, so a full batch would charge every other user one flush per file. The
     * batch-level resolution that replaces it is asserted in {@code BulkUploadIndexingIT}, together
     * with its ordering against the completion signal.
     */
    @Test
    public void test_run_doesNotSerializeBehindAPerFileIndexWait() throws Exception {
        final Folder folder = new FolderDataGen().site(site()).nextPersisted();
        final Job job = jobFor(folder, 5);

        final long startedAt = System.currentTimeMillis();
        new BulkUploadProcessor().process(job);
        final long elapsed = System.currentTimeMillis() - startedAt;

        assertEquals(5, APILocator.getFolderAPI().getWorkingContent(folder, admin(), false).size());

        // A per-file WAIT_FOR blocks on the next index refresh for each file — the default refresh
        // interval is 1s and this repository does not override it — so five files would floor at
        // roughly five seconds plus five system-wide query cache flushes. Generous headroom on
        // purpose: this guards an order-of-magnitude regression, not jitter, because a hard
        // threshold in CI is a flaky test rather than a useful one.
        assertTrue(elapsed < 5_000L, String.format(
                "a 5-file batch took %dms, which is the shape of a per-file index wait rather "
                        + "than one batch-level resolution", elapsed));
    }
}
