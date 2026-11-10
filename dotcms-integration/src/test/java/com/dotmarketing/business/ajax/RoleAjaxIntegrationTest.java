package com.dotmarketing.business.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.liferay.portal.util.WebKeys;
import com.liferay.portal.model.User;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The legacy DWR role-member listing applies the same privilege as the REST one: an
 * administrator, or a user with both the Roles and the Users portlets. The DWR context is
 * faked the way {@link PermissionAjaxIntegrationTest} does it and restored afterwards.
 */
public class RoleAjaxIntegrationTest {

    private static RoleAPI roleAPI;
    private static WebContextFactory.WebContextBuilder previousWebContextBuilder;
    private static final List<Role> rolesToClean = new ArrayList<>();
    private static final List<User> usersToClean = new ArrayList<>();
    private static final List<Layout> layoutsToClean = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        roleAPI = APILocator.getRoleAPI();
        previousWebContextBuilder = currentWebContextBuilder();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        attachWebContextBuilder(previousWebContextBuilder);
        usersToClean.forEach(UserDataGen::remove);
        Collections.reverse(rolesToClean);
        rolesToClean.forEach(RoleDataGen::remove);
        for (final Layout layout : layoutsToClean) {
            try {
                APILocator.getLayoutAPI().removeLayout(APILocator.getLayoutAPI().findLayout(layout.getId()));
            } catch (final Exception e) {
                Logger.warn(RoleAjaxIntegrationTest.class, "Unable to delete test layout: " + e.getMessage());
            }
        }
        // the deletions cleared the global role cache; warm the back-end role again for the
        // next suite class
        roleAPI.loadBackEndUserRole();
    }

    private static Role track(final Role role) {
        rolesToClean.add(role);
        return role;
    }

    private static User track(final User user) {
        usersToClean.add(user);
        return user;
    }

    private static WebContextFactory.WebContextBuilder currentWebContextBuilder() throws Exception {
        final Field builder = WebContextFactory.class.getDeclaredField("builder");
        builder.setAccessible(true);
        return (WebContextFactory.WebContextBuilder) builder.get(null);
    }

    private static void attachWebContextBuilder(final WebContextFactory.WebContextBuilder webContextBuilder) {
        final com.dotcms.repackage.org.directwebremoting.Container dwrContainer =
                mock(com.dotcms.repackage.org.directwebremoting.Container.class);
        when(dwrContainer.getBean(WebContextFactory.WebContextBuilder.class)).thenReturn(webContextBuilder);
        WebContextFactory.attach(dwrContainer);
    }

    /**
     * Makes DWR see the given user as the logged-in caller.
     */
    private static void callAs(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);
        final WebContextFactory.WebContextBuilder webContextBuilder =
                mock(WebContextFactory.WebContextBuilder.class);
        when(webContextBuilder.get()).thenReturn(webContext);
        attachWebContextBuilder(webContextBuilder);
    }

    /**
     * A back-end user with no portlets cannot list the users of a role.
     */
    @Test(expected = DotSecurityException.class)
    public void getUsersByRole_plainBackendUser_isRejected() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        track(new UserDataGen().roles(role).nextPersisted());
        final User plain = track(new UserDataGen().roles(roleAPI.loadBackEndUserRole()).nextPersisted());

        callAs(plain);
        new RoleAjax().getUsersByRole(new String[] {role.getId()});
    }

    /**
     * A back-end user with the Roles and Users portlets gets the members, as before.
     */
    @Test
    public void getUsersByRole_rolesAndUsersPortlets_returnsMembers() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        final User member = track(new UserDataGen().roles(role).nextPersisted());
        final Layout layout = new LayoutDataGen().portletIds("roles", "users").nextPersisted();
        final Role portletRole = track(new RoleDataGen().layout(layout).nextPersisted());
        final User delegate = track(new UserDataGen()
                .roles(roleAPI.loadBackEndUserRole(), portletRole).nextPersisted());

        callAs(delegate);
        final Map<String, List<Map<String, Object>>> usersByRole =
                new RoleAjax().getUsersByRole(new String[] {role.getId()});

        assertTrue(usersByRole.get(role.getId()).stream()
                .anyMatch(map -> member.getUserId().equals(map.get("userId"))));
    }

    /**
     * Method to test: {@link RoleAjax#getRole(String)}, through the Roles portlet check in
     * {@link DwrUtil#validateRolesPortletPermissions(User)}
     * Given Scenario: a back-end user whose only portlet is {@code roles} reads a role
     * ExpectedResult: the role comes back. This is the baseline for the {@code roles-legacy} case
     * below.
     */
    @Test
    public void getRole_rolesPortletHolder_returnsTheRole() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());

        callAs(backendUserWithOnly(PortletID.ROLES.toString()));

        assertEquals(role.getId(), new RoleAjax().getRole(role.getId()).get("id"));
    }

    /**
     * Method to test: {@link RoleAjax#getRole(String)}, through the Roles portlet check in
     * {@link DwrUtil#validateRolesPortletPermissions(User)}
     * Given Scenario: a back-end user whose only portlet is the old Roles &amp; Tools screen, kept as
     * {@code roles-legacy} after the Angular portlet took over {@code roles}, reads a role
     * ExpectedResult: the role comes back, exactly as for a holder of {@code roles}, so a role that
     * kept the old screen can still use it
     */
    @Test
    public void getRole_rolesLegacyPortletHolder_returnsTheRole() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());

        callAs(backendUserWithOnly(PortletID.ROLES_LEGACY.toString()));

        assertEquals(role.getId(), new RoleAjax().getRole(role.getId()).get("id"));
    }

    /**
     * Creates a back-end user whose only portlet is the given one, through a layout and role of
     * their own, all tracked for clean-up.
     *
     * @param portletId the id of the only portlet the user will hold
     * @return the persisted {@link User}
     * @throws DotDataException if the back-end role cannot be loaded
     */
    private static User backendUserWithOnly(final String portletId) throws DotDataException {
        final Layout layout = new LayoutDataGen().portletIds(portletId).nextPersisted();
        layoutsToClean.add(layout);
        final Role portletRole = track(new RoleDataGen().layout(layout).nextPersisted());
        return track(new UserDataGen().roles(roleAPI.loadBackEndUserRole(), portletRole).nextPersisted());
    }
}
