package com.dotcms.rest.api.v1.authentication.url;


import com.dotcms.auth.providers.jwt.beans.JWTBean;
import com.dotcms.auth.providers.jwt.factories.JsonWebTokenFactory;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.liferay.portal.model.User;

import java.util.Map;


/**
 * This {@link UrlStrategy} is for a Angular Reset Password.
 * @author jsanca
 */
public class AngularResetPasswordUrlStrategy implements UrlStrategy {

    private final JsonWebTokenService jsonWebTokenService;
    private final long jwtMillis = Config.getIntProperty("RECOVER_PASSWORD_TOKEN_TTL_MINS", 20) * DateUtil.MINUTE_MILLIS;

    private static final String HTML_NG_RESET_PASSWORD_TRUE_USER_ID_0_TOKEN_1 = "/html/ng?resetPassword=true&token={0}";

    public AngularResetPasswordUrlStrategy() {
        this(JsonWebTokenFactory.getInstance().getJsonWebTokenService());
    }

    @VisibleForTesting
    public AngularResetPasswordUrlStrategy(final JsonWebTokenService jsonWebTokenService) {
        this.jsonWebTokenService = jsonWebTokenService;
    }

    @Override
    public String getURL(final Map<String, Object> params) {

        final User user       = (User)    params.get(USER);
        final String token    = (String)  params.get(TOKEN);
        final String jwt      = this.jsonWebTokenService.generateToken(
                new JWTBean(user.getUserId(), token, user.getUserId(), this.jwtMillis

        ));

        return java.text.MessageFormat.format(HTML_NG_RESET_PASSWORD_TRUE_USER_ID_0_TOKEN_1, jwt);
    } // getResetUserPasswordRelativeURL.

} // E:O:F:AngularResetPasswordUrlStrategy.
