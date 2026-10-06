package com.dotcms.auth.providers.oauth.viewtool;

import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.auth.providers.oauth.OAuthConstants;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * Velocity helper exposing the configured OAuth app to templates.
 * Registered as {@code $oauth} in {@code toolbox.xml}.
 */
public class OAuthTool implements ViewTool {

    private HttpServletRequest request;

    @Override
    public void init(final Object initData) {
        if (initData instanceof ViewContext) {
            this.request = ((ViewContext) initData).getRequest();
        }
    }

    /** True when the current site (or SYSTEM_HOST) has an enabled dotOAuth app. */
    public boolean isConfigured() {
        return OAuthAppConfig.config(request).isPresent();
    }

    /** "OIDC" or "OAuth2" when configured, empty string otherwise. */
    public String getProviderType() {
        return OAuthAppConfig.config(request).map(c -> c.providerType).orElse("");
    }

    /** URL that triggers the OAuth flow for the admin console. */
    public String getLoginUrl() {
        return "/dotAdmin/";
    }

    /** URL that ends the session (provider logout is handled server-side). */
    public String getLogoutUrl() {
        return OAuthConstants.LOGOUT_PATHS[0];
    }
}
