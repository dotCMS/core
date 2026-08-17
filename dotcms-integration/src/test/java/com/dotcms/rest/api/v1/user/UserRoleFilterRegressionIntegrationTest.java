package com.dotcms.rest.api.v1.user;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Locks down the role filtering behavior of {@link com.dotcms.util.pagination.UserPaginator}
 * and its consumers before and after the role filter is switched from role_key to role_id
 * matching. See https://github.com/dotCMS/core/issues/37070
 *
 * The regression tests must be green BEFORE the filter change and stay green unchanged after
 * it. The keyless-role test documents the new behavior and is red before the change.
 *
 * @author hassandotcms
 */
public class UserRoleFilterRegressionIntegrationTest {

    static HttpServletResponse response;
    static UserResource resource;
    static UserAPI userAPI;
    static RoleAPI roleAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new UserResource();
        userAPI = APILocator.getUserAPI();
        roleAPI = APILocator.getRoleAPI();
        response = new MockHttpResponse();
    }

    private static HttpServletRequest mockAdminRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(
                                new MockHttpRequestIntegrationTest("localhost", "/").request())
                                .request())
                        .request());
        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));
        return request;
    }

    @SuppressWarnings("unchecked")
    private static List<String> filterEndpointUserIds(final String filter, final List<String> roleKeys) {
        final Response restResponse = resource.filter(mockAdminRequest(), response,
                filter, 0, 40, null, "ASC", false, false, null, 0, roleKeys);
        assertEquals(Response.Status.OK.getStatusCode(), restResponse.getStatus());
        final List<Map<String, Object>> users =
                (List<Map<String, Object>>) ((ResponseEntityView<Object>) restResponse.getEntity()).getEntity();
        return users.stream().map(map -> map.get("userId").toString()).collect(Collectors.toList());
    }

    /**
     * Regression: GET /v1/users/filter?roleKey= keeps returning exactly the granted users.
     */
    @Test
    public void filterEndpoint_byRoleKey_returnsGrantedUsers() throws Exception {
        final String unique = "rkFilter" + System.currentTimeMillis();
        final Role role = new RoleDataGen().key(unique + "Key").nextPersisted();
        final User granted = new UserDataGen().firstName(unique).roles(role).nextPersisted();
        final User notGranted = new UserDataGen().firstName(unique).nextPersisted();

        final List<String> userIds = filterEndpointUserIds(unique, List.of(role.getRoleKey()));
        assertTrue("granted user must be returned", userIds.contains(granted.getUserId()));
        assertFalse("user without the role must not be returned",
                userIds.contains(notGranted.getUserId()));
    }

    /**
     * Regression: multiple role keys return users holding any of them.
     */
    @Test
    public void filterEndpoint_multipleRoleKeys_returnsUnion() throws Exception {
        final String unique = "rkUnion" + System.currentTimeMillis();
        final Role roleA = new RoleDataGen().key(unique + "A").nextPersisted();
        final Role roleB = new RoleDataGen().key(unique + "B").nextPersisted();
        final User userA = new UserDataGen().firstName(unique).roles(roleA).nextPersisted();
        final User userB = new UserDataGen().firstName(unique).roles(roleB).nextPersisted();

        final List<String> userIds = filterEndpointUserIds(unique,
                List.of(roleA.getRoleKey(), roleB.getRoleKey()));
        assertTrue(userIds.contains(userA.getUserId()));
        assertTrue(userIds.contains(userB.getUserId()));
    }

    /**
     * Regression: the API-level list and count stay consistent when filtering by roles,
     * and only direct grants match.
     */
    @Test
    public void getUsersByName_withRoles_listAndCountConsistent() throws Exception {
        final String unique = "rkCount" + System.currentTimeMillis();
        final Role role = new RoleDataGen().key(unique + "Key").nextPersisted();
        new UserDataGen().firstName(unique).roles(role).nextPersisted();
        new UserDataGen().firstName(unique).roles(role).nextPersisted();
        new UserDataGen().firstName(unique).nextPersisted();

        final List<User> users = userAPI.getUsersByName(unique, List.of(role), 0, 40);
        assertEquals(2, users.size());
        assertEquals(2, userAPI.getCountUsersByName(unique, List.of(role)));
    }

    /**
     * Regression: the exact roles parameter /v1/users/loginAsData passes (the back-end user
     * role) keeps matching users granted that role.
     */
    @Test
    public void backEndUserRole_viaRolesParam_matches() throws Exception {
        final String unique = "rkBackend" + System.currentTimeMillis();
        final Role backendRole = roleAPI.loadBackEndUserRole();
        final User backendUser = new UserDataGen().firstName(unique).roles(backendRole).nextPersisted();
        final User plainUser = new UserDataGen().firstName(unique).nextPersisted();

        final List<String> userIds = userAPI.getUsersByName(unique, List.of(backendRole), 0, 40)
                .stream().map(User::getUserId).collect(Collectors.toList());
        assertTrue(userIds.contains(backendUser.getUserId()));
        assertFalse(userIds.contains(plainUser.getUserId()));
    }

    /**
     * Guard: a hand-built Role carrying only a roleKey (no id) keeps matching. This works
     * today through key binding and must keep working after the switch to id binding via
     * the defensive key-to-id resolution.
     */
    @Test
    public void handBuiltRole_keyOnly_stillMatches() throws Exception {
        final String unique = "rkHand" + System.currentTimeMillis();
        final Role persisted = new RoleDataGen().key(unique + "Key").nextPersisted();
        final User granted = new UserDataGen().firstName(unique).roles(persisted).nextPersisted();

        final Role keyOnly = new Role();
        keyOnly.setRoleKey(persisted.getRoleKey());

        final List<String> userIds = userAPI.getUsersByName(unique, List.of(keyOnly), 0, 40)
                .stream().map(User::getUserId).collect(Collectors.toList());
        assertTrue("a key-only Role object must keep matching",
                userIds.contains(granted.getUserId()));
    }

    /**
     * New behavior (red before the filter switch): a role WITHOUT a roleKey passed through
     * the roles parameter matches its directly granted users. This is the gap that blocks
     * GET /v1/roles/roleId/users for keyless roles.
     */
    @Test
    public void keylessRole_viaRolesParam_returnsGrantedUsers() throws Exception {
        final String unique = "rkKeyless" + System.currentTimeMillis();
        final Role keylessRole = new RoleDataGen().key(null).nextPersisted();
        final User granted = new UserDataGen().firstName(unique).roles(keylessRole).nextPersisted();
        new UserDataGen().firstName(unique).nextPersisted();

        final List<String> userIds = userAPI.getUsersByName(unique, List.of(keylessRole), 0, 40)
                .stream().map(User::getUserId).collect(Collectors.toList());
        assertTrue("users granted a keyless role must be returned",
                userIds.contains(granted.getUserId()));
        assertEquals(1, userAPI.getCountUsersByName(unique, List.of(keylessRole)));
    }
}
