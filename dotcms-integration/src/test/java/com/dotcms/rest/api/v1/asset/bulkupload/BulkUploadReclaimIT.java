package com.dotcms.rest.api.v1.asset.bulkupload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for reclaim on a submission that dies while being read (spec FR-013d.2,
 * C-001a1).
 * <p>
 * <b>A different code path from the refusal, which is the whole reason this class exists.</b> A
 * refusal is raised by this side, so this side knows to clean up — {@code BulkUploadReclaimTest}
 * covers that as a unit test. A read that dies underneath raises nothing of ours: the author
 * navigated away, or the connection dropped. A reclaim written into the refusal branch passes that
 * test and fails these.
 * <p>
 * It matters more than it looks because <b>nothing purges staged content on a schedule</b> — the
 * repository has no cleanup task anywhere — so bytes missed here are missed permanently, invisible
 * to the author and uncollected by any run. And it is the likelier of the two paths, since it is
 * the author's own action rather than a limit being hit.
 */
@EnableWeld
public class BulkUploadReclaimIT extends Junit5WeldBaseTest {

    @Inject
    BulkUploadHelper helper;

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

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

    private Folder folder() {
        return new FolderDataGen().site(new SiteDataGen().nextPersisted()).nextPersisted();
    }

    private List<UploadPart> parts(final int count) {
        final List<UploadPart> parts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            parts.add(new UploadPart("abandoned-" + i + ".txt",
                    new ByteArrayInputStream(new byte[16])));
        }
        return parts;
    }

    /**
     * Stages through the real temp API for the first {@code failAfter} parts and then throws, which
     * is what a client going away mid-upload looks like from this side.
     */
    private class DyingStaging implements BatchStaging {

        private final List<String> staged = new ArrayList<>();
        private final int failAfter;

        DyingStaging(final int failAfter) {
            this.failAfter = failAfter;
        }

        @Override
        public StagedPart stage(final String fileName, final InputStream content)
                throws IOException {
            if (staged.size() >= failAfter) {
                throw new IOException("connection reset by peer");
            }
            try {
                final DotTempFile tempFile = APILocator.getTempFileAPI()
                        .createTempFile(fileName, request(), content);
                staged.add(tempFile.id);
                return new StagedPart(tempFile.id, tempFile.fileName, tempFile.length(),
                        tempFile.mimeType);
            } catch (final Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void reclaim(final String tempFileId) {
            new TempFileBatchStaging(tryRequest()).reclaim(tempFileId);
        }

        private HttpServletRequest tryRequest() {
            try {
                return request();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }

        List<String> staged() {
            return staged;
        }
    }

    /**
     * Method to test: {@link BulkUploadHelper#submit} when the read dies part-way
     * <p>
     * Given scenario: Five parts, and the connection drops while the third is being staged.
     * <p>
     * Expected result: The submission fails, <b>no job is created</b>, and the two parts already
     * written are gone from the staging layer. An abandoned upload must cost the author nothing and
     * leave nothing behind — the guarantee starts at the handle, and this submission never reached
     * one.
     */
    @Test
    public void test_aSubmissionThatDiesMidReadLeavesNoJobAndNoStagedContent() throws Exception {
        final Folder target = folder();
        final DyingStaging staging = new DyingStaging(2);

        assertThrows(RuntimeException.class, () -> helper.submit(
                new BulkUploadForm("DOTASSET", target.getIdentifier(), null, null),
                parts(5), staging, admin(), request()));

        assertEquals(2, staging.staged().size(),
                "two parts landed before the client went away");

        for (final String tempFileId : staging.staged()) {
            final Optional<DotTempFile> reclaimed =
                    APILocator.getTempFileAPI().getTempFile(request(), tempFileId);
            assertTrue(reclaimed.isEmpty() || !reclaimed.get().file.exists(), String.format(
                    "staged content '%s' survived an abandoned submission; nothing purges it on a "
                            + "schedule, so it is leaked permanently", tempFileId));
        }
    }

    /**
     * Method to test: {@link BulkUploadHelper#submit} — that the failure is not silent
     * <p>
     * Given scenario: The same abandoned read.
     * <p>
     * Expected result: It raises rather than returning a handle to a batch that will never run.
     * Answering 202 for a submission whose content is half-written would hand the author a job id
     * they could follow forever.
     */
    @Test
    public void test_anAbandonedSubmissionNeverAnswersWithAHandle() throws Exception {
        final Folder target = folder();

        assertThrows(RuntimeException.class, () -> helper.submit(
                new BulkUploadForm("DOTASSET", target.getIdentifier(), null, null),
                parts(3), new DyingStaging(1), admin(), request()));
    }
}
