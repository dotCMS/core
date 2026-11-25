package com.dotmarketing.business.ajax;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Integration tests for the layout reads of {@link RoleAjax} with real roles and layouts: listing
 * all layouts, or the layouts of a role, requires the Roles portlet.
 */
public class RoleAjaxLayoutsIntegrationTest {

    private static final String ROLES_PORTLET = "roles";

    @ClassRule
    public static final DwrWebContextRule dwrContext = new DwrWebContextRule();

    private static Layout rolesLayout;
    private static User userWithoutPortlet;
    private static User rolesPortletUser;
    private static String rolesPortletUserRoleId;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();

        userWithoutPortlet = backendUser("roles-no-portlet");
        rolesPortletUser = backendUser("roles-portlet");
        rolesLayout = new LayoutDataGen()
                .name("roles-" + System.nanoTime())
                .portletIds(ROLES_PORTLET)
                .nextPersisted();
        rolesPortletUserRoleId = roleAPI.getUserRole(rolesPortletUser).getId();
        roleAPI.addLayoutToRole(rolesLayout, roleAPI.getUserRole(rolesPortletUser));

        assertFalse(layoutAPI.doesUserHaveAccessToPortlet(ROLES_PORTLET, userWithoutPortlet));
        assertFalse(rolesPortletUser.isAdmin());
        assertTrue(layoutAPI.doesUserHaveAccessToPortlet(ROLES_PORTLET, rolesPortletUser));
    }

    /**
     * Deletes the test users, handing their content to the admin user, and the test layout
     * through a freshly loaded instance (the persisted one is detached from the current session).
     */
    @AfterClass
    public static void cleanUp() throws Exception {
        final User replacement = TestUserUtils.getAdminUser();
        for (final User user : new User[]{userWithoutPortlet, rolesPortletUser}) {
            if (null != user) {
                APILocator.getUserAPI().delete(user, replacement, APILocator.systemUser(), false);
            }
        }
        if (null != rolesLayout) {
            final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
            layoutAPI.removeLayout(layoutAPI.findLayout(rolesLayout.getId()));
        }
    }

    /**
     * Method to test: {@link RoleAjax#getAllLayouts()}
     * Given: a back-end user without the Roles portlet
     * Expected: DotSecurityException
     */
    @Test
    public void getAllLayouts_requiresRolesPortlet() {
        dwrContext.loginAs(userWithoutPortlet);

        assertThrows(DotSecurityException.class, () -> new RoleAjax().getAllLayouts());
    }

    /**
     * Method to test: {@link RoleAjax#loadRoleLayouts(String)}
     * Given: a back-end user without the Roles portlet asks for the layouts of a role that has one
     * Expected: DotSecurityException
     */
    @Test
    public void loadRoleLayouts_requiresRolesPortlet() {
        dwrContext.loginAs(userWithoutPortlet);

        assertThrows(DotSecurityException.class,
                () -> new RoleAjax().loadRoleLayouts(rolesPortletUserRoleId));
    }

    /**
     * Method to test: {@link RoleAjax#getAllLayouts()}
     * Given: a back-end user with the Roles portlet who is not a CMS administrator
     * Expected: the list of layouts, including the test layout
     */
    @Test
    public void getAllLayouts_allowsRolesPortletHolder() throws Exception {
        dwrContext.loginAs(rolesPortletUser);

        final List<Map<String, Object>> layouts = new RoleAjax().getAllLayouts();

        assertTrue(layouts.stream().anyMatch(layout -> layout.containsValue(rolesLayout.getId())));
    }

    /**
     * Method to test: {@link RoleAjax#loadRoleLayouts(String)}
     * Given: a back-end user with the Roles portlet asks for the layouts of a role that has one
     * Expected: the role's layout
     */
    @Test
    public void loadRoleLayouts_allowsRolesPortletHolder() throws Exception {
        dwrContext.loginAs(rolesPortletUser);

        final List<Map<String, Object>> layouts = new RoleAjax().loadRoleLayouts(rolesPortletUserRoleId);

        assertTrue(layouts.stream().anyMatch(layout -> layout.containsValue(rolesLayout.getId())));
    }

    private static User backendUser(final String label) throws DotDataException {
        return new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
    }
}
