package com.dotcms.rest.api.v1.authentication;

import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.ejb.UserUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ResetPasswordTokenUtilTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ResetPasswordTokenUtil#createToken()}
     * Given Scenario: create a token for the reset password process
     * ExpectedResult: token with the syntax <Random alphanumeric characters>:<currently timestamp>
     *                  The length of the <Random alphanumeric characters> part is defined by a property,
     *                  default value is 30 characters.
     */
    @Test
    public void test_createToken_success(){
        final String token = ResetPasswordTokenUtil.createToken();

        Assert.assertTrue(UtilMethods.isSet(token));
        Assert.assertTrue(token.matches("^[a-zA-Z0-9]+:[0-9]+$"));
        final String randomPart = token.substring(0,token.indexOf(':'));
        Assert.assertEquals(Config.getIntProperty( "RECOVER_PASSWORD_TOKEN_LENGTH", 30 ), randomPart.length());
    }

    /**
     * Method to test: {@link ResetPasswordTokenUtil#checkToken(User, String)}
     * Given Scenario: Create an user and a token, associate the token to the user (icqId field),
     *                  check that the token is valid and belongs to the user.
     * ExpectedResult: no error should be thrown.since token is valid and belongs to the user.
     */
    @Test
    public void test_checkToken_success() throws Exception {
        final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String token = ResetPasswordTokenUtil.createToken();
        newUser.setIcqId(token);
        UserUtil.update(newUser);

        ResetPasswordTokenUtil.checkToken(newUser,token);
    }

    /**
     * Method to test: {@link ResetPasswordTokenUtil#checkToken(User, String)}
     * Given Scenario: Create an user and a token, but never associate the token to the user (icqId field).
     * ExpectedResult: DotInvalidTokenException, since icqId field of the user is not set.
     */
    @Test(expected = DotInvalidTokenException.class)
    public void test_checkToken_userIcqIdNotSet_throwDotInvalidTokenException() throws Exception {
        final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String token = ResetPasswordTokenUtil.createToken();

        ResetPasswordTokenUtil.checkToken(newUser,token);
    }

    /**
     * Method to test: {@link ResetPasswordTokenUtil#checkToken(User, String)}
     * Given Scenario: Create an user and a token (create the token manually not using the createToken method),
     *                 associate the token to the user (icqId field).
     * ExpectedResult: DotInvalidTokenException, since icqId(token) does not match the regex
     */
    @Test(expected = DotInvalidTokenException.class)
    public void test_checkToken_userIcqIdNotMatchingRegex_throwDotInvalidTokenException() throws Exception {
        final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String token = RandomStringUtils.randomAlphanumeric(10);
        newUser.setIcqId(token);
        UserUtil.update(newUser);

        ResetPasswordTokenUtil.checkToken(newUser,token);
    }

    /**
     * Method to test: {@link ResetPasswordTokenUtil#checkToken(User, String)}
     * Given Scenario: Create an user and a token, associate the token to the user (icqId field).
     *                  Call the checkToken method but pass another token.
     * ExpectedResult: DotInvalidTokenException, since icqId value is not the same as the token provided.
     */
    @Test(expected = DotInvalidTokenException.class)
    public void test_checkToken_userIcqIdNotEqualsToken_throwDotInvalidTokenException() throws Exception {
        final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final String token = ResetPasswordTokenUtil.createToken();
        newUser.setIcqId(token);
        UserUtil.update(newUser);

        ResetPasswordTokenUtil.checkToken(newUser,ResetPasswordTokenUtil.createToken());
    }

    /**
     * Method to test: {@link ResetPasswordTokenUtil#checkToken(User, String)}
     * Given Scenario: Create an user and a token, associate the token to the user (icqId field).
     *                  But the token created was 2 hours ago.
     * ExpectedResult: DotInvalidTokenException, since the token is expired (20 min is the default TTL of the token).
     */
    @Test(expected = DotInvalidTokenException.class)
    public void test_checkToken_tokenExpired_throwDotInvalidTokenException() throws Exception {
        final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        final Calendar timeTwoHoursAgo = Calendar.getInstance();
        timeTwoHoursAgo.setTime(new Date());
        timeTwoHoursAgo.add(Calendar.HOUR,-2);
        final String token = RandomStringUtils.randomAlphanumeric( Config.getIntProperty( "RECOVER_PASSWORD_TOKEN_LENGTH", 30 ) )
                + StringPool.COLON + timeTwoHoursAgo.getTimeInMillis();
        newUser.setIcqId(token);
        UserUtil.update(newUser);

        ResetPasswordTokenUtil.checkToken(newUser,token);
    }
}
