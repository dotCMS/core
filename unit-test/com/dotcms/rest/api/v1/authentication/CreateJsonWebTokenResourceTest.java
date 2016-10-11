package com.dotcms.rest.api.v1.authentication;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.cms.login.LoginService;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.*;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link CreateJsonWebTokenResource}
 * @author jsanca
 */
public class CreateJsonWebTokenResourceTest {


    @Before
    public void initTest(){
        RestUtilTest.initMockContext();
    }

    public CreateJsonWebTokenResourceTest() {

	}

    @Test
    public void testEmptyParameter() throws JSONException{

        try {
            final CreateTokenForm createTokenForm =
                    new CreateTokenForm.Builder().build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }
    }

    @Test
    public void testWrongParameter() throws JSONException{

        try {
            final CreateTokenForm createTokenForm =
                    new CreateTokenForm.Builder().user("").build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }

        try {
            final CreateTokenForm createTokenForm =
                    new CreateTokenForm.Builder().user("").password("").build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }

        try {
            final CreateTokenForm createTokenForm =
                    new CreateTokenForm.Builder().user("hello").password(null).build();

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
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil responseUtil = ResponseUtil.INSTANCE;
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
        final SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

        Config.CONTEXT = context;

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


        final CreateJsonWebTokenResource createJsonWebTokenResource =
                new CreateJsonWebTokenResource(loginService, userLocalManager, responseUtil, jsonWebTokenUtils, securityLoggerServiceAPI);
        final CreateTokenForm createTokenForm =
                new CreateTokenForm.Builder().user(userId).password(pass).build();

        final Response response1 = createJsonWebTokenResource.getApiToken(request, response, createTokenForm);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 401);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("authentication-failed"));
    }

    @Test
    public void testUserEmailAddressException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil authenticationHelper = ResponseUtil.INSTANCE;
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
        final SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session); //
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new UserEmailAddressException();
            }
        });


        final CreateJsonWebTokenResource createJsonWebTokenResource =
                new CreateJsonWebTokenResource(loginService, userLocalManager, authenticationHelper, jsonWebTokenUtils, securityLoggerServiceAPI);
        final CreateTokenForm createTokenForm =
                new CreateTokenForm.Builder().user(userId).password(pass).build();

        final Response response1 = createJsonWebTokenResource.getApiToken(request, response, createTokenForm);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 401);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("authentication-failed"));
    }

    @Test
    public void testAuthException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
        final SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session); //
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new AuthException();
            }
        });


        final CreateJsonWebTokenResource createJsonWebTokenResource =
                new CreateJsonWebTokenResource(loginService, userLocalManager, ResponseUtil.INSTANCE, jsonWebTokenUtils, securityLoggerServiceAPI);
        final CreateTokenForm createTokenForm =
                new CreateTokenForm.Builder().user(userId).password(pass).build();

        final Response response1 = createJsonWebTokenResource.getApiToken(request, response, createTokenForm);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 401);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("authentication-failed"));
    }

    @Test
    public void testUserPasswordException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
        final SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session); //
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new UserPasswordException(UserPasswordException.PASSWORD_ALREADY_USED);
            }
        });


        final CreateJsonWebTokenResource createJsonWebTokenResource =
                new CreateJsonWebTokenResource(loginService, userLocalManager, ResponseUtil.INSTANCE, jsonWebTokenUtils, securityLoggerServiceAPI);
        final CreateTokenForm createTokenForm =
                new CreateTokenForm.Builder().user(userId).password(pass).build();

        final Response response1 = createJsonWebTokenResource.getApiToken(request, response, createTokenForm);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 401);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("authentication-failed"));
    }

    @Test
    public void testRequiredLayoutException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
        final SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession()).thenReturn(session); //
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new RequiredLayoutException();
            }
        });


        final CreateJsonWebTokenResource createJsonWebTokenResource =
                new CreateJsonWebTokenResource(loginService, userLocalManager, ResponseUtil.INSTANCE, jsonWebTokenUtils, securityLoggerServiceAPI);
        final CreateTokenForm createTokenForm =
                new CreateTokenForm.Builder().user(userId).password(pass).build();

        final Response response1 = createJsonWebTokenResource.getApiToken(request, response, createTokenForm);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 500);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("user-without-portlet"));

    }

    @Test
    public void testUserActiveException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final User user = new User();
        final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
        final SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

        Config.CONTEXT = context;

        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("CR").build();
        user.setLocale(locale);
        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getLocale()).thenReturn(locale); //
        when(request.getSession(false)).thenReturn(session); //
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(userId);
        when(userLocalManager.getUserById(userId)).thenReturn(user);
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new UserActiveException();
            }
        });


        final CreateJsonWebTokenResource createJsonWebTokenResource =
                new CreateJsonWebTokenResource(loginService, userLocalManager, ResponseUtil.INSTANCE, jsonWebTokenUtils, securityLoggerServiceAPI);
        final CreateTokenForm createTokenForm =
                new CreateTokenForm.Builder().user(userId).password(pass).build();

        final Response response1 = createJsonWebTokenResource.getApiToken(request, response, createTokenForm);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 401);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("your-account-is-not-active"));
        System.out.println(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
    }

    @Test
    public void testGetApiTokenAuthenticationFalse() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final User user = new User();
        final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
        final SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

        Config.CONTEXT = context;

        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("CR").build();
        user.setLocale(locale);
        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getLocale()).thenReturn(locale); //
        when(request.getSession(false)).thenReturn(session); //
        when(request.getSession()).thenReturn(session); //
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(userId);
        when(userLocalManager.getUserById(userId)).thenReturn(user);
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenReturn(false);


        final CreateJsonWebTokenResource createJsonWebTokenResource =
                new CreateJsonWebTokenResource(loginService, userLocalManager, ResponseUtil.INSTANCE, jsonWebTokenUtils, securityLoggerServiceAPI);
        final CreateTokenForm createTokenForm =
                new CreateTokenForm.Builder().user(userId).password(pass).build();

        final Response response1 = createJsonWebTokenResource.getApiToken(request, response, createTokenForm);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 401);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("authentication-failed"));
        System.out.println(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
    }


    @Test
    public void testGetApiToken() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);
        final User user = new User();
        final String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJpWEtweXU2QmtzcWI0MHZNa3VSUVF3PT0iLCJpYXQiOjE0NzEyODM4MjYsInN1YiI6IntcbiAgXCJ1c2VySWRcIjogXCJpWEtweXU2QmtzcWI0MHZNa3VSUVF3XFx1MDAzZFxcdTAwM2RcIixcbiAgXCJsYXN0TW9kaWZpZWRcIjogMTQ3MDg2NjM1NDAwMCxcbiAgXCJjb21wYW55SWRcIjogXCJkb3RjbXMub3JnXCJcbn0iLCJpc3MiOiJpWEtweXU2QmtzcWI0MHZNa3VSUVF3PT0iLCJleHAiOjE0NzI0OTM0MjZ9.YEtN28ENfpNRnugTFjZoiANlnnura5T5R0Pagi9wiC4";
        final JsonWebTokenUtils jsonWebTokenUtils = mock(JsonWebTokenUtils.class);
        final SecurityLoggerServiceAPI securityLoggerServiceAPI = mock(SecurityLoggerServiceAPI.class);

        Config.CONTEXT = context;

        final Locale locale = new Locale.Builder().setLanguage("en").setRegion("CR").build();
        user.setLocale(locale);
        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getLocale()).thenReturn(locale); //
        when(request.getSession(false)).thenReturn(session); //
        when(request.getSession()).thenReturn(session); //
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(userId);
        when(userLocalManager.getUserById(userId)).thenReturn(user);
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenReturn(true);


        final CreateJsonWebTokenResource createJsonWebTokenResource =
                new CreateJsonWebTokenResource(loginService, userLocalManager, ResponseUtil.INSTANCE, jsonWebTokenUtils, securityLoggerServiceAPI);
        final CreateTokenForm createTokenForm =
                new CreateTokenForm.Builder().user(userId).password(pass).build();

        final Response response1 = createJsonWebTokenResource.getApiToken(request, response, createTokenForm);

        assertNotNull(response1);
        assertEquals(response1.getStatus(), 200);
        assertNotNull(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() == 0);

        System.out.println(ResponseEntityView.class.cast(response1.getEntity()).getEntity());
    }


}
