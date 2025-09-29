package com.dotcms.rest.api.v1.user;

import com.dotcms.UnitTestBase;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.ErrorResponseHelper;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.rest.api.DotRestInstanceProvider;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.UserUtilTest;
import com.dotcms.util.pagination.UserPaginator;
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
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        final HostAPI siteAPI = mock(HostAPI.class);
        final RoleAPI roleAPI = mock(RoleAPI.class);

        final UserResourceHelper userHelper  = mock(UserResourceHelper.class);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);

        Config.CONTEXT = context;
        try {
            when(initDataObject.getUser()).thenReturn(user);
            when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);
            when(webResource.init(null, request, response, true, null)).thenReturn(initDataObject);
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session);
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(new Locale.Builder().setLanguage("en").setRegion("US").build());

            String filter = "filter";
            int page = 3;
            int perPage = 4;
            final List<User> users = new ArrayList<>();
            Response responseExpected = Response.ok(new ResponseEntityView<>(users)).build();
            final PaginationUtil paginationUtil = mock(PaginationUtil.class);
            when(paginationUtil.getPage(request, user, filter, page, perPage )).thenReturn(responseExpected);

            final DotRestInstanceProvider instanceProvider = new DotRestInstanceProvider()
                                                                     .setUserAPI(userAPI)
                                                                     .setHostAPI(siteAPI)
                                                                     .setRoleAPI(roleAPI)
                                                                     .setErrorHelper(errorHelper);
            UserResource userResource =
                    new UserResource(webResource, userHelper, paginationUtil, instanceProvider);

            try {

                UpdateCurrentUserForm updateUserForm = new UpdateCurrentUserForm.Builder()/*.userId("dotcms.org.1")*/.givenName("Admin").surname("User Admin").email("admin@dotcms.com").build();
                userResource.updateCurent(request, response, updateUserForm);
                fail ("Should throw a ValidationException");
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                UpdateCurrentUserForm updateUserForm = new UpdateCurrentUserForm.Builder().userId("dotcms.org.1")/*.givenName("Admin")*/.surname("User Admin").email("admin@dotcms.com").build();
                userResource.updateCurent(request, response, updateUserForm);
                fail ("Should throw a ValidationException");
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                UpdateCurrentUserForm updateUserForm = new UpdateCurrentUserForm.Builder()/*.userId("dotcms.org.1")*/.givenName("Admin")/*.surname("User Admin")*/.email("admin@dotcms.com").build();
                userResource.updateCurent(request, response, updateUserForm);
                fail ("Should throw a ValidationException");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            Config.CONTEXT = null;
        }

    }

    @Test
    public void testUpdateWithoutPassword() throws Exception {
        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        HttpSession session = request.getSession();
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final HostAPI siteAPI = mock(HostAPI.class);
        final LoginServiceAPI loginService= mock(LoginServiceAPI.class);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        RestUtilTest.initMockContext();
        try {
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
                    .loadUserById(Mockito.anyString(), Mockito.any(User.class),
                            Mockito.anyBoolean()))
                    .thenReturn(user);
            when(webResource.init(request, httpServletResponse, true)).thenReturn(initDataObject);
            when(request.getSession()).thenReturn(session);
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(
                    new Locale.Builder().setLanguage("en").setRegion("US").build());
            when(loginService.passwordMatch("password", user)).thenReturn(true);

            final DotRestInstanceProvider helperInstanceProvider = new DotRestInstanceProvider()
                    .setRoleAPI(roleAPI)
                    .setUserAPI(userAPI)
                    .setLayoutAPI(layoutAPI)
                    .setHostWebAPI(hostWebAPI)
                    .setUserWebAPI(userWebAPI)
                    .setPermissionAPI(permissionAPI)
                    .setUserProxyAPI(userProxyAPI)
                    .setLoginService(loginService);
            final UserResourceHelper userHelper = new UserResourceHelper(helperInstanceProvider);

            String filter = "filter";
            int page = 3;
            int perPage = 4;
            final List<User> users = new ArrayList<>();
            Response responseExpected = Response.ok(new ResponseEntityView<>(users)).build();
            final PaginationUtil paginationUtil = mock(PaginationUtil.class);
            when(paginationUtil.getPage(request, user, filter, page, perPage)).thenReturn(
                    responseExpected);

            final DotRestInstanceProvider instanceProvider = new DotRestInstanceProvider()
                    .setUserAPI(userAPI)
                    .setHostAPI(siteAPI)
                    .setRoleAPI(roleAPI)
                    .setErrorHelper(errorHelper);
            UserResource userResource =
                    new UserResource(webResource, userHelper, paginationUtil, instanceProvider);

            UpdateCurrentUserForm updateUserForm = new UpdateCurrentUserForm.Builder()
                    .userId("dotcms.org.1")
                    .givenName("Admin")
                    .surname("User Admin")
                    .email("admin@dotcms.com")
                    .currentPassword("password")
                    .build();

            Response response = userResource.updateCurent(request, httpServletResponse, updateUserForm);
            RestUtilTest.verifySuccessResponse(response);

            Map userMap = Map.class.cast(
                    ResponseEntityView.class.cast(response.getEntity()).getEntity());

            assertTrue(!userMap.isEmpty());
            assertTrue(userMap.containsKey("reauthenticate"));
            assertTrue(userMap.get("reauthenticate").equals(false));
            assertTrue(userMap.containsKey("userID"));
            assertTrue(userMap.get("userID").equals("dotcms.org.1"));
            assertTrue(userMap.containsKey("user"));

            verify(userAPI).save(user, systemUser, false, false);
        } finally {
            RestUtilTest.cleanupContext();
        }
    }

    @Test
    public void testUpdateWithPassword() throws Exception {
        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        HttpSession session = request.getSession();
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final LoginServiceAPI loginService= mock(LoginServiceAPI.class);
        final HostAPI siteAPI = mock(HostAPI.class);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        RestUtilTest.initMockContext();
        try {
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
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(
                    new Locale.Builder().setLanguage("en").setRegion("US").build());
            when(loginService.passwordMatch("password", user)).thenReturn(true);

            final DotRestInstanceProvider helperInstanceProvider = new DotRestInstanceProvider()
                    .setRoleAPI(roleAPI)
                    .setUserAPI(userAPI)
                    .setLayoutAPI(layoutAPI)
                    .setHostWebAPI(hostWebAPI)
                    .setUserWebAPI(userWebAPI)
                    .setPermissionAPI(permissionAPI)
                    .setUserProxyAPI(userProxyAPI)
                    .setLoginService(loginService);
            final UserResourceHelper userHelper = new UserResourceHelper(helperInstanceProvider);
            String filter = "filter";
            int page = 3;
            int perPage = 4;
            final List<User> users = new ArrayList<>();
            Response responseExpected = Response.ok(new ResponseEntityView<>(users)).build();
            final PaginationUtil paginationUtil = mock(PaginationUtil.class);
            when(paginationUtil.getPage(request, user, filter, page, perPage)).thenReturn(
                    responseExpected);
            final DotRestInstanceProvider instanceProvider = new DotRestInstanceProvider()
                    .setUserAPI(userAPI)
                    .setHostAPI(siteAPI)
                    .setRoleAPI(roleAPI)
                    .setErrorHelper(errorHelper);
            UserResource userResource =
                    new UserResource(webResource, userHelper, paginationUtil, instanceProvider);

            UpdateCurrentUserForm updateUserForm = new UpdateCurrentUserForm.Builder()
                    .userId("dotcms.org.1")
                    .givenName("Admin")
                    .surname("User Admin")
                    .email("admin@dotcms.com")
                    .currentPassword("password")
                    .newPassword("new password")
                    .build();

            Response response = userResource.updateCurent(request, httpServletResponse, updateUserForm);
            RestUtilTest.verifySuccessResponse(response);

            Map userMap = Map.class.cast(
                    ResponseEntityView.class.cast(response.getEntity()).getEntity());

            assertTrue(!userMap.isEmpty());
            assertTrue(userMap.containsKey("reauthenticate"));
            assertTrue(userMap.get("reauthenticate").equals(true));
            assertTrue(userMap.containsKey("userID"));
            assertTrue(userMap.get("userID").equals("dotcms.org.1"));
            assertTrue(userMap.containsKey("user"));
            assertTrue(userMap.containsKey("user"));
            assertTrue(((Map) userMap.get("user")).isEmpty());

            verify(userAPI).save(user, systemUser, true, false);
        } finally {
            RestUtilTest.cleanupContext();
        }
    }

    @Test
    public void testUpdateWithIncorrectPassword() throws Exception {
        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        HttpSession session = request.getSession();
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final LoginServiceAPI loginService= mock(LoginServiceAPI.class);
        final HostAPI siteAPI = mock(HostAPI.class);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        RestUtilTest.initMockContext();
        try {
            final User user = new User();
            user.setCompanyId(User.DEFAULT + "NO");
            user.setUserId("dotcms.org.1");

            final User systemUser = new User();
            final Locale enUsLocale = new Locale.Builder().setLanguage("en").setRegion("US")
                    .build();
            systemUser.setLocale(enUsLocale);
            when(initDataObject.getUser()).thenReturn(user);
            when(userAPI.getSystemUser()).thenReturn(systemUser);
            when(userAPI.loadUserById("dotcms.org.1", systemUser, false)).thenReturn(user);
            when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);
            when(request.getSession()).thenReturn(session);
            when(request.getSession(false)).thenReturn(session);
            when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(enUsLocale);
            when(loginService.passwordMatch("password", user)).thenReturn(false);
            when(errorHelper
                    .getErrorResponse(Response.Status.BAD_REQUEST, enUsLocale,
                            "current.usermanager.password.incorrect"))
                    .thenReturn(Response.status(Response.Status.BAD_REQUEST).build());

            final DotRestInstanceProvider helperInstanceProvider = new DotRestInstanceProvider()
                    .setRoleAPI(roleAPI)
                    .setUserAPI(userAPI)
                    .setLayoutAPI(layoutAPI)
                    .setHostWebAPI(hostWebAPI)
                    .setUserWebAPI(userWebAPI)
                    .setPermissionAPI(permissionAPI)
                    .setUserProxyAPI(userProxyAPI)
                    .setLoginService(loginService);
            final UserResourceHelper userHelper = new UserResourceHelper(helperInstanceProvider);

            String filter = "filter";
            int page = 3;
            int perPage = 4;
            final List<User> users = new ArrayList<>();
            Response responseExpected = Response.ok(new ResponseEntityView<>(users)).build();
            final PaginationUtil paginationUtil = mock(PaginationUtil.class);
            when(paginationUtil.getPage(request, user, filter, page, perPage)).thenReturn(
                    responseExpected);

            final DotRestInstanceProvider instanceProvider = new DotRestInstanceProvider()
                    .setUserAPI(userAPI)
                    .setHostAPI(siteAPI)
                    .setRoleAPI(roleAPI)
                    .setErrorHelper(errorHelper);
            UserResource userResource =
                    new UserResource(webResource, userHelper, paginationUtil, instanceProvider);

            UpdateCurrentUserForm updateUserForm = new UpdateCurrentUserForm.Builder()
                    .userId("dotcms.org.1")
                    .givenName("Admin")
                    .surname("User Admin")
                    .email("admin@dotcms.com")
                    .currentPassword("password")
                    .newPassword("new password")
                    .build();

            Response response = userResource.updateCurent(request, httpServletResponse, updateUserForm);
            assertEquals(response.getStatus(), 400);
        } finally {
            RestUtilTest.cleanupContext();
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAsData(HttpServletRequest, HttpServletResponse, String, int, int)}</li>
     *     <li><b>Given Scenario:</b> Getting the list of Users that can be used for impersonation, and requesting page
     *     #3 in the pagination.</li>
     *     <li><b>Expected Result:</b> As page #3 has been requested, the list of results must be empty.</li>
     * </ul>
     */
    @Test
    public void testLoginAsData() throws DotDataException {

        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final HostAPI siteAPI = mock(HostAPI.class);
        final LoginServiceAPI loginService= mock(LoginServiceAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        final DotRestInstanceProvider helperInstanceProvider = new DotRestInstanceProvider()
                                                                       .setRoleAPI(roleAPI)
                                                                       .setUserAPI(userAPI)
                                                                       .setLayoutAPI(layoutAPI)
                                                                       .setHostWebAPI(hostWebAPI)
                                                                       .setUserWebAPI(userWebAPI)
                                                                       .setPermissionAPI(permissionAPI)
                                                                       .setUserProxyAPI(userProxyAPI)
                                                                       .setLoginService(loginService);
        final UserResourceHelper userHelper  = new UserResourceHelper(helperInstanceProvider);
        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);
        final User user = new User();
        final Role loginAsRole = new Role();
        final Role backEndRole = new Role();
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);
        when(roleAPI.loadRoleByKey(Role.LOGIN_AS)).thenReturn(loginAsRole);
        when(roleAPI.loadBackEndUserRole()).thenReturn(backEndRole);
        when(roleAPI.doesUserHaveRole(initDataObject.getUser(), loginAsRole)).thenReturn(true);

        PaginatedArrayList<?> users = new PaginatedArrayList<>();
        Response responseExpected = Response.ok(new ResponseEntityView<>(users)).build();

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        final List<Role> roles = Collections.singletonList(roleAPI.loadBackEndUserRole());
        final Map<String, Object> extraParams = Map.of(
                UserPaginator.ROLES_PARAM, roles,
                UserAPI.FilteringParams.INCLUDE_ANONYMOUS_PARAM, false,
                UserAPI.FilteringParams.INCLUDE_DEFAULT_PARAM, false,
                UserPaginator.REMOVE_CURRENT_USER_PARAM, true,
                UserPaginator.REQUEST_PASSWORD_PARAM, true);
        final String filter = "";
        // Setting pagination parameters to NOT RETURN any result
        final int page = 3;
        final int perPage = 4;
        when(paginationUtil.getPage(request, user, filter, page, perPage, extraParams )).thenReturn(responseExpected);

        final DotRestInstanceProvider instanceProvider = new DotRestInstanceProvider()
                                                                 .setUserAPI(userAPI)
                                                                 .setHostAPI(siteAPI)
                                                                 .setRoleAPI(roleAPI)
                                                                 .setErrorHelper(errorHelper);
        UserResource resource =
                new UserResource(webResource, userHelper, paginationUtil, instanceProvider);
        Response response;
        RestUtilTest.verifySuccessResponse(
                response = resource.loginAsData(request, httpServletResponse, filter, page, perPage)
        );

        Assert.assertEquals(responseExpected.getEntity(), response.getEntity());
    }


    @Test(expected=ForbiddenException.class)
    public void testLoginAsData_Without_LoginAs_Role() throws DotDataException {

        HttpServletRequest request = RestUtilTest.getMockHttpRequest();
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final RoleAPI roleAPI  = mock( RoleAPI.class );
        final UserAPI userAPI  = mock( UserAPI.class );
        final LayoutAPI layoutAPI  = mock( LayoutAPI.class );
        final HostWebAPI hostWebAPI  = mock( HostWebAPI.class );
        final WebResource webResource = mock(WebResource.class);
        final UserWebAPI userWebAPI = mock(UserWebAPI.class);
        final PermissionAPI permissionAPI= mock(PermissionAPI.class);
        final UserProxyAPI userProxyAPI= mock(UserProxyAPI.class);
        final HostAPI siteAPI = mock(HostAPI.class);
        final LoginServiceAPI loginService= mock(LoginServiceAPI.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);

        final ErrorResponseHelper errorHelper  = mock(ErrorResponseHelper.class);
        final User user = new User();
        final Role loginAsRole = new Role();
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);
        when(roleAPI.loadRoleByKey(Role.LOGIN_AS)).thenReturn(loginAsRole);
        // does not have permissions
        when(roleAPI.doesUserHaveRole(user, loginAsRole)).thenReturn(false);

        final DotRestInstanceProvider helperInstanceProvider = new DotRestInstanceProvider()
                                                                       .setRoleAPI(roleAPI)
                                                                       .setUserAPI(userAPI)
                                                                       .setLayoutAPI(layoutAPI)
                                                                       .setHostWebAPI(hostWebAPI)
                                                                       .setUserWebAPI(userWebAPI)
                                                                       .setPermissionAPI(permissionAPI)
                                                                       .setUserProxyAPI(userProxyAPI)
                                                                       .setLoginService(loginService);
        final UserResourceHelper userHelper  = new UserResourceHelper(helperInstanceProvider);

        String filter = "filter";
        int page = 3;
        int perPage = 4;

        List<User> users = new ArrayList<>();
        Response responseExpected = Response.ok(new ResponseEntityView<>(users)).build();

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        when(paginationUtil.getPage(request, user, filter, page, perPage )).thenReturn(responseExpected);

        final DotRestInstanceProvider instanceProvider = new DotRestInstanceProvider()
                                                                 .setUserAPI(userAPI)
                                                                 .setHostAPI(siteAPI)
                                                                 .setRoleAPI(roleAPI)
                                                                 .setErrorHelper(errorHelper);
        UserResource resource =
                new UserResource(webResource, userHelper, paginationUtil, instanceProvider);

        Response response = resource.loginAsData(request, httpServletResponse, filter, page, perPage);
    }
}
