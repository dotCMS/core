package com.dotcms.rest.api.v1.maintenance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the maintenance tools REST endpoints in {@link MaintenanceResource}.
 *
 * @author hassandotcms
 */
public class MaintenanceResourceIntegrationTest extends IntegrationTestBase {

    private static MaintenanceResource resource;
    private static HttpServletResponse mockResponse;
    private static User adminUser;
    private static User nonAdminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        resource = new MaintenanceResource();
        mockResponse = new MockHttpResponse().response();
        adminUser = TestUserUtils.getAdminUser();

        nonAdminUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), nonAdminUser);
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
}
