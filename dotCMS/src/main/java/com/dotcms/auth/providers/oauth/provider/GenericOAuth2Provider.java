package com.dotcms.auth.providers.oauth.provider;

import static com.dotcms.auth.providers.oauth.OAuthConstants.PROVIDER_TYPE_OAUTH2;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Generic OAuth 2.0 provider for non-OIDC identity providers (e.g. Facebook).
 * All endpoint URLs must be supplied explicitly; nothing is discovered.
 */
public class GenericOAuth2Provider implements OAuthProvider {

    private static final ObjectMapper MAPPER = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    private final String clientId;
    private final char[] clientSecret;
    private final String authorizationUrl;
    private final String tokenUrl;
    private final String userinfoUrl;
    private final String revocationUrl;
    private final String logoutUrl;
    private final String groupsClaim;
    private final String groupsUrl;
    private final String groupsResponsePath;

    public GenericOAuth2Provider(final String clientId,
                                 final char[] clientSecret,
                                 final String authorizationUrl,
                                 final String tokenUrl,
                                 final String userinfoUrl,
                                 final String revocationUrl,
                                 final String logoutUrl,
                                 final String groupsClaim,
                                 final String groupsUrl,
                                 final String groupsResponsePath) {
        if (!UtilMethods.isSet(authorizationUrl) || !UtilMethods.isSet(tokenUrl) || !UtilMethods.isSet(userinfoUrl)) {
            throw new DotRuntimeException("GenericOAuth2Provider requires authorizationUrl, tokenUrl, and userinfoUrl");
        }
        this.clientId         = clientId;
        this.clientSecret     = clientSecret == null ? new char[0] : clientSecret;
        this.authorizationUrl = authorizationUrl;
        this.tokenUrl         = tokenUrl;
        this.userinfoUrl      = userinfoUrl;
        this.revocationUrl    = revocationUrl;
        this.logoutUrl        = logoutUrl;
        this.groupsClaim      = groupsClaim;
        this.groupsUrl        = groupsUrl;
        this.groupsResponsePath = groupsResponsePath;
    }

