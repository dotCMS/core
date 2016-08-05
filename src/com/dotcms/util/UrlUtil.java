package com.dotcms.util;


import com.dotcms.rest.api.v1.authentication.url.AngularResetPasswordUrlStrategy;
import com.dotcms.rest.api.v1.authentication.url.DefaultResetPasswordUrlStrategy;
import com.dotcms.rest.api.v1.authentication.url.ResetPasswordUrlStrategy;
import com.liferay.portal.ejb.CompanyUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;

/**
 * Util class to get the app base URL
 */
public abstract class  UrlUtil {

    private static final ResetPasswordUrlStrategy ANGULAR_RESET_PASSWORD_URL_STRATEGY =
            new AngularResetPasswordUrlStrategy();
    private static final ResetPasswordUrlStrategy DEFAULT_RESET_PASSWORD_URL_STRATEGY =
            new DefaultResetPasswordUrlStrategy();

    public static String getBaseURL(Company company){
        return (company.getPortalURL().contains("://") ? "" : "https://") + company.getPortalURL();
    }

    public static String getAbsoluteResetPasswordURL(Company company, User user, String token, Locale locale,
                                                     boolean fromAngular){

         if ( !fromAngular ) {
            return getBaseURL(company) + ANGULAR_RESET_PASSWORD_URL_STRATEGY.getResetUserPasswordRelativeURL(user,
                    token, locale, company);
        }else{
            return getBaseURL(company) + DEFAULT_RESET_PASSWORD_URL_STRATEGY.getResetUserPasswordRelativeURL(user,
                    token, locale, company);
        }
    }
}
