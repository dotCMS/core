package com.dotcms.auth;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import java.util.Locale;
import java.util.Optional;

/** Shared policy for username/credential transports, independent of browser and Bearer login. */
public final class BasicAuthentication {

    /** NONE disables credentials, JWT_TOKEN accepts API JWTs, and ALL also permits legacy passwords. */
    public enum Mode { NONE, JWT_TOKEN, ALL }

    /**
     * Why a username/API-JWT pair was refused. Lets a caller that audits authentication say what was
     * wrong without reproducing the credential.
     */
    public enum Rejection {
        /** The login name or the credential was missing or blank. */
        NO_CREDENTIALS,
        /** The credential is not a JWT the token API accepts: malformed, badly signed, expired, revoked, sent from outside the token's allowed network, or otherwise refused. */
        NOT_A_TOKEN,
        /** The credential is a valid JWT but not an API token, for example a browser login token. */
        WRONG_TOKEN_TYPE,
        /** The API token's owner no longer exists or is inactive. */
        INACTIVE_USER,
        /** The API token belongs to a different account than the login name. */
        OWNER_MISMATCH,
        /**
         * The token is valid and its owner matches, but that account may not act as the identity of a
         * client request (the built-in system account, see {@link InteractiveUserPolicy}). Reported by
         * callers that apply that policy; this class does not check it.
         */
        INELIGIBLE_ACCOUNT
    }

    /**
     * Outcome of authenticating a username/API-JWT pair: either the active owner of the token or the
     * reason the pair was refused, plus the parsed token when there was one. Immutable; built only by
     * {@link BasicAuthentication#authenticateApiTokenDetailed(String, String, String)}.
     */
    public static final class ApiTokenAuthentication {

        private final User user;
        private final JWToken token;
        private final Rejection rejection;

        private ApiTokenAuthentication(final User user, final JWToken token, final Rejection rejection) {
            this.user = user;
            this.token = token;
            this.rejection = rejection;
        }

        /** An accepted pair: the token's active owner, whose login name matched. */
        private static ApiTokenAuthentication accepted(final User user, final JWToken token) {
            return new ApiTokenAuthentication(user, token, null);
        }

        /** A refused pair, with the token when one was parsed before the refusal. */
        private static ApiTokenAuthentication rejected(final Rejection rejection, final JWToken token) {
            return new ApiTokenAuthentication(null, token, rejection);
        }

        /** @return whether the pair identifies an active account */
        public boolean isAccepted() {
            return user != null;
        }

        /** @return the authenticated {@link User}, present only when the pair was accepted */
        public Optional<User> user() {
            return Optional.ofNullable(user);
        }

        /**
         * The parsed token, present whenever the credential was a token the token API accepts. For a
         * login token its id and subject are token material and must not be logged or shown.
         *
         * @return the parsed {@link JWToken}, when there was one
         */
        public Optional<JWToken> token() {
            return Optional.ofNullable(token);
        }

        /** @return why the pair was refused, present only when it was */
        public Optional<Rejection> rejection() {
            return Optional.ofNullable(rejection);
        }
    }

    /** Utility class; callers resolve policy for each request. */
    private BasicAuthentication() {
    }

    /** Returns the configured mode; explicit FALSE, empty and unrecognized values fail closed. */
    public static Mode mode() {
        try {
            // The default accepts API JWTs as the Basic password: it validates the same persisted
            // token a Bearer header would (no password verification, no session), so a Basic
            // wrapper is credential-equivalent to Bearer. An absent property is that posture;
            // explicit NONE or FALSE disables the transports entirely.
            return Mode.valueOf(Config.getStringProperty("ALLOW_BASIC_AUTH", "JWT_TOKEN")
                    .trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Mode.NONE;
        }
    }

    /**
     * Validates a persisted API JWT and binds its active owner to the submitted login name.
     * The existing token API checks signature, issuer policy, dates, persistence, revocation and IP.
     * No password verifier, login hooks, browser cookies or session state are involved.
     *
     * @param username the original login name, following the company's email or user-ID convention
     * @param credential the compact API JWT
     * @param ipAddress the requesting client's address
     * @return the token's active {@link User}, only when its login name matches
     */
    public static Optional<User> authenticateApiToken(final String username, final String credential,
            final String ipAddress) {
        return authenticateApiTokenDetailed(username, credential, ipAddress).user();
    }

    /**
     * Same checks as {@link #authenticateApiToken(String, String, String)}, reporting why a pair was
     * refused so the caller can audit the decision. The checks run in the same order: the credential
     * must parse as a token the token API accepts, be an API token, belong to an active account, and
     * that account's login name must match the one submitted.
     *
     * @param username the original login name, following the company's email or user-ID convention
     * @param credential the compact API JWT
     * @param ipAddress the requesting client's address
     * @return the accepted {@link User} or the {@link Rejection}, with the parsed token when there was one
     */
    public static ApiTokenAuthentication authenticateApiTokenDetailed(final String username,
            final String credential, final String ipAddress) {
        if (username == null || username.isEmpty() || credential == null || credential.isEmpty()) {
            return ApiTokenAuthentication.rejected(Rejection.NO_CREDENTIALS, null);
        }
        final String jwt = credential.trim();
        if (jwt.isEmpty()) {
            return ApiTokenAuthentication.rejected(Rejection.NO_CREDENTIALS, null);
        }
        final Optional<JWToken> parsed = APILocator.getApiTokenAPI().fromJwt(jwt, ipAddress);
        if (parsed.isEmpty()) {
            return ApiTokenAuthentication.rejected(Rejection.NOT_A_TOKEN, null);
        }
        final JWToken token = parsed.get();
        if (!(token instanceof ApiToken)) {
            return ApiTokenAuthentication.rejected(Rejection.WRONG_TOKEN_TYPE, token);
        }
        final Optional<User> owner = token.getActiveUser();
        if (owner.isEmpty()) {
            return ApiTokenAuthentication.rejected(Rejection.INACTIVE_USER, token);
        }
        final User user = owner.get();
        final boolean loginMatches = Company.AUTH_TYPE_EA.equals(CompanyUtils.getDefaultCompany().getAuthType())
                ? username.equalsIgnoreCase(user.getEmailAddress())
                : username.equals(user.getUserId());
        return loginMatches
                ? ApiTokenAuthentication.accepted(user, token)
                : ApiTokenAuthentication.rejected(Rejection.OWNER_MISMATCH, token);
    }
}
