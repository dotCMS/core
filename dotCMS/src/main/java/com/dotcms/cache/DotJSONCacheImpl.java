package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.enterprise.license.LicenseLevel;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Optional;

/**
 * Provides the caching implementation for HTML pages. This approach uses a main
 * key to retrieve a cached page, and a subkey to retrieve the different
 * versions of it. With this structure, during the removal of a page, all the
 * different versions of it will also be deleted easily. So, basically:
 * <ul>
 * 	<li>
 * 		The main key is composed of:
 * 		<ul>
 * 		<li>The page Inode.</li>
 * 		<li>The page modification date in milliseconds.</li>
 * 		</ul>
 *  </li>
 *  <li>
 * 		The subkey is composed of:
 * 		<ul>
 * 		<li>The current user ID.</li>
 * 		<li>The currently selected language ID.</li>
 * 		<li>The URL map.</li>
 * 		<li>The query String in the URL.</li>
 * 		</ul>
 *  </li>
 * </ul>
 *
 * @author Jose Castro
 * @version 1.0
 * @since 10-17-2014
 *
 */
public class DotJSONCacheImpl extends DotJSONCache {

    private boolean canCache;
    private final DotCacheAdministrator cache;
    private static String primaryCacheGroup = "DotJSONCache";

    /**
     * Default constructor. Initializes the internal caching structures.
     */
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
    public void add(final DotJSONCacheKey dotJSONCacheKey, final DotJSON dotJSON) {
        DotPreconditions.checkNotNull(dotJSONCacheKey);
        DotPreconditions.checkArgument(UtilMethods.isSet(dotJSONCacheKey.getKey()));

        if(dotJSON.getCacheTTL()>0) {
            dotJSON.setCachedSince(LocalDateTime.now());
            this.cache.put(dotJSONCacheKey.getKey(), dotJSON, primaryCacheGroup);
        }
    }

    @Override
    public Optional<DotJSON> get(final DotJSONCacheKey dotJSONCacheKey) {
        Optional<DotJSON> dotJSONOptional = Optional.empty();

        if (!canCache || dotJSONCacheKey == null) {
            return dotJSONOptional;
        }

        synchronized(this.cache) {
            try {
                final DotJSON dotJSON = (DotJSON) this.cache.get(dotJSONCacheKey.getKey(), primaryCacheGroup);

                if(UtilMethods.isSet(dotJSON)) {
                    final LocalDateTime cachedSince = dotJSON.getCachedSince();

                    final LocalDateTime cachedSincePlusTTL = cachedSince.plus(dotJSON.getCacheTTL(),
                            ChronoField.MILLI_OF_DAY.getBaseUnit());

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
