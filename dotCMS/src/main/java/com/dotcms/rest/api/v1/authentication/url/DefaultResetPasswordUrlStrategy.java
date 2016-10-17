package com.dotcms.rest.api.v1.authentication.url;


import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

import java.util.Locale;
import java.util.Map;


/**
 * This {@link UrlStrategy} is the default one for the web site legacy site.
 * @author jsanca
 */
public class DefaultResetPasswordUrlStrategy implements UrlStrategy {

    private static final String C_PORTAL_PUBLIC_LOGIN_MY_ACCOUNT_CMD_ERESET_MY_USER_ID = "/c/portal_public/login?my_account_cmd=ereset";
    private static final String TOKEN_PARAM = "&token=";
    private static final String SWITCH_LOCALE = "&switchLocale=";
    private static final String UNDERSCORE = "_";
    private final JsonWebTokenService jsonWebTokenService;
    private final long jwtMillis = Config.getIntProperty("RECOVER_PASSWORD_TOKEN_TTL_MINS", 20) * DateUtil.MINUTE_MILLIS;

    public DefaultResetPasswordUrlStrategy() {
        this(JsonWebTokenFactory.getInstance().getJsonWebTokenService());
    }

    @VisibleForTesting
    public DefaultResetPasswordUrlStrategy(final JsonWebTokenService jsonWebTokenService) {
        this.jsonWebTokenService = jsonWebTokenService;
    }
    
    @Override
    public String getURL(final Map<String, Object> params) {

        final User user       = (User)    params.get(USER);
        final String token    = (String)  params.get(TOKEN);
        final Locale locale   = (Locale)  params.get(LOCALE);
        final String jwt      = this.jsonWebTokenService.generateToken(
                new JWTBean(user.getUserId(), token, user.getUserId(), this.jwtMillis

        ));

        return new StringBuilder(C_PORTAL_PUBLIC_LOGIN_MY_ACCOUNT_CMD_ERESET_MY_USER_ID)
                .append(SWITCH_LOCALE).append(locale.getLanguage()).append(UNDERSCORE)
                .append(locale.getCountry()).append(TOKEN_PARAM).append(jwt).toString();
    } // getResetUserPasswordRelativeURL.

} // E:O:F:DefaultResetPasswordUrlStrategy.
