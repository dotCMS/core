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
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ConflictException;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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

    // ==================== POST /_fixAssets ====================

    @Test
    @SuppressWarnings("rawtypes")
    public void test_startFixAssets_asAdmin_succeeds() {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityView<List<Map>> result =
                resource.startFixAssets(request, mockResponse);

        assertNotNull(result);
    }

    @Test(expected = SecurityException.class)
    public void test_startFixAssets_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.startFixAssets(request, mockResponse);
    }

    @Test(expected = ConflictException.class)
    public void test_startFixAssets_whileRunning_throwsConflict() throws Exception {
        final HttpServletRequest request = createAdminRequest();
        final AtomicBoolean flag = getFixAssetsRunningFlag();
        flag.set(true);
        try {
            resource.startFixAssets(request, mockResponse);
        } finally {
            flag.set(false);
        }
    }

    // ==================== GET /_fixAssets ====================

    @Test
    @SuppressWarnings("rawtypes")
    public void test_getFixAssetsProgress_asAdmin_succeeds() {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityView<List<Map>> result =
                resource.getFixAssetsProgress(request, mockResponse);

        assertNotNull(result);
    }

    @Test(expected = SecurityException.class)
    public void test_getFixAssetsProgress_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.getFixAssetsProgress(request, mockResponse);
    }

    // ==================== POST /_cleanAssets ====================

    @Test
    public void test_startCleanAssets_asAdmin_startsProcess() {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityCleanAssetsStatusView result =
                resource.startCleanAssets(request, mockResponse);

        assertNotNull(result);
        final CleanAssetsStatusView status = result.getEntity();
        assertNotNull(status);
        assertTrue("running should be true immediately after start", status.running());
        assertNotNull("status should have a status string", status.status());

        waitForCleanAssetsToFinish();
    }

    @Test(expected = SecurityException.class)
    public void test_startCleanAssets_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.startCleanAssets(request, mockResponse);
    }

    @Test(expected = ConflictException.class)
    public void test_startCleanAssets_whileRunning_throwsConflict() {
        final HttpServletRequest request = createAdminRequest();
        try {
            resource.startCleanAssets(request, mockResponse);
            // Second call while the first is still running must return 409.
            resource.startCleanAssets(request, mockResponse);
        } finally {
            waitForCleanAssetsToFinish();
        }
    }

    // ==================== GET /_cleanAssets ====================

    @Test
    public void test_getCleanAssetsStatus_asAdmin_returnsStatus() {
        final HttpServletRequest request = createAdminRequest();

        final ResponseEntityCleanAssetsStatusView result =
                resource.getCleanAssetsStatus(request, mockResponse);

        assertNotNull(result);
        final CleanAssetsStatusView status = result.getEntity();
        assertNotNull(status);
        assertNotNull("status string should not be null", status.status());
    }

    @Test(expected = SecurityException.class)
    public void test_getCleanAssetsStatus_asNonAdmin_throwsSecurity() {
        final HttpServletRequest request = createRequestForUser(nonAdminUser);
        resource.getCleanAssetsStatus(request, mockResponse);
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

    private static AtomicBoolean getFixAssetsRunningFlag() throws Exception {
        final Field field = MaintenanceResource.class.getDeclaredField("FIX_ASSETS_RUNNING");
        field.setAccessible(true);
        return (AtomicBoolean) field.get(null);
    }

    private void waitForCleanAssetsToFinish() {
        final long timeout = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < timeout) {
            final HttpServletRequest req = createAdminRequest();
            final CleanAssetsStatusView status =
                    resource.getCleanAssetsStatus(req, mockResponse).getEntity();
            if (!status.running()) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
