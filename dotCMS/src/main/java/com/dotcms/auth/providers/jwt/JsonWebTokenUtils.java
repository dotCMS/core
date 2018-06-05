package com.dotcms.auth.providers.jwt;

import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.business.LazyUserAPIWrapper;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.User;
import java.util.Date;

/**
 * Helper to get things in more simple way.
 * @author jsanca
 */
public class JsonWebTokenUtils {

    public static final String CLAIM_UPDATED_AT = "updated_at";

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
        this(JsonWebTokenFactory.getInstance().getJsonWebTokenService(),
                new LazyUserAPIWrapper());
    }

    @VisibleForTesting
    protected JsonWebTokenUtils(final  JsonWebTokenService jsonWebTokenService,
                             final  UserAPI userAPI) {

        this.jsonWebTokenService = jsonWebTokenService;
        this.userAPI             = userAPI;
    }

    private final JsonWebTokenService jsonWebTokenService;
    private final UserAPI userAPI;

    /**
     * Gets from the json web access token, the subject.
     *
     * @param jwtAccessToken String
     * @return String returns the subject, if the subjet does not exists or is a invalid token will
     * return null
     */
    public String getSubject(final String jwtAccessToken) {

        JWTBean jwtBean;
        String subject = null;

        try {

            jwtBean = this.jsonWebTokenService.parseToken(jwtAccessToken);
            if (null != jwtBean) {
                subject = jwtBean.getSubject();
            }
        } catch (Exception e) {
            Logger.error(JsonWebTokenUtils.class, e.getMessage(), e);
            subject = null;
        }

        return subject;
    }

    /**
     * Gets from the json web access token, the user.
     *
     * @param jwtAccessToken String
     * @return String returns the User, if the user does not exists or is invalid will return null;
     */
    public User getUser(final String jwtAccessToken) {

        User userToReturn = null;
        IsValidResult isValidResult;

        try {

            //Parse the token
            JWTBean jwtBean = this.jsonWebTokenService.parseToken(jwtAccessToken);
            if (null != jwtBean) {

                //Read the user id
                String subject = jwtBean.getSubject();
                if (null != subject) {

                    isValidResult = this.isValidUser(subject, jwtBean.getModificationDate());

                    if (isValidResult.isValid()) {
                        userToReturn = isValidResult.getUser();
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(JsonWebTokenUtils.class, e.getMessage(), e);
            userToReturn = null;
        }

        return userToReturn;
    } // getUser

    private IsValidResult isValidUser(final String userId, final Date lastModifiedDate)
            throws DotSecurityException, DotDataException {

        boolean isValidUser = false;
        User user = null;

        if (null != userId) {

            user = this.userAPI.loadUserById(userId);

            // The user hasn't change since the creation of the JWT
            isValidUser = ((null != user) && (0 == user.getModificationDate()
                    .compareTo(lastModifiedDate)));
        }

        return new IsValidResult(isValidUser, user);
    } // isValidUser.

    /**
     * Gets from the json web access token, the user id decrypt.
     *
     * @param jwtAccessToken String
     * @return String returns the userId, null if it is not possible to get it.
     */
    public String getUserId(final String jwtAccessToken) {

        String userId = null;

        try {
            final String subject = this.getSubject(jwtAccessToken);
            if (null != subject) {
                userId = subject;
            }
        } catch (Exception e) {
            Logger.error(JsonWebTokenUtils.class, e.getMessage(), e);
            userId = null;
        }

        return userId;
    } // getUserId

    /**
     * Gets from the json web access token, the user id decrypt.
     * This method is static just to keep an easier way to be access on a jsp.
     * @param jwtAccessToken String
     * @return String returns the userId, null if it is not possible to get it.
     */
    public static String getUserIdFromJsonWebToken(final String jwtAccessToken) {

        return getInstance().getUserId(jwtAccessToken);
    } // getUserIdFromJsonWebToken

    /**
     * Creates the Json Web Token based on the user
     * @param user User
     * @param jwtMaxAge int how much days to keep the token valid
     * @return String Json Web Token
     */
    public String createToken(final User user, int jwtMaxAge) throws SystemException, PortalException {

        final String encryptUserId
                = UserManagerUtil.encryptUserId(user.getUserId());

        return  this.jsonWebTokenService.generateToken(
                        new JWTBean(encryptUserId,
                                encryptUserId,
                                user.getModificationDate(),
                                (jwtMaxAge > 0)?
                                        DateUtil.daysToMillis(jwtMaxAge):
                                        jwtMaxAge
                        )
                );

    } // getUserIdFromJsonWebToken


    private class IsValidResult {

        private final boolean valid;
        private final User user;

        private IsValidResult(final boolean valid, final User user) {

            this.valid   = valid;
            this.user    = user;
        }

        public boolean isValid() {
            return valid;
        }

        public User getUser() {
            return user;
        }
    }
} // E:O:F:JsonWebTokenUtils.
