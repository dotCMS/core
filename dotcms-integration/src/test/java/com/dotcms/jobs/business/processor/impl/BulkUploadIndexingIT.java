package com.dotcms.jobs.business.processor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
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
 * Integration tests for when a batch's files become findable (spec FR-008a, C-006a).
 * <p>
 * <b>An ordering the client depends on and cannot see.</b> The frontend refreshes its listing when
 * the completion signal arrives, so if the signal were emitted before the batch resolved index
 * visibility, the refresh would miss files — and it would surface as a client-side defect for a
 * cause living entirely on the server.
 * <p>
 * Per ADR-0018 the plain folder listing is resolved from the database, so the grid is fine either
 * way. What needs this is the index-routed half: free text and searchable fields. The ADR names
 * that exact failure — <i>"I saved it and it vanished from the browser"</i> — as a recurring,
 * hard-to-reproduce complaint, so a run reporting finished while its files are unsearchable would
 * reproduce the thing the ADR exists to prevent.
 */
@EnableWeld
public class BulkUploadIndexingIT extends Junit5WeldBaseTest {

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

    private Job jobFor(final Folder folder, final int count) throws Exception {
        final HttpServletRequest request = request();
        final List<Map<String, Object>> stagedFiles = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            final String fileName = "indexed-" + UUID.randomUUID() + ".txt";
            final DotTempFile tempFile = APILocator.getTempFileAPI().createTempFile(
                    fileName, request, new ByteArrayInputStream(("body " + i).getBytes()));

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
     * Method to test: {@link BulkUploadProcessor#process} — index visibility on return
     * <p>
     * Given scenario: A batch of five files completes.
     * <p>
     * Expected result: The created assets are in the index <b>by the time {@code process} returns</b>,
     * with no wait added by the test. That is the assertion that pins the ordering: the completion
     * event fires after {@code process} returns, so a batch findable at this instant is a batch
     * findable before the client is ever told (C-006a). Sleeping first would prove nothing — it
     * would pass just as well if the resolution happened afterwards.
     */
    @Test
    public void test_run_resolvesIndexVisibilityBeforeItReturns() throws Exception {
        final Folder folder = new FolderDataGen().site(new SiteDataGen().nextPersisted())
                .nextPersisted();
        final Job job = jobFor(folder, 5);

        new BulkUploadProcessor().process(job);

        final List<Contentlet> created =
                APILocator.getFolderAPI().getWorkingContent(folder, admin(), false);
        assertEquals(5, created.size());

        for (final Contentlet asset : created) {
            final long indexed = APILocator.getContentletAPI()
                    .indexCount("+identifier:" + asset.getIdentifier(), admin(), false);
            assertTrue(indexed > 0, String.format(
                    "asset %s is in the folder but not in the index at the moment the run "
                            + "returned; the completion signal fires after this, so the client "
                            + "would refresh and miss it under a text filter",
                    asset.getIdentifier()));
        }
    }

    /**
     * Method to test: {@link BulkUploadProcessor#process} — that the batch does not serialise
     * <p>
     * Given scenario: Five files in one run.
     * <p>
     * Expected result: The run completes well inside the floor a per-file wait would impose.
     * {@code WAIT_FOR} blocks on the next index refresh for every file — the default interval is
     * one second and this repository does not override it — and it also flushes the system-wide
     * query cache each time, so five files would cost roughly five seconds and five flushes
     * charged to every other user. Generous headroom on purpose: this guards an order-of-magnitude
     * regression, not jitter, because a tight threshold in CI is a flaky test rather than a useful
     * one.
     */
    @Test
    public void test_run_doesNotPayAPerFileIndexWait() throws Exception {
        final Folder folder = new FolderDataGen().site(new SiteDataGen().nextPersisted())
                .nextPersisted();
        final Job job = jobFor(folder, 5);

        final long startedAt = System.currentTimeMillis();
        new BulkUploadProcessor().process(job);
        final long elapsed = System.currentTimeMillis() - startedAt;

        assertTrue(elapsed < 5_000L, String.format(
                "a 5-file batch took %dms, which is the shape of a per-file index wait rather than "
                        + "one batch-level resolution", elapsed));
    }
}
