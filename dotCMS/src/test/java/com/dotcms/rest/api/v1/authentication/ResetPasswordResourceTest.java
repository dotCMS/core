package com.dotcms.rest.api.v1.authentication;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.RestUtilTest;
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.ejb.UserManager;
import com.liferay.util.LocaleUtil;

public class ResetPasswordResourceTest{

    HttpServletRequest request;
    ResponseUtil responseUtil;
    ResetPasswordForm  resetPasswordForm;
    
    @Before
    public void initTest(){
        request = RestUtilTest.getMockHttpRequest();
        RestUtilTest.initMockContext();
        responseUtil = ResponseUtil.INSTANCE;
        resetPasswordForm = this.getForm();

    }

    @Test
    public void testEmptyParameter() {
        try {
            new ResetPasswordForm.Builder().build();
            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }
    }


    @Test
    public void testWrongParameter() {
        try {
            new ResetPasswordForm.Builder().password("").build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }
    }

    @Test
    public void testDotInvalidPasswordException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException {
        UserManager userManager = getUserManagerThrowingException( new DotInvalidPasswordException("") );
        final JsonWebTokenService jsonWebTokenService = mock(JsonWebTokenService.class);
        final ResponseUtil mResponseUtil = mock(ResponseUtil.class);
        final JWTBean jwtBean = new JWTBean("dotcms.org.1",
                "token",
                "dotcms.org.1", 100000);
        final Locale locale = LocaleUtil.getLocale(request);
        when(jsonWebTokenService.parseToken(eq("token"))).thenReturn(jwtBean);
        when(mResponseUtil.getFormattedMessage(null,"reset-password-invalid-password")).thenReturn("");
        when(mResponseUtil.getErrorResponse(request, Response.Status.BAD_REQUEST, locale, null,
        		"reset-password-invalid-password")).thenCallRealMethod();

        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, mResponseUtil, jsonWebTokenService);
        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);
        RestUtilTest.verifyErrorResponse(response,  Response.Status.BAD_REQUEST.getStatusCode(), "reset-password-invalid-password");
    }

    @Test
    public void testOk() {
        UserManager userManager = mock( UserManager.class );
        final JsonWebTokenService jsonWebTokenService = mock(JsonWebTokenService.class);
        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil, jsonWebTokenService);
        final JWTBean jwtBean = new JWTBean("dotcms.org.1",
                "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkb3RjbXMub3JnLjEiLCJpYXQiOjE0NzM3MTE1OTIsInN1YiI6IlhJazdsUENYUkxWQmlQWWNJOTJpY01MbXVET1ZLeTE0NzM3MTE1OTI5MTIiLCJpc3MiOiJkb3RjbXMub3JnLjEiLCJleHAiOjE0NzM3MTI3OTJ9.65fqPIKHUdfk35uVPy4x9mzhvh2A1EW_UOF2oEc9DUM",
                "dotcms.org.1", 100000);
        when(jsonWebTokenService.parseToken(eq("token"))).thenReturn(jwtBean);
        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);
        RestUtilTest.verifySuccessResponse(response);
    }

    private UserManager getUserManagerThrowingException(Exception e)
            throws NoSuchUserException, DotSecurityException, DotInvalidTokenException {
        UserManager userManager = mock( UserManager.class );
        doThrow( e ).when( userManager ).resetPassword("dotcms.org.1",
                resetPasswordForm.getToken(), resetPasswordForm.getPassword());
        return userManager;
    }

    private ResetPasswordForm getForm(){
        final String password = "admin";
        final String token = "token";

        return new ResetPasswordForm.Builder()
                .password(password)
                .token(token)
                .build();
    }
}
