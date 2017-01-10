package com.dotcms.rest.api.v1.user;

import com.dotcms.api.system.user.UserService;
import com.dotcms.cms.login.LoginService;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.*;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.rest.api.v1.site.SiteBrowserResource;
import com.dotmarketing.business.*;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.BaseMessageResources;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * {@link SiteBrowserResource} test
 * @author jsanca
 */
public class UserResourceTest extends BaseMessageResources {

    @Test
    public void testUpdateUserNullValues() throws JSONException, DotSecurityException, DotDataException {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
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
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());
        UserResource userResource =
                new UserResource(webResource, userAPI, userHelper, errorHelper);

        try {

            UpdateUserForm updateUserForm = new UpdateUserForm.Builder()/*.userId("dotcms.org.1")*/.givenName("Admin").surname("User Admin").email("admin@dotcms.com").build();
            userResource.update(request, updateUserForm);
            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            UpdateUserForm updateUserForm = new UpdateUserForm.Builder().userId("dotcms.org.1")/*.givenName("Admin")*/.surname("User Admin").email("admin@dotcms.com").build();
            userResource.update(request, updateUserForm);
            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            UpdateUserForm updateUserForm = new UpdateUserForm.Builder()/*.userId("dotcms.org.1")*/.givenName("Admin")/*.surname("User Admin")*/.email("admin@dotcms.com").build();
            userResource.update(request, updateUserForm);
            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Test
    public void testUpdateWithoutPassword() throws Exception {
        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
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
        final LoginService loginService= mock(LoginService.class);
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
        when(webResource.init(true, request, true)).thenReturn(initDataObject);
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

        Response response = userResource.update(request, updateUserForm);
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
        final LoginService loginService= mock(LoginService.class);
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
        when(webResource.init(true, request, true)).thenReturn(initDataObject);
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

        Response response = userResource.update(request, updateUserForm);
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
        final LoginService loginService= mock(LoginService.class);
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
        when(webResource.init(true, request, true)).thenReturn(initDataObject);
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

        Response response = userResource.update(request, updateUserForm);
        assertEquals(response.getStatus(), 400);
    }

    @Test
    public void testLoginAsData() throws Exception {
        final UserService userService = mock(UserService.class);
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final LoginService loginService= mock(LoginService.class);

        final UserResourceHelper userHelper  = new UserResourceHelper(userService, roleAPI, userAPI, layoutAPI, hostWebAPI,
                userWebAPI, permissionAPI, userProxyAPI, loginService);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);

        String userId1 = "admin.role.id";
        String userId2 = "login.as.id";

        User user1 = mock(User.class);
        when( user1.getUserId() ).thenReturn( userId1 );

        User user2 = mock(User.class);
        when( user2.getUserId() ).thenReturn( userId2 );

        List<User> users = list(user1, user2);

        when(userAPI.getUsersByNameOrEmailOrUserID("", 1, 30, false, false)).thenReturn( users );

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

        UserResource userResource =
                new UserResource(webResource, userAPI, userHelper, errorHelper);

        Response response = userResource.loginAsData();

        RestUtilTest.verifySuccessResponse( response );

        List<Map<String,Object>> responseUsers = (List<Map<String, Object>>) ((Map) ((ResponseEntityView) response.getEntity()).getEntity()).get("users");
        assertEquals(2, responseUsers.size());
    }
}
