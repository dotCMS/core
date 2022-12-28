package com.dotcms.rest.api.v1.authentication;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

import io.vavr.control.Try;
import org.apache.commons.lang.RandomStringUtils;

/**
 * Util to manage the token that is used in the user reset password proccess.
 */
public class ResetPasswordTokenUtil {

    static String VALIDATE_TOKEN_REGEX="^[a-zA-Z0-9]+:[0-9]+$";
    /**
     * Check if a token is a valid token.
     * there are two reasons why a token could be:
     * <ul>
     *     <li>The token has a wrong syntax, the token have to match the follow regular expression:
     *     <b>^[a-zA-Z0-9]+:[0-9]+$</b></li>
     *     <li>The token expired, the token has a expiration time of 10 minutes by default but it could be
     *     overwrite setting the RECOVER_PASSWORD_TOKEN_TTL_MINS properties</li>
     * </ul>
     *
     * @param user for who the token is check
     * @param token to be check
     * @throws DotInvalidTokenException if the token is invalid, if the token expired then the expired properties
     *          is set to true
     */
    public static void checkToken(final User user, final String token) throws DotInvalidTokenException {
        final String storedToken = user.getIcqId();

        // honey pot token verification for 2 seconds
        Try.run(()->Thread.sleep(Config.getIntProperty("RECOVER_PASSWORD_TOKEN_AUTH_DELAY", 2000)));

        if (UtilMethods.isSet(storedToken) && storedToken.matches(VALIDATE_TOKEN_REGEX) && storedToken.equals(token)
                && UtilMethods.isSet(token)) {
            // check if token expired
            final long minutes = Config.getIntProperty("RECOVER_PASSWORD_TOKEN_TTL_MINS", 10);
            final Instant now = Instant.now();
            final Instant tokenInstant = Instant.ofEpochMilli(Long.parseLong(storedToken.substring(storedToken.indexOf(':') + 1)));

            if(tokenInstant.plus(minutes, ChronoUnit.MINUTES).isBefore(now)){
                throw new DotInvalidTokenException(storedToken, true);
            }
        } else {
            throw new DotInvalidTokenException(storedToken);
        }
    }

    static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Create a token to be used in the user reset password process.
     *
     * The token has the follow syntax: <Random alphanumeric characters>:<currently timestamp>
     *
     * The <b>Random alphanumeric characters</b> part has a default length of 30 characters but a different
     * value could be define setting the RECOVER_PASSWORD_TOKEN_LENGTH properties.
     *
     * @return a newly token
     */
    public static String createToken() {

        final SecureRandom rnd = new SecureRandom();
        final int len = Config.getIntProperty("RECOVER_PASSWORD_TOKEN_LENGTH", 30);

        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(ALPHANUMERIC.charAt(rnd.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString()  + StringPool.COLON + Instant.now().toEpochMilli();
    }

}
