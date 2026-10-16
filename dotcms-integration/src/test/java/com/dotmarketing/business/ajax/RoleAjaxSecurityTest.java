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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Regression tests for the admin-gate hardening of the portlet-only gated {@link RoleAjax} DWR
 * methods (private-issues#642 / private-issues#643).
 *
 * <p>Each test models a non-admin backend user who legitimately holds <strong>roles-portlet
 * access only</strong> (no "users" portlet, not a CMS Administrator) — the exact actor the
 * portlet-only gate used to let through. The layout deliberately grants only the "roles" portlet
 * so that {@code getAdminUser()} — which authorizes on "users"-portlet access OR CMS Admin — is
 * actually exercised as a gate rather than being satisfied by an incidental "users" grant.</p>
 *
 * <ul>
 *   <li>{@code addUserToRole} — step 2 of the #642 escalation chain, blocked by {@code isAdmin()}.</li>
 *   <li>{@code saveRolePermission} — the method fixed by #36345 (admin check restored).</li>
 *   <li>{@code saveRoleLayouts} — admin check moved before any DB read (fail-fast).</li>
 * </ul>
 */
public class RoleAjaxSecurityTest extends IntegrationTestBase {

    private static User backendUser;
    private static Layout rolesPortletLayout;
    private static String cmsAdminRoleId;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        backendUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), backendUser);

        // Give the backend user access to the roles portlet ONLY. Not the "users" portlet, so
        // getAdminUser() (which checks "users"-portlet access OR CMS Admin) is a real gate here,
        // and not a layout that incidentally satisfies it.
        rolesPortletLayout = new LayoutDataGen().name("RoleAjaxSecurityTest-roles-layout")
                .portletIds("roles")
                .nextPersisted();
        APILocator.getRoleAPI().addLayoutToRole(rolesPortletLayout, backendUser.getUserRole());

        assertTrue("backend user should now have roles portlet access",
                APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("roles", backendUser));

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
            if (rolesPortletLayout != null) {
                APILocator.getRoleAPI().removeLayoutFromRole(rolesPortletLayout,
                        backendUser.getUserRole());
                APILocator.getLayoutAPI().removeLayout(rolesPortletLayout);
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
            // getAdminUser() gate.
            setUpDwrContext(APILocator.systemUser());
        }
    }

    /**
     * Method to test: {@link RoleAjax#addUserToRole}
     * Given Scenario: A low-privilege backend user has already obtained roles-portlet access
     *                 (simulating a successful step 1), then calls the DWR endpoint to add
     *                 themselves to the CMS Administrator role (step 2 of the attack chain).
     * Expected Result: {@link DotSecurityException} is thrown by the {@code getAdminUser()}
     *                  admin check — the self-escalation is blocked.
     */
    @Test(expected = DotSecurityException.class)
    public void test_addUserToRole_withRolesPortletAccess_cannotSelfEscalateToAdmin()
            throws Exception {
        final RoleAjax roleAjax = new RoleAjax();
        roleAjax.addUserToRole(backendUser.getUserId(), cmsAdminRoleId);
    }

    /**
     * Method to test: {@link RoleAjax#saveRolePermission(String, String, Map, boolean)}
     * Given Scenario: A non-admin backend user holding only roles-portlet access — which passes
     *                 {@code validateRolesPortletPermissions} — calls {@code saveRolePermission}.
     *                 This is the method whose admin guard was accidentally dropped in 2018 and
     *                 is restored by #36345.
     * Expected Result: {@link DotSecurityException} is thrown by the {@code getAdminUser()} check
     *                  before any permission is persisted.
     */
    @Test(expected = DotSecurityException.class)
    public void test_saveRolePermission_withRolesPortletAccess_throwsDotSecurityException()
            throws Exception {
        final RoleAjax roleAjax = new RoleAjax();
        final Map<String, String> permissions = new HashMap<>();
        permissions.put("individual", "1");
        roleAjax.saveRolePermission(cmsAdminRoleId, Host.SYSTEM_HOST, permissions, false);
    }

    /**
     * Method to test: {@link RoleAjax#saveRoleLayouts(String, String[])}
     * Given Scenario: A non-admin backend user holding only roles-portlet access — which passes
     *                 {@code validateRolesPortletPermissions} — calls {@code saveRoleLayouts}.
     * Expected Result: {@link DotSecurityException} is thrown by the {@code getAdminUser()} check,
     *                  which #36345 moved ahead of every DB read (fail-fast), so no role or layout
     *                  is loaded for an unauthorized caller.
     */
    @Test(expected = DotSecurityException.class)
    public void test_saveRoleLayouts_withRolesPortletAccess_throwsDotSecurityException()
            throws Exception {
        final RoleAjax roleAjax = new RoleAjax();
        roleAjax.saveRoleLayouts(cmsAdminRoleId, new String[]{rolesPortletLayout.getId()});
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
