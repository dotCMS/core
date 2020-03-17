package com.dotcms.rest.api.v1.authentication.url;

import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.liferay.portal.model.User;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * This {@link UrlStrategy} is the default one for the web site legacy site.
 *
 * @author jsanca
 */
public class DefaultResetPasswordUrlStrategy implements UrlStrategy {

    private static final String C_PORTAL_PUBLIC_LOGIN_MY_ACCOUNT_CMD_ERESET_MY_USER_ID = "/c/portal_public/login?my_account_cmd=ereset";
    private static final String TOKEN_PARAM = "&token=";
    private static final String SWITCH_LOCALE = "&switchLocale=";
    private static final String UNDERSCORE = "_";
    private final JsonWebTokenService jsonWebTokenService;
    private final long jwtMillis =
            Config.getIntProperty("RECOVER_PASSWORD_TOKEN_TTL_MINS", 20) * DateUtil.MINUTE_MILLIS;

    public DefaultResetPasswordUrlStrategy() {
        this(JsonWebTokenFactory.getInstance().getJsonWebTokenService());
    }

    @VisibleForTesting
    public DefaultResetPasswordUrlStrategy(final JsonWebTokenService jsonWebTokenService) {
        this.jsonWebTokenService = jsonWebTokenService;
    }

    @Override
    public String getURL(final Map<String, Object> params) {

        final User user = (User) params.get(USER);
        final String token = (String) params.get(TOKEN);
        final Locale locale = (Locale) params.get(LOCALE);

        // private UserToken(String id, String subject, Date modificationDate, long ttlMillis, final String skinId) {
        final String jwt = this.jsonWebTokenService.generateUserToken(
                new UserToken.Builder().id(user.getRememberMeToken())
                        .subject(token).modificationDate(user.getModificationDate())
                        .expiresDate(this.jwtMillis).build());

        return new StringBuilder(C_PORTAL_PUBLIC_LOGIN_MY_ACCOUNT_CMD_ERESET_MY_USER_ID)
                .append(SWITCH_LOCALE).append(locale.getLanguage()).append(UNDERSCORE)
                .append(locale.getCountry()).append(TOKEN_PARAM).append(jwt).toString();
    } // getResetUserPasswordRelativeURL.

} // E:O:F:DefaultResetPasswordUrlStrategy.
