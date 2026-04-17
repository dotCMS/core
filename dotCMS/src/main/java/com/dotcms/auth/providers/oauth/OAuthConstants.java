package com.dotcms.auth.providers.oauth;

/**
 * Shared keys and URL patterns for the core OAuth 2.0 / OpenID Connect integration.
 * <p>
 * Endpoints and session/app keys are deliberately different from the legacy
 * {@code plugin-dotcms-oauth} OSGI plugin so both can coexist while customers
 * migrate their configuration.
 */
public final class OAuthConstants {

    private OAuthConstants() {}

    // Apps API key — core uses "dotOAuth", plugin used "dotOAuthApp"
    public static final String APP_KEY = "dotOAuth";

    // REST paths
    public static final String OAUTH_BASE_PATH = "/api/v1/oauth";
    public static final String CALLBACK_PATH   = "/api/v1/oauth/callback";
    public static final String TOKEN_PATH      = "/api/v1/oauth/token";

    // Logout paths that trigger OAuth-aware logout flow
    public static final String[] LOGOUT_PATHS = {"/api/v1/logout", "/dotCMS/logout", "/dotAdmin/logout"};

    // URL prefixes that require backend authentication
    public static final String[] BACK_END_URLS  = {"/dotAdmin/", "/c/", "/html/portal/login"};
    public static final String[] FRONT_END_URLS = {"/dotCMS/login", "/application/login/login", "/login"};

    // Session attribute keys
    public static final String SESSION_REDIRECT_URI     = "OAUTH_REDIRECT";
    public static final String SESSION_STATE            = "OAUTH_STATE";
    public static final String SESSION_ACCESS_TOKEN     = "OAUTH_ACCESS_TOKEN";
    public static final String SESSION_PROVIDER_TYPE    = "OAUTH_PROVIDER_TYPE";
    public static final String SESSION_FRONT_END_LOGIN  = "OAUTH_FRONT_END_LOGIN";
    public static final String SESSION_NATIVE_LOGIN     = "OAUTH_NATIVE_LOGIN_BYPASS";
    public static final String SESSION_ORIGINAL_REQUEST = "OAUTH_ORIGINAL_REQUEST";

    // Request params / cookies
    public static final String PARAM_NATIVE   = "native";
    public static final String PARAM_REFERRER = "referrer";
    public static final String PARAM_CODE     = "code";
    public static final String PARAM_STATE    = "state";
    public static final String COOKIE_ACCESS_TOKEN = "access_token";

    // OIDC discovery
    public static final String DISCOVERY_PATH = "/.well-known/openid-configuration";

    // Provider types
    public static final String PROVIDER_TYPE_OIDC  = "OIDC";
    public static final String PROVIDER_TYPE_OAUTH2 = "OAuth2";

    // URL patterns to skip OAuth interception (assets, API calls, etc.)
    public static final String[] DEFAULT_ALLOWED_URL_FRAGMENTS = {
            ".bundle.", ".chunk.", ".js", ".css", ".woff", ".ttf", ".ico", ".png", ".jpg", ".svg",
            "/api/", "/authentication", "/loginform", "/logout", "/appconfiguration"
    };
}
