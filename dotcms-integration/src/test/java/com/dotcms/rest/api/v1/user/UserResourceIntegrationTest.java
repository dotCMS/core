package com.dotcms.rest.api.v1.user;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.liferay.portal.ejb.UserTestUtil;
import java.util.Collections;
import java.util.List;
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
import java.util.Base64;
import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class UserResourceIntegrationTest {

    static HttpServletResponse response;
    static HttpServletRequest request;
    static UserResource resource;
    static User user;
    static Host host;
    static User adminUser;

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
}
