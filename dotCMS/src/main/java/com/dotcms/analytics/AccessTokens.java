package com.dotcms.analytics;

import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.util.DotPreconditions;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Analytics cache layer that puts and gets {@link AccessToken} instances into configured cache.
 *
 * @author vico
 */
public class AccessTokens {

    private static final AccessTokens INSTANCE = new AccessTokens();

    private final ConcurrentMap<String, AccessToken> accessTokens;

    private AccessTokens() {
        this.accessTokens = new ConcurrentHashMap<>();
    }

    /**
     * @return singleton instance
     */
    public static AccessTokens get() {
        return INSTANCE;
    }

    /**
     * Adds an {@link AccessToken} instance to cache identified by default cache.
     *
     * @param accessToken access token to cache
     */
    public void putAccessToken(final AccessToken accessToken) {
        DotPreconditions.notNull(accessToken, "AccessToken cannot be null");
        accessTokens.put(AnalyticsHelper.get().resolveKey(accessToken), accessToken);
    }

    /**
     * Finds a cached {@link AccessToken} instance by client id and audience.
     *
     * @param clientId client id
     * @param audience audience
     * @return a {@link Optional<AccessToken>} instance with token if found, otherwise empty
     */
    public Optional<AccessToken> getAccessToken(final String clientId, final String audience) {
        return Optional.ofNullable(accessTokens.get(AnalyticsHelper.get().resolveKey(clientId, audience)));
    }

    /**
     * Removes cache entry associated with provided access token key (client id + audience).
     *
     * @param clientId client id
     * @param audience audience
     */
    public void removeAccessToken(final String clientId, final String audience) {
        accessTokens.remove(AnalyticsHelper.get().resolveKey(clientId, audience));
    }

    /**
     * Removes cache entry associated with provided access token.
     *
     * @param accessToken access token tp remove
     */
    public void removeAccessToken(final AccessToken accessToken) {
        DotPreconditions.notNull(accessToken, "AccessToken cannot be null");
        accessTokens.remove(AnalyticsHelper.get().resolveKey(accessToken));
    }

}
