package com.dotcms.rest.api.v1.authentication.url;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;


/**
 * Created by freddyrodriguez on 8/5/16.
 */
public class AngularResetPasswordUrlStrategy implements ResetPasswordUrlStrategy {

    @Override
    public String getResetUserPasswordRelativeURL(User user, String token, Locale locale, Company company) {
        return java.text.MessageFormat.format("/html/ng?resetPassword=true&userId={0}&token={1}", user.getUserId(), token);
    }
}
