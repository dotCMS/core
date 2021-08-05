package com.dotcms.graphql;

import com.dotcms.enterprise.license.LicenseManager;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Tuple2;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Optional;

public class GraphQLCache implements Cachable {


    private final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    private boolean canCache;

    public Optional<String> get(final String key) {
        if(!canCache()) return Optional.empty();

        Optional<String> result = Optional.empty();
        final String cacheKey = hashQuery(key);
        final Tuple2<String, LocalDateTime> resultExpireTimeTuple
                = (Tuple2<String, LocalDateTime>) cache.getNoThrow(cacheKey, getPrimaryGroup());

        if(UtilMethods.isSet(resultExpireTimeTuple)) {
            final LocalDateTime expireTime = resultExpireTimeTuple._2();

            if (expireTime.isAfter(LocalDateTime.now())) {
                result = Optional.of(resultExpireTimeTuple._1());
            } else { // expired, let's remove from cache
                remove(cacheKey);
            }
        }

        return result;
    }


    public void put(String query, String result, int cacheTTL) {
        if(UtilMethods.isNotSet(result)) return;

        final LocalDateTime cachedSincePlusTTL = LocalDateTime.now().plus(cacheTTL,
                ChronoField.SECOND_OF_DAY.getBaseUnit());

        Tuple2<String, LocalDateTime> resultExpireTimeTuple =
                new Tuple2<>(result, cachedSincePlusTTL);
        final String cacheKey = hashQuery(query);
        cache.put(cacheKey, resultExpireTimeTuple, getPrimaryGroup());
    }

    private String hashQuery(final String query) {
        long hashCode = 1125899906842597L;
        for (int i = 0; i < query.length(); i++) {
            hashCode = 31 * hashCode + query.charAt(i);
        }

        return String.valueOf(hashCode);
    }


    public enum INSTANCE {
        INSTANCE;
        final GraphQLCache cache = new GraphQLCache();

        public static GraphQLCache get() {
            return INSTANCE.cache;
        }
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
        if(!canCache) {
            canCache = LicenseManager.getInstance().isEnterprise();
        }
        return canCache;
    }

    public void remove(final String key) {
        final String cacheKey = hashQuery(key);
        this.cache.remove(cacheKey, getPrimaryGroup());
    }

}
