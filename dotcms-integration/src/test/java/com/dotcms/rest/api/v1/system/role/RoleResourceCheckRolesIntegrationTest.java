package com.dotcms.rest.api.v1.system.role;

import static org.junit.Assert.assertEquals;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link RoleResource#checkRoles}
 * ({@code GET /v1/roles/checkuserroles/userid/{userId}/roleids/{roleIds}}): any back-end user can
 * check their own roles, while checking another user's roles takes an administrator or access to
 * both the Roles and the Users portlets.
 */
public class RoleResourceCheckRolesIntegrationTest {

    private static RoleResource resource;
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
        resource = new RoleResource();
        roleAPI = APILocator.getRoleAPI();
        adminRole = roleAPI.loadCMSAdminRole();
        backEndRole = roleAPI.loadBackEndUserRole();
        administrator = track(new UserDataGen().roles(backEndRole, adminRole).nextPersisted());
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        usersToClean.forEach(UserDataGen::remove);
        Collections.reverse(rolesToClean);
        rolesToClean.forEach(RoleDataGen::remove);
        // delete through a freshly loaded instance: the Hibernate session may already hold another
        // copy of the row, and deleting the data-gen instance would collide with it
        for (final Layout layout : layoutsToClean) {
            APILocator.getLayoutAPI().removeLayout(APILocator.getLayoutAPI().findLayout(layout.getId()));
        }
        // the deletions above cleared the role cache; load the back-end role again so the next
        // class's REST auth check does not start against a cold cache
        APILocator.getRoleAPI().loadBackEndUserRole();
    }

    private static User track(final User user) {
        usersToClean.add(user);
        return user;
    }

    /**
     * Creates a back-end user who is not an administrator and whose tools hold the given portlets.
     */
    private static User backEndUserWithPortlets(final String... portletIds) {
        if (portletIds.length == 0) {
            return track(new UserDataGen().roles(backEndRole).nextPersisted());
        }
        final Layout layout = new LayoutDataGen().portletIds(portletIds).nextPersisted();
        layoutsToClean.add(layout);
        final Role toolsRole = new RoleDataGen().layout(layout).nextPersisted();
        rolesToClean.add(toolsRole);
        return track(new UserDataGen().roles(backEndRole, toolsRole).nextPersisted());
    }

    private static MockSessionRequest baseRequest() {
        return new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request());
    }

    private static HttpServletRequest requestFor(final User user) {
        final MockSessionRequest request = baseRequest();
        request.getSession().setAttribute(com.liferay.portal.util.WebKeys.USER_ID, user.getUserId());
        return request.request();
    }

    private static HttpServletRequest adminRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(baseRequest().request());
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        return request;
    }

    @SuppressWarnings("unchecked")
    private static Object checkRolesValue(final Response response) {
        final Map<String, Object> entity =
                ((ResponseEntityRoleOperationView) response.getEntity()).getEntity();
        return entity.get("checkRoles");
    }

    /**
     * Method to test: {@link RoleResource#checkRoles}
     * Given Scenario: A back-end user with no portlets asks whether an administrator holds the CMS
     * Administrator role.
     * Expected Result: {@link ForbiddenException}.
     */
    @Test(expected = ForbiddenException.class)
    public void plainBackEndUser_checkingAnotherUser_isForbidden() throws Exception {
        final User plain = backEndUserWithPortlets();

        resource.checkRoles(requestFor(plain), new MockHttpResponse(),
                administrator.getUserId(), adminRole.getId());
    }

    /**
     * Method to test: {@link RoleResource#checkRoles}
     * Given Scenario: A back-end user with no portlets asks about a user id that does not exist.
     * Expected Result: {@link ForbiddenException}, the same answer as for an existing user.
     */
    @Test(expected = ForbiddenException.class)
    public void plainBackEndUser_checkingUnknownUser_isForbidden() throws Exception {
        final User plain = backEndUserWithPortlets();

        resource.checkRoles(requestFor(plain), new MockHttpResponse(),
                "user-id-" + UUIDGenerator.generateUuid(), adminRole.getId());
    }

    /**
     * Method to test: {@link RoleResource#checkRoles}
     * Given Scenario: A back-end user with the Roles portlet but not the Users portlet checks
     * another user's roles.
     * Expected Result: {@link ForbiddenException}: both portlets are required.
     */
    @Test(expected = ForbiddenException.class)
    public void rolesPortletOnly_checkingAnotherUser_isForbidden() throws Exception {
        final User rolesOnly = backEndUserWithPortlets("roles");

        resource.checkRoles(requestFor(rolesOnly), new MockHttpResponse(),
                administrator.getUserId(), adminRole.getId());
    }

    /**
     * Method to test: {@link RoleResource#checkRoles}
     * Given Scenario: A back-end user with no portlets checks their own roles.
     * Expected Result: the answer for their own roles: they hold the back-end role, not the CMS
     * Administrator role.
     */
    @Test
    public void plainBackEndUser_checkingOwnRoles_getsTheAnswer() throws Exception {
        final User plain = backEndUserWithPortlets();

        assertEquals(Boolean.TRUE, checkRolesValue(resource.checkRoles(requestFor(plain),
                new MockHttpResponse(), plain.getUserId(), backEndRole.getId())));
        assertEquals(Boolean.FALSE, checkRolesValue(resource.checkRoles(requestFor(plain),
                new MockHttpResponse(), plain.getUserId(), adminRole.getId())));
    }

    /**
     * Method to test: {@link RoleResource#checkRoles}
     * Given Scenario: A back-end user who is not an administrator but has both the Roles and the
     * Users portlets checks another user's roles.
     * Expected Result: the answer for that user.
     */
    @Test
    public void rolesAndUsersPortlets_checkingAnotherUser_getsTheAnswer() throws Exception {
        final User delegate = backEndUserWithPortlets("roles", "users");

        assertEquals(Boolean.TRUE, checkRolesValue(resource.checkRoles(requestFor(delegate),
                new MockHttpResponse(), administrator.getUserId(), adminRole.getId())));
    }

    /**
     * Method to test: {@link RoleResource#checkRoles}
     * Given Scenario: An administrator checks another user's roles.
     * Expected Result: the answer for that user.
     */
    @Test
    public void administrator_checkingAnotherUser_getsTheAnswer() throws Exception {
        final User plain = backEndUserWithPortlets();

        assertEquals(Boolean.FALSE, checkRolesValue(resource.checkRoles(adminRequest(),
                new MockHttpResponse(), plain.getUserId(), adminRole.getId())));
    }
}
