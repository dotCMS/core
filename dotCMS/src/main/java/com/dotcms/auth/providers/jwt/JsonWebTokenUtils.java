package com.dotcms.auth.providers.jwt;

import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import io.jsonwebtoken.IncorrectClaimException;
import io.jsonwebtoken.SignatureException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;

import static com.dotcms.exception.ExceptionUtil.causedBy;

/**
 * Helper to get things in more simple way for the Json Web Tokens
 * @author jsanca
 */
public class JsonWebTokenUtils {

    public static final String CLAIM_UPDATED_AT = "xmod";
    public static final String CLAIM_ALLOWED_NETWORK = "xnet";

    private static class SingletonHolder {
        private static final JsonWebTokenUtils INSTANCE = new JsonWebTokenUtils();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static JsonWebTokenUtils getInstance() {

        return JsonWebTokenUtils.SingletonHolder.INSTANCE;
    } // getInstance.

    private JsonWebTokenUtils() {
        // singleton
        this(JsonWebTokenFactory.getInstance().getJsonWebTokenService());
    }

    @VisibleForTesting
    protected JsonWebTokenUtils(final  JsonWebTokenService jsonWebTokenService) {

        this.jsonWebTokenService = jsonWebTokenService;

    }

    private final JsonWebTokenService jsonWebTokenService;

    /**
     * Returns the active user a login token belongs to.
     *
     * @param jwtAccessToken the signed login token
     * @param ipAddress      the address the request came from
     * @return the user, or {@code null} when the token does not validate or its user does not
     *         exist or is inactive
     */
    public User getUser(final String jwtAccessToken, final String ipAddress) {

        return APILocator.getApiTokenAPI().fromJwt(jwtAccessToken, ipAddress)
                .flatMap(JWToken::getActiveUser).orElse(null);
    } // getUser



    /**
     * Gets from the json web access token, the user id decrypt.
     * This method is static just to keep an easier way to be access on a jsp.
     * @param jwtAccessToken String
     * @return String returns the userId, null if it is not possible to get it.
     */
    public static String getUserIdFromJsonWebToken(final String jwtAccessToken) {

        final JWToken token = getInstance().jsonWebTokenService.parseToken(jwtAccessToken);
        return token!=null ? token.getUserId() : null;
    } // getUserIdFromJsonWebToken

    /**
     * Creates the Json Web Token based on the user
     *
     * @param user User
     * @param jwtMaxAge int how much days to keep the token valid
     * @return String Json Web Token
     */
    public String createUserToken(final User user, int jwtMaxAge) {

        return createUserToken(user, jwtMaxAge > 0 ? DateUtil.daysToMillis(jwtMaxAge) : jwtMaxAge);
    }

    /**
     * Creates a login token for the user that expires the given duration from now.
     *
     * @param user     the user the token logs in
     * @param lifetime how long the token stays valid
     * @return the signed token
     */
    public String createUserToken(final User user, final Duration lifetime) {

        return createUserToken(user, lifetime.toMillis());
    }

    private String createUserToken(final User user, final long ttlMillis) {

        return this.jsonWebTokenService.generateUserToken(
                new UserToken.Builder().id(user.getRememberMeToken())
                        .subject(PublicEncryptionFactory.encryptString(user.getUserId()))
                        .modificationDate(user.getModificationDate())
                        .expiresDate(ttlMillis).build()
        );
    }

    /**
     * When a Invalid JSON Web Token is found this method handles the error
     */
    public void handleInvalidTokenExceptions(final Class from, final Throwable e,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        if (Logger.isDebugEnabled(from)) {
            Logger.debug(from, e.getMessage(), e);
        }

        if (causedBy(e, SignatureException.class, IncorrectClaimException.class)) {
            Logger.warn(from,
                    () -> String.format("An invalid attempt to use a JWT [%s]", e.getMessage()));
        } else {
            //For another type of errors lets show it in the logs as an error
            if (Logger.isErrorEnabled(from)) {
                Logger.error(from, "An invalid attempt to use a JWT", e);
            }
        }

        final String securityLoggerMessage =
             null != request?
                     String.format("An invalid attempt to use an invalid "
                            + "JWT has been made from IP [%s] [%s]", request.getRemoteAddr(), e.getMessage()):
                     String.format("An invalid attempt to use a JWT [%s]", e.getMessage());

        SecurityLogger.logInfo(from, () -> securityLoggerMessage);

        if (null != request && null != response) {
            try {
                //Force a clean up of the invalid token cookie
                APILocator.getLoginServiceAPI().doActionLogout(request, response);
            } catch (Exception internalException) {
                if (Logger.isDebugEnabled(from)) {
                    Logger.debug(from,
                            "Unable to apply a logout action when invalid JWT was found.",
                            internalException);
                }
            }
        }
    }

    /**
     * Expose parse token validation
     * This one is more elaborate since it explores the contents of the token body
     * @param token
     * @return
     */
    public boolean parseToken(final String token){
         return jsonWebTokenService.parseToken(token) != null;
    }

    /**
     * Expose the simple Token validation method
     * This one only validates it is well-formed taking into account the sing key used
     * @param token
     * @return
     */
    public boolean isWellFormed(final String token){
        return jsonWebTokenService.isWellFormed(token);
    }

} // E:O:F:JsonWebTokenUtils.
