package com.dotcms.rest.api.v1.asset.bulkdelete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@code POST /api/v1/assets/folders/_bulkdelete} — the submission itself
 * (#37063, spec FR-001, FR-002, FR-003, FR-004; contracts §1).
 * <p>
 * Drives {@link FolderBulkDeleteHelper} directly — same convention {@code BulkUploadResourceIT}
 * documents: injected via CDI (never {@code new}, which passes even when the real deployment would
 * fail Weld's no-args-constructor requirement for a proxyable {@code @ApplicationScoped} bean), so
 * these tests notice a broken deployment. <b>This does not exercise the exception-mapper layer</b> —
 * {@code BulkUploadRefusedExceptionMapper}'s own history records that helper-level tests alone
 * missed a real refusal-status bug that only the Postman collection caught. T018 is where this
 * feature's mapper wiring is actually verified over the wire.
 */
@EnableWeld
public class FolderBulkDeleteResourceIT extends Junit5WeldBaseTest {

    @Inject
    FolderBulkDeleteHelper helper;

    @Inject
    JobQueueManagerAPI jobQueueManagerAPI;

    private static User admin;
    private static Host site;

    @BeforeAll
    public static void prepare() throws Exception {
        com.dotcms.util.IntegrationTestInitService.getInstance().init();
        admin = APILocator.systemUser();
        site = new SiteDataGen().nextPersisted();
    }

    private Folder folder() {
        return new FolderDataGen().site(site).nextPersisted();
    }

    private String pathOf(final Folder folder) {
        return String.format("//%s/%s/", site.getHostname(), folder.getName());
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: A single accepted path
     * ExpectedResult: `202`-equivalent handle carrying jobId, a ready-to-use statusUrl, and
     * submitted == 1 — a single-path submission runs as a batch of one, not routed to the
     * synchronous endpoint (FR-001, FR-002, FR-003, US1 scenario 5)
     */
    @Test
    public void test_submit_singlePath_returnsHandle_batchOfOne() throws Exception {

        final String path = pathOf(folder());
        final FolderBulkDeleteForm form = FolderBulkDeleteForm.builder()
                .assetPaths(List.of(path))
                .build();

        final AbstractFolderBulkDeleteSubmitResponse response = helper.submit(form, admin);

        assertNotNull(response.jobId());
        assertEquals("/api/v1/jobs/" + response.jobId() + "/status", response.statusUrl());
        assertEquals(1, response.submitted());
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: Several distinct accepted paths
     * ExpectedResult: submitted equals the number of distinct paths — this is the number the
     * final outcome's `total` must also equal, by construction (FR-003, C-003)
     */
    @Test
    public void test_submit_severalPaths_submittedMatchesDistinctPaths() throws Exception {

        final List<String> paths = List.of(pathOf(folder()), pathOf(folder()), pathOf(folder()));
        final FolderBulkDeleteForm form = FolderBulkDeleteForm.builder()
                .assetPaths(paths)
                .build();

        final AbstractFolderBulkDeleteSubmitResponse response = helper.submit(form, admin);

        assertEquals(3, response.submitted());
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: An empty selection
     * ExpectedResult: Refused with FolderBulkDeleteRefusedException, errorCode EMPTY_SELECTION,
     * fieldName "assetPaths" — before any job is created (FR-004, contracts §1)
     */
    @Test
    public void test_submit_emptySelection_refusedWithEmptySelectionCode() {

        final FolderBulkDeleteForm form = FolderBulkDeleteForm.builder().build();

        final FolderBulkDeleteRefusedException ex = assertThrows(
                FolderBulkDeleteRefusedException.class, () -> helper.submit(form, admin));

        assertEquals("EMPTY_SELECTION", ex.errorCode());
        assertEquals("assetPaths", ex.fieldName());
        assertEquals(Response.Status.BAD_REQUEST, ex.status());
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: More paths than the configured maximum (default 50, contracts §5)
     * ExpectedResult: Refused with errorCode OVER_MAX_PATHS, distinguishable from an empty
     * selection despite both being 400s (the CR-02 gap raised on the issue and resolved in
     * contracts §1)
     */
    @Test
    public void test_submit_overMaxPaths_refusedWithOverMaxPathsCode() {

        final int max = Config.getIntProperty(
                FolderBulkDeleteHelper.MAX_PATHS_KEY, FolderBulkDeleteHelper.DEFAULT_MAX_PATHS);
        final List<String> tooMany = java.util.stream.IntStream.rangeClosed(0, max)
                .mapToObj(i -> "//" + site.getHostname() + "/not-real-" + i + "/")
                .toList();
        final FolderBulkDeleteForm form = FolderBulkDeleteForm.builder()
                .assetPaths(tooMany)
                .build();

        final FolderBulkDeleteRefusedException ex = assertThrows(
                FolderBulkDeleteRefusedException.class, () -> helper.submit(form, admin));

        assertEquals("OVER_MAX_PATHS", ex.errorCode());
        assertEquals("assetPaths", ex.fieldName());
        assertTrue(ex.getMessage().contains(String.valueOf(max)),
                "message should name the configured ceiling: " + ex.getMessage());
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: The same path submitted twice in one request
     * ExpectedResult: Deduplicated before the run — submitted is 1, not 2 (FR-012)
     */
    @Test
    public void test_submit_duplicatePath_collapsedBeforeTheRun() throws Exception {

        final String path = pathOf(folder());
        final FolderBulkDeleteForm form = FolderBulkDeleteForm.builder()
                .assetPaths(List.of(path, path))
                .build();

        final AbstractFolderBulkDeleteSubmitResponse response = helper.submit(form, admin);

        assertEquals(1, response.submitted());
    }

    /**
     * Method to test: {@link FolderBulkDeleteHelper#submit(FolderBulkDeleteForm, User)}
     * Given Scenario: The same folder submitted twice, once with a trailing slash and once
     * without — the same path as far as any folder path is concerned, but two different strings
     * ExpectedResult: Still collapsed to one — submitted is 1, and the single stored path is the
     * normalized (trailing-slash) form, matching what the processor's own dedup logic already
     * normalizes to (#37685 review — previously both survived, and the second was reported
     * PATH_NOT_FOUND against a folder the first had just deleted)
     */
    @Test
    public void test_submit_samePathDifferingOnlyByTrailingSlash_collapsedBeforeTheRun()
            throws Exception {

        final String pathWithSlash = pathOf(folder());
        final String pathWithoutSlash =
                pathWithSlash.substring(0, pathWithSlash.length() - 1);
        final FolderBulkDeleteForm form = FolderBulkDeleteForm.builder()
                .assetPaths(List.of(pathWithSlash, pathWithoutSlash))
                .build();

        final AbstractFolderBulkDeleteSubmitResponse response = helper.submit(form, admin);

        assertEquals(1, response.submitted());

        final List<String> storedPaths = FolderBulkDeleteHelper.pathsOf(
                jobQueueManagerAPI.getJob(response.jobId()).parameters());
        assertEquals(1, storedPaths.size(),
                "only one path must reach the job's own parameters — found: " + storedPaths);
        assertEquals(pathWithSlash, storedPaths.getFirst(),
                "the stored path must be the normalized (trailing-slash) form");
    }
}
