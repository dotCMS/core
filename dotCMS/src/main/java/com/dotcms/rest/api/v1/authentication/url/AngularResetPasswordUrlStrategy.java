package com.dotcms.rest.api.v1.authentication.url;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.util.PortletURLUtil;
import java.util.Map;

/**
 * This {@link UrlStrategy} is for a Angular Reset Password.
 *
 * @author jsanca
 */
public class AngularResetPasswordUrlStrategy implements UrlStrategy {

    private static final String HTML_NG_RESET_PASSWORD_TRUE_USER_ID_0_TOKEN_1 =
            "/" + PortletURLUtil.URL_ADMIN_PREFIX + "/#/public/resetPassword/{0}";

    @VisibleForTesting
    public AngularResetPasswordUrlStrategy() {
    }

    @Override
    public String getURL(final Map<String, Object> params) {

        final String token = (String) params.get(TOKEN);
        return java.text.MessageFormat.format(HTML_NG_RESET_PASSWORD_TRUE_USER_ID_0_TOKEN_1,
                (token));
    } // getResetUserPasswordRelativeURL.

} // E:O:F:AngularResetPasswordUrlStrategy.
