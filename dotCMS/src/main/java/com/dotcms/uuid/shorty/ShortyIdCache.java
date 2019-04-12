package com.dotcms.uuid.shorty;

import java.util.Optional;


import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

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


    public Optional<ShortyId> get(String shortId) {

        try { 
            ShortyId shorty = (ShortyId) cache.get(shortId, SHORT_CACHE);
            if(shorty!=null)
                return Optional.of(shorty) ;
        } catch (DotCacheException e) {
            
        }

        return Optional.empty();
    }

    public void add(ShortyId shortyId) {

        cache.put(shortyId.shortId, shortyId, SHORT_CACHE);


    }

    public void remove(ShortyId ShortyId) {
        cache.remove(ShortyId.shortId, SHORT_CACHE);
    }



}