    @Override
    public String buildAuthorizationUrl(final String state,
                                        final String nonce,
                                        final String codeChallenge,
                                        final String callbackUrl,
                                        final String scope) {
        // Plain OAuth2 doesn't support nonce — only OIDC does. We accept the parameter to keep
        // the interface uniform but silently ignore it here.
        final String effectiveScope = UtilMethods.isSet(scope) ? scope : "";
        // Pin response_mode=query so a hostile or misconfigured IdP cannot flip to
        // form_post (which would POST the auth code through a browser-rendered form
        // and sidestep our query-string callback parser).
        final StringBuilder sb = new StringBuilder(authorizationUrl)
                .append(authorizationUrl.contains("?") ? "&" : "?")
                .append("response_type=code")
                .append("&response_mode=query")
                .append("&client_id=").append(urlEncode(clientId))
                .append("&redirect_uri=").append(urlEncode(callbackUrl));
        if (UtilMethods.isSet(effectiveScope)) {
            sb.append("&scope=").append(urlEncode(effectiveScope));
        }
        sb.append("&state=").append(urlEncode(state));
        if (UtilMethods.isSet(codeChallenge)) {
            sb.append("&code_challenge=").append(urlEncode(codeChallenge))
              .append("&code_challenge_method=S256");
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> exchangeCodeForToken(final String code,
                                                    final String codeVerifier,
                                                    final String callbackUrl) {
        final StringBuilder body = new StringBuilder()
                .append("grant_type=authorization_code")
                .append("&code=").append(urlEncode(code))
                .append("&redirect_uri=").append(urlEncode(callbackUrl));
        if (UtilMethods.isSet(codeVerifier)) {
            body.append("&code_verifier=").append(urlEncode(codeVerifier));
        }
        try {
            final CircuitBreakerUrl.Response<String> resp = CircuitBreakerUrl.builder()
                    .setUrl(tokenUrl)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setRawData(body.toString())
                    .setHeaders(ImmutableMap.of(
                            "Authorization", OAuthCrypto.basicAuthHeader(clientId, clientSecret),
                            "Accept", "application/json",
                            "Content-Type", "application/x-www-form-urlencoded"))
                    .setTimeout(10000)
                    .setMaxResponseBytes(OAuthGroupsFetcher.MAX_IDP_RESPONSE_BYTES)
                    .build()
                    .doResponse();
            if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
                throw new DotRuntimeException("OAuth2 token exchange failed: HTTP " + resp.getStatusCode());
            }
            final Map<String, Object> json = MAPPER.readValue(resp.getResponse(), new TypeReference<Map<String, Object>>() {});
            if (json.get("access_token") == null) {
                throw new DotRuntimeException("OAuth2 token response missing access_token");
            }
            return json;
        } catch (final DotRuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotRuntimeException("OAuth2 token exchange failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getUserInfo(final String accessToken) {
        try {
            final CircuitBreakerUrl.Response<String> resp = CircuitBreakerUrl.builder()
                    .setUrl(userinfoUrl)
                    .setMethod(CircuitBreakerUrl.Method.GET)
                    .setHeaders(ImmutableMap.of(
                            "Authorization", "Bearer " + accessToken,
                            "Accept", "application/json"))
                    .setTimeout(5000)
                    .setMaxResponseBytes(OAuthGroupsFetcher.MAX_IDP_RESPONSE_BYTES)
                    .build()
                    .doResponse();
            if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
                throw new DotRuntimeException("OAuth2 userinfo failed: HTTP " + resp.getStatusCode());
            }
            final Map<String, Object> raw = MAPPER.readValue(resp.getResponse(), new TypeReference<Map<String, Object>>() {});
            final Map<String, Object> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            ci.putAll(raw);
            return ci;
        } catch (final DotRuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotRuntimeException("OAuth2 userinfo call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Collection<String> getGroups(final String accessToken, final Map<String, Object> userInfo) {
        if (UtilMethods.isSet(groupsClaim) && userInfo != null && userInfo.containsKey(groupsClaim)) {
            return OAuthGroupsFetcher.toStringList(userInfo.get(groupsClaim));
        }
        // Failures propagate — the caller must be able to tell "endpoint down" from "user has
        // no groups", otherwise an IdP outage silently strips roles during the role rebuild.
        if (UtilMethods.isSet(groupsUrl)) {
            return OAuthGroupsFetcher.fetch(groupsUrl, groupsResponsePath, accessToken, userInfo, "OAuth2");
        }
        return Collections.emptyList();
    }

    @Override
    public void revokeToken(final String accessToken) {
        if (!UtilMethods.isSet(revocationUrl) || !UtilMethods.isSet(accessToken)) {
            return;
        }
        try {
            final String body = "token=" + urlEncode(accessToken);
            CircuitBreakerUrl.builder()
                    .setUrl(revocationUrl)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setRawData(body)
                    .setHeaders(ImmutableMap.of(
                            "Authorization", OAuthCrypto.basicAuthHeader(clientId, clientSecret),
                            "Content-Type", "application/x-www-form-urlencoded"))
                    .setTimeout(5000)
                    .setMaxResponseBytes(OAuthGroupsFetcher.MAX_IDP_RESPONSE_BYTES)
                    .build()
                    .doResponse();
        } catch (final Exception e) {
            Logger.warn(this, "OAuth2 token revocation failed: " + e.getMessage());
        }
    }

    @Override
    public Optional<String> getLogoutUrl(final String idToken, final String postLogoutRedirectUri) {
        return UtilMethods.isSet(logoutUrl) ? Optional.of(logoutUrl) : Optional.empty();
    }

    @Override
    public String getProviderType() {
        return PROVIDER_TYPE_OAUTH2;
    }

    private static String urlEncode(final String s) {
        if (s == null) {
            return "";
        }
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
