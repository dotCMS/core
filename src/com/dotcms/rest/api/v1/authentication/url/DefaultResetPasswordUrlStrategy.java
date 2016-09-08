package com.dotcms.rest.api.v1.authentication.url;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;


/**
 * Created by freddyrodriguez on 8/5/16.
 */
public class DefaultResetPasswordUrlStrategy implements ResetPasswordUrlStrategy {

    private static final String C_PORTAL_PUBLIC_LOGIN_MY_ACCOUNT_CMD_ERESET_MY_USER_ID = "/c/portal_public/login?my_account_cmd=ereset&my_user_id=";
    private static final String TOKEN = "&token=";
    private static final String SWITCH_LOCALE = "&switchLocale=";
    private static final String UNDERSCORE = "_";

    @Override
    public String getResetUserPasswordRelativeURL(final User user, final String token, final Locale locale, final Company company) {

        return new StringBuilder(C_PORTAL_PUBLIC_LOGIN_MY_ACCOUNT_CMD_ERESET_MY_USER_ID).append(user.getUserId())
                .append(TOKEN).append(token).append(SWITCH_LOCALE).append(locale.getLanguage())
                .append(UNDERSCORE).append(locale.getCountry()).toString();
    } // getResetUserPasswordRelativeURL.

} // E:O:F:DefaultResetPasswordUrlStrategy.
