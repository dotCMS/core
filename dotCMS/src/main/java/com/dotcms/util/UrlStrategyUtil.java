package com.dotcms.util;


import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.rest.api.v1.authentication.url.UrlStrategy;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;
import java.util.Map;

/**
 * Util class to get the app base URL
 * @author Freddy R
 * @author jsanca
 */
public class UrlStrategyUtil {

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
     * Get the absolute reset password url based on the {@link UrlStrategy}
     * @param company {@link Company}
     * @param params  {@link Map}
     * @param urlStrategy {@link UrlStrategy}
     * @return String
     */
    public static String getURL(final Company company,
                                final Map<String, Object> params,
                                final UrlStrategy urlStrategy) {

            return getBaseURL(company) + urlStrategy.getURL(params);
    } // getURL.
} // UrlStrategyUtil.
