package com.dotcms.auth.dotAuth.rest;

import com.dotcms.auth.dotAuth.session.DotAuthSessionCache;
import com.dotcms.auth.dotAuth.session.DotAuthSessionCacheImpl;
import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.auth.providers.oauth.OAuthHelper;
import com.dotcms.auth.providers.oauth.provider.OIDCProvider;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
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

            // Site-scoped OAuth config. Falls back to SYSTEM_HOST per the dotAuth contract.
            final Optional<OAuthAppConfig> cfgOpt = OAuthAppConfig.config(request);
            if (cfgOpt.isEmpty()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(new ResponseEntityView<>("OAuth is not configured for this site"))
                        .build();
            }
            final OAuthAppConfig config = cfgOpt.get();

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

            // Build the OIDC provider, which pulls jwks_uri, token_endpoint, etc. from the
            // cached discovery document. Cheap on the warm path — hits the IdP only if the
            // TTL has expired.
            final OIDCProvider provider = new OIDCProvider(
                    config.issuerUrl, config.clientId, config.clientSecret,
                    config.groupsClaim, config.groupsUrl);

            // This is the core security step: signature-verify against JWKS, then check
            // iss / aud / exp / nonce. Throws DotRuntimeException on any failure.
            final Map<String, Object> claims;
            try {
                claims = provider.validateIdTokenAndExtractClaims(form.getIdToken(), form.getNonce());
            } catch (final DotRuntimeException e) {
                SecurityLogger.logInfo(DotAuthOAuthExchangeResource.class,
                        "OAuth id_token validation failed from " + request.getRemoteAddr()
                                + ": " + e.getMessage());
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ResponseEntityView<>("id_token validation failed: " + e.getMessage()))
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
            }

            final int expirationDays = clampExpirationDays(form.getExpirationDays());
            final long lifetimeMillis = ChronoUnit.DAYS.getDuration().toMillis() * expirationDays;
            final String sessionRef = sessionCache.create(user.getUserId(), lifetimeMillis);
            final String expiresAt  = Instant.now().plusMillis(lifetimeMillis).toString();

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
            Logger.warn(DotAuthOAuthExchangeResource.class,
                    "OAuth exchange failed: " + e.getMessage());
            return ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
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
     */
    private int clampExpirationDays(final int requested) {
        final int defaultDays = Config.getIntProperty(DEFAULT_DAYS_PROP, DEFAULT_DAYS_FALLBACK);
        final int maxAllowed  = Config.getIntProperty(MAX_DAYS_PROP, MAX_DAYS_FALLBACK);
        final int effective   = requested <= 0 ? defaultDays : requested;
        return maxAllowed > 0 && effective > maxAllowed ? maxAllowed : effective;
    }
}
