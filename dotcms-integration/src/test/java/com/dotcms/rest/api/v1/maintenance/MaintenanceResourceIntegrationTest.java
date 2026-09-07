package com.dotcms.rest.api.v1.maintenance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.cmsmaintenance.ajax.UserSessionAjax;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.listeners.SessionMonitor;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSession;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityJobStatusView;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.job.JobStatusResponse;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.StatefulJob;
import org.quartz.Trigger;

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

    /** Session ids planted in {@link SessionMonitor} by a test; cleaned up in {@link #cleanPlantedSessions()}. */
    private final Set<String> plantedSessionIds = new HashSet<>();

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

    // ==================== GET /_threads ====================

    @Test
    public void test_getThreadDump_asAdmin_succeeds() {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityThreadDumpView result =
                resource.getThreadDump(request, mockResponse, true);

        assertNotNull(result);
        final ThreadDumpView view = result.getEntity();
        assertNotNull(view);
        assertNotNull(view.timestamp());
        assertTrue("vmInfo should be populated", view.vmInfo() != null && !view.vmInfo().isEmpty());
        assertTrue("threads list should be non-empty", !view.threads().isEmpty());
        assertEquals("threadCount should match threads list size",
                view.threads().size(), view.threadCount());
        assertTrue("deadlockedCount should be >= 0", view.deadlockedCount() >= 0);
    }

    @Test
    public void test_getThreadDump_hideSystemFalse_returnsAllThreads() {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityThreadDumpView filtered =
                resource.getThreadDump(request, mockResponse, true);
        final ResponseEntityThreadDumpView all =
                resource.getThreadDump(request, mockResponse, false);

        assertTrue("hideSystem=false should return >= threads vs hideSystem=true",
                all.getEntity().threadCount() >= filtered.getEntity().threadCount());
    }

    @Test(expected = SecurityException.class)
    public void test_getThreadDump_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.getThreadDump(request, mockResponse, true);
    }

    // ==================== GET /_threads/info ====================

    @Test
    public void test_getThreadInfo_asAdmin_succeeds() {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityThreadSystemInfoView result =
                resource.getThreadInfo(request, mockResponse);

        assertNotNull(result);
        final ThreadSystemInfoView view = result.getEntity();
        assertNotNull(view);
        assertTrue("systemStartupTime should be populated",
                view.systemStartupTime() != null && !view.systemStartupTime().isEmpty());
        assertTrue("startTimeMillis should be > 0", view.startTimeMillis() > 0);
        assertTrue("uptimeMillis should be >= 0", view.uptimeMillis() >= 0);
        assertTrue("currentThreadCount should be > 0", view.currentThreadCount() > 0);
        assertTrue("peakThreadCount should be >= currentThreadCount",
                view.peakThreadCount() >= view.currentThreadCount());
    }

    @Test(expected = SecurityException.class)
    public void test_getThreadInfo_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.getThreadInfo(request, mockResponse);
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

    // ==================== DELETE /_contentlets ====================

    @Test
    public void test_deleteContentlets_asAdmin_destroysContentlet() throws Exception {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet contentlet = new ContentletDataGen(contentType.id()).nextPersisted();
        final String identifier = contentlet.getIdentifier();

        final HttpServletRequest request = createAdminRequest();
        final DeleteContentletsForm form =
                new DeleteContentletsForm(Collections.singletonList(identifier));

        final ResponseEntityDeleteContentletsResultView result =
                resource.deleteContentlets(request, mockResponse, form);

        assertNotNull(result);
        final DeleteContentletsResultView view = result.getEntity();
        assertNotNull(view);
        assertTrue("At least one contentlet should be deleted", view.deleted() > 0);
        assertTrue("errors should be empty", view.errors().isEmpty());

        final List<Contentlet> siblings =
                APILocator.getContentletAPI().getSiblings(identifier);
        assertTrue("Contentlet should no longer exist", siblings.isEmpty());
    }

    @Test
    public void test_deleteContentlets_nonExistentIdentifier_returnsZeroDeleted() {
        final HttpServletRequest request = createAdminRequest();
        final DeleteContentletsForm form = new DeleteContentletsForm(
                Collections.singletonList("non-existent-identifier-" + System.currentTimeMillis()));

        final ResponseEntityDeleteContentletsResultView result =
                resource.deleteContentlets(request, mockResponse, form);

        assertNotNull(result);
        final DeleteContentletsResultView view = result.getEntity();
        assertEquals(0, view.deleted());
        assertTrue(view.errors().isEmpty());
    }

    @Test
    public void test_deleteContentlets_multipleIdentifiers_deletesAll() throws Exception {
        final ContentType contentType = new ContentTypeDataGen().nextPersisted();
        final Contentlet c1 = new ContentletDataGen(contentType.id()).nextPersisted();
        final Contentlet c2 = new ContentletDataGen(contentType.id()).nextPersisted();

        final HttpServletRequest request = createAdminRequest();
        final DeleteContentletsForm form = new DeleteContentletsForm(
                Arrays.asList(c1.getIdentifier(), c2.getIdentifier()));

        final ResponseEntityDeleteContentletsResultView result =
                resource.deleteContentlets(request, mockResponse, form);

        assertNotNull(result);
        final DeleteContentletsResultView view = result.getEntity();
        assertTrue("Both contentlets should be deleted", view.deleted() >= 2);
        assertTrue(view.errors().isEmpty());
    }

    @Test(expected = SecurityException.class)
    public void test_deleteContentlets_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        final DeleteContentletsForm form = new DeleteContentletsForm(
                Collections.singletonList("some-id"));
        resource.deleteContentlets(request, mockResponse, form);
    }

    @Test(expected = BadRequestException.class)
    public void test_deleteContentlets_nullForm_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        resource.deleteContentlets(request, mockResponse, null);
    }

    @Test(expected = BadRequestException.class)
    public void test_deleteContentlets_emptyIdentifiers_throwsBadRequest() {
        new DeleteContentletsForm(Collections.emptyList());
    }

    @Test(expected = BadRequestException.class)
    public void test_deleteContentlets_nullIdentifiers_throwsBadRequest() {
        new DeleteContentletsForm(null);
    }

    // ==================== GET /_sessions ====================

    /**
     * Given scenario: a second admin session is planted in SessionMonitor and the caller's
     *                 session is also registered there
     * Expected result: both sessions are listed; the caller's entry has isCurrent=true,
     *                  the planted entry has isCurrent=false, every token is the 22-char
     *                  HMAC form (never the raw session id), and a fresh CSRF secret is
     *                  stored on the caller's session
     */
    @Test
    public void test_listSessions_asAdmin_returnsPlantedAndCallerSessions() {
        final HttpServletRequest request = createAdminRequest();
        final HttpSession callerSession = request.getSession();
        callerSession.setAttribute(WebKeys.USER_ID, adminUser.getUserId());
        plantSession(callerSession);

        final HttpSession otherSession = newPlantedAdminSession("9.9.9.9");

        final ResponseEntitySessionListView result = resource.listSessions(request, mockResponse);

        assertNotNull(result);
        final List<SessionView> sessions = result.getEntity();
        assertNotNull(sessions);

        SessionView callerView = null;
        SessionView otherView = null;
        for (final SessionView view : sessions) {
            assertEquals("HMAC tokens are exactly 22 chars (16 bytes URL Base64 no padding)",
                    22, view.token().length());
            assertNotEquals("Raw session id must never leak in the token",
                    callerSession.getId(), view.token());
            assertNotEquals("Raw session id must never leak in the token",
                    otherSession.getId(), view.token());

            if (view.userId().equals(adminUser.getUserId())
                    && view.isCurrent()) {
                callerView = view;
            } else if (view.userId().equals(adminUser.getUserId())
                    && "9.9.9.9".equals(view.address())) {
                otherView = view;
            }
        }
        assertNotNull("The caller's session must appear with isCurrent=true", callerView);
        assertNotNull("The planted session must appear in the list", otherView);
        assertFalse("Planted session must not be marked as current", otherView.isCurrent());

        final String csrf = (String) callerSession.getAttribute(
                MaintenanceResource.CSRF_TOKEN_ATTRIBUTE);
        assertNotNull("listSessions must persist a fresh CSRF token on the caller's session",
                csrf);
        assertTrue("CSRF timestamp must be stored alongside the token",
                callerSession.getAttribute(MaintenanceResource.CSRF_TOKEN_TIMESTAMP_ATTRIBUTE)
                        instanceof Instant);

        assertEquals("Planted other session token must match the HMAC of its id under the issued CSRF",
                SessionTokenUtil.obfuscateSessionId(otherSession.getId(), csrf),
                otherView.token());
    }

    /**
     * Given scenario: a non-admin user calls listSessions
     * Expected result: SecurityException is thrown
     */
    @Test(expected = SecurityException.class)
    public void test_listSessions_asNonAdmin_throwsSecurity() {
        resource.listSessions(createRequestForUser(nonAdminUser), mockResponse);
    }

    // ==================== DELETE /_sessions/{token} ====================

    /**
     * Given scenario: admin issues a GET to receive a fresh CSRF, then DELETEs a planted
     *                 session's token derived from that CSRF
     * Expected result: the target session is invalidated (its USER_ID attribute is cleared
     *                  by MockSession.invalidate()), the endpoint returns "Session invalidated"
     */
    @Test
    public void test_killSession_validToken_invalidatesTarget() {
        final HttpSession target = newPlantedAdminSession("1.2.3.4");
        final HttpServletRequest request = createAdminRequest();
        request.getSession().setAttribute(WebKeys.USER_ID, adminUser.getUserId());
        plantSession(request.getSession());

        resource.listSessions(request, mockResponse);
        final String csrf = (String) request.getSession()
                .getAttribute(MaintenanceResource.CSRF_TOKEN_ATTRIBUTE);
        final String token = SessionTokenUtil.obfuscateSessionId(target.getId(), csrf);

        final ResponseEntityStringView result =
                resource.killSession(request, mockResponse, token);

        assertNotNull(result);
        assertEquals("Session invalidated", result.getEntity());
        assertNull("Target session must have been invalidated (USER_ID cleared)",
                target.getAttribute(WebKeys.USER_ID));
    }

    /**
     * Given scenario: admin tries to invalidate its own session
     * Expected result: BadRequestException — self-invalidation is forbidden
     */
    @Test(expected = BadRequestException.class)
    public void test_killSession_ownToken_throwsBadRequest() {
        final HttpServletRequest request = createAdminRequest();
        final HttpSession callerSession = request.getSession();
        callerSession.setAttribute(WebKeys.USER_ID, adminUser.getUserId());
        plantSession(callerSession);

        resource.listSessions(request, mockResponse);
        final String csrf = (String) callerSession.getAttribute(
                MaintenanceResource.CSRF_TOKEN_ATTRIBUTE);
        final String selfToken = SessionTokenUtil.obfuscateSessionId(
                callerSession.getId(), csrf);

        resource.killSession(request, mockResponse, selfToken);
    }

    /**
     * Given scenario: admin DELETEs a session without having called GET first
     * Expected result: ForbiddenException — there is no CSRF secret on the caller's session
     */
    @Test(expected = ForbiddenException.class)
    public void test_killSession_missingCsrf_throwsForbidden() {
        final HttpServletRequest request = createAdminRequest();
        resource.killSession(request, mockResponse, "any-token");
    }

    /**
     * Given scenario: the CSRF secret on the caller's session was issued 20 minutes ago
     * Expected result: ForbiddenException — exceeds the 15-minute expiry window
     */
    @Test(expected = ForbiddenException.class)
    public void test_killSession_expiredCsrf_throwsForbidden() {
        final HttpServletRequest request = createAdminRequest();
        final HttpSession callerSession = request.getSession();
        callerSession.setAttribute(MaintenanceResource.CSRF_TOKEN_ATTRIBUTE, "stale-csrf");
        callerSession.setAttribute(MaintenanceResource.CSRF_TOKEN_TIMESTAMP_ATTRIBUTE,
                Instant.now().minus(Duration.ofMinutes(20)));

        resource.killSession(request, mockResponse, "any-token");
    }

    /**
     * Given scenario: a syntactically valid HMAC is presented but it does not match any
     *                 session currently tracked by SessionMonitor
     * Expected result: NotFoundException
     */
    @Test(expected = NotFoundException.class)
    public void test_killSession_unknownToken_throwsNotFound() {
        final HttpServletRequest request = createAdminRequest();
        resource.listSessions(request, mockResponse);
        final String unknown = SessionTokenUtil.obfuscateSessionId(
                "session-that-does-not-exist", "secret-that-was-never-issued");

        resource.killSession(request, mockResponse, unknown);
    }

    /**
     * Given scenario: non-admin DELETE
     * Expected result: SecurityException — the auth gate runs before any session lookup
     */
    @Test(expected = SecurityException.class)
    public void test_killSession_asNonAdmin_throwsSecurity() {
        resource.killSession(createRequestForUser(nonAdminUser), mockResponse, "tok");
    }

    // ==================== GET /_systemJobs ====================

    /**
     * Given scenario: a custom Quartz job is scheduled with a known misfire instruction
     *                 ({@link CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING}), then admin
     *                 calls listSystemJobs
     * Expected result: the scheduled job appears with the EXACT field values produced by
     *                  the scheduler: className equals the test class name, durable=true
     *                  (set on the JobDetail), stateful=true (TestNoOpJob implements
     *                  StatefulJob), volatile=false, running=false (just scheduled),
     *                  nextFireTime is a future epoch ms, nextFireTimeFormatted matches
     *                  the documented "yyyy-MM-dd 'at' HH:mm:ss z" pattern, and
     *                  misfireInstruction is the mapped enum string "DO_NOTHING".
     */
    @Test
    public void test_listSystemJobs_asAdmin_returnsScheduledJob() throws Exception {
        final String jobName = "test-list-job-" + UUIDGenerator.generateUuid();
        final String jobGroup = "test-jobs-group";
        final long beforeSchedule = System.currentTimeMillis();
        scheduleTestJob(jobName, jobGroup, CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        try {
            final ResponseEntitySystemJobListView result =
                    resource.listSystemJobs(createAdminRequest(), mockResponse);

            assertNotNull(result);
            final List<Map<String, Object>> jobs = result.getEntity();
            assertNotNull(jobs);

            Map<String, Object> mine = null;
            for (final Map<String, Object> job : jobs) {
                if (jobName.equals(job.get("name")) && jobGroup.equals(job.get("group"))) {
                    mine = job;
                    break;
                }
            }
            assertNotNull("The freshly scheduled test job must appear in the response", mine);

            // Class & flags — exact value assertions, not just "field exists"
            assertEquals(TestNoOpJob.class.getSimpleName(), mine.get("className"));
            assertEquals("durable was set to true on the JobDetail",
                    Boolean.TRUE, mine.get("durable"));
            assertEquals("TestNoOpJob implements StatefulJob, so detail.isStateful() must be true",
                    Boolean.TRUE, mine.get("stateful"));
            assertEquals(Boolean.FALSE, mine.get("volatile"));
            assertEquals("Job was just scheduled, not currently executing",
                    Boolean.FALSE, mine.get("running"));

            // nextFireTime must be a future Long
            final Object nextFireRaw = mine.get("nextFireTime");
            assertTrue("nextFireTime must be a Long epoch ms, was " + nextFireRaw,
                    nextFireRaw instanceof Long);
            assertTrue("nextFireTime must be in the future for a 5-minute cron",
                    ((Long) nextFireRaw) > beforeSchedule);

            // Formatted timestamp must match the documented pattern
            final Object formatted = mine.get("nextFireTimeFormatted");
            assertTrue("nextFireTimeFormatted must match \"yyyy-MM-dd 'at' HH:mm:ss z\", was "
                            + formatted,
                    formatted instanceof String
                            && ((String) formatted).matches(
                                    "\\d{4}-\\d{2}-\\d{2} at \\d{2}:\\d{2}:\\d{2} \\S+"));

            // Misfire instruction must be the DO_NOTHING branch we explicitly scheduled with
            assertEquals("Scheduled with MISFIRE_INSTRUCTION_DO_NOTHING — resource must map it",
                    "DO_NOTHING", mine.get("misfireInstruction"));
        } finally {
            QuartzUtils.removeJob(jobName, jobGroup);
        }
    }

    /**
     * Given scenario: a non-admin user calls listSystemJobs
     * Expected result: SecurityException — the maintenance portlet requires admin
     */
    @Test(expected = SecurityException.class)
    public void test_listSystemJobs_asNonAdmin_throwsSecurity() {
        resource.listSystemJobs(createRequestForUser(nonAdminUser), mockResponse);
    }

    // ==================== DELETE /_systemJobs/{group}/{name} ====================

    /**
     * Given scenario: a test job is scheduled, then admin DELETEs it via the resource
     * Expected result: response indicates deleted=true with the matching name/group, and
     *                  the job is gone from the scheduler
     */
    @Test
    public void test_deleteSystemJob_asAdmin_removesScheduledJob() throws Exception {
        final String jobName = "test-delete-job-" + UUIDGenerator.generateUuid();
        final String jobGroup = "test-jobs-group";
        scheduleTestJob(jobName, jobGroup);

        final ResponseEntitySystemJobDeleteView result =
                resource.deleteSystemJob(createAdminRequest(), mockResponse, jobGroup, jobName);

        assertNotNull(result);
        final Map<String, Object> entity = result.getEntity();
        assertEquals(Boolean.TRUE, entity.get("deleted"));
        assertEquals(jobName, entity.get("name"));
        assertEquals(jobGroup, entity.get("group"));
        assertNull("Job must be removed from the scheduler",
                QuartzUtils.getScheduler().getJobDetail(jobName, jobGroup));
    }

    /**
     * Given scenario: admin DELETEs a job that does not exist
     * Expected result: NotFoundException — the resource must not silently succeed
     */
    @Test(expected = NotFoundException.class)
    public void test_deleteSystemJob_nonExistent_throwsNotFound() {
        resource.deleteSystemJob(createAdminRequest(), mockResponse,
                "no-such-group-" + UUIDGenerator.generateUuid(),
                "no-such-job-" + UUIDGenerator.generateUuid());
    }

    // ==================== DELETE /_sessions ====================

    /**
     * Given scenario: two planted sessions plus the caller's own (also planted) are in
     *                 SessionMonitor
     * Expected result: killedCount >= 2 (both planted), caller's session survives (USER_ID
     *                  attribute is still set)
     */
    @Test
    public void test_killAllSessions_killsEveryoneExceptCaller() {
        final HttpSession a = newPlantedAdminSession("4.4.4.4");
        final HttpSession b = newPlantedAdminSession("5.5.5.5");
        final HttpServletRequest request = createAdminRequest();
        final HttpSession caller = request.getSession();
        caller.setAttribute(WebKeys.USER_ID, adminUser.getUserId());
        plantSession(caller);

        final ResponseEntityKillSessionsResultView result =
                resource.killAllSessions(request, mockResponse);

        assertNotNull(result);
        final KillSessionsResultView view = result.getEntity();
        assertNotNull(view);
        assertTrue("Both planted sessions must be killed",
                view.killedCount() >= 2);

        assertNull("Planted session a should have been invalidated",
                a.getAttribute(WebKeys.USER_ID));
        assertNull("Planted session b should have been invalidated",
                b.getAttribute(WebKeys.USER_ID));
        assertEquals("Caller's session must survive",
                adminUser.getUserId(), caller.getAttribute(WebKeys.USER_ID));
    }

    /**
     * Given scenario: non-admin DELETE
     * Expected result: SecurityException
     */
    @Test(expected = SecurityException.class)
    public void test_killAllSessions_asNonAdmin_throwsSecurity() {
        resource.killAllSessions(createRequestForUser(nonAdminUser), mockResponse);
    }

    // ==================== Legacy parity ====================

    /**
     * Given scenario: legacy DWR class still exposes obfuscateSessionId/validateSessionId
     *                 as a backwards-compat shim
     * Expected result: it produces and validates the same tokens as the REST utility — any
     *                  pre-existing client code calling the static methods continues to work
     */
    @Test
    public void test_legacy_userSessionAjax_delegates_to_sessionTokenUtil() {
        final String sessionId = "session-" + UUIDGenerator.generateUuid();
        final String csrf = "csrf-" + UUIDGenerator.generateUuid();

        @SuppressWarnings("deprecation")
        final String legacyToken = UserSessionAjax.obfuscateSessionId(sessionId, csrf);
        final String modernToken = SessionTokenUtil.obfuscateSessionId(sessionId, csrf);
        assertEquals("Legacy DWR shim must produce the same token as the REST utility",
                modernToken, legacyToken);

        @SuppressWarnings("deprecation")
        final boolean legacyAccepts = UserSessionAjax.validateSessionId(
                sessionId, csrf, legacyToken);
        assertTrue("Legacy validate must accept its own token", legacyAccepts);

        @SuppressWarnings("deprecation")
        final boolean modernAcceptsLegacy = SessionTokenUtil.validateSessionId(
                sessionId, csrf, legacyToken);
        assertTrue("REST utility must accept tokens minted by the legacy shim",
                modernAcceptsLegacy);
    }

    /**
     * Removes any sessions planted by the current test from {@link SessionMonitor} so that
     * subsequent tests start with a clean cache. Reflection is required because the cache
     * is a private static field with no public mutator beyond {@link SessionMonitor}'s own
     * listener callbacks.
     */
    @After
    public void cleanPlantedSessions() throws Exception {
        for (final String id : plantedSessionIds) {
            sessionCache().invalidate(id);
        }
        plantedSessionIds.clear();
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

    /**
     * Creates a fresh {@link MockSession} that already carries the USER_ID and remote
     * address attributes a real authenticated session would have, registers it with
     * {@link SessionMonitor} via reflection, and tracks it for cleanup.
     */
    private HttpSession newPlantedAdminSession(final String remoteAddr) {
        final HttpSession session = new MockSession(UUIDGenerator.generateUuid());
        session.setAttribute(WebKeys.USER_ID, adminUser.getUserId());
        session.setAttribute(SessionMonitor.USER_REMOTE_ADDR, remoteAddr);
        plantSession(session);
        return session;
    }

    /**
     * Registers a pre-built session with {@link SessionMonitor}'s static cache via reflection.
     * The cache is keyed by session id so multiple plants with the same id collide — every
     * call uses a fresh UUID to keep tests independent.
     */
    private void plantSession(final HttpSession session) {
        try {
            sessionCache().put(session.getId(), session);
            plantedSessionIds.add(session.getId());
        } catch (final Exception e) {
            throw new AssertionError("Could not plant session in SessionMonitor", e);
        }
    }

    /**
     * Reflectively grabs the {@code SessionMonitor.userSessions} static cache. The cache has
     * no public accessor so tests need this hatch to set up a deterministic session list.
     */
    @SuppressWarnings("unchecked")
    private static com.dotcms.cache.DynamicTTLCache<String, HttpSession> sessionCache()
            throws Exception {
        final Field field = SessionMonitor.class.getDeclaredField("userSessions");
        field.setAccessible(true);
        return (com.dotcms.cache.DynamicTTLCache<String, HttpSession>) field.get(null);
    }

    /** Schedules a {@link TestNoOpJob} with the scheduler's default misfire policy. */
    private static void scheduleTestJob(final String jobName, final String jobGroup)
            throws Exception {
        scheduleTestJob(jobName, jobGroup, Trigger.MISFIRE_INSTRUCTION_SMART_POLICY);
    }

    /**
     * Schedules a {@link TestNoOpJob} directly on the Quartz scheduler with an explicit
     * misfire instruction. Mirrors the {@code QuartzUtilsTest#test_schedule_delete_job}
     * pattern: raw {@link JobDetail} + {@link CronTrigger} so the {@code Class<?>} reference
     * is used directly and no {@link Class#forName(String)} lookup is needed (which would
     * fail for inner classes). The 5-minute cron is harmless because {@link TestNoOpJob#execute}
     * is a no-op.
     */
    private static void scheduleTestJob(final String jobName, final String jobGroup,
            final int misfireInstruction) throws Exception {
        final JobDetail detail = new JobDetail(jobName, jobGroup, TestNoOpJob.class);
        detail.setDurability(true);
        final CronTrigger trigger = new CronTrigger(jobName + "_trigger", jobGroup,
                jobName, jobGroup, new Date(), null,
                DotInitScheduler.CRON_EXPRESSION_EVERY_5_MINUTES);
        trigger.setMisfireInstruction(misfireInstruction);
        QuartzUtils.getScheduler().addJob(detail, true);
        QuartzUtils.getScheduler().scheduleJob(trigger);
    }

    /**
     * Inert Quartz {@link StatefulJob} used by the {@code _systemJobs} tests. Mirrors the
     * inner-class pattern in {@code QuartzUtilsTest.TestJob}: never fires (we only need
     * scheduler-side visibility), so {@link #execute} is a no-op.
     */
    public static class TestNoOpJob implements StatefulJob {
        @Override
        public void execute(final JobExecutionContext context) {
            // intentional no-op
        }
    }
}
