package com.dotmarketing.portlets.user.ajax;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.ajax.DwrWebContextRule;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Integration tests for {@link UserAjax#hasUserRoles(String, String[])}: any logged-in back-end
 * user can check their own roles, while checking another user's roles takes an administrator or
 * access to both the Roles and the Users portlets.
 */
public class UserAjaxHasUserRolesIntegrationTest {

    @ClassRule
    public static final DwrWebContextRule dwrContext = new DwrWebContextRule();

    private static RoleAPI roleAPI;
    private static Role adminRole;
    private static Role backEndRole;
    private static User administrator;

    private static final List<User> usersToClean = new ArrayList<>();
    private static final List<Role> rolesToClean = new ArrayList<>();
    private static final List<Layout> layoutsToClean = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        roleAPI = APILocator.getRoleAPI();
        adminRole = roleAPI.loadCMSAdminRole();
        backEndRole = roleAPI.loadBackEndUserRole();
        administrator = track(new UserDataGen().roles(backEndRole, adminRole).nextPersisted());
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        usersToClean.forEach(UserDataGen::remove);
        rolesToClean.forEach(RoleDataGen::remove);
        // delete through a freshly loaded instance: the Hibernate session may already hold another
        // copy of the row, and deleting the data-gen instance would collide with it
        for (final Layout layout : layoutsToClean) {
            APILocator.getLayoutAPI().removeLayout(APILocator.getLayoutAPI().findLayout(layout.getId()));
        }
    }

    private static User track(final User user) {
        usersToClean.add(user);
        return user;
    }

    /**
     * Creates a back-end user who is not an administrator and whose tools hold the given portlets,
     * and makes them the logged-in DWR user.
     */
    private static User loggedInBackEndUserWithPortlets(final String... portletIds) {
        final User user;
        if (portletIds.length == 0) {
            user = track(new UserDataGen().roles(backEndRole).nextPersisted());
        } else {
            final Layout layout = new LayoutDataGen().portletIds(portletIds).nextPersisted();
            layoutsToClean.add(layout);
            final Role toolsRole = new RoleDataGen().layout(layout).nextPersisted();
            rolesToClean.add(toolsRole);
            user = track(new UserDataGen().roles(backEndRole, toolsRole).nextPersisted());
        }
        dwrContext.loginAs(user);
        return user;
    }

    /**
     * Method to test: {@link UserAjax#hasUserRoles(String, String[])}
     * Given Scenario: A logged-in back-end user with no portlets asks whether an administrator
     * holds the CMS Administrator role.
     * Expected Result: {@link DotSecurityException}.
     */
    @Test(expected = DotSecurityException.class)
    public void plainBackEndUser_checkingAnotherUser_isRejected() throws Exception {
        loggedInBackEndUserWithPortlets();

        new UserAjax().hasUserRoles(administrator.getUserId(), new String[] {adminRole.getId()});
    }

    /**
     * Method to test: {@link UserAjax#hasUserRoles(String, String[])}
     * Given Scenario: A logged-in back-end user with no portlets checks their own roles.
     * Expected Result: the answer for their own roles: they hold the back-end role, not the CMS
     * Administrator role.
     */
    @Test
    public void plainBackEndUser_checkingOwnRoles_getsTheAnswer() throws Exception {
        final User plain = loggedInBackEndUserWithPortlets();

        assertTrue(new UserAjax().hasUserRoles(plain.getUserId(), new String[] {backEndRole.getId()}));
        assertFalse(new UserAjax().hasUserRoles(plain.getUserId(), new String[] {adminRole.getId()}));
    }

    /**
     * Method to test: {@link UserAjax#hasUserRoles(String, String[])}
     * Given Scenario: A logged-in back-end user who is not an administrator but has both the Roles
     * and the Users portlets checks another user's roles.
     * Expected Result: the answer for that user.
     */
    @Test
    public void rolesAndUsersPortlets_checkingAnotherUser_getsTheAnswer() throws Exception {
        loggedInBackEndUserWithPortlets("roles", "users");

        assertTrue(new UserAjax().hasUserRoles(administrator.getUserId(), new String[] {adminRole.getId()}));
    }
}
