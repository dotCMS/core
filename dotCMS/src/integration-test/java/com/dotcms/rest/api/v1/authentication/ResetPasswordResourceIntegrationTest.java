package com.dotcms.rest.api.v1.authentication;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.liferay.portal.ejb.UserUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Calendar;
import java.util.Date;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public class ResetPasswordResourceIntegrationTest{

    static ResetPasswordResource resource;

    @BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();

        resource = new ResetPasswordResource();
	}

    private HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                                .request())
                        .request());

        return request;
    }

    private ResetPasswordForm getResetPasswordForm(final String newPassword, final String token){
        return new ResetPasswordForm.Builder()
                .password(newPassword)
                .token(token)
                .build();
    }

    /**
     * Method to test: {@link ResetPasswordResource#resetPassword(HttpServletRequest, ResetPasswordForm)}
     * Given Scenario: Create an user and a token, associate the token to the user (icqId field),
     *                  call the resource.
     * ExpectedResult: 200, password changed successfully.
     */
	@Test
    public void test_resetPassword_success() throws Exception{
        User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String oldPassword = newUser.getPassword();
        final String token = ResetPasswordTokenUtil.createToken();
        newUser.setIcqId(token);
        UserUtil.update(newUser);

        //Call Resource
        final Response responseResource = resource.resetPassword(getHttpRequest(),getResetPasswordForm("n3wPa$$w0rD",token));
        //Check that the response is 200, OK
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        //Check password has changed
        Assert.assertNotEquals(oldPassword, APILocator.getUserAPI().loadUserById(newUser.getUserId()).getPassword());
    }

    /**
     * Method to test: {@link ResetPasswordResource#resetPassword(HttpServletRequest, ResetPasswordForm)}
     * Given Scenario: Create a token, but no associate the token to any user (icqId field),
     *                  call the resource.
     * ExpectedResult: 400, token invalid since it's no associated to any user.
     */
    @Test
    public void test_resetPassword_tokenNotBelongAnyUser_badRequest() {
        final String token = ResetPasswordTokenUtil.createToken();

        //Call Resource
        final Response responseResource = resource.resetPassword(getHttpRequest(),getResetPasswordForm("n3wPa$$w0rD",token));
        //Check that the response is 400
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        Assert.assertEquals("reset-password-token-invalid",responseEntityView.getErrors().get(0).getErrorCode());
    }

    /**
     * Method to test: {@link ResetPasswordResource#resetPassword(HttpServletRequest, ResetPasswordForm)}
     * Given Scenario: Create an user and a token (but the token is two hours old), associate the token to the user (icqId field),
     *                  call the resource.
     * ExpectedResult: 401, token expired.
     */
    @Test
    public void testEXPIREDTOKEN() throws Exception{
        final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final Calendar timeTwoHoursAgo = Calendar.getInstance();
        timeTwoHoursAgo.setTime(new Date());
        timeTwoHoursAgo.add(Calendar.HOUR,-2);
        final String token = RandomStringUtils
                .randomAlphanumeric( Config.getIntProperty( "RECOVER_PASSWORD_TOKEN_LENGTH", 30 ) )
                + StringPool.COLON + timeTwoHoursAgo.getTimeInMillis();
        newUser.setIcqId(token);
        UserUtil.update(newUser);

        //Call Resource
        final Response responseResource = resource.resetPassword(getHttpRequest(),getResetPasswordForm("n3wPa$$w0rD",token));
        //Check that the response is 401
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        Assert.assertEquals("reset-password-token-expired",responseEntityView.getErrors().get(0).getErrorCode());
    }

    /**
     * Method to test: {@link ResetPasswordResource#resetPassword(HttpServletRequest, ResetPasswordForm)}
     * Given Scenario: Create an user and a token, associate the token to the user (icqId field),
     *                  call the resource.
     * ExpectedResult: 400, password does not meet requirements.
     */
    @Test
    public void testINVALIDPASSWORD() throws Exception{
        User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String token = ResetPasswordTokenUtil.createToken();
        newUser.setIcqId(token);
        UserUtil.update(newUser);

        //Call Resource
        final Response responseResource = resource.resetPassword(getHttpRequest(),getResetPasswordForm("admin",token));
        //Check that the response is 400
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        Assert.assertEquals("reset-password-invalid-password",responseEntityView.getErrors().get(0).getErrorCode());
    }


}
