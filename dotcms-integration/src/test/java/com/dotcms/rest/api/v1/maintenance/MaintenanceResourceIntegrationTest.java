package com.dotcms.rest.api.v1.maintenance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityJobStatusView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.job.JobStatusResponse;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Integration tests for the maintenance tools REST endpoints in {@link MaintenanceResource}.
 *
 * @author hassandotcms
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MaintenanceResourceIntegrationTest extends IntegrationTestBase {

    private static MaintenanceResource resource;
    private static HttpServletResponse mockResponse;
    private static User adminUser;
    private static User nonAdminUser;
    private static JobQueueManagerAPI jobQueueManagerAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new MaintenanceResource();
        mockResponse = new MockHttpResponse().response();
        adminUser = TestUserUtils.getAdminUser();

        nonAdminUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), nonAdminUser);

        jobQueueManagerAPI = APILocator.getJobQueueManagerAPI();
        if (!jobQueueManagerAPI.isStarted()) {
            jobQueueManagerAPI.start();
            jobQueueManagerAPI.awaitStart(10, TimeUnit.SECONDS);
        }
    }

    // ==================== POST /_searchAndReplace ====================

    @Test
    public void test_searchAndReplace_asAdmin_succeeds() {
        final HttpServletRequest request = createAdminRequest();

        final String marker = "MAINT_TEST_MARKER_" + System.currentTimeMillis();
        final String replacement = "MAINT_TEST_REPLACED_" + System.currentTimeMillis();

        final SearchAndReplaceForm form = new SearchAndReplaceForm(marker, replacement);
        final ResponseEntitySearchAndReplaceResultView result =
                resource.searchAndReplace(request, mockResponse, form);

        assertNotNull(result);
        final SearchAndReplaceResultView view = result.getEntity();
        assertNotNull(view);
        assertTrue("success should be true", view.success());
        assertFalse("hasErrors should be false", view.hasErrors());
    }

    @Test(expected = SecurityException.class)
    public void test_searchAndReplace_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        final SearchAndReplaceForm form = new SearchAndReplaceForm("search", "replace");
        resource.searchAndReplace(request, mockResponse, form);
    }

    @Test(expected = BadRequestException.class)
    public void test_searchAndReplace_nullForm_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        resource.searchAndReplace(request, mockResponse, null);
    }

    @Test(expected = BadRequestException.class)
    public void test_searchAndReplace_emptySearchString_throwsBadRequest() {
        new SearchAndReplaceForm("", "replace");
    }

    @Test(expected = ValidationException.class)
    public void test_searchAndReplace_nullSearchString_throwsValidation() {
        new SearchAndReplaceForm(null, "replace");
    }

    @Test
    public void test_searchAndReplace_emptyReplaceString_isValid() {
        final HttpServletRequest request = createAdminRequest();
        final String marker = "MAINT_TEST_DELETE_" + System.currentTimeMillis();

        final SearchAndReplaceForm form = new SearchAndReplaceForm(marker, "");
        final ResponseEntitySearchAndReplaceResultView result =
                resource.searchAndReplace(request, mockResponse, form);

        assertNotNull(result);
        assertTrue(result.getEntity().success());
    }

    // ==================== DELETE /_oldVersions ====================

    @Test
    public void test_dropOldVersions_asAdmin_succeeds() {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityDropOldVersionsResultView result =
                resource.dropOldVersions(request, mockResponse, "2000-01-01");

        assertNotNull(result);
        final DropOldVersionsResultView view = result.getEntity();
        assertNotNull(view);
        assertTrue("success should be true (deletedCount >= 0)", view.success());
        assertTrue("deletedCount should be >= 0", view.deletedCount() >= 0);
    }

    @Test(expected = SecurityException.class)
    public void test_dropOldVersions_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.dropOldVersions(request, mockResponse, "2000-01-01");
    }

    @Test(expected = BadRequestException.class)
    public void test_dropOldVersions_missingDate_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        resource.dropOldVersions(request, mockResponse, null);
    }

    @Test(expected = BadRequestException.class)
    public void test_dropOldVersions_invalidDateFormat_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        resource.dropOldVersions(request, mockResponse, "01/01/2000");
    }

    @Test(expected = BadRequestException.class)
    public void test_dropOldVersions_garbageDate_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        resource.dropOldVersions(request, mockResponse, "not-a-date");
    }

    // ==================== DELETE /_pushedAssets ====================

    @Test
    public void test_deletePushedAssets_asAdmin_succeeds() {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityStringView result =
                resource.deletePushedAssets(request, mockResponse);

        assertNotNull(result);
        assertEquals("success", result.getEntity());
    }

    @Test(expected = SecurityException.class)
    public void test_deletePushedAssets_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.deletePushedAssets(request, mockResponse);
    }

    // ==================== POST /assets/_fix ====================

    /**
     * Given scenario: an admin user requests a fix-assets job
     * Expected result: a job is enqueued, reaches SUCCESS, and its metadata records
     *                  {@code tasksRun} and a {@code results} list
     */
    @Test
    public void test_requestFixAssetsJob_asAdmin_enqueuesAndCompletes() throws Exception {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityJobStatusView view =
                resource.requestFixAssetsJob(request, mockResponse);

        assertNotNull(view);
        final JobStatusResponse status = view.getEntity();
        assertNotNull(status);
        assertNotNull("jobId must be present", status.jobId());
        assertTrue("statusUrl must point to the generic jobs endpoint",
                status.statusUrl().contains("/api/v1/jobs/" + status.jobId() + "/status"));

        final Job completed = awaitJobCompletion(status.jobId(), 60);
        assertEquals("Fix-assets job should complete successfully",
                JobState.SUCCESS, completed.state());

        final Map<String, Object> metadata = extractMetadata(completed);
        assertTrue("fix-assets metadata should record tasksRun",
                metadata.containsKey("tasksRun"));
        assertTrue("tasksRun must be a non-negative integer",
                ((Number) metadata.get("tasksRun")).intValue() >= 0);
        assertTrue("fix-assets metadata should include a results list",
                metadata.get("results") instanceof List);
    }

    /**
     * Given scenario: a non-admin user requests a fix-assets job
     * Expected result: SecurityException is thrown
     */
    @Test(expected = SecurityException.class)
    public void test_requestFixAssetsJob_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.requestFixAssetsJob(request, mockResponse);
    }

    /**
     * Given scenario: a fix-assets job is already active and another POST arrives
     * Expected result: ConflictException is thrown for the second request
     */
    @Test(expected = ConflictException.class)
    public void test_requestFixAssetsJob_whileActive_throwsConflict() throws Exception {
        final HttpServletRequest request = createAdminRequest();
        final ResponseEntityJobStatusView first =
                resource.requestFixAssetsJob(request, mockResponse);
        try {
            resource.requestFixAssetsJob(request, mockResponse);
        } finally {
            awaitJobCompletion(first.getEntity().jobId(), 60);
        }
    }

    // ==================== GET /assets/_fix ====================

    /**
     * Given scenario: a fix-assets job has completed, admin queries the latest
     * Expected result: the most recently completed job is returned
     */
    @Test
    public void test_getLatestFixAssetsJob_asAdmin_returnsJob() throws Exception {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityJobStatusView created =
                resource.requestFixAssetsJob(request, mockResponse);
        awaitJobCompletion(created.getEntity().jobId(), 60);

        final ResponseEntityView<Job> latest =
                resource.getLatestFixAssetsJob(request, mockResponse);
        assertNotNull(latest);
        assertNotNull("A completed job should be returned", latest.getEntity());
        assertEquals(created.getEntity().jobId(), latest.getEntity().id());
    }

    /**
     * Given scenario: a non-admin user queries the latest fix-assets job
     * Expected result: SecurityException is thrown
     */
    @Test(expected = SecurityException.class)
    public void test_getLatestFixAssetsJob_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.getLatestFixAssetsJob(request, mockResponse);
    }

    // ==================== POST /assets/_clean ====================

    /**
     * Given scenario: an admin user requests a clean-assets job
     * Expected result: a job is enqueued, reaches SUCCESS, and its metadata records
     *                  {@code finalStatus=Finished} plus non-negative {@code totalFiles}
     *                  and {@code deleted} counters
     */
    @Test
    public void test_requestCleanAssetsJob_asAdmin_enqueuesAndCompletes() throws Exception {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityJobStatusView view =
                resource.requestCleanAssetsJob(request, mockResponse);

        assertNotNull(view);
        final JobStatusResponse status = view.getEntity();
        assertNotNull(status);
        assertNotNull("jobId must be present", status.jobId());

        final Job completed = awaitJobCompletion(status.jobId(), 120);
        assertEquals("Clean-assets job should complete successfully",
                JobState.SUCCESS, completed.state());

        final Map<String, Object> metadata = extractMetadata(completed);
        assertEquals("Clean-assets must mark the process as Finished",
                "Finished", metadata.get("finalStatus"));
        assertTrue("totalFiles counter must be present and non-negative",
                ((Number) metadata.get("totalFiles")).intValue() >= 0);
        assertTrue("deleted counter must be present and non-negative",
                ((Number) metadata.get("deleted")).intValue() >= 0);
    }

    /**
     * Given scenario: a UUID-named directory is planted under the assets root and
     *                 clean-assets runs (the UUID cannot match any real contentlet inode)
     * Expected result: the planted directory is deleted and the {@code deleted} counter
     *                  in the job metadata is at least 1
     */
    @Test
    public void test_cleanAssets_deletesPlantedOrphanDirectory() throws Exception {
        final String assetsRoot = APILocator.getFileAssetAPI().getRealAssetsRootPath();
        assertNotNull("assets root path must be resolvable", assetsRoot);

        final String fakeInode = UUIDGenerator.generateUuid();
        final File hexLevel1 = new File(assetsRoot, String.valueOf(fakeInode.charAt(0)));
        final File hexLevel2 = new File(hexLevel1, String.valueOf(fakeInode.charAt(1)));
        final File orphanDir = new File(hexLevel2, fakeInode);

        assertTrue(orphanDir.mkdirs());
        Files.write(new File(orphanDir, "orphan.bin").toPath(), new byte[]{0, 1, 2, 3});
        assertTrue(orphanDir.isDirectory());

        try {
            final HttpServletRequest request = createAdminRequest();
            final ResponseEntityJobStatusView view =
                    resource.requestCleanAssetsJob(request, mockResponse);
            final Job completed = awaitJobCompletion(view.getEntity().jobId(), 120);

            assertEquals(JobState.SUCCESS, completed.state());
            assertFalse("Planted orphan directory must be deleted by clean-assets",
                    orphanDir.exists());

            final Map<String, Object> metadata = extractMetadata(completed);
            assertTrue("deleted counter should reflect at least our planted orphan",
                    ((Number) metadata.get("deleted")).intValue() >= 1);
        } finally {
            if (orphanDir.exists()) {
                FileUtils.deleteQuietly(orphanDir);
            }
        }
    }

    /**
     * Given scenario: under the assets root we plant (1) an orphan UUID-named directory,
     *                 (2) a {@code .donotdelete.dat} marker file, (3) a regular
     *                 non-directory file, (4) a directory whose name matches a real
     *                 existing contentlet's inode (the default Host)
     * Expected result: only the orphan directory is deleted; the marker file, regular
     *                  file, and real contentlet's directory all survive the walk
     */
    @Test
    public void test_cleanAssets_preservesNonOrphanEntries() throws Exception {
        final String assetsRoot = APILocator.getFileAssetAPI().getRealAssetsRootPath();

        final String fakeInode = UUIDGenerator.generateUuid();
        final File orphanHexLevel1 = new File(assetsRoot, String.valueOf(fakeInode.charAt(0)));
        final File orphanHexLevel2 = new File(orphanHexLevel1, String.valueOf(fakeInode.charAt(1)));
        final File orphanDir = new File(orphanHexLevel2, fakeInode);
        assertTrue(orphanDir.mkdirs());
        Files.write(new File(orphanDir, "orphan.bin").toPath(), new byte[]{0, 1});

        final File markerFile = new File(orphanHexLevel2, ".donotdelete.dat");
        final boolean markerCreatedByUs = !markerFile.exists();
        if (markerCreatedByUs) {
            Files.write(markerFile.toPath(), new byte[]{0});
        }

        final File regularFile = new File(orphanHexLevel2,
                "preserve-" + UUIDGenerator.generateUuid() + ".txt");
        Files.write(regularFile.toPath(), new byte[]{0});

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(adminUser, false);
        final String realInode = defaultHost.getInode();
        assertNotNull(realInode);
        final File realHexLevel1 = new File(assetsRoot, String.valueOf(realInode.charAt(0)));
        final File realHexLevel2 = new File(realHexLevel1, String.valueOf(realInode.charAt(1)));
        final File realInodeDir = new File(realHexLevel2, realInode);
        final boolean realInodeCreatedByUs = !realInodeDir.exists();
        if (realInodeCreatedByUs) {
            assertTrue(realInodeDir.mkdirs());
        }

        try {
            final HttpServletRequest request = createAdminRequest();
            final ResponseEntityJobStatusView view =
                    resource.requestCleanAssetsJob(request, mockResponse);
            final Job completed = awaitJobCompletion(view.getEntity().jobId(), 120);

            assertEquals(JobState.SUCCESS, completed.state());
            assertFalse("orphan UUID directory should be deleted by clean-assets",
                    orphanDir.exists());
            assertTrue(".donotdelete.dat marker must be preserved",
                    markerFile.exists());
            assertTrue("regular non-directory file must be preserved",
                    regularFile.exists());
            assertTrue("real contentlet's binary directory must be preserved",
                    realInodeDir.exists());
        } finally {
            FileUtils.deleteQuietly(orphanDir);
            if (markerCreatedByUs) {
                FileUtils.deleteQuietly(markerFile);
            }
            FileUtils.deleteQuietly(regularFile);
            if (realInodeCreatedByUs) {
                FileUtils.deleteQuietly(realInodeDir);
            }
        }
    }

    /**
     * Given scenario: a non-admin user requests a clean-assets job
     * Expected result: SecurityException is thrown
     */
    @Test(expected = SecurityException.class)
    public void test_requestCleanAssetsJob_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.requestCleanAssetsJob(request, mockResponse);
    }

    /**
     * Given scenario: a clean-assets job is already active and another POST arrives
     * Expected result: ConflictException is thrown for the second request
     */
    @Test(expected = ConflictException.class)
    public void test_requestCleanAssetsJob_whileActive_throwsConflict() throws Exception {
        final HttpServletRequest request = createAdminRequest();
        final ResponseEntityJobStatusView first =
                resource.requestCleanAssetsJob(request, mockResponse);
        try {
            resource.requestCleanAssetsJob(request, mockResponse);
        } finally {
            awaitJobCompletion(first.getEntity().jobId(), 120);
        }
    }

    // ==================== GET /assets/_clean ====================

    /**
     * Given scenario: a clean-assets job has completed, admin queries the latest
     * Expected result: the most recently completed job is returned
     */
    @Test
    public void test_getLatestCleanAssetsJob_asAdmin_returnsJob() throws Exception {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityJobStatusView created =
                resource.requestCleanAssetsJob(request, mockResponse);
        awaitJobCompletion(created.getEntity().jobId(), 120);

        final ResponseEntityView<Job> latest =
                resource.getLatestCleanAssetsJob(request, mockResponse);
        assertNotNull(latest);
        assertNotNull("A completed job should be returned", latest.getEntity());
        assertEquals(created.getEntity().jobId(), latest.getEntity().id());
    }

    /**
     * Given scenario: a non-admin user queries the latest clean-assets job
     * Expected result: SecurityException is thrown
     */
    @Test(expected = SecurityException.class)
    public void test_getLatestCleanAssetsJob_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.getLatestCleanAssetsJob(request, mockResponse);
    }

    // ==================== Helpers ====================

    private HttpServletRequest createAdminRequest() {
        return createRequestForUser(adminUser);
    }

    private static HttpServletRequest createRequestForUser(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()
        ).request();

        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    private static Job awaitJobCompletion(final String jobId, final int timeoutSeconds)
            throws Exception {
        final long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        Job job = null;
        while (System.currentTimeMillis() < deadline) {
            job = jobQueueManagerAPI.getJob(jobId);
            if (job != null && isTerminal(job.state())) {
                return job;
            }
            Thread.sleep(500);
        }
        throw new AssertionError(
                "Job " + jobId + " did not complete within " + timeoutSeconds + "s; "
                        + "last state=" + (job == null ? "null" : job.state()));
    }

    private static boolean isTerminal(final JobState state) {
        return state == JobState.SUCCESS
                || state == JobState.FAILED
                || state == JobState.CANCELED
                || state == JobState.ABANDONED_PERMANENTLY;
    }

    private static Map<String, Object> extractMetadata(final Job job) {
        final Optional<JobResult> result = job.result();
        assertTrue("Completed job must carry a JobResult", result.isPresent());
        final Optional<Map<String, Object>> metadata = result.get().metadata();
        assertTrue("JobResult must carry a metadata map", metadata.isPresent());
        return metadata.get();
    }
}
