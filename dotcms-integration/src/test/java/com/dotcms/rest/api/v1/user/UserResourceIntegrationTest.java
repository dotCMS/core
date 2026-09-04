package com.dotcms.rest.api.v1.user;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotmarketing.business.RoleAPI;
import com.liferay.portal.ejb.UserTestUtil;
import java.util.Collections;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.business.Role;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.system.role.SmallRoleView;
import com.dotcms.rest.ErrorResponseHelper;
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
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Base64;
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
        return mockRequestAs("admin@dotcms.com", "admin");
    }

    /** Builds a request authenticated (Basic) as the given user. */
    private static HttpServletRequest mockRequestAs(final String email, final String password) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequestIntegrationTest(host.getHostname(), "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + Base64.getEncoder().encodeToString((email + ":" + password).getBytes()));

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
    private List<String> filterUserIdsByRoleKeys(final String filter, final List<String> roleKeys)
            throws Exception {
        final Response resourceResponse = resource.filter(mockRequest(), response, filter, 0, 40,
                null, "ASC", false, false, null, 0, roleKeys, false);
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

    // ==================== #37209: role IDs accepted alongside keys in `roles` ====================

    /**
     * Minimal valid create form: explicit userId (so the test can reload the user without
     * parsing the response), required names/email, and a password (createNewUser dereferences it).
     */
    private static UserForm.Builder createFormFor(final String userId) {
        return new UserForm.Builder()
                .userId(userId)
                .firstName("RoleIds")
                .lastName("Create" + uniq())
                .email("roleids-" + uniq() + "@dotcms.com")
                .password("Passw0rd!".toCharArray())
                .active(true);
    }

    /**
     * Method to test: {@link UserResource#update(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: The payload names roles by ID — one custom role WITHOUT a key (cannot be
     * expressed as a key at all) and one keyed role addressed by its ID — while the user holds a
     * third assignable role that is not in the payload.
     * Expected Result: 200; both ID entries resolve and are granted; the role not sent is
     * reconciled away exactly as with key entries.
     */
    @Test
    public void test_update_roleIdEntries_resolveLikeKeys() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("roleids" + uniq(), false, true);
        final Role keyless = new RoleDataGen().key(null).nextPersisted();
        final Role keyed = new RoleDataGen().key("roleidskeyed" + uniq()).nextPersisted();
        final Role notSent = new RoleDataGen().key("roleidsnotsent" + uniq()).nextPersisted();
        roleAPI.addRoleToUser(notSent, target);
        // Cold cache on purpose: RoleFactoryImpl.loadRoleByKey consults the role cache first, and
        // RoleCacheImpl.get checks the ID group before the key group — so an ID entry resolves by
        // accident whenever the role happens to be cached. The DB-backed lookup is key-only.
        CacheLocator.getRoleCache().clearCache();

        final Response resourceResponse = resource.update(mockRequest(), response,
                updateFormFor(target).roles(List.of(keyless.getId(), keyed.getId())).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());

        assertTrue("keyless role must be granted by id", roleAPI.doesUserHaveRole(target, keyless));
        assertTrue("keyed role must be granted by id too", roleAPI.doesUserHaveRole(target, keyed));
        assertFalse("role absent from the payload must be reconciled away",
                roleAPI.doesUserHaveRole(target, notSent));
    }

    /**
     * Given Scenario: A user holds a keyless custom role and a keyed role. An admin saves once
     * sending the keyed role by key and the keyless one by ID, then saves again sending only the
     * keyed role.
     * Expected Result: first save preserves both (keys and IDs mix freely); second save removes
     * the keyless role — the payload remains the source of truth for user-assignable roles.
     *
     * Pin, not a red test: for roles the user ALREADY holds this passes even before #37209,
     * because the update's permission check warms the user's roles into the role cache and
     * loadRoleByKey happens to resolve cached roles by ID. The explicit resolver makes the outcome
     * independent of cache state; this test guards the "kept" half regardless.
     */
    @Test
    public void test_update_keylessRole_keptWhenIdSent_removedWhenAbsent() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("roleidskeep" + uniq(), false, true);
        final Role keyless = new RoleDataGen().key(null).nextPersisted();
        final Role keyed = new RoleDataGen().key("roleidskept" + uniq()).nextPersisted();
        roleAPI.addRoleToUser(keyless, target);
        roleAPI.addRoleToUser(keyed, target);
        // Cold cache on purpose: RoleFactoryImpl.loadRoleByKey consults the role cache first, and
        // RoleCacheImpl.get checks the ID group before the key group — so an ID entry resolves by
        // accident whenever the role happens to be cached. The DB-backed lookup is key-only.
        CacheLocator.getRoleCache().clearCache();

        Response resourceResponse = resource.update(mockRequest(), response,
                updateFormFor(target).roles(List.of(keyed.getRoleKey(), keyless.getId())).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());
        assertTrue(roleAPI.doesUserHaveRole(target, keyless));
        assertTrue(roleAPI.doesUserHaveRole(target, keyed));

        resourceResponse = resource.update(mockRequest(), response,
                updateFormFor(target).roles(List.of(keyed.getRoleKey())).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());
        assertFalse("keyless role not in the payload must be removed",
                roleAPI.doesUserHaveRole(target, keyless));
        assertTrue(roleAPI.doesUserHaveRole(target, keyed));
    }

    /**
     * Method to test: {@link UserResource#create(HttpServletRequest, HttpServletResponse, UserForm)}
     * Given Scenario: A user is created with `roles` naming a keyless custom role by ID.
     * Expected Result: 200; the new user holds that role.
     */
    @Test
    public void test_create_roleIdEntry_resolvesLikeKey() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final Role keyless = new RoleDataGen().key(null).nextPersisted();
        final String userId = "roleidscreate-" + uniq();
        // Cold cache on purpose: RoleFactoryImpl.loadRoleByKey consults the role cache first, and
        // RoleCacheImpl.get checks the ID group before the key group — so an ID entry resolves by
        // accident whenever the role happens to be cached. The DB-backed lookup is key-only.
        CacheLocator.getRoleCache().clearCache();

        final Response resourceResponse = resource.create(mockRequest(), response,
                createFormFor(userId).roles(List.of(keyless.getId())).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());

        final User created = APILocator.getUserAPI().loadUserById(userId, APILocator.systemUser(), false);
        assertTrue("keyless role must be granted by id on create", roleAPI.doesUserHaveRole(created, keyless));
    }

    /**
     * Given Scenario: A user is created WITHOUT the roles field.
     * Expected Result: the legacy default applies — the user gets the Front-end User role.
     * Pins today's create default so the resolver refactor cannot drift it.
     */
    @Test
    public void test_create_rolesAbsent_defaultsToFrontEndUser() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        // note: UserIdValidatorImpl rejects any userId containing "default" (User.DEFAULT)
        final String userId = "roleidsnoroles-" + uniq();

        final Response resourceResponse = resource.create(mockRequest(), response, createFormFor(userId).build());
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());

        final User created = APILocator.getUserAPI().loadUserById(userId, APILocator.systemUser(), false);
        // Assert by KEY on a fresh load, not against roleAPI.loadFrontEndUserRole(): that method
        // memoizes a Role instance for the JVM's lifetime (RoleAPIImpl.LOGGEDIN_SITE_USER), and a
        // prior test in the shard may replace the DOTCMS_FRONT_END_USER row (Task05170...RolesTest
        // renames it via SQL and the upgrade task inserts a new one), leaving the memo stale.
        final List<Role> directRoles = roleAPI.loadRolesForUser(created.getUserId(), false);
        assertTrue("new user must receive the default Front-end User role",
                directRoles.stream().anyMatch(role -> Role.DOTCMS_FRONT_END_USER.equals(role.getRoleKey())));
    }

    // ==================== GET /v1/users/filter — includeRoles (#37233) ====================

    /**
     * Invokes {@code GET /v1/users/filter} with the given query, paging, role-key filter and
     * {@code includeRoles} flag; every other parameter keeps its default.
     */
    private Response filter(final String query, final int page, final int perPage,
                            final List<String> roleKeys, final boolean includeRoles) throws Exception {
        return resource.filter(mockRequest(), response, query, page, perPage, null, "ASC",
                false, false, null, 0, roleKeys, includeRoles);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(final Response resourceResponse) {
        assertEquals(Status.OK.getStatusCode(), resourceResponse.getStatus());
        return (List<Map<String, Object>>) ((ResponseEntityView<?>) resourceResponse.getEntity()).getEntity();
    }

    private static Map<String, Object> itemFor(final List<Map<String, Object>> items, final String userId) {
        return items.stream()
                .filter(item -> userId.equals(item.get("userId")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("user " + userId + " not in page"));
    }

    @SuppressWarnings("unchecked")
    private static List<SmallRoleView> rolesOf(final Map<String, Object> item) {
        assertTrue("item must carry a roles key", item.containsKey("roles"));
        return (List<SmallRoleView>) item.get("roles");
    }

    private static Set<String> roleIds(final List<SmallRoleView> roles) {
        return roles.stream().map(SmallRoleView::getId).collect(Collectors.toSet());
    }

    /**
     * Method to test: {@link UserResource#filter}
     * Given Scenario: A user holds a role; the list is requested WITHOUT {@code includeRoles}.
     * Expected Result: The item has no {@code roles} key and its key set is exactly
     * {@link User#toMap()} — the existing payload is untouched (opt-in contract).
     */
    @Test
    public void test_filter_includeRolesOff_payloadUnchanged() throws Exception {
        final User target = UserTestUtil.getUser("rolesoff" + uniq(), false, true);
        final Role role = new RoleDataGen().key("rolesoff" + uniq()).nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(role, target);

        final Map<String, Object> item = itemFor(items(filter(target.getUserId(), 1, 40, null, false)),
                target.getUserId());

        assertFalse("roles must be absent when not requested", item.containsKey("roles"));
        assertEquals("item keys must be exactly User#toMap()", target.toMap().keySet(), item.keySet());
    }

    /**
     * Given Scenario: A user directly holds two user-assignable roles; {@code includeRoles=true}.
     * Expected Result: The item carries {@code roles} with both roles, each exposing id, name and
     * roleKey; the user's personal role is not listed.
     */
    @Test
    public void test_filter_includeRolesOn_listsDirectRolesWithIdNameKey() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("rolesonuser" + uniq(), false, true);
        final Role roleA = new RoleDataGen().key("rolesona" + uniq()).name("Roles On A " + uniq()).nextPersisted();
        final Role roleB = new RoleDataGen().key("rolesonb" + uniq()).name("Roles On B " + uniq()).nextPersisted();
        roleAPI.addRoleToUser(roleA, target);
        roleAPI.addRoleToUser(roleB, target);
        final Role personal = roleAPI.getUserRole(target);

        final List<SmallRoleView> roles = rolesOf(itemFor(items(filter(target.getUserId(), 1, 40, null, true)),
                target.getUserId()));

        final Set<String> ids = roleIds(roles);
        assertTrue("role A must be listed", ids.contains(roleA.getId()));
        assertTrue("role B must be listed", ids.contains(roleB.getId()));
        assertFalse("the personal role must not be listed", ids.contains(personal.getId()));
        final SmallRoleView viewA = roles.stream().filter(r -> roleA.getId().equals(r.getId())).findFirst().get();
        assertEquals(roleA.getName(), viewA.getName());
        assertEquals(roleA.getRoleKey(), viewA.getRoleKey());
    }

    /**
     * Given Scenario: Role hierarchy parent -> child; the user directly holds ONLY the parent, so
     * the child is held by inheritance (visible through {@code loadRolesForUser(id, true)}).
     * Expected Result: {@code roles} lists the parent and NOT the child — direct memberships only.
     */
    @Test
    public void test_filter_includeRolesOn_excludesInheritedChildRole() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("inheritroles" + uniq(), false, true);
        final Role parent = new RoleDataGen().key("inheritparent" + uniq()).nextPersisted();
        final Role child = new RoleDataGen().key("inheritchild" + uniq()).parent(parent.getId()).nextPersisted();
        roleAPI.addRoleToUser(parent, target);
        // sanity: the child really is implicit for this user
        assertTrue(roleAPI.loadRolesForUser(target.getUserId(), true).stream()
                .anyMatch(r -> child.getId().equals(r.getId())));

        final Set<String> ids = roleIds(rolesOf(itemFor(items(filter(target.getUserId(), 1, 40, null, true)),
                target.getUserId())));

        assertTrue("directly held parent must be listed", ids.contains(parent.getId()));
        assertFalse("inherited child must not be listed", ids.contains(child.getId()));
    }

    /**
     * Given Scenario: The user directly holds a role saved WITHOUT a role key (as the Roles
     * portlet allows); {@code includeRoles=true}.
     * Expected Result: The role is listed with its id and name and a null roleKey.
     */
    @Test
    public void test_filter_includeRolesOn_keylessRoleListedWithNullKey() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("keylessroles" + uniq(), false, true);
        final Role keyless = new RoleDataGen().key(null).name("Keyless " + uniq()).nextPersisted();
        assertNull("precondition: the role must have no key", roleAPI.loadRoleById(keyless.getId()).getRoleKey());
        roleAPI.addRoleToUser(keyless, target);

        final List<SmallRoleView> roles = rolesOf(itemFor(items(filter(target.getUserId(), 1, 40, null, true)),
                target.getUserId()));

        final SmallRoleView view = roles.stream().filter(r -> keyless.getId().equals(r.getId())).findFirst()
                .orElseThrow(() -> new AssertionError("keyless role must be listed by id"));
        assertEquals(keyless.getName(), view.getName());
        assertNull(view.getRoleKey());
    }

    /**
     * Given Scenario: Two users share role S (unique key); one of them also holds role E. The list
     * is requested with {@code roleKey=S}, {@code per_page=1}, {@code includeRoles=true}, for
     * pages 1 and 2.
     * Expected Result: Each page carries exactly one item, both pages together cover both users,
     * totalEntries is 2, every item lists S, and only the second user's item lists E.
     */
    @Test
    public void test_filter_includeRolesOn_combinedWithRoleKeyFilterAndPaging() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final String prefix = "pageroles" + uniq();
        final User userOne = UserTestUtil.getUser(prefix + "a", false, true);
        final User userTwo = UserTestUtil.getUser(prefix + "b", false, true);
        final Role shared = new RoleDataGen().key("pagerolesshared" + uniq()).nextPersisted();
        final Role extra = new RoleDataGen().key("pagerolesextra" + uniq()).nextPersisted();
        roleAPI.addRoleToUser(shared, userOne);
        roleAPI.addRoleToUser(shared, userTwo);
        roleAPI.addRoleToUser(extra, userTwo);

        final Response pageOne = filter(prefix, 1, 1, List.of(shared.getRoleKey()), true);
        final Response pageTwo = filter(prefix, 2, 1, List.of(shared.getRoleKey()), true);
        final List<Map<String, Object>> first = items(pageOne);
        final List<Map<String, Object>> second = items(pageTwo);

        assertEquals(1, first.size());
        assertEquals(1, second.size());
        assertEquals(2, ((ResponseEntityView<?>) pageOne.getEntity()).getPagination().getTotalEntries());
        final Set<String> seen = Set.of((String) first.get(0).get("userId"), (String) second.get(0).get("userId"));
        assertEquals(Set.of(userOne.getUserId(), userTwo.getUserId()), seen);

        for (final Map<String, Object> item : List.of(first.get(0), second.get(0))) {
            final Set<String> ids = roleIds(rolesOf(item));
            assertTrue("every page item must list the shared role", ids.contains(shared.getId()));
            assertEquals("only userTwo holds the extra role",
                    userTwo.getUserId().equals(item.get("userId")), ids.contains(extra.getId()));
        }
    }

    /**
     * Given Scenario: A plain back-end user -- not a CMS Administrator and without Users+Roles portlet
     * access -- calls the list with and without {@code includeRoles}.
     * Expected Result: Without the flag the call succeeds exactly as before (200); with the flag it is
     * rejected with a {@link ForbiddenException} (403), matching the gate of
     * {@code GET /v1/roles/users/{id}} so the flag does not widen who can read role membership.
     */
    @Test
    public void test_filter_includeRolesOn_plainBackendUserForbidden_defaultPathUnchanged() throws Exception {
        final String suffix = uniq();
        final String password = "pw" + suffix;
        final Role plain = new RoleDataGen().key("plainbackend" + suffix).nextPersisted();
        final User caller = new UserDataGen().firstName("plain").lastName("Backend")
                .emailAddress("plainbackend" + suffix + "@dotcms.com").password(password)
                .roles(plain, TestUserUtils.getBackendRole()).nextPersisted();
        final LayoutAPI layoutAPI = APILocator.getLayoutAPI();
        assertFalse("precondition: caller must not be an admin", caller.isAdmin());
        assertFalse("precondition: caller must lack Roles+Users portlet access",
                layoutAPI.doesUserHaveAccessToPortlet(PortletID.ROLES.toString(), caller)
                        && layoutAPI.doesUserHaveAccessToPortlet(PortletID.USERS.toString(), caller));

        final Response allowed = resource.filter(mockRequestAs(caller.getEmailAddress(), password), response,
                caller.getUserId(), 1, 40, null, "ASC", false, false, null, 0, null, false);
        assertEquals("default path must keep working for any back-end user",
                Status.OK.getStatusCode(), allowed.getStatus());
        assertFalse(itemFor(items(allowed), caller.getUserId()).containsKey("roles"));

        try {
            resource.filter(mockRequestAs(caller.getEmailAddress(), password), response,
                    caller.getUserId(), 1, 40, null, "ASC", false, false, null, 0, null, true);
            fail("includeRoles=true must be forbidden for a non role-administrator");
        } catch (final ForbiddenException e) {
            // expected
        }
    }

    /**
     * Given Scenario: The user directly holds an ordinary root role whose NAME starts with "User"
     * ({@code Role#isUser()} is name-based and would flag it as a personal role) plus their real
     * personal role; {@code includeRoles=true}.
     * Expected Result: The "User ..." role is listed and only the personal role is left out --
     * personal roles are recognized by their ID-based DBFQN under the cms_users root, as the legacy
     * Users portlet did.
     */
    @Test
    public void test_filter_includeRolesOn_rootRoleNamedUserIsNotTreatedAsPersonal() throws Exception {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final User target = UserTestUtil.getUser("usernamedrole" + uniq(), false, true);
        final Role userManagers = new RoleDataGen().key("usermanagers" + uniq()).name("User Managers " + uniq())
                .nextPersisted();
        assertTrue("precondition: the name-based check must misclassify this role",
                roleAPI.loadRoleById(userManagers.getId()).isUser());
        roleAPI.addRoleToUser(userManagers, target);
        final Role personal = roleAPI.getUserRole(target);

        final Set<String> ids = roleIds(rolesOf(itemFor(items(filter(target.getUserId(), 1, 40, null, true)),
                target.getUserId())));

        assertTrue("a root role merely named 'User...' must be listed", ids.contains(userManagers.getId()));
        assertFalse("the personal role must still be excluded", ids.contains(personal.getId()));
    }
}
