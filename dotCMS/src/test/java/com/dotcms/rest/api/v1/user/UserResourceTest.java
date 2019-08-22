package com.dotcms.rest.api.v1.user;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.api.system.user.UserService;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.util.UserUtilTest;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.UserProxyAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * {@link UserResource} test
 * @author jsanca
 */
public class UserResourceTest extends UnitTestBase {

    @Test
    public void testUpdateUserNullValues() throws JSONException, DotSecurityException, DotDataException {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final WebResource webResource       = mock(WebResource.class);
        final ServletContext context = mock(ServletContext.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        final UserAPI userAPI = mock(UserAPI.class);
        final UserResourceHelper userHelper  = mock(UserResourceHelper.class);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);

        Config.CONTEXT = context;

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);
        when(webResource.init(null, request, response, true, null)).thenReturn(initDataObject);
        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
        UserResource userResource =
                new UserResource(webResource, userAPI, userHelper, errorHelper);

        try {

            UpdateUserForm updateUserForm = new UpdateUserForm.Builder()/*.userId("dotcms.org.1")*/.givenName("Admin").surname("User Admin").email("admin@dotcms.com").build();
            userResource.update(request, response, updateUserForm);
            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            UpdateUserForm updateUserForm = new UpdateUserForm.Builder().userId("dotcms.org.1")/*.givenName("Admin")*/.surname("User Admin").email("admin@dotcms.com").build();
            userResource.update(request, response, updateUserForm);
            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            UpdateUserForm updateUserForm = new UpdateUserForm.Builder()/*.userId("dotcms.org.1")*/.givenName("Admin")/*.surname("User Admin")*/.email("admin@dotcms.com").build();
            userResource.update(request, response, updateUserForm);
            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Test
    public void testUpdateWithoutPassword() throws Exception {
        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        HttpSession session = request.getSession();
        final UserService userService = mock(UserService.class);
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final LoginServiceAPI loginService= mock(LoginServiceAPI.class);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        RestUtilTest.initMockContext();

        User user = Mockito.mock(User.class);
        UserUtilTest.set(user);
        user.setCompanyId(User.DEFAULT + "NO");
        user.setUserId("dotcms.org.1");

        final User systemUser = new User();

        when(user.clone()).thenReturn(user);
        when(user.getUserId()).thenReturn("dotcms.org.1");
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI
                .loadUserById(Mockito.anyString(), Mockito.any(User.class), Mockito.anyBoolean()))
                .thenReturn(user);
        when(webResource.init(request, httpServletResponse, true)).thenReturn(initDataObject);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
        when(loginService.passwordMatch("password", user)).thenReturn(true);

        final UserResourceHelper userHelper  = new UserResourceHelper(userService, roleAPI, userAPI, layoutAPI, hostWebAPI,
                userWebAPI, permissionAPI, userProxyAPI, loginService);

        UserResource userResource =
                new UserResource(webResource, userAPI, userHelper, errorHelper);

        UpdateUserForm updateUserForm = new UpdateUserForm.Builder()
                .userId("dotcms.org.1")
                .givenName("Admin")
                .surname("User Admin")
                .email("admin@dotcms.com")
                .currentPassword("password")
                .build();

        Response response = userResource.update(request, httpServletResponse, updateUserForm);
        RestUtilTest.verifySuccessResponse(response);

        Map userMap = Map.class.cast(ResponseEntityView.class.cast(response.getEntity()).getEntity());

        assertTrue(!userMap.isEmpty());
        assertTrue(userMap.containsKey("reauthenticate"));
        assertTrue(userMap.get("reauthenticate").equals(false));
        assertTrue(userMap.containsKey("userID"));
        assertTrue(userMap.get("userID").equals("dotcms.org.1"));
        assertTrue(userMap.containsKey("user"));

        verify(userAPI).save(user, systemUser, false, false);
    }

    @Test
    public void testUpdateWithPassword() throws Exception {
        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        HttpSession session = request.getSession();
        final UserService userService = mock(UserService.class);
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final LoginServiceAPI loginService= mock(LoginServiceAPI.class);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        RestUtilTest.initMockContext();

        final User user = new User();
        user.setCompanyId(User.DEFAULT + "NO");
        user.setUserId("dotcms.org.1");

        final User systemUser = new User();

        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI.loadUserById("dotcms.org.1", systemUser, false)).thenReturn(user);
        when(webResource.init(request, httpServletResponse, true)).thenReturn(initDataObject);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
        when(loginService.passwordMatch("password", user)).thenReturn(true);

        final UserResourceHelper userHelper  = new UserResourceHelper(userService, roleAPI, userAPI, layoutAPI, hostWebAPI,
                userWebAPI, permissionAPI, userProxyAPI, loginService);

        UserResource userResource =
                new UserResource(webResource, userAPI, userHelper, errorHelper);

        UpdateUserForm updateUserForm = new UpdateUserForm.Builder()
                .userId("dotcms.org.1")
                .givenName("Admin")
                .surname("User Admin")
                .email("admin@dotcms.com")
                .currentPassword("password")
                .newPassword("new password")
                .build();

        Response response = userResource.update(request, httpServletResponse, updateUserForm);
        RestUtilTest.verifySuccessResponse(response);

