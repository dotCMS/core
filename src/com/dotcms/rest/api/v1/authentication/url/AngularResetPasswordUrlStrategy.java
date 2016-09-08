package com.dotcms.rest.api.v1.authentication.url;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;


/**
 * Created by freddyrodriguez on 8/5/16.
 */
public class AngularResetPasswordUrlStrategy implements ResetPasswordUrlStrategy {

    private static final String HTML_NG_RESET_PASSWORD_TRUE_USER_ID_0_TOKEN_1 = "/html/ng?resetPassword=true&userId={0}&token={1}";

    @Override
    public String getResetUserPasswordRelativeURL(final User user, final String token, final Locale locale, final Company company) {
        // todo: create here the json web token, with the user and token
        return java.text.MessageFormat.format(HTML_NG_RESET_PASSWORD_TRUE_USER_ID_0_TOKEN_1, user.getUserId(), token);
    } // getResetUserPasswordRelativeURL.

} // E:O:F:AngularResetPasswordUrlStrategy.
