package com.dotcms.rest.api.v1.authentication.url;


import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;


/**
 * Strategy to return the reset user password
 */
public interface ResetPasswordUrlStrategy {
    String getResetUserPasswordRelativeURL(User user, String token, Locale locale, Company company);
}
