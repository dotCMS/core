package com.dotcms.rest.api.v1.authentication;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import javax.ws.rs.core.Response;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.NoSuchUserException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyPool;
import com.liferay.portal.ejb.UserManager;
import com.liferay.portal.model.Company;
import java.util.Date;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ResetPasswordResourceIntegrationTest{

    HttpServletRequest request;
    ResponseUtil responseUtil;
    ResetPasswordForm  resetPasswordForm;
    
    @BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
        final Company company = new Company() {

            @Override
            public String getAuthType() {

                return Company.AUTH_TYPE_ID;
            }
        };
        CompanyPool.put(RestUtilTest.DEFAULT_COMPANY, company);
	}

    @Before
    public void initTest(){
        request = RestUtilTest.getMockHttpRequest();
        RestUtilTest.initMockContext();
        responseUtil = ResponseUtil.INSTANCE;
        resetPasswordForm = this.getForm();
    }

    @Test
    public void testNoSuchUserException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException, SystemException, PortalException, DotDataException {
        UserManager userManager = getUserManagerThrowingException( new NoSuchUserException("") );
        final User user = APILocator.getUserAPI().loadUserById("dotcms.org.1");
        final JsonWebTokenService jsonWebTokenService = mock(JsonWebTokenService.class);
        final UserToken jwtBean = new UserToken(UUID.randomUUID().toString(),
                "dotcms.org.1",
                new Date(), 100000, user.getSkinId());

        when(jsonWebTokenService.parseToken(eq("token1"))).thenReturn(jwtBean);
        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil, jsonWebTokenService);
        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);

        RestUtilTest.verifyErrorResponse(response,  Response.Status.BAD_REQUEST.getStatusCode(), "please-enter-a-valid-login");
    }

    @Test
    public void testTokenInvalidException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException, DotDataException {
        UserManager userManager = getUserManagerThrowingException( new DotInvalidTokenException("") );
        final User user = APILocator.getUserAPI().loadUserById("dotcms.org.1");
        final JsonWebTokenService jsonWebTokenService = mock(JsonWebTokenService.class);
        final UserToken jwtBean = new UserToken(UUID.randomUUID().toString(),
                "dotcms.org.1",
                new Date(), 100000, user.getSkinId());
        when(jsonWebTokenService.parseToken(eq("token1"))).thenReturn(jwtBean);
        
        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil, jsonWebTokenService);
        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);
        RestUtilTest.verifyErrorResponse(response,  Response.Status.BAD_REQUEST.getStatusCode(), "reset-password-token-invalid");
    }

    @Test
    public void testTokenExpiredException() throws DotSecurityException, NoSuchUserException, DotInvalidTokenException, DotDataException {
        UserManager userManager = getUserManagerThrowingException( new DotInvalidTokenException("", true) );
        final User user = APILocator.getUserAPI().loadUserById("dotcms.org.1");
        final JsonWebTokenService jsonWebTokenService = mock(JsonWebTokenService.class);
        final UserToken jwtBean = new UserToken(UUID.randomUUID().toString(),
                "dotcms.org.1",
                new Date(), 100000, user.getSkinId());

        when(jsonWebTokenService.parseToken(eq("token1"))).thenReturn(jwtBean);
        ResetPasswordResource resetPasswordResource = new ResetPasswordResource(userManager, responseUtil, jsonWebTokenService);
        Response response = resetPasswordResource.resetPassword(request, resetPasswordForm);

        RestUtilTest.verifyErrorResponse(response,  Response.Status.UNAUTHORIZED.getStatusCode(), "reset-password-token-expired");
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
    
    @AfterClass
    public static void cleanUp(){
    	CompanyPool.remove(RestUtilTest.DEFAULT_COMPANY);
    }

}
