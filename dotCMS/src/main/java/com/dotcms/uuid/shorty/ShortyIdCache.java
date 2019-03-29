package com.dotcms.uuid.shorty;

import java.util.Optional;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;

public class ShortyIdCache implements Cachable {

    private final DotCacheAdministrator cache;
    final String SHORT_CACHE = "ShortyIdCache";

    public ShortyIdCache(DotCacheAdministrator cache) {
        super();
        this.cache = cache;
    }

    public ShortyIdCache() {
        super();
        this.cache = CacheLocator.getCacheAdministrator();
    }

    @Override
    public String getPrimaryGroup() {
        return SHORT_CACHE;
    }

    @Override
    public String[] getGroups() {
        return new String[] {getPrimaryGroup()};
    }

    @Override
    public void clearCache() {
        CacheLocator.getCacheAdministrator().flushGroup(getPrimaryGroup());
    }

    public Optional<ShortyId> get(String id) {

        final String shortUId = APILocator.getShortyAPI().shortify(id);

        return Optional.ofNullable((ShortyId) cache.getNoThrow(shortUId, SHORT_CACHE));

    }

    public void add(ShortyId shortyId) {
        final String shortUId = APILocator.getShortyAPI().shortify(shortyId.shortId);
        
        cache.put(shortUId, shortyId, SHORT_CACHE);

    }

    public void remove(ShortyId ShortyId) {
        cache.remove(ShortyId.longId, SHORT_CACHE);
    }

    public void remove(final String id) {
        final String shortUId = APILocator.getShortyAPI().shortify(id);

        cache.remove(shortUId, SHORT_CACHE);
    }

}
