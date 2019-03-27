package com.dotcms.rest.api.v1.authentication.url;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;


/**
 * Strategy to return the reset user password
 * @author jsanca
 */
public interface UrlStrategy extends Serializable {

    public static final String USER   = "user";
    public static final String TOKEN  = "token";
    public static final String LOCALE = "locale";

    /**
     * Based on the params and context creates an url
     * @param params {@link Map}
     * @return String
     */
    String getURL(Map<String, Object> params);
} // UrlStrategy.
