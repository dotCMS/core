package com.dotmarketing.portal.struts;

import static org.junit.Assert.assertFalse;
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
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link StrutsPathAccess} with real users, roles and layouts: the
 * administrative legacy screens follow the portlets that own them, and every other legacy screen
 * is left reachable as it was.
 */
public class StrutsPathAccessTest {

    private static final String SCHEDULER_PATH = "/ext/scheduler/edit_scheduler";
    private static final String MAINTENANCE_PATH = "/ext/cmsmaintenance/view_cms_maintenance";
    private static final String USERADMIN_PATH = "/ext/useradmin/view_users";
    private static final String USERFILTER_PATH = "/ext/userfilter/edit_userfilter";
    private static final String ROLEADMIN_PATH = "/ext/roleadmin/view_roles";
    private static final String LANGUAGES_PATH = "/ext/languages_manager/view_languages_manager";
    private static final String SITES_PATH = "/ext/hostadmin/view_hosts";
    private static final String UNRESTRICTED_PATH = "/ext/contentlet/edit_contentlet";
    private static final String LINKS_PATH = "/ext/links/edit_link";

    private static final String USERS_BETA_PORTLET = "users-beta";

    private static final List<User> usersToClean = new ArrayList<>();
    private static final List<Layout> layoutsToClean = new ArrayList<>();

    private static User adminUser;
    private static User jobsPortletUser;
    private static User usersPortletUser;
    private static User rolesPortletUser;
    private static User languagesPortletUser;
    private static User localesPortletUser;
    private static User contentPortletUser;
    private static User usersBetaPortletUser;
    private static User siteSearchPortletUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();

        adminUser = TestUserUtils.getAdminUser();
        jobsPortletUser = backendUserWithPortlet("jobs-portlet", PortletID.JOBS.toString());
        usersPortletUser = backendUserWithPortlet("users-portlet", PortletID.USERS.toString());
        rolesPortletUser = backendUserWithPortlet("roles-portlet", PortletID.ROLES.toString());
        languagesPortletUser = backendUserWithPortlet("languages-portlet",
                PortletID.LANGUAGES.toString());
        localesPortletUser = backendUserWithPortlet("locales-portlet",
                PortletID.LOCALES.toString());
        contentPortletUser = backendUserWithPortlet("content-portlet", PortletID.CONTENT.toString());
        usersBetaPortletUser = backendUserWithPortlet("users-beta-portlet", USERS_BETA_PORTLET);
        siteSearchPortletUser = backendUserWithPortlet("site-search-portlet",
                PortletID.SITE_SEARCH.toString());

        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
        assertTrue(adminUser.isAdmin());
        assertFalse(jobsPortletUser.isAdmin());
        assertTrue(layoutAPI.doesUserHaveAccessToPortlet(PortletID.JOBS.toString(),
                jobsPortletUser));

