package com.dotmarketing.business.ajax;

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
import com.dotmarketing.exception.DotSecurityException;
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
}
