package com.dotcms.graphql;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.Tuple2;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Optional;

/**
 * A Expiring cache implementation to store the results of GraphQL requests.
 *
 * The amount of time the results will be cached can be specified either by providing a TTL (int)
 * by calling {@link #put(String, String, Integer)} method or by the config property <code>cache.graphqlquerycache.seconds</code>
 * which applies when calling {@link #put(String, String)}, which does not take a TTL
 *
 * This entire cache can be turned off by setting GRAPHQL_CACHE_RESULTS=false in your dotmarketing-config.properties
 *
 */
public class GraphQLCache implements Cachable {


    private final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    private final Lazy<Boolean> ENABLED_FROM_CONFIG = Lazy.of(()->Config
            .getBooleanProperty("GRAPHQL_CACHE_RESULT", true));

    public Optional<String> get(final String key) {
        if(!canCache()) {
            return Optional.empty();
        }

        Optional<String> result = Optional.empty();
        final String cacheKey = hashKey(key);
        final Tuple2<String, LocalDateTime> resultExpireTimeTuple
                = (Tuple2<String, LocalDateTime>) cache.getNoThrow(cacheKey, getPrimaryGroup());

        if(UtilMethods.isSet(resultExpireTimeTuple)) {
            final LocalDateTime expireTime = resultExpireTimeTuple._2();

            if (expireTime==null || expireTime.isAfter(LocalDateTime.now())) {
                result = Optional.of(resultExpireTimeTuple._1());
            } else { // expired, let's remove from cache
                remove(cacheKey);
            }
        }

        return result;
    }


    public void put(String key, String result, Integer cacheTTL) {
        if(!canCache()) return;

        if(UtilMethods.isNotSet(result)) return;

        final LocalDateTime cachedSincePlusTTL = cacheTTL!=null ? LocalDateTime.now().plus(cacheTTL,
                ChronoField.SECOND_OF_DAY.getBaseUnit()) : null;

        Tuple2<String, LocalDateTime> resultExpireTimeTuple =
                new Tuple2<>(result, cachedSincePlusTTL);
        final String cacheKey = hashKey(key);
        cache.put(cacheKey, resultExpireTimeTuple, getPrimaryGroup());
    }

    public void put(String key, String result) {
        this.put(key, result, null);
    }

    private String hashKey(final String query) {
        long hashCode = 1125899906842597L;
        for (int i = 0; i < query.length(); i++) {
            hashCode = 31 * hashCode + query.charAt(i);
        }

        return String.valueOf(hashCode);
    }

    @Override
    public void clearCache() {
        cache.flushGroup(getPrimaryGroup());

    }


    @Override
    public String[] getGroups() {
        return new String[] {getPrimaryGroup()};
    }


    @Override
    public String getPrimaryGroup() {
        return "graphqlquerycache";
    }

    private boolean canCache() {
        return LicenseManager.getInstance().isEnterprise() && ENABLED_FROM_CONFIG.get();
    }

    public void remove(final String key) {
        final String cacheKey = hashKey(key);
        this.cache.remove(cacheKey, getPrimaryGroup());
    }

}
