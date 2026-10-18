package com.dotmarketing.business.ajax;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.servlet.SessionMessages;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the CMS Administrator check on the {@link RoleAjax} DWR methods that
 * change role membership, role layouts, layouts and role permissions.
 *
 * <p>Every test runs as a non-admin backend user who can open both the Roles and the Users
 * portlets. That user passes the Roles portlet check and the Users portlet check, so each call can
 * only be rejected by the CMS Administrator check.</p>
 */
public class RoleAjaxSecurityTest extends IntegrationTestBase {

    private static User backendUser;
    private static Layout userLayout;
    private static Layout targetLayout;
    private static String cmsAdminRoleId;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        backendUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), backendUser);

        // Grant both the Roles and the Users portlets, so portlet access alone cannot be what
        // rejects the calls below.
        userLayout = new LayoutDataGen().name("RoleAjaxSecurityTest-user-layout")
                .portletIds("roles", "users")
                .nextPersisted();
        APILocator.getRoleAPI().addLayoutToRole(userLayout, backendUser.getUserRole());

        // Layout targeted by the update and delete tests, so they never touch shared data.
        targetLayout = new LayoutDataGen().name("RoleAjaxSecurityTest-target-layout")
                .nextPersisted();

        assertTrue("backend user should have Roles portlet access",
                APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("roles", backendUser));
        assertTrue("backend user should have Users portlet access",
                APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("users", backendUser));
        assertFalse("backend user must not be a CMS Administrator for this test to be meaningful",
                backendUser.isAdmin());

        cmsAdminRoleId = APILocator.getRoleAPI().loadCMSAdminRole().getId();

        // Attach the non-admin backend user as the current DWR user for the whole class, mirroring
        // the established pattern in BrowserAjaxTest (which attaches in @BeforeClass). Attaching
        // inside the @Test body is unreliable across the suite because WebContextFactory.builder is
        // a JVM-global static with no per-test isolation.
        setUpDwrContext(backendUser);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        try {
            if (userLayout != null) {
                APILocator.getRoleAPI().removeLayoutFromRole(userLayout,
                        backendUser.getUserRole());
                APILocator.getLayoutAPI().removeLayout(userLayout);
            }
            if (targetLayout != null) {
                APILocator.getLayoutAPI().removeLayout(targetLayout);
            }
            if (backendUser != null) {
                APILocator.getUserAPI().delete(backendUser, APILocator.systemUser(), false);
            }
        } finally {
            // DWR's WebContextFactory.builder is a JVM-global static with no per-test teardown
            // (attach() simply overwrites it). Other DWR-backed tests in MainSuite2b — e.g.
            // LayoutAPITest and PermissionAPITest, which call RoleAjax.saveRolePermission — rely on
            // a system-user context being attached by earlier tests. Restore that context here so
            // this test's non-admin backend-user context does not leak and make them fail the
            // CMS Administrator check.
            setUpDwrContext(APILocator.systemUser());
        }
    }

    /**
     * Method to test: {@link RoleAjax#addUserToRole(String, String)}
     * Given Scenario: A non-admin backend user with Roles and Users portlet access adds their own
     *                 user to the CMS Administrator role.
     * Expected Result: {@link DotSecurityException} is thrown by the CMS Administrator check.
     */
    @Test(expected = DotSecurityException.class)
    public void test_addUserToRole_nonAdmin_throwsDotSecurityException() throws Exception {
        new RoleAjax().addUserToRole(backendUser.getUserId(), cmsAdminRoleId);
    }

    /**
     * Method to test: {@link RoleAjax#saveRolePermission(String, String, Map, boolean)}
     * Given Scenario: A non-admin backend user with Roles and Users portlet access saves
     *                 permissions for a role.
     * Expected Result: {@link DotSecurityException} is thrown by the CMS Administrator check,
     *                  before any permission is saved.
     */
    @Test(expected = DotSecurityException.class)
    public void test_saveRolePermission_nonAdmin_throwsDotSecurityException() throws Exception {
        final Map<String, String> permissions = new HashMap<>();
        permissions.put("individual", "1");
        new RoleAjax().saveRolePermission(cmsAdminRoleId, Host.SYSTEM_HOST, permissions, false);
    }

    /**
     * Method to test: {@link RoleAjax#saveRoleLayouts(String, String[])}
     * Given Scenario: A non-admin backend user with Roles and Users portlet access saves the
     *                 layouts of a role.
     * Expected Result: {@link DotSecurityException} is thrown by the CMS Administrator check,
     *                  before the role or its layouts are loaded.
     */
    @Test(expected = DotSecurityException.class)
    public void test_saveRoleLayouts_nonAdmin_throwsDotSecurityException() throws Exception {
        new RoleAjax().saveRoleLayouts(cmsAdminRoleId, new String[]{userLayout.getId()});
    }

    /**
     * Method to test: {@link RoleAjax#updateLayout(String, String, String, int, List)}
     * Given Scenario: A non-admin backend user with Roles and Users portlet access renames a
     *                 layout and changes its portlets.
     * Expected Result: {@link DotSecurityException} is thrown by the CMS Administrator check.
     */
    @Test(expected = DotSecurityException.class)
    public void test_updateLayout_nonAdmin_throwsDotSecurityException() throws Exception {
        new RoleAjax().updateLayout(targetLayout.getId(), "RoleAjaxSecurityTest-renamed",
                targetLayout.getDescription(), targetLayout.getTabOrder(), List.of("roles"));
    }

    /**
     * Method to test: {@link RoleAjax#deleteLayout(String)}
     * Given Scenario: A non-admin backend user with Roles and Users portlet access deletes a
     *                 layout.
     * Expected Result: {@link DotSecurityException} is thrown by the CMS Administrator check.
     */
    @Test(expected = DotSecurityException.class)
    public void test_deleteLayout_nonAdmin_throwsDotSecurityException() throws Exception {
        new RoleAjax().deleteLayout(targetLayout.getId());
    }

    private static void setUpDwrContext(final User user) {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SessionMessages.KEY)).thenReturn(new LinkedHashMap<>());

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(session);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);

        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);

        final WebContextFactory.WebContextBuilder builderMock =
                mock(WebContextFactory.WebContextBuilder.class);
        when(builderMock.get()).thenReturn(webContext);

        final com.dotcms.repackage.org.directwebremoting.Container containerMock =
                mock(com.dotcms.repackage.org.directwebremoting.Container.class);
        when(containerMock.getBean(WebContextFactory.WebContextBuilder.class))
                .thenReturn(builderMock);

        WebContextFactory.attach(containerMock);
    }
}
