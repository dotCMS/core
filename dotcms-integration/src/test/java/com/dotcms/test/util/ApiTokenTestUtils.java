package com.dotcms.test.util;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.datagen.TestUserUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.rules.ExternalResource;

/**
 * Mints short-lived API tokens for integration-test fixtures so authenticated requests can use a
 * Bearer credential instead of the username/password Basic transport, which is disabled by default
 * ({@code ALLOW_BASIC_AUTH=NONE}).
 *
 * <p>This is a per-fixture JUnit resource, not a global helper: each fixture declares its own
 * instance as a {@code @ClassRule}, so the tokens it mints during the class are deleted at the end
 * of that class. No other fixture's tokens, cache credentials or the "revoke all" endpoint are
 * touched. Because the instance owns its own list, fixtures running in parallel cannot delete each
 * other's tokens.</p>
 *
 * <p>The token object is retained until the delete call, so cleanup never has to look the token up
 * again. {@code ApiTokenAPI.deleteToken} revalidates the owning user, so a token whose owner was
 * already removed fails the first delete; the deletion copy is then re-homed to the system user and
 * deleted by the same token id. Both calls are best-effort: if each one fails the failure is logged
 * and the remaining tokens are still cleaned up.</p>
 */
public class ApiTokenTestUtils extends ExternalResource implements AutoCloseable {

    /** Fixture tokens live only long enough for one test class to run. */
    private static final Duration FIXTURE_TOKEN_TTL = Duration.ofDays(1);

    private final List<ApiToken> issued = new ArrayList<>();
    private ApiTokenAPI tokenAPI;
    private User deletingUser;

    /** Creates a resource that resolves the token API and system user lazily on first use. */
    public ApiTokenTestUtils() {
    }

    /**
     * Test-only constructor that avoids the static API locator and the database.
     *
     * @param tokenAPI the token API to persist, sign and delete with
     * @param deletingUser the user whose authority deletes the issued tokens
     */
    @VisibleForTesting
    ApiTokenTestUtils(final ApiTokenAPI tokenAPI, final User deletingUser) {
        this.tokenAPI = tokenAPI;
        this.deletingUser = deletingUser;
    }

    /**
     * Persists an API token owned by the given user, remembers it for cleanup, and returns its
     * {@code Bearer} header value.
     *
     * @param user the fixture user the request should authenticate as
     * @return the {@code Authorization} header value carrying that user's API token
     */
    public String bearerHeader(final User user) {
        final ApiToken apiToken = apiTokenAPI().persistApiToken(
                user.getUserId(),
                Date.from(Instant.now().plus(FIXTURE_TOKEN_TTL)),
                deletingUser().getUserId(),
                "127.0.0.1");
        issued.add(apiToken);
        return "Bearer " + apiTokenAPI().getJWT(apiToken, user);
    }

    /**
     * Resolves a fixture user by email and returns a Bearer header owned by that user. Keeps the
     * existing email/password-shaped request helpers working without changing their call sites.
     *
     * @param email the fixture user's email address
     * @return the {@code Authorization} header value carrying that user's API token
     */
    public String bearerHeaderForEmail(final String email) {
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
    public String adminBearerHeader() {
        try {
            return bearerHeader(TestUserUtils.getAdminUser());
        } catch (DotDataException e) {
            Logger.error(ApiTokenTestUtils.class, "Unable to resolve the admin fixture user", e);
            throw new IllegalStateException("Unable to resolve the admin fixture user", e);
        }
    }

    /** Deletes every token this instance minted, then forgets them. */
    @Override
    protected void after() {
        deleteIssuedTokens();
    }

    /**
     * Deletes every token this instance minted, for helpers that cannot register a JUnit rule and
     * must clean up before the token owner is removed.
     */
    @Override
    public void close() {
        deleteIssuedTokens();
    }

    /**
     * Deletes exactly the tokens minted through this instance and clears the tracking list. The
     * delete is by token id, so it does not depend on the owning user still existing.
     */
    @VisibleForTesting
    void deleteIssuedTokens() {
        final User deleter = deletingUser();
        for (final ApiToken token : issued) {
            delete(token, deleter);
        }
        issued.clear();
    }

    /**
     * Deletes one minted token. {@code ApiTokenAPI.deleteToken} revalidates the owning user, so if
     * the owner was removed after the token was minted the first call fails; the deletion copy is
     * then re-homed to the system user and deleted by the same token id. Failures are logged so a
     * single orphan cannot stop the rest of the cleanup.
     *
     * @param token the token this instance minted
     * @param deleter the system user performing the delete
     */
    private void delete(final ApiToken token, final User deleter) {
        try {
            apiTokenAPI().deleteToken(token, deleter);
        } catch (Exception firstAttempt) {
            try {
                apiTokenAPI().deleteToken(
                        ApiToken.from(token).withUserId(deleter.getUserId()).build(), deleter);
            } catch (Exception e) {
                Logger.warn(ApiTokenTestUtils.class,
                        "Unable to delete fixture API token " + token.id, e);
            }
        }
    }

    /** Returns the token API, resolving it from the locator on first use. */
    private ApiTokenAPI apiTokenAPI() {
        if (tokenAPI == null) {
            tokenAPI = APILocator.getApiTokenAPI();
        }
        return tokenAPI;
    }

    /** Returns the deleting user, resolving the system user from the locator on first use. */
    private User deletingUser() {
        if (deletingUser == null) {
            deletingUser = APILocator.systemUser();
        }
        return deletingUser;
    }
}
