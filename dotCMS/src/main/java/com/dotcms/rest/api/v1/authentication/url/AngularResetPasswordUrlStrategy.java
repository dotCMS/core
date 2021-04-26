package com.dotcms.rest.api.v1.authentication.url;

import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.PortletURLUtil;
import com.liferay.portal.model.User;
import java.util.Map;
import java.util.UUID;

/**
 * This {@link UrlStrategy} is for a Angular Reset Password.
 *
 * @author jsanca
 */
public class AngularResetPasswordUrlStrategy implements UrlStrategy {

    private static final String TOKEN_SEPARATOR = "+++";
    private final JsonWebTokenService jsonWebTokenService;
    private final long jwtMillis =
            Config.getIntProperty("RECOVER_PASSWORD_TOKEN_TTL_MINS", 20) * DateUtil.MINUTE_MILLIS;

    private static final String HTML_NG_RESET_PASSWORD_TRUE_USER_ID_0_TOKEN_1 =
            "/" + PortletURLUtil.URL_ADMIN_PREFIX + "/#/public/resetPassword/{0}";

    public AngularResetPasswordUrlStrategy() {
        this(JsonWebTokenFactory.getInstance().getJsonWebTokenService());
    }

    @VisibleForTesting
    public AngularResetPasswordUrlStrategy(final JsonWebTokenService jsonWebTokenService) {
        this.jsonWebTokenService = jsonWebTokenService;
    }

    @Override
    public String getURL(final Map<String, Object> params) {

        final User user    = (User) params.get(USER);
        final String token = (String) params.get(TOKEN);

        // private UserToken(String id, String subject, Date modificationDate, long ttlMillis, final String skinId) {
        final String jwt = this.jsonWebTokenService.generateUserToken(
                new UserToken.Builder().id(user.getRememberMeToken())
                        .subject(user.getUserId())
                        .modificationDate(user.getModificationDate())
                        .expiresDate(this.jwtMillis).build()
                );

        return java.text.MessageFormat.format(HTML_NG_RESET_PASSWORD_TRUE_USER_ID_0_TOKEN_1,
                (jwt + TOKEN_SEPARATOR + token));
    } // getResetUserPasswordRelativeURL.

} // E:O:F:AngularResetPasswordUrlStrategy.
