package com.dotcms.analytics.cache;

import com.dotcms.analytics.model.AccessToken;
import com.dotcms.rest.validation.Preconditions;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


/**
 * Analytics cache layer.
 *
 * @author vico
 */
public class AnalyticsCache implements Cachable {

    private static final String CACHE_GROUP = AnalyticsCache.class.getSimpleName().toLowerCase();
    private static final String[] GROUPS = { CACHE_GROUP };
    private static final String ANALYTICS_ACCESS_TOKEN_KEY_PREFIX = "ANALYTICS_ACCESS_TOKEN";

    private final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    /**
     * Adds an {@link AccessToken} instance to cache identified by default cache.
     *
     * @param accessToken access token to cache
     */
    public void putAccessToken(final AccessToken accessToken) {
        Preconditions.checkNotNull(accessToken, DotStateException.class, "ACCESS_TOKEN is missing");
        cache.put(resolveKey(accessToken), accessToken, getPrimaryGroup());
    }

    /**
     * Finds a cached {@link AccessToken} instance.
     *
     * @param key key access token is mapped to
     * @return a {@link Optional<AccessToken>} instance with token if found, otherwise empty
     */
    public Optional<AccessToken> getAccessToken(final String key) {
        try {
            return Optional.ofNullable((AccessToken) cache.get(key, getPrimaryGroup()));
        } catch (DotCacheException e) {
            Logger.error(this, "Error while trying to get from cache the ACCESS_TOKEN", e);
            return Optional.empty();
        }
    }

    /**
     * Finds a cached {@link AccessToken} instance using a key created with provided client id and audience.
     *
     * @param clientId token client id
     * @param audience token audience
     * @return a {@link Optional<AccessToken>} instance with token if found, otherwise empty
     */
    public Optional<AccessToken> getAccessToken(final String clientId, final String audience) {
        return getAccessToken(resolveKey(clientId, audience));
    }

    /**
     * Removes cache entry associated with provided token client id and audience.
     *
     * @param clientId token client id
     * @param audience token audience
     */
    public void removeAccessToken(final String clientId, final String audience) {
        cache.remove(resolveKey(clientId, audience), getPrimaryGroup());
    }

    /**
     * Removes cache entry associated with provided {@link AccessToken}.
     *
     * @param accessToken access token
     */
    public void removeAccessToken(final AccessToken accessToken) {
        removeAccessToken(accessToken.clientId(), accessToken.aud());
    }

    /**
     * @return the primary group/region for the concrete cache
     */
    @Override
    public String getPrimaryGroup() {
        return CACHE_GROUP;
    }

    /**
     * @return all groups the concrete cache belongs to
     */
    @Override
    public String[] getGroups()  {
        return GROUPS;
    }

    /**
     * Flushes each group cache.
     */
    @Override
    public void clearCache() {
        Arrays.asList(getGroups()).forEach(cache::flushGroup);
    }

    /**
     * Resolves a cache key from token client id and audience.
     *
     * @param clientId token client id
     * @param audience token audience
     * @return key to use as key to cache for a specific access token
     */
    private String resolveKey(final String clientId, final String audience) {
        final List<String> keyChunks = new ArrayList<>();
        keyChunks.add(ANALYTICS_ACCESS_TOKEN_KEY_PREFIX);

        if (StringUtils.isNotBlank(clientId)) {
            keyChunks.add(clientId);
        }

        if (StringUtils.isNotBlank(audience)) {
            keyChunks.add(audience);
        }

        return String.join(StringPool.UNDERLINE, keyChunks);
    }

    /**
     * Creates a cache key from given {@link AccessToken} evaluating several conditions.
     *
     * @param accessToken provided access token
     * @return key to use as key to cache for a specific access token
     */
    private String resolveKey(final AccessToken accessToken) {
        return resolveKey(accessToken.clientId(), accessToken.aud());
    }

}
