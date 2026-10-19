package com.dotcms.test.util;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.datagen.TestUserUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Mints short-lived API tokens for integration-test fixtures so authenticated requests can use a
 * Bearer credential instead of the username/password Basic transport, which is disabled by default
 * ({@code ALLOW_BASIC_AUTH=NONE}). Each call persists a new token for exactly one fixture user and
 * returns the {@code Authorization} header value that presents it; nothing is cached or shared
 * across fixtures, so production token validation and revocation stay on the real code path.
 */
public final class ApiTokenTestUtils {

    /** Fixture tokens live only long enough for one test class to run. */
    private static final Duration FIXTURE_TOKEN_TTL = Duration.ofDays(1);

    private ApiTokenTestUtils() {
    }

    /**
     * Persists an API token owned by the given user and returns its {@code Bearer} header value.
     *
     * @param user the fixture user the request should authenticate as
     * @return the {@code Authorization} header value carrying that user's API token
     */
    public static String bearerHeader(final User user) {
        final ApiToken apiToken = APILocator.getApiTokenAPI().persistApiToken(
                user.getUserId(),
                Date.from(Instant.now().plus(FIXTURE_TOKEN_TTL)),
                APILocator.systemUser().getUserId(),
                "127.0.0.1");
        return "Bearer " + APILocator.getApiTokenAPI().getJWT(apiToken, user);
    }

    /**
     * Resolves a fixture user by email and returns a Bearer header owned by that user. Keeps the
     * existing email/password-shaped request helpers working without changing their call sites.
     *
     * @param email the fixture user's email address
     * @return the {@code Authorization} header value carrying that user's API token
     */
    public static String bearerHeaderForEmail(final String email) {
        try {
            return bearerHeader(APILocator.getUserAPI()
                    .loadByUserByEmail(email, APILocator.systemUser(), false));
        } catch (Exception e) {
            Logger.error(ApiTokenTestUtils.class, "Unable to resolve fixture user " + email, e);
            throw new IllegalStateException("Unable to resolve fixture user " + email, e);
        }
    }

    /**
     * Convenience for the many fixtures that authenticated as the default {@code admin@dotcms.com}
     * account through Basic. Resolves that account and mints a token owned by it.
     *
     * @return the {@code Authorization} header value carrying the admin user's API token
     */
    public static String adminBearerHeader() {
        try {
            return bearerHeader(TestUserUtils.getAdminUser());
        } catch (DotDataException e) {
            Logger.error(ApiTokenTestUtils.class, "Unable to resolve the admin fixture user", e);
            throw new IllegalStateException("Unable to resolve the admin fixture user", e);
        }
    }
}
