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
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.UserUtilTest;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.UserPaginator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.util.SQLUtil;
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
import com.liferay.portal.auth.PrincipalThreadLocal;
import org.junit.After;
import org.junit.Assert;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.LoginAsSession;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.business.NoSuchUserException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.util.WebKeys;
import org.mockito.MockedStatic;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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

    private static final String CALLER_ID = "dotcms.org.caller";
    private static final String PRINCIPAL_ID = "dotcms.org.principal";
    private static final String LOGGED_IN_AS_ID = "dotcms.org.logged-in-as";
    private static final String TARGET_ID = "dotcms.org.target";
    private static final String SITE_ID = "site-identifier";
    private static final String COMPLETED_LINE = "has successfully logged in as";
    /** The message keys of the Login As refusals that answer 200 with {@code loginAs:false}. */
    private static final List<String> REFUSAL_MESSAGE_KEYS = List.of("loginas.error.requireduserid",
            "loginas.error.selfloginas", "loginas.error.missingloginaspwd", "loginas.error.invalidloginascredentials");

    /** {@link UserResource#loginAs} sets the request's principal name; don't leave it on the test thread. */
    @After
    public void clearPrincipalName() {
        PrincipalThreadLocal.setName(null);
    }

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
                userResource.updateCurrent(request, response, updateUserForm);
                fail ("Should throw a ValidationException");
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                UpdateCurrentUserForm updateUserForm = new UpdateCurrentUserForm.Builder().userId("dotcms.org.1")/*.givenName("Admin")*/.surname("User Admin").email("admin@dotcms.com").build();
                userResource.updateCurrent(request, response, updateUserForm);
                fail ("Should throw a ValidationException");
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                UpdateCurrentUserForm updateUserForm = new UpdateCurrentUserForm.Builder()/*.userId("dotcms.org.1")*/.givenName("Admin")/*.surname("User Admin")*/.email("admin@dotcms.com").build();
                userResource.updateCurrent(request, response, updateUserForm);
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

            Response response = userResource.updateCurrent(request, httpServletResponse, updateUserForm);
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

            Response response = userResource.updateCurrent(request, httpServletResponse, updateUserForm);
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

            Response response = userResource.updateCurrent(request, httpServletResponse, updateUserForm);
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

    /**
     * Utility method that creates a {@link UserResource} pointed at the provided mocks, ready to exercise the
     * {@code /filter} endpoint.
     */
    private UserResource getFilterTestResource(final WebResource webResource, final RoleAPI roleAPI,
                                               final PaginationUtil paginationUtil) {
        final DotRestInstanceProvider instanceProvider = new DotRestInstanceProvider()
                                                                 .setUserAPI(mock(UserAPI.class))
                                                                 .setHostAPI(mock(HostAPI.class))
                                                                 .setRoleAPI(roleAPI)
                                                                 .setErrorHelper(mock(ErrorResponseHelper.class));
        return new UserResource(webResource, mock(UserResourceHelper.class), paginationUtil, instanceProvider);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#filter(HttpServletRequest, HttpServletResponse, String, int,
     *     int, String, String, boolean, boolean, String, int, List, List, boolean)}</li>
     *     <li><b>Given Scenario:</b> A caller who is neither an administrator nor a Roles plus Users portlet holder
     *     filters by a {@code roleKey}.</li>
     *     <li><b>Expected Result:</b> {@link ForbiddenException}: filtering by role reveals who holds it, the same
     *     privilege {@code includeRoles} takes. The paginator is never reached.</li>
     * </ul>
     */
    @Test(expected = ForbiddenException.class)
    public void testFilterWithRoleKeysWithoutPrivilegeIsForbidden() throws DotDataException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        final RoleAPI roleAPI = mock(RoleAPI.class);
        final Role backendRole = new Role();
        backendRole.setId("backend-role-id");
        when(roleAPI.loadRoleByKey("DOTCMS_BACK_END_USER")).thenReturn(backendRole);

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        final UserResource resource = getFilterTestResource(webResource, roleAPI, paginationUtil);

        resource.filter(request, response, "jane", 0, 40, null, "ASC", false, false, null, 0,
                List.of("DOTCMS_BACK_END_USER"), null, false);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#filter(HttpServletRequest, HttpServletResponse, String, int,
     *     int, String, String, boolean, boolean, String, int, List, List, boolean)}</li>
     *     <li><b>Given Scenario:</b> Call the {@code /filter} endpoint passing {@code roleKey} values, both repeated
     *     and comma-separated.</li>
     *     <li><b>Expected Result:</b> Every key is resolved through the {@link RoleAPI} and the resulting Role list
     *     is handed to the paginator under {@link UserPaginator#ROLES_PARAM}.</li>
     * </ul>
     */
    @Test
    public void testFilterWithRoleKeysAddsRolesToExtraParams() throws DotDataException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        // filtering by role reveals who holds it, so the caller must be privileged
        final User user = mock(User.class);
        when(user.isAdmin()).thenReturn(true);
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        final RoleAPI roleAPI = mock(RoleAPI.class);
        final Role backendRole = new Role();
        backendRole.setId("backend-role-id");
        final Role frontendRole = new Role();
        frontendRole.setId("frontend-role-id");
        when(roleAPI.loadRoleByKey("DOTCMS_BACK_END_USER")).thenReturn(backendRole);
        when(roleAPI.loadRoleByKey("DOTCMS_FRONT_END_USER")).thenReturn(frontendRole);

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        final UserResource resource = getFilterTestResource(webResource, roleAPI, paginationUtil);

        resource.filter(request, response, "jane", 0, 40, null, "ASC", false, false, null, 0,
                List.of("DOTCMS_BACK_END_USER,DOTCMS_FRONT_END_USER"), null, false);

        final ArgumentCaptor<Map<String, Object>> extraParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(paginationUtil).getPage(Mockito.eq(request), Mockito.eq(user), Mockito.eq("jane"), Mockito.eq(0),
                Mockito.eq(40), Mockito.isNull(), Mockito.eq(OrderDirection.ASC), extraParamsCaptor.capture(), Mockito.isNull());
        assertEquals(List.of(backendRole, frontendRole),
                extraParamsCaptor.getValue().get(UserPaginator.ROLES_PARAM));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#filter(HttpServletRequest, HttpServletResponse, String, int,
     *     int, String, String, boolean, boolean, String, int, List, List, boolean)}</li>
     *     <li><b>Given Scenario:</b> Call the {@code /filter} endpoint without any {@code roleKey} value.</li>
     *     <li><b>Expected Result:</b> The paginator extra params do NOT include {@link UserPaginator#ROLES_PARAM},
     *     preserving the endpoint's previous behavior.</li>
     * </ul>
     */
    @Test
    public void testFilterWithoutRoleKeysLeavesRolesParamOut() throws DotDataException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = mock(User.class);
        when(user.isAdmin()).thenReturn(true);
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        final UserResource resource = getFilterTestResource(webResource, mock(RoleAPI.class), paginationUtil);

        resource.filter(request, response, "jane", 0, 40, null, "ASC", false, false, null, 0, null, null, false);

        final ArgumentCaptor<Map<String, Object>> extraParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(paginationUtil).getPage(Mockito.eq(request), Mockito.eq(user), Mockito.eq("jane"), Mockito.eq(0),
                Mockito.eq(40), Mockito.isNull(), Mockito.eq(OrderDirection.ASC), extraParamsCaptor.capture(), Mockito.isNull());
        Assert.assertFalse(extraParamsCaptor.getValue().containsKey(UserPaginator.ROLES_PARAM));
        Assert.assertFalse("includeRoles=false must not reach the paginator (Link header stays identical)",
                extraParamsCaptor.getValue().containsKey(UserPaginator.INCLUDE_ROLES_PARAM));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#filter(HttpServletRequest, HttpServletResponse, String, int,
     *     int, String, String, boolean, boolean, String, int, List, List, boolean)}</li>
     *     <li><b>Given Scenario:</b> A CMS Administrator calls the {@code /filter} endpoint with
     *     {@code includeRoles=true}.</li>
     *     <li><b>Expected Result:</b> The paginator extra params carry {@link UserPaginator#INCLUDE_ROLES_PARAM}
     *     set to {@code true}, so every item is enriched with its direct roles. (The 403 path for non-administrators
     *     is covered by {@code UserResourceIntegrationTest}.)</li>
     * </ul>
     */
    @Test
    public void testFilterWithIncludeRolesPassesFlagToPaginator() throws DotDataException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = mock(User.class);
        when(user.isAdmin()).thenReturn(true);
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        final UserResource resource = getFilterTestResource(webResource, mock(RoleAPI.class), paginationUtil);

        resource.filter(request, response, "jane", 0, 40, null, "ASC", false, false, null, 0, null, null, true);

        final ArgumentCaptor<Map<String, Object>> extraParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(paginationUtil).getPage(Mockito.eq(request), Mockito.eq(user), Mockito.eq("jane"), Mockito.eq(0),
                Mockito.eq(40), Mockito.isNull(), Mockito.eq(OrderDirection.ASC), extraParamsCaptor.capture(), Mockito.isNull());
        assertEquals(Boolean.TRUE, extraParamsCaptor.getValue().get(UserPaginator.INCLUDE_ROLES_PARAM));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#filter(HttpServletRequest, HttpServletResponse, String, int,
     *     int, String, String, boolean, boolean, String, int, List, List, boolean)}</li>
     *     <li><b>Given Scenario:</b> The {@code /filter} endpoint is called with {@code orderby=firstName} and
     *     {@code direction=DESC}.</li>
     *     <li><b>Expected Result:</b> Both the order-by field and the order direction reach the paginator through
     *     the extra params ({@link UserAPI.FilteringParams#ORDER_BY_PARAM} and
     *     {@link UserAPI.FilteringParams#ORDER_DIRECTION_PARAM}), the direction in the {@link SQLUtil} form the
     *     factory expects; the {@link OrderDirection} is also passed to {@code getPage} for the Link header. Before
     *     #37458 the direction key was never set, so every sort ran ascending.</li>
     * </ul>
     */
    @Test
    public void testFilterWithOrderByAndDirectionForwardsBothKeys() throws DotDataException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = mock(User.class);
        when(user.isAdmin()).thenReturn(true);
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        final UserResource resource = getFilterTestResource(webResource, mock(RoleAPI.class), paginationUtil);

        resource.filter(request, response, "jane", 0, 40, "firstName", "DESC", false, false, null, 0, null, null, false);

        final ArgumentCaptor<Map<String, Object>> extraParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(paginationUtil).getPage(Mockito.eq(request), Mockito.eq(user), Mockito.eq("jane"), Mockito.eq(0),
                Mockito.eq(40), Mockito.eq("firstName"), Mockito.eq(OrderDirection.DESC), extraParamsCaptor.capture(), Mockito.isNull());
        assertEquals("firstName", extraParamsCaptor.getValue().get(UserAPI.FilteringParams.ORDER_BY_PARAM));
        assertEquals(SQLUtil._DESC, extraParamsCaptor.getValue().get(UserAPI.FilteringParams.ORDER_DIRECTION_PARAM));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#filter(HttpServletRequest, HttpServletResponse, String, int,
     *     int, String, String, boolean, boolean, String, int, List, List, boolean)}</li>
     *     <li><b>Given Scenario:</b> A CMS Administrator calls the {@code /filter} endpoint with a {@code roleKey}
     *     that does not match any existing Role.</li>
     *     <li><b>Expected Result:</b> A {@link BadRequestException} is thrown instead of silently returning an empty
     *     or unfiltered result set.</li>
     * </ul>
     */
    @Test(expected = BadRequestException.class)
    public void testFilterWithUnknownRoleKeyThrowsBadRequest() throws DotDataException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User admin = mock(User.class);
        when(admin.isAdmin()).thenReturn(true);
        when(initDataObject.getUser()).thenReturn(admin);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleByKey("NOT_A_ROLE")).thenReturn(null);

        final UserResource resource = getFilterTestResource(webResource, roleAPI, mock(PaginationUtil.class));

        resource.filter(request, response, "jane", 0, 40, null, "ASC", false, false, null, 0,
                List.of("NOT_A_ROLE"), null, false);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#filter(HttpServletRequest, HttpServletResponse, String, int,
     *     int, String, String, boolean, boolean, String, int, List, List, boolean)}</li>
     *     <li><b>Given Scenario:</b> A CMS Administrator calls the {@code /filter} endpoint with a {@code roleKey}
     *     and two {@code roleId} values, one repeated and one comma-separated.</li>
     *     <li><b>Expected Result:</b> The key is resolved through {@link RoleAPI#loadRoleByKey(String)}, every ID
     *     through {@link RoleAPI#loadRoleById(String)}, and all three Roles reach the paginator under
     *     {@link UserPaginator#ROLES_PARAM}, so users holding any of them are returned.</li>
     * </ul>
     */
    @Test
    public void testFilterWithRoleIdsAddsRolesToExtraParams() throws DotDataException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User admin = mock(User.class);
        when(admin.isAdmin()).thenReturn(true);
        when(initDataObject.getUser()).thenReturn(admin);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        final RoleAPI roleAPI = mock(RoleAPI.class);
        final Role cmsAdminRole = new Role();
        cmsAdminRole.setId("cms-admin-role-id");
        final Role keylessRole = new Role();
        keylessRole.setId("keyless-role-id");
        final Role otherRole = new Role();
        otherRole.setId("other-role-id");
        when(roleAPI.loadRoleByKey(Role.CMS_ADMINISTRATOR_ROLE)).thenReturn(cmsAdminRole);
        when(roleAPI.loadRoleById("keyless-role-id")).thenReturn(keylessRole);
        when(roleAPI.loadRoleById("other-role-id")).thenReturn(otherRole);

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        final UserResource resource = getFilterTestResource(webResource, roleAPI, paginationUtil);

        resource.filter(request, response, "jane", 0, 40, null, "ASC", false, false, null, 0,
                List.of(Role.CMS_ADMINISTRATOR_ROLE), List.of("keyless-role-id, other-role-id"), false);

        final ArgumentCaptor<Map<String, Object>> extraParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(paginationUtil).getPage(Mockito.eq(request), Mockito.eq(admin), Mockito.eq("jane"), Mockito.eq(0),
                Mockito.eq(40), Mockito.isNull(), Mockito.eq(OrderDirection.ASC), extraParamsCaptor.capture(), Mockito.isNull());
        assertEquals(List.of(cmsAdminRole, keylessRole, otherRole),
                extraParamsCaptor.getValue().get(UserPaginator.ROLES_PARAM));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#filter(HttpServletRequest, HttpServletResponse, String, int,
     *     int, String, String, boolean, boolean, String, int, List, List, boolean)}</li>
     *     <li><b>Given Scenario:</b> A CMS Administrator calls the {@code /filter} endpoint with a {@code roleId}
     *     that does not match any existing Role.</li>
     *     <li><b>Expected Result:</b> A {@link BadRequestException} is thrown instead of silently returning an empty
     *     or unfiltered result set.</li>
     * </ul>
     */
    @Test(expected = BadRequestException.class)
    public void testFilterWithUnknownRoleIdThrowsBadRequest() throws DotDataException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User admin = mock(User.class);
        when(admin.isAdmin()).thenReturn(true);
        when(initDataObject.getUser()).thenReturn(admin);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        final RoleAPI roleAPI = mock(RoleAPI.class);
        when(roleAPI.loadRoleById("not-a-role-id")).thenReturn(null);

        final UserResource resource = getFilterTestResource(webResource, roleAPI, mock(PaginationUtil.class));

        resource.filter(request, response, "jane", 0, 40, null, "ASC", false, false, null, 0,
                null, List.of("not-a-role-id"), false);
    }
    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#filter(HttpServletRequest, HttpServletResponse, String, int,
     *     int, String, String, boolean, boolean, String, int, List, List, boolean)}</li>
     *     <li><b>Given Scenario:</b> A CMS Administrator calls the {@code /filter} endpoint with {@code roleKey} and
     *     {@code roleId} values that are blank or hold only separators, as a client that sends an empty query
     *     parameter would.</li>
     *     <li><b>Expected Result:</b> The blank values are dropped: no Role is looked up, no 400 is raised and the
     *     paginator extra params carry no {@link UserPaginator#ROLES_PARAM}.</li>
     * </ul>
     */
    @Test
    public void testFilterWithBlankRoleValuesLeavesRolesParamOut() throws DotDataException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User admin = mock(User.class);
        when(admin.isAdmin()).thenReturn(true);
        when(initDataObject.getUser()).thenReturn(admin);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        final RoleAPI roleAPI = mock(RoleAPI.class);
        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        final UserResource resource = getFilterTestResource(webResource, roleAPI, paginationUtil);

        resource.filter(request, response, "jane", 0, 40, null, "ASC", false, false, null, 0,
                List.of("", " , "), List.of(" "), false);

        final ArgumentCaptor<Map<String, Object>> extraParamsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(paginationUtil).getPage(Mockito.eq(request), Mockito.eq(admin), Mockito.eq("jane"), Mockito.eq(0),
                Mockito.eq(40), Mockito.isNull(), Mockito.eq(OrderDirection.ASC), extraParamsCaptor.capture(),
                Mockito.isNull());
        Assert.assertFalse(extraParamsCaptor.getValue().containsKey(UserPaginator.ROLES_PARAM));
        verify(roleAPI, Mockito.never()).loadRoleByKey(Mockito.anyString());
        verify(roleAPI, Mockito.never()).loadRoleById(Mockito.anyString());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> The helper accepts the Login As and returns the target id as stored, which
     *     differs from the posted one (posted with surrounding spaces).</li>
     *     <li><b>Expected Result:</b> 200 with {@code loginAs:true}, and exactly one "logged in as" security-log
     *     line, naming the stored id.</li>
     * </ul>
     */
    @Test
    public void loginAs_completed_logsTheStoredUserIdOnce() throws Exception {
        final LoginAsCall call = new LoginAsCall(CALLER_ID);
        call.session.put(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, SITE_ID);
        call.acceptsAs(TARGET_ID);

        final Response response = call.run(" " + TARGET_ID + " ");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(Map.of("loginAs", true), ((ResponseEntityView<?>) response.getEntity()).getEntity());
        final List<String> completed = call.completedLines();
        assertEquals(completed.toString(), 1, completed.size());
        assertTrue(completed.get(0), completed.get(0).contains("'" + TARGET_ID + "'"));
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> The helper refuses with a {@link DotDataException} that carries a message key,
     *     once for each key the Login As refusals use.</li>
     *     <li><b>Expected Result:</b> 200 with {@code loginAs:false} and that key, the refusal is logged, and no
     *     "logged in as" line is.</li>
     * </ul>
     */
    @Test
    public void loginAs_refusedWithMessageKey_logsNoCompletedLine() throws Exception {
        for (final String messageKey : REFUSAL_MESSAGE_KEYS) {
            final LoginAsCall call = new LoginAsCall(CALLER_ID);
            call.refusesWith(new DotDataException("refused", messageKey));

            final Response response = call.run(TARGET_ID);

            assertEquals(messageKey, Response.Status.OK.getStatusCode(), response.getStatus());
            final ResponseEntityView<?> entity = (ResponseEntityView<?>) response.getEntity();
            assertEquals(messageKey, Map.of("loginAs", false), entity.getEntity());
            assertEquals(messageKey, entity.getErrors().get(0).getErrorCode());
            assertTrue(messageKey + ": " + call.securityLog,
                    call.securityLog.stream().anyMatch(line -> line.startsWith("ERROR:")));
            assertEquals(messageKey, List.of(), call.completedLines());
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> The helper accepts, but the session has no selected site, so setting the
     *     target's site fails after the session was updated.</li>
     *     <li><b>Expected Result:</b> 500, and no "logged in as" line.</li>
     * </ul>
     */
    @Test
    public void loginAs_failingAfterSessionUpdate_logsNoCompletedLine() throws Exception {
        final LoginAsCall call = new LoginAsCall(CALLER_ID);
        call.acceptsAs(TARGET_ID);

        final Response response = call.run(TARGET_ID);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(List.of(), call.completedLines());
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> The session is user A logged in as user B; B asks for another Login As and the
     *     helper refuses with a message key, once for each key the Login As refusals use.</li>
     *     <li><b>Expected Result:</b> The session still holds the A to B Login As.</li>
     * </ul>
     */
    @Test
    public void loginAs_refusedWithMessageKey_keepsTheActiveLoginAs() throws Exception {
        for (final String messageKey : REFUSAL_MESSAGE_KEYS) {
            final LoginAsCall call = new LoginAsCall(LOGGED_IN_AS_ID).withActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
            call.refusesWith(new DotDataException("refused", messageKey));

            call.run(LOGGED_IN_AS_ID);

            call.assertActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
        }
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> The session is user A logged in as user B, on a selected site; the helper
     *     accepts user C and C's site is selected, but writing the "logged in as" line, the last step, fails.</li>
     *     <li><b>Expected Result:</b> 500; the session is back to the A to B Login As on the site it had selected.</li>
     * </ul>
     */
    @Test
    public void loginAs_failingAfterSiteSelection_restoresTheSelectedSite() throws Exception {
        final LoginAsCall call = new LoginAsCall(LOGGED_IN_AS_ID).withActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
        call.session.put(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, "selected-before");
        call.acceptsAs(TARGET_ID);
        call.failsOnCompletedLine = true;

        final Response response = call.run(TARGET_ID);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("selected-before", call.session.get(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID));
        call.assertActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> The session is user A logged in as user B; the helper throws
     *     {@link DotSecurityException}.</li>
     *     <li><b>Expected Result:</b> 403, and the session still holds the A to B Login As.</li>
     * </ul>
     */
    @Test
    public void loginAs_notAllowed_keepsTheActiveLoginAs() throws Exception {
        final LoginAsCall call = new LoginAsCall(LOGGED_IN_AS_ID).withActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
        call.refusesWith(new DotSecurityException("not allowed"));

        final Response response = call.run(TARGET_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        call.assertActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> The session is user A logged in as user B; the helper throws
     *     {@link NoSuchUserException}.</li>
     *     <li><b>Expected Result:</b> 401, and the session still holds the A to B Login As.</li>
     * </ul>
     */
    @Test
    public void loginAs_unknownUser_keepsTheActiveLoginAs() throws Exception {
        final LoginAsCall call = new LoginAsCall(LOGGED_IN_AS_ID).withActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
        call.refusesWith(new NoSuchUserException("no such user"));

        final Response response = call.run(TARGET_ID);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        call.assertActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> The session is user A logged in as user B; the helper fails with an error caused
     *     by a {@link DotSecurityException}.</li>
     *     <li><b>Expected Result:</b> 401, and the session still holds the A to B Login As.</li>
     * </ul>
     */
    @Test
    public void loginAs_deniedByNestedCause_keepsTheActiveLoginAs() throws Exception {
        final LoginAsCall call = new LoginAsCall(LOGGED_IN_AS_ID).withActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
        call.refusesWith(new RuntimeException(new DotSecurityException("not allowed")));

        final Response response = call.run(TARGET_ID);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        call.assertActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> The session is user A logged in as user B; the helper accepts user C, but
     *     setting C's site fails after the session was updated.</li>
     *     <li><b>Expected Result:</b> 500, and the session is back to the A to B Login As.</li>
     * </ul>
     */
    @Test
    public void loginAs_failingAfterSessionUpdate_restoresTheActiveLoginAs() throws Exception {
        final LoginAsCall call = new LoginAsCall(LOGGED_IN_AS_ID).withActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
        call.acceptsAs(TARGET_ID);

        final Response response = call.run(TARGET_ID);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        call.assertActiveLoginAs(PRINCIPAL_ID, LOGGED_IN_AS_ID);
    }

    /**
     * <ul>
     *     <li><b>Method to test:</b> {@link UserResource#loginAs(HttpServletRequest, HttpServletResponse, LoginAsForm)}</li>
     *     <li><b>Given Scenario:</b> A plain session for the caller; the helper accepts user C, but setting C's site
     *     fails after the session was updated.</li>
     *     <li><b>Expected Result:</b> 500; the session is the caller's again, with no Login As in it.</li>
     * </ul>
     */
    @Test
    public void loginAs_failingAfterSessionUpdate_restoresTheCaller() throws Exception {
        final LoginAsCall call = new LoginAsCall(CALLER_ID);
        call.session.put(WebKeys.USER_ID, CALLER_ID);
        call.acceptsAs(TARGET_ID);

        final Response response = call.run(TARGET_ID);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals(CALLER_ID, call.session.get(WebKeys.USER_ID));
        Assert.assertNull(call.session.get(LoginAsSession.SESSION_KEY));
        Assert.assertNull(call.session.get(WebKeys.PRINCIPAL_USER_ID));
        Assert.assertNull(call.session.get(com.dotmarketing.util.WebKeys.CURRENT_HOST));
        assertEquals(CALLER_ID, PrincipalThreadLocal.getName());
    }

    /**
     * One call to {@link UserResource#loginAs}: a caller holding the Login As role, a mocked helper, a session backed
     * by {@link #session}, and every {@link SecurityLogger#logInfo} message collected in {@link #securityLog}.
     */
    private static final class LoginAsCall {

        final Map<String, Object> session = new HashMap<>();
        final List<String> securityLog = new ArrayList<>();
        /** When set, writing the "logged in as" line fails, as the last step of a Login As that got that far. */
        boolean failsOnCompletedLine;
        final UserResourceHelper helper = mock(UserResourceHelper.class);
        final HostAPI siteAPI = mock(HostAPI.class);
        final User caller = new User();

        LoginAsCall(final String callerId) {
            caller.setUserId(callerId);
        }

        final User sessionUser = new User();
        final Host sessionSite = mock(Host.class);

        LoginAsCall withActiveLoginAs(final String principalId, final String loggedInAsId) {
            session.put(LoginAsSession.SESSION_KEY, new LoginAsSession(principalId, loggedInAsId));
            session.put(WebKeys.PRINCIPAL_USER_ID, principalId);
            session.put(WebKeys.USER_ID, loggedInAsId);
            sessionUser.setUserId(loggedInAsId);
            session.put(WebKeys.USER, sessionUser);
            session.put(com.dotmarketing.util.WebKeys.CURRENT_HOST, sessionSite);
            return this;
        }

        void acceptsAs(final String storedUserId) throws Exception {
            final Host site = mock(Host.class);
            when(site.getIdentifier()).thenReturn(SITE_ID);
            when(siteAPI.find(Mockito.anyString(), Mockito.any(), Mockito.anyBoolean())).thenReturn(site);
            when(helper.doLoginAs(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Map.of(
                    WebKeys.USER_ID, storedUserId, com.dotmarketing.util.WebKeys.CURRENT_HOST, site));
        }

        void refusesWith(final Exception refusal) throws Exception {
            when(helper.doLoginAs(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenThrow(refusal);
        }

        Response run(final String postedUserId) throws Exception {
            final WebResource webResource = mock(WebResource.class);
            final InitDataObject initData = mock(InitDataObject.class);
            when(initData.getUser()).thenReturn(caller);
            when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initData);
            final RoleAPI roleAPI = mock(RoleAPI.class);
            final Role loginAsRole = new Role();
            when(roleAPI.loadRoleByKey(Role.LOGIN_AS)).thenReturn(loginAsRole);
            when(roleAPI.doesUserHaveRole(caller, loginAsRole)).thenReturn(true);
            final DotRestInstanceProvider instanceProvider = new DotRestInstanceProvider()
                    .setUserAPI(mock(UserAPI.class))
                    .setHostAPI(siteAPI)
                    .setRoleAPI(roleAPI)
                    .setErrorHelper(mock(ErrorResponseHelper.class));
            final UserResource resource = new UserResource(webResource, helper, mock(PaginationUtil.class),
                    instanceProvider);

            final HttpSession httpSession = mock(HttpSession.class);
            when(httpSession.getAttribute(Mockito.anyString())).thenAnswer(i -> session.get(i.<String>getArgument(0)));
            Mockito.doAnswer(i -> session.put(i.getArgument(0), i.getArgument(1))).when(httpSession)
                    .setAttribute(Mockito.anyString(), Mockito.any());
            Mockito.doAnswer(i -> session.remove(i.<String>getArgument(0))).when(httpSession)
                    .removeAttribute(Mockito.anyString());
            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getSession()).thenReturn(httpSession);
            when(request.getSession(Mockito.anyBoolean())).thenReturn(httpSession);
            when(request.getServerName()).thenReturn("localhost");
            when(request.getRemoteAddr()).thenReturn("127.0.0.1");

            final LoginAsForm form = new LoginAsForm.Builder().userId(postedUserId).build();
            // the request authentication names the caller as the request's principal
            PrincipalThreadLocal.setName(caller.getUserId());
            // LanguageUtil is mocked only to keep the error messages away from the real language files
            try (MockedStatic<SecurityLogger> logger = Mockito.mockStatic(SecurityLogger.class);
                 MockedStatic<LanguageUtil> ignored = Mockito.mockStatic(LanguageUtil.class)) {
                logger.when(() -> SecurityLogger.logInfo(Mockito.any(Class.class), Mockito.anyString()))
                        .thenAnswer(i -> {
                            final String line = i.getArgument(1);
                            if (failsOnCompletedLine && line.contains(COMPLETED_LINE)) {
                                throw new IllegalStateException("log unavailable");
                            }
                            return securityLog.add(line);
                        });
                return resource.loginAs(request, mock(HttpServletResponse.class), form);
            }
        }

        List<String> completedLines() {
            return securityLog.stream().filter(line -> line.contains(COMPLETED_LINE)).collect(Collectors.toList());
        }

        void assertActiveLoginAs(final String principalId, final String loggedInAsId) {
            final LoginAsSession loginAs = (LoginAsSession) session.get(LoginAsSession.SESSION_KEY);
            Assert.assertNotNull("the Login As record is gone", loginAs);
            assertEquals(principalId, loginAs.getPrincipalUserId());
            assertEquals(loggedInAsId, loginAs.getLoginAsUserId());
            assertEquals(principalId, session.get(WebKeys.PRINCIPAL_USER_ID));
            assertEquals(loggedInAsId, session.get(WebKeys.USER_ID));
            Assert.assertSame(sessionUser, session.get(WebKeys.USER));
            Assert.assertSame(sessionSite, session.get(com.dotmarketing.util.WebKeys.CURRENT_HOST));
            assertEquals(loggedInAsId, PrincipalThreadLocal.getName());
        }
    }
}
