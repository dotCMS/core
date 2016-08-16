package com.dotcms.auth.providers.jwt;

import com.dotcms.auth.providers.jwt.beans.DotCMSSubjectBean;
import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.util.marshal.MarshalFactory;
import com.dotcms.util.marshal.MarshalUtils;
import com.dotcms.util.security.Encryptor;
import com.dotcms.util.security.EncryptorFactory;
import com.dotmarketing.util.Logger;
import com.liferay.portal.ejb.CompanyLocalManager;
import com.liferay.portal.ejb.CompanyLocalManagerFactory;

/**
 * Helper to get things in more simple way.
 * @author jsanca
 */
// todo: this class should be eventually removed when the index.jsp not more required.
public class JsonWebTokenUtils {

    private static JsonWebTokenService jsonWebTokenService =
            JsonWebTokenFactory.getInstance().getJsonWebTokenService();

    private static MarshalUtils marshalUtils =
            MarshalFactory.getInstance().getMarshalUtils();

    private static CompanyLocalManager companyLocalManager =
            CompanyLocalManagerFactory.getManager();

    private static Encryptor encryptor =
            EncryptorFactory.getInstance().getEncryptor();

    /**
     * Gets from the json web access token, the user id decrypt.
     * @param jwtAccessToken String
     * @return String returns the userId, null if it is not possible to get it.
     */
    public static String getUserIdFromJsonWebToken(final String jwtAccessToken) {

        String userId = null;
        JWTBean jwtBean = null;

        try {

            jwtBean = jsonWebTokenService.parseToken(jwtAccessToken);

            if (null != jwtBean && jwtBean.getTtlMillis() - System.currentTimeMillis() > 0) {

                final DotCMSSubjectBean subject =
                        marshalUtils.unmarshal(jwtBean.getSubject(), DotCMSSubjectBean.class);

                if (null != subject) {

                    userId = encryptor.decrypt(
                            companyLocalManager.getCompany(subject.getCompanyId()).getKeyObj(),
                            subject.getUserId());
                }
            }
        } catch (Exception e) {

            Logger.error(JsonWebTokenUtils.class, e.getMessage(), e);
            userId = null;
        }

        return userId;
    } // getUserIdFromJsonWebToken

} // E:O:F:JsonWebTokenUtils.
