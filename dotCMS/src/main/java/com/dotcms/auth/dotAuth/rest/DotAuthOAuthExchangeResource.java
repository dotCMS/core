package com.dotcms.auth.dotAuth.rest;

import com.dotcms.auth.dotAuth.session.DotAuthSessionCache;
import com.dotcms.auth.dotAuth.session.DotAuthSessionCacheImpl;
import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.auth.providers.oauth.OAuthHelper;
import com.dotcms.auth.providers.oauth.provider.OIDCProvider;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint that exchanges a caller-supplied OIDC {@code id_token} for an
 * opaque dotAuth <em>session-ref</em>. Designed for headless / SPA consumers
 * (e.g. a Next.js front-end) that have already completed an Authorization-Code
 * + PKCE flow with an OIDC provider such as Okta and now need a dotCMS-scoped
 * credential to call the content APIs with per-user permissions.
 *
 * <h3>Why a session-ref, not a JWT</h3>
 *
 * The earlier iteration of this endpoint minted a dotCMS USER_TOKEN JWT. That
 * path requires {@code jwt.jti == user.rememberMeToken}, and {@code
 * rememberMeToken} is a transient in-memory field — any user-cache flush
 * invalidates every outstanding JWT. For a stateless bearer flow the correct
 * analogue is the SAML browser session: ephemeral server-side state, no DB
 * persistence, re-auth on loss. That's what the session-ref is. See
 * {@link DotAuthSessionCache}.
 *
 * <h3>Contract</h3>
 *
 * <pre>{@code
 * POST /api/v1/dotauth/oauth/exchange
 * Content-Type: application/json
 *
 * {
 *   "idToken":        "eyJhbGciOiJSUzI1NiIsImtpZCI6Ii4uLiJ9...",
 *   "nonce":          "<same nonce the SPA sent in the /authorize request>",
 *   "expirationDays": 7          // optional; clamped to the configured max
 * }
 * }</pre>
 *
 * <p>On success the server replies with a session-ref + a minimal user summary:
 *
 * <pre>{@code
 * {
 *   "entity": {
 *     "sessionRef":     "dsr_<opaque, url-safe base64>",
 *     "expiresAt":      "2026-04-30T14:22:11Z",
 *     "expirationDays": 7,
 *     "user": {
 *       "userId":    "oauth:OIDC:00u1234...",
 *       "email":     "user@example.com",
 *       "firstName": "Scott",
 *       "lastName":  "Wicken",
 *       "fullName":  "Scott Wicken",
 *       "roles":     ["CMS Administrator", "Editor"]
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h3>What gets validated downstream</h3>
 *
 * Before a dotCMS user is resolved or a session-ref is issued, the {@code id_token}
 * passes the full OIDC validation chain in
 * {@link OIDCProvider#validateIdTokenAndExtractClaims}:
 * <ol>
 *   <li>JWS signature verified against the IdP's JWKS (discovered via
 *       {@code jwks_uri} in the OIDC discovery document).</li>
 *   <li>{@code alg} in the header must be in the allow-list (RS256/ES256 family) —
 *       prevents alg-confusion attacks.</li>
 *   <li>{@code iss} must equal the configured {@code issuerUrl}.</li>
 *   <li>{@code aud} must contain the configured {@code client_id}.
 *       This is the check that makes the token <em>ours</em>, not just "valid
 *       somewhere on this IdP" — it's the mitigation for the confused-deputy
 *       class the old {@code /v1/oauth/token} endpoint shipped with.</li>
 *   <li>{@code exp} must be in the future.</li>
 *   <li>{@code nonce} must equal the {@code nonce} supplied in the request body,
 *       which must in turn equal the nonce the SPA passed in its original
 *       {@code /authorize} request (bound to its session).</li>
 * </ol>
 *
 * <h3>Safety notes</h3>
 * <ul>
 *   <li>Disabled by default. Set the config property
 *       {@code DOTAUTH_OAUTH_EXCHANGE_ENABLED=true} to turn on per environment.</li>
 *   <li>Requires the current site's {@code dotAuth} App config to use
 *       {@code providerType = OIDC}. Plain OAuth2 providers do not issue an
 *       {@code id_token} that can be validated downstream and are rejected with
 *       400.</li>
 *   <li>Session-refs live only in the in-memory dotCMS cache (cluster-replicated
 *       via Hazelcast where configured). Loss of the cache entry — node restart,
 *       eviction, explicit logout — forces the SPA through Okta again. No row
 *       is ever written to the database for the session itself.</li>
 * </ul>
 */
@Path("/v1/dotauth/oauth")
@Tag(name = "dotAuth", description = "OAuth/OIDC token exchange for headless SPA consumers")
public class DotAuthOAuthExchangeResource implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Feature flag — endpoint is off unless explicitly enabled per-environment. */
    private static final String ENABLED_PROP = "DOTAUTH_OAUTH_EXCHANGE_ENABLED";

    /** Default session lifetime when the caller omits {@code expirationDays}. */
    private static final String DEFAULT_DAYS_PROP = "DOTAUTH_SESSION_DEFAULT_DAYS";
    private static final int    DEFAULT_DAYS_FALLBACK = 7;

    /** Hard ceiling on requested session lifetime; any larger request is clamped to this. */
    private static final String MAX_DAYS_PROP = "DOTAUTH_SESSION_MAX_DAYS";
    private static final int    MAX_DAYS_FALLBACK = 7;

    private final OAuthHelper oauthHelper          = new OAuthHelper();
    private final DotAuthSessionCache sessionCache = DotAuthSessionCacheImpl.getInstance();

    @OPTIONS
    @Path("/exchange")
    @NoCache
    public Response exchangePreflight(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response) {
        final Optional<OAuthAppConfig> cfgOpt = OAuthAppConfig.exchangeConfig(request);
        if (cfgOpt.isEmpty()) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        final List<String> origins = parseJsonStringList(cfgOpt.get().allowedOriginsJson);
        final String origin = request.getHeader("Origin");
        if (!origins.isEmpty() && UtilMethods.isSet(origin) && origins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "86400");
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @POST
    @Path("/exchange")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Exchange an OIDC id_token for a dotAuth session-ref",
            description = "Validates the caller-supplied id_token against the configured OIDC "
                    + "provider's JWKS (signature, iss, aud == our client_id, exp, nonce), "
                    + "resolves or JIT-provisions the matching dotCMS user, and returns an "
                    + "opaque session-ref bound to that user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token exchange succeeded",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityOAuthExchangeView.class))),
            @ApiResponse(responseCode = "400", description = "Malformed payload or non-OIDC provider configured"),
            @ApiResponse(responseCode = "401", description = "id_token failed validation (signature, iss, aud, exp, or nonce)"),
            @ApiResponse(responseCode = "403", description = "Resolved dotCMS user exists but is not active"),
            @ApiResponse(responseCode = "404", description = "Endpoint disabled (DOTAUTH_OAUTH_EXCHANGE_ENABLED=false)"),
            @ApiResponse(responseCode = "503", description = "OAuth is not configured for this site")
    })
    public Response exchange(@Context final HttpServletRequest request,
                             @Context final HttpServletResponse response,
                             final OAuthExchangeForm form) {
        try {
            // Feature flag: disabled by default. Hide the endpoint entirely when off
            // so discovery (a plain 404) doesn't reveal its existence.
            if (!Config.getBooleanProperty(ENABLED_PROP, false)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (form == null
                    || !UtilMethods.isSet(form.getIdToken())
                    || !UtilMethods.isSet(form.getNonce())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>("idToken and nonce are both required"))
                        .build();
            }

            // Site-scoped exchange config. Falls back to SYSTEM_HOST per the dotAuth contract,
            // then falls back from exchange-specific keys to the browser-login keys for
            // pre-split configurations.
            final Optional<OAuthAppConfig> cfgOpt = OAuthAppConfig.exchangeConfig(request);
            if (cfgOpt.isEmpty()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(new ResponseEntityView<>("OAuth is not configured for this site"))
                        .build();
            }
            final OAuthAppConfig config = cfgOpt.get();

            // CORS origin check: reject requests from unlisted browser origins
            final List<String> allowedOrigins = parseJsonStringList(config.allowedOriginsJson);
            if (!allowedOrigins.isEmpty()) {
                final String origin = request.getHeader("Origin");
                if (UtilMethods.isSet(origin) && !allowedOrigins.contains(origin)) {
                    SecurityLogger.logInfo(DotAuthOAuthExchangeResource.class,
                            "OAuth exchange rejected: origin '" + origin
                                    + "' not in allowed origins from " + request.getRemoteAddr());
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(new ResponseEntityView<>("Origin not allowed"))
                            .build();
                }
                // Set CORS headers for allowed origins
                if (UtilMethods.isSet(origin)) {
                    response.setHeader("Access-Control-Allow-Origin", origin);
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                }
            }

            if (!config.isOidc()) {
                // Plain OAuth2 has no id_token to validate downstream. Tell the client why
                // explicitly rather than pretend-validating something we can't actually check.
                SecurityLogger.logInfo(DotAuthOAuthExchangeResource.class,
                        "Refused OAuth exchange: site is configured for non-OIDC provider '"
                                + config.providerType + "' from " + request.getRemoteAddr());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>(
                                "OIDC is required for token exchange. Site is configured for "
                                        + "'" + config.providerType + "'. Only providers that issue "
                                        + "an id_token (signed JWT) can be validated by this endpoint."))
                        .build();
            }

            // Trusted IdP validation: if trusted IdPs are configured, decode the JWT's
            // iss claim (unverified) and match it against the allowlist. Use the matched
            // IdP's JWKS/audience/issuer for verification instead of the site-level config.
            final List<Map<String, Object>> trustedIdps = parseJsonMapList(config.trustedIdpsJson);
            String effectiveIssuer    = config.issuerUrl;
            String effectiveClientId  = config.clientId;
            char[] effectiveSecret    = config.clientSecret;
            String effectiveGroupsClaim = config.groupsClaim;
            String effectiveGroupsUrl   = config.groupsUrl;

            if (!trustedIdps.isEmpty()) {
                final String tokenIssuer = extractUnverifiedIssuer(form.getIdToken());
                if (tokenIssuer == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ResponseEntityView<>("Could not decode iss claim from id_token"))
                            .build();
                }
                final Optional<Map<String, Object>> matched = trustedIdps.stream()
                        .filter(idp -> Boolean.TRUE.equals(idp.get("enabled"))
                                || "true".equals(String.valueOf(idp.get("enabled"))))
                        .filter(idp -> tokenIssuer.equals(String.valueOf(idp.get("issuer"))))
                        .findFirst();
                if (matched.isEmpty()) {
                    SecurityLogger.logInfo(DotAuthOAuthExchangeResource.class,
                            "OAuth exchange rejected: issuer '" + tokenIssuer
                                    + "' not in trusted IdP list from " + request.getRemoteAddr());
                    return Response.status(Response.Status.UNAUTHORIZED)
                            .entity(new ResponseEntityView<>("Untrusted token issuer"))
                            .build();
                }
                final Map<String, Object> idp = matched.get();
                effectiveIssuer    = String.valueOf(idp.getOrDefault("issuer", effectiveIssuer));
                effectiveGroupsClaim = String.valueOf(idp.getOrDefault("claimGroups", effectiveGroupsClaim));
                final String idpAudience = String.valueOf(idp.getOrDefault("audience", ""));
                if (UtilMethods.isSet(idpAudience)) {
                    effectiveClientId = idpAudience;
                }
            }

            // Build the OIDC provider using the effective config (from trusted IdP match or site-level).
            final OIDCProvider provider = new OIDCProvider(
                    effectiveIssuer, effectiveClientId, effectiveSecret,
                    effectiveGroupsClaim, effectiveGroupsUrl);

            // Core security step: signature-verify against JWKS, check iss/aud/exp/nonce.
            final Map<String, Object> claims;
            try {
                claims = provider.validateIdTokenAndExtractClaims(form.getIdToken(), form.getNonce());
            } catch (final DotRuntimeException e) {
                SecurityLogger.logInfo(DotAuthOAuthExchangeResource.class,
                        "OAuth id_token validation failed from " + request.getRemoteAddr()
                                + ": " + e.getMessage());
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ResponseEntityView<>("id_token validation failed"))
                        .build();
            }

            // Hand the verified claims to the shared JIT-provisioning path as the userinfo
            // payload. accessToken=null because we do not accept or rely on one — the SPA
            // already did the token exchange with the IdP; we only need the signed claims.
            final User user;
            try {
                user = oauthHelper.resolveOrProvisionUser(provider, null, claims, config, /* frontEndLogin */ true);
            } catch (final DotRuntimeException e) {
                // "is not active" is the one case where the user exists but we refuse to
                // mint a credential for them — that's a 403 rather than a 401.
                if (e.getMessage() != null && e.getMessage().contains("is not active")) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(new ResponseEntityView<>(e.getMessage())).build();
                }
                throw e;
            } catch (final DotDataException e) {
                // Checked exception from the user/role APIs — typically a DB / data-layer
                // failure during JIT provisioning. Do not leak the underlying message (it
                // can include SQL state, table / column names) onto the wire. Log server-side
                // with the throwable so ops gets the full stack, return a fixed-string 500.
                Logger.error(DotAuthOAuthExchangeResource.class,
                        "OAuth exchange user provisioning failed for request from " + request.getRemoteAddr(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ResponseEntityView<>("User provisioning failed"))
                        .build();
            }

            // Session lifetime: prefer per-site config (minutes), fall back to env-var (days)
            long lifetimeMillis;
            final int configTtlMinutes = config.sessionRefTtlMinutes;
            if (configTtlMinutes > 0) {
                lifetimeMillis = configTtlMinutes * 60_000L;
            } else {
                final int expirationDays = clampExpirationDays(form.getExpirationDays());
                lifetimeMillis = ChronoUnit.DAYS.getDuration().toMillis() * expirationDays;
            }

            // Clamp to IdP token exp so the session-ref never outlives the JWT it was minted from
            if (config.clampToIdpExp) {
                final Object expClaim = claims.get("exp");
                if (expClaim instanceof Number) {
                    final long idpExpiresAtMillis = ((Number) expClaim).longValue() * 1000L;
                    final long idpRemainingMillis = idpExpiresAtMillis - System.currentTimeMillis();
                    if (idpRemainingMillis > 0 && idpRemainingMillis < lifetimeMillis) {
                        lifetimeMillis = idpRemainingMillis;
                    }
                }
            }

            final String sessionRef = sessionCache.create(user.getUserId(), lifetimeMillis);
            final String expiresAt  = Instant.now().plusMillis(lifetimeMillis).toString();
            final int expirationDays = (int) Math.ceil(lifetimeMillis / (double) ChronoUnit.DAYS.getDuration().toMillis());

            SecurityLogger.logInfo(DotAuthOAuthExchangeResource.class,
                    "OAuth exchange issued session-ref for user " + user.getUserId()
                            + " (" + user.getEmailAddress() + ") from " + request.getRemoteAddr());

            final OAuthExchangeView.UserSummary summary = new OAuthExchangeView.UserSummary(
                    user.getUserId(),
                    user.getEmailAddress(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getFullName(),
                    loadRoleKeys(user));

            return Response.ok(new ResponseEntityOAuthExchangeView(
                    new OAuthExchangeView(sessionRef, expiresAt, expirationDays, summary))).build();
        } catch (final Exception e) {
            // Fixed-string 500 on the wire — the exception's message can include
            // low-level detail (SQL state, table / column names, stack context) that
            // the SPA has no legitimate need for. Log server-side with the throwable
            // so ops gets the full stack; Logger.warn(message) alone would have
            // silently dropped it.
            Logger.error(DotAuthOAuthExchangeResource.class,
                    "OAuth exchange failed for request from " + request.getRemoteAddr(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ResponseEntityView<>("Token exchange failed"))
                    .build();
        }
    }

    /**
     * Load the applied role keys for this user so the SPA can render role-gated UI
     * without a second round-trip. Returns an empty list if role resolution fails —
     * the SPA can still proceed, it just won't have role-aware UI hints.
     */
    private List<String> loadRoleKeys(final User user) {
        return Try.of(() -> APILocator.getRoleAPI().loadRolesForUser(user.getUserId()))
                .getOrElse(Collections.emptyList())
                .stream()
                .map(Role::getRoleKey)
                .filter(UtilMethods::isSet)
                .collect(Collectors.toList());
    }

    /**
     * Clamp the requested session lifetime to the configured default and max. The
     * defaults (7 / 7) match the FE team's NextAuth session {@code maxAge} so the
     * two session clocks stay aligned without per-environment tuning.
     *
     * <p>Guards against three misconfig foot-guns:
     * <ul>
     *   <li>A non-positive {@code DOTAUTH_SESSION_DEFAULT_DAYS} falls back to the
     *       hard-coded default — otherwise a caller that omits {@code expirationDays}
     *       would receive a 0-day session (dead on arrival).</li>
     *   <li>A non-positive {@code DOTAUTH_SESSION_MAX_DAYS} falls back to the
     *       hard-coded max — otherwise the ceiling is effectively disabled and any
     *       caller-supplied value is honored up to {@link Integer#MAX_VALUE}.</li>
     *   <li>The result is floored at 1 day so we never hand back a session whose
     *       expiry is already in the past.</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private static List<String> parseJsonStringList(final String json) {
        if (!UtilMethods.isSet(json) || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        return Try.of(() -> (List<String>) new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, List.class))
                .getOrElse(Collections.emptyList());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseJsonMapList(final String json) {
        if (!UtilMethods.isSet(json) || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        return Try.of(() -> (List<Map<String, Object>>) new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {}))
                .getOrElse(Collections.emptyList());
    }

    private static String extractUnverifiedIssuer(final String idToken) {
        try {
            final String[] parts = idToken.split("\\.");
            if (parts.length < 2) return null;
            final String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            final com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
            return node.has("iss") ? node.get("iss").asText() : null;
        } catch (final Exception e) {
            Logger.debug(DotAuthOAuthExchangeResource.class, "Failed to extract iss from id_token", e);
            return null;
        }
    }

    private int clampExpirationDays(final int requested) {
        final int rawDefault = Config.getIntProperty(DEFAULT_DAYS_PROP, DEFAULT_DAYS_FALLBACK);
        final int rawMax     = Config.getIntProperty(MAX_DAYS_PROP,     MAX_DAYS_FALLBACK);
        final int defaultDays = rawDefault > 0 ? rawDefault : DEFAULT_DAYS_FALLBACK;
        final int maxAllowed  = rawMax     > 0 ? rawMax     : MAX_DAYS_FALLBACK;
        final int effective   = requested <= 0 ? defaultDays : requested;
        return Math.max(1, Math.min(effective, maxAllowed));
    }
}
