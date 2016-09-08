package com.dotcms.util;


import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.rest.api.v1.authentication.url.ResetPasswordUrlStrategy;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;

/**
 * Util class to get the app base URL
 * @author Freddy R
 * @author jsanca
 */
public class  UrlUtil {

    private static final String URL_PREFIX = "://";
    private static final String HTTPS = "https://";

    /**
     * Get's the base url for the company
     * @param company {@link Company}
     * @return String
     */
    public static String getBaseURL(final Company company){

        return (company.getPortalURL().contains(URL_PREFIX) ? StringUtils.EMPTY : HTTPS)
                + company.getPortalURL();
    } // getBaseURL.

    /**
     * Get the absolute reset password url based on the {@link ResetPasswordUrlStrategy}
     * @param company {@link Company}
     * @param user {@link User}
     * @param token {@link String}
     * @param locale {@link Locale}
     * @param resetPasswordUrlStrategy {@link ResetPasswordUrlStrategy}
     * @return String
     */
    public static String getAbsoluteResetPasswordURL(final Company company,
                                                     final User user,
                                                     final String token,
                                                     final Locale locale,
                                                     final ResetPasswordUrlStrategy resetPasswordUrlStrategy) {

            return getBaseURL(company) + resetPasswordUrlStrategy.getResetUserPasswordRelativeURL(user,
                    token, locale, company);
    } // getAbsoluteResetPasswordURL.
} // UrlUtil.
