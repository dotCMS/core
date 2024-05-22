package com.dotcms.rest.api.v1.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.api.web.WebSessionContext;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.util.UserUtilTest;
import com.dotmarketing.business.LoginAsAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.logConsole.model.LogMapper;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.RequiredLayoutException;
import com.liferay.portal.UserActiveException;
import com.liferay.portal.UserEmailAddressException;
import com.liferay.portal.UserPasswordException;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.model.User;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AuthenticationResourceTest extends UnitTestBase {


    public AuthenticationResourceTest() {

	}

    @Test
    public void testEmptyParameter() throws JSONException{

        try {

            LogMapper mockLogMapper = mock(LogMapper.class);
            when(mockLogMapper.isLogEnabled(any())).thenReturn(false);
            LogMapper.setLogMapper(mockLogMapper);

            final AuthenticationForm authenticationForm =
                    new AuthenticationForm.Builder().build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }
    }

    @Test
    public void testWrongParameter() throws JSONException{

        try {
            final AuthenticationForm authenticationForm =
                    new AuthenticationForm.Builder().userId("").build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }

        try {
            final AuthenticationForm authenticationForm =
                    new AuthenticationForm.Builder().userId("").password("").build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }
    }

    @Test
    public void testNoSuchUserException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginServiceAPI loginService     = mock(LoginServiceAPI.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil responseUtil = ResponseUtil.INSTANCE;
        final LoginAsAPI loginAsAPI = mock(LoginAsAPI.class);
        final AuthenticationHelper authenticationHelper = new AuthenticationHelper(loginAsAPI, loginService);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session); //
            when(loginService.doActionLogin(
                    userId,
                    pass,
                    false,
                    request,
                    response))
                    .thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {

                    throw new NoSuchUserException();
                }
            });


            final AuthenticationResource authenticationResource =
                    new AuthenticationResource(loginService, userLocalManager, responseUtil, authenticationHelper);
            final AuthenticationForm authenticationForm =
                    new AuthenticationForm.Builder().userId(userId).password(pass).build();

            final Response response1 = authenticationResource.authentication(request, response, authenticationForm);

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 401);
            assertNotNull(response1.getEntity());
            assertTrue(response1.getEntity() instanceof ResponseEntityView);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
            assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
            assertTrue(ErrorEntity.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0)).getErrorCode().equals("authentication-failed"));

        } finally {
            Config.CONTEXT = null;
        }
    }


    @Test
    public void testUserEmailAddressException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginServiceAPI loginService     = mock(LoginServiceAPI.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil responseUtil = ResponseUtil.INSTANCE;
        final LoginAsAPI loginAsAPI = mock(LoginAsAPI.class);
        final AuthenticationHelper authenticationHelper = new AuthenticationHelper(loginAsAPI, loginService);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session); //
            when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {

                    throw new UserEmailAddressException();
                }
            });


            final AuthenticationResource authenticationResource =
                    new AuthenticationResource(loginService, userLocalManager, responseUtil, authenticationHelper);
            final AuthenticationForm authenticationForm =
                    new AuthenticationForm.Builder().userId(userId).password(pass).build();

            final Response response1 = authenticationResource.authentication(request, response, authenticationForm);

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 401);
            assertNotNull(response1.getEntity());
            assertTrue(response1.getEntity() instanceof ResponseEntityView);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
            assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
            assertTrue(ErrorEntity.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0)).getErrorCode().equals("authentication-failed"));

        } finally {
            Config.CONTEXT = null;
        }
    }

    @Test
    public void testAuthException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginServiceAPI loginService     = mock(LoginServiceAPI.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final LoginAsAPI loginAsAPI = mock(LoginAsAPI.class);
        final AuthenticationHelper authenticationHelper = new AuthenticationHelper(loginAsAPI, loginService);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session); //
            when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {

                    throw new AuthException();
                }
            });


            final AuthenticationResource authenticationResource =
                    new AuthenticationResource(loginService, userLocalManager, ResponseUtil.INSTANCE,  authenticationHelper);
            final AuthenticationForm authenticationForm =
                    new AuthenticationForm.Builder().userId(userId).password(pass).build();

            final Response response1 = authenticationResource.authentication(request, response, authenticationForm);

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 401);
            assertNotNull(response1.getEntity());
            assertTrue(response1.getEntity() instanceof ResponseEntityView);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
            assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
            assertTrue(ErrorEntity.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0)).getErrorCode().equals("authentication-failed"));
        } finally {
            Config.CONTEXT = null;
        }
    }

    @Test
    public void testUserPasswordException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginServiceAPI loginService     = mock(LoginServiceAPI.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final LoginAsAPI loginAsAPI = mock(LoginAsAPI.class);
        final AuthenticationHelper authenticationHelper = new AuthenticationHelper(loginAsAPI, loginService);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session); //
            when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {

                    throw new UserPasswordException(UserPasswordException.PASSWORD_ALREADY_USED);
                }
            });


            final AuthenticationResource authenticationResource =
                    new AuthenticationResource(loginService, userLocalManager, ResponseUtil.INSTANCE,  authenticationHelper);
            final AuthenticationForm authenticationForm =
                    new AuthenticationForm.Builder().userId(userId).password(pass).build();

            final Response response1 = authenticationResource.authentication(request, response, authenticationForm);

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 401);
            assertNotNull(response1.getEntity());
            assertTrue(response1.getEntity() instanceof ResponseEntityView);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
            assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
            assertTrue(ErrorEntity.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0)).getErrorCode().equals("authentication-failed"));
        } finally {
            Config.CONTEXT = null;
        }
    }

    @Test
    public void testRequiredLayoutException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginServiceAPI loginService     = mock(LoginServiceAPI.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final LoginAsAPI loginAsAPI = mock(LoginAsAPI.class);
        final AuthenticationHelper authenticationHelper = new AuthenticationHelper(loginAsAPI, loginService);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session); //
            when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {

                    throw new RequiredLayoutException();
                }
            });


            final AuthenticationResource authenticationResource =
                    new AuthenticationResource(loginService, userLocalManager, ResponseUtil.INSTANCE,  authenticationHelper);
            final AuthenticationForm authenticationForm =
                    new AuthenticationForm.Builder().userId(userId).password(pass).build();

            final Response response1 = authenticationResource.authentication(request, response, authenticationForm);

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 500);
            assertNotNull(response1.getEntity());
            assertTrue(response1.getEntity() instanceof ResponseEntityView);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
            assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
            assertTrue(ErrorEntity.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0)).getErrorCode().equals("user-without-portlet"));
        } finally {
            Config.CONTEXT = null;
        }
    }

    @Test
    public void testUserActiveException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginServiceAPI loginService     = mock(LoginServiceAPI.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final LoginAsAPI loginAsAPI = mock(LoginAsAPI.class);
        final AuthenticationHelper authenticationHelper = new AuthenticationHelper(loginAsAPI, loginService);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;
        try {
            when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
            when(request.getSession()).thenReturn(session); //
            when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

                @Override
                public Boolean answer(InvocationOnMock invocation) throws Throwable {

                    throw new UserActiveException();
                }
            });


            final AuthenticationResource authenticationResource =
                    new AuthenticationResource(loginService, userLocalManager, ResponseUtil.INSTANCE,  authenticationHelper);
            final AuthenticationForm authenticationForm =
                    new AuthenticationForm.Builder().userId(userId).password(pass).language("en").country("US").build();

            final Response response1 = authenticationResource.authentication(request, response, authenticationForm);

            assertNotNull(response1);
            assertEquals(response1.getStatus(), 401);
            assertNotNull(response1.getEntity());
            assertTrue(response1.getEntity() instanceof ResponseEntityView);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
            assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
            assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
            assertTrue(ErrorEntity.class.cast(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0)).getErrorCode().equals("your-account-is-not-active"));
            System.out.println(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        } finally {
            Config.CONTEXT = null;
        }
    }

    @Test
    public void testgetUsersWithoutLoginAs() throws DotSecurityException, DotDataException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException, LanguageException, ClassNotFoundException {

        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        HttpServletRequest mockHttpRequest = RestUtilTest.getMockHttpRequest();

        LoginAsAPI loginAsAPI = mock( LoginAsAPI.class );
        LoginServiceAPI loginService = mock( LoginServiceAPI.class );

        User user = mock(User.class);
        UserUtilTest.set(user);

        when( loginService.getLoggedInUser( mockHttpRequest ) ).thenReturn( user );

        AuthenticationHelper helper = new AuthenticationHelper(loginAsAPI, loginService);

        final AuthenticationResource resource = new AuthenticationResource( loginService, userLocalManager, ResponseUtil.INSTANCE, helper );
        Response responseEntityView = resource.getLoginUser(mockHttpRequest);

        RestUtilTest.verifySuccessResponse( responseEntityView );
        Object entity = ((ResponseEntityView) responseEntityView.getEntity()).getEntity();
        assertTrue(entity instanceof Map);

        Map map = ( Map ) entity;
        assertEquals(user.toMap(), map.get( "user" ));
        assertNull(map.get( "loginAsUser" ));
    }

    @Test
    public void testgetUsersWithLoginAs() throws DotSecurityException, DotDataException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException, LanguageException, ClassNotFoundException {

        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        HttpServletRequest mockHttpRequest = RestUtilTest.getMockHttpRequest();
        LoginAsAPI loginAsAPI = mock( LoginAsAPI.class );

        LoginServiceAPI loginService = mock( LoginServiceAPI.class );

        User user = mock(User.class);
        UserUtilTest.set(user);

        User loginAsUser = mock(User.class);
        UserUtilTest.set(loginAsUser);

        when( loginService.getLoggedInUser( mockHttpRequest ) ).thenReturn( loginAsUser );
        when(loginAsAPI.getPrincipalUser(WebSessionContext.getInstance(mockHttpRequest))).thenReturn(user);

        AuthenticationHelper helper = new AuthenticationHelper(loginAsAPI, loginService);

        final AuthenticationResource resource = new AuthenticationResource( loginService, userLocalManager, ResponseUtil.INSTANCE, helper );
        Response responseEntityView = resource.getLoginUser(mockHttpRequest);

        RestUtilTest.verifySuccessResponse( responseEntityView );
        Object entity = ((ResponseEntityView) responseEntityView.getEntity()).getEntity();
        assertTrue(entity instanceof Map);

        Map map = ( Map ) entity;
        assertEquals(user.toMap(), map.get( "user" ));
        assertEquals(loginAsUser.toMap(), map.get( "loginAsUser" ));
    }

}
