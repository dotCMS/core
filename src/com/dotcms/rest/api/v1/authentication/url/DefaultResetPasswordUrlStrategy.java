package com.dotcms.rest.api.v1.authentication.url;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;
import java.util.Map;


/**
 * This {@link UrlStrategy} is the default one for the web site legacy site.
 * @author jsanca
 */
public class DefaultResetPasswordUrlStrategy implements UrlStrategy {

    private static final String C_PORTAL_PUBLIC_LOGIN_MY_ACCOUNT_CMD_ERESET_MY_USER_ID = "/c/portal_public/login?my_account_cmd=ereset&my_user_id=";
    private static final String TOKEN = "&token=";
    private static final String SWITCH_LOCALE = "&switchLocale=";
    private static final String UNDERSCORE = "_";

    @Override
    public String getURL(final Map<String, Object> params) {

        final User user       = (User)    params.get(USER);
        final String token    = (String)  params.get(TOKEN);
        final Locale locale   = (Locale)  params.get(LOCALE);

        return new StringBuilder(C_PORTAL_PUBLIC_LOGIN_MY_ACCOUNT_CMD_ERESET_MY_USER_ID).append(user.getUserId())
                .append(TOKEN).append(token).append(SWITCH_LOCALE).append(locale.getLanguage())
                .append(UNDERSCORE).append(locale.getCountry()).toString();
    } // getResetUserPasswordRelativeURL.

} // E:O:F:DefaultResetPasswordUrlStrategy.
