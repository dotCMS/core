package com.dotcms.auth.providers.oauth.provider;

import static com.dotcms.auth.providers.oauth.OAuthConstants.DISCOVERY_PATH;
import static com.dotcms.auth.providers.oauth.OAuthConstants.PROVIDER_TYPE_OIDC;

import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.vavr.control.Try;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OIDC provider that auto-configures endpoints from {@code /.well-known/openid-configuration}.
 * <p>
 * Configure with the issuer URL (e.g. {@code https://accounts.google.com}); discovery
 * populates authorization, token, userinfo, revocation and end-session endpoints.
 */
public class OIDCProvider implements OAuthProvider {

    private static final ObjectMapper MAPPER = DotObjectMapperProvider.getInstance().getDefaultObjectMapper();

    /**
     * Cache of parsed {@code .well-known/openid-configuration} documents keyed by issuer URL.
     * TTL is controlled by {@code OAUTH_DISCOVERY_CACHE_TTL_SECONDS} (default 15 min).
     * Avoids a network round-trip on every OIDCProvider construction (previously hit during
     * every login redirect, every callback, and every ViewTool access).
     */
    private static final ConcurrentHashMap<String, CachedDiscovery> DISCOVERY_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, RemoteJWKSet<SecurityContext>> JWKS_CACHE = new ConcurrentHashMap<>();

    /**
     * Safe {@code id_token} signing algorithms. Asymmetric only (RSA or EC) — symmetric
     * {@code HS*} algs rely on a shared secret, and {@code none} is an explicit attacker
     * vector. The effective allow-list for a given IdP is this set intersected with the
     * algorithms advertised in discovery's {@code id_token_signing_alg_values_supported}.
     */
    private static final Set<JWSAlgorithm> SAFE_ID_TOKEN_ALGS = Collections.unmodifiableSet(
            new LinkedHashSet<>(java.util.Arrays.asList(
                    JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512,
                    JWSAlgorithm.ES256, JWSAlgorithm.ES384, JWSAlgorithm.ES512,
                    JWSAlgorithm.PS256, JWSAlgorithm.PS384, JWSAlgorithm.PS512)));

    private static final class CachedDiscovery {
        final Map<String, Object> document;
        final long expiresAtMs;
        CachedDiscovery(final Map<String, Object> document, final long expiresAtMs) {
            this.document = document;
            this.expiresAtMs = expiresAtMs;
        }
        boolean isFresh() {
            return System.currentTimeMillis() < expiresAtMs;
        }
    }

    private final String issuerUrl;
    private final String clientId;
    private final char[] clientSecret;

    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String userinfoEndpoint;
    private final String revocationEndpoint;
    private final String endSessionEndpoint;
    private final String jwksUri;

    private final String groupsClaim;
    private final String groupsUrl;

    private final Set<JWSAlgorithm> idTokenAllowedAlgs;

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

        final Map<String, Object> discovery = discover(this.issuerUrl);
        verifyDiscoveryIssuer(discovery, this.issuerUrl);
        this.authorizationEndpoint = validateDiscoveredUrl(
                (String) discovery.get("authorization_endpoint"), "authorization_endpoint", true);
        this.tokenEndpoint = validateDiscoveredUrl(
                (String) discovery.get("token_endpoint"), "token_endpoint", true);
        this.userinfoEndpoint = validateDiscoveredUrl(
                (String) discovery.get("userinfo_endpoint"), "userinfo_endpoint", false);
        this.revocationEndpoint = validateDiscoveredUrl(
                (String) discovery.get("revocation_endpoint"), "revocation_endpoint", false);
        this.endSessionEndpoint = validateDiscoveredUrl(
                (String) discovery.get("end_session_endpoint"), "end_session_endpoint", false);
        this.jwksUri = validateDiscoveredUrl(
                (String) discovery.get("jwks_uri"), "jwks_uri", true);

        if (!UtilMethods.isSet(this.authorizationEndpoint) || !UtilMethods.isSet(this.tokenEndpoint)) {
            throw new DotRuntimeException("OIDC discovery at " + this.issuerUrl + DISCOVERY_PATH
                    + " did not return the required authorization_endpoint / token_endpoint");
        }
        this.idTokenAllowedAlgs = resolveAllowedIdTokenAlgs(discovery);
    }

    /**
     * Pin the {@code id_token} verification to the algorithms the IdP published in
     * discovery, intersected with {@link #SAFE_ID_TOKEN_ALGS}. Without this pin, a
     * hostile IdP could sign tokens with whatever alg it chose and the RP would
     * happily validate them — defeating the point of verified discovery. If the
     * intersection is empty (IdP advertises only unsafe algs, or omits the list),
     * fall back to {@link JWSAlgorithm#RS256}, which OIDC Core §2 requires every
     * provider to support.
     */
    private static Set<JWSAlgorithm> resolveAllowedIdTokenAlgs(final Map<String, Object> discovery) {
        final Object published = discovery.get("id_token_signing_alg_values_supported");
        if (!(published instanceof Collection)) {
            return Collections.singleton(JWSAlgorithm.RS256);
        }
        final Set<JWSAlgorithm> allowed = new HashSet<>();
        for (final Object entry : (Collection<?>) published) {
            if (entry == null) {
                continue;
            }
            final JWSAlgorithm parsed = JWSAlgorithm.parse(entry.toString());
            if (SAFE_ID_TOKEN_ALGS.contains(parsed)) {
                allowed.add(parsed);
            }
        }
        if (allowed.isEmpty()) {
            return Collections.singleton(JWSAlgorithm.RS256);
        }
        return Collections.unmodifiableSet(allowed);
    }

    /**
     * Resolve the OIDC discovery document for an issuer, caching the parsed JSON per issuer
     * for the configured TTL. Stale or missing entries trigger a fresh fetch.
     */
    static Map<String, Object> discover(final String issuerUrl) {
        final long ttlMs = Config.getIntProperty("OAUTH_DISCOVERY_CACHE_TTL_SECONDS", 900) * 1000L;
        return DISCOVERY_CACHE.compute(issuerUrl, (key, cached) -> {
            if (cached != null && cached.isFresh()) {
                return cached;
            }
            final Map<String, Object> fresh = fetchDiscovery(key + DISCOVERY_PATH);
            return new CachedDiscovery(fresh, System.currentTimeMillis() + ttlMs);
        }).document;
    }

    private static void verifyDiscoveryIssuer(final Map<String, Object> discovery,
                                              final String expectedIssuer) {
        final Object issuer = discovery.get("issuer");
        if (!UtilMethods.isSet(issuer == null ? null : issuer.toString())) {
            return;
        }
        final String normalizedIssuer = stripTrailingSlash(issuer.toString());
        if (!expectedIssuer.equals(normalizedIssuer)) {
            throw new DotRuntimeException("OIDC discovery issuer mismatch: expected '"
                    + expectedIssuer + "' got '" + issuer + "'");
        }
    }

    static String validateDiscoveredUrl(final String url,
                                        final String fieldName,
                                        final boolean required) {
        if (!UtilMethods.isSet(url)) {
            if (required) {
                throw new DotRuntimeException("OIDC discovery missing required " + fieldName);
            }
            return null;
        }
        final URI uri;
        try {
            uri = new URI(url);
        } catch (final URISyntaxException e) {
            throw new DotRuntimeException("OIDC discovery " + fieldName
                    + " is not a valid URI: " + e.getMessage(), e);
        }
        final String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!"https".equals(scheme) && !"http".equals(scheme)) {
            throw new DotRuntimeException("OIDC discovery " + fieldName
                    + " uses unsupported scheme '" + scheme + "'");
        }
        final boolean allowInsecure = Config.getBooleanProperty("OAUTH_ALLOW_INSECURE_URLS", false);
        if ("http".equals(scheme) && !allowInsecure) {
            throw new DotRuntimeException("OIDC discovery " + fieldName
                    + " must use https unless OAUTH_ALLOW_INSECURE_URLS=true");
        }
        final String host = uri.getHost();
        if (!UtilMethods.isSet(host)) {
            throw new DotRuntimeException("OIDC discovery " + fieldName + " is missing a host");
        }
        if (!allowInsecure && isInternalHost(host)) {
            throw new DotRuntimeException("OIDC discovery " + fieldName
                    + " resolves to an internal/private address");
        }
        return url;
    }

    private static boolean isInternalHost(final String host) {
        try {
            final InetAddress[] addresses = InetAddress.getAllByName(host);
            for (final InetAddress addr : addresses) {
                if (addr.isAnyLocalAddress()
                        || addr.isLoopbackAddress()
                        || addr.isLinkLocalAddress()
                        || addr.isSiteLocalAddress()
                        || addr.isMulticastAddress()) {
                    return true;
                }
            }
            return false;
        } catch (final UnknownHostException e) {
            Logger.debug(OIDCProvider.class,
                    "SSRF guard skipped for unresolvable discovered host '" + host
                            + "': " + e.getMessage());
            return false;
        }
    }

    private static Map<String, Object> fetchDiscovery(final String discoveryUrl) {
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
    public String buildAuthorizationUrl(final String state,
                                        final String nonce,
                                        final String codeChallenge,
                                        final String callbackUrl,
                                        final String scope) {
        final String effectiveScope = UtilMethods.isSet(scope) ? scope : "openid email profile";
        // Pin response_mode=query: keeps the auth code in the URL query-string where our
        // callback parser expects it, and prevents a hostile IdP from switching to
        // form_post — which would POST the code through the browser and bypass our
        // parser entirely.
        final StringBuilder sb = new StringBuilder(authorizationEndpoint)
                .append(authorizationEndpoint.contains("?") ? "&" : "?")
                .append("response_type=code")
                .append("&response_mode=query")
                .append("&client_id=").append(urlEncode(clientId))
                .append("&redirect_uri=").append(urlEncode(callbackUrl))
                .append("&scope=").append(urlEncode(effectiveScope))
                .append("&state=").append(urlEncode(state));
        if (UtilMethods.isSet(nonce)) {
            sb.append("&nonce=").append(urlEncode(nonce));
        }
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
                    .setUrl(tokenEndpoint)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setRawData(body.toString())
                    .setHeaders(ImmutableMap.of(
                            "Authorization", OAuthCrypto.basicAuthHeader(clientId, clientSecret),
                            "Accept", "application/json",
                            "Content-Type", "application/x-www-form-urlencoded"))
                    .setTimeout(10000)
                    .build()
                    .doResponse();
            if (resp.getStatusCode() < 200 || resp.getStatusCode() >= 300) {
                throw new DotRuntimeException("OIDC token exchange failed: HTTP " + resp.getStatusCode());
            }
            final Map<String, Object> json = MAPPER.readValue(resp.getResponse(), new TypeReference<Map<String, Object>>() {});
            if (json.get("access_token") == null) {
                throw new DotRuntimeException("OIDC token response missing access_token");
            }
            return json;
        } catch (final DotRuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotRuntimeException("OIDC token exchange failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate an OIDC {@code id_token} per the core OIDC spec and return the
     * verified claim set as a case-insensitive map keyed by claim name.
     * <ul>
     *   <li>Signature verified against the IdP's JWKS (fetched via {@code jwks_uri})</li>
     *   <li>{@code alg} is in the allow-list (prevents alg-confusion attacks)</li>
     *   <li>{@code iss} matches the configured issuer</li>
     *   <li>{@code aud} contains the configured {@code client_id}</li>
     *   <li>{@code exp} is in the future</li>
     *   <li>{@code nonce} matches the expected server-stored nonce</li>
     * </ul>
     * Only returns once every check has passed. Throws {@link DotRuntimeException}
     * on any validation failure.
     */
    @Override
    public Map<String, Object> validateIdTokenAndExtractClaims(final String idToken,
                                                               final String expectedNonce) {
        if (!UtilMethods.isSet(idToken)) {
            throw new DotRuntimeException("OIDC id_token is missing — cannot validate");
        }
        if (!UtilMethods.isSet(jwksUri)) {
            throw new DotRuntimeException("OIDC discovery did not provide a jwks_uri; cannot verify id_token");
        }
        try {
            final SignedJWT jwt = SignedJWT.parse(idToken);
            final JWKSource<SecurityContext> keySource = JWKS_CACHE.computeIfAbsent(jwksUri, uri -> {
                try {
                    return new RemoteJWKSet<>(new URL(uri));
                } catch (final MalformedURLException e) {
                    throw new DotRuntimeException("OIDC jwks_uri is malformed: " + uri, e);
                }
            });
            final JWSAlgorithm tokenAlg = jwt.getHeader().getAlgorithm() == null
                    ? null
                    : JWSAlgorithm.parse(jwt.getHeader().getAlgorithm().getName());
            if (tokenAlg == null || !idTokenAllowedAlgs.contains(tokenAlg)) {
                throw new DotRuntimeException("OIDC id_token alg '" + tokenAlg
                        + "' is not in the allow-list " + idTokenAllowedAlgs
                        + " — reject to prevent alg-confusion attacks");
            }
            final ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(new JWSVerificationKeySelector<>(idTokenAllowedAlgs, keySource));

            final JWTClaimsSet claims = processor.process(jwt, null);

            verifyIdTokenClaims(claims, issuerUrl, clientId, expectedNonce);

            // Expose verified claims to callers (case-insensitive to match getUserInfo shape).
            final Map<String, Object> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            ci.putAll(claims.getClaims());
            return ci;
        } catch (final DotRuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new DotRuntimeException("OIDC id_token validation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String validateIdTokenAndExtractSubject(final String idToken,
                                                   final String expectedNonce) {
        final Map<String, Object> claims = validateIdTokenAndExtractClaims(idToken, expectedNonce);
        return String.valueOf(claims.get("sub"));
    }

    /**
     * Post-signature claim checks for a parsed id_token: iss / aud / exp / nonce / sub.
     * Package-private so the logic can be unit-tested without the JWKS round-trip and
     * Nimbus processor setup that the public entry point performs first.
     * <p>
     * iss is compared after trailing-slash normalization on both sides — the configured
     * {@code expectedIssuer} has already been normalized in the constructor; the token's
     * iss gets the same treatment here so an IdP that emits iss with a slash (some Auth0
     * / Azure B2C tenant variants) doesn't fail validation against an admin config that
     * was entered without one, or vice-versa. OIDC Core §2 calls for strict URL equality
     * but IdP behavior in the wild is inconsistent.
     */
    static void verifyIdTokenClaims(final JWTClaimsSet claims,
                                    final String expectedIssuer,
                                    final String expectedClientId,
                                    final String expectedNonce) {
        // iss
        final String tokenIss = claims.getIssuer();
        final String normalizedTokenIss = stripTrailingSlash(tokenIss);
        if (!expectedIssuer.equals(normalizedTokenIss)) {
            throw new DotRuntimeException(
                    "OIDC id_token iss mismatch: expected '" + expectedIssuer + "' got '" + tokenIss + "'");
        }
        // aud — must contain our clientId (aud may be single string or array per spec).
        // THIS is the check that makes the token "ours" vs. "just valid somewhere on this IdP".
        final List<String> audiences = claims.getAudience();
        if (audiences == null || !audiences.contains(expectedClientId)) {
            throw new DotRuntimeException(
                    "OIDC id_token aud does not contain this client_id; got " + audiences);
        }
        // exp — nimbus validates claim set structure but expiry is not enforced by default processor.
        if (claims.getExpirationTime() == null || claims.getExpirationTime().getTime() <= System.currentTimeMillis()) {
            throw new DotRuntimeException("OIDC id_token is expired or has no exp claim");
        }
        // nonce
        final Object tokenNonce = claims.getClaim("nonce");
        if (!UtilMethods.isSet(expectedNonce)
                || tokenNonce == null
                || !expectedNonce.equals(tokenNonce.toString())) {
            throw new DotRuntimeException("OIDC id_token nonce mismatch — possible replay");
        }
        // sub is required by the spec — downstream provisioning relies on it.
        if (!UtilMethods.isSet(claims.getSubject())) {
            throw new DotRuntimeException("OIDC id_token missing sub claim");
        }
    }

    private static String stripTrailingSlash(final String value) {
        return value != null && value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
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
            final String body = "token=" + urlEncode(accessToken);
            CircuitBreakerUrl.builder()
                    .setUrl(revocationEndpoint)
                    .setMethod(CircuitBreakerUrl.Method.POST)
                    .setRawData(body)
                    .setHeaders(ImmutableMap.of(
                            "Authorization", OAuthCrypto.basicAuthHeader(clientId, clientSecret),
                            "Content-Type", "application/x-www-form-urlencoded"))
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
