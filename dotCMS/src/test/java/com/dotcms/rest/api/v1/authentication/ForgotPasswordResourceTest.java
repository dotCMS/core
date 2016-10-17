package com.dotcms.rest.api.v1.authentication;

import com.dotcms.api.system.user.UserService;
import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.cms.login.LoginService;
import com.dotcms.company.CompanyAPI;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.*;
import com.liferay.portal.ejb.UserLocalManager;
import com.liferay.portal.ejb.UserManager;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForgotPasswordResourceTest {


    public ForgotPasswordResourceTest() {

    }

    @Before
    public void initTest(){
        RestUtilTest.initMockContext();
    }

    @Test
    public void testEmptyParameter() throws JSONException{

        try {
            final ForgotPasswordForm forgotPasswordForm =
                    new ForgotPasswordForm.Builder().build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }
    }

    @Test
    public void testWrongParameter() throws JSONException{

        try {
            final ForgotPasswordForm forgotPasswordForm =
                    new ForgotPasswordForm.Builder().userId("").build();

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
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil responseUtil = ResponseUtil.INSTANCE;
        final CompanyAPI companyAPI = mock(CompanyAPI.class);
        final String userId = "admin@dotcms.com";
        final ServletContext context = mock(ServletContext.class);
        final UserService userService = mock(UserService.class);
        final Company company = new Company() {

            @Override
            public String getAuthType() {

                return Company.AUTH_TYPE_ID;
            }
        };
        final ForgotPasswordForm forgotPasswordForm =
                new ForgotPasswordForm.Builder().userId(userId).build();

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession(false)).thenReturn(session); //
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(Locale.getDefault()); //
        when(companyAPI.getCompany(request)).thenReturn(company);
        when(userLocalManager.getUserById(anyString()))
                .thenAnswer(new Answer<User>() { // if this method is called, should fail

                    @Override
                    public User answer(InvocationOnMock invocation) throws Throwable {

                        throw new NoSuchUserException();
                    }
                });



        final ForgotPasswordResource authenticationResource =
                new ForgotPasswordResource(userLocalManager, userService,
                        companyAPI, responseUtil);


        final Response response1 = authenticationResource.forgotPassword(request, response, forgotPasswordForm);

        System.out.println(response1);
        assertNotNull(response1);
        assertEquals(response1.getStatus(), Response.Status.UNAUTHORIZED.getStatusCode());
        assertNotNull(response1.getEntity());
        System.out.println(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals
                ("a-new-password-has-been-sent-to-x"));
    }

    @Test
    public void testSendPasswordException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil authenticationHelper = ResponseUtil.INSTANCE;
        final CompanyAPI companyAPI = mock(CompanyAPI.class);
        final String userId = "admin@dotcms.com";
        final ServletContext context = mock(ServletContext.class);
        final UserService userService = mock(UserService.class);
        final Company company = new Company() {

            @Override
            public String getAuthType() {

                return Company.AUTH_TYPE_ID;
            }
        };
        final ForgotPasswordForm forgotPasswordForm =
                new ForgotPasswordForm.Builder().userId(userId).build();

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession(false)).thenReturn(session); //
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(Locale.getDefault()); //
        when(companyAPI.getCompany(request)).thenReturn(company);
        when(userLocalManager.getUserById(anyString()))
                .thenAnswer(new Answer<User>() { // if this method is called, should fail

                    @Override
                    public User answer(InvocationOnMock invocation) throws Throwable {

                        throw new SendPasswordException();
                    }
                });



        final ForgotPasswordResource authenticationResource =
                new ForgotPasswordResource(userLocalManager, userService,
                        companyAPI, authenticationHelper);


        final Response response1 = authenticationResource.forgotPassword(request, response, forgotPasswordForm);

        System.out.println(response1);
        assertNotNull(response1);
        assertEquals(response1.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        assertNotNull(response1.getEntity());
        System.out.println(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals
                ("a-new-password-can-only-be-sent-to-an-external-email-address"));
    }

    @Test
    public void testUserEmailAddressException() throws Exception {

        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session  = mock(HttpSession.class);
        final UserLocalManager userLocalManager = mock(UserLocalManager.class);
        final ResponseUtil authenticationHelper = ResponseUtil.INSTANCE;
        final CompanyAPI companyAPI = mock(CompanyAPI.class);
        final ApiProvider apiProvider = mock(ApiProvider.class);
        final String userId = "admin@dotcms.com";
        final ServletContext context = mock(ServletContext.class);
        final Company company = new Company() {

            @Override
            public String getAuthType() {

                return Company.AUTH_TYPE_ID;
            }
        };
        final ForgotPasswordForm forgotPasswordForm =
                new ForgotPasswordForm.Builder().userId(userId).build();

        Config.CONTEXT = context;

        when(context.getInitParameter("company_id")).thenReturn(RestUtilTest.DEFAULT_COMPANY);
        when(request.getSession(false)).thenReturn(session); //
        when(session.getAttribute(Globals.LOCALE_KEY)).thenReturn(Locale.getDefault()); //
        when(companyAPI.getCompany(request)).thenReturn(company);
        when(userLocalManager.getUserById(anyString()))
                .thenAnswer(new Answer<User>() { // if this method is called, should fail

                    @Override
                    public User answer(InvocationOnMock invocation) throws Throwable {

                        throw new UserEmailAddressException();
                    }
                });

        final UserService userService = mock(UserService.class);

        final ForgotPasswordResource authenticationResource =
                new ForgotPasswordResource(userLocalManager, userService,
                        companyAPI, authenticationHelper);


        final Response response1 = authenticationResource.forgotPassword(request, response, forgotPasswordForm);

        System.out.println(response1);
        assertNotNull(response1);
        assertEquals(response1.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
        assertNotNull(response1.getEntity());
        System.out.println(response1.getEntity());
        assertTrue(response1.getEntity() instanceof ResponseEntityView);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors());
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().size() > 0);
        assertNotNull(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0));
        assertTrue(ResponseEntityView.class.cast(response1.getEntity()).getErrors().get(0).getErrorCode().equals
                ("please-enter-a-valid-email-address"));
    }

}
