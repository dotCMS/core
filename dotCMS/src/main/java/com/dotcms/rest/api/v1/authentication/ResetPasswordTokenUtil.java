package com.dotcms.rest.api.v1.authentication;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang.RandomStringUtils;

/**
 * Util to manage the token that is used in the user reset password proccess.
 */
public class ResetPasswordTokenUtil {

    /**
     * Check if a token is a valid token.
     * there are two reasons why a token could be:
     * <ul>
     *     <li>The token has a wrong syntax, the token have to match the follor regular expression:
     *     <b>^[a-zA-Z0-9]+:[0-9]+$</b></li>
     *     <li>The token expired, the token has a expiration time of 20 minutes by default but it could be
     *     overwrite setting the RECOVER_PASSWORD_TOKEN_TTL_MINS properties</li>
     * </ul>
     *
     * @param user for who the token is check
     * @param token to be check
     * @throws DotInvalidTokenException if the token is invalid, if the token expired then the expired properties
     *          is set to true
     */
    public static void checkToken(final User user, final String token) throws DotInvalidTokenException {
        final String userIcqId = user.getIcqId();
        if(UtilMethods.isSet(userIcqId) && userIcqId.matches("^[a-zA-Z0-9]+:[0-9]+$") && userIcqId.equals(token) && UtilMethods.isSet(token)) {
                // check if token expired
                Calendar ttl = Calendar.getInstance();
                ttl.setTimeInMillis(Long.parseLong(userIcqId.substring(userIcqId.indexOf(':')+1)));
                ttl.add(Calendar.MINUTE, Config.getIntProperty("RECOVER_PASSWORD_TOKEN_TTL_MINS", 20));
                if(!ttl.after(Calendar.getInstance())) {
                    throw new DotInvalidTokenException(userIcqId, true);
                }
        }else{
            throw new DotInvalidTokenException(userIcqId);
        }
    }

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
    public static String createToken(){
        return RandomStringUtils.randomAlphanumeric( Config.getIntProperty( "RECOVER_PASSWORD_TOKEN_LENGTH", 30 ) )
                + new Date().getTime() + StringPool.COLON + new Date().getTime();
    }

}
