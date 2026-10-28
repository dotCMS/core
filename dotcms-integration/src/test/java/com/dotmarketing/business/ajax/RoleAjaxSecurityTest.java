package com.dotmarketing.business.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.servlet.SessionMessages;
import java.lang.reflect.Field;
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
 * <p>The rejection tests run as a non-admin backend user who can open both the Roles and the Users
 * portlets. That user passes the Roles portlet check and the Users portlet check, so each call can
 * only be rejected by the CMS Administrator check. They target a throwaway role and layout, so a
 * failing check could not change shared data. A CMS Administrator case confirms the check still
 * lets administrators through.</p>
 */
public class RoleAjaxSecurityTest extends IntegrationTestBase {

    private static User backendUser;
    private static Layout userLayout;
    private static Layout targetLayout;
    private static Role targetRole;
    private static String cmsAdminRoleId;
    private static WebContextFactory.WebContextBuilder previousWebContextBuilder;

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

        // Role and layout targeted by the rejection tests, so they never touch shared data.
        targetLayout = new LayoutDataGen().name("RoleAjaxSecurityTest-target-layout")
                .nextPersisted();
        targetRole = new RoleDataGen().nextPersisted();

        assertTrue("backend user should have Roles portlet access",
                APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("roles", backendUser));
        assertTrue("backend user should have Users portlet access",
                APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("users", backendUser));
        assertFalse("backend user must not be a CMS Administrator for this test to be meaningful",
                backendUser.isAdmin());

        cmsAdminRoleId = APILocator.getRoleAPI().loadCMSAdminRole().getId();

        // WebContextFactory.builder is a JVM-global static shared by the whole suite: keep the
        // builder attached before this class ran so cleanUp() can put it back, then attach the
        // non-admin backend user for the whole class.
        previousWebContextBuilder = currentWebContextBuilder();
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
            if (targetRole != null) {
                APILocator.getRoleAPI().delete(targetRole);
            }
            if (backendUser != null) {
                // The three-argument delete hands the user's content to the system user, which has
                // no user role in a fresh database, so name an admin as the replacement instead.
                APILocator.getUserAPI().delete(backendUser, TestUserUtils.getAdminUser(),
                        APILocator.systemUser(), false);
            }
        } finally {
            // Put back the DWR context attached before this class ran, so later tests in the suite
            // (e.g. LayoutAPITest and PermissionAPITest, which call RoleAjax.saveRolePermission)
            // keep the context they expect instead of this class's non-admin user.
            attachWebContextBuilder(previousWebContextBuilder);
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
     *                 permissions for a role on the System Host.
     * Expected Result: {@link DotSecurityException} is thrown by the CMS Administrator check,
     *                  before any permission is saved.
     */
    @Test(expected = DotSecurityException.class)
    public void test_saveRolePermission_nonAdmin_throwsDotSecurityException() throws Exception {
        final Map<String, String> permissions = new HashMap<>();
        permissions.put("individual", "1");
        new RoleAjax().saveRolePermission(targetRole.getId(), Host.SYSTEM_HOST, permissions, false);
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
        new RoleAjax().saveRoleLayouts(targetRole.getId(), new String[]{targetLayout.getId()});
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

    /**
     * Method to test: {@link RoleAjax#updateLayout(String, String, String, int, List)} and
     *                 {@link RoleAjax#deleteLayout(String)}
     * Given Scenario: A CMS Administrator renames a layout and then deletes it.
     * Expected Result: Both calls go through: the layout is renamed, then removed.
     */
    @Test
    public void test_updateAndDeleteLayout_admin_succeeds() throws Exception {
        final User admin = new UserDataGen().roles(APILocator.getRoleAPI().loadBackEndUserRole(),
                APILocator.getRoleAPI().loadCMSAdminRole()).nextPersisted();
        final Layout layout = new LayoutDataGen().name("RoleAjaxSecurityTest-admin-layout")
                .nextPersisted();
        assertTrue("admin user must be a CMS Administrator", admin.isAdmin());

        setUpDwrContext(admin);
        try {
            final RoleAjax roleAjax = new RoleAjax();
            roleAjax.updateLayout(layout.getId(), "RoleAjaxSecurityTest-admin-renamed",
                    layout.getDescription(), layout.getTabOrder(), List.of("roles"));
            assertEquals("RoleAjaxSecurityTest-admin-renamed",
                    APILocator.getLayoutAPI().findLayout(layout.getId()).getName());

            roleAjax.deleteLayout(layout.getId());
            final Layout deleted = APILocator.getLayoutAPI().findLayout(layout.getId());
            assertTrue("layout should be deleted",
                    deleted == null || !UtilMethods.isSet(deleted.getId()));
        } finally {
            setUpDwrContext(backendUser);
            APILocator.getUserAPI().delete(admin, TestUserUtils.getAdminUser(),
                    APILocator.systemUser(), false);
        }
    }

    /**
     * Reads the builder {@code WebContextFactory} currently uses; the field is private and has no
     * getter.
     */
    private static WebContextFactory.WebContextBuilder currentWebContextBuilder() throws Exception {
        final Field builder = WebContextFactory.class.getDeclaredField("builder");
        builder.setAccessible(true);
        return (WebContextFactory.WebContextBuilder) builder.get(null);
    }

    /**
     * Makes {@code WebContextFactory} use the given builder ({@code null} detaches it).
     */
    private static void attachWebContextBuilder(
            final WebContextFactory.WebContextBuilder webContextBuilder) {
        final com.dotcms.repackage.org.directwebremoting.Container containerMock =
                mock(com.dotcms.repackage.org.directwebremoting.Container.class);
        when(containerMock.getBean(WebContextFactory.WebContextBuilder.class))
                .thenReturn(webContextBuilder);
        WebContextFactory.attach(containerMock);
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

        attachWebContextBuilder(builderMock);
    }
}
