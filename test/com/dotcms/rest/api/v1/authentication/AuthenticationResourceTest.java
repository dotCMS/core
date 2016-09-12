package com.dotcms.rest.api.v1.authentication;

import com.dotcms.cms.login.LoginService;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.*;
import com.liferay.portal.auth.AuthException;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationResourceTest {


    public AuthenticationResourceTest() {

	}

    @Test
    public void testEmptyParameter() throws JSONException{

        try {
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
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final AuthenticationHelper authenticationHelper = AuthenticationHelper.INSTANCE;
        final WebResource webResource       = null;
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
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
                new AuthenticationResource(loginService, userLocalManager, authenticationHelper, webResource);
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
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("authentication-failed"));
    }

    @Test
    public void testUserEmailAddressException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final AuthenticationHelper authenticationHelper = AuthenticationHelper.INSTANCE;
        final WebResource webResource       = null;
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        when(request.getSession()).thenReturn(session); //
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new UserEmailAddressException();
            }
        });


        final AuthenticationResource authenticationResource =
                new AuthenticationResource(loginService, userLocalManager, authenticationHelper, webResource);
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
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("authentication-failed"));
    }

    @Test
    public void testAuthException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final WebResource webResource       = null;
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        when(request.getSession()).thenReturn(session); //
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new AuthException();
            }
        });


        final AuthenticationResource authenticationResource =
                new AuthenticationResource(loginService, userLocalManager, AuthenticationHelper.INSTANCE,  webResource);
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
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("authentication-failed"));
    }

    @Test
    public void testUserPasswordException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final WebResource webResource       = null;
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        when(request.getSession()).thenReturn(session); //
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new UserPasswordException(UserPasswordException.PASSWORD_ALREADY_USED);
            }
        });


        final AuthenticationResource authenticationResource =
                new AuthenticationResource(loginService, userLocalManager, AuthenticationHelper.INSTANCE,  webResource);
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
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("authentication-failed"));
    }

    @Test
    public void testRequiredLayoutException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final WebResource webResource       = null;
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        when(request.getSession()).thenReturn(session); //
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new RequiredLayoutException();
            }
        });


        final AuthenticationResource authenticationResource =
                new AuthenticationResource(loginService, userLocalManager, AuthenticationHelper.INSTANCE,  webResource);
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
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("user-without-portlet"));

    }

    @Test
    public void testUserActiveException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final LoginService loginService     = mock(LoginService.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final WebResource webResource       = null;
        final String userId = "admin@dotcms.com";
        final String pass   = "pass";
        final ServletContext context = mock(ServletContext.class);

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(User.DEFAULT);
        when(request.getSession()).thenReturn(session); //
        when(loginService.doActionLogin(userId, pass, false, request, response)).thenAnswer(new Answer<Boolean>() { // if this method is called, should fail

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {

                throw new UserActiveException();
            }
        });


        final AuthenticationResource authenticationResource =
                new AuthenticationResource(loginService, userLocalManager, AuthenticationHelper.INSTANCE,  webResource);
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
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals("your-account-is-not-active"));
        System.out.println(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
    }



}
