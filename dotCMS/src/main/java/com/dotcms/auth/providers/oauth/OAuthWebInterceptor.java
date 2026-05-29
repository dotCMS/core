package com.dotcms.auth.providers.oauth;

import com.dotcms.auth.AuthAccessDeniedUtil;
import com.dotcms.auth.providers.oauth.provider.GenericOAuth2Provider;
import com.dotcms.auth.providers.oauth.provider.OAuthCrypto;
import com.dotcms.auth.providers.oauth.provider.OAuthProvider;
import com.dotcms.auth.providers.oauth.provider.OIDCProvider;
import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Single consolidated interceptor that handles the OAuth 2.0 / OIDC flow:
 * <ul>
 *     <li>Login redirect when an unauthenticated user hits a protected URL</li>
 *     <li>Callback at {@code /api/v1/oauth/callback} — exchanges code, logs user in</li>
 *     <li>Logout — revokes the token and (optionally) redirects to provider logout</li>
 * </ul>
 * <p>
 * Kept as one class (instead of the plugin's three) because the routing is URL-based;
 * splitting gains nothing and costs readability.
 */
public class OAuthWebInterceptor implements WebInterceptor {

    private static final long serialVersionUID = 1L;

    private static final List<String> BACK_END_URLS =
            ImmutableList.copyOf(OAuthConstants.BACK_END_URLS);
    private static final List<String> FRONT_END_URLS =
            ImmutableList.copyOf(OAuthConstants.FRONT_END_URLS);
    private static final String[] ALLOWED_URL_FRAGMENTS = Config.getStringArrayProperty(
            "OAUTH_LOGIN_ALLOWED_URLS", OAuthConstants.DEFAULT_ALLOWED_URL_FRAGMENTS);
    private static final List<String> ALLOWED_URL_FRAGMENTS_LIST = Arrays.asList(ALLOWED_URL_FRAGMENTS);

    private final OAuthHelper oauthHelper = new OAuthHelper();

    @Override
    public String[] getFilters() {
        // Register interest in back-end, front-end, callback, and logout URLs.
        return ImmutableList.<String>builder()
                .addAll(BACK_END_URLS)
                .addAll(FRONT_END_URLS)
                .add(OAuthConstants.CALLBACK_PATH)
                .add(OAuthConstants.LOGOUT_PATHS)
                .build()
                .toArray(new String[0]);
    }

    @Override
    public Result intercept(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        final String uri = request.getRequestURI();

        if (uri.startsWith(OAuthConstants.CALLBACK_PATH)) {
            return handleCallback(request, response);
        }
        if (isLogoutPath(uri)) {
            return handleLogout(request, response);
        }
        return handleLoginRequired(request, response);
    }

    // ---------- login redirect ----------

    private Result handleLoginRequired(final HttpServletRequest request,
                                       final HttpServletResponse response) throws IOException {

        final Optional<OAuthAppConfig> cfgOpt = OAuthAppConfig.config(request);
        if (cfgOpt.isEmpty()) {
            return Result.NEXT;
        }
        final OAuthAppConfig config = cfgOpt.get();

        // Avoid request.getSession() (no-arg) here — that creates a fresh session on every
        // hit, including scanner/bot traffic that never reaches the redirect branch below.
        // Only materialize a session when we have something real to store or read.
        final HttpSession existingSession = request.getSession(false);

        // ?native=false clears the bypass — nothing to remove if no session exists yet.
        if (Boolean.FALSE.toString().equalsIgnoreCase(request.getParameter(OAuthConstants.PARAM_NATIVE))
                && existingSession != null) {
            existingSession.removeAttribute(OAuthConstants.SESSION_NATIVE_LOGIN);
        }
        // ?native=true sets the bypass for this session — explicit opt-in, create if needed.
        if (Boolean.TRUE.toString().equalsIgnoreCase(request.getParameter(OAuthConstants.PARAM_NATIVE))) {
            request.getSession().setAttribute(OAuthConstants.SESSION_NATIVE_LOGIN, Boolean.TRUE);
            return Result.NEXT;
        }
        if (existingSession != null
                && Boolean.TRUE.equals(existingSession.getAttribute(OAuthConstants.SESSION_NATIVE_LOGIN))) {
            return Result.NEXT;
        }

        // Normalize the URI once so path-matching can't be tricked with "/./" or "/../" segments.
        final String uri = normalizePath(request.getRequestURI());
        if (ALLOWED_URL_FRAGMENTS_LIST.stream().anyMatch(uri::contains)) {
            return Result.NEXT;
        }

        final boolean isFrontEndLogin = config.enableFrontend
                && FRONT_END_URLS.stream().anyMatch(uri::startsWith);
        final boolean isBackEndLogin = config.enableBackend
                && BACK_END_URLS.stream().anyMatch(uri::startsWith);

        if (!isFrontEndLogin && !isBackEndLogin) {
            return Result.NEXT;
        }

        final com.liferay.portal.model.User existingUser = PortalUtil.getUser(request);
        if (existingUser != null) {
            final boolean hasRequiredRole = (isBackEndLogin && existingUser.isBackendUser())
                    || (isFrontEndLogin && existingUser.isFrontendUser());
            if (hasRequiredRole) {
                return Result.NEXT;
            }
            // Authenticated via SSO but lacks the required role — show a 403 rather
            // than redirecting to the IdP (which would loop since the user IS authenticated).
            AuthAccessDeniedUtil.sendNoAccessPage(response, existingUser);
            return Result.SKIP_NO_CHAIN;
        }

        final OAuthProvider provider = Try.of(() -> buildProvider(config))
                .onFailure(e -> Logger.warn(this, "Unable to build OAuth provider: " + e.getMessage()))
                .getOrNull();
        if (provider == null) {
            return Result.NEXT;
        }

        final String callbackUrl = computeCallbackUrl(request, config);
        if (callbackUrl == null) {
            // §1.6: fail open to NEXT rather than redirecting to a callback URL derived
            // from an attacker-controlled Host header. Admins see a SECURITY-logged warning.
            return Result.NEXT;
        }

        // Remember the URI we want to return to after login, and whether this was a front-end login.
        AuthAccessDeniedUtil.setNoCacheHeaders(response);
        final HttpSession session = request.getSession();
        session.setAttribute(OAuthConstants.SESSION_ORIGINAL_REQUEST, originalRequestUri(request));
        session.setAttribute(OAuthConstants.SESSION_FRONT_END_LOGIN, isFrontEndLogin);

        // state (CSRF), nonce (OIDC id_token replay guard), PKCE verifier (auth-code interception guard).
        final String state         = OAuthCrypto.newState();
        final String nonce         = config.isOidc() ? OAuthCrypto.newNonce() : null;
        final String codeVerifier  = OAuthCrypto.newPkceVerifier();
        final String codeChallenge = OAuthCrypto.pkceChallengeS256(codeVerifier);

        session.setAttribute(OAuthConstants.SESSION_STATE, state);
        session.setAttribute(OAuthConstants.SESSION_CODE_VERIFIER, codeVerifier);
        if (nonce != null) {
            session.setAttribute(OAuthConstants.SESSION_NONCE, nonce);
        } else {
            session.removeAttribute(OAuthConstants.SESSION_NONCE);
        }

        final String authUrl = provider.buildAuthorizationUrl(
                state, nonce, codeChallenge, callbackUrl, config.scopes);
        Logger.info(this, "OAuth: redirecting to provider authorization URL");
        response.sendRedirect(authUrl);
        return Result.SKIP_NO_CHAIN;
    }

    // ---------- callback ----------

    private Result handleCallback(final HttpServletRequest request,
                                  final HttpServletResponse response) throws IOException {

        AuthAccessDeniedUtil.setNoCacheHeaders(response);

        final User existingUser = PortalUtil.getUser(request);
        if (existingUser != null) {
            final HttpSession existingSession = request.getSession(false);
            final boolean frontEndLogin = existingSession != null
                    && Boolean.TRUE.equals(existingSession.getAttribute(OAuthConstants.SESSION_FRONT_END_LOGIN));
            if (AuthAccessDeniedUtil.hasRequiredRole(existingUser, frontEndLogin)) {
                response.sendRedirect(frontEndLogin ? "/" : "/dotAdmin/");
            } else {
                AuthAccessDeniedUtil.sendNoAccessPage(response, existingUser);
            }
            return Result.SKIP_NO_CHAIN;
        }

        final Optional<OAuthAppConfig> cfgOpt = OAuthAppConfig.config(request);
        if (cfgOpt.isEmpty()) {
            response.sendRedirect("/?error=oauth+not+configured");
            return Result.SKIP_NO_CHAIN;
        }
        final OAuthAppConfig config = cfgOpt.get();

        final String code  = request.getParameter(OAuthConstants.PARAM_CODE);
        final String state = request.getParameter(OAuthConstants.PARAM_STATE);
        if (!UtilMethods.isSet(code)) {
            response.sendRedirect("/?error=oauth+no+code");
            return Result.SKIP_NO_CHAIN;
        }

        // CSRF protection: state must match what we stored before redirect. Constant-time
        // comparison so a timing channel can't leak the expected value byte-by-byte.
        final HttpSession session = request.getSession();
        final String expectedState = (String) session.getAttribute(OAuthConstants.SESSION_STATE);
        if (!UtilMethods.isSet(expectedState) || !UtilMethods.isSet(state)
                || !MessageDigest.isEqual(
                        expectedState.getBytes(StandardCharsets.UTF_8),
                        state.getBytes(StandardCharsets.UTF_8))) {
            Logger.warn(this, "OAuth callback state mismatch — possible CSRF, rejecting");
            response.sendRedirect("/?error=oauth+state+mismatch");
            return Result.SKIP_NO_CHAIN;
        }
        session.removeAttribute(OAuthConstants.SESSION_STATE);

        try {
            final OAuthProvider provider = buildProvider(config);
            final String callbackUrl = computeCallbackUrl(request, config);
            if (callbackUrl == null) {
                response.sendRedirect("/?error=oauth+callback+url+missing");
                return Result.SKIP_NO_CHAIN;
            }
            final String codeVerifier = (String) session.getAttribute(OAuthConstants.SESSION_CODE_VERIFIER);
            session.removeAttribute(OAuthConstants.SESSION_CODE_VERIFIER);
            final String expectedNonce = (String) session.getAttribute(OAuthConstants.SESSION_NONCE);
            session.removeAttribute(OAuthConstants.SESSION_NONCE);

            final Map<String, Object> tokenResponse = provider.exchangeCodeForToken(code, codeVerifier, callbackUrl);
            final String accessToken = (String) tokenResponse.get("access_token");
            final String idToken     = (String) tokenResponse.get("id_token");

            // For OIDC, the id_token MUST be present and MUST validate. Refusing to proceed here
            // means a man-in-the-middle or rogue IdP can't log a user in by intercepting just the code.
            final Map<String, Object> verifiedClaims;
            if (config.isOidc()) {
                if (!UtilMethods.isSet(idToken)) {
                    throw new com.dotmarketing.exception.DotRuntimeException(
                            "OIDC token response did not include an id_token — refusing to authenticate");
                }
                verifiedClaims = provider.validateIdTokenAndExtractClaims(idToken, expectedNonce);
                OIDCProvider.validateAtHash(idToken, accessToken);
            } else {
                verifiedClaims = null;
            }

            final boolean frontEndLogin = Boolean.TRUE.equals(session.getAttribute(OAuthConstants.SESSION_FRONT_END_LOGIN));
            final Map<String, Object> userInfo = provider.getUserInfo(accessToken);

            // The id_token is the only signed, audience- and nonce-bound artifact in this flow;
            // the userinfo response is not. OIDC Core §5.3.2 requires the userinfo subject to
            // equal the id_token subject — enforce it so a substituted access token can't pivot
            // provisioning onto a different identity than the one we cryptographically verified.
            if (verifiedClaims != null) {
                final Object idSub = verifiedClaims.get("sub");
                final Object userInfoSub = userInfo == null ? null : userInfo.get("sub");
                if (idSub != null && userInfoSub != null
                        && !String.valueOf(idSub).equals(String.valueOf(userInfoSub))) {
                    throw new com.dotmarketing.exception.DotRuntimeException(
                            "OIDC userinfo subject does not match the verified id_token subject — refusing to authenticate");
                }
            }

            final User user = oauthHelper.resolveOrProvisionUser(
                    provider, accessToken, userInfo, verifiedClaims, config, frontEndLogin);

            if (!AuthAccessDeniedUtil.hasRequiredRole(user, frontEndLogin)) {
                AuthAccessDeniedUtil.sendNoAccessPage(response, user);
                return Result.SKIP_NO_CHAIN;
            }

            oauthHelper.login(request, response, provider, accessToken, user);

            final HttpSession postLoginSession = request.getSession(false);
            if (postLoginSession != null && UtilMethods.isSet(idToken)) {
                postLoginSession.setAttribute(OAuthConstants.SESSION_ID_TOKEN, idToken);
            }

            final String originalUri = (String) session.getAttribute(OAuthConstants.SESSION_ORIGINAL_REQUEST);
            session.removeAttribute(OAuthConstants.SESSION_ORIGINAL_REQUEST);

            final String fallback = user.isBackendUser() ? "/dotAdmin/" : "/";
            final String redirectTo = sanitizeRedirect(originalUri, fallback);
            response.sendRedirect(redirectTo);
            return Result.SKIP_NO_CHAIN;
        } catch (final Exception e) {
            Logger.error(this, "OAuth callback failed: " + e.getMessage(), e);
            Try.run(() -> APILocator.getLoginServiceAPI().doActionLogout(request, response))
                    .onFailure(ex -> Logger.debug(this, "cleanup logout failed: " + ex.getMessage()));
            final HttpSession staleSession = request.getSession(false);
            if (staleSession != null) {
                Try.run(staleSession::invalidate);
            }
            response.sendRedirect("/?error=oauth+callback+failed");
            return Result.SKIP_NO_CHAIN;
        }
    }

    // ---------- logout ----------

    private Result handleLogout(final HttpServletRequest request,
                                final HttpServletResponse response) throws IOException {

        final HttpSession session = request.getSession(false);
        final Optional<OAuthAppConfig> cfgOpt = OAuthAppConfig.config(request);

        // No session or no config — fall through to the normal logout chain
        // (LogoutResource / LogoutWebInterceptor).
        if (session == null || cfgOpt.isEmpty()) {
            return Result.NEXT;
        }

        final OAuthAppConfig config = cfgOpt.get();
        AuthAccessDeniedUtil.setNoCacheHeaders(response);

        final String accessToken = (String) session.getAttribute(OAuthConstants.SESSION_ACCESS_TOKEN);
        final String idToken = (String) session.getAttribute(OAuthConstants.SESSION_ID_TOKEN);
        String providerLogoutUrl = null;

        try {
            final OAuthProvider provider = buildProvider(config);
            if (UtilMethods.isSet(accessToken)) {
                provider.revokeToken(accessToken);
            }
            providerLogoutUrl = provider.getLogoutUrl(idToken, null).orElse(null);
        } catch (final Exception e) {
            Logger.warn(this, "OAuth logout failed during token revocation / logout URL resolution: " + e.getMessage());
        }

        session.removeAttribute(OAuthConstants.SESSION_ACCESS_TOKEN);
        session.removeAttribute(OAuthConstants.SESSION_ID_TOKEN);
        session.removeAttribute(OAuthConstants.SESSION_PROVIDER_TYPE);

        return doCoreLogout(request, response, providerLogoutUrl);
    }

    /**
     * OAuth logout short-circuits the interceptor chain (returns SKIP_NO_CHAIN) because the
     * 302 to the IdP end-session endpoint must be the final response. LogoutWebInterceptor
     * would otherwise forward to show-logout.jsp, which conflicts with the provider logout.
     * We replicate its audit log here so the logout event is still captured.
     */
    private Result doCoreLogout(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final String providerLogoutUrl) throws IOException {
        final User user = PortalUtil.getUser(request);
        Try.run(() -> APILocator.getLoginServiceAPI().doActionLogout(request, response))
                .onFailure(e -> Logger.warn(this, "doActionLogout failed: " + e.getMessage()));
        if (user != null) {
            SecurityLogger.logInfo(OAuthWebInterceptor.class,
                    "User " + user.getFullName() + " (" + user.getUserId()
                            + ") has logged out via OAuth from IP: " + request.getRemoteAddr());
        }
        if (!response.isCommitted()) {
            response.sendRedirect(UtilMethods.isSet(providerLogoutUrl) ? providerLogoutUrl : "/");
        }
        return Result.SKIP_NO_CHAIN;
    }

    // ---------- helpers ----------

    private OAuthProvider buildProvider(final OAuthAppConfig config) {
        if (config.isOidc()) {
            return new OIDCProvider(config.issuerUrl, config.clientId, config.clientSecret,
                    config.groupsClaim, config.groupsUrl);
        }
        return new GenericOAuth2Provider(config.clientId, config.clientSecret,
                config.authorizationUrl, config.tokenUrl, config.userinfoUrl,
                config.revocationUrl, config.logoutUrl,
                config.groupsClaim, config.groupsUrl);
    }

    /**
     * Resolve the callback URL the IdP should redirect to. If an explicit {@code callbackUrl}
     * is configured it is used as the base (the callback path is appended if missing).
     * Otherwise login is refused so the redirect URI is never derived from the Host header.
     */
    private String computeCallbackUrl(final HttpServletRequest request, final OAuthAppConfig config) {
        if (UtilMethods.isSet(config.callbackUrl)) {
            final String base = config.callbackUrl.replaceAll("/+$", "");
            return base.endsWith(OAuthConstants.CALLBACK_PATH)
                    ? base
                    : base + OAuthConstants.CALLBACK_PATH;
        }
        SecurityLogger.logInfo(OAuthWebInterceptor.class,
                "OAuth callbackUrl is not configured; refusing to derive redirect URI from request Host header");
        return null;
    }

    private static String originalRequestUri(final HttpServletRequest request) {
        final Object forward = request.getAttribute(javax.servlet.RequestDispatcher.FORWARD_REQUEST_URI);
        if (forward != null) {
            return forward.toString();
        }
        final String q = request.getQueryString();
        return q == null ? request.getRequestURI() : request.getRequestURI() + "?" + q;
    }

    /**
     * Normalize the request URI so path-matching cannot be bypassed with "/./" or "/../"
     * segments. Falls back to the raw path if the URI is malformed.
     */
    private static String normalizePath(final String uri) {
        if (uri == null) {
            return "";
        }
        try {
            final String normalized = new URI(uri).normalize().getPath();
            return normalized == null ? uri : normalized;
        } catch (final URISyntaxException e) {
            return uri;
        }
    }

    /**
     * Sanitize a post-auth redirect target. Only accept same-origin relative paths that
     * start with a single "/" — reject protocol-relative ("//evil"), absolute URLs,
     * backslashes, and anything with a scheme/authority delimiter.
     */
    static String sanitizeRedirect(final String candidate, final String fallback) {
        if (candidate == null || candidate.isEmpty()) {
            return fallback;
        }
        final int queryIndex = candidate.indexOf('?');
        final String path = queryIndex < 0 ? candidate : candidate.substring(0, queryIndex);
        if (!candidate.startsWith("/")
                || candidate.startsWith("//")
                || candidate.startsWith("/\\")
                || candidate.contains("\\")
                || path.contains(":")) {
            return fallback;
        }
        return candidate;
    }

    private static boolean isLogoutPath(final String uri) {
        for (final String path : OAuthConstants.LOGOUT_PATHS) {
            if (uri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

}