        // Every refusal below rests on the control users holding none of the administrative
        // portlets. They carry the shared Back-end User role, so a layout another test attaches to
        // that role would grant one of these and turn the refusals into silent passes. Assert the
        // precondition here, where the failure names the cause instead of looking like a gate bug.
        for (final PortletID administrativePortlet : new PortletID[]{PortletID.JOBS,
                PortletID.MAINTENANCE, PortletID.SITES, PortletID.USERS, PortletID.ROLES,
                PortletID.LANGUAGES, PortletID.LOCALES}) {
            assertFalse("The Back-end User role grants '" + administrativePortlet
                            + "', so this test cannot tell a refusal from a pass. Another test "
                            + "attached a layout holding it to the shared role.",
                    layoutAPI.doesUserHaveAccessToPortlet(administrativePortlet.toString(),
                            contentPortletUser));
        }
    }

    /**
     * Deletes the test users, handing their content to the admin user, and the test layouts
     * through freshly loaded instances, since the persisted ones are detached from this session.
     * A failure on one entry does not strand the rest.
     */
    @AfterClass
    public static void cleanUp() {
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
        for (final User user : usersToClean) {
            try {
                APILocator.getUserAPI()
                        .delete(user, TestUserUtils.getAdminUser(), APILocator.systemUser(), false);
            } catch (final Exception e) {
                Logger.warn(StrutsPathAccessTest.class,
                        "Unable to delete test user: " + e.getMessage());
            }
        }
        for (final Layout layout : layoutsToClean) {
            try {
                layoutAPI.removeLayout(layoutAPI.findLayout(layout.getId()));
            } catch (final Exception e) {
                Logger.warn(StrutsPathAccessTest.class,
                        "Unable to delete test layout: " + e.getMessage());
            }
        }
    }

    /**
     * Method to test: {@link StrutsPathAccess#canAccess(String, User)}
     * Given: a back-end user holding the portlet that owns an administrative directory
     * Expected: allowed, so the portlet keeps opening its own screens
     */
    @Test
    public void canAccess_restrictedDirectory_allowsOwningPortletHolder() {
        assertTrue(StrutsPathAccess.canAccess(SCHEDULER_PATH, jobsPortletUser));
        assertTrue(StrutsPathAccess.canAccess(USERADMIN_PATH, usersPortletUser));
        assertTrue(StrutsPathAccess.canAccess(USERFILTER_PATH, usersPortletUser));
    }

    /**
     * Method to test: {@link StrutsPathAccess#canAccess(String, User)}
     * Given: a directory owned by two portlets — roles are administered from both the Users and
     * the Roles screens, and languages from both Languages and Locales
     * Expected: either portlet opens it, and a portlet outside the pair does not
     */
    @Test
    public void canAccess_directoryOwnedByTwoPortlets_allowsEither() {
        assertTrue(StrutsPathAccess.canAccess(ROLEADMIN_PATH, usersPortletUser));
        assertTrue(StrutsPathAccess.canAccess(ROLEADMIN_PATH, rolesPortletUser));
        assertFalse(StrutsPathAccess.canAccess(ROLEADMIN_PATH, contentPortletUser));

        assertTrue(StrutsPathAccess.canAccess(LANGUAGES_PATH, languagesPortletUser));
        assertTrue(StrutsPathAccess.canAccess(LANGUAGES_PATH, localesPortletUser));
        assertFalse(StrutsPathAccess.canAccess(LANGUAGES_PATH, contentPortletUser));
    }

    /**
     * Method to test: {@link StrutsPathAccess#canAccess(String, User)}
     * Given: a portlet that owns one administrative directory but not a neighbouring one
     * Expected: holding one administrative portlet does not open the others
     */
    @Test
    public void canAccess_oneAdministrativePortlet_doesNotOpenTheOthers() {
        assertFalse(StrutsPathAccess.canAccess(MAINTENANCE_PATH, jobsPortletUser));
        assertFalse(StrutsPathAccess.canAccess(SITES_PATH, jobsPortletUser));
        assertFalse(StrutsPathAccess.canAccess(LANGUAGES_PATH, usersPortletUser));
        assertFalse(StrutsPathAccess.canAccess(SCHEDULER_PATH, languagesPortletUser));
    }

    /**
     * Method to test: {@link StrutsPathAccess#canAccess(String, User)}
     * Given: a back-end user whose only portlet is the beta id of an administrative tool
     * Expected: the directories that tool administers are allowed, since the beta portlet opens
     * the same legacy screens as the shipped one
     */
    @Test
    public void canAccess_betaPortletHolder_allowsTheSameDirectories() {
        assertTrue(StrutsPathAccess.canAccess(USERADMIN_PATH, usersBetaPortletUser));
        assertTrue(StrutsPathAccess.canAccess(ROLEADMIN_PATH, usersBetaPortletUser));
        assertFalse(StrutsPathAccess.canAccess(SCHEDULER_PATH, usersBetaPortletUser));
    }

    /**
     * Method to test: {@link StrutsPathAccess#canAccess(String, User)}
     * Given: a back-end user holding a portlet unrelated to an administrative directory
     * Expected: restricted, for every administrative directory
     */
    @Test
    public void canAccess_restrictedDirectory_refusesOtherPortletHolders() {
        assertFalse(StrutsPathAccess.canAccess(SCHEDULER_PATH, contentPortletUser));
        assertFalse(StrutsPathAccess.canAccess(SCHEDULER_PATH, siteSearchPortletUser));
        assertFalse(StrutsPathAccess.canAccess(MAINTENANCE_PATH, contentPortletUser));
        assertFalse(StrutsPathAccess.canAccess(USERADMIN_PATH, contentPortletUser));
        assertFalse(StrutsPathAccess.canAccess(USERFILTER_PATH, contentPortletUser));
        assertFalse(StrutsPathAccess.canAccess(ROLEADMIN_PATH, contentPortletUser));
        assertFalse(StrutsPathAccess.canAccess(LANGUAGES_PATH, contentPortletUser));
        assertFalse(StrutsPathAccess.canAccess(SITES_PATH, contentPortletUser));
    }

    /**
     * Method to test: {@link StrutsPathAccess#canAccess(String, User)}
     * Given: a CMS administrator
     * Expected: every administrative directory is allowed
     */
    @Test
    public void canAccess_restrictedDirectory_allowsAdministrator() {
        assertTrue(StrutsPathAccess.canAccess(SCHEDULER_PATH, adminUser));
        assertTrue(StrutsPathAccess.canAccess(MAINTENANCE_PATH, adminUser));
        assertTrue(StrutsPathAccess.canAccess(USERADMIN_PATH, adminUser));
        assertTrue(StrutsPathAccess.canAccess(USERFILTER_PATH, adminUser));
        assertTrue(StrutsPathAccess.canAccess(ROLEADMIN_PATH, adminUser));
        assertTrue(StrutsPathAccess.canAccess(LANGUAGES_PATH, adminUser));
        assertTrue(StrutsPathAccess.canAccess(SITES_PATH, adminUser));
    }

    /**
     * Method to test: {@link StrutsPathAccess#canAccess(String, User)}
     * Given: any legacy screen outside the administrative directories, which the other legacy
     * screens cross-link to
     * Expected: allowed for every back-end user, so no existing screen stops working
     */
    @Test
    public void canAccess_unrestrictedPath_isLeftAsItWas() {
        assertTrue(StrutsPathAccess.canAccess(UNRESTRICTED_PATH, siteSearchPortletUser));
        assertTrue(StrutsPathAccess.canAccess(UNRESTRICTED_PATH, jobsPortletUser));
        assertTrue(StrutsPathAccess.canAccess(LINKS_PATH, contentPortletUser));
    }

    /**
     * Method to test: {@link StrutsPathAccess#canAccess(String, User)}
     * Given: no user
     * Expected: restricted inside an administrative directory, and left alone outside one
     */
    @Test
    public void canAccess_noUser_isRestrictedOnlyInsideAnAdministrativeDirectory() {
        assertFalse(StrutsPathAccess.canAccess(SCHEDULER_PATH, null));
        assertTrue(StrutsPathAccess.canAccess(UNRESTRICTED_PATH, null));
    }

    /**
     * Method to test: {@link StrutsPathAccess#canAccess(String, User)}
     * Given: a path with no action directory, or none at all
     * Expected: left alone, since there is no directory to match
     */
    @Test
    public void canAccess_pathWithNoDirectory_isLeftAsItWas() {
        assertTrue(StrutsPathAccess.canAccess(null, adminUser));
        assertTrue(StrutsPathAccess.canAccess("/ext", contentPortletUser));
    }

    /**
     * Creates a back-end user holding exactly one portlet, through a layout of its own.
     *
     * @param label     a short label that makes the generated email address readable
     * @param portletId the id of the only portlet the user will hold
     * @return the persisted {@link User}
     * @throws DotDataException if the user, layout or role cannot be persisted
     */
    private static User backendUserWithPortlet(final String label, final String portletId)
            throws DotDataException {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User user = new UserDataGen()
                .emailAddress(label + System.nanoTime() + "@dotcms.com")
                .roles(roleAPI.loadBackEndUserRole())
                .nextPersisted();
        usersToClean.add(user);

        final Layout layout = new LayoutDataGen()
                .name(label + "-" + System.nanoTime())
                .portletIds(portletId)
                .nextPersisted();
        layoutsToClean.add(layout);

        roleAPI.addLayoutToRole(layout, roleAPI.getUserRole(user));
        return user;
    }
}
