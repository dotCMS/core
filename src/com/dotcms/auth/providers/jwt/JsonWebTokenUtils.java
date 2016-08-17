package com.dotcms.auth.providers.jwt;

import com.dotcms.auth.providers.jwt.beans.DotCMSSubjectBean;
import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.ejb.CompanyLocalManager;
import com.liferay.portal.ejb.CompanyLocalManagerFactory;
import com.liferay.portal.ejb.UserManagerUtil;
import com.liferay.portal.model.User;

/**
 * Helper to get things in more simple way.
 * @author jsanca
 */
public class JsonWebTokenUtils {

    public static final JsonWebTokenUtils INSTANCE = new JsonWebTokenUtils();

    private JsonWebTokenUtils() {
        // singleton
    }

    private final JsonWebTokenService jsonWebTokenService =
            JsonWebTokenFactory.getInstance().getJsonWebTokenService();

    private final  MarshalUtils marshalUtils =
            MarshalFactory.getInstance().getMarshalUtils();

    private final  CompanyLocalManager companyLocalManager =
            CompanyLocalManagerFactory.getManager();

    private final  Encryptor encryptor =
            EncryptorFactory.getInstance().getEncryptor();


    private final UserAPI userAPI = APILocator.getUserAPI();


    /**
     * Check if the Json Web token is valid.
     * @param jwtBean {@link JWTBean}
     * @return boolean true if it is valid
     */
    public static boolean isJsonWebTokenValid (final JWTBean jwtBean) {

        return jwtBean.getTtlMillis() - System.currentTimeMillis() > 0;
    } // isJsonWebTokenValid.

    /**
     * Gets from the json web access token, the subject.
     * @param jwtAccessToken String
     * @return String returns the User, if the user does not exists or is invalid will return null;
     */
    public DotCMSSubjectBean getSubject(final String jwtAccessToken) {

        JWTBean jwtBean = null;
        DotCMSSubjectBean subject = null;

        try {

            jwtBean = this.jsonWebTokenService.parseToken(jwtAccessToken);

            if (null != jwtBean && isJsonWebTokenValid(jwtBean)) {

                subject =
                        this.marshalUtils.unmarshal(jwtBean.getSubject(), DotCMSSubjectBean.class);

            }
        } catch (Exception e) {

            Logger.error(JsonWebTokenUtils.class, e.getMessage(), e);
            subject = null;
        }

        return subject;
    } // getUserId

    /**
     * Gets from the json web access token, the user.
     * @param jwtAccessToken String
     * @return String returns the User, if the user does not exists or is invalid will return null;
     */
    public static User getUserFromJsonWebToken(final String jwtAccessToken) {

        return INSTANCE.getUser(jwtAccessToken);
    }

    /**
     * Gets from the json web access token, the user.
     * @param jwtAccessToken String
     * @return String returns the User, if the user does not exists or is invalid will return null;
     */
    public User getUser(final String jwtAccessToken) {

        User userToReturn = null;
        String userId = null;
        IsValidResult isValidResult = null;

        try {

            final DotCMSSubjectBean subject =
                    this.getSubject(jwtAccessToken);

            if (null != subject) {

                userId = this.encryptor.decrypt(
                        this.companyLocalManager.getCompany(subject.getCompanyId()).getKeyObj(),
                        subject.getUserId());

                isValidResult = this.isValidUser(userId, subject);

                if (isValidResult.isValid()) {

                        userToReturn = isValidResult.getUser();
                }
            }
        } catch (Exception e) {

            Logger.error(JsonWebTokenUtils.class, e.getMessage(), e);
            userToReturn = null;
        }

        return userToReturn;
    } // getUser

    private IsValidResult isValidUser (final String userId, final DotCMSSubjectBean subject) throws DotSecurityException, DotDataException {

        boolean isValidUser = false;
        User user = null;

        if (null != userId) {

            user = this.userAPI.loadUserById(userId);

            // The user hasn't change since the creation of the JWT
            isValidUser = ((null != user) &&  (0 == user.getModificationDate().compareTo(subject.getLastModified())));
        }

        return new IsValidResult(isValidUser, user);
    } // isValidUser.

    /**
     * Gets from the json web access token, the user id decrypt.
     * @param jwtAccessToken String
     * @return String returns the userId, null if it is not possible to get it.
     */
    public String getUserId(final String jwtAccessToken) {

        String userId = null;

        try {

            final DotCMSSubjectBean subject =
                    this.getSubject(jwtAccessToken);

            if (null != subject) {

                userId = this.encryptor.decrypt(
                        this.companyLocalManager.getCompany(subject.getCompanyId()).getKeyObj(),
                        subject.getUserId());
            }
        } catch (Exception e) {

            Logger.error(JsonWebTokenUtils.class, e.getMessage(), e);
            userId = null;
        }

        return userId;
    } // getUserId

    /**
     * Gets from the json web access token, the user id decrypt.
     * @param jwtAccessToken String
     * @return String returns the userId, null if it is not possible to get it.
     */
    public static String getUserIdFromJsonWebToken(final String jwtAccessToken) {

        return INSTANCE.getUserId(jwtAccessToken);
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
                                this.marshalUtils.marshal(
                                        new DotCMSSubjectBean(user.getModificationDate(),
                                                encryptUserId,
                                                user.getCompanyId())),
                                encryptUserId,
                                (jwtMaxAge > 0)?
                                        DateUtil.daysToMillis(jwtMaxAge):
                                        jwtMaxAge
                        )
                );

    } // getUserIdFromJsonWebToken

    /**
     * Creates the Json Web Token based on the user
     * @param user User
     * @return String Json Web Token
     */
    public static String createJsonWebToken(final User user, int jwtMaxAge) throws SystemException, PortalException {

        return INSTANCE.createToken(user, jwtMaxAge);
    }


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
