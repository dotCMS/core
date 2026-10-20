package com.dotmarketing.portlets.cmsmaintenance.ajax;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.ajax.DwrWebContextRule;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Integration tests for {@link CMSMaintenanceAjax} with real roles and layouts: every method
 * requires a CMS administrator who also has the Maintenance portlet. Only the methods that change
 * nothing are called here; the ones that delete data are covered by the unit tests.
 */
public class CMSMaintenanceAjaxIntegrationTest {

    private static final String MAINTENANCE_PORTLET = "maintenance";

    @ClassRule
    public static final DwrWebContextRule dwrContext = new DwrWebContextRule();

    private static Layout maintenanceLayout;
    private static User maintenancePortletUser;
    private static User adminWithoutPortlet;
    private static User adminWithPortlet;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();

        maintenanceLayout = new LayoutDataGen()
                .name("maintenance-" + System.nanoTime())
                .portletIds(MAINTENANCE_PORTLET)
                .nextPersisted();
        maintenancePortletUser = user("maintenance-portlet", roleAPI.loadBackEndUserRole());
        roleAPI.addLayoutToRole(maintenanceLayout, roleAPI.getUserRole(maintenancePortletUser));

        // A back-end CMS administrator always has every portlet, so the only administrator without
        // the Maintenance portlet is one without the back-end role.
        adminWithoutPortlet = user("admin-no-portlet",
                roleAPI.loadCMSAdminRole(), roleAPI.loadFrontEndUserRole());
        adminWithPortlet = user("admin-portlet",
                roleAPI.loadCMSAdminRole(), roleAPI.loadBackEndUserRole());

        assertFalse(maintenancePortletUser.isAdmin());
        assertTrue(layoutAPI.doesUserHaveAccessToPortlet(MAINTENANCE_PORTLET, maintenancePortletUser));
        assertTrue(adminWithoutPortlet.isAdmin());
        assertFalse(layoutAPI.doesUserHaveAccessToPortlet(MAINTENANCE_PORTLET, adminWithoutPortlet));
        assertTrue(adminWithPortlet.isAdmin());
        assertTrue(layoutAPI.doesUserHaveAccessToPortlet(MAINTENANCE_PORTLET, adminWithPortlet));
    }

    /**
     * Deletes the test users, handing their content to the admin user, and the test layout
     * through a freshly loaded instance (the persisted one is detached from the current session).
     */
    @AfterClass
    public static void cleanUp() throws Exception {
        final User replacement = TestUserUtils.getAdminUser();
        for (final User user : new User[]{maintenancePortletUser, adminWithoutPortlet, adminWithPortlet}) {
            if (null != user) {
                APILocator.getUserAPI().delete(user, replacement, APILocator.systemUser(), false);
            }
        }
        if (null != maintenanceLayout) {
            final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
            layoutAPI.removeLayout(layoutAPI.findLayout(maintenanceLayout.getId()));
        }
    }

    /**
     * Method to test: {@link CMSMaintenanceAjax#validateUser()}
     * Given: a back-end user with the Maintenance portlet who is not a CMS administrator
     * Expected: DotSecurityException
     */
    @Test
    public void validateUser_rejectsPortletHolderWhoIsNotAdmin() {
        dwrContext.loginAs(maintenancePortletUser);

        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().validateUser());
    }

    /**
     * Method to test: {@link CMSMaintenanceAjax#validateUser()}
     * Given: a CMS administrator without the Maintenance portlet
     * Expected: DotSecurityException
     */
    @Test
    public void validateUser_rejectsAdminWithoutPortlet() {
        dwrContext.loginAs(adminWithoutPortlet);

        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().validateUser());
    }

    /**
     * Method to test: {@link CMSMaintenanceAjax#validateUser()}
     * Given: a CMS administrator with the Maintenance portlet
     * Expected: {@code true}
     */
    @Test
    public void validateUser_allowsAdminWithPortlet() throws Exception {
        dwrContext.loginAs(adminWithPortlet);

        assertTrue(new CMSMaintenanceAjax().validateUser());
    }

    /**
     * Method to test: {@link CMSMaintenanceAjax#getCleanAssetsStatus()}
     * Given: a non-administrator with the Maintenance portlet, then an administrator without it
     * Expected: DotSecurityException for both, and no status is returned
     */
    @Test
    public void getCleanAssetsStatus_requiresAdminWithMaintenancePortlet() {
        dwrContext.loginAs(maintenancePortletUser);
        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().getCleanAssetsStatus());

        dwrContext.loginAs(adminWithoutPortlet);
        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().getCleanAssetsStatus());
    }

    /**
     * Method to test: {@link CMSMaintenanceAjax#getCleanAssetsStatus()}
     * Given: a CMS administrator with the Maintenance portlet
     * Expected: the status of the clean-assets process
     */
    @Test
    public void getCleanAssetsStatus_allowsAdminWithPortlet() throws Exception {
        dwrContext.loginAs(adminWithPortlet);

        assertNotNull(new CMSMaintenanceAjax().getCleanAssetsStatus());
    }

    /**
     * Method to test: {@link CMSMaintenanceAjax#getFixAssetsProgress()}
     * Given: a non-administrator with the Maintenance portlet, then an administrator without it
     * Expected: DotSecurityException for both
     */
    @Test
    public void getFixAssetsProgress_requiresAdminWithMaintenancePortlet() {
        dwrContext.loginAs(maintenancePortletUser);
        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().getFixAssetsProgress());

        dwrContext.loginAs(adminWithoutPortlet);
        assertThrows(DotSecurityException.class, () -> new CMSMaintenanceAjax().getFixAssetsProgress());
    }

    /**
     * Method to test: {@link CMSMaintenanceAjax#getFixAssetsProgress()}
     * Given: a CMS administrator with the Maintenance portlet
     * Expected: the call completes without an exception (the answer is {@code null} when no fix
     * task has run)
     */
    @Test
    public void getFixAssetsProgress_allowsAdminWithPortlet() {
        dwrContext.loginAs(adminWithPortlet);

        try {
            new CMSMaintenanceAjax().getFixAssetsProgress();
        } catch (final Exception e) {
            fail("the fix assets progress must be readable by an administrator with the Maintenance portlet, got " + e);
        }
    }

    private static User user(final String label, final Role... roles) {
        return new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(roles)
                .nextPersisted();
    }
}
