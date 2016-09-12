package com.dotcms.rest.api.v1.authentication.url;


import com.dotcms.util.UrlUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;


/**
 * Created by freddyrodriguez on 8/5/16.
 */
public class DefaultResetPasswordUrlStrategy implements ResetPasswordUrlStrategy {

    @Override
    public String getResetUserPasswordRelativeURL(User user, String token, Locale locale, Company company) {
        return "/c/portal_public/login?my_account_cmd=ereset&my_user_id=" + user.getUserId() +
                "&token=" + token + "&switchLocale=" + locale.getLanguage() + "_" + locale.getCountry();
    }
}
