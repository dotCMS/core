package com.dotcms.auth.providers.oauth.provider;

import static com.dotcms.auth.providers.oauth.OAuthConstants.DISCOVERY_PATH;
import static com.dotcms.auth.providers.oauth.OAuthConstants.PROVIDER_TYPE_OIDC;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.vavr.control.Try;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * OIDC provider that auto-configures endpoints from {@code /.well-known/openid-configuration}.
 * <p>
 * Configure with the issuer URL (e.g. {@code https://accounts.google.com}); discovery
 * populates authorization, token, userinfo, revocation and end-session endpoints.
 */
public class OIDCProvider implements OAuthProvider {

    private static final ObjectMapper MAPPER = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    private final String issuerUrl;
    private final String clientId;
    private final char[] clientSecret;

    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String userinfoEndpoint;
    private final String revocationEndpoint;
    private final String endSessionEndpoint;

    private final String groupsClaim;
    private final String groupsUrl;

    public OIDCProvider(final String issuerUrl,
                        final String clientId,
                        final char[] clientSecret,
                        final String groupsClaim,
                        final String groupsUrl) {
        if (!UtilMethods.isSet(issuerUrl)) {
            throw new DotRuntimeException("OIDC issuerUrl is required");
        }
        this.issuerUrl    = issuerUrl.endsWith("/") ? issuerUrl.substring(0, issuerUrl.length() - 1) : issuerUrl;
        this.clientId     = clientId;
        this.clientSecret = clientSecret == null ? new char[0] : clientSecret;
        this.groupsClaim  = groupsClaim;
        this.groupsUrl    = groupsUrl;

        final Map<String, Object> discovery = discover(this.issuerUrl + DISCOVERY_PATH);
        this.authorizationEndpoint = (String) discovery.get("authorization_endpoint");
        this.tokenEndpoint         = (String) discovery.get("token_endpoint");
        this.userinfoEndpoint      = (String) discovery.get("userinfo_endpoint");
        this.revocationEndpoint    = (String) discovery.get("revocation_endpoint");
        this.endSessionEndpoint    = (String) discovery.get("end_session_endpoint");

        if (!UtilMethods.isSet(this.authorizationEndpoint) || !UtilMethods.isSet(this.tokenEndpoint)) {
            throw new DotRuntimeException("OIDC discovery at " + this.issuerUrl + DISCOVERY_PATH
                    + " did not return the required authorization_endpoint / token_endpoint");
        }
    }

    private static Map<String, Object> discover(final String discoveryUrl) {
        try {
            final CircuitBreakerUrl.Response<String> resp = CircuitBreakerUrl.builder()
                    .setUrl(discoveryUrl)
                    .setMethod(CircuitBreakerUrl.Method.GET)
                    .setTimeout(5000)
                    .build()
                    .doResponse();
            if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
                throw new DotRuntimeException("OIDC discovery failed: HTTP " + resp.getStatusCode() + " from " + discoveryUrl);
            }
            return MAPPER.readValue(resp.getResponse(), new TypeReference<Map<String, Object>>() {});
        } catch (final DotRuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotRuntimeException("OIDC discovery failed at " + discoveryUrl + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String buildAuthorizationUrl(final String state, final String callbackUrl, final String scope) {
        final String effectiveScope = UtilMethods.isSet(scope) ? scope : "openid email profile";
        return authorizationEndpoint
                + (authorizationEndpoint.contains("?") ? "&" : "?")
                + "response_type=code"
                + "&client_id="     + urlEncode(clientId)
                + "&redirect_uri="  + urlEncode(callbackUrl)
                + "&scope="         + urlEncode(effectiveScope)
                + "&state="         + urlEncode(state);
    }

    @Override
    public String exchangeCodeForToken(final String code, final String callbackUrl) {
        final String body = "grant_type=authorization_code"
                + "&code="         + urlEncode(code)
                + "&redirect_uri=" + urlEncode(callbackUrl)
                + "&client_id="    + urlEncode(clientId)
                + "&client_secret=" + urlEncode(new String(clientSecret));
        try {
            final CircuitBreakerUrl.Response<String> resp = CircuitBreakerUrl.builder()
                    .setUrl(tokenEndpoint)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setRawData(body)
                    .setHeaders(ImmutableMap.of("Accept", "application/json"))
                    .setTimeout(10000)
                    .build()
                    .doResponse();
            if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
                throw new DotRuntimeException("OIDC token exchange failed: HTTP " + resp.getStatusCode());
            }
            final Map<String, Object> json = MAPPER.readValue(resp.getResponse(), new TypeReference<Map<String, Object>>() {});
            final Object token = json.get("access_token");
            if (token == null) {
                throw new DotRuntimeException("OIDC token response missing access_token");
            }
            return token.toString();
        } catch (final DotRuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotRuntimeException("OIDC token exchange failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getUserInfo(final String accessToken) {
        if (!UtilMethods.isSet(userinfoEndpoint)) {
            throw new DotRuntimeException("OIDC discovery did not provide a userinfo_endpoint");
        }
        try {
            final CircuitBreakerUrl.Response<String> resp = CircuitBreakerUrl.builder()
                    .setUrl(userinfoEndpoint)
                    .setMethod(CircuitBreakerUrl.Method.GET)
                    .setHeaders(ImmutableMap.of(
                            "Authorization", "Bearer " + accessToken,
                            "Accept", "application/json"))
                    .setTimeout(5000)
                    .build()
                    .doResponse();
            if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
                throw new DotRuntimeException("OIDC userinfo failed: HTTP " + resp.getStatusCode());
            }
            final Map<String, Object> raw = MAPPER.readValue(resp.getResponse(), new TypeReference<Map<String, Object>>() {});
            // Return a case-insensitive map so downstream lookups don't depend on provider casing.
            final Map<String, Object> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            ci.putAll(raw);
            return ci;
        } catch (final DotRuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotRuntimeException("OIDC userinfo call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Collection<String> getGroups(final String accessToken, final Map<String, Object> userInfo) {
        // Prefer the claim from userinfo if configured
        if (UtilMethods.isSet(groupsClaim) && userInfo != null && userInfo.containsKey(groupsClaim)) {
            return toStringList(userInfo.get(groupsClaim));
        }
        // Fall back to a separate groups endpoint if configured
        if (UtilMethods.isSet(groupsUrl)) {
            return Try.of(() -> fetchGroupsFromUrl(accessToken)).getOrElse(Collections.emptyList());
        }
        return Collections.emptyList();
    }

    private Collection<String> fetchGroupsFromUrl(final String accessToken) throws Exception {
        final CircuitBreakerUrl.Response<String> resp = CircuitBreakerUrl.builder()
                .setUrl(groupsUrl)
                .setMethod(CircuitBreakerUrl.Method.GET)
                .setHeaders(ImmutableMap.of(
                        "Authorization", "Bearer " + accessToken,
                        "Accept", "application/json"))
                .setTimeout(5000)
                .build()
                .doResponse();
        if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
            Logger.warn(this, "OIDC groups endpoint returned HTTP " + resp.getStatusCode());
            return Collections.emptyList();
        }
        // Accept either a JSON array of strings, or a JSON object with a "groups" array.
        final Object parsed = MAPPER.readValue(resp.getResponse(), Object.class);
        if (parsed instanceof List) {
            return toStringList(parsed);
        }
        if (parsed instanceof Map) {
            return toStringList(((Map<?, ?>) parsed).get("groups"));
        }
        return Collections.emptyList();
    }

    @Override
    public void revokeToken(final String accessToken) {
        if (!UtilMethods.isSet(revocationEndpoint) || !UtilMethods.isSet(accessToken)) {
            return;
        }
        try {
            final String body = "token=" + urlEncode(accessToken)
                    + "&client_id="     + urlEncode(clientId)
                    + "&client_secret=" + urlEncode(new String(clientSecret));
            CircuitBreakerUrl.builder()
                    .setUrl(revocationEndpoint)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setRawData(body)
                    .setTimeout(5000)
                    .build()
                    .doResponse();
        } catch (final Exception e) {
            Logger.warn(this, "OIDC token revocation failed: " + e.getMessage());
        }
    }

    @Override
    public Optional<String> getLogoutUrl(final String idToken, final String postLogoutRedirectUri) {
        if (!UtilMethods.isSet(endSessionEndpoint)) {
            return Optional.empty();
        }
        final StringBuilder sb = new StringBuilder(endSessionEndpoint);
        sb.append(endSessionEndpoint.contains("?") ? "&" : "?");
        if (UtilMethods.isSet(idToken)) {
            sb.append("id_token_hint=").append(urlEncode(idToken)).append('&');
        }
        if (UtilMethods.isSet(postLogoutRedirectUri)) {
            sb.append("post_logout_redirect_uri=").append(urlEncode(postLogoutRedirectUri));
        }
        return Optional.of(sb.toString());
    }

    @Override
    public String getProviderType() {
        return PROVIDER_TYPE_OIDC;
    }

    @SuppressWarnings("unchecked")
    private static Collection<String> toStringList(final Object value) {
        if (value instanceof Collection) {
            final Collection<Object> c = (Collection<Object>) value;
            return c.stream().filter(java.util.Objects::nonNull).map(Object::toString).collect(java.util.stream.Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static String urlEncode(final String s) {
        if (s == null) {
            return "";
        }
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
