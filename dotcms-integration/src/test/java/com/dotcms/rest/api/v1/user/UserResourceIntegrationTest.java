package com.dotcms.rest.api.v1.user;

import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
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
    public static void cleanUpFilterFixtures() {
        usersToClean.forEach(UserDataGen::remove);
        rolesToClean.forEach(RoleDataGen::remove);
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
}
