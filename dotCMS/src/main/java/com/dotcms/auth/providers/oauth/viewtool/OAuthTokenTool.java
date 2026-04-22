package com.dotcms.auth.providers.oauth.viewtool;

import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.auth.providers.oauth.provider.OAuthCrypto;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * Velocity helper that fetches a service-to-service access token using the
 * {@code client_credentials} grant against the configured OAuth app's token
 * endpoint. Tokens are cached in-memory until their (approximate) expiry.
 * <p>
 * Replaces the plugin's {@code ADFSTool} with a provider-agnostic version.
 * Registered as {@code $oauthToken} in {@code toolbox.xml}.
 */
public class OAuthTokenTool implements ViewTool {

    private static final ObjectMapper MAPPER = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    private static final Map<String, CachedToken> CACHE = new ConcurrentHashMap<>();
    private static final long DEFAULT_TIMEOUT_MS = 5000L;

    private HttpServletRequest request;

    @Override
    public void init(final Object initData) {
        if (initData instanceof ViewContext) {
            this.request = ((ViewContext) initData).getRequest();
        }
    }

    /**
     * Fetch a cached client_credentials access token using the dotOAuth app config.
     * Returns null when OAuth is not configured or the token exchange fails.
     */
    public String getAccessToken() {
        return getAccessToken(null);
    }

    /**
     * Fetch a cached client_credentials access token, optionally narrowing the scope.
     * Returns null on error; templates should check for null before use.
     * <p>
     * Access-gated to authenticated backend admin users. The raw client_credentials
     * token has the privileges of the dotCMS OAuth app itself (not of any end-user),
     * so leaking it to an anonymous frontend template — even by accident (typo in a
     * public VTL) — is equivalent to handing out the app secret. Only admins editing
     * backend templates should be able to retrieve it.
     */
    public String getAccessToken(final String scope) {
        if (!isAuthorized()) {
            return null;
        }
        final Optional<OAuthAppConfig> cfgOpt = OAuthAppConfig.config(request);
        if (cfgOpt.isEmpty()) {
            return null;
        }
        final OAuthAppConfig config = cfgOpt.get();
        final String tokenEndpoint = resolveTokenEndpoint(config);
        if (!UtilMethods.isSet(tokenEndpoint)) {
            Logger.warn(this, "OAuthTokenTool: no token endpoint available; OIDC discovery or manual tokenUrl required");
            return null;
        }

        final String effectiveScope = UtilMethods.isSet(scope) ? scope : config.scopes;
        final String cacheKey = config.clientId + "|" + tokenEndpoint + "|" + effectiveScope;
        final CachedToken cached = CACHE.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.accessToken;
        }

        try {
            // HTTP Basic (preferred by RFC 6749 §2.3.1) keeps the secret out of the request body
            // and avoids materializing it as a String beyond the narrow window inside OAuthCrypto.
            final String body = "grant_type=client_credentials"
                    + (UtilMethods.isSet(effectiveScope) ? "&scope=" + urlEncode(effectiveScope) : "");

            final CircuitBreakerUrl.Response<String> resp = CircuitBreakerUrl.builder()
                    .setUrl(tokenEndpoint)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setRawData(body)
                    .setHeaders(ImmutableMap.of(
                            "Authorization", OAuthCrypto.basicAuthHeader(config.clientId, config.clientSecret),
                            "Accept", "application/json",
                            "Content-Type", "application/x-www-form-urlencoded"))
                    .setTimeout(DEFAULT_TIMEOUT_MS)
                    .build()
                    .doResponse();

            if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
                Logger.warn(this, "OAuthTokenTool: client_credentials exchange returned HTTP " + resp.getStatusCode());
                return null;
            }

            final Map<String, Object> json = MAPPER.readValue(resp.getResponse(), new TypeReference<Map<String, Object>>() {});
            final Object token = json.get("access_token");
            if (token == null) {
                return null;
            }
            final long expiresInSeconds = json.get("expires_in") instanceof Number
                    ? ((Number) json.get("expires_in")).longValue()
                    : 1800L; // default to 30 min if provider omits expires_in
            CACHE.put(cacheKey, new CachedToken(token.toString(), System.currentTimeMillis() + (expiresInSeconds * 1000L)));
            return token.toString();
        } catch (final Exception e) {
            Logger.warn(this, "OAuthTokenTool: exchange failed: " + e.getMessage());
            return null;
        }
    }

    private boolean isAuthorized() {
        if (request == null) {
            return false;
        }
        final User user = PortalUtil.getUser(request);
        if (user == null || user.isAnonymousUser()) {
            Logger.debug(this, "OAuthTokenTool: anonymous caller blocked from client_credentials token");
            return false;
        }
        if (user.isFrontendUser() || !user.isBackendUser()) {
            Logger.warn(this, "OAuthTokenTool: non-backend user '" + user.getUserId()
                    + "' blocked from client_credentials token");
            return false;
        }
        if (!user.isAdmin()) {
            Logger.warn(this, "OAuthTokenTool: non-admin backend user '" + user.getUserId()
                    + "' blocked from client_credentials token");
            return false;
        }
        return true;
    }

    private String resolveTokenEndpoint(final OAuthAppConfig config) {
        if (UtilMethods.isSet(config.tokenUrl)) {
            return config.tokenUrl;
        }
        if (config.isOidc() && UtilMethods.isSet(config.issuerUrl)) {
            // Fall back to OIDC discovery. This is a one-shot call per-JVM via the OIDCProvider cache
            // upstream; here we re-fetch because the ViewTool does not instantiate an OIDCProvider.
            return discoverTokenEndpoint(config.issuerUrl);
        }
        return null;
    }

    private static String discoverTokenEndpoint(final String issuerUrl) {
        try {
            final String base = issuerUrl.endsWith("/") ? issuerUrl.substring(0, issuerUrl.length() - 1) : issuerUrl;
            final CircuitBreakerUrl.Response<String> resp = CircuitBreakerUrl.builder()
                    .setUrl(base + com.dotcms.auth.providers.oauth.OAuthConstants.DISCOVERY_PATH)
                    .setMethod(CircuitBreakerUrl.Method.GET)
                    .setTimeout(DEFAULT_TIMEOUT_MS)
                    .build()
                    .doResponse();
            if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
                return null;
            }
            final Map<String, Object> json = MAPPER.readValue(resp.getResponse(), new TypeReference<Map<String, Object>>() {});
            return (String) json.get("token_endpoint");
        } catch (final Exception e) {
            Logger.warn(OAuthTokenTool.class, "OIDC discovery for ViewTool token fetch failed: " + e.getMessage());
            return null;
        }
    }

    private static String urlEncode(final String s) {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static final class CachedToken {
        final String accessToken;
        final long   expiresAt;
        CachedToken(final String accessToken, final long expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt   = expiresAt;
        }
        boolean isExpired() {
            // Refresh 30 seconds before the provider says it's expired so the caller doesn't race.
            return System.currentTimeMillis() >= (expiresAt - 30_000L);
        }
    }
}
