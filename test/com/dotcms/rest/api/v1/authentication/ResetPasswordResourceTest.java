package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.RestUtilTest;
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.ejb.UserManager;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ResetPasswordResourceTest {

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
            new ResetPasswordForm.Builder().userId("").build();

            fail ("Should throw a ValidationException");
        } catch (Exception e) {
            // quiet
        }
    }

    @Test
    public void testNoSuchUserException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException {
        UserManager userManager = getUserManagerThrowingException( new NoSuchUserException("") );

        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil);

        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);

        RestUtilTest.verifyErrorResponse(response,  Response.Status.BAD_REQUEST.getStatusCode(), "please-enter-a-valid-login");

    }

    @Test
    public void testTokenInvalidException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException {

        UserManager userManager = getUserManagerThrowingException( new DotInvalidTokenException("") );

        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil);

        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);

        RestUtilTest.verifyErrorResponse(response,  Response.Status.BAD_REQUEST.getStatusCode(), "reset-password-token-invalid");
    }


    @Test
    public void testTokenExpiredException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException {
        UserManager userManager = getUserManagerThrowingException( new DotInvalidTokenException("", true) );

        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil);

        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);

        RestUtilTest.verifyErrorResponse(response,  Response.Status.UNAUTHORIZED.getStatusCode(), "reset_token_expired");
    }

    @Test
    public void testDotInvalidPasswordException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException {

        UserManager userManager = getUserManagerThrowingException( new DotInvalidPasswordException("") );
        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil);

        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);

        RestUtilTest.verifyErrorResponse(response,  Response.Status.BAD_REQUEST.getStatusCode(), "reset-password-invalid-password");
    }

    @Test
    public void testOk() {
        UserManager userManager = mock( UserManager.class );
        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil);

        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);

        RestUtilTest.verifySuccessResponse(response);
    }

    private UserManager getUserManagerThrowingException(Exception e)
            throws NoSuchUserException, DotSecurityException, DotInvalidTokenException {
        UserManager userManager = mock( UserManager.class );
        doThrow( e ).when( userManager ).resetPassword(resetPasswordForm.getUserId(),
                resetPasswordForm.getToken(), resetPasswordForm.getPassword());
        return userManager;
    }



    private ResetPasswordForm getForm(){
        final String userId = "admin@dotcms.com";
        final String password = "admin";
        final String token = "token";

        return new ResetPasswordForm.Builder()
                .userId(userId)
                .password(password)
                .token(token)
                .build();


    }
}
