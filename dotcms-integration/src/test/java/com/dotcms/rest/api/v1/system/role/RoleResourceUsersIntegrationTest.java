package com.dotcms.rest.api.v1.system.role;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityPaginatedDataView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@code GET /v1/roles/{roleid}/users}: the paginated list of users
 * directly granted a role. See https://github.com/dotCMS/core/issues/37070
 *
 * Created roles and users are tracked and removed once in {@link #cleanUp()} instead of
 * per-test: every role deletion clears the global role cache (and user deletion cascades
 * through the user's own role), which can race the REST auth check of the next test in the
 * class and surface as a spurious 401. Class-level cleanup leaves the suite just as clean
 * without interleaving deletions between REST calls.
 *
 * @author hassandotcms
 */
public class RoleResourceUsersIntegrationTest {

    static HttpServletResponse response;
    static RoleResource resource;
    static RoleAPI roleAPI;

    private static final List<Role> rolesToClean = new ArrayList<>();
    private static final List<User> usersToClean = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new RoleResource();
        roleAPI = APILocator.getRoleAPI();
        response = new MockHttpResponse();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        usersToClean.forEach(UserDataGen::remove);
        // children were created after their parents, so remove in reverse creation order
        Collections.reverse(rolesToClean);
        rolesToClean.forEach(RoleDataGen::remove);
        // the deletions above cleared the global role cache; resolve the back-end role again
        // so the next suite class's REST auth check never starts against a cold cache
        APILocator.getRoleAPI().loadBackEndUserRole();
    }

    private static Role track(final Role role) {
        rolesToClean.add(role);
        return role;
    }

    private static User track(final User user) {
        usersToClean.add(user);
        return user;
    }

    private static MockSessionRequest baseRequest() {
        return new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request());
    }

    private static HttpServletRequest mockAdminRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(baseRequest().request());
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        return request;
    }

    private static ResponseEntityPaginatedDataView loadUsers(final String roleId,
            final String filter, final int page, final int perPage) throws Exception {
        return resource.loadUsersByRoleId(mockAdminRequest(), new MockHttpResponse(),
                roleId, filter, page, perPage, null, "ASC");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> users(final ResponseEntityPaginatedDataView view) {
        return (List<Map<String, Object>>) view.getEntity();
    }

    private static List<String> userIds(final ResponseEntityPaginatedDataView view) {
        return users(view).stream().map(map -> map.get("userId").toString())
                .collect(Collectors.toList());
    }

    /**
     * Directly granted users come back with the standard user detail fields, email included.
     */
    @Test
    public void directGrants_returnUserDetailFields() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        final User userA = track(new UserDataGen().roles(role).nextPersisted());
        final User userB = track(new UserDataGen().roles(role).nextPersisted());

        final ResponseEntityPaginatedDataView view = loadUsers(role.getId(), null, 1, 40);
        assertEquals(2, view.getPagination().getTotalEntries());
        assertTrue(userIds(view).containsAll(List.of(userA.getUserId(), userB.getUserId())));
        for (final Map<String, Object> userMap : users(view)) {
            assertTrue("emailAddress must be present",
                    userMap.get("emailAddress").toString().contains("@"));
            assertFalse(userMap.get("firstName").toString().isEmpty());
            assertFalse(userMap.get("lastName").toString().isEmpty());
        }
    }

    /**
     * The headline regression: a role WITHOUT a roleKey returns its granted users.
     */
    @Test
    public void keylessRole_returnsUsers() throws Exception {
        final Role keylessRole = track(new RoleDataGen().key(null).nextPersisted());
        final User granted = track(new UserDataGen().roles(keylessRole).nextPersisted());

        final ResponseEntityPaginatedDataView view = loadUsers(keylessRole.getId(), null, 1, 40);
        assertEquals(1, view.getPagination().getTotalEntries());
        assertTrue(userIds(view).contains(granted.getUserId()));
    }

    /**
     * Inheritance is a client concern: users granted only on an ancestor are not returned
     * for the descendant role.
     */
    @Test
    public void ancestorGrants_notIncluded() throws Exception {
        final Role parent = track(new RoleDataGen().nextPersisted());
        final Role child = track(new RoleDataGen().parent(parent.getId()).nextPersisted());
        track(new UserDataGen().roles(parent).nextPersisted());

        final ResponseEntityPaginatedDataView view = loadUsers(child.getId(), null, 1, 40);
        assertEquals(0, view.getPagination().getTotalEntries());
        assertTrue(users(view).isEmpty());
    }

    /**
     * A user-role is served uniformly: it returns the user themself via the creation-time
     * self-grant row.
     */
    @Test
    public void userRole_returnsSelfGrant() throws Exception {
        final User user = track(new UserDataGen().nextPersisted());
        final Role userRole = roleAPI.getUserRole(user);

        final ResponseEntityPaginatedDataView view = loadUsers(userRole.getId(), null, 1, 40);
        assertEquals(1, view.getPagination().getTotalEntries());
        assertEquals(user.getUserId(), userIds(view).get(0));
    }

    /**
     * The filter matches on name and on email address.
     */
    @Test
    public void filter_matchesNameAndEmail() throws Exception {
        final String unique = "roleusers" + System.currentTimeMillis();
        final Role role = track(new RoleDataGen().nextPersisted());
        final User byName = track(new UserDataGen().firstName(unique + "Alpha").roles(role).nextPersisted());
        final User byEmail = track(new UserDataGen()
                .emailAddress(unique + "beta@filter.test").roles(role).nextPersisted());

        final List<String> nameMatches = userIds(loadUsers(role.getId(), unique + "Alpha", 1, 40));
        assertEquals(List.of(byName.getUserId()), nameMatches);

        final List<String> emailMatches = userIds(loadUsers(role.getId(), unique + "beta", 1, 40));
        assertEquals(List.of(byEmail.getUserId()), emailMatches);
    }

    /**
     * The direction parameter orders the listing. Without an orderBy the listing sorts by
     * full name, so DESC must return the exact reverse of ASC.
     */
    @Test
    public void direction_ordersTheListing() throws Exception {
        final String unique = "roleorder" + System.currentTimeMillis();
        final Role role = track(new RoleDataGen().nextPersisted());
        final User first = track(new UserDataGen().firstName("Aaa" + unique).roles(role).nextPersisted());
        final User last = track(new UserDataGen().firstName("Zzz" + unique).roles(role).nextPersisted());

        final List<String> ascending = userIds(resource.loadUsersByRoleId(mockAdminRequest(),
                new MockHttpResponse(), role.getId(), unique, 1, 40, null, "ASC"));
        assertEquals(List.of(first.getUserId(), last.getUserId()), ascending);

        final List<String> descending = userIds(resource.loadUsersByRoleId(mockAdminRequest(),
                new MockHttpResponse(), role.getId(), unique, 1, 40, null, "DESC"));
        assertEquals("DESC must reverse the listing",
                List.of(last.getUserId(), first.getUserId()), descending);
    }

    /**
     * Regression guard for #37458, which teaches the shared user query to sort by the Users portlet fields:
     * the members listing must honor {@code orderby} and {@code direction} while still returning only the
     * role's direct grants, and its order without an orderBy must stay the historical full-name ascending.
     * The emails are seeded in the opposite order of the names, so the {@code emailAddress} assertion only
     * passes when the sort field is actually applied -- a name-based fallback fails it.
     */
    @Test
    public void orderByFirstName_ordersMembersAndStaysRoleScoped() throws Exception {
        final String unique = "rolesort" + System.currentTimeMillis();
        final Role role = track(new RoleDataGen().nextPersisted());
        final User first = track(new UserDataGen().firstName("Aaa" + unique)
                .emailAddress(unique + "2@rolesort.dotcms.com").roles(role).nextPersisted());
        final User last = track(new UserDataGen().firstName("Zzz" + unique)
                .emailAddress(unique + "1@rolesort.dotcms.com").roles(role).nextPersisted());
        final User outsider = track(new UserDataGen().firstName("Mmm" + unique)
                .emailAddress(unique + "0@rolesort.dotcms.com").nextPersisted());

        final List<String> descending = userIds(resource.loadUsersByRoleId(mockAdminRequest(),
                new MockHttpResponse(), role.getId(), unique, 1, 40, "firstName", "DESC"));
        assertEquals("firstName DESC must order the members", List.of(last.getUserId(), first.getUserId()),
                descending);
        assertFalse("a user outside the role must never be listed", descending.contains(outsider.getUserId()));

        final List<String> byEmail = userIds(resource.loadUsersByRoleId(mockAdminRequest(),
                new MockHttpResponse(), role.getId(), unique, 1, 40, "emailAddress", "ASC"));
        assertEquals("emailAddress ASC must follow the email column, not the name",
                List.of(last.getUserId(), first.getUserId()), byEmail);
        assertFalse("a user outside the role must never be listed", byEmail.contains(outsider.getUserId()));

        final List<String> byDefault = userIds(resource.loadUsersByRoleId(mockAdminRequest(),
                new MockHttpResponse(), role.getId(), unique, 1, 40, null, "ASC"));
        assertEquals("default order must stay full-name ascending", List.of(first.getUserId(), last.getUserId()),
                byDefault);
    }

    /**
     * Pagination boundaries: pages split the result set, the total stays constant, and a
     * page past the end is empty.
     */
    @Test
    public void pagination_boundaries() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        track(new UserDataGen().roles(role).nextPersisted());
        track(new UserDataGen().roles(role).nextPersisted());
        track(new UserDataGen().roles(role).nextPersisted());

        final ResponseEntityPaginatedDataView firstPage = loadUsers(role.getId(), null, 1, 2);
        assertEquals(2, users(firstPage).size());
        assertEquals(3, firstPage.getPagination().getTotalEntries());

        final ResponseEntityPaginatedDataView secondPage = loadUsers(role.getId(), null, 2, 2);
        assertEquals(1, users(secondPage).size());
        assertEquals(3, secondPage.getPagination().getTotalEntries());

        final ResponseEntityPaginatedDataView pastEnd = loadUsers(role.getId(), null, 5, 2);
        assertTrue(users(pastEnd).isEmpty());
    }

    /**
     * A missing role resolves to 404 through the DoesNotExistException mapper.
     */
    @Test(expected = DoesNotExistException.class)
    public void missingRole_throwsDoesNotExist() throws Exception {
        loadUsers(UUIDGenerator.generateUuid(), null, 1, 40);
    }

    /**
     * A backend user without access to the roles portlet is rejected.
     */
    @Test(expected = com.dotcms.rest.exception.SecurityException.class)
    public void backendUserWithoutRolesPortlet_isRejected() throws Exception {
        final Role role = track(new RoleDataGen().nextPersisted());
        final User limited = track(new UserDataGen()
                .roles(roleAPI.loadBackEndUserRole()).nextPersisted());

        final MockSessionRequest request = baseRequest();
        request.getSession().setAttribute(
                com.liferay.portal.util.WebKeys.USER_ID, limited.getUserId());

        resource.loadUsersByRoleId(request.request(), new MockHttpResponse(),
                role.getId(), null, 1, 40, null, "ASC");
    }
}
