package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Optional;

public class DotJSONCacheImpl extends DotJSONCache {

    private boolean canCache;
    private final DotCacheAdministrator cache;
    private static final String primaryCacheGroup = "DotJSONCache";

    public DotJSONCacheImpl() {
        this.cache = CacheLocator.getCacheAdministrator();
        this.canCache = LicenseUtil.getLevel() >= LicenseLevel.COMMUNITY.level;
    }

    @Override
    public String getPrimaryGroup() {
        return primaryCacheGroup;
    }

    @Override
    public String[] getGroups() {
        return new String[]{ primaryCacheGroup };
    }

    @Override
    public void clearCache() {
        cache.flushGroup(primaryCacheGroup);
    }

    @Override
    public void add(final HttpServletRequest request, final User user, final DotJSON dotJSON) {
        final DotJSONCache.DotJSONCacheKey cacheKey = getDotJSONCacheKey(request, user);

        if(dotJSON.getCacheTTL()>0) {
            dotJSON.setCachedSince(LocalDateTime.now());
            this.cache.put(cacheKey.getKey(), dotJSON, primaryCacheGroup);
        }
    }

    @Override
    public Optional<DotJSON> get(final HttpServletRequest request, final User user) {
        Optional<DotJSON> dotJSONOptional = Optional.empty();

        if (!canCache) {
            return dotJSONOptional;
        }

        synchronized(this.cache) {
            final DotJSONCache.DotJSONCacheKey dotJSONCacheKey = getDotJSONCacheKey(request, user);

            try {
                final DotJSON dotJSON = (DotJSON) this.cache.get(dotJSONCacheKey.getKey(), primaryCacheGroup);

                if(UtilMethods.isSet(dotJSON)) {
                    final LocalDateTime cachedSince = dotJSON.getCachedSince();

                    final LocalDateTime cachedSincePlusTTL = cachedSince.plus(dotJSON.getCacheTTL(),
                            ChronoField.SECOND_OF_DAY.getBaseUnit());

                    if (cachedSincePlusTTL.isAfter(LocalDateTime.now())) {
                        dotJSONOptional = Optional.of(dotJSON);
                    } else {
                        // expired, let's remove from cache
                        remove(dotJSONCacheKey);
                    }
                }
            } catch (DotCacheException e) {
                Logger.error(this, "Unable to find cache entry. Key:" + dotJSONCacheKey.getKey());
            }
        }

        return dotJSONOptional;
    }

    @Override
    public void remove(final DotJSONCacheKey dotJSONCacheKey) {
        DotPreconditions.checkNotNull(dotJSONCacheKey);
        this.cache.remove(dotJSONCacheKey.getKey(), primaryCacheGroup);
    }

}
