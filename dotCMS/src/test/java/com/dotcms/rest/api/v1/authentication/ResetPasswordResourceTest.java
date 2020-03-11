package com.dotcms.rest.api.v1.authentication;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import javax.ws.rs.core.Response;
import com.dotcms.rest.RestUtilTest;
import com.dotmarketing.business.DotInvalidPasswordException;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.ejb.UserManager;
import com.liferay.util.LocaleUtil;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
@PrepareForTest({ResponseUtil.class})
@RunWith(PowerMockRunner.class)
public class ResetPasswordResourceTest extends UnitTestBase {

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

        final UserToken jwtBean = new UserToken(UUID.randomUUID().toString(), "dotcms.org.1", "dummy_cluster_id",
                new Date(), 100000,"");
        when(jsonWebTokenService.parseToken(eq("token1"))).thenReturn(jwtBean);

        final Locale locale = LocaleUtil.getLocale(request);
        PowerMockito.mockStatic(ResponseUtil.class);
        PowerMockito.when(ResponseUtil.getFormattedMessage(null, "reset-password-invalid-password"))
                .thenReturn("");
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
        final UserToken jwtBean = new UserToken(UUID.randomUUID().toString(), "dotcms.org.1", "dummy_cluster_id",
                new Date(), 100000,"");
        when(jsonWebTokenService.parseToken(eq("token1"))).thenReturn(jwtBean);
        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);
        RestUtilTest.verifySuccessResponse(response);
    }

    private UserManager getUserManagerThrowingException(Exception e)
            throws NoSuchUserException, DotSecurityException, DotInvalidTokenException {
        UserManager userManager = mock( UserManager.class );
        doThrow( e ).when( userManager ).resetPassword("dotcms.org.1",
                "token2", resetPasswordForm.getPassword());
        return userManager;
    }

    private ResetPasswordForm getForm(){
        final String password = "admin";
        final String token = "token1+++token2";

        return new ResetPasswordForm.Builder()
                .password(password)
                .token(token)
                .build();
    }
}
