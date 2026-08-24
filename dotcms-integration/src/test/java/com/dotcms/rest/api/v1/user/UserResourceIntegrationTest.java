package com.dotcms.rest.api.v1.user;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotmarketing.business.RoleAPI;
import com.liferay.portal.ejb.UserTestUtil;
import java.util.Collections;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.DotRestInstanceProvider;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.UserPaginator;
import com.dotmarketing.business.ApiProvider;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserResourceIntegrationTest {

    static HttpServletResponse response;
    static HttpServletRequest request;
    static UserResource resource;
    static User user;
    static Host host;
    static User adminUser;

    // Fixtures from the roleKey filter tests, removed once after the class: deleting
    // roles/users between tests clears the global role cache and can race the next
    // test's REST auth check into a spurious 401.
    private static final List<Role> rolesToClean = new ArrayList<>();
    private static final List<User> usersToClean = new ArrayList<>();

    @AfterClass
    public static void cleanUpFilterFixtures() throws Exception {
        usersToClean.forEach(UserDataGen::remove);
        rolesToClean.forEach(RoleDataGen::remove);
        // the deletions above cleared the global role cache; resolve the back-end role again
        // so the next suite class's REST auth check never starts against a cold cache
        APILocator.getRoleAPI().loadBackEndUserRole();
    }

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        // Create resource with 4-parameter constructor
        resource = new UserResource(new WebResource(new ApiProvider()), UserResourceHelper.getInstance(),
                new PaginationUtil(new UserPaginator()), new DotRestInstanceProvider()
                        .setUserAPI(APILocator.getUserAPI())
                        .setHostAPI(APILocator.getHostAPI())
                        .setRoleAPI(APILocator.getRoleAPI())
                        .setErrorHelper(ErrorResponseHelper.INSTANCE));

        adminUser = TestUserUtils.getAdminUser();
        host = new SiteDataGen().nextPersisted();
        user = TestUserUtils.getChrisPublisherUser(host);
        response = new MockHttpResponse();

        //Check if role has any layout, if is empty add one
        if(APILocator.getLayoutAPI().loadLayoutsForUser(user).isEmpty()) {
            APILocator.getRoleAPI()
                    .addLayoutToRole(APILocator.getLayoutAPI().findAllLayouts().get(0),
                            APILocator.getRoleAPI().getUserRole(user));
        }
        //Add permissions to the host
        final Permission readPermissionsPermission = new Permission( host.getPermissionId(),
                APILocator.getRoleAPI().getUserRole(user).getId(), PermissionAPI.PERMISSION_READ, true );
        APILocator.getPermissionAPI().save(readPermissionsPermission,host,adminUser,false);
    }

    private static HttpServletRequest mockRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest(host.getHostname(), "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString("admin@dotcms.com:admin".getBytes()));

        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST,host);
        request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID,host.getIdentifier());

        return request;
    }

    private void loginAs() throws Exception {
        final LoginAsForm loginAsForm = new LoginAsForm.Builder().userId(user.getUserId()).build();
        request = mockRequest();
        final Response resourceResponse = resource.loginAs(request,response,loginAsForm);
        assertNotNull(resourceResponse);
        assertEquals(Status.OK.getStatusCode(),resourceResponse.getStatus());
        assertEquals(user.getUserId(),request.getSession().getAttribute(WebKeys.USER_ID));
        assertNull(request.getSession().getAttribute(WebKeys.USER));
        assertEquals(adminUser.getUserId(),request.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID));
    }

    @Test
    public void test_loginAs_success() throws Exception{
        loginAs();
    }

    @Test
    public void test_logoutAs_success() throws Exception {
        loginAs();
        final Response resourceResponse = resource.logoutAs(request,response);
        assertNotNull(resourceResponse);
        assertEquals(Status.OK.getStatusCode(),resourceResponse.getStatus());
        assertEquals(adminUser.getUserId(),request.getSession().getAttribute(WebKeys.USER_ID));
        assertNull(request.getSession().getAttribute(WebKeys.USER));
        assertNull(request.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID));
    }

    @SuppressWarnings("unchecked")
    private List<String> filterUserIdsByRoleKeys(final String filter, final List<String> roleKeys) {
        final Response resourceResponse = resource.filter(mockRequest(), response, filter, 0, 40,
                null, "ASC", false, false, null, 0, roleKeys);
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());
        final List<Map<String, Object>> userMaps = (List<Map<String, Object>>)
                ((ResponseEntityView<Object>) resourceResponse.getEntity()).getEntity();
        return userMaps.stream().map(map -> map.get("userId").toString())
                .collect(Collectors.toList());
    }

    /**
     * Method to test: {@link UserResource#filter}
     * Given Scenario: A role with a roleKey has one user directly granted to it; another user
     * with the same name prefix is not granted. The endpoint is called with that roleKey.
     * ExpectedResult: Only the granted user is returned.
     */
    @Test
    public void test_filter_byRoleKey_returnsOnlyGrantedUsers() throws Exception {
        final String unique = "rkFilter" + System.currentTimeMillis();
        final Role role = new RoleDataGen().key(unique + "Key").nextPersisted();
        final User granted = new UserDataGen().firstName(unique).roles(role).nextPersisted();
        final User notGranted = new UserDataGen().firstName(unique).nextPersisted();
        rolesToClean.add(role);
        usersToClean.add(granted);
        usersToClean.add(notGranted);

        final List<String> userIds = filterUserIdsByRoleKeys(unique, List.of(role.getRoleKey()));
        assertTrue("granted user must be returned", userIds.contains(granted.getUserId()));
        assertFalse("user without the role must not be returned",
                userIds.contains(notGranted.getUserId()));
    }

    /**
     * Method to test: {@link UserResource#filter}
     * Given Scenario: Two roles with roleKeys hold one granted user each, sharing a name prefix.
     * The endpoint is called with both roleKeys.
     * ExpectedResult: Users holding any of the roles are returned.
     */
    @Test
    public void test_filter_byMultipleRoleKeys_returnsUnion() throws Exception {
        final String unique = "rkUnion" + System.currentTimeMillis();
        final Role roleA = new RoleDataGen().key(unique + "A").nextPersisted();
        final Role roleB = new RoleDataGen().key(unique + "B").nextPersisted();
        final User userA = new UserDataGen().firstName(unique).roles(roleA).nextPersisted();
        final User userB = new UserDataGen().firstName(unique).roles(roleB).nextPersisted();
        rolesToClean.add(roleA);
        rolesToClean.add(roleB);
        usersToClean.add(userA);
        usersToClean.add(userB);

        final List<String> userIds = filterUserIdsByRoleKeys(unique,
                List.of(roleA.getRoleKey(), roleB.getRoleKey()));
        assertTrue(userIds.contains(userA.getUserId()));
        assertTrue(userIds.contains(userB.getUserId()));
    }

    // ==================== PUT /v1/users — roles reconcile (#37109) ====================

    private static String uniq() {
        return Long.toString(System.nanoTime());
    }

    /**
     * Builds the minimal valid update form for the given user: same names and email,
     * no password, no roles (call {@code .roles(...)} to send the field).
     */
    private static UserForm.Builder updateFormFor(final User target) {
        return new UserForm.Builder()
                .userId(target.getUserId())
                .firstName(target.getFirstName())
                .lastName(target.getLastName())
                .email(target.getEmailAddress());
    }

    /**
     * Method to test: {@link UserResource#update(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: A user holds two user-assignable roles and an admin sends an update with
     * an explicit empty roles array — legacy parity with DWR UserAjax#updateUserRoles(userId, []).
     * Expected Result: 200; every user-assignable role is removed; the user's individual role
     * (editUsers=false, the anchor for individually-granted permissions) is preserved.
     */
    @Test
    public void test_update_emptyRolesArray_removesAllUserAssignableRoles() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("emptyroles" + uniq(), false, true);
        final Role roleA = new RoleDataGen().key("emptyrolesa" + uniq()).nextPersisted();
        final Role roleB = new RoleDataGen().key("emptyrolesb" + uniq()).nextPersisted();
        roleAPI.addRoleToUser(roleA, target);
        roleAPI.addRoleToUser(roleB, target);
        final Role individualRole = roleAPI.getUserRole(target);

        final Response resourceResponse = resource.update(mockRequest(), response,
                updateFormFor(target).roles(Collections.emptyList()).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());

        final List<Role> remaining = roleAPI.loadRolesForUser(target.getUserId(), false);
        assertTrue("all user-assignable roles must be gone",
                remaining.stream().noneMatch(Role::isEditUsers));
        assertTrue("the user's individual role must be preserved",
                remaining.stream().anyMatch(role -> individualRole.getId().equals(role.getId())));
        assertFalse(roleAPI.doesUserHaveRole(target, roleA));
        assertFalse(roleAPI.doesUserHaveRole(target, roleB));
    }

    /**
     * Given Scenario: A user holds a user-assignable role and an admin sends an update WITHOUT
     * the roles field at all.
     * Expected Result: 200; the user's roles are untouched.
     */
    @Test
    public void test_update_rolesFieldAbsent_leavesRolesUntouched() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("norolesfield" + uniq(), false, true);
        final Role roleA = new RoleDataGen().key("norolesfielda" + uniq()).nextPersisted();
        roleAPI.addRoleToUser(roleA, target);

        final Response resourceResponse = resource.update(mockRequest(), response,
                updateFormFor(target).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());

        assertTrue("roles must be untouched when the field is absent",
                roleAPI.doesUserHaveRole(target, roleA));
    }

    /**
     * Given Scenario: A user holds roles A and B; an admin sends roles [B, C] (role keys).
     * Expected Result: 200; the payload is the complete desired set — A removed, B kept,
     * C added.
     */
    @Test
    public void test_update_nonEmptyRoles_replacesUserAssignableSet() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("swaproles" + uniq(), false, true);
        final Role roleA = new RoleDataGen().key("swaprolesa" + uniq()).nextPersisted();
        final Role roleB = new RoleDataGen().key("swaprolesb" + uniq()).nextPersisted();
        final Role roleC = new RoleDataGen().key("swaprolesc" + uniq()).nextPersisted();
        roleAPI.addRoleToUser(roleA, target);
        roleAPI.addRoleToUser(roleB, target);

        final Response resourceResponse = resource.update(mockRequest(), response,
                updateFormFor(target).roles(List.of(roleB.getRoleKey(), roleC.getRoleKey())).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());

        assertFalse("role not in the payload must be removed", roleAPI.doesUserHaveRole(target, roleA));
        assertTrue("role kept in the payload must remain", roleAPI.doesUserHaveRole(target, roleB));
        assertTrue("new role in the payload must be added", roleAPI.doesUserHaveRole(target, roleC));
    }

    /**
     * Given Scenario: The payload names a non-user-assignable role (editUsers=false) and a key
     * that resolves to no role, alongside nothing else; the user holds one assignable role.
     * Expected Result: 200 — both entries are ignored (never granted, no error; legacy DWR
     * gates adds on editUsers and the old add path silently skipped unknown keys), and the
     * held assignable role is removed because it is not in the desired set.
     */
    @Test
    public void test_update_nonAssignableAndUnknownKeys_ignored() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("frozenkeys" + uniq(), false, true);
        final Role held = new RoleDataGen().key("frozenkeysheld" + uniq()).nextPersisted();
        roleAPI.addRoleToUser(held, target);
        final Role frozen = new RoleDataGen().key("frozenkeysrole" + uniq()).editUsers(false)
                .nextPersisted();

        final Response resourceResponse = resource.update(mockRequest(), response,
                updateFormFor(target)
                        .roles(List.of(frozen.getRoleKey(), "no-such-role-key-" + uniq()))
                        .build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());

        assertFalse("a non-assignable role must never be granted",
                roleAPI.doesUserHaveRole(target, frozen));
        assertFalse("the held assignable role is not in the desired set, so it must be removed",
                roleAPI.doesUserHaveRole(target, held));
    }

    /**
     * Given Scenario: The roles list contains a null entry (Jackson accepts null elements in a
     * JSON array bound to List&lt;String&gt;) alongside a valid key; the user holds one role.
     * Expected Result: 400 BadRequestException — mirroring RoleUsersForm on the role-side
     * membership endpoints — and, since processRoles runs inside the update transaction, the
     * user's roles are untouched.
     */
    @Test
    public void test_update_nullRoleEntry_badRequestAndRolesUntouched() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("nullentry" + uniq(), false, true);
        final Role held = new RoleDataGen().key("nullentryheld" + uniq()).nextPersisted();
        roleAPI.addRoleToUser(held, target);

        try {
            resource.update(mockRequest(), response,
                    updateFormFor(target)
                            .roles(java.util.Arrays.asList(held.getRoleKey(), null))
                            .build());
            fail("Should have thrown BadRequestException for a null roles entry");
        } catch (final com.dotcms.rest.exception.BadRequestException e) {
            // expected
        }

        assertTrue("roles must be untouched after a rejected payload",
                roleAPI.doesUserHaveRole(target, held));
    }
}
