package com.dotcms.auth;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
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
        if (username == null || username.isEmpty() || credential == null || credential.isEmpty()) {
            return Optional.empty();
        }
        final String jwt = credential.trim();
        if (jwt.isEmpty()) {
            return Optional.empty();
        }
        return APILocator.getApiTokenAPI().fromJwt(jwt, ipAddress)
                .filter(ApiToken.class::isInstance)
                .flatMap(token -> token.getActiveUser())
                .filter(user -> Company.AUTH_TYPE_EA.equals(CompanyUtils.getDefaultCompany().getAuthType())
                        ? username.equalsIgnoreCase(user.getEmailAddress())
                        : username.equals(user.getUserId()));
    }
}
