package com.dotcms.analytics.cache;

import com.dotcms.analytics.model.AccessToken;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;

import java.util.Arrays;
import java.util.Optional;


/**
 * Analytics cache layer.
 *
 * @author vico
 */
public class AnalyticsCache implements Cachable {

    private static final String CACHE_GROUP = AnalyticsCache.class.getSimpleName().toLowerCase();
    private static final String[] GROUPS = { CACHE_GROUP };
    private static final String ANALYTICS_ACCESS_TOKEN_KEY = "ANALYTICS_ACCESS_TOKEN";

    private final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

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
            Logger.error(this, "Error while trying to get from cache the access token", e);
            return Optional.empty();
        }
    }

    /**
     * Finds a cached {@link AccessToken} instance using default key.
     *
     * @return a {@link Optional<AccessToken>} instance with token if found, otherwise empty
     */
    public Optional<AccessToken> getAccessToken() {
        return getAccessToken(ANALYTICS_ACCESS_TOKEN_KEY);
    }

    /**
     * Adds an {@link AccessToken} instance to cache identified by provided key.
     *
     * @param key key access token is mapped to
     * @param accessToken access token to cache
     */
    public void putAccessToken(final String key, final AccessToken accessToken) {
        cache.put(key, accessToken, getPrimaryGroup());
    }

    /**
     * Adds an {@link AccessToken} instance to cache identified by default cache.
     *
     * @param accessToken access token to cache
     */
    public void putAccessToken(final AccessToken accessToken) {
        putAccessToken(ANALYTICS_ACCESS_TOKEN_KEY, accessToken);
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

}
