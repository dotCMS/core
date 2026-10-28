package com.dotcms.rest.api.v1.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.internal.util.Base64;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Changing a user's roles through {@code POST}/{@code PUT /api/v1/users} requires a CMS
 * Administrator, even for a user who has the Users and Roles tools.
 */
public class UserResourceRoleChangeIntegrationTest {

    private static final String MANAGER_PASSWORD = "Passw0rd!";

    private static HttpServletResponse response;
    private static UserResource resource;
    private static Host host;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new UserResource();
        host = new SiteDataGen().nextPersisted();
        response = new MockHttpResponse();
    }

    private static HttpServletRequest requestAs(final String email, final String password) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest(host.getHostname(), "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode((email + ":" + password).getBytes())));
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST, host);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID,
                host.getIdentifier());
        return request;
    }

    /**
     * Creates a back-end user who is not a CMS Administrator but has the Users and Roles tools.
     */
    private static User newUserManager() throws Exception {
        final String suffix = String.valueOf(System.nanoTime());
        final Layout layout = new LayoutDataGen().name("Users and Roles " + suffix)
                .portletIds("users", "roles").nextPersisted();
        final Role toolsRole = new RoleDataGen().layout(layout).nextPersisted();
        final User manager = new UserDataGen().firstName("Manager").lastName("User" + suffix)
                .emailAddress("usermanager" + suffix + "@dotcms.com").password(MANAGER_PASSWORD)
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(), toolsRole).nextPersisted();
        assertFalse("precondition: the caller must not be a CMS Administrator", manager.isAdmin());
        return manager;
    }

    private static Set<String> directRoleIds(final User user) throws Exception {
        return APILocator.getRoleAPI().loadRolesForUser(user.getUserId(), false).stream()
                .map(Role::getId).collect(Collectors.toSet());
    }

    private static UserForm.Builder formFor(final User user) {
        return new UserForm.Builder().userId(user.getUserId()).firstName(user.getFirstName())
                .lastName(user.getLastName()).email(user.getEmailAddress())
                .password(MANAGER_PASSWORD.toCharArray());
    }

    /**
     * Method to test: {@link UserResource#udpate(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: A user with the Users and Roles tools who is not a CMS Administrator updates
     * their own account sending the CMS Administrator role.
     * Expected Result: {@link DotSecurityException}; the user is still not a CMS Administrator and
     * their roles are unchanged.
     */
    @Test
    public void test_update_nonAdminAddingAdministratorRole_forbidden() throws Exception {
        final User manager = newUserManager();
        final Set<String> before = directRoleIds(manager);

        assertThrows(DotSecurityException.class, () -> resource.udpate(
                requestAs(manager.getEmailAddress(), MANAGER_PASSWORD), response,
                formFor(manager).roles(List.of(Role.CMS_ADMINISTRATOR_ROLE)).build()));

        assertFalse(APILocator.getUserAPI().loadUserById(manager.getUserId()).isAdmin());
        assertEquals(before, directRoleIds(manager));
    }

    /**
     * Method to test: {@link UserResource#udpate(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: A user with the Users and Roles tools who is not a CMS Administrator changes
     * only their own first name and sends no roles.
     * Expected Result: 200; the name is updated and the roles are not changed.
     */
    @Test
    public void test_update_nonAdminWithoutRoles_succeeds() throws Exception {
        final User manager = newUserManager();
        final Set<String> before = directRoleIds(manager);

        final Response result = resource.udpate(
                requestAs(manager.getEmailAddress(), MANAGER_PASSWORD), response,
                formFor(manager).firstName("Renamed").build());

        assertEquals(Status.OK.getStatusCode(), result.getStatus());
        assertEquals("Renamed",
                APILocator.getUserAPI().loadUserById(manager.getUserId()).getFirstName());
        assertTrue(directRoleIds(manager).containsAll(before));
    }

    /**
     * Method to test: {@link UserResource#udpate(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: A CMS Administrator adds a role by key to a user.
     * Expected Result: 200; the user holds the role.
     */
    @Test
    public void test_update_adminAddingRole_succeeds() throws Exception {
        final User target = new UserDataGen().roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .password(MANAGER_PASSWORD).nextPersisted();
        final Role role = new RoleDataGen().key("rolechange" + System.nanoTime()).nextPersisted();
        final User admin = TestUserUtils.getAdminUser();

        final Response result = resource.udpate(requestAs(admin.getEmailAddress(), "admin"), response,
                formFor(target).roles(List.of(role.getRoleKey())).build());

        assertEquals(Status.OK.getStatusCode(), result.getStatus());
        assertTrue(APILocator.getRoleAPI().doesUserHaveRole(
                APILocator.getUserAPI().loadUserById(target.getUserId()), role));
    }

    /**
     * Method to test: {@link UserResource#create(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: A user with the Users and Roles tools who is not a CMS Administrator creates a user.
     * Expected Result: {@link ForbiddenException}; nothing is created.
     */
    @Test
    public void test_create_nonAdmin_forbidden() throws Exception {
        final User manager = newUserManager();
        final String email = "created" + System.nanoTime() + "@dotcms.com";

        assertThrows(ForbiddenException.class, () -> resource.create(
                requestAs(manager.getEmailAddress(), MANAGER_PASSWORD), response,
                new UserForm.Builder().firstName("New").lastName("User").email(email)
                        .password(MANAGER_PASSWORD.toCharArray()).build()));

        assertFalse(APILocator.getUserAPI().userExistsWithEmail(email));
    }

    /**
     * Method to test: {@link UserResource#create(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: A CMS Administrator creates a user.
     * Expected Result: 200 and the user exists.
     */
    @Test
    public void test_create_admin_succeeds() throws Exception {
        final User admin = TestUserUtils.getAdminUser();
        final String email = "created" + System.nanoTime() + "@dotcms.com";

        final Response result = resource.create(requestAs(admin.getEmailAddress(), "admin"), response,
                new UserForm.Builder().firstName("New").lastName("User").email(email)
                        .password(MANAGER_PASSWORD.toCharArray()).build());

        assertEquals(Status.OK.getStatusCode(), result.getStatus());
        assertNotNull(result.getEntity());
    }
}