        Map userMap = Map.class.cast(ResponseEntityView.class.cast(response.getEntity()).getEntity());

        assertTrue(!userMap.isEmpty());
        assertTrue(userMap.containsKey("reauthenticate"));
        assertTrue(userMap.get("reauthenticate").equals(true));
        assertTrue(userMap.containsKey("userID"));
        assertTrue(userMap.get("userID").equals("dotcms.org.1"));
        assertTrue(userMap.containsKey("user"));
        assertTrue(userMap.containsKey("user"));
        assertTrue(((Map) userMap.get("user")).isEmpty());

        verify(userAPI).save(user, systemUser, true, false);
    }

    @Test
    public void testUpdateWithIncorrectPassword() throws Exception {
        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        HttpSession session = request.getSession();
        final UserService userService = mock(UserService.class);
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final LoginServiceAPI loginService= mock(LoginServiceAPI.class);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        RestUtilTest.initMockContext();

        final User user = new User();
        user.setCompanyId(User.DEFAULT + "NO");
        user.setUserId("dotcms.org.1");

        final User systemUser = new User();

        when(initDataObject.getUser()).thenReturn(user);
        when(userAPI.getSystemUser()).thenReturn(systemUser);
        when(userAPI.loadUserById("dotcms.org.1", systemUser, false)).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
        when(loginService.passwordMatch("password", user)).thenReturn(false);

        final UserResourceHelper userHelper  = new UserResourceHelper(userService, roleAPI, userAPI, layoutAPI, hostWebAPI,
                userWebAPI, permissionAPI, userProxyAPI, loginService);

        UserResource userResource =
                new UserResource(webResource, userAPI, userHelper, errorHelper);

        UpdateUserForm updateUserForm = new UpdateUserForm.Builder()
                .userId("dotcms.org.1")
                .givenName("Admin")
                .surname("User Admin")
                .email("admin@dotcms.com")
                .currentPassword("password")
                .newPassword("new password")
                .build();

        Response response = userResource.update(request, httpServletResponse, updateUserForm);
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testLoginAsData() throws Exception {
        String userFilter = "filter";

    	HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final UserService userService = mock(UserService.class);
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final LoginServiceAPI loginService= mock(LoginServiceAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        final UserResourceHelper userHelper  = new UserResourceHelper(userService, roleAPI, userAPI, layoutAPI, hostWebAPI,
                userWebAPI, permissionAPI, userProxyAPI, loginService);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);

        String userId1 = "admin.role.id";
        String userId2 = "login.as.id";
        String userId3 = "admin";

        Map<String,Object> user1Map = new HashMap<String,Object>();
        user1Map.put("userId", userId1);
        User user1 = mock(User.class);
        when( user1.getUserId() ).thenReturn( userId1 );
        when( user1.toMap() ).thenReturn(user1Map);

        Map<String,Object> user2Map = new HashMap<String,Object>();
        user2Map.put("userId", userId2);
        User user2 = mock(User.class);
        when( user2.getUserId() ).thenReturn( userId2 );
        when( user2.toMap() ).thenReturn(user2Map);
        
        Map<String,Object> user3Map = new HashMap<String,Object>();
        user3Map.put("userId", userId3);
        User user3 = mock(User.class);
        when( user3.getUserId() ).thenReturn( userId3 );
        when( user3.toMap() ).thenReturn(user3Map);

        List<User> users = list(user1, user2);

        when(userAPI.getUsersByName(userFilter, 1, 100, user3,false)).thenReturn( users );

        List<String> rolesId = new ArrayList<>();
        rolesId.add( "admin.role.id" );
        rolesId.add( "login.as.id" );

        Role adminRole = mock(Role.class);
        Role loginAsRole = mock(Role.class);

        when( adminRole.getId() ).thenReturn( "admin.role.id" );
        when( loginAsRole.getId() ).thenReturn( "login.as.id" );

        when( roleAPI.loadRoleByKey(Role.ADMINISTRATOR) ).thenReturn( adminRole );
        when( roleAPI.loadCMSAdminRole() ).thenReturn( loginAsRole );
        when( roleAPI.doesUserHaveRoles(userId1, rolesId) ).thenReturn( true ) ;
        when( roleAPI.doesUserHaveRoles(userId2, rolesId) ).thenReturn( false ) ;
        when( roleAPI.findRoleByFQN(Role.SYSTEM + " --> " + Role.LOGIN_AS) ).thenReturn( loginAsRole ) ;
        when( roleAPI.doesUserHaveRole(user3, loginAsRole) ).thenReturn( true ) ;
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);
        when( initDataObject.getUser()).thenReturn(user3);
        when( webResource.init(null, request, httpServletResponse, true, null)).thenReturn(initDataObject);



        UserResource userResource =
                new UserResource(webResource, userAPI, userHelper, errorHelper);

        Response response = userResource
                .loginAsData(request, httpServletResponse, userFilter, true);

        RestUtilTest.verifySuccessResponse( response );

        List<Map<String,Object>> responseUsers = (List<Map<String, Object>>) ((Map) ((ResponseEntityView) response.getEntity()).getEntity()).get("users");
        assertEquals(2, responseUsers.size());
    }
}
